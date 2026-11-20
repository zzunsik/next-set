package io.github.zzunsik.nextset.ui.workout

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.zzunsik.nextset.domain.rest.RestTimer
import io.github.zzunsik.nextset.domain.rest.RestTimerEngine
import io.github.zzunsik.nextset.domain.rest.RestTimerState
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 운동 중 화면과 휴식 타이머의 연결(REST-01~REST-03). 값은 지어낸 것이다. */
@RunWith(RobolectricTestRunner::class)
class WorkoutRestTest {
    @get:Rule
    val compose = createComposeRule()

    // ---- 시작할 시간(REST-01, REST-03) ----

    @Test
    fun exerciseRestTimeWinsOverDefault() {
        val state = WorkoutSamples.typical().copy(defaultRestSeconds = 150)
        val set = state.exercises[0].sets[2].id
        assertEquals(90, restSecondsToStart(WorkoutEvent.SetCompleted(set, 1, restSeconds = 90), state))
    }

    @Test
    fun defaultIsUsedWhenExerciseHasNoRestTime() {
        val state = WorkoutSamples.typical().copy(defaultRestSeconds = 150)
        val set = state.exercises[0].sets[2].id
        assertEquals(150, restSecondsToStart(WorkoutEvent.SetCompleted(set, 1, restSeconds = null), state))
    }

    @Test
    fun lastSetOfWorkoutStartsNoRest() {
        val state = WorkoutSamples.allDone()
        val last = state.exercises[1].sets[1].id
        assertNull(restSecondsToStart(WorkoutEvent.SetCompleted(last, 2, null), state))
        // 완료 직후 아직 화면 상태에 반영되지 않았어도(그 세트가 미완료로 보여도) 마지막이면 시작하지 않는다.
        val stale = state.copy(
            exercises = state.exercises.map { block ->
                block.copy(sets = block.sets.map { if (it.id == last) it.copy(completed = false) else it })
            },
        )
        assertNull(restSecondsToStart(WorkoutEvent.SetCompleted(last, 2, null), stale))
    }

    @Test
    fun remainingTimeRoundsUp() {
        assertEquals("1:45", remainingSecondsText(104_001))
        assertEquals("1:45", remainingSecondsText(105_000))
        assertEquals("0:00", remainingSecondsText(0))
        assertEquals("0:01", remainingSecondsText(1))
    }

    // ---- 하단 바(REST-02) ----

    @Test
    fun barButtonsAdjustTheTimer() {
        var now = 0L
        val engine = RestTimerEngine { now }
        val timer = EngineTimer(engine)
        engine.start(workoutId = 1, setId = 10, seconds = 120)
        compose.setContent {
            NextSetTheme {
                WorkoutRestBar(timer, workoutId = 1, compact = false, showNotificationButton = false, onEnableNotifications = {})
            }
        }
        compose.onNodeWithText("2:00").assertExists()
        compose.onNodeWithText("+15초").performClick()
        assertEquals(135_000L, engine.state.value!!.remainingMillis(now))
        compose.onNodeWithText("−15초").performClick()
        compose.onNodeWithText("−15초").performClick()
        assertEquals(105_000L, engine.state.value!!.remainingMillis(now))
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithText("1:45").assertExists()
        compose.onNodeWithContentDescription("휴식 건너뛰기").performClick()
        assertNull(engine.state.value)
    }

    @Test
    fun increaseIsDisabledAtTenMinutes() {
        compose.setContent {
            NextSetTheme {
                RestTimerBar(
                    remainingMillis = 600_000,
                    progress = 1f,
                    nextSet = "스쿼트 100kg × 5",
                    compact = false,
                    canIncrease = false,
                    onDecrease = {},
                    onIncrease = {},
                    onSkip = {},
                )
            }
        }
        compose.onNodeWithText("+15초").assertIsNotEnabled()
        compose.onNodeWithText("−15초").assertIsEnabled()
        compose.onNodeWithText("다음 스쿼트 100kg × 5").assertExists()
    }

    @Test
    fun notificationButtonCallsBack() {
        var asked = 0
        compose.setContent {
            NextSetTheme {
                RestTimerBar(
                    remainingMillis = 60_000,
                    progress = 0.5f,
                    nextSet = null,
                    compact = false,
                    showNotificationButton = true,
                    onEnableNotifications = { asked++ },
                    onDecrease = {},
                    onIncrease = {},
                    onSkip = {},
                )
            }
        }
        compose.onNodeWithText("알림 켜기").performClick()
        assertEquals(1, asked)
    }

    @Test
    fun barIsHiddenForAnotherWorkout() {
        val engine = RestTimerEngine { 0L }
        engine.start(workoutId = 2, setId = 10, seconds = 60)
        compose.setContent {
            NextSetTheme {
                WorkoutRestBar(EngineTimer(engine), workoutId = 1, compact = false, showNotificationButton = false, onEnableNotifications = {})
            }
        }
        compose.onNodeWithText("1:00").assertDoesNotExist()
    }

    /** 엔진만 쓰는 타이머. 서비스와 알림이 없다. */
    private class EngineTimer(private val engine: RestTimerEngine) : RestTimer {
        override val state: StateFlow<RestTimerState?> = engine.state
        override fun now() = engine.now()
        override fun start(workoutId: Long, setId: Long, seconds: Int) = engine.start(workoutId, setId, seconds)
        override fun adjust(deltaSeconds: Int) = engine.adjust(deltaSeconds)
        override fun skip() = engine.skip()
        override fun stopForSet(setId: Long) = engine.stopForSet(setId)
        override fun cancel() = engine.skip()
        override fun setNextSet(text: String?) = engine.setNextSet(text)
        override fun onNotificationsAllowed() = Unit
    }
}
