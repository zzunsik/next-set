package io.github.zzunsik.nextset.rest

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.zzunsik.nextset.domain.rest.RestTimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 프로세스가 죽은 뒤에도 끝나는 시각이 남는지(REST-06, docs/rest-timer.md "타이머의 운명"). */
@RunWith(RobolectricTestRunner::class)
class RestTimerStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val state = RestTimerState(workoutId = 3, setId = 30, endsAt = 70_000, totalMillis = 90_000, nextSet = "턱걸이 8회")

    @Test
    fun newStoreReadsWhatTheOldProcessSaved() {
        RestTimerStore(context).save(state, nowElapsed = 10_000, nowWall = 5_000_000)
        // 새 프로세스: 같은 파일을 새 객체로 읽는다. 30초가 흘렀다.
        val restored = RestTimerStore(context).load(nowElapsed = 40_000, nowWall = 5_030_000)
        assertEquals(state, restored)
        assertEquals(30_000L, restored!!.remainingMillis(40_000))
    }

    @Test
    fun afterRebootTheWallClockDecides() {
        RestTimerStore(context).save(state, nowElapsed = 10_000, nowWall = 5_000_000)
        // 재부팅으로 단조 시계가 다시 0부터 센다. 벽시계로는 끝난 지 오래다.
        val restored = RestTimerStore(context).load(nowElapsed = 2_000, nowWall = 5_600_000)
        assertEquals(-540_000L, restored!!.endsAt - 2_000)
    }

    @Test
    fun clearedWhenRestEnds() {
        val store = RestTimerStore(context)
        store.save(state, 10_000, 5_000_000)
        store.save(null, 20_000, 5_010_000)
        assertNull(store.load(20_000, 5_010_000))
        store.save(state, 10_000, 5_000_000)
        store.save(state.copy(finished = true), 70_000, 5_060_000)
        assertNull(store.load(70_000, 5_060_000))
    }
}
