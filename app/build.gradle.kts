import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

android {
    namespace = "io.github.zzunsik.nextset"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.zzunsik.nextset"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    // JVM 테스트에서 BundledSQLiteDriver의 네이티브 라이브러리를 올린다.
    testImplementation(libs.androidx.sqlite.bundled.jvm)
    testImplementation(libs.androidx.room3.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
