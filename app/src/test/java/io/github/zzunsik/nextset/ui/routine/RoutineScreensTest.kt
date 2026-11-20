package io.github.zzunsik.nextset.ui.routine

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.RoutineExercise
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.ui.exercise.TestExercises.benchPress
import io.github.zzunsik.nextset.ui.exercise.TestExercises.plank
import io.github.zzunsik.nextset.ui.exercise.TestExercises.pushUp
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** 루틴 편집과 종목 설정 화면을 상태로 그려 본다. */
@RunWith(RobolectricTestRunner::class)
class RoutineScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()

    private fun item(id: Long, exercise: Exercise, position: Int, setCount: Int = 3, target: Target = Target.NONE) =
        RoutineExercise(id, routineId = 1, exercise = exercise, position = position, setCount = setCount, target = target)

    private fun showEdit(state: RoutineEditUiState, onDelete: () -> Unit = {}, onReorder: (List<Long>) -> Unit = {}) {
        compose.setContent {
            NextSetTheme {
                RoutineEditScreen(
                    state = state,
                    onBack = {},
                    onNameChange = {},
                    onAddExercise = {},
                    onOpenExercise = {},
                    onReorder = onReorder,
                    onDelete = onDelete,
                )
            }
        }
    }

    private fun showSettings(state: RoutineExerciseUiState) {
        compose.setContent {
            NextSetTheme {
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
        }
    }

    private fun settingsFor(exercise: Exercise, setCount: Int = 3, target: Target = Target.NONE) =
        RoutineExerciseUiState(exercise = exercise, setCount = setCount, target = target, loading = false)

    private fun SemanticsNodeInteraction.customAction(label: String) {
        val actions = fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        val action = actions.firstOrNull { it.label == label } ?: error("'$label' 동작이 없다: ${actions.map { it.label }}")
        compose.runOnIdle { action.action() }
    }

    @Test
    fun newRoutineWithoutExercisesShowsTheEmptyStateAndNoDeleteButton() {
        showEdit(RoutineEditUiState(isNew = true, loading = false))

        compose.onNodeWithText(app.getString(R.string.routine_new)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.routine_name_hint)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.routine_no_exercises)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.routine_delete)).assertDoesNotExist()
    }

    @Test
    fun rowsShowSetsAndTargetsForEachRecordType() {
        showEdit(
            RoutineEditUiState(
                name = "전신",
                loading = false,
                exercises = listOf(
                    item(1, benchPress, 0, target = Target(weight = Weight.kg(60), repsMin = 8, repsMax = 12)),
                    item(2, pushUp, 1, setCount = 2, target = Target(repsMin = 15)),
                    item(3, plank, 2, setCount = 1, target = Target(durationSeconds = 60)),
                    item(4, benchPress, 3, setCount = 5),
                ),
            ),
        )

        compose.onNodeWithText(app.getString(R.string.routine_edit_title)).assertIsDisplayed()
        listOf("3세트 · 60kg × 8~12회", "2세트 · 15회 이상", "1세트 · 1:00", "5세트").forEach { caption ->
            compose.onNodeWithTag(ROUTINE_EDIT_LIST_TAG).performScrollToNode(hasText(caption))
            compose.onNodeWithText(caption).assertIsDisplayed()
        }
    }

    @Test
    fun deleteAsksFirst() {
        var deleted = 0
        showEdit(RoutineEditUiState(name = "가슴", loading = false), onDelete = { deleted++ })

        compose.onNodeWithText(app.getString(R.string.routine_delete)).performScrollTo().performClick()
        compose.onNodeWithText(app.getString(R.string.routine_delete_confirm)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.action_cancel)).performClick()
        assertEquals(0, deleted)

        compose.onNodeWithText(app.getString(R.string.routine_delete)).performClick()
        compose.onNodeWithText(app.getString(R.string.action_delete)).performClick()
        assertEquals(1, deleted)
    }

    /** 화면 낭독기의 "위로 옮기기", "아래로 옮기기"로 순서를 바꾼다. 맨 위에는 위로, 맨 아래에는 아래로가 없다. */
    @Test
    fun rowsCanBeMovedWithAccessibilityActions() {
        val orders = mutableListOf<List<Long>>()
        showEdit(
            RoutineEditUiState(
                name = "전신",
                loading = false,
                exercises = listOf(item(1, benchPress, 0), item(2, pushUp, 1), item(3, plank, 2)),
            ),
            onReorder = { orders += it },
        )

        compose.onNodeWithText(benchPress.nameKo).customAction(app.getString(R.string.move_down))
        compose.onNodeWithText(plank.nameKo).customAction(app.getString(R.string.move_up))
        assertEquals(listOf(listOf(2L, 1L, 3L), listOf(1L, 3L, 2L)), orders)

        val first = compose.onNodeWithText(benchPress.nameKo).fetchSemanticsNode()
        val labels = first.config.getOrNull(SemanticsActions.CustomActions).orEmpty().map { it.label }
        assertEquals(listOf(app.getString(R.string.move_down)), labels)
    }

    /** 기록 방식에 따라 목표 입력 칸이 바뀐다(RTN-03). */
    @Test
    fun weightAndRepsExerciseHasWeightAndRepRangeFields() {
        showSettings(settingsFor(benchPress))
        compose.onNodeWithTag(targetFieldTag(TargetField.WEIGHT)).assertIsDisplayed()
        compose.onNodeWithTag(targetFieldTag(TargetField.REPS_MIN)).assertIsDisplayed()
        compose.onNodeWithTag(targetFieldTag(TargetField.REPS_MAX)).assertIsDisplayed()
        compose.onNodeWithTag(targetFieldTag(TargetField.DURATION)).assertDoesNotExist()
    }

    @Test
    fun repsOnlyExerciseHasOnlyTheRepRange() {
        showSettings(settingsFor(pushUp))
        compose.onNodeWithTag(targetFieldTag(TargetField.WEIGHT)).assertDoesNotExist()
        compose.onNodeWithTag(targetFieldTag(TargetField.REPS_MIN)).assertIsDisplayed()
        compose.onNodeWithTag(targetFieldTag(TargetField.REPS_MAX)).assertIsDisplayed()
        compose.onNodeWithTag(targetFieldTag(TargetField.DURATION)).assertDoesNotExist()
    }

    @Test
    fun durationExerciseHasOnlyTheDuration() {
        showSettings(settingsFor(plank, target = Target(durationSeconds = 45)))
        compose.onNodeWithTag(targetFieldTag(TargetField.WEIGHT)).assertDoesNotExist()
        compose.onNodeWithTag(targetFieldTag(TargetField.REPS_MIN)).assertDoesNotExist()
        compose.onNodeWithTag(targetFieldTag(TargetField.DURATION)).assertIsDisplayed()
        compose.onNodeWithContentDescription(
            app.getString(R.string.number_field_description, app.getString(R.string.routine_target_duration_label), "0:45"),
        ).assertIsDisplayed()
    }

    @Test
    fun decreaseIsDisabledAtOneSet() {
        showSettings(settingsFor(benchPress, setCount = 1))
        compose.onNodeWithContentDescription(app.getString(R.string.routine_exercise_sets_decrease)).assertIsNotEnabled()
        compose.onNodeWithContentDescription(app.getString(R.string.routine_exercise_sets_increase)).assertIsEnabled()
        compose.onNodeWithContentDescription("1세트").assertIsDisplayed()
    }

    @Test
    fun minAboveMaxShowsAnError() {
        showSettings(settingsFor(pushUp, target = Target(repsMin = 12, repsMax = 8)))
        compose.onNodeWithText(app.getString(R.string.routine_target_reps_invalid)).assertIsDisplayed()
    }

    @Test
    fun restRowShowsTheDefaultWhenNotSet() {
        showSettings(settingsFor(benchPress).copy(defaultRestSeconds = 150))
        compose.onNodeWithText(app.getString(R.string.routine_exercise_rest_default, "2분 30초")).performScrollTo().assertIsDisplayed()
        assertTrue(benchPress.restSeconds == null)
    }
}
