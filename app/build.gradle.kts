import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
    alias(libs.plugins.roborazzi)
}

// 앱 버전은 여기 한 곳에서 정한다. 릴리스 태그는 `v<versionName>`이고 릴리스 워크플로가 둘이 같은지 본다.
// versionCode는 versionName에서 계산한다: major * 1_000_000 + minor * 1_000 + patch(예: 0.1.0 → 1000).
// 그래서 minor와 patch는 0~999만 쓴다. 자세한 규칙은 AGENTS.md "릴리스".
val appVersionName = "0.1.0"
val appVersionCode = appVersionName.split(".").let { parts ->
    require(parts.size == 3 && parts.all { it.matches(Regex("0|[1-9][0-9]*")) }) {
        "versionName은 MAJOR.MINOR.PATCH 꼴이어야 합니다: $appVersionName"
    }
    val (major, minor, patch) = parts.map { it.toInt() }
    require(minor <= 999 && patch <= 999 && major <= 2099) { "versionCode로 바꿀 수 없는 버전입니다: $appVersionName" }
    major * 1_000_000 + minor * 1_000 + patch
}

// 릴리스 서명 정보는 저장소에 두지 않는다(NF-11). 환경 변수나 Gradle 속성(-P, ~/.gradle/gradle.properties)으로만 받는다.
// 키스토어 경로가 없으면 서명 설정을 만들지 않는다. 그때 assembleRelease는 서명하지 않은 APK를 만들고
// 디버그 빌드와 CI 검사는 그대로 돈다. PKCS12 키스토어는 키 비밀번호가 키스토어 비밀번호와 같아서
// 키 비밀번호를 따로 주지 않으면 키스토어 비밀번호를 쓴다.
fun releaseSigningValue(name: String): String? =
    providers.environmentVariable(name).orElse(providers.gradleProperty(name)).orNull?.takeIf { it.isNotEmpty() }

val releaseStoreFile = releaseSigningValue("NEXTSET_RELEASE_STORE_FILE")
val releaseStorePassword = releaseSigningValue("NEXTSET_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseSigningValue("NEXTSET_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSigningValue("NEXTSET_RELEASE_KEY_PASSWORD") ?: releaseStorePassword

android {
    namespace = "io.github.zzunsik.nextset"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.zzunsik.nextset"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            if (releaseStorePassword == null || releaseKeyAlias == null) {
                throw GradleException(
                    "NEXTSET_RELEASE_STORE_FILE이 있으면 NEXTSET_RELEASE_STORE_PASSWORD와 NEXTSET_RELEASE_KEY_ALIAS도 있어야 합니다.",
                )
            }
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // 코드와 리소스를 줄인다(R8). 근거와 확인한 흐름은 docs/architecture.md "코드 축소(R8)".
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    testOptions {
        unitTests {
            // Robolectric이 앱 리소스(문구, 테마)를 읽을 수 있게 한다.
            isIncludeAndroidResources = true
        }
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        // MigrationTestHelper는 스키마 JSON을 앱의 assets에서 읽는다. 단위 테스트는 디버그 빌드의 병합된
        // assets를 읽으므로 디버그 소스 세트에만 스키마 폴더를 더한다. 릴리스 APK에는 들어가지 않는다
        // (docs/architecture.md "마이그레이션 테스트").
        getByName("debug").assets.directories.add("$projectDir/schemas")
    }
}

room3 {
    // 스키마 JSON을 내보내 커밋한다(docs/architecture.md "스키마 버전 관리").
    schemaDirectory("$projectDir/schemas")
}

roborazzi {
    // 기준 이미지는 저장소에 커밋한다. build/ 아래는 .gitignore 대상이라 따로 둔다.
    outputDir.set(file("src/test/screenshots"))
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.bundled)
    ksp(libs.androidx.room3.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.reorderable)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    // JVM 테스트에서 BundledSQLiteDriver의 네이티브 라이브러리를 올린다.
    testImplementation(libs.androidx.sqlite.bundled.jvm)
    testImplementation(libs.androidx.room3.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.accessibility.check)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
