package io.github.zzunsik.nextset.ui.home

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** 홈이 상태에 따라 무엇을 보이고 누르면 무엇을 부르는지(HOME-01~HOME-06). 이름과 값은 지어낸 것이다. */
@RunWith(RobolectricTestRunner::class)
class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val calls = mutableListOf<String>()

    private val actions = HomeActions(
        onStartRoutine = { calls += "start:$it" },
        onStartEmpty = { calls += "empty" },
        onResume = { calls += "resume:$it" },
        onPin = { calls += "pin:$it" },
        onUnpin = { calls += "unpin" },
        onEditRoutine = { calls += "edit:$it" },
        onDeleteRequest = { calls += "delete:$it" },
        onNewRoutine = { calls += "new" },
        onPromptResume = { calls += "prompt-resume" },
        onPromptFinishAndStart = { calls += "prompt-finish" },
        onDismissDialog = { calls += "dismiss" },
    )

    private fun text(id: Int, vararg args: Any) = app.getString(id, *args)

    /** 목록은 화면에 보이는 만큼만 그리므로 아래 항목은 내려서 본다. */
    private fun scrollTo(matcher: SemanticsMatcher) {
        compose.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(matcher)
    }

    private fun scrollToText(value: String, substring: Boolean = false) = scrollTo(hasText(value, substring = substring))

    private fun show(state: HomeUiState) {
        compose.setContent {
            NextSetTheme { HomeScreen(state = state, now = HomeSamples.now, today = HomeSamples.today, actions = actions) }
        }
    }

    // HOME-01, HOME-02

    @Test
    fun topCardShowsTheNextRoutineAndStartsItWithOneTap() {
        show(HomeSamples.many)
        compose.onNodeWithText(text(R.string.home_next_label)).assertIsDisplayed()
        compose.onNodeWithText("랫 풀다운, 바벨 로우 외 2개").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.home_last_done, "11월 16일 (월)")).assertIsDisplayed()

        compose.onNodeWithText(text(R.string.home_start)).performClick()
        assertEquals(listOf("start:2"), calls)
    }

    @Test
    fun tappingTheCardItselfAlsoStarts() {
        show(HomeSamples.many)
        compose.onNodeWithText("랫 풀다운, 바벨 로우 외 2개").performClick()
        assertEquals(listOf("start:2"), calls)
    }

    // HOME-03

    @Test
    fun pinnedRoutineIsOnTopWithAutoOrderButton() {
        show(HomeSamples.pinned)
        compose.onNodeWithText(text(R.string.home_pinned_label)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.home_next_label)).assertDoesNotExist()
        compose.onNodeWithContentDescription(text(R.string.home_start_routine_description, "하체")).assertIsDisplayed()

        compose.onNodeWithText(text(R.string.home_unpin)).performClick()
        assertEquals(listOf("unpin"), calls)
    }

    @Test
    fun rowMenuPinsEditsAndDeletes() {
        show(HomeSamples.many)
        scrollTo(hasContentDescription(text(R.string.home_routine_more, "하체")))
        compose.onNodeWithContentDescription(text(R.string.home_routine_more, "하체")).performClick()
        compose.onNodeWithText(text(R.string.home_pin)).performClick()
        compose.onNodeWithContentDescription(text(R.string.home_routine_more, "하체")).performClick()
        compose.onNodeWithText(text(R.string.home_edit)).performClick()
        compose.onNodeWithContentDescription(text(R.string.home_routine_more, "하체")).performClick()
        compose.onNodeWithText(text(R.string.action_delete)).performClick()
        assertEquals(listOf("pin:3", "edit:3", "delete:3"), calls)
    }

    // HOME-04

    @Test
    fun listShowsTheOtherRoutinesAndStartsWithOneTap() {
        show(HomeSamples.many)
        scrollToText(text(R.string.home_other_routines))
        compose.onNodeWithText(text(R.string.home_other_routines)).assertIsDisplayed()
        scrollToText("가슴과 삼두")
        compose.onNodeWithText(text(R.string.home_row_detail, text(R.string.home_exercise_count, 5), text(R.string.home_last_done, "11월 14일 (토)")))
            .assertIsDisplayed()
        // 올해가 아닌 날짜에는 연도를 붙이고, 한 적이 없으면 종목 수만 적는다.
        scrollToText(text(R.string.home_last_done, "2025년 12월 30일 (화)"), substring = true)
        compose.onNodeWithText(text(R.string.home_last_done, "2025년 12월 30일 (화)"), substring = true).assertExists()
        scrollToText(text(R.string.home_exercise_count, 2))
        compose.onNodeWithText(text(R.string.home_exercise_count, 2)).assertExists()
        scrollToText("가슴과 삼두")

        compose.onNodeWithText("가슴과 삼두").performClick()
        assertEquals(listOf("start:1"), calls)
    }

    @Test
    fun singleRoutineHidesTheList() {
        show(HomeSamples.one)
        compose.onNodeWithText("가슴과 삼두").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.home_other_routines)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.home_reorder)).assertDoesNotExist()
    }

    // HOME-05

    @Test
    fun emptyWorkoutStartsWithOneTap() {
        show(HomeSamples.many)
        scrollToText(text(R.string.home_start_empty))
        compose.onNodeWithText(text(R.string.home_start_empty)).performClick()
        assertEquals(listOf("empty"), calls)
    }

    // HOME-06

    @Test
    fun inProgressWorkoutShowsResumeOnTop() {
        show(HomeSamples.resume)
        compose.onNodeWithText(text(R.string.home_resume_label)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.home_resume_progress, 12, 5), substring = true).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.home_next_label)).assertDoesNotExist()

        compose.onNodeWithText(text(R.string.home_resume)).performClick()
        assertEquals(listOf("resume:10"), calls)
    }

    @Test
    fun inProgressPromptOffersResumeFinishAndCancel() {
        show(HomeSamples.inProgressPrompt)
        compose.onNodeWithText(text(R.string.home_in_progress_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.home_in_progress_detail, "가슴과 삼두", 5)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.home_finish_and_start)).performClick()
        compose.onNodeWithText(text(R.string.action_cancel)).performClick()
        // 카드에도 "이어서 하기"가 있어 대화상자 안의 것을 고른다.
        compose.onNode(hasText(text(R.string.home_resume)) and hasAnyAncestor(isDialog())).performClick()
        assertEquals(listOf("prompt-finish", "dismiss", "prompt-resume"), calls)
    }

    // 빈 상태

    @Test
    fun noRoutinesShowsOneCreateButtonAndKeepsEmptyWorkout() {
        show(HomeSamples.empty)
        compose.onNodeWithText(text(R.string.home_empty)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.routine_new)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.home_start_empty)).assertIsDisplayed()

        compose.onNodeWithText(text(R.string.routine_create)).performClick()
        assertEquals(listOf("new"), calls)
    }

    @Test
    fun newRoutineButtonCreatesARoutine() {
        show(HomeSamples.many)
        scrollToText(text(R.string.routine_new))
        compose.onNodeWithText(text(R.string.routine_new)).performClick()
        assertEquals(listOf("new"), calls)
    }

    // RTN-05

    @Test
    fun reorderShowsEveryRoutineAndOpensEditOnTap() {
        show(HomeSamples.many)
        scrollToText(text(R.string.home_reorder))
        compose.onNodeWithText(text(R.string.home_reorder)).performClick()
        compose.onNodeWithText(text(R.string.home_reorder_done)).assertIsDisplayed()
        // 맨 위 카드의 루틴("등과 이두")도 목록에 들어온다.
        scrollTo(hasContentDescription(text(R.string.home_routine_delete, "등과 이두")))
        compose.onNodeWithContentDescription(text(R.string.home_routine_delete, "등과 이두")).assertExists()

        scrollToText("하체")
        compose.onNodeWithText("하체").performClick()
        scrollTo(hasContentDescription(text(R.string.home_routine_delete, "어깨")))
        compose.onNodeWithContentDescription(text(R.string.home_routine_delete, "어깨")).performClick()
        assertEquals(listOf("edit:3", "delete:4"), calls)
    }
}
