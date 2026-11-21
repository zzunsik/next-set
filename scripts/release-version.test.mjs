// scripts/release-version.mjs 자체 테스트. node --test scripts/release-version.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { checkApk, parseTag, parseVersionName, readBadging, VersionError } from './release-version.mjs';

test('versionCode는 MAJOR*10000 + MINOR*100 + PATCH다', () => {
  assert.equal(parseTag('v0.1.0').versionCode, 100);
  assert.equal(parseTag('v0.1.1').versionCode, 101);
  assert.equal(parseTag('v0.2.1').versionCode, 201);
  assert.equal(parseTag('v0.10.0').versionCode, 1000);
  assert.equal(parseTag('v0.99.99').versionCode, 9999);
  assert.equal(parseTag('v1.0.0').versionCode, 10000);
  assert.equal(parseTag('v12.3.4').versionCode, 120304);
});

test('각 자리는 독립된 정수라 0.10.0이 0.9.0보다 크다', () => {
  assert.ok(parseTag('v0.10.0').versionCode > parseTag('v0.9.0').versionCode);
  assert.ok(parseTag('v1.0.0').versionCode > parseTag('v0.99.99').versionCode);
});

test('versionName은 태그에서 v를 뺀 값이다', () => {
  assert.equal(parseTag('v0.1.0').versionName, '0.1.0');
  assert.equal(parseTag('v0.2.0-rc.1').versionName, '0.2.0-rc.1');
});

test('시험판 -rc.N은 Pre-release이고 정식판과 versionCode가 같다', () => {
  const rc = parseTag('v0.2.0-rc.1');
  assert.equal(rc.prerelease, true);
  assert.equal(rc.rc, 1);
  assert.equal(rc.versionCode, 200);
  assert.equal(parseTag('v0.2.0-rc.12').versionCode, 200);
  assert.equal(parseTag('v0.2.0').prerelease, false);
  assert.equal(parseTag('v0.2.0').rc, null);
});

test('MINOR나 PATCH가 99를 넘으면 거부한다', () => {
  assert.throws(() => parseTag('v0.100.0'), VersionError);
  assert.throws(() => parseTag('v0.1.100'), VersionError);
  assert.throws(() => parseVersionName('1.100.0-rc.1'), VersionError);
});

test('규칙에 맞지 않는 태그를 거부한다', () => {
  for (const tag of [
    '0.1.0', 'v0.1', 'v0.1.0.0', 'v01.1.0', 'v0.01.0', 'v0.1.00', 'v0.1.0-rc', 'v0.1.0-rc.0', 'v0.1.0-rc.01',
    'v0.1.0-beta.1', 'v0.1.0-RC.1', 'v0.1.0-rc.1+build', 'v 0.1.0', 'v0.1.0\n', 'vtest', 'V0.1.0', '',
  ]) {
    assert.throws(() => parseTag(tag), VersionError, `거부해야 함: ${JSON.stringify(tag)}`);
  }
});

test('Android 최댓값을 넘는 versionCode는 거부한다', () => {
  assert.equal(parseTag('v210000.0.0').versionCode, 2_100_000_000);
  assert.throws(() => parseTag('v210000.0.1'), VersionError);
});

const badging = (name, code) =>
  `package: name='io.github.zzunsik.nextset' versionCode='${code}' versionName='${name}' platformBuildVersionName='17'\n` +
  "uses-permission: name='android.permission.POST_NOTIFICATIONS'\n";

test('badging에서 versionName과 versionCode를 읽는다', () => {
  assert.deepEqual(readBadging(badging('0.1.0', 100)), { versionName: '0.1.0', versionCode: 100 });
  assert.throws(() => readBadging('application: label=x'), VersionError);
});

test('APK 버전이 태그와 규칙에 맞으면 통과한다', () => {
  assert.deepEqual(checkApk(badging('0.1.0', 100), 'v0.1.0'), []);
  assert.deepEqual(checkApk(badging('0.2.0-rc.1', 200), 'v0.2.0-rc.1'), []);
  assert.deepEqual(checkApk(badging('0.1.0', 100)), []);
});

test('APK versionName이 태그와 다르면 실패한다', () => {
  const errors = checkApk(badging('0.1.0', 100), 'v0.1.1');
  assert.equal(errors.length, 2);
  assert.match(errors[0], /versionName\(0\.1\.0\)이 태그\(v0\.1\.1\)와 다릅니다/);
});

test('APK versionCode가 계산값과 다르면 실패한다(#68의 0.1.0 → 1000)', () => {
  const errors = checkApk(badging('0.1.0', 1000), 'v0.1.0');
  assert.equal(errors.length, 1);
  assert.match(errors[0], /versionCode\(1000\)가 규칙으로 계산한 값\(100\)과 다릅니다/);
  assert.equal(checkApk(badging('0.1.0', 1000)).length, 1);
});

test('태그나 APK versionName이 규칙에 맞지 않으면 실패한다', () => {
  assert.equal(checkApk(badging('0.1.0', 100), 'v0.1.0-beta.1').length, 1);
  assert.equal(checkApk(badging('0.1', 100)).length, 1);
});
