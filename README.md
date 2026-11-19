# 다음 세트 (next-set)

운동을 기록하는 Android 앱입니다.

## 현재 상태

시작 화면만 있는 뼈대 단계입니다. 첫 버전에 넣을 것과 뺄 것은 [요구사항](docs/requirements.md)에 정리했습니다.

## 빌드

JDK 17 이상과 Android SDK(platform 37)가 필요합니다. Android Studio는 없어도 됩니다.

1. Android SDK 위치를 알려 줍니다. `ANDROID_HOME` 환경 변수를 쓰거나 저장소 루트에 `local.properties`를 만듭니다. 이 파일은 커밋하지 않습니다.

   ```properties
   sdk.dir=/path/to/Android/sdk
   ```

2. 디버그 APK를 빌드합니다.

   ```bash
   ./gradlew assembleDebug
   ```

   APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생깁니다.

3. 검사는 CI와 같은 명령으로 돌립니다.

   ```bash
   ./gradlew assembleDebug lint testDebugUnitTest
   ```

빌드 자체는 JDK 25로 돕니다. `./gradlew`를 실행한 JDK가 25가 아니면 Gradle이 설치된 JDK 25를 찾아 쓰고, 없으면 내려받습니다(`gradle/gradle-daemon-jvm.properties`).

## 빌드 설정

| 항목 | 값 | 근거 |
|---|---|---|
| Java | 25 | 현재 최신 LTS. 빌드 JDK와 바이트코드 대상을 같은 버전으로 맞춘다 |
| compileSdk, targetSdk | 37 | 최신 안정 플랫폼 |
| minSdk | 26 (Android 8.0) | 적응형 아이콘과 알림 채널을 기본으로 쓸 수 있는 가장 낮은 버전. 요구사항을 정할 때 다시 본다([#6](https://github.com/zzunsik/next-set/issues/6)) |
| 애플리케이션 ID | `io.github.zzunsik.nextset` | 소유한 도메인이 없어 GitHub 계정 기반 이름을 쓴다 |

AGP, Gradle, Kotlin, Compose는 작업 시점의 최신 안정 버전을 쓰고, 의존성 버전은 `gradle/libs.versions.toml`에서만 관리합니다.

쓰는 라이브러리는 다음과 같습니다. 고른 이유와 견준 후보는 [데이터 모델과 코드 구조](docs/architecture.md)에 있습니다.

| 용도 | 라이브러리 |
|---|---|
| 화면 | Jetpack Compose, Material 3 |
| 화면 이동 | Navigation 3 |
| 화면 상태 | Lifecycle(ViewModel) |
| 데이터베이스 | Room 3, `BundledSQLiteDriver`(`androidx.sqlite`). 주석 처리기는 KSP |
| 직렬화 | kotlinx.serialization(JSON) |
| 비동기 | kotlinx.coroutines |
| 의존성 주입 | 쓰지 않는다. `AppContainer`에서 직접 만든다 |
| 테스트 | JUnit 4, Robolectric, Compose UI 테스트, `room3-testing`, `kotlinx-coroutines-test`, Roborazzi(스크린샷 테스트와 접근성 검사) |

인터넷 권한을 요청하지 않습니다. 단위 테스트와 CI가 병합된 매니페스트를 검사합니다.

## 문서

- [요구사항](docs/requirements.md): 첫 버전에 넣을 것과 뺄 것, 확인이 남은 항목
- [데이터 모델과 코드 구조](docs/architecture.md): 패키지 구조, 라이브러리 선택, 저장 스키마, 백업 파일 형식, 테스트 전략
- [종목 데이터 조사](docs/exercise-data.md): 내장할 종목 데이터의 후보와 라이선스, 한국어 이름
- [내장 종목 목록](docs/exercise-list.md): 앱에 내장할 종목의 이름, 부위, 세부 부위, 장비, 기록 방식
- [디자인 레퍼런스](docs/design-references.md): 참고 앱의 세트 입력·직전 기록·휴식 타이머 화면과 정리한 원칙
- [디자인 계약](docs/DESIGN.md): 색·타이포·간격 값, 컴포넌트, 화면 구성과 흐름, 모션, 문구 규칙
- [휴식 타이머 조사](docs/rest-timer.md): 화면 밖에서 타이머와 신호를 돌리는 방법, 필요한 권한, Live Updates, Android 버전별 차이

## 작업 흐름

평소 작업은 `develop`에 모으고 릴리스할 때만 `main`으로 올립니다. 이슈, 브랜치, PR 규칙은 [AGENTS.md](AGENTS.md)에 있습니다.

## 참고한 자료

- [free-exercise-db](https://github.com/yuhonas/free-exercise-db): 내장 종목을 고르고 부위와 장비 분류를 대조하는 데 참고했습니다. 이름, 설명, 이미지는 가져오지 않았습니다.

## 라이선스

[MIT](LICENSE)

## 글꼴

화면 글꼴은 [Pretendard](https://github.com/orioncactus/pretendard)입니다. SIL Open Font License 1.1로 배포됩니다.

| 파일 | 출처 |
|---|---|
| `app/src/main/res/font/pretendard_regular.otf`, `pretendard_semibold.otf`, `pretendard_bold.otf` | [v1.3.9 릴리스](https://github.com/orioncactus/pretendard/releases/tag/v1.3.9)의 `Pretendard-1.3.9.zip` 안 `public/static/`의 `Pretendard-Regular.otf`, `Pretendard-SemiBold.otf`, `Pretendard-Bold.otf`. 이름만 바꿨고 내용은 그대로입니다 |
| `app/src/main/assets/licenses/pretendard.txt` | 저장소 커밋 [`0189074`](https://github.com/orioncactus/pretendard/blob/0189074fdad117a5e360aad00a59b47225429c31/LICENSE)의 `LICENSE`. 바탕 글꼴(Source, Inter, M PLUS 1)의 저작권 표시까지 담긴 판입니다. 고치지 않았습니다 |

앱의 설정 화면 "오픈소스 라이선스"에서 이 라이선스 전문을 보여 줍니다.

| 파일 | 출처 |
|---|---|
| `app/src/main/assets/licenses/apache-2.0.txt` | [Apache License 2.0 전문](https://www.apache.org/licenses/LICENSE-2.0.txt). 아이콘과 라이브러리의 고지에 함께 씁니다. 고치지 않았습니다 |
| `app/src/main/assets/licenses/index.json` | 화면에 보일 글꼴, 아이콘, 라이브러리 목록. 손으로 적고 테스트가 `app/build.gradle.kts`와 견줍니다 |

## 아이콘

아이콘은 [Material Symbols](https://github.com/google/material-design-icons)(Apache License 2.0)의 Rounded 모양을 벡터 XML로 넣습니다. 받은 파일에서 AppCompat 속성을 쓰는 `android:tint` 한 줄만 지웁니다. 색은 화면 코드에서 입힙니다.

| 파일 | 출처 |
|---|---|
| `app/src/main/res/drawable/ic_arrow_back.xml` | 커밋 [`10ecc14`](https://github.com/google/material-design-icons/blob/10ecc14790612ca8fd164e3f4ffe9c16fc50d5b7/symbols/android/arrow_back/materialsymbolsrounded/arrow_back_24px.xml)의 `symbols/android/arrow_back/materialsymbolsrounded/arrow_back_24px.xml` |
| `app/src/main/res/drawable/ic_backspace.xml` | 커밋 [`10ecc14`](https://github.com/google/material-design-icons/blob/10ecc14790612ca8fd164e3f4ffe9c16fc50d5b7/symbols/android/backspace/materialsymbolsrounded/backspace_24px.xml)의 `symbols/android/backspace/materialsymbolsrounded/backspace_24px.xml` |
| `app/src/main/res/drawable/ic_check.xml` | 커밋 [`10ecc14`](https://github.com/google/material-design-icons/blob/10ecc14790612ca8fd164e3f4ffe9c16fc50d5b7/symbols/android/check/materialsymbolsrounded/check_24px.xml)의 `symbols/android/check/materialsymbolsrounded/check_24px.xml` |
| `app/src/main/res/drawable/ic_keyboard_hide.xml` | 커밋 [`10ecc14`](https://github.com/google/material-design-icons/blob/10ecc14790612ca8fd164e3f4ffe9c16fc50d5b7/symbols/android/keyboard_hide/materialsymbolsrounded/keyboard_hide_24px.xml)의 `symbols/android/keyboard_hide/materialsymbolsrounded/keyboard_hide_24px.xml` |
