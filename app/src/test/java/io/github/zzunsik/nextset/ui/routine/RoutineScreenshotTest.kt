package io.github.zzunsik.nextset.ui.routine

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.Routine
import io.github.zzunsik.nextset.domain.model.RoutineExercise
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.ui.a11y.assertAccessible
import io.github.zzunsik.nextset.ui.component.NumberPadField
import io.github.zzunsik.nextset.ui.component.NumberPadSession
import io.github.zzunsik.nextset.ui.exercise.TestExercises
import io.github.zzunsik.nextset.ui.screenshot.LARGE_FONT_SCALE
import io.github.zzunsik.nextset.ui.screenshot.SCREENSHOT_DEVICE
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 지어낸 루틴 값. */
private object Samples {
    private fun item(id: Long, exercise: Exercise, setCount: Int, target: Target = Target.NONE) =
        RoutineExercise(id, routineId = 1, exercise = exercise, position = id.toInt() - 1, setCount = setCount, target = target)

    /** 종목이 많은 루틴. 화면을 넘친다. */
    val manyExercises = listOf(
        item(1, TestExercises.benchPress, 4, Target(weight = Weight.kg(60), repsMin = 8, repsMax = 12)),
        item(2, TestExercises.dumbbellBenchPress, 3, Target(weight = Weight(22_500), repsMin = 10, repsMax = 10)),
        item(3, TestExercises.closeGripBenchPress, 3),
        item(4, TestExercises.pushUp, 3, Target(repsMin = 15)),
        item(5, TestExercises.latPulldown, 4, Target(repsMin = 10, repsMax = 12)),
        item(6, TestExercises.backSquat, 5, Target(weight = Weight.kg(80))),
        item(7, TestExercises.lyingLegCurl, 3, Target(repsMax = 15)),
        item(8, TestExercises.plank, 2, Target(durationSeconds = 60)),
    )

    val routines = listOf("가슴과 삼두", "등과 이두", "하체", "어깨").mapIndexed { i, name -> Routine(i + 1L, name, i) }
}

/** 루틴 편집의 스크린샷 비교와 접근성 검사. 화면 모드는 시스템 설정(`+night`)을 따른다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class RoutineEditScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val many = RoutineEditUiState(name = "가슴과 삼두", exercises = Samples.manyExercises, loading = false)
    private val newEmpty = RoutineEditUiState(isNew = true, loading = false)

    private fun capture(state: RoutineEditUiState) {
        compose.setContent {
            NextSetTheme {
                RoutineEditScreen(
                    state = state,
                    onBack = {},
                    onNameChange = {},
                    onAddExercise = {},
                    onOpenExercise = {},
                    onReorder = {},
                    onDelete = {},
                )
            }
        }
        // 접근성 검사를 먼저 돌려 접근성 문제를 스크린샷 차이보다 먼저 알린다.
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun light() = capture(many)

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun lightLargeFont() = capture(many)

    @Test
    @Config(qualifiers = "+night")
    fun dark() = capture(many)

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun darkLargeFont() = capture(many)

    /** 종목이 없는 새 루틴. */
    @Test
    fun newEmpty() = capture(newEmpty)

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun newEmptyDarkLargeFont() = capture(newEmpty)
}

/** 종목 설정의 스크린샷 비교와 접근성 검사. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class RoutineExerciseScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val weightTarget = Target(weight = Weight(62_500), repsMin = 8, repsMax = 12)

    /** 무게와 횟수 종목. 목표를 다 적은 상태. */
    private val weightReps = RoutineExerciseUiState(
        exercise = TestExercises.benchPress,
        setCount = 4,
        target = weightTarget,
        loading = false,
    )

    private fun capture(state: RoutineExerciseUiState) {
        compose.setContent { NextSetTheme { Screen(state) } }
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Screen(state: RoutineExerciseUiState) {
        RoutineExerciseScreen(
            state = state,
            onBack = {},
            onSetCountChange = {},
            onFieldClick = {},
            onNumberPadKey = {},
            onRestChange = {},
            onRemove = {},
        )
    }

    @Test
    fun light() = capture(weightReps)

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun lightLargeFont() = capture(weightReps)

    @Test
    @Config(qualifiers = "+night")
    fun dark() = capture(weightReps)

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun darkLargeFont() = capture(weightReps)

    /** 횟수만 종목. 하한이 상한보다 커 오류 문구가 보이고, 세트 수가 1이라 "−"가 비활성이다. */
    @Test
    fun repsOnlyInvalidRange() = capture(
        RoutineExerciseUiState(
            exercise = TestExercises.pushUp,
            setCount = 1,
            target = Target(repsMin = 15, repsMax = 10),
            loading = false,
        ),
    )

    /**
     * 시간 종목의 목표 칸을 눌러 숫자판이 열린 상태. 한 칸뿐이라 "다음" 키가 "완료"다.
     * 내용이 숫자판 위에 다 들어간다. 무게와 횟수 종목은 내용이 숫자판 경계에 걸려 ATF가 가려진 글자로 알리므로 숫자판 없이 찍는다.
     */
    @Test
    fun durationWithNumberPad() {
        val target = Target(durationSeconds = 45)
        capture(
            RoutineExerciseUiState(
                exercise = TestExercises.plank,
                setCount = 3,
                target = target,
                numberPad = NumberPadSession.open(
                    listOf(NumberPadField(TargetField.DURATION, TargetField.DURATION.kind, target.amountOf(TargetField.DURATION))),
                    0,
                ),
                loading = false,
            ),
        )
    }

    /** 시간 종목. 휴식 시간을 따로 정한 종목이다. */
    @Test
    @Config(qualifiers = "+night")
    fun durationDark() = capture(
        RoutineExerciseUiState(
            exercise = TestExercises.plank.copy(restSeconds = 90),
            setCount = 3,
            target = Target(durationSeconds = 45),
            loading = false,
        ),
    )
}

/** 루틴 목록(임시 화면)의 스크린샷 비교와 접근성 검사. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class RoutineListScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val routines = RoutineListUiState(routines = Samples.routines, loading = false)

    private fun capture(state: RoutineListUiState) {
        compose.setContent {
            NextSetTheme {
                RoutineListScreen(state = state, onBack = {}, onNewRoutine = {}, onOpenRoutine = {}, onReorder = {})
            }
        }
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun light() = capture(routines)

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun lightLargeFont() = capture(routines)

    @Test
    @Config(qualifiers = "+night")
    fun dark() = capture(routines)

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun darkLargeFont() = capture(routines)

    @Test
    fun empty() = capture(RoutineListUiState(loading = false))
}
