# 작업 규칙

이 저장소에서 사람과 코딩 에이전트가 함께 지키는 규칙입니다.

## 이슈부터 시작

- 모든 변경(기능·버그·리팩터링·테스트·빌드 설정·문서)은 GitHub 이슈부터 만든다. 같은 범위의 열린 이슈가 있으면 그것을 쓴다.
- 이슈에는 배경, 범위, 완료 기준, 선행 이슈를 적는다.
- 서로 독립된 작업은 이슈와 PR을 나눈다.
- 라벨은 `세팅`, `조사`, `구현` 중 하나를 붙인다.
- 요구사항이 정해지지 않은 기능은 구현 이슈로 만들지 않고 요구사항 이슈의 후보로 둔다.

## 브랜치

- `develop`: 평소 작업이 모이는 기본 브랜치다.
- `main`: 릴리스한 상태만 담는다. 릴리스할 때 `develop`에서 `main`으로 PR을 보내 머지 커밋으로 합치고, 그 커밋에 버전 태그(예: `v0.1.0`)를 단다.
- 작업 브랜치는 `git fetch`로 받은 최신 `develop`에서 갈라지고 `develop`으로 PR을 보낸다.
  - `feat/<이슈번호>-<요약>`: 기능
  - `fix/<이슈번호>-<요약>`: 버그
  - `chore/<이슈번호>-<요약>`: 빌드·설정·문서
  - `hotfix/<이슈번호>-<요약>`: `develop`을 거칠 수 없는 핫픽스만. `main`에서 갈라지고 `main`으로 PR을 보낸다("릴리스" > "브랜치와 태그").
  - 릴리스 전용 브랜치(`release/*`)는 만들지 않는다.
- 작업은 이슈마다 worktree를 따로 만들어 한다. 원본 클론에서는 브랜치를 바꾸지 않는다. 여러 세션이 클론 하나를 같이 쓰면 한쪽이 브랜치를 바꿀 때 다른 쪽의 작업 파일이 바뀐다.

  ```bash
  git fetch origin
  git worktree add --no-track -b chore/14-design ../next-set-worktrees/14-design origin/develop
  cd ../next-set-worktrees/14-design
  cp ../../next-set/local.properties .    # ANDROID_HOME을 쓰면 건너뛴다
  git push -u origin chore/14-design      # 첫 push
  ```

  - 위치는 저장소 폴더 옆의 `next-set-worktrees/<이슈번호>-<요약>`이다. 저장소 폴더 안에는 만들지 않는다.
  - `--no-track`을 붙인다. 빼면 새 브랜치의 upstream이 `origin/develop`이 되어 `git push`가 작업 브랜치를 올리지 않는다.
  - 새 worktree에는 `local.properties`가 없어서 빌드가 "SDK location not found"로 실패한다. `ANDROID_HOME` 환경 변수를 쓰거나 원본 클론에서 복사한다.
  - PR이 머지되면 원본 클론에서 `git worktree remove <경로>`로 지운다. 지우기 전에 push하지 않은 커밋과 커밋하지 않은 변경이 없는지 확인한다.
- 작업 브랜치에도 force push하지 않는다(`--force`, `-f`, `--force-with-lease` 모두). 훅을 건너뛰는 `--no-verify`도 쓰지 않는다. `develop`과 `main`은 GitHub이 막지만(아래 "저장소 설정") 작업 브랜치는 막지 않는다.
- 저장소의 현재 상태를 말할 때는 로컬 체크아웃이 아니라 `git fetch` 뒤의 `origin/develop`을 기준으로 본다.

## 커밋

- 형식: `<종류>: <한국어 요약> (#이슈번호)` 예) `feat: 휴식 타이머 (#12)`
- 종류: feat, fix, refactor, test, docs, chore
- 본문에는 무엇을 왜 바꿨는지 적는다.
- `git add -A`와 `git commit -a`를 쓰지 않는다. 바꾼 파일을 하나씩 지정해 올린다.
- `.gitignore`는 임의로 고치지 않는다. 고쳐야 하면 먼저 묻는다.

## PR

- 본문에 `Closes #이슈번호`, 변경 목록, 확인한 내용을 적는다.
- CI의 `ci-passed` 검사가 통과해야 머지한다.
- 작업 PR은 squash로 합치고 브랜치를 지운다. 릴리스 PR(`develop` → `main`)만 머지 커밋으로 합친다.
- `develop`보다 뒤처졌거나 충돌하면 `develop`을 합친 뒤 테스트를 다시 돌린다.

## 테스트와 CI

- 검사 명령: `./gradlew assembleDebug lint testDebugUnitTest` (CI와 같다)
- 버그 수정 PR은 먼저 실패하는 재현 테스트를 넣고 고친다. 고치기 전 코드에서 그 테스트가 실패하는 것을 확인하고 PR 본문에 적는다.
- 기능 PR은 영향받는 기존 테스트가 통과해야 한다. 동작을 일부러 바꿨으면 테스트도 같은 PR에서 고친다.
- 실패한 검사를 끄거나 기준을 낮춰 통과시키지 않는다. lint 예외를 늘리거나 테스트를 지워서 맞추지 않는다. 기준을 바꿔야 하면 PR 본문에 이유를 적고 승인을 받는다.
- 꺼 둔 테스트(`@Ignore`), 내용이 빈 테스트, `TODO`는 완료 근거가 아니다. 구현하거나 막힌 것으로 보고한다.
- CI의 `test-guard` job이 PR에서 기존 테스트를 지우거나 약하게 바꾼 곳을 찾는다(`scripts/test-guard.mjs`). base 브랜치와의 merge-base에 있던 테스트 소스(`app/src/test*`, `app/src/androidTest*`)를 기준으로 다음이 있으면 실패한다.
  - `@Test` 함수를 지우거나 이름을 바꿈, `@Test` 함수 안의 줄을 지우거나 바꿈(공백만 바뀐 줄은 빼고). 기존 테스트 안에 `return`, `try`/`catch`, `runCatching`을 끼워 넣음
  - 테스트 함수 밖(도우미 등)의 검증 줄을 지우거나 바꿈: `assert…`, `fail(`, `check(`, `require(`, `verify`, `should…`, Truth 체인(`.isEqualTo(` 등), Compose `onNode…`, `assertAccessible()`, `captureRoboImage`
  - 테스트 리소스(`app/src/test/resources/` 등, 소스가 아닌 파일)를 바꾸거나 지움
  - `@Ignore`, `@Disabled`, `assume…(`을 새로 더함(새 파일 포함)
  - 기준 스크린샷(`app/src/test/screenshots/`)은 보지 않는다. CI 비교와 리뷰로 다룬다. 새 테스트·새 파일·새 리소스를 더하거나 기존 테스트에 검증을 더하는 것은 통과한다.
  - 테스트를 다른 파일로 옮겨도 실패한다. 같은 이름의 테스트나 같은 검증 줄이 다른 곳에 생겼으면 로그에 근거로 보여 준다.
- 동작을 일부러 바꿔 기존 테스트를 고쳐야 하면 PR 본문에 한 줄을 적는다: `테스트 기대값 변경: <왜 바꿨는지>`. 줄 맨 앞(목록 기호 `- `는 허용)에 쓰고 사유를 비우지 않는다. 승인한 사람이 라벨 `test-change-approved`를 붙여도 통과한다. 사유는 `test-guard` 로그와 단계 요약에 남는다.
  - 본문 수정과 라벨 변경에도 CI가 다시 돈다. 이때 같은 커밋에서 끝난 `build` 결과가 있으면 그것을 다시 쓰고 `build`를 건너뛴다(`build-plan` job). 진행 중이던 run은 취소되므로 그때는 `build`를 다시 돌린다.
- CI에 job을 추가하면 `ci-passed`의 `needs`에 넣는다. 머지 판단은 이 검사 하나로 한다.
- CI는 `testDebugUnitTest`를 빌드 캐시 없이 매번 돌린다(`--rerun`). 입력이 같다고 판정돼 테스트가 `FROM-CACHE`나 `UP-TO-DATE`로 건너뛰어지면 `ci-passed`가 실제 검사 없이 통과하기 때문이다. 빌드와 lint는 캐시를 쓴다. 실행한 테스트 수는 CI 단계 요약의 "Test count"에 나오고, 0개면 실패한다. 로컬 검사 명령은 캐시를 그대로 쓴다.
- 화면 테스트는 Robolectric으로 JVM에서 돌린다. 테스트 SDK는 `app/src/test/resources/robolectric.properties`에서 정한다.
- 스크린샷 테스트는 Roborazzi로 찍는다. 기준 이미지는 `app/src/test/screenshots/`에 커밋한다.
  - 비교: 검사 명령의 `testDebugUnitTest`가 늘 기준 이미지와 비교한다(`gradle.properties`의 `roborazzi.test.verify=true`). 다르면 실패하고 `app/build/outputs/roborazzi/`에 실제 이미지(`_actual`)와 차이 이미지(`_compare`)가 생긴다. CI는 이 폴더를 `screenshot-diffs` 아티팩트로 올린다.
  - 다시 만들기: `./gradlew recordRoborazziDebug`. 화면을 일부러 바꾼 PR에서만 돌리고, 바뀐 이미지를 같은 커밋에 넣는다. 이미지를 열어 의도한 변화인지 본 뒤에 커밋한다.
  - 새 화면과 컴포넌트는 밝은 화면과 어두운 화면, 글자 크기 기본과 200%의 네 조합을 찍는다(`@Config(qualifiers = "+night", fontScale = 2.0f)`). `@GraphicsMode(NATIVE)`를 붙인다.
  - 지어낸 문구와 값만 찍는다. 실제 기록이나 다른 앱의 화면을 넣지 않는다.
- 접근성 검사는 화면 테스트에서 `assertAccessible()`(`ui/a11y/AccessibilityChecks.kt`)로 돌린다. 스크린샷 테스트는 찍기 전에 이 검사를 먼저 돌린다.
  - ATF 검사(빠진 라벨, 글자 대비 등)는 경고까지 실패로 본다. 누르는 요소는 배치 크기가 48dp 이상이어야 한다(NF-10).
  - 예외는 하나다. 비활성 요소(그 안의 글자 포함)의 글자 대비(TextContrastCheck)는 실패로 보지 않는다. `docs/DESIGN.md`와 WCAG 1.4.3이 비활성 UI 요소를 대비 기준에서 빼기 때문이다. 비활성 요소의 다른 검사(빠진 라벨 등)와 활성 요소의 대비는 그대로 실패다. `AccessibilityChecksTest`가 이 경계를 지킨다(승인: 소유자, 2026-11-20, #21).
  - 누르는 요소의 크기는 화면에 보이는 영역이 아니라 배치 크기로 잰다. 스크롤 밖에 걸친 요소도 제 크기로 잰다(승인: 소유자, 2026-11-20, #21).
  - Compose UI 테스트의 `enableAccessibilityChecks()`는 Robolectric에서 아무것도 검사하지 않는다. 이것으로 접근성을 확인했다고 보지 않는다.
  - 검사가 실패하면 화면을 고친다. 검사를 빼거나 억제 목록(suppressions)을 더하려면 PR 본문에 이유를 적고 승인을 받는다.

## 작업 범위

- 범위 밖 결함을 발견하면 그 PR에서 고치지 않는다. 새 이슈로 남기거나 PR 본문에 적어 알린다.
- 사용자가 지우거나 보류한 코드와 문장을 되살리지 않는다. 필요해 보이면 먼저 묻는다.
- 같은 값이 두 곳에 있으면 한쪽만 바꾸지 않는다. 함께 바꾸고, 둘이 어긋나면 실패하는 테스트나 검사를 둔다.

## 겪고 나서 추가하는 규칙

실제로 겪은 문제에서 나온 규칙만 이 절에 적는다. 추측으로 규칙을 늘리지 않는다. 항목마다 근거(이슈나 PR 번호)와 날짜를 붙인다.

아직 항목이 없다.

## 공개 저장소에서 지킬 것

코드, 이슈, PR, 커밋 메시지, CI 로그가 모두 공개됩니다.

- 서명 키, 토큰, SDK 경로 같은 비밀 정보는 `local.properties`나 저장소 Secrets에만 둔다. `local.properties`, `*.jks`, `*.keystore`는 커밋하지 않는다.
- 테스트 예시 데이터는 지어낸 값만 쓴다. 실제 사람의 운동·신체 기록을 넣지 않는다.
- 이슈·PR·커밋 메시지에 개인 정보나 다른 조직의 내부 정보를 적지 않는다. 화면 캡처도 같다.
- 외부 코드·데이터·글꼴·이미지를 넣을 때는 라이선스를 확인하고 출처를 적는다.
- 워크플로는 필요한 권한만 선언하고(`permissions`), 외부 액션은 커밋 SHA로 고정한다. 포크에서 온 PR에 시크릿이 노출되는 `pull_request_target`은 쓰지 않는다.

## 릴리스

v1은 GitHub Releases에 서명한 APK를 올려 배포한다(REL-01). 태그를 올리면 CI(`.github/workflows/release.yml`)가 빌드하고 서명하고 올린다. 손으로 빌드하거나 서명하지 않는다. 아래 규칙은 저장소 소유자가 정했다(#30, 2026-11-21. zzunsik/sun-seat#118과 같은 규칙).

### 브랜치와 태그

- 작업은 `develop`에 모으고, 릴리스할 때만 `develop` → `main` PR을 머지 커밋으로 합친다.
- 릴리스 전용 브랜치(`release/*`)는 만들지 않는다. **`main`의 머지 커밋에 단 태그**로 버전을 관리한다.
- 태그 `v*`는 ruleset `protect-release-tags`로 이동·삭제를 막는다(우회 없음, #81). 시험용 태그는 `v`로 시작하지 않는 이름을 쓴다.
- 핫픽스: 기본은 `develop`에서 고쳐 다음 끝번호로 릴리스한다. `develop`에 내보내면 안 되는 변경이 쌓여 있을 때만 `main`에서 `hotfix/<이슈번호>-<요약>`을 만들어 `main`으로 PR → 태그 → 같은 수정을 `develop`에도 PR로 반영한다.

### 절차

1. `develop`에서 `app/build.gradle.kts`의 `appVersionName`을 이번 버전으로 올리는 PR을 보내 합친다(아래 "버전").
2. `develop`에서 `main`으로 릴리스 PR을 보내 머지 커밋으로 합친다. `ci-passed`가 통과해야 한다.
3. 그 머지 커밋에 태그를 달아 올린다. 태그는 `v` + `appVersionName`이다. 머지하면 `main` push로 CI가 다시 도는데, 그 머지 커밋의 `ci-passed`가 성공해야 릴리스가 만들어진다(아래 "워크플로가 하는 일").

   ```bash
   git fetch origin
   git tag -a v0.1.0 origin/main -m "v0.1.0"   # origin/main이 방금 합친 릴리스 머지 커밋인지 git log로 먼저 본다
   git push origin v0.1.0
   ```

4. Actions의 Release 실행이 끝나면 Releases에 APK와 체크섬이 올라와 있는지 본다. 시험판이면 Pre-release로 표시돼 있는지도 본다.

### 워크플로가 하는 일

- `v*` 태그 push에서만 돈다. PR에서는 돌지 않으므로 포크에서 온 PR은 서명 시크릿에 닿지 않는다. 액션은 커밋 SHA로 고정한다.
- job이 둘이다. 워크플로 기본 권한은 없음(`permissions: {}`)이고 job마다 필요한 것만 준다.
  - `verify`(`contents: read`, `checks: read`): 빌드 전에 태그를 확인한다. 다음 중 하나라도 어긋나면 실패하고 `release`는 돌지 않는다.
    - 태그가 `vX.Y.Z` 또는 `vX.Y.Z-rc.N` 꼴이고 아래 "버전" 규칙(중간·끝 99까지)에 맞는다(`scripts/release-version.mjs`).
    - 태그의 커밋이 `main`에 있다.
    - 그 커밋의 `ci-passed` 검사(GitHub Actions가 만든 것, 여럿이면 가장 나중 것)가 `success`다. 커밋 check run API(`repos/{repo}/commits/{sha}/check-runs?check_name=ci-passed`)로 본다. 머지 직후 태그를 달면 CI가 아직 돌고 있을 수 있어서 끝날 때까지 최대 40분 기다린다. 실패·취소로 끝났으면 바로 실패하고, 40분 안에 끝나지 않거나 검사가 없으면 그때 실패한다. 이때는 `main`의 CI가 성공한 뒤 Release 실행을 다시 돌린다(Re-run jobs).
  - `release`(`contents: write`): Release를 만드는 job에만 쓰기 권한을 준다.
- 서명 시크릿이 없으면 무엇이 빠졌는지 적고 바로 실패한다.
- 키스토어를 시크릿에서 풀어 작업 폴더 밖(`RUNNER_TEMP`)에 두고, 환경 변수로 Gradle `signingConfigs`에 넘겨 `assembleRelease`로 서명한다. 비밀번호를 출력하지 않고, 설정 캐시와 Gradle 캐시를 쓰지 않으며, 마지막에 키스토어를 지운다(실패해도 지운다). 아티팩트는 올리지 않는다.
- 릴리스 매니페스트와 APK에 `android.permission.INTERNET`이 없는지 보고, 만든 APK를 `aapt2 dump badging`으로 읽어 `versionName`이 태그에서 `v`를 뺀 값인지, `versionCode`가 아래 규칙으로 계산한 값인지 본다. 서명은 `apksigner verify --print-certs`로 확인한다.
- APK(`next-set-<태그>.apk`)와 SHA-256 체크섬(`next-set-<태그>.apk.sha256`)을 올려 Release를 만든다. 시험판(`-rc.N`)은 Pre-release로 표시한다(`--prerelease`). 릴리스 노트는 GitHub이 앞 태그 뒤에 합친 PR 목록으로 만든다(`--generate-notes`). 고칠 것이 있으면 Release 화면에서 손으로 고친다.
- 시크릿이 빠졌거나 일시적인 오류로 실패했으면 고친 뒤 그 실행을 다시 돌린다(Re-run jobs). 코드를 고쳐야 하면 태그를 옮기지 말고 고친 뒤 다음 버전으로 새 태그를 단다. 실패한 태그는 Release 없이 남는다.
- PR CI(`ci.yml`의 "Release version rule")도 서명 없이 만든 릴리스 APK의 버전이 규칙에 맞는지 보고 `scripts/release-version.test.mjs`를 돌린다. 버전 계산은 `app/build.gradle.kts`와 이 스크립트 두 곳에 있어서, 둘이 어긋나면 PR에서 실패한다.

### 버전

SemVer `v주.중간.끝`(`vMAJOR.MINOR.PATCH`)을 쓴다.

- 끝번호: 버그 수정만 있는 릴리스(`0.1.0` → `0.1.1`).
- 중간번호: 기능·화면 변경이 하나라도 있는 릴리스(`0.1.1` → `0.2.0`, 끝번호는 0으로).
- 앞번호: 완성판 선언이나 저장 데이터·설정 비호환 변경(`1.0.0`). 당분간은 `0.x`로 운영한다.
- 각 자리는 독립된 정수다. `0.9.0` 다음은 `0.10.0`이고 `0.10.0` > `0.1.0`이다. 앞자리에 0을 붙이지 않는다(`0.01.0`은 틀림).
- 시험판이 필요하면 `v0.2.0-rc.1` 형식을 쓰고 Releases에서 Pre-release로 표시한다(워크플로가 한다). `N`은 1부터 센다. 다른 꼴(`-beta`, `-rc` 등)은 워크플로가 거부한다.
- `versionName`은 `app/build.gradle.kts`의 `appVersionName` 한 곳에서 정하고, 태그에서 `v`를 뺀 값과 같아야 한다. 시험판이면 `appVersionName`도 `0.2.0-rc.1`처럼 쓴다.
- `versionCode = 주 × 10000 + 중간 × 100 + 끝`(예: `v0.1.0` → 100, `v0.2.1` → 201, `v1.0.0` → 10000). `appVersionName`에서 계산하므로 따로 고치지 않는다. 중간·끝은 각각 99까지이고, 넘으면 빌드가 실패한다.
- 시험판은 `-rc.N`을 뗀 버전과 같은 `versionCode`를 쓴다(`v0.2.0-rc.1`, `v0.2.0-rc.2`, `v0.2.0` 모두 200). 규칙의 계산식이 주·중간·끝만 쓰고 중간·끝 자리에 남는 칸이 없기 때문이다. Android는 `versionCode`가 같으면 업데이트로 설치하므로 시험판 위에 정식판을 그대로 올릴 수 있다. 반대로 정식판 `0.2.0`을 낸 뒤 `0.2.0-rc.N`을 다시 내지 않는다.
- 버전을 낮추지 않는다. Android는 `versionCode`가 작은 APK로 업데이트하지 않는다.
- 릴리스 워크플로는 태그와 `versionName`·`versionCode`가 이 규칙과 다르면 실패한다.

### 첫 릴리스

- `v0.1.0`. 마일스톤 `v0.1.0`의 이슈를 모두 끝낸 뒤 진행한다. `versionName`은 0.1.0이고 `versionCode`는 100이다(#68의 계산식으로는 1000이었다).
- 기기에 있는 디버그 서명 앱은 첫 서명 APK를 설치할 때 한 번 지워야 한다. 서명 키가 다르고(디버그 빌드의 `versionCode`가 #68 계산식의 1000일 수도 있다) 업데이트로 설치되지 않는다. 지우면 기록도 지워지므로 먼저 설정 > 백업에서 백업 파일을 내보내고, 새로 설치한 뒤 가져온다.

### 서명 키와 시크릿

- 릴리스 키스토어는 저장소 소유자가 만들고 보관한다. 저장소, 이슈, PR, 로그, 아티팩트에 키스토어와 비밀번호를 올리지 않는다(NF-11). 시크릿 등록은 소유자만 한다.
- 워크플로가 읽는 시크릿은 셋이다.

  | 시크릿 | 값 |
  |---|---|
  | `RELEASE_KEYSTORE_BASE64` | PKCS12 키스토어 파일을 base64 한 줄로 바꾼 값 |
  | `RELEASE_KEYSTORE_PASSWORD` | 키스토어 비밀번호. PKCS12라 키 비밀번호도 같다 |
  | `RELEASE_KEY_ALIAS` | 키 별칭 |

- 지금은 저장소 시크릿(Settings > Secrets and variables > Actions > Repository secrets)이다. sun-seat처럼 GitHub 환경 `release`(배포 규칙 `v*` 태그)로 옮길지는 소유자 판단으로 남아 있다(#30). 옮기려면 소유자가 같은 이름으로 `gh secret set --env release`로 다시 넣은 뒤 `release.yml`의 `release` job에 `environment: release`를 단다. 환경에 시크릿을 넣기 전에 달면 시크릿을 읽지 못해 릴리스가 실패한다.
- 로컬에서 서명한 릴리스 APK를 만들 때는 환경 변수 `NEXTSET_RELEASE_STORE_FILE`, `NEXTSET_RELEASE_STORE_PASSWORD`, `NEXTSET_RELEASE_KEY_ALIAS`(키 비밀번호가 다르면 `NEXTSET_RELEASE_KEY_PASSWORD`)를 주거나 같은 이름의 Gradle 속성을 `~/.gradle/gradle.properties`에 둔다. 저장소 안의 `gradle.properties`에는 넣지 않는다. 키스토어 경로가 없으면 서명하지 않은 APK가 나오고 디버그 빌드와 CI 검사는 영향을 받지 않는다.
- **키를 잃으면 되돌릴 수 없다.** Android는 처음 설치한 APK와 같은 키로 서명한 APK만 업데이트로 받는다. 첫 APK를 배포한 뒤 키를 잃거나 바꾸면 기존 사용자는 앱을 지우고(기록도 함께 지워진다) 다시 설치해야 한다. 키가 새면 다른 사람이 업데이트로 위장한 APK를 만들 수 있다. 키스토어와 비밀번호는 저장소 밖 두 곳 이상(예: 비밀번호 관리자와 오프라인 저장 장치)에 나눠 보관한다. Android 개발자 인증(`docs/requirements.md` "Android 개발자 인증(11번)")에도 이 키의 인증서 지문을 등록하므로 키를 잃으면 등록도 할 수 없다.
- 릴리스 태그는 옮기거나 지울 수 없다(ruleset `protect-release-tags`, "저장소 설정"). 잘못 단 태그는 고치지 말고 다음 버전으로 새 태그를 단다. 워크플로나 설정을 시험할 때는 `v`로 시작하지 않는 태그 이름을 쓴다.

## 저장소 설정

GitHub 설정으로 강제하는 것들입니다. 설정을 바꾸면 이 절도 함께 고칩니다.

- `develop`과 `main`은 ruleset(`protect-develop`, `protect-main`)으로 보호한다.
  - PR로만 올릴 수 있다. 직접 push, force push, 브랜치 삭제는 거부된다.
  - `ci-passed`가 PR의 현재 커밋에서 성공해야 머지된다.
  - `develop` 대상 PR은 `develop`의 최신 커밋을 포함해야 하고 squash로만 합쳐진다.
  - `main` 대상 PR은 머지 커밋으로만 합쳐진다. 릴리스 머지 커밋은 `develop`에 없으므로 최신 반영 조건은 두지 않는다.
  - 우회 목록이 비어 있어 저장소 소유자도 같은 규칙을 따른다.
- `v*` 태그는 ruleset `protect-release-tags`로 보호한다. 태그를 옮기거나(update) 지우는(deletion) push는 거부된다. 새 태그를 다는 것은 막지 않는다. 우회 목록이 비어 있다(#81).
- 머지된 작업 브랜치는 자동으로 지워진다.
- 시크릿 스캐닝과 푸시 차단이 켜져 있다.
- Dependabot이 보안 알림과 보안 업데이트 PR을 보내고, 주 1회 의존성 업데이트 PR을 보낸다(`.github/dependabot.yml`). Dependabot PR은 이슈 없이 받고 다른 PR과 같은 검사를 거친다.
- 워크플로의 기본 토큰 권한은 읽기 전용이다.

## 빌드 환경

- `./gradlew`는 JDK 17 이상에서 실행한다. 빌드 자체는 JDK 25로 돌고(`gradle/gradle-daemon-jvm.properties`), 없으면 Gradle이 내려받는다.
- Android SDK platform 37이 필요하다. Android Studio 없이 명령줄로 빌드한다.
- 의존성 버전은 `gradle/libs.versions.toml`에서만 관리하고, 작업 시점의 최신 안정 버전을 쓴다.
- 라이브러리와 패키지 구조는 `docs/architecture.md`가 정한다. 그 문서에 없는 라이브러리를 더하려면 문서를 먼저 고친다.
  - 주석 처리기는 KSP만 쓴다. AGP 9의 내장 Kotlin에서는 `kotlin-kapt`를 쓸 수 없다.
  - 의존성 주입 라이브러리를 쓰지 않는다. 객체는 `AppContainer`에서 만들어 넘긴다.
  - 인터넷 권한을 끌어오는 라이브러리를 넣지 않는다(DATA-02). `ManifestPermissionTest`가 디버그 매니페스트를, CI의 "Release manifest has no INTERNET permission" 단계가 릴리스 매니페스트를 본다.
- 계층 사이 의존 방향(`ui → domain ← data`)은 `LayerDependencyTest`가 import를 읽어 검사한다. `domain`은 `android.`, `androidx.`, `ui`, `data`를, `ui`는 `data`를 import하지 않는다.
- Room 스키마 JSON은 빌드할 때 `app/schemas/`에 나온다. 엔티티를 바꿨으면 바뀐 JSON을 같은 커밋에 넣는다.
- 데이터베이스 드라이버는 `BundledSQLiteDriver`다. JVM 테스트에는 `sqlite-bundled-jvm`이 들어 있어야 한다. 없으면 `UnsatisfiedLinkError: no sqliteJni`로 실패한다.
