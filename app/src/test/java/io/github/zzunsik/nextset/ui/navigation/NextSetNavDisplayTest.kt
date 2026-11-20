package io.github.zzunsik.nextset.ui.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.exercise.FakeExerciseRepository
import io.github.zzunsik.nextset.ui.routine.FakeRoutineRepository
import io.github.zzunsik.nextset.ui.settings.FakeBackupRepository
import io.github.zzunsik.nextset.ui.settings.FakeLicenseRepository
import io.github.zzunsik.nextset.ui.settings.FakeSettingsRepository
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import io.github.zzunsik.nextset.data.repository.TestDbRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NextSetNavDisplayTest {
    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val opened = mutableListOf<String>()
    private var browserMissing = false

    /** 메모리 데이터베이스. Compose 규칙보다 바깥이라 화면이 사라진 뒤에 닫힌다. */
    @get:Rule(order = 0)
    internal val dbRule = TestDbRule(seed = false)

    private val db get() = dbRule.db

    private val uriHandler = object : UriHandler {
        override fun openUri(uri: String) {
            // AndroidUriHandler는 주소를 열 앱이 없으면 IllegalArgumentException을 던진다.
            if (browserMissing) throw IllegalArgumentException("no activity")
            opened += uri
        }
    }

    private fun show() {
        compose.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                NextSetTheme {
                    NextSetNavDisplay(
                        settingsRepository = FakeSettingsRepository(),
                        licenseRepository = FakeLicenseRepository(),
                        exerciseRepository = FakeExerciseRepository(),
                        workoutRepository = db.workouts,
                        routineRepository = FakeRoutineRepository(),
                        backupRepository = FakeBackupRepository(),
                    )
                }
            }
        }
    }

    private fun openSettings() {
        // 하단 내비게이션의 "설정" 탭(#23).
        compose.onNodeWithText(app.getString(R.string.nav_settings)).performClick()
        compose.onNodeWithText(app.getString(R.string.settings_default_rest)).assertIsDisplayed()
    }

    @Test
    fun startsAtHome() {
        show()
        // 루틴이 없는 홈의 빈 상태(#23).
        compose.onNodeWithText(app.getString(R.string.home_empty)).assertIsDisplayed()
    }

    @Test
    fun homeLeadsToSettingsAndLicenses() {
        show()
        openSettings()
        // 저장소의 처음 값(2분, 시스템 설정 따름)이 행에 보인다.
        compose.onNodeWithText("2분").assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.theme_mode_system)).assertIsDisplayed()

        compose.onNodeWithText(app.getString(R.string.settings_licenses)).performScrollTo().performClick()
        compose.onNodeWithText("Test Library").performClick()
        compose.onNodeWithText("Test Library license text.", substring = true).assertIsDisplayed()

        compose.onNodeWithContentDescription(app.getString(R.string.navigate_back)).performClick()
        compose.onNodeWithText("Test Font").assertIsDisplayed()
        compose.onNodeWithContentDescription(app.getString(R.string.navigate_back)).performClick()
        compose.onNodeWithText(app.getString(R.string.settings_default_rest)).assertIsDisplayed()
    }

    /** 설정의 "종목 관리"가 종목 고르기를 관리로 연다(#21). */
    @Test
    fun manageExercisesOpensPickerInManageMode() {
        show()
        openSettings()
        compose.onNodeWithText(app.getString(R.string.settings_manage_exercises)).performScrollTo().performClick()
        compose.onNodeWithText(app.getString(R.string.exercise_manage_title)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.exercise_add_selected, 0)).assertDoesNotExist()

        compose.onNodeWithContentDescription(app.getString(R.string.navigate_back)).performClick()
        compose.onNodeWithText(app.getString(R.string.settings_default_rest)).assertIsDisplayed()
    }

    /** 설정의 "백업"이 백업 화면을 연다(#29). 앱에서는 "준비 중이에요."가 남지 않는다. */
    @Test
    fun backupRowOpensBackupScreen() {
        show()
        openSettings()
        compose.onNodeWithText(app.getString(R.string.settings_not_ready)).assertDoesNotExist()
        compose.onNodeWithText(app.getString(R.string.settings_backup)).performScrollTo().performClick()
        compose.onNodeWithText(app.getString(R.string.backup_export)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.backup_import)).assertIsDisplayed()

        compose.onNodeWithContentDescription(app.getString(R.string.navigate_back)).performClick()
        compose.onNodeWithText(app.getString(R.string.settings_default_rest)).assertIsDisplayed()
    }

    @Test
    fun sourceCodeOpensRepositoryInBrowser() {
        show()
        openSettings()
        compose.onNodeWithText(app.getString(R.string.settings_source_code)).performScrollTo().performClick()
        assertEquals(listOf("https://github.com/zzunsik/next-set"), opened)
    }

    @Test
    fun missingBrowserShowsMessage() {
        browserMissing = true
        show()
        openSettings()
        compose.onNodeWithText(app.getString(R.string.settings_source_code)).performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText(app.getString(R.string.settings_open_link_failed))).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
