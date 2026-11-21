package io.github.zzunsik.nextset.rest

import io.github.zzunsik.nextset.domain.rest.RestCue
import io.github.zzunsik.nextset.domain.rest.RestTimerEngine
import io.github.zzunsik.nextset.domain.rest.RestTimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 휴식 타이머가 서비스, 신호, 알림, 저장을 제때 부르는지. 바깥 일은 기록만 하는 가짜다. 시계는 가상 시간이다. */
@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerControllerTest {
    private class FakeEffects(var saved: RestTimerState? = null) : RestTimerEffects {
        val log = mutableListOf<String>()
        override fun startService() { log += "service" }
        override fun signal(cue: RestCue) { log += "signal:$cue" }
        override fun postDone(state: RestTimerState) { log += "done" }
        override fun cancelDone() { log += "cancelDone" }
        override fun released() { log += "released" }
        override fun save(state: RestTimerState?) { saved = state?.takeUnless { it.finished } }
        override fun load(): RestTimerState? = saved
    }

    private fun TestScope.controller(effects: FakeEffects) =
        RestTimerController(RestTimerEngine { testScheduler.currentTime }, backgroundScope, effects).also { runCurrent() }

    @Test
    fun startRunsServiceSignalsAndPostsDoneWhenAppIsHidden() = runTest {
        val effects = FakeEffects()
        val timer = controller(effects)
        timer.onAppHidden()
        timer.start(workoutId = 1, setId = 10, seconds = 30)
        runCurrent()
        assertEquals(listOf("cancelDone", "service"), effects.log)
        assertEquals(30_000L, effects.saved!!.remainingMillis(currentTime))

        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(
            listOf("signal:WARNING") + List(5) { "signal:COUNTDOWN" } + listOf("signal:END", "done"),
            effects.log.drop(2),
        )
        assertNull("끝난 휴식은 남기지 않는다", effects.saved)
        advanceTimeBy(2_000)
        runCurrent()
        assertNull(timer.state.value)
        assertEquals("released", effects.log.last())
    }

    @Test
    fun noDoneNotificationWhileAppIsOnScreen() = runTest {
        val effects = FakeEffects()
        val timer = controller(effects)
        timer.onAppVisible(serviceRunning = false)
        timer.start(1, 10, 15)
        advanceTimeBy(16_000)
        runCurrent()
        assertTrue(effects.log.contains("signal:END"))
        assertFalse(effects.log.contains("done"))
    }

    @Test
    fun cancelClearsRestAndDoneNotification() = runTest {
        val effects = FakeEffects()
        val timer = controller(effects)
        timer.start(1, 10, 60)
        runCurrent()
        effects.log.clear()
        timer.cancel()
        runCurrent()
        assertNull(timer.state.value)
        assertNull(effects.saved)
        assertEquals(listOf("cancelDone", "released"), effects.log)
    }

    @Test
    fun restoredRestRestartsServiceWhenAppBecomesVisible() = runTest {
        // 프로세스가 죽기 전에 남긴 휴식. 지금 시각(0) 기준 40초 남았다.
        val effects = FakeEffects(saved = RestTimerState(workoutId = 1, setId = 10, endsAt = 40_000, totalMillis = 90_000))
        val timer = controller(effects)
        assertEquals(40_000L, timer.state.value!!.remainingMillis(currentTime))

        timer.onAppVisible(serviceRunning = false)
        assertEquals(listOf("cancelDone", "service"), effects.log)
        timer.onAppVisible(serviceRunning = true)
        assertEquals("서비스가 돌고 있으면 다시 띄우지 않는다", listOf("cancelDone", "service", "cancelDone"), effects.log)

        advanceTimeBy(40_000)
        runCurrent()
        assertTrue(effects.log.contains("signal:END"))
    }

    @Test
    fun dismissedNotificationStaysHiddenUntilNextRest() = runTest {
        val timer = controller(FakeEffects())
        timer.start(1, 10, 60)
        timer.onOngoingDismissed()
        assertTrue(timer.ongoingDismissed)
        timer.adjust(15)
        assertTrue("시간을 바꿔도 다시 띄우지 않는다", timer.ongoingDismissed)
        timer.start(1, 11, 60)
        assertFalse(timer.ongoingDismissed)
    }

    @Test
    fun allowingNotificationsAsksServiceToRepost() = runTest {
        val timer = controller(FakeEffects())
        val before = timer.notificationRefreshes.value
        timer.onNotificationsAllowed()
        assertEquals(before + 1, timer.notificationRefreshes.value)
    }
}
