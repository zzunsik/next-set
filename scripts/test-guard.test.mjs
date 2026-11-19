// test-guard.mjs의 자체 테스트. 실행: node --test scripts/test-guard.test.mjs
// 임시 git 저장소에 base 커밋과 head 커밋을 만들고 판정을 확인한다.

import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { after, describe, test } from 'node:test';

import { analyze, findExceptionReasons, findTestFunctions, isVerificationLine, main } from './test-guard.mjs';

const TEST_DIR = 'app/src/test/java/io/example';
const WEIGHT = `${TEST_DIR}/WeightTest.kt`;

const WEIGHT_BASE = `package io.example

import org.junit.Assert.assertEquals
import org.junit.Test

class WeightTest {
    @Test
    fun parses() {
        val w = parse("60")
        assertEquals(60, w)
        assertEquals(61, parse("61"))
    }

    @Test
    fun formats() {
        assertEquals("60", format(60))
    }

    @Test
    fun oneLiner() = assertEquals(1, 1)

    private fun helper(x: Int) {
        assertEquals(0, x % 2)
    }
}
`;

const SCREEN_BASE = `package io.example

import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Test

class ScreenTest {
    @Test
    fun showsTitle() {
        compose.setContent { Screen() }
        compose.onNodeWithText("제목").assertIsDisplayed()
        compose.assertAccessible()
    }
}
`;

const BASE_FILES = {
  [WEIGHT]: WEIGHT_BASE,
  [`${TEST_DIR}/ScreenTest.kt`]: SCREEN_BASE,
  'app/src/test/resources/robolectric.properties': 'sdk=36\n',
  'app/src/test/screenshots/io.example.ScreenTest.light.png': 'png-1',
  'app/src/main/java/io/example/Weight.kt': 'fun parse(s: String) = s.toInt()\n',
};

const roots = [];
after(() => {
  for (const r of roots) rmSync(r, { recursive: true, force: true });
});

function git(cwd, ...args) {
  return execFileSync('git', args, { cwd, encoding: 'utf8' }).trim();
}

function write(cwd, files) {
  for (const [path, content] of Object.entries(files)) {
    const full = join(cwd, path);
    if (content === null) {
      rmSync(full);
      continue;
    }
    mkdirSync(dirname(full), { recursive: true });
    writeFileSync(full, content);
  }
}

/** base 커밋을 만들고 head에서 files를 바꾼 저장소를 돌려준다. null은 지우기. */
function repoWith(changes) {
  const cwd = mkdtempSync(join(tmpdir(), 'test-guard-'));
  roots.push(cwd);
  git(cwd, 'init', '-q', '-b', 'develop');
  git(cwd, 'config', 'user.email', 'guard@example.invalid');
  git(cwd, 'config', 'user.name', 'guard');
  git(cwd, 'config', 'core.autocrlf', 'false');
  write(cwd, BASE_FILES);
  git(cwd, 'add', '.');
  git(cwd, 'commit', '-q', '-m', 'base');
  const base = git(cwd, 'rev-parse', 'HEAD');
  git(cwd, 'checkout', '-q', '-b', 'feature');
  write(cwd, changes);
  git(cwd, 'add', '-A', '.');
  git(cwd, 'commit', '-q', '--allow-empty', '-m', 'head');
  const head = git(cwd, 'rev-parse', 'HEAD');
  return { cwd, base, head };
}

function kinds(changes) {
  const { cwd, base, head } = repoWith(changes);
  return analyze({ cwd, base, head }).violations.map((v) => v.kind);
}

function weightWith(from, to) {
  assert.ok(WEIGHT_BASE.includes(from), `기준 문자열이 없습니다: ${from}`);
  return { [WEIGHT]: WEIGHT_BASE.replace(from, to) };
}

describe('통과하는 변경', () => {
  test('바뀐 것이 없다', () => assert.deepEqual(kinds({}), []));

  test('기존 파일에 테스트를 더한다', () => {
    const added = '\n    @Test\n    fun rounds() {\n        assertEquals(2, round(1.5))\n    }\n';
    assert.deepEqual(kinds(weightWith('\n    @Test\n    fun oneLiner()', `${added}\n    @Test\n    fun oneLiner()`)), []);
  });

  test('기존 테스트 함수에 검증을 더한다', () => {
    assert.deepEqual(kinds(weightWith('assertEquals(61, parse("61"))', 'assertEquals(61, parse("61"))\n        assertEquals(62, parse("62"))')), []);
  });

  test('새 테스트 파일을 더한다', () => {
    assert.deepEqual(kinds({ [`${TEST_DIR}/NewTest.kt`]: 'class NewTest {\n    @Test\n    fun a() = assertTrue(true)\n}\n' }), []);
  });

  test('새 테스트 리소스를 더한다', () => {
    assert.deepEqual(kinds({ 'app/src/test/resources/fixture.json': '{}\n' }), []);
  });

  test('앱 코드만 바꾼다', () => {
    assert.deepEqual(kinds({ 'app/src/main/java/io/example/Weight.kt': 'fun parse(s: String) = s.trim().toInt()\n' }), []);
  });

  test('기준 스크린샷만 바꾸거나 지운다', () => {
    assert.deepEqual(kinds({ 'app/src/test/screenshots/io.example.ScreenTest.light.png': 'png-2' }), []);
    assert.deepEqual(kinds({ 'app/src/test/screenshots/io.example.ScreenTest.light.png': null }), []);
  });

  test('들여쓰기만 바꾼다', () => {
    assert.deepEqual(kinds(weightWith('        assertEquals(60, w)', '        assertEquals(60,  w)')), []);
  });

  test('테스트 밖의 import를 정리한다', () => {
    assert.deepEqual(kinds(weightWith('import org.junit.Assert.assertEquals\n', 'import org.junit.Assert.assertEquals as eq\n')), []);
  });

  test('테스트 밖의 주석만 지운다', () => {
    assert.deepEqual(kinds({ [WEIGHT]: WEIGHT_BASE.replace('class WeightTest {', '// assertEquals 설명\nclass WeightTest {') }), []);
  });
});

describe('실패하는 변경', () => {
  test('검증 줄을 지운다', () => {
    assert.deepEqual(kinds(weightWith('        assertEquals(61, parse("61"))\n', '')), ['test-changed']);
  });

  test('검증 줄의 기대값을 바꾼다', () => {
    assert.deepEqual(kinds(weightWith('assertEquals("60", format(60))', 'assertEquals("60.0", format(60))')), ['test-changed']);
  });

  test('검증 줄을 주석으로 막는다', () => {
    assert.deepEqual(kinds(weightWith('        assertEquals(60, w)', '        // assertEquals(60, w)')), ['test-changed']);
  });

  test('테스트 함수의 준비 줄을 바꾼다', () => {
    assert.deepEqual(kinds(weightWith('val w = parse("60")', 'val w = 60')), ['test-changed']);
  });

  test('테스트 함수를 지운다', () => {
    const r = kinds(weightWith('    @Test\n    fun formats() {\n        assertEquals("60", format(60))\n    }\n\n', ''));
    assert.deepEqual(r, ['test-removed']);
  });

  test('테스트 이름을 바꾼다', () => {
    assert.deepEqual(kinds(weightWith('fun formats()', 'fun formatsKg()')), ['test-removed']);
  });

  test('한 줄짜리 테스트를 바꾼다', () => {
    assert.deepEqual(kinds(weightWith('fun oneLiner() = assertEquals(1, 1)', 'fun oneLiner() = Unit')), ['test-changed']);
  });

  test('@Test 주석을 지운다', () => {
    assert.deepEqual(kinds(weightWith('    @Test\n    fun formats()', '    fun formats()')), ['test-removed']);
  });

  test('테스트 밖 도우미의 검증 줄을 바꾼다', () => {
    assert.deepEqual(kinds(weightWith('assertEquals(0, x % 2)', 'println(x)')), ['verify-changed']);
  });

  test('Compose 검증과 접근성 검사를 지운다', () => {
    const path = `${TEST_DIR}/ScreenTest.kt`;
    const r = kinds({ [path]: SCREEN_BASE.replace('        compose.assertAccessible()\n', '') });
    assert.deepEqual(r, ['test-changed']);
  });

  test('테스트 파일을 지운다', () => {
    assert.deepEqual(kinds({ [`${TEST_DIR}/ScreenTest.kt`]: null }), ['test-removed']);
  });

  test('@Ignore를 기존 테스트에 붙인다', () => {
    assert.deepEqual(kinds(weightWith('    @Test\n    fun formats()', '    @Ignore("나중에")\n    @Test\n    fun formats()')), ['skip']);
  });

  test('@Ignore를 붙인 새 테스트를 더한다', () => {
    const r = kinds({ [`${TEST_DIR}/NewTest.kt`]: 'class NewTest {\n    @org.junit.Ignore\n    @Test\n    fun a() = assertTrue(true)\n}\n' });
    assert.deepEqual(r, ['skip']);
  });

  test('assume으로 건너뛰게 한다', () => {
    assert.deepEqual(kinds(weightWith('        val w = parse("60")\n', '        val w = parse("60")\n        assumeTrue(false)\n')), ['skip']);
  });

  test('기존 테스트 중간에 return을 끼워 넣는다', () => {
    assert.deepEqual(kinds(weightWith('        val w = parse("60")\n', '        val w = parse("60")\n        return\n')), ['test-changed']);
  });

  test('테스트 리소스를 바꾸거나 지운다', () => {
    assert.deepEqual(kinds({ 'app/src/test/resources/robolectric.properties': 'sdk=35\n' }), ['resource']);
    assert.deepEqual(kinds({ 'app/src/test/resources/robolectric.properties': null }), ['resource']);
  });

  test('테스트를 다른 파일로 옮기면 근거를 보이고 실패한다', () => {
    const { cwd, base, head } = repoWith({
      ...weightWith('    @Test\n    fun formats() {\n        assertEquals("60", format(60))\n    }\n\n', ''),
      [`${TEST_DIR}/FormatTest.kt`]: 'class FormatTest {\n    @Test\n    fun formats() {\n        assertEquals("60", format(60))\n    }\n}\n',
    });
    const { violations, evidence } = analyze({ cwd, base, head });
    assert.deepEqual(violations.map((v) => v.kind), ['test-removed']);
    assert.equal(evidence.length, 1);
    assert.match(evidence[0], /FormatTest\.kt:2/);
  });
});

describe('예외 표기', () => {
  test('본문의 사유를 읽는다', () => {
    assert.deepEqual(findExceptionReasons('## 변경\n- 무게 표기\n\n테스트 기대값 변경: 소수점 표기를 바꿈\n', []), [
      'PR 본문: 테스트 기대값 변경: 소수점 표기를 바꿈',
    ]);
    assert.equal(findExceptionReasons('- **테스트 기대값 변경**: 이유\r\n', []).length, 1);
  });

  test('빈 사유, 자리표시, HTML 주석, 문장 속 언급은 세지 않는다', () => {
    assert.deepEqual(findExceptionReasons('테스트 기대값 변경:\n테스트 기대값 변경:   \n', []), []);
    assert.deepEqual(findExceptionReasons('테스트 기대값 변경: <사유>\n', []), []);
    assert.deepEqual(findExceptionReasons('<!--\n테스트 기대값 변경: 예시 사유\n-->\n', []), []);
    assert.deepEqual(findExceptionReasons('본문에 `테스트 기대값 변경: <사유>`를 적으면 통과한다\n', []), []);
    assert.deepEqual(findExceptionReasons(null, undefined), []);
  });

  test('라벨을 읽는다', () => {
    assert.deepEqual(findExceptionReasons('', ['세팅', 'test-change-approved']), ['라벨: test-change-approved']);
    assert.deepEqual(findExceptionReasons('', ['세팅']), []);
  });
});

describe('main', () => {
  const quiet = (fn) => {
    const log = console.log;
    const err = console.error;
    console.log = () => {};
    console.error = () => {};
    try {
      return fn();
    } finally {
      console.log = log;
      console.error = err;
    }
  };
  const removal = () => repoWith(weightWith('        assertEquals(61, parse("61"))\n', ''));

  test('위반이 있으면 1, 예외 표기가 있으면 0', () => {
    const { cwd, base, head } = removal();
    const args = ['--base', base, '--head', head];
    assert.equal(quiet(() => main(args, { PR_BODY: '', PR_LABELS: '[]' }, cwd)), 1);
    assert.equal(quiet(() => main(args, { PR_BODY: '테스트 기대값 변경: 의도한 변경', PR_LABELS: '[]' }, cwd)), 0);
    assert.equal(quiet(() => main(args, { PR_BODY: '', PR_LABELS: '["test-change-approved"]' }, cwd)), 0);
  });

  test('--pr-json의 현재 PR 값을 이벤트 값보다 먼저 쓴다', () => {
    const { cwd, base, head } = removal();
    const json = join(cwd, 'pr.json');
    writeFileSync(json, JSON.stringify({ body: '테스트 기대값 변경: API 본문', labels: [{ name: '세팅' }] }));
    assert.equal(quiet(() => main(['--base', base, '--head', head, '--pr-json', json], { PR_BODY: '' }, cwd)), 0);
    writeFileSync(json, JSON.stringify({ body: '사유 없음', labels: [] }));
    assert.equal(quiet(() => main(['--base', base, '--head', head, '--pr-json', json], { PR_BODY: '테스트 기대값 변경: 옛 본문' }, cwd)), 1);
  });

  test('위반이 없으면 0, 인자가 틀리면 2', () => {
    const { cwd, base, head } = repoWith({});
    assert.equal(quiet(() => main(['--base', base, '--head', head], {}, cwd)), 0);
    assert.equal(quiet(() => main(['--base', base], {}, cwd)), 2);
    assert.equal(quiet(() => main(['--base', 'nope', '--head', head], {}, cwd)), 2);
  });

  test('merge-base 뒤에 base 브랜치가 바꾼 테스트는 PR 탓으로 보지 않는다', () => {
    const { cwd, head } = removal();
    git(cwd, 'checkout', '-q', 'develop');
    write(cwd, { [`${TEST_DIR}/ScreenTest.kt`]: null });
    git(cwd, 'add', '-A', '.');
    git(cwd, 'commit', '-q', '-m', 'develop moved');
    const movedBase = git(cwd, 'rev-parse', 'HEAD');
    const { violations } = analyze({ cwd, base: movedBase, head });
    assert.deepEqual(violations.map((v) => v.path), [WEIGHT]);
  });
});

describe('도우미', () => {
  test('검증 줄 판정', () => {
    for (const line of [
      'assertEquals(1, x)',
      'assertThat(x).isEqualTo(1)',
      '    .isEqualTo(1)',
      'compose.onNodeWithText("a").assertIsDisplayed()',
      'compose.onNodeWithText("a")',
      'compose.assertAccessible()',
      'assertFailsWith<IllegalStateException> { f() }',
      'x shouldBe 1',
      'captureRoboImage("a.png")',
      'fail("x")',
      'val url = "http://a" ; assertTrue(ok)',
    ]) assert.ok(isVerificationLine(line), line);
    for (const line of ['val x = 1', '// assertEquals(1, x)', ' * assertThat 설명', 'import org.junit.Assert.assertEquals', 'compose.setContent { A() }']) {
      assert.ok(!isVerificationLine(line), line);
    }
  });

  test('테스트 함수 범위', () => {
    const fns = findTestFunctions(WEIGHT_BASE);
    assert.deepEqual(fns.map((f) => [f.name, f.start, f.end]), [
      ['parses', 7, 12],
      ['formats', 14, 17],
      ['oneLiner', 19, 20],
    ]);
    const backtick = findTestFunctions('class A {\n    @Test fun `이름 있는 테스트`() {\n        assertTrue(true)\n    }\n}\n');
    assert.deepEqual(backtick.map((f) => [f.name, f.start, f.end]), [['이름 있는 테스트', 2, 4]]);
  });
});
