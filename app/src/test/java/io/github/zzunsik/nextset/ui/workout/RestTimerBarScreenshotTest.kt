package io.github.zzunsik.nextset.ui.workout

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.zzunsik.nextset.ui.a11y.assertAccessible
import io.github.zzunsik.nextset.ui.component.NumberFieldKind
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
 * 휴식 타이머 하단 바(REST-02)를 운동 중 화면 안에서 찍는다. 세트 표가 바에 가리지 않는지, 숫자판이 열리면 바가 숫자판 위에
 * 한 줄로 붙는지 본다. 남은 시간 1:45, 전체 2:00. 종목과 값은 지어낸 것이다([WorkoutSamples]).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class RestTimerBarScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun capture(state: WorkoutUiState = WorkoutSamples.typical(), notificationButton: Boolean = false) {
        compose.setContent {
            NextSetTheme {
                WorkoutScreen(
                    state = state,
                    now = WorkoutSamples.now,
                    actions = WorkoutActions(),
                    restTimerBar = {
                        RestTimerBar(
                            remainingMillis = 105_000,
                            progress = 105f / 120f,
                            nextSet = "벤치 프레스 62.5kg × 8",
                            compact = state.numberPad != null,
                            showNotificationButton = notificationButton,
                            onDecrease = {},
                            onIncrease = {},
                            onSkip = {},
                        )
                    },
                )
            }
        }
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
    }

    private val padOpen get() = WorkoutSamples.typical(numberPadOn = 3L to NumberFieldKind.REPS)

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

    /** '알림 켜기'가 보이는 상태(REST-09). 바의 높이는 버튼이 없을 때와 같다. */
    @Test
    fun notificationButton() = capture(notificationButton = true)

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun notificationButtonDarkLargeFont() = capture(notificationButton = true)

    /** 숫자판이 열리면 바가 숫자판 바로 위에 붙고 한 줄로 줄어든다. */
    @Test
    fun numberPadOpen() = capture(padOpen)

    @Test
    @Config(qualifiers = "+night")
    fun numberPadOpenDark() = capture(padOpen)

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun numberPadOpenLargeFont() = capture(padOpen)

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun numberPadOpenDarkLargeFont() = capture(padOpen)
}
