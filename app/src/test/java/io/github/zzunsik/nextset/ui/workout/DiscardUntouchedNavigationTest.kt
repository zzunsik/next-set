package io.github.zzunsik.nextset.ui.workout

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.data.repository.TestDbRule
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.ui.home.HOME_LIST_TAG
import io.github.zzunsik.nextset.ui.navigation.NextSetNavDisplay
import io.github.zzunsik.nextset.ui.settings.FakeBackupRepository
import io.github.zzunsik.nextset.ui.settings.FakeLicenseRepository
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

/**
 * 손대지 않은 운동은 운동 중 화면을 뒤로 가기로 나가면 버리고, 손댄 운동은 남아 홈이 묻는다(docs/DESIGN.md
 * "한 번 눌러 시작", "진행 중인 운동이 있을 때 다른 루틴을 시작하려 하면", #73). 앱의 화면 이동 그대로 지나간다.
 * 저장소는 메모리 데이터베이스의 진짜 구현이고 루틴 이름은 지어낸 것이다.
 */
@RunWith(RobolectricTestRunner::class)
class DiscardUntouchedNavigationTest {
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

    private fun inProgress() = runBlocking { db.workouts.observeInProgressWorkout().first() }

    private fun routine(name: String, exerciseId: String): Long = runBlocking {
        // 목표를 둬 세트 값이 미리 채워진다. 체크 한 번으로 완료할 수 있다.
        db.routines.addRoutine(name).also { db.routines.addRoutineExercise(it, exerciseId, 2, Target(weight = Weight.kg(50), repsMin = 10)) }
    }

    private fun startTopRoutine(name: String) {
        waitFor(hasContentDescription(text(R.string.home_start_routine_description, name)))
        compose.onNodeWithContentDescription(text(R.string.home_start_routine_description, name)).performClick()
        waitFor(hasContentDescription("테스트 벤치 1세트 완료"))
    }

    private fun systemBack() = compose.runOnUiThread { app.onBackPressedDispatcher.onBackPressed() }

    @Test
    fun systemBackFromAnUntouchedWorkoutLeavesNothingInProgress() {
        routine("가슴 루틴", BENCH)
        show()
        startTopRoutine("가슴 루틴")
        assertNotNull(inProgress())

        systemBack()
        // 홈 맨 위는 이어서 하기가 아니라 다음 차례 루틴 카드다.
        waitFor(hasText(text(R.string.home_next_label)))
        assertNull(inProgress())
        compose.onNodeWithText(text(R.string.home_resume)).assertDoesNotExist()
    }

    @Test
    fun topBarBackFromAnUntouchedWorkoutAlsoDiscardsIt() {
        routine("가슴 루틴", BENCH)
        show()
        startTopRoutine("가슴 루틴")

        compose.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()
        waitFor(hasText(text(R.string.home_next_label)))
        assertNull(inProgress())
    }

    @Test
    fun aTouchedWorkoutStaysAndHomeAsksBeforeStartingAnother() {
        val chest = routine("가슴 루틴", BENCH)
        routine("등 루틴", PULL_UP)
        show()
        startTopRoutine("가슴 루틴")
        compose.onNodeWithContentDescription("테스트 벤치 1세트 완료").performClick()
        compose.waitUntil(TIMEOUT) { inProgress()?.exercises?.first()?.sets?.first()?.isCompleted == true }

        systemBack()
        waitFor(hasText(text(R.string.home_resume)))
        assertEquals(chest, inProgress()?.routineId)

        compose.onNodeWithTag(HOME_LIST_TAG).performScrollToNode(hasText("등 루틴"))
        compose.onNodeWithText("등 루틴").performClick()
        waitFor(hasText(text(R.string.home_in_progress_title)))
        assertEquals("묻는 동안 새 운동을 시작하지 않는다", chest, inProgress()?.routineId)
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
