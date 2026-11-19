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

## 저장소 설정

GitHub 설정으로 강제하는 것들입니다. 설정을 바꾸면 이 절도 함께 고칩니다.

- `develop`과 `main`은 ruleset(`protect-develop`, `protect-main`)으로 보호한다.
  - PR로만 올릴 수 있다. 직접 push, force push, 브랜치 삭제는 거부된다.
  - `ci-passed`가 PR의 현재 커밋에서 성공해야 머지된다.
  - `develop` 대상 PR은 `develop`의 최신 커밋을 포함해야 하고 squash로만 합쳐진다.
  - `main` 대상 PR은 머지 커밋으로만 합쳐진다. 릴리스 머지 커밋은 `develop`에 없으므로 최신 반영 조건은 두지 않는다.
  - 우회 목록이 비어 있어 저장소 소유자도 같은 규칙을 따른다.
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
