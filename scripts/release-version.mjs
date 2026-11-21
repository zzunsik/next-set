#!/usr/bin/env node
// 릴리스 버전 규칙(AGENTS.md "릴리스" > "버전", #30)을 태그와 APK에 적용한다.
//
//   versionName = 태그에서 v를 뺀 값. MAJOR.MINOR.PATCH 또는 MAJOR.MINOR.PATCH-rc.N(시험판)
//   versionCode = MAJOR * 10000 + MINOR * 100 + PATCH. MINOR와 PATCH는 0~99.
//   시험판(-rc.N)은 -rc.N을 뗀 버전과 같은 versionCode를 쓴다.
//
// 같은 계산이 app/build.gradle.kts에도 있다. CI가 빌드한 릴리스 APK를 이 스크립트로 확인해 둘이 어긋나면 실패한다.
//
// 사용:
//   node scripts/release-version.mjs tag <태그>
//     태그를 검사하고 version-name=, version-code=, prerelease=를 한 줄씩 출력한다($GITHUB_OUTPUT에 붙여 쓴다).
//   node scripts/release-version.mjs apk <aapt2 dump badging 출력 파일> [--tag <태그>]
//     APK의 versionName이 규칙에 맞고 versionCode가 계산값과 같은지 본다. --tag가 있으면 태그와도 견준다.
// 종료 코드: 0 통과, 1 규칙 위반, 2 사용법 오류.
//
// Node 내장 모듈만 쓴다. 자체 테스트는 scripts/release-version.test.mjs다.

import { readFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

const NUM = '(0|[1-9][0-9]*)';
const VERSION_RE = new RegExp(`^${NUM}\\.${NUM}\\.${NUM}(?:-rc\\.([1-9][0-9]*))?$`);
// Android가 받는 versionCode의 최댓값.
const MAX_VERSION_CODE = 2_100_000_000;

export class VersionError extends Error {}

// versionName을 읽는다. 규칙에 맞지 않으면 VersionError를 던진다.
export function parseVersionName(name) {
  const m = VERSION_RE.exec(name);
  if (!m) {
    throw new VersionError(`버전은 MAJOR.MINOR.PATCH 또는 MAJOR.MINOR.PATCH-rc.N 꼴이어야 합니다(예: 0.1.0, 0.2.0-rc.1): ${name}`);
  }
  const [major, minor, patch] = [m[1], m[2], m[3]].map(Number);
  if (minor > 99 || patch > 99) {
    throw new VersionError(`MINOR와 PATCH는 0~99만 씁니다: ${name}`);
  }
  const versionCode = major * 10000 + minor * 100 + patch;
  if (!Number.isSafeInteger(versionCode) || versionCode > MAX_VERSION_CODE) {
    throw new VersionError(`versionCode가 Android 최댓값(${MAX_VERSION_CODE})을 넘습니다: ${name}`);
  }
  const rc = m[4] === undefined ? null : Number(m[4]);
  return { major, minor, patch, rc, prerelease: rc !== null, versionCode };
}

// 태그(v + versionName)를 읽는다.
export function parseTag(tag) {
  if (typeof tag !== 'string' || !tag.startsWith('v')) {
    throw new VersionError(`태그는 v로 시작해야 합니다(예: v0.1.0, v0.2.0-rc.1): ${tag}`);
  }
  const versionName = tag.slice(1);
  return { versionName, ...parseVersionName(versionName) };
}

// aapt2 dump badging 출력의 package: 줄에서 versionName과 versionCode를 꺼낸다.
export function readBadging(text) {
  const line = text.split(/\r?\n/).find((l) => l.startsWith('package:'));
  if (!line) throw new VersionError('badging 출력에 package: 줄이 없습니다.');
  const name = /\bversionName='([^']*)'/.exec(line);
  const code = /\bversionCode='([^']*)'/.exec(line);
  if (!name || !code) throw new VersionError(`package: 줄에 versionName이나 versionCode가 없습니다: ${line}`);
  return { versionName: name[1], versionCode: Number(code[1]) };
}

// APK의 버전이 규칙(그리고 tag가 있으면 태그)과 맞는지 본다. 틀린 것을 문장 목록으로 돌려준다.
export function checkApk(badging, tag) {
  const errors = [];
  const apk = readBadging(badging);
  let expected;
  try {
    expected = tag === undefined ? parseVersionName(apk.versionName) : parseTag(tag);
  } catch (e) {
    if (e instanceof VersionError) return [e.message];
    throw e;
  }
  if (tag !== undefined && apk.versionName !== expected.versionName) {
    errors.push(`APK의 versionName(${apk.versionName})이 태그(${tag})와 다릅니다. app/build.gradle.kts의 appVersionName을 고치세요.`);
  }
  if (apk.versionCode !== expected.versionCode) {
    errors.push(`APK의 versionCode(${apk.versionCode})가 규칙으로 계산한 값(${expected.versionCode})과 다릅니다. app/build.gradle.kts의 계산식을 확인하세요.`);
  }
  return errors;
}

function main(argv) {
  const [command, arg, ...rest] = argv;
  try {
    if (command === 'tag' && arg && rest.length === 0) {
      const v = parseTag(arg);
      console.log(`version-name=${v.versionName}`);
      console.log(`version-code=${v.versionCode}`);
      console.log(`prerelease=${v.prerelease}`);
      return 0;
    }
    if (command === 'apk' && arg && (rest.length === 0 || (rest.length === 2 && rest[0] === '--tag'))) {
      const text = readFileSync(arg, 'utf8');
      const errors = checkApk(text, rest[1]);
      const apk = readBadging(text);
      if (errors.length) {
        for (const e of errors) console.log(`::error::${e}`);
        return 1;
      }
      console.log(`APK 버전 확인: versionName=${apk.versionName} versionCode=${apk.versionCode}`);
      return 0;
    }
  } catch (e) {
    if (e instanceof VersionError) {
      console.log(`::error::${e.message}`);
      return 1;
    }
    throw e;
  }
  console.error('사용: node scripts/release-version.mjs tag <태그> | apk <badging 파일> [--tag <태그>]');
  return 2;
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) {
  process.exitCode = main(process.argv.slice(2));
}
