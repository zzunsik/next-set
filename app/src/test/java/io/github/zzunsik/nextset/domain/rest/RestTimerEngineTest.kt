package io.github.zzunsik.nextset.domain.rest

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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

/**
 * 휴식 타이머의 신호 시각과 시간 조절(REST-01, REST-02, REST-04). 가짜 시계(가상 시간)로 돌려 실제로 기다리지 않는다.
 * 시각은 휴식을 시작한 순간을 0으로 한 밀리초다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerEngineTest {
    private data class Fired(val atMillis: Long, val cue: RestCue)

    private class Harness(val scope: TestScope) {
        val engine = RestTimerEngine { scope.testScheduler.currentTime }
        val fired = mutableListOf<Fired>()
        val finished = mutableListOf<Long>()

        fun run() {
            scope.backgroundScope.launch {
                engine.runCues(
                    signal = { fired += Fired(scope.testScheduler.currentTime, it) },
                    onFinished = { finished += scope.testScheduler.currentTime },
                )
            }
            scope.runCurrent()
        }
    }

    private fun harness(scope: TestScope) = Harness(scope).also { it.run() }

    @Test
    fun cuesAtTenSecondsFiveToOneAndEnd() = runTest {
        val h = harness(this)
        h.engine.start(workoutId = 1, setId = 10, seconds = 60)
        advanceTimeBy(70_000)
        runCurrent()

        assertEquals(
            listOf(
                Fired(50_000, RestCue.WARNING),
                Fired(55_000, RestCue.COUNTDOWN),
                Fired(56_000, RestCue.COUNTDOWN),
                Fired(57_000, RestCue.COUNTDOWN),
                Fired(58_000, RestCue.COUNTDOWN),
                Fired(59_000, RestCue.COUNTDOWN),
                Fired(60_000, RestCue.END),
            ),
            h.fired,
        )
        assertEquals(listOf(60_000L), h.finished)
    }

    @Test
    fun endShowsZeroForTwoSecondsThenClears() = runTest {
        val h = harness(this)
        h.engine.start(1, 10, 30)
        advanceTimeBy(30_000)
        runCurrent()
        val state = h.engine.state.value!!
        assertTrue(state.finished)
        assertEquals(0L, state.remainingMillis(currentTime))
        assertFalse("끝난 뒤에는 늘릴 수 없다", state.canIncrease(currentTime))

        advanceTimeBy(RestTimerRules.FINISHED_DISPLAY_MILLIS - 1)
        runCurrent()
        assertTrue(h.engine.state.value!!.finished)
        advanceTimeBy(1)
        runCurrent()
        assertNull(h.engine.state.value)
    }

    @Test
    fun increaseMovesRemainingAndCues() = runTest {
        val h = harness(this)
        h.engine.start(1, 10, 30)
        advanceTimeBy(20_000)
        runCurrent()
        // 10초 전 신호가 막 났다. 15초 늘리면 남은 시간이 25초가 되고 10초 전 신호가 다시 다가온다.
        assertEquals(listOf(Fired(20_000, RestCue.WARNING)), h.fired)
        h.engine.adjust(RestTimerRules.STEP_SECONDS)
        assertEquals(25_000L, h.engine.state.value!!.remainingMillis(currentTime))
        advanceTimeBy(30_000)
        runCurrent()

        assertEquals(
            listOf(20_000L, 35_000L, 40_000L, 41_000L, 42_000L, 43_000L, 44_000L, 45_000L),
            h.fired.map { it.atMillis },
        )
        assertEquals(RestCue.END, h.fired.last().cue)
    }

    @Test
    fun decreaseSkipsCuesThatAlreadyPassed() = runTest {
        val h = harness(this)
        h.engine.start(1, 10, 30)
        advanceTimeBy(12_000)
        runCurrent()
        // 남은 18초에서 15초 줄이면 3초. 10초 전, 5초 전, 4초 전 신호는 지났으므로 내지 않는다(제안).
        h.engine.adjust(-RestTimerRules.STEP_SECONDS)
        assertEquals(3_000L, h.engine.state.value!!.remainingMillis(currentTime))
        advanceTimeBy(10_000)
        runCurrent()

        assertEquals(
            listOf(
                Fired(12_000, RestCue.COUNTDOWN),
                Fired(13_000, RestCue.COUNTDOWN),
                Fired(14_000, RestCue.COUNTDOWN),
                Fired(15_000, RestCue.END),
            ),
            h.fired,
        )
    }

    @Test
    fun decreaseAtOrBelowStepEndsWithoutSignal() = runTest {
        val h = harness(this)
        h.engine.start(1, 10, 30)
        advanceTimeBy(16_000)
        runCurrent()
        h.engine.adjust(-RestTimerRules.STEP_SECONDS)
        runCurrent()
        assertNull("남은 14초에서 15초를 줄이면 휴식이 끝난다", h.engine.state.value)
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(emptyList<Fired>(), h.fired)
        assertEquals(emptyList<Long>(), h.finished)
    }

    @Test
    fun skipStopsWithoutCuesOrDoneNotice() = runTest {
        val h = harness(this)
        h.engine.start(1, 10, 120)
        advanceTimeBy(52_000)
        h.engine.skip()
        advanceTimeBy(120_000)
        runCurrent()
        assertNull(h.engine.state.value)
        assertEquals(emptyList<Fired>(), h.fired)
        assertEquals(emptyList<Long>(), h.finished)
    }

    @Test
    fun completingAnotherSetRestartsWithItsTime() = runTest {
        val h = harness(this)
        h.engine.start(1, setId = 10, seconds = 120)
        advanceTimeBy(115_000)
        runCurrent()
        assertEquals(listOf(Fired(110_000, RestCue.WARNING), Fired(115_000, RestCue.COUNTDOWN)), h.fired)

        h.engine.start(1, setId = 11, seconds = 90)
        val state = h.engine.state.value!!
        assertEquals(11L, state.setId)
        assertEquals(90_000L, state.remainingMillis(currentTime))
        advanceTimeBy(100_000)
        runCurrent()
        // 새 휴식은 115초에 시작해 205초에 끝난다. 앞 휴식의 남은 카운트다운(116~119초)은 나지 않는다.
        assertEquals(
            listOf(
                Fired(195_000, RestCue.WARNING),
                Fired(200_000, RestCue.COUNTDOWN),
                Fired(201_000, RestCue.COUNTDOWN),
                Fired(202_000, RestCue.COUNTDOWN),
                Fired(203_000, RestCue.COUNTDOWN),
                Fired(204_000, RestCue.COUNTDOWN),
                Fired(205_000, RestCue.END),
            ),
            h.fired.drop(2),
        )
        assertEquals(listOf(115_000L + 90_000), h.finished)
    }

    @Test
    fun increaseStopsAtTenMinutes() = runTest {
        val h = harness(this)
        h.engine.start(1, 10, 590)
        assertTrue(h.engine.state.value!!.canIncrease(currentTime))
        h.engine.adjust(RestTimerRules.STEP_SECONDS)
        val state = h.engine.state.value!!
        assertEquals(RestTimerRules.MAX_REMAINING_MILLIS, state.remainingMillis(currentTime))
        assertFalse(state.canIncrease(currentTime))
        assertEquals("진행 막대는 늘어난 시간을 전체로 본다", 1f, state.progress(currentTime))
    }

    @Test
    fun restShorterThanTenSecondsGivesOnlyRemainingCues() = runTest {
        val h = harness(this)
        h.engine.start(1, 10, 3)
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(listOf(0L, 1_000L, 2_000L, 3_000L), h.fired.map { it.atMillis })
        assertEquals(RestCue.END, h.fired.last().cue)
    }

    @Test
    fun uncompletingOnlyStopsTheRestItStarted() = runTest {
        val h = harness(this)
        h.engine.start(1, setId = 10, seconds = 60)
        h.engine.stopForSet(11)
        assertEquals(10L, h.engine.state.value!!.setId)
        h.engine.stopForSet(10)
        assertNull(h.engine.state.value)
    }

    @Test
    fun nextSetChangeDoesNotRepeatCues() = runTest {
        val h = harness(this)
        h.engine.start(1, 10, 30)
        advanceTimeBy(20_000)
        runCurrent()
        h.engine.setNextSet("벤치 프레스 60kg × 10")
        runCurrent()
        assertEquals(listOf(Fired(20_000, RestCue.WARNING)), h.fired)
        assertEquals("벤치 프레스 60kg × 10", h.engine.state.value!!.nextSet)
    }

    @Test
    fun restoreKeepsEndTimeAndDropsFinishedRest() = runTest {
        val h = harness(this)
        val saved = RestTimerState(workoutId = 1, setId = 10, endsAt = 45_000, totalMillis = 60_000)
        assertTrue(h.engine.restore(saved))
        advanceTimeBy(50_000)
        runCurrent()
        assertEquals(45_000L, h.finished.single())

        val other = harness(this)
        assertFalse("끝나는 시각이 지난 휴식은 되살리지 않는다", other.engine.restore(saved))
        assertNull(other.engine.state.value)
    }
}
