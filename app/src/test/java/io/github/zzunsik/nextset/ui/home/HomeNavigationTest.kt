package io.github.zzunsik.nextset.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.data.repository.TestDbRule
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.TestExercises.SQUAT
import io.github.zzunsik.nextset.data.repository.at
import io.github.zzunsik.nextset.ui.navigation.NextSetNavDisplay
import io.github.zzunsik.nextset.ui.settings.FakeBackupRepository
import io.github.zzunsik.nextset.ui.settings.FakeLicenseRepository
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 앱의 화면 이동 그대로 홈에서 운동을 시작하고 탭 사이를 오간다(HOME-01~HOME-06).
 * 저장소는 메모리 데이터베이스의 진짜 구현이다. 루틴 이름은 지어낸 것이다.
 */
@RunWith(RobolectricTestRunner::class)
class HomeNavigationTest {
    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = compose.activity

    /** 메모리 데이터베이스. Compose 규칙보다 바깥이라 화면이 사라진 뒤에 닫힌다. */
    @get:Rule(order = 0)
    internal val dbRule = TestDbRule(seed = true)

    private val db get() = dbRule.db

    private fun show() {
        compose.setContent {
            NextSetTheme {
                NextSetNavDisplay(
                    settingsRepository = db.settings,
                    licenseRepository = FakeLicenseRepository(),
                    exerciseRepository = db.exercises,
                    workoutRepository = db.workouts,
                    routineRepository = db.routines,
                    backupRepository = FakeBackupRepository(),
                )
            }
        }
    }

    private fun text(id: Int, vararg args: Any) = app.getString(id, *args)

    private fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(matcher.description, TIMEOUT) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    /** 목록은 화면에 보이는 만큼만 그리므로 아래 항목은 내려서 본다. */
    private fun scrollTo(matcher: SemanticsMatcher) {
        waitFor(hasTestTag(HOME_LIST_TAG))
        compose.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(matcher)
    }

    private fun inProgress() = runBlocking { db.workouts.observeInProgressWorkout().first() }

    private fun routine(name: String, exerciseId: String): Long = runBlocking {
        db.routines.addRoutine(name).also { db.routines.addRoutineExercise(it, exerciseId, 2) }
    }

    /** 운동 중 화면이 떠 있다. 첫 종목의 첫 세트 체크 버튼으로 알아본다. */
    private fun waitForWorkout(exerciseName: String) = waitFor(hasContentDescription("$exerciseName 1세트 완료"))

    // HOME-01

    @Test
    fun oneTapOnTheTopCardOpensTheWorkoutWithNothingInBetween() {
        val chest = routine("가슴 루틴", BENCH)
        routine("등 루틴", PULL_UP)
        show()
        waitFor(hasContentDescription(text(R.string.home_start_routine_description, "가슴 루틴")))
        compose.onNodeWithText(text(R.string.nav_history)).assertExists()

        compose.onNodeWithContentDescription(text(R.string.home_start_routine_description, "가슴 루틴")).performClick()

        // 누른 바로 다음 화면이 운동 중 화면이다. 운동 기록은 이미 저장돼 있고 하단 내비게이션은 숨는다.
        waitForWorkout("테스트 벤치")
        assertEquals(chest, inProgress()?.routineId)
        compose.onNodeWithText(text(R.string.nav_history)).assertDoesNotExist()

        // 뒤로 가면 바로 홈이다. 맨 위가 이어서 하기 카드로 바뀐다(HOME-06).
        compose.runOnUiThread { app.onBackPressedDispatcher.onBackPressed() }
        waitFor(hasText(text(R.string.home_resume_label)))
        compose.onNodeWithText(text(R.string.nav_history)).assertExists()
    }

    // HOME-04

    @Test
    fun oneTapOnAListRowStartsThatRoutine() {
        routine("가슴 루틴", BENCH)
        val back = routine("등 루틴", PULL_UP)
        show()
        waitFor(hasText("가슴 루틴"))
        scrollTo(hasText("등 루틴"))
        compose.onNodeWithText("등 루틴").performClick()

        waitForWorkout("테스트 풀업")
        assertEquals(back, inProgress()?.routineId)
    }

    // HOME-05

    @Test
    fun oneTapOnEmptyWorkoutStartsIt() {
        show()
        waitFor(hasText(text(R.string.home_empty)))
        scrollTo(hasText(text(R.string.home_start_empty)))
        compose.onNodeWithText(text(R.string.home_start_empty)).performClick()

        waitFor(hasText(text(R.string.workout_empty)))
        val workout = inProgress()
        assertNotNull(workout)
        assertEquals(null, workout?.routineId)
    }

    // HOME-03

    @Test
    fun pinnedRoutineMovesToTheTop() {
        routine("가슴 루틴", BENCH)
        routine("등 루틴", PULL_UP)
        val legs = routine("하체 루틴", SQUAT)
        show()
        waitFor(hasText("가슴 루틴"))
        scrollTo(hasContentDescription(text(R.string.home_routine_more, "하체 루틴")))
        compose.onNodeWithContentDescription(text(R.string.home_routine_more, "하체 루틴")).performClick()
        compose.onNodeWithText(text(R.string.home_pin)).performClick()

        compose.onNodeWithTag(HOME_LIST_TAG).performScrollToIndex(0)
        waitFor(hasText(text(R.string.home_pinned_label)))
        compose.onNodeWithContentDescription(text(R.string.home_start_routine_description, "하체 루틴")).assertExists()
        assertEquals(legs, runBlocking { db.settings.observeSettings().first().pinnedRoutineId })

        compose.onNodeWithText(text(R.string.home_unpin)).performClick()
        waitFor(hasText(text(R.string.home_next_label)))
        compose.onNodeWithContentDescription(text(R.string.home_start_routine_description, "가슴 루틴")).assertExists()
    }

    // HOME-06과 HOME-01의 예외

    @Test
    fun startingAnotherRoutineWhileInProgressAsksFirst() {
        val chest = routine("가슴 루틴", BENCH)
        val back = routine("등 루틴", PULL_UP)
        val oldId = runBlocking {
            val id = db.workouts.startFromRoutine(chest, at(0))
            val set = db.workouts.observeWorkout(id).first()!!.exercises.first().sets.first()
            db.workouts.completeSet(set.id, at(1))
            id
        }
        show()
        waitFor(hasText(text(R.string.home_resume_label)))
        scrollTo(hasText("등 루틴"))
        compose.onNodeWithText("등 루틴").performClick()

        waitFor(hasText(text(R.string.home_in_progress_title)))
        compose.onNodeWithText(text(R.string.home_in_progress_detail, "가슴 루틴", 1)).assertExists()
        compose.onNodeWithText(text(R.string.home_finish_and_start)).performClick()

        waitForWorkout("테스트 풀업")
        assertEquals(back, inProgress()?.routineId)
        val old = runBlocking { db.workouts.observeWorkout(oldId).first()!! }
        assertNotNull(old.finishedAt)
    }

    // 빈 상태와 RTN-01

    @Test
    fun emptyStateCreatesARoutineAndOpensItsEditor() {
        show()
        waitFor(hasText(text(R.string.home_empty)))
        compose.onNodeWithText(text(R.string.routine_create)).performClick()

        waitFor(hasText(text(R.string.routine_no_exercises)))
        assertEquals(listOf(text(R.string.routine_new)), runBlocking { db.routines.observeRoutines().first() }.map { it.name })
    }

    // 하단 내비게이션(docs/DESIGN.md "이동 구조")

    @Test
    fun bottomNavigationSwitchesTabsAndBackReturnsHome() {
        show()
        waitFor(hasText(text(R.string.home_empty)))

        compose.onNodeWithText(text(R.string.nav_history)).performClick()
        waitFor(hasText(text(R.string.history_placeholder)))

        compose.onNodeWithText(text(R.string.nav_settings)).performClick()
        waitFor(hasText(text(R.string.settings_default_rest)))
        compose.onNodeWithText(text(R.string.history_placeholder)).assertDoesNotExist()

        // 탭 화면에서 뒤로 가면 홈이다.
        compose.runOnUiThread { app.onBackPressedDispatcher.onBackPressed() }
        waitFor(hasText(text(R.string.home_empty)))

        compose.onNodeWithText(text(R.string.nav_history)).performClick()
        waitFor(hasText(text(R.string.history_placeholder)))
        compose.onNodeWithText(text(R.string.nav_home)).performClick()
        waitFor(hasText(text(R.string.home_empty)))
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
