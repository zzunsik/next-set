package io.github.zzunsik.nextset.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.data.repository.TestDbRule
import io.github.zzunsik.nextset.ui.exercise.EXERCISE_LIST_TAG
import io.github.zzunsik.nextset.ui.exercise.TestExercises.pushUp
import io.github.zzunsik.nextset.ui.home.HOME_LIST_TAG
import io.github.zzunsik.nextset.ui.routine.FakeRoutineRepository
import io.github.zzunsik.nextset.ui.settings.FakeBackupRepository
import io.github.zzunsik.nextset.ui.settings.FakeLicenseRepository
import io.github.zzunsik.nextset.ui.settings.FakeSettingsRepository
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 버튼을 빠르게 두 번 눌러도 화면이 한 번만 열리고 데이터가 한 번만 만들어진다(#103).
 *
 * 두 번 누르기는 두 가지로 흉내 낸다.
 * - [doubleTap]: 같은 프레임 안에서 버튼의 클릭 동작을 두 번 부른다. 첫 번째 누르기의 결과(화면 이동, 저장)가 반영되기 전이다.
 * - [staleTap]: 첫 번째 누르기로 다음 화면이 열린 뒤, 화면 전환 동안 아직 보이는 앞 화면의 버튼이 한 번 더 눌린다.
 *
 * 저장소는 루틴과 종목이 메모리 가짜, 운동이 메모리 데이터베이스다.
 */
@RunWith(RobolectricTestRunner::class)
class DoubleTapNavigationTest {
    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** 메모리 데이터베이스. Compose 규칙보다 바깥이라 화면이 사라진 뒤에 닫힌다. */
    @get:Rule(order = 0)
    internal val dbRule = TestDbRule(seed = false)

    private val app get() = compose.activity
    private val routines = FakeRoutineRepository()
    private val db get() = dbRule.db

    private fun show() {
        compose.setContent {
            NextSetTheme {
                NextSetNavDisplay(
                    settingsRepository = FakeSettingsRepository(),
                    licenseRepository = FakeLicenseRepository(),
                    exerciseRepository = routines.exercises,
                    workoutRepository = db.workouts,
                    routineRepository = routines,
                    backupRepository = FakeBackupRepository(),
                )
            }
        }
        waitFor(hasTestTag(HOME_LIST_TAG))
    }

    private fun text(id: Int, vararg args: Any) = app.getString(id, *args)

    private fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(matcher.description, TIMEOUT) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    /** [matcher] 버튼의 클릭 동작. 화면이 바뀐 뒤에 불러도 그 버튼을 누른 것과 같다. */
    private fun clickOf(matcher: SemanticsMatcher): () -> Unit {
        waitFor(matcher)
        val action = compose.onNode(matcher).fetchSemanticsNode().config[SemanticsActions.OnClick].action
        assertNotNull(action)
        return { action!!.invoke() }
    }

    private fun doubleTap(matcher: SemanticsMatcher) {
        val click = clickOf(matcher)
        compose.runOnUiThread {
            click()
            click()
        }
        compose.waitForIdle()
    }

    /**
     * [matcher] 버튼을 누르고 [opened] 화면이 들어오기 시작하면(화면 전환 중), 아직 보이는 앞 화면의 같은 버튼을 한 번 더 누른다.
     * 화면 전환이 끝나기 전에 누르도록 시계를 멈추고 한 프레임씩 진행한다. 저장은 다른 스레드에서 끝나므로 프레임 사이에 잠깐 쉰다.
     * [beforeSecondTap]이 참이 될 때까지도 같은 방법으로 기다린다.
     */
    private fun staleTap(matcher: SemanticsMatcher, opened: SemanticsMatcher, beforeSecondTap: SemanticsMatcher? = null) {
        val click = clickOf(matcher)
        compose.mainClock.autoAdvance = false
        try {
            compose.runOnUiThread { click() }
            advanceUntil(opened)
            if (beforeSecondTap != null) advanceUntil(beforeSecondTap)
            compose.runOnUiThread { click() }
        } finally {
            compose.mainClock.autoAdvance = true
        }
        compose.waitForIdle()
    }

    private fun advanceUntil(matcher: SemanticsMatcher) {
        var frames = 0
        while (compose.onAllNodes(matcher).fetchSemanticsNodes().isEmpty()) {
            check(frames++ < MAX_FRAMES) { "${matcher.description}가 화면 전환 안에 나오지 않았다" }
            compose.mainClock.advanceTimeByFrame()
            Thread.sleep(FRAME_PAUSE_MS)
        }
    }

    private fun button(id: Int, vararg args: Any) = hasText(text(id, *args)) and hasClickAction()

    private fun back() = compose.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()

    private val emptyHome get() = hasText(text(R.string.home_empty))
    private val routineEditor get() = hasText(text(R.string.routine_add_exercise))

    /** 운동 중 화면. 빈 운동에는 "종목 추가"와 "운동 끝내기"가 있다. */
    private val workoutScreen get() = hasText(text(R.string.workout_finish))

    private fun inProgressId(): Long? = runBlocking { db.workouts.observeInProgressWorkout().first()?.id }

    @Test
    fun doubleTapOnCreateRoutineOpensOneEditorAndCreatesOneRoutine() {
        show()
        doubleTap(button(R.string.routine_create))
        waitFor(routineEditor)
        compose.waitForIdle()

        assertEquals("루틴은 하나만 만들어진다", 1, routines.routines.value.size)
        back()
        waitFor(emptyHome)
        compose.onNode(emptyHome).assertIsDisplayed()
        assertTrue("빈 새 루틴은 나갈 때 버린다", routines.routines.value.isEmpty())
    }

    /** 루틴이 있으면 홈 아래의 "새 루틴"으로 만든다. 이 버튼은 루틴이 생긴 뒤에도 홈에 남아 화면 전환 중에 다시 눌릴 수 있다. */
    @Test
    fun tapOnNewRoutineWhileTheEditorOpensDoesNothing() {
        runBlocking { routines.addRoutine("가슴") }
        show()
        compose.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(button(R.string.routine_new))
        staleTap(button(R.string.routine_new), opened = routineEditor)

        assertEquals("새 루틴은 하나만 만들어진다", listOf("가슴", text(R.string.routine_new)), routines.routines.value.map { it.name })
        back()
        waitFor(hasText(text(R.string.home_start_empty)))
        assertEquals(listOf("가슴"), routines.routines.value.map { it.name })

        // 홈으로 돌아오면 다시 누를 수 있다.
        compose.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(button(R.string.routine_new))
        compose.onNode(button(R.string.routine_new)).performClick()
        waitFor(routineEditor)
        assertEquals(2, routines.routines.value.size)
    }

    /** 고른 종목을 돌려주는 "n개 추가"를 두 번 눌러도 루틴 편집까지 닫히지 않고 종목이 루틴에 들어간다. */
    @Test
    fun doubleTapOnAddSelectedKeepsTheEditorAndAddsTheExercise() {
        show()
        compose.onNodeWithText(text(R.string.routine_create)).performClick()
        waitFor(routineEditor)
        compose.onNode(hasSetTextAction()).performTextInput("가슴")
        compose.onNodeWithText(text(R.string.routine_add_exercise)).performScrollTo().performClick()
        compose.onNodeWithTag(EXERCISE_LIST_TAG).performScrollToNode(hasText(pushUp.nameKo))
        compose.onNodeWithText(pushUp.nameKo).performClick()

        doubleTap(button(R.string.exercise_add_selected, 1))

        waitFor(routineEditor)
        waitFor(hasText(pushUp.nameKo))
        val routine = routines.routines.value.single()
        assertEquals("가슴", routine.name)
        assertEquals(listOf(pushUp.id), routines.itemsOf(routine.id).map { it.exerciseId })
    }

    @Test
    fun doubleTapOnAddExerciseOpensOnePicker() {
        show()
        compose.onNodeWithText(text(R.string.routine_create)).performClick()
        waitFor(routineEditor)
        compose.onNode(hasSetTextAction()).performTextInput("등")

        doubleTap(button(R.string.routine_add_exercise))
        waitFor(hasTestTag(EXERCISE_LIST_TAG))
        back()

        waitFor(routineEditor)
        compose.onNode(routineEditor).assertIsDisplayed()
    }

    @Test
    fun doubleTapOnStartEmptyStartsOneWorkoutAndOneBackReturnsHome() {
        show()
        doubleTap(button(R.string.home_start_empty))
        waitFor(workoutScreen)
        compose.waitForIdle()
        assertNotNull(inProgressId())
        assertTrue(
            "진행 중인 운동 물음이 뜨지 않는다",
            compose.onAllNodes(hasText(text(R.string.home_in_progress_title))).fetchSemanticsNodes().isEmpty(),
        )

        back()
        waitFor(emptyHome)
        compose.onNode(emptyHome).assertIsDisplayed()
    }

    /** 운동 중 화면이 열린 뒤 홈의 "빈 운동 시작"이 한 번 더 눌려도 막 만든 운동을 버리고 새로 만들지 않는다. */
    @Test
    fun tapOnStartEmptyWhileTheWorkoutOpensKeepsTheSameWorkout() {
        show()
        // 두 번째 누르기 전에 홈이 막 시작한 운동(손대지 않은 운동)의 이어서 하기 카드를 그릴 때까지 기다린다.
        staleTap(button(R.string.home_start_empty), opened = workoutScreen, beforeSecondTap = hasText(text(R.string.home_resume)))
        waitFor(workoutScreen)

        assertEquals("처음 만든 운동이 그대로 진행 중이다", FIRST_WORKOUT_ID, inProgressId())
        back()
        waitFor(emptyHome)
        compose.onNode(emptyHome).assertIsDisplayed()
    }

    @Test
    fun doubleTapOnASettingsRowOpensOneScreenAndDoubleTapOnBackClosesOne() {
        show()
        compose.onNodeWithText(text(R.string.nav_settings)).performClick()
        waitFor(hasText(text(R.string.settings_licenses)))
        compose.onNode(hasText(text(R.string.settings_licenses))).performScrollTo()

        doubleTap(hasText(text(R.string.settings_licenses)) and hasClickAction())
        waitFor(hasText("Test Font"))
        doubleTap(hasText("Test Font") and hasClickAction())
        waitFor(hasText("Test Font license text.", substring = true))

        // 라이선스 한 건 → 라이선스 목록. 뒤로 가기를 두 번 눌러도 한 화면만 닫힌다.
        doubleTap(hasContentDescription(text(R.string.navigate_back)))
        waitFor(hasText(text(R.string.licenses_title)))
        compose.onNodeWithText("Test Library").assertIsDisplayed()
        back()
        waitFor(hasText(text(R.string.nav_home)))
    }

    private companion object {
        const val TIMEOUT = 5_000L

        /** 화면 전환(700ms)보다 짧게 잡는다. 이 안에 다음 화면이 나와야 전환 중에 누른 것이 된다. */
        const val MAX_FRAMES = 30
        const val FRAME_PAUSE_MS = 5L

        /** 빈 데이터베이스에서 처음 만든 운동의 식별자. */
        const val FIRST_WORKOUT_ID = 1L
    }
}
