package io.github.zzunsik.nextset.ui.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.zzunsik.nextset.domain.model.ThemeMode
import io.github.zzunsik.nextset.ui.a11y.assertAccessible
import io.github.zzunsik.nextset.ui.screenshot.LARGE_FONT_SCALE
import io.github.zzunsik.nextset.ui.screenshot.SCREENSHOT_DEVICE
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 설정의 "알림 꺼짐" 행(REST-09, docs/DESIGN.md "설정" 4번). 알림이 꺼져 있을 때만 보인다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class SettingsNotificationRowTest {
    @get:Rule
    val compose = createComposeRule()

    private var enableClicks = 0

    private fun show(notificationsOff: Boolean) {
        compose.setContent {
            NextSetTheme {
                SettingsScreen(
                    state = SettingsUiState(defaultRestSeconds = 120, themeMode = ThemeMode.SYSTEM),
                    versionName = "0.1.0",
                    onDefaultRestChange = {},
                    onThemeModeChange = {},
                    onOpenLicenses = {},
                    onOpenSourceCode = {},
                    onManageExercises = {},
                    onBackup = {},
                    notificationsOff = notificationsOff,
                    onEnableNotifications = { enableClicks++ },
                )
            }
        }
    }

    private fun capture() {
        show(notificationsOff = true)
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun hiddenWhenNotificationsAreOn() {
        show(notificationsOff = false)
        compose.onNodeWithText("알림 꺼짐").assertDoesNotExist()
        compose.onNodeWithText("알림 켜기").assertDoesNotExist()
    }

    @Test
    fun enableButtonAsksForPermission() {
        show(notificationsOff = true)
        compose.onNodeWithText("켜면 다른 앱을 쓰는 동안에도 남은 휴식 시간이 보여요.").assertExists()
        compose.onNodeWithText("알림 켜기").performClick()
        assertEquals(1, enableClicks)
    }

    @Test
    fun light() = capture()

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun lightLargeFont() = capture()

    @Test
    @Config(qualifiers = "+night")
    fun dark() = capture()

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun darkLargeFont() = capture()
}
