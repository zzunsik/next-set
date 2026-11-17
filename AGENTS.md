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
- 작업 브랜치는 `develop`에서 갈라지고 `develop`으로 PR을 보낸다.
  - `feat/<이슈번호>-<요약>`: 기능
  - `fix/<이슈번호>-<요약>`: 버그
  - `chore/<이슈번호>-<요약>`: 빌드·설정·문서
- `develop`과 `main`에 직접 커밋하거나 force push하지 않는다.

## 커밋

- 형식: `<종류>: <한국어 요약> (#이슈번호)` 예) `feat: 휴식 타이머 (#12)`
- 종류: feat, fix, refactor, test, docs, chore
- 본문에는 무엇을 왜 바꿨는지 적는다.

## PR

- 본문에 `Closes #이슈번호`, 변경 목록, 확인한 내용을 적는다.
- CI의 `ci-passed` 검사가 통과해야 머지한다.
- 작업 PR은 squash로 합치고 브랜치를 지운다. 릴리스 PR(`develop` → `main`)만 머지 커밋으로 합친다.
- `develop`과 충돌하면 `develop`을 합친 뒤 테스트를 다시 돌린다.

## 테스트와 CI

- 검사 명령: `./gradlew assembleDebug lint testDebugUnitTest` (CI와 같다)
- 버그 수정 PR은 먼저 실패하는 재현 테스트를 넣고 고친다. 고치기 전 코드에서 그 테스트가 실패하는 것을 확인하고 PR 본문에 적는다.
- 기능 PR은 영향받는 기존 테스트가 통과해야 한다. 동작을 일부러 바꿨으면 테스트도 같은 PR에서 고친다.
- CI에 job을 추가하면 `ci-passed`의 `needs`에 넣는다. 머지 판단은 이 검사 하나로 한다.
- 화면 테스트는 Robolectric으로 JVM에서 돌린다. 테스트 SDK는 `app/src/test/resources/robolectric.properties`에서 정한다.

## 공개 저장소에서 지킬 것

코드, 이슈, PR, 커밋 메시지, CI 로그가 모두 공개됩니다.

- 서명 키, 토큰, SDK 경로 같은 비밀 정보는 `local.properties`나 저장소 Secrets에만 둔다. `local.properties`, `*.jks`, `*.keystore`는 커밋하지 않는다.
- 테스트 예시 데이터는 지어낸 값만 쓴다. 실제 사람의 운동·신체 기록을 넣지 않는다.
- 이슈·PR·커밋 메시지에 개인 정보나 다른 조직의 내부 정보를 적지 않는다. 화면 캡처도 같다.
- 외부 코드·데이터·글꼴·이미지를 넣을 때는 라이선스를 확인하고 출처를 적는다.
- 워크플로는 필요한 권한만 선언하고(`permissions`), 외부 액션은 커밋 SHA로 고정한다. 포크에서 온 PR에 시크릿이 노출되는 `pull_request_target`은 쓰지 않는다.

## 빌드 환경

- `./gradlew`는 JDK 17 이상에서 실행한다. 빌드 자체는 JDK 25로 돌고(`gradle/gradle-daemon-jvm.properties`), 없으면 Gradle이 내려받는다.
- Android SDK platform 37이 필요하다. Android Studio 없이 명령줄로 빌드한다.
- 의존성 버전은 `gradle/libs.versions.toml`에서만 관리하고, 작업 시점의 최신 안정 버전을 쓴다.
