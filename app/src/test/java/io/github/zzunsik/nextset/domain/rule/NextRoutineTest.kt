package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.Routine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextRoutineTest {
    private val a = Routine(id = 10, name = "A", position = 0)
    private val b = Routine(id = 20, name = "B", position = 1)
    private val c = Routine(id = 30, name = "C", position = 2)
    private val routines = listOf(a, b, c)

    @Test
    fun nextIsTheRoutineAfterTheLastOne() {
        assertEquals(b, nextRoutine(routines, lastRoutineId = a.id))
        assertEquals(c, nextRoutine(routines, lastRoutineId = b.id))
    }

    @Test
    fun afterTheLastRoutineComesTheFirst() {
        assertEquals(a, nextRoutine(routines, lastRoutineId = c.id))
    }

    @Test
    fun withoutFinishedWorkoutOrUnknownRoutineStartsFromTheFirst() {
        assertEquals(a, nextRoutine(routines, lastRoutineId = null))
        // 마지막으로 한 루틴이 지워져 목록에 없다.
        assertEquals(a, nextRoutine(routines, lastRoutineId = 99))
    }

    @Test
    fun singleRoutineAndEmptyList() {
        assertEquals(a, nextRoutine(listOf(a), lastRoutineId = a.id))
        assertEquals(a, nextRoutine(listOf(a), lastRoutineId = null))
        assertNull(nextRoutine(emptyList(), lastRoutineId = null))
        assertNull(nextRoutine(emptyList(), lastRoutineId = a.id))
    }

    @Test
    fun pinnedRoutineWinsOverNext() {
        assertEquals(c, topRoutine(routines, pinnedRoutineId = c.id, lastRoutineId = a.id))
        assertEquals(b, topRoutine(routines, pinnedRoutineId = null, lastRoutineId = a.id))
        // 고른 루틴이 목록에 없으면 다음 차례로 돌아간다.
        assertEquals(b, topRoutine(routines, pinnedRoutineId = 99, lastRoutineId = a.id))
        assertNull(topRoutine(emptyList(), pinnedRoutineId = null, lastRoutineId = null))
    }
}
