package io.github.zzunsik.nextset.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.a11y.assertAccessible
import io.github.zzunsik.nextset.ui.screenshot.LARGE_FONT_SCALE
import io.github.zzunsik.nextset.ui.screenshot.SCREENSHOT_DEVICE
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 끝내기에서 루틴 반영이 실패했을 때의 대화상자(#80). 누르기, 접근성 검사, 스크린샷 비교. 상태는 지어낸 값이다([WorkoutSamples]).
 *
 * 대화상자는 창이 따로 떠서 `assertAccessible()`(화면 하나를 본다)이 닿지 않는다. 글과 버튼을 대화상자 바탕(`surface`) 위에
 * 그대로 그려 먼저 검사하고, 운동 중 화면 위에 뜬 대화상자를 화면째 찍는다(`WorkoutReorderScreenshotTest`와 같은 방법).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class RoutineUpdateFailedDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val state = WorkoutSamples.allDone().copy(dialog = WorkoutDialog.RoutineUpdateFailed)

    @Test
    fun failureOffersRetryAndFinishingWithoutTheRoutine() {
        val answers = mutableListOf<String>()
        compose.setContent {
            NextSetTheme {
                WorkoutScreen(
                    state = state,
                    now = WorkoutSamples.now,
                    actions = WorkoutActions(
                        onRoutineChangeAnswer = { answers += "apply=$it" },
                        onDismissDialog = { answers += "dismiss" },
                    ),
                )
            }
        }

        compose.onNodeWithText(app.getString(R.string.finish_routine_update_failed)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.finish_retry)).performClick()
        compose.onNodeWithText(app.getString(R.string.finish_without_routine)).performClick()
        assertEquals("다시 시도는 반영을 다시 하고, 이번만 끝내기는 반영하지 않고 끝낸다", listOf("apply=true", "apply=false"), answers)
    }

    private fun capture() {
        // 한 테스트에서 setContent는 한 번만 부를 수 있어 상태로 바꿔 그린다.
        var showDialog by mutableStateOf(false)
        compose.setContent {
            NextSetTheme {
                if (showDialog) {
                    WorkoutScreen(state = state, now = WorkoutSamples.now, actions = WorkoutActions())
                } else {
                    val colors = NextSetTheme.colors
                    Surface(color = colors.surface) {
                        Column {
                            ConfirmText(stringResource(R.string.finish_routine_update_failed))
                            DialogButton(stringResource(R.string.finish_without_routine), colors.accentText) {}
                            DialogButton(stringResource(R.string.finish_retry), colors.accentText) {}
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
