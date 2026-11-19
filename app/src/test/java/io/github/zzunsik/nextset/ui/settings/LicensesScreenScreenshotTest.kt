package io.github.zzunsik.nextset.ui.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.zzunsik.nextset.ui.a11y.assertAccessible
import io.github.zzunsik.nextset.ui.screenshot.LARGE_FONT_SCALE
import io.github.zzunsik.nextset.ui.screenshot.SCREENSHOT_DEVICE
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 오픈소스 라이선스 목록의 스크린샷 비교와 접근성 검사. 값은 지어낸 것이다.
 * 화면 모드는 시스템 설정(`+night`)을 따르는 기본값으로 둔다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class LicensesScreenScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun capture() {
        compose.setContent {
            NextSetTheme {
                LicensesScreen(notices = FakeLicenseRepository().notices, onOpen = {}, onBack = {})
            }
        }
        // 접근성 검사를 먼저 돌려 접근성 문제를 스크린샷 차이보다 먼저 알린다.
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
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
