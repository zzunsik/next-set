package io.github.zzunsik.nextset.ui.history

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.data.repository.TestDbRule
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.finishedWorkout
import io.github.zzunsik.nextset.data.repository.wr
import io.github.zzunsik.nextset.ui.home.HOME_LIST_TAG
import io.github.zzunsik.nextset.ui.navigation.NextSetNavDisplay
import io.github.zzunsik.nextset.ui.settings.FakeBackupRepository
import io.github.zzunsik.nextset.ui.settings.FakeLicenseRepository
import io.github.zzunsik.nextset.ui.settings.FakeSettingsRepository
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** 하단 내비게이션의 "기록" 탭에서 기록 목록, 기록 상세, 지우기까지 앱의 화면 이동 그대로 지나간다(HIST-01, HIST-05). */
@RunWith(RobolectricTestRunner::class)
class HistoryNavigationTest {
    @get:Rule(order = 1)
    val compose = createComposeRule()

    @get:Rule(order = 0)
    internal val dbRule = TestDbRule(seed = true)

    private val db get() = dbRule.db
    private val app = RuntimeEnvironment.getApplication()

    private fun text(id: Int, vararg args: Any) = app.getString(id, *args)

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

    @Test
    fun emptyHistoryLeadsBackHome() {
        show()
        waitFor(hasTestTag(HOME_LIST_TAG))
        compose.onNodeWithText(text(R.string.nav_history)).performClick()
        waitFor(hasText(text(R.string.history_empty)))
        compose.onNodeWithText(text(R.string.history_go_home)).performClick()
        waitFor(hasTestTag(HOME_LIST_TAG))
    }

    @Test
    fun openADetailAndDeleteIt() {
        runBlocking {
            db.finishedWorkout(Instant.parse("2026-03-09T09:00:00Z"), mapOf(BENCH to listOf(wr(60_000, 10))))
            db.finishedWorkout(Instant.parse("2026-03-14T09:00:00Z"), mapOf(BENCH to listOf(wr(62_500, 8), wr(62_500, 8))))
        }
        show()
        waitFor(hasTestTag(HOME_LIST_TAG))
        compose.onNodeWithText(text(R.string.nav_history)).performClick()

        // 최근 기록이 위다. 둘째 기록은 62.5 × 8 두 세트, 볼륨 1,000kg, 최고 기록 배지가 있다.
        val newest = text(R.string.history_row_detail, text(R.string.workout_duration_hours_minutes, 1, 0), 2, "1,000kg")
        waitFor(hasText(newest))
        compose.onNodeWithText(text(R.string.history_row_detail, text(R.string.workout_duration_hours_minutes, 1, 0), 1, "600kg"))
            .assertExists()
        compose.onNodeWithText(newest).performClick()

        // 기록 상세: 최고 기록 목록과 숫자 셋.
        waitFor(hasText(text(R.string.report_records_title)))
        compose.onNodeWithText("62.5kg × 8회").assertExists()

        compose.onNodeWithContentDescription(text(R.string.report_more)).performClick()
        compose.onNodeWithText(text(R.string.action_delete)).performClick()
        compose.onNodeWithText(text(R.string.report_delete_confirm)).assertExists()
        compose.onNodeWithText(text(R.string.action_delete)).performClick()

        // 목록으로 돌아오고 지운 기록이 빠진다.
        waitFor(hasText(text(R.string.history_add_past)))
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasText(newest)).fetchSemanticsNodes().isEmpty() }
        assertEquals(1, runBlocking { db.workouts.observeHistory().first().size })

        // 홈으로 돌아가 기록 화면의 화면 상태를 정리한다. 남겨 두면 데이터베이스를 닫은 뒤에도 읽으려 한다.
        compose.onNodeWithText(text(R.string.nav_home)).performClick()
        waitFor(hasTestTag(HOME_LIST_TAG))
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
