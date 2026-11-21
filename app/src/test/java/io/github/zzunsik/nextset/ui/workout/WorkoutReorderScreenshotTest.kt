package io.github.zzunsik.nextset.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.github.takahirom.roborazzi.captureScreenRoboImage
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
 * 운동 중 종목 순서 바꾸기 대화상자의 스크린샷 비교와 접근성 검사(#74). 종목 이름은 지어낸 것이다([WorkoutSamples]).
 *
 * 대화상자는 창이 따로 떠서 `assertAccessible()`(화면 하나를 본다)이 닿지 않는다. 제목과 목록을 대화상자 바탕(`surface`)
 * 위에 그대로 그려 먼저 검사하고, 운동 중 화면 위에 뜬 대화상자를 화면째 찍는다(홈의 "진행 중인 운동이 있어요."와 같은 방법).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class WorkoutReorderScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun capture() {
        val state = WorkoutSamples.typical().copy(dialog = WorkoutDialog.Reorder)
        // 한 테스트에서 setContent는 한 번만 부를 수 있어 상태로 바꿔 그린다.
        var showDialog by mutableStateOf(false)
        compose.setContent {
            NextSetTheme {
                if (showDialog) {
                    WorkoutScreen(state = state, now = WorkoutSamples.now, actions = WorkoutActions())
                } else {
                    Surface(color = NextSetTheme.colors.surface) {
                        Column {
                            ReorderTitle()
                            ReorderList(entries = state.exercises.map { ReorderEntry(it.id, it.name) }, onReorder = {})
                        }
                    }
                }
            }
        }
        compose.assertAccessible()
        showDialog = true
        compose.waitForIdle()
        captureScreenRoboImage()
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
