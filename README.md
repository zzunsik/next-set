# 다음 세트 (next-set)

운동을 기록하는 Android 앱입니다.

## 현재 상태

시작 화면만 있는 뼈대 단계입니다. 무엇을 만들지는 [이슈](https://github.com/zzunsik/next-set/issues)에서 정하고 있습니다.

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

## 문서

- [종목 데이터 조사](docs/exercise-data.md): 내장할 종목 데이터의 후보와 라이선스, 한국어 이름
- [디자인 레퍼런스](docs/design-references.md): 참고 앱의 세트 입력·직전 기록·휴식 타이머 화면과 정리한 원칙

## 작업 흐름

평소 작업은 `develop`에 모으고 릴리스할 때만 `main`으로 올립니다. 이슈, 브랜치, PR 규칙은 [AGENTS.md](AGENTS.md)에 있습니다.

## 라이선스

[MIT](LICENSE)
