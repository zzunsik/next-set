package io.github.zzunsik.nextset.ui.workout

import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import android.view.View
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.ui.component.NumberFieldKind
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import io.github.zzunsik.nextset.ui.workout.WorkoutSamples.block
import io.github.zzunsik.nextset.ui.workout.WorkoutSamples.done
import io.github.zzunsik.nextset.ui.workout.WorkoutSamples.todo
import io.github.zzunsik.nextset.ui.workout.WorkoutSamples.workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import io.github.zzunsik.nextset.ui.screenshot.SCREENSHOT_DEVICE

/** 운동 중 화면의 그리기와 누르기. 상태는 [WorkoutSamples]의 지어낸 값이다. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = SCREENSHOT_DEVICE)
class WorkoutScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private lateinit var view: View

    private fun show(state: WorkoutUiState, actions: WorkoutActions = WorkoutActions()) {
        compose.setContent {
            view = LocalView.current
            NextSetTheme { WorkoutScreen(state = state, now = WorkoutSamples.now, actions = actions) }
        }
    }

    private fun scrollTo(tag: String) {
        compose.onNodeWithTag(WORKOUT_LIST_TAG).performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun inputFieldsFollowTheRecordType() {
        val state = WorkoutSamples.typical()
        show(state)
        val (bench, pullUp, plank) = state.exercises

        // 무게와 횟수: 칸 둘. 머리는 "kg"와 목표 범위 "회 8~12"(LOG-05).
        compose.onNodeWithTag(fieldTag(bench.sets[2].id, NumberFieldKind.WEIGHT)).assertIsDisplayed()
        compose.onNodeWithTag(fieldTag(bench.sets[2].id, NumberFieldKind.REPS)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.set_table_reps_range, "8~12")).assertIsDisplayed()

        // 횟수만: 횟수 칸 하나.
        scrollTo(fieldTag(pullUp.sets[0].id, NumberFieldKind.REPS))
        compose.onNodeWithTag(fieldTag(pullUp.sets[0].id, NumberFieldKind.REPS)).assertIsDisplayed()
        compose.onNodeWithTag(fieldTag(pullUp.sets[0].id, NumberFieldKind.WEIGHT)).assertDoesNotExist()

        // 시간: 시간 칸 하나. "1:00"으로 적는다.
        scrollTo(fieldTag(plank.sets[0].id, NumberFieldKind.DURATION))
        compose.onNodeWithTag(fieldTag(plank.sets[0].id, NumberFieldKind.DURATION)).assertIsDisplayed()
        compose.onNodeWithContentDescription("플랭크 1세트 시간, 1:00").assertExists()
        compose.onNodeWithTag(fieldTag(plank.sets[0].id, NumberFieldKind.REPS)).assertDoesNotExist()
        compose.onNodeWithText(app.getString(R.string.set_table_duration)).assertIsDisplayed()
    }

    @Test
    fun rowShowsNumberPreviousRecordValuesAndCheck() {
        val state = WorkoutSamples.typical()
        show(state)
        val bench = state.exercises[0]
        compose.onNodeWithContentDescription(app.getString(R.string.set_number_description, 3)).assertExists()
        compose.onNodeWithText("60kg × 8").assertIsDisplayed() // 셋째 세트의 직전 기록
        compose.onNodeWithContentDescription("벤치 프레스 3세트 무게, 62.5").assertExists()
        compose.onNodeWithTag(checkTag(bench.sets[0].id)).assertIsOn()
        compose.onNodeWithTag(checkTag(bench.sets[2].id)).assertIsOff()
    }

    @Test
    fun nextSetIsShownLargeWithItsPreviousRecord() {
        show(WorkoutSamples.typical())
        val label = app.getString(R.string.next_set_label, 4, 3)
        val previous = app.getString(R.string.next_set_previous, "60kg × 8")
        compose.onNodeWithContentDescription(
            app.getString(R.string.next_set_description, label, "벤치 프레스", "62.5kg × 8") + ", " + previous,
        ).assertIsDisplayed()
    }

    @Test
    fun nextSetCollapsesWhileTheNumberPadIsOpen() {
        val state = WorkoutSamples.typical(numberPadOn = 3L to NumberFieldKind.REPS)
        show(state)
        compose.onNodeWithText(app.getString(R.string.next_set_collapsed, "벤치 프레스", "62.5kg × 8")).assertIsDisplayed()
        compose.onNodeWithContentDescription(app.getString(R.string.number_pad_close)).assertIsDisplayed()
    }

    @Test
    fun nextSetWithoutValuesOffersDirectInput() {
        val clicked = mutableListOf<Pair<Long, NumberFieldKind>>()
        val pullUp = block(1, "풀업", listOf(todo(null, null)), RecordType.REPS)
        show(WorkoutSamples.state(workout(pullUp)), WorkoutActions(onFieldClick = { id, kind -> clicked += id to kind }))

        compose.onNodeWithText(app.getString(R.string.next_set_enter_value)).performClick()

        assertEquals(listOf(pullUp.sets[0].id to NumberFieldKind.REPS), clicked)
    }

    @Test
    fun allDoneOffersToFinish() {
        var finished = 0
        show(WorkoutSamples.allDone(), WorkoutActions(onFinish = { finished++ }))
        compose.onNodeWithText(app.getString(R.string.next_set_all_done)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.next_set_finish)).performClick()
        assertEquals(1, finished)
        assertEquals(2, compose.onAllNodesWithContentDescription(app.getString(R.string.exercise_header_all_done)).fetchSemanticsNodes().size)
    }

    @Test
    fun emptyWorkoutAsksForAnExercise() {
        var added = 0
        show(WorkoutSamples.empty(), WorkoutActions(onAddExercise = { added++ }))
        compose.onNodeWithText(app.getString(R.string.workout_empty_title)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.workout_empty)).assertIsDisplayed()
        compose.onNodeWithTag(NEXT_SET_TAG).assertDoesNotExist()
        compose.onNodeWithText(app.getString(R.string.workout_add_exercise)).performClick()
        assertEquals(1, added)
    }

    @Test
    fun recordBadgeIsShownOnTheSetThatBeatTheRecord() {
        show(WorkoutSamples.typical())
        compose.onNodeWithContentDescription(app.getString(R.string.record_badge)).assertIsDisplayed()
    }

    @Test
    fun checkAndFieldsReportTheirSet() {
        val checked = mutableListOf<Long>()
        val clicked = mutableListOf<Pair<Long, NumberFieldKind>>()
        val state = WorkoutSamples.typical()
        show(state, WorkoutActions(onCheck = { checked += it }, onFieldClick = { id, kind -> clicked += id to kind }))
        val set = state.exercises[0].sets[2]

        compose.onNodeWithTag(checkTag(set.id)).performClick()
        compose.onNodeWithTag(fieldTag(set.id, NumberFieldKind.REPS)).performClick()

        assertEquals(listOf(set.id), checked)
        assertEquals(listOf(set.id to NumberFieldKind.REPS), clicked)
    }

    @Test
    fun keepsTheScreenOn() {
        show(WorkoutSamples.typical())
        compose.waitForIdle()
        assertTrue("운동 중에는 화면이 저절로 꺼지지 않는다(LOG-12)", view.keepScreenOn)
    }

    @Test
    fun finishQuestionsFollowTheDesignOrder() {
        val answers = mutableListOf<String>()
        val actions = WorkoutActions(
            onDiscardConfirm = { answers += "discard" },
            onIncompleteConfirm = { answers += "finish" },
            onRoutineChangeAnswer = { answers += "apply=$it" },
            onDismissDialog = { answers += "dismiss" },
        )
        val typical = WorkoutSamples.typical()

        show(typical.copy(dialog = WorkoutDialog.IncompleteSets(5)), actions)
        compose.onNodeWithText(app.getString(R.string.finish_incomplete, 5)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.finish_keep_going)).performClick()
        assertEquals(listOf("dismiss"), answers)
    }

    @Test
    fun routineChangeQuestionHasTwoAnswers() {
        val answers = mutableListOf<Boolean>()
        show(
            WorkoutSamples.allDone().copy(dialog = WorkoutDialog.RoutineChange),
            WorkoutActions(onRoutineChangeAnswer = { answers += it }),
        )
        compose.onNodeWithText(app.getString(R.string.finish_routine_change)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.finish_this_time)).performClick()
        compose.onNodeWithText(app.getString(R.string.finish_apply_routine)).performClick()
        assertEquals(listOf(false, true), answers)
    }

    @Test
    fun noCompletedSetsOffersToDiscard() {
        var discarded = 0
        show(WorkoutSamples.empty().copy(dialog = WorkoutDialog.NoCompletedSets), WorkoutActions(onDiscardConfirm = { discarded++ }))
        compose.onNodeWithText(app.getString(R.string.finish_no_completed)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.finish_discard)).performClick()
        assertEquals(1, discarded)
    }

    @Test
    fun prefilledValuesAreDimmedButEnteredOnesAreNot() {
        // 같은 값이라도 미리 채운 칸만 `suggested`에 든다. 화면은 이 집합으로 색을 고른다.
        val bench = block(
            1, "벤치 프레스", listOf(todo(60.0, 10), todo(60.0, 12)),
            target = Target(weight = WorkoutSamples.kg(60.0), repsMin = 10),
        )
        val state = WorkoutSamples.state(workout(bench), previous = mapOf(1L to listOf(SetValue(WorkoutSamples.kg(60.0), 10))))
        assertEquals(setOf(NumberFieldKind.WEIGHT, NumberFieldKind.REPS), state.exercises[0].sets[0].suggested)
        assertEquals("12회는 직접 고친 값이다", setOf(NumberFieldKind.WEIGHT), state.exercises[0].sets[1].suggested)
        assertFalse(state.exercises[0].sets[1].completed)
        show(state)
        compose.onNodeWithContentDescription("벤치 프레스 2세트 횟수, 12").assertIsDisplayed()
        val doneRow = block(2, "덤벨 플라이", listOf(done(14.0, 12)))
        assertTrue(WorkoutSamples.state(workout(doneRow)).exercises[0].sets[0].completed)
    }
}
