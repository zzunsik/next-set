package io.github.zzunsik.nextset.ui.workout

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

/** 운동을 끝낸 뒤의 완료 화면(운동 요약 #27 전까지). 값은 지어낸 것이다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class WorkoutDoneScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun capture() {
        compose.setContent {
            NextSetTheme {
                WorkoutDoneScreen(state = WorkoutDoneUi(routineName = "가슴 루틴", completedSets = 14, minutes = 72), onConfirm = {})
            }
        }
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
