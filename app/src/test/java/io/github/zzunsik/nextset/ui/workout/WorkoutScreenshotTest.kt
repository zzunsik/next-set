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
 * 운동 중 화면의 스크린샷 비교와 접근성 검사. 종목 이름과 값은 지어낸 것이다([WorkoutSamples]).
 *
 * 기본 장면은 벤치 프레스 두 세트를 완료하고(둘째가 최고 기록) 셋째가 다음 세트인 상태다.
 * 미리 채운 값(`text2`)과 완료한 값(`text`), 횟수만과 시간 종목의 칸, 목표 범위 "회 8~12", 종목 메모가 함께 보인다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class WorkoutScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun capture(state: WorkoutUiState = WorkoutSamples.typical()) {
        compose.setContent {
            NextSetTheme { WorkoutScreen(state = state, now = WorkoutSamples.now, actions = WorkoutActions()) }
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

    /** 빈 운동을 시작한 직후. */
    @Test
    fun emptyWorkout() = capture(WorkoutSamples.empty())

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun emptyWorkoutDarkLargeFont() = capture(WorkoutSamples.empty())

    /** 종목이 여섯인 운동. 목록이 화면을 넘는다. */
    @Test
    fun manyExercises() = capture(WorkoutSamples.many())

    /** 모든 세트를 끝내 다음 세트 자리에 "운동 끝내기"가 있다. */
    @Test
    fun allDone() = capture(WorkoutSamples.allDone())

    @Test
    @Config(qualifiers = "+night")
    fun allDoneDark() = capture(WorkoutSamples.allDone())

    /** 다음 세트의 횟수 칸을 골라 숫자판이 열렸다. 다음 세트 표시가 한 줄로 줄어든다. */
    @Test
    fun numberPadOpen() = capture(WorkoutSamples.typical(numberPadOn = 3L to NumberFieldKind.REPS))

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun numberPadOpenDarkLargeFont() = capture(WorkoutSamples.typical(numberPadOn = 3L to NumberFieldKind.REPS))
}
