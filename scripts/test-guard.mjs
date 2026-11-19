#!/usr/bin/env node
// test-guard: PR이 기존 테스트를 지우거나 약하게 바꾸지 않았는지 본다(#43).
//
// 사용: node scripts/test-guard.mjs --base <base sha> --head <head sha> [--pr-json <파일>]
//   두 커밋의 merge-base와 head를 비교한다. PR 본문과 라벨은 --pr-json(GitHub API의 PR JSON)에서 먼저 읽고,
//   없으면 환경 변수 PR_BODY, PR_LABELS(JSON 배열)에서 읽는다.
// 종료 코드: 0 통과(위반 없음 또는 예외 표기 있음), 1 위반, 2 사용법·git 오류.
//
// Node 내장 모듈만 쓴다. 자체 테스트는 scripts/test-guard.test.mjs다.

import { execFileSync } from 'node:child_process';
import { appendFileSync, existsSync, readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

export const EXCEPTION_LABEL = 'test-change-approved';
export const EXCEPTION_PREFIX = '테스트 기대값 변경';

// 검사 대상: 테스트 소스 세트(app/src/test, app/src/androidTest, app/src/testDebug 등).
const TEST_ROOT_RE = /^app\/src\/(test|androidTest)[^/]*\//;
// 기준 스크린샷은 빼고 CI 비교와 리뷰로 다룬다.
const SCREENSHOT_RE = /^app\/src\/[^/]+\/screenshots\//;
const SOURCE_RE = /\.(kt|kts|java)$/;

// 테스트 함수에 붙는 주석(annotation).
const TEST_ANNOTATION_RE = /@(?:[\w.]+\.)?(?:Test|ParameterizedTest|RepeatedTest|TestFactory)\b/;
// 테스트를 건너뛰게 하는 것. 테스트 소스 어디에 더해도 실패한다.
const SKIP_RES = [
  /@(?:[\w.]+\.)?(?:Ignore|Disabled)\b/,
  /\bassume\w*\s*\(/,
  /\bAssume\.\w+/,
];
// 검증 줄. 지우거나 바꾸면 실패한다.
const VERIFY_RES = [
  /\bassert\w*/, // assert, assertEquals, assertThat, assertTrue, assertAccessible, assertIsDisplayed …
  /\b(?:fail|check|checkNotNull|require|requireNotNull)\s*\(/,
  /\b(?:verify|coVerify|confirmVerified)\w*\s*[({]/,
  /\bshould[A-Z]\w*/, // kotest: x shouldBe y
  /^\s*\.(?:is|has|contains|doesNot|matches|startsWith|endsWith)\w*\s*\(/, // Truth/AssertJ 체인의 이어진 줄
  /\bon(?:All)?Nodes?\w*\s*\(/, // Compose: onNode…, onAllNodes…
  /\bonRoot\s*\(/,
  /\b(?:capture|compare)RoboImage\b/,
  TEST_ANNOTATION_RE,
];
// 기존 테스트 함수 안에 새로 넣으면 검증을 건너뛰게 할 수 있는 줄.
const WEAKEN_INSERT_RES = [/\breturn\b/, /\brunCatching\b/, /\btry\s*\{/, /\bcatch\s*\(/, /^\s*try\s*$/];

export function isGuardedPath(path) {
  return TEST_ROOT_RE.test(path) && !SCREENSHOT_RE.test(path);
}

export function isSourcePath(path) {
  return SOURCE_RE.test(path);
}

/** 줄에서 주석을 지운 코드 부분. 주석만 있는 줄이면 빈 문자열. 문자열 안의 //는 남긴다. */
export function codeOf(line) {
  const t = line.trim();
  if (t.startsWith('//') || t.startsWith('*') || t.startsWith('/*')) return '';
  let quote = null;
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (quote) {
      if (c === '\\') i++;
      else if (c === quote) quote = null;
    } else if (c === '"' || c === "'") {
      quote = c;
    } else if (c === '/' && line[i + 1] === '/') {
      return line.slice(0, i);
    }
  }
  return line;
}

const isImport = (code) => /^\s*(?:import|package)\s/.test(code);

export function isVerificationLine(line) {
  const code = codeOf(line);
  if (!code.trim() || isImport(code)) return false;
  return VERIFY_RES.some((re) => re.test(code));
}

export function isSkipLine(line) {
  const code = codeOf(line);
  if (!code.trim() || isImport(code)) return false;
  return SKIP_RES.some((re) => re.test(code));
}

function isWeakeningInsert(line) {
  const code = codeOf(line);
  if (!code.trim()) return false;
  return WEAKEN_INSERT_RES.some((re) => re.test(code));
}

const indentOf = (line) => line.length - line.trimStart().length;

/**
 * 소스에서 테스트 함수를 찾는다. 범위는 들여쓰기로 정한다: 주석 줄부터, 그 주석보다 들여쓰기가
 * 깊지 않은 첫 줄 앞까지(그 줄이 닫는 괄호면 포함). 줄 번호는 1부터.
 */
export function findTestFunctions(text) {
  const lines = text.split(/\r?\n/);
  const found = [];
  for (let i = 0; i < lines.length; i++) {
    if (!TEST_ANNOTATION_RE.test(codeOf(lines[i]))) continue;
    let funLine = -1;
    let name = null;
    for (let j = i; j < Math.min(lines.length, i + 15); j++) {
      const code = codeOf(lines[j]);
      if (j > i && /\bclass\s/.test(code)) break;
      const m = /\bfun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(`[^`]+`|\w+)\s*\(/.exec(code);
      if (m) {
        funLine = j;
        name = m[1].replace(/`/g, '');
        break;
      }
    }
    if (funLine < 0) continue;
    const indent = indentOf(lines[i]);
    let end = funLine;
    for (let k = funLine + 1; k < lines.length; k++) {
      if (lines[k].trim() === '') continue;
      if (indentOf(lines[k]) <= indent) {
        if (/^\s*[})\]]/.test(lines[k])) end = k;
        break;
      }
      end = k;
    }
    found.push({ name, start: i + 1, end: end + 1 });
    i = funLine;
  }
  return found;
}

/** `git diff -U0` 출력에서 지운 줄(base 줄 번호)과 더한 줄(head 줄 번호, 끼워 넣기만 했으면 base에서 앞 줄 번호)을 꺼낸다. */
export function parseUnifiedDiff(diffText) {
  const removed = [];
  const added = [];
  let oldLine = 0;
  let newLine = 0;
  let anchor = 0;
  let inHunk = false;
  for (const raw of diffText.split('\n')) {
    const h = /^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@/.exec(raw);
    if (h) {
      oldLine = Number(h[1]);
      const oldCount = h[2] === undefined ? 1 : Number(h[2]);
      newLine = Number(h[3]);
      // 지운 줄 없이 더하기만 한 덩어리는 base의 oldLine번째 줄 뒤에 들어간다. 바꾼 덩어리는 null.
      anchor = oldCount === 0 ? oldLine : null;
      inHunk = true;
      continue;
    }
    if (!inHunk) continue;
    if (raw.startsWith('-') && !raw.startsWith('---')) {
      removed.push({ line: oldLine, text: raw.slice(1) });
      oldLine++;
    } else if (raw.startsWith('+') && !raw.startsWith('+++')) {
      added.push({ line: newLine, after: anchor, text: raw.slice(1) });
      newLine++;
    } else if (raw.startsWith('diff ')) {
      inHunk = false;
    }
  }
  return { removed, added };
}

/** PR 본문과 라벨에서 예외 사유를 찾는다. HTML 주석 안의 안내 문구와 `<사유>` 같은 자리표시는 세지 않는다. */
export function findExceptionReasons(body, labels) {
  const reasons = [];
  const clean = String(body ?? '').replace(/<!--[\s\S]*?-->/g, '');
  const re = new RegExp(`^\\s*(?:[-*>]\\s*)*(?:\\*\\*)?${EXCEPTION_PREFIX}(?:\\*\\*)?\\s*[:：]\\s*(.*)$`);
  for (const line of clean.split(/\r?\n/)) {
    const m = re.exec(line);
    if (!m) continue;
    const reason = m[1].trim();
    if (!reason || /^<[^>]*>$/.test(reason)) continue;
    reasons.push(`PR 본문: ${EXCEPTION_PREFIX}: ${reason}`);
  }
  if ((labels ?? []).includes(EXCEPTION_LABEL)) reasons.push(`라벨: ${EXCEPTION_LABEL}`);
  return reasons;
}

function git(cwd, args) {
  return execFileSync('git', ['-c', 'core.quotePath=false', ...args], {
    cwd,
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
    maxBuffer: 256 * 1024 * 1024,
  });
}

function show(cwd, rev, path) {
  return git(cwd, ['show', `${rev}:${path}`]);
}

/** base(merge-base)와 head를 비교해 위반과 근거를 돌려준다. */
export function analyze({ cwd = process.cwd(), base, head }) {
  const mergeBase = git(cwd, ['merge-base', base, head]).trim();
  const raw = git(cwd, ['diff', '--name-status', '--no-renames', '-z', mergeBase, head, '--', 'app/src']);
  const parts = raw.split('\0').filter((p) => p !== '');
  const changes = [];
  for (let i = 0; i + 1 < parts.length; i += 2) changes.push({ status: parts[i][0], path: parts[i + 1] });

  const violations = [];
  const addedTests = []; // { name, path, line } head에 새로 생긴 테스트(옮긴 테스트 근거)
  const addedLines = []; // { path, line, text } head에 새로 생긴 줄
  const removedTests = [];
  const removedVerifications = [];

  for (const { status, path } of changes) {
    if (!isGuardedPath(path)) continue;
    if (!isSourcePath(path)) {
      if (status === 'D') violations.push({ kind: 'resource', path, detail: '테스트 리소스를 지웠습니다' });
      else if (status !== 'A') violations.push({ kind: 'resource', path, detail: '테스트 리소스를 바꿨습니다' });
      continue;
    }
    const diff = git(cwd, ['diff', '-U0', '-b', '--no-renames', '--no-color', '--no-ext-diff', mergeBase, head, '--', path]);
    const { removed, added } = parseUnifiedDiff(diff);
    for (const a of added) {
      addedLines.push({ path, line: a.line, text: a.text });
      if (isSkipLine(a.text)) {
        violations.push({ kind: 'skip', path, line: a.line, detail: `테스트를 건너뛰게 하는 줄을 더했습니다: ${a.text.trim()}` });
      }
    }
    const headText = status === 'D' ? '' : show(cwd, head, path);
    const headTests = findTestFunctions(headText);
    if (status === 'A') {
      for (const t of headTests) addedTests.push({ name: t.name, path, line: t.start });
      continue;
    }
    const baseTests = findTestFunctions(show(cwd, mergeBase, path));
    const headNames = new Set(headTests.map((t) => t.name));
    const baseNames = new Set(baseTests.map((t) => t.name));
    for (const t of headTests) if (!baseNames.has(t.name)) addedTests.push({ name: t.name, path, line: t.start });

    const testAt = (n) => baseTests.find((t) => t.start <= n && n <= t.end);
    const changedTests = new Map();
    for (const t of baseTests) {
      if (!headNames.has(t.name)) {
        removedTests.push({ name: t.name, path });
        violations.push({ kind: 'test-removed', path, line: t.start, name: t.name, detail: `테스트 ${t.name}을(를) 지우거나 이름을 바꿨습니다` });
      }
    }
    for (const r of removed) {
      const t = testAt(r.line);
      if (t) {
        if (!headNames.has(t.name)) continue; // 위에서 함수째 보고했다
        if (!changedTests.has(t.name)) changedTests.set(t.name, { t, lines: [] });
        changedTests.get(t.name).lines.push(r);
      } else if (isVerificationLine(r.text)) {
        removedVerifications.push({ path, text: r.text.trim() });
        violations.push({ kind: 'verify-changed', path, line: r.line, detail: `검증 줄을 지우거나 바꿨습니다: ${r.text.trim()}` });
      }
    }
    for (const a of added) {
      // 지운 줄 없이 기존 테스트 함수 안에 끼워 넣은 줄 중 검증을 건너뛰게 할 수 있는 것.
      if (a.after === null) continue;
      const t = baseTests.find((x) => x.start <= a.after && a.after < x.end);
      if (t && headNames.has(t.name) && isWeakeningInsert(a.text)) {
        violations.push({ kind: 'test-changed', path, line: a.line, name: t.name, detail: `테스트 ${t.name}에 검증을 건너뛸 수 있는 줄을 더했습니다: ${a.text.trim()}` });
      }
    }
    for (const { t, lines } of changedTests.values()) {
      for (const r of lines) if (isVerificationLine(r.text)) removedVerifications.push({ path, text: r.text.trim() });
      const shown = lines.slice(0, 5).map((r) => `\n      - ${r.line}: ${r.text.trim()}`).join('');
      const more = lines.length > 5 ? `\n      … 외 ${lines.length - 5}줄` : '';
      violations.push({ kind: 'test-changed', path, line: t.start, name: t.name, detail: `테스트 ${t.name}의 줄을 지우거나 바꿨습니다:${shown}${more}` });
    }
  }

  // 옮긴 테스트의 근거: 지운 테스트와 같은 이름의 테스트, 지운 검증 줄과 같은 줄이 다른 곳에 생겼는지.
  const evidence = [];
  for (const r of removedTests) {
    for (const a of addedTests.filter((x) => x.name === r.name && x.path !== r.path)) {
      evidence.push(`테스트 ${r.name}: ${r.path}에서 사라지고 ${a.path}:${a.line}에 같은 이름이 생겼습니다(옮긴 것으로 보임)`);
    }
  }
  for (const r of removedVerifications) {
    const same = addedLines.find((a) => a.text.trim() === r.text);
    if (same) evidence.push(`검증 줄 "${r.text}": ${r.path}에서 사라지고 ${same.path}:${same.line}에 같은 줄이 있습니다`);
  }
  return { mergeBase, violations, evidence };
}

function readPr({ prJson, env }) {
  if (prJson && existsSync(prJson)) {
    try {
      const pr = JSON.parse(readFileSync(prJson, 'utf8'));
      return { body: pr.body ?? '', labels: (pr.labels ?? []).map((l) => (typeof l === 'string' ? l : l.name)), source: 'GitHub API(현재 PR)' };
    } catch (e) {
      console.log(`PR JSON을 읽지 못해 이벤트 값을 씁니다: ${e.message}`);
    }
  }
  let labels = [];
  try {
    labels = JSON.parse(env.PR_LABELS || '[]');
  } catch {
    labels = String(env.PR_LABELS).split('\n').filter(Boolean);
  }
  return { body: env.PR_BODY ?? '', labels, source: '이벤트 페이로드' };
}

function parseArgs(argv) {
  const args = {};
  for (let i = 0; i < argv.length; i++) {
    const m = /^--(base|head|pr-json)$/.exec(argv[i]);
    if (!m || i + 1 >= argv.length) throw new Error(`알 수 없는 인자: ${argv[i]}`);
    args[m[1]] = argv[++i];
  }
  if (!args.base || !args.head) throw new Error('--base와 --head가 필요합니다');
  return args;
}

export function main(argv = process.argv.slice(2), env = process.env, cwd = process.cwd()) {
  let args;
  try {
    args = parseArgs(argv);
  } catch (e) {
    console.error(e.message);
    console.error('사용: node scripts/test-guard.mjs --base <sha> --head <sha> [--pr-json <파일>]');
    return 2;
  }
  let result;
  try {
    result = analyze({ cwd, base: args.base, head: args.head });
  } catch (e) {
    console.error(`git 비교에 실패했습니다: ${e.message}`);
    return 2;
  }
  const { mergeBase, violations, evidence } = result;
  const out = [];
  out.push(`비교: merge-base ${mergeBase.slice(0, 12)} → head ${args.head.slice(0, 12)}`);
  if (violations.length === 0) {
    out.push('기존 테스트를 지우거나 약하게 바꾼 곳이 없습니다.');
    report(out, env);
    return 0;
  }
  out.push(`기존 테스트를 지우거나 바꾼 곳 ${violations.length}건:`);
  for (const v of violations) out.push(`  - ${v.path}${v.line ? `:${v.line}` : ''}: ${v.detail}`);
  if (evidence.length) {
    out.push('옮긴 것으로 보이는 근거(옮겼어도 예외 표기가 필요합니다):');
    for (const e of evidence) out.push(`  - ${e}`);
  }
  const pr = readPr({ prJson: args['pr-json'], env });
  const reasons = findExceptionReasons(pr.body, pr.labels);
  if (reasons.length) {
    out.push(`예외 표기가 있어 통과합니다(${pr.source}):`);
    for (const r of reasons) out.push(`  - ${r}`);
    report(out, env);
    return 0;
  }
  out.push('');
  out.push('의도한 변경이면 PR 본문에 한 줄을 더하세요(본문을 고치면 CI가 다시 돕니다):');
  out.push(`  ${EXCEPTION_PREFIX}: <왜 바꿨는지>`);
  out.push(`또는 라벨 ${EXCEPTION_LABEL}을 붙입니다. 의도하지 않았다면 테스트를 되돌리세요.`);
  report(out, env);
  return 1;
}

function report(lines, env) {
  const text = lines.join('\n');
  console.log(text);
  if (env.GITHUB_STEP_SUMMARY) {
    try {
      appendFileSync(env.GITHUB_STEP_SUMMARY, `### test-guard\n\n\`\`\`\n${text}\n\`\`\`\n`);
    } catch {
      // 요약은 보조 출력이다.
    }
  }
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) {
  process.exitCode = main();
}
