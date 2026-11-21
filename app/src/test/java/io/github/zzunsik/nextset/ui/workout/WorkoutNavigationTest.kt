package io.github.zzunsik.nextset.ui.workout

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithTag
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.data.repository.TestDbRule
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.wr
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.ui.exercise.EXERCISE_LIST_TAG
import io.github.zzunsik.nextset.ui.navigation.NextSetNavDisplay
import io.github.zzunsik.nextset.ui.settings.FakeBackupRepository
import io.github.zzunsik.nextset.ui.settings.FakeLicenseRepository
import io.github.zzunsik.nextset.ui.settings.FakeSettingsRepository
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * 홈의 운동 입구에서 운동 중 화면, 종목 고르기, 운동 요약까지 앱의 화면 이동 그대로 지나간다.
 * 저장소는 메모리 데이터베이스의 진짜 구현이다.
 */
@RunWith(RobolectricTestRunner::class)
class WorkoutNavigationTest {
    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    /** 메모리 데이터베이스. Compose 규칙보다 바깥이라 화면이 사라진 뒤에 닫힌다. */
    @get:Rule(order = 0)
    internal val dbRule = TestDbRule(seed = true)

    private val db get() = dbRule.db

    private fun show() {
        compose.setContent {
            NextSetTheme {
                NextSetNavDisplay(
                    settingsRepository = FakeSettingsRepository(),
                    licenseRepository = FakeLicenseRepository(),
                    exerciseRepository = db.exercises,
                    workoutRepository = db.workouts,
                    routineRepository = db.routines,
                    backupRepository = FakeBackupRepository(),
                )
            }
        }
    }

    private fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(matcher.description, TIMEOUT) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun text(id: Int, vararg args: Any) = app.getString(id, *args)

    private fun inProgress() = runBlocking { db.workouts.observeInProgressWorkout().first() }

    @Test
    fun emptyWorkoutFromHomeToDone() {
        show()
        waitFor(hasText(text(R.string.home_start_empty)))
        compose.onNodeWithText(text(R.string.home_start_empty)).performClick()

        // 빈 운동의 빈 상태에서 종목 고르기로 간다.
        waitFor(hasText(text(R.string.workout_empty)))
        compose.onNodeWithText(text(R.string.workout_empty_title)).assertExists()
        compose.onNodeWithText(text(R.string.workout_add_exercise)).performClick()
        waitFor(hasText("테스트 벤치"))
        compose.onNodeWithTag(EXERCISE_LIST_TAG).performScrollToNode(hasText("테스트 벤치"))
        compose.onNodeWithText("테스트 벤치").performClick()
        compose.onNodeWithText(text(R.string.exercise_add_selected, 1)).performClick()

        // 고른 종목이 세트 하나와 함께 더해진다. 빈 칸이라 체크를 누르면 숫자판이 열린다.
        waitFor(hasContentDescription("테스트 벤치 1세트 완료"))
        compose.onNodeWithContentDescription("테스트 벤치 1세트 완료").performClick()
        waitFor(hasContentDescription(text(R.string.number_pad_close)))
        listOf("4", "0").forEach { compose.onNodeWithText(it).performClick() }
        compose.onNodeWithText(text(R.string.number_pad_next)).performClick()
        compose.onNodeWithText("5").performClick()
        compose.onNodeWithText(text(R.string.number_pad_done)).performClick()

        // 모든 세트를 끝내 운동 끝내기가 나온다. 빈 운동이라 루틴 반영을 묻지 않는다.
        waitFor(hasText(text(R.string.next_set_finish)))
        compose.onNodeWithText(text(R.string.next_set_finish)).performClick()
        waitFor(hasText(text(R.string.workout_done_title)))
        // 운동 요약(#27): 숫자 셋 가운데 볼륨이 40kg × 5회다.
        compose.onNodeWithText(text(R.string.report_volume_value, "200")).assertExists()
        compose.onNodeWithText(text(R.string.action_confirm)).performClick()

        waitFor(hasText(text(R.string.home_start_empty)))
        assertNull(inProgress())
        val history = runBlocking { db.workouts.observeHistory().first() }
        assertEquals(1, history.size)
        val saved = runBlocking { db.workouts.observeWorkout(history.single().id).first()!! }
        assertEquals(wr(40_000, 5), saved.exercises.single().sets.single().value)
    }

    @Test
    fun backKeepsTheWorkoutAndHomeResumesIt() {
        runBlocking {
            val routineId = db.routines.addRoutine("테스트 루틴")
            db.routines.addRoutineExercise(routineId, BENCH, 2, Target(weight = Weight.kg(50), repsMin = 10))
            db.routines.addRoutineExercise(routineId, PULL_UP, 1)
        }
        show()
        waitFor(hasContentDescription(text(R.string.home_start_routine_description, "테스트 루틴")))
        compose.onNodeWithContentDescription(text(R.string.home_start_routine_description, "테스트 루틴")).performClick()

        waitFor(hasContentDescription("테스트 벤치 1세트 완료"))
        compose.onNodeWithContentDescription("테스트 벤치 1세트 완료").performClick()
        compose.waitUntil(TIMEOUT) { inProgress()?.exercises?.first()?.sets?.first()?.isCompleted == true }

        // 뒤로 가기는 홈으로 간다. 운동은 계속된다.
        compose.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()
        waitFor(hasText(text(R.string.home_resume)))
        compose.onNodeWithText(text(R.string.home_resume_progress, 3, 1), substring = true).assertExists()
        assertNotNull(inProgress())

        compose.onNodeWithText(text(R.string.home_resume)).performClick()
        // 다음 세트는 벤치 둘째 세트다. 표시는 라벨과 값을 한 이름으로 읽힌다.
        waitFor(hasContentDescription(text(R.string.next_set_label, 2, 2), substring = true))

        // 버리면 홈으로 가고 이어서 하기가 사라진다.
        compose.onNodeWithContentDescription(text(R.string.workout_more)).performClick()
        compose.onNodeWithText(text(R.string.workout_discard)).performClick()
        compose.onNodeWithText(text(R.string.finish_discard)).performClick()
        waitFor(hasText(text(R.string.home_start_empty)))
        assertNull(inProgress())
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
