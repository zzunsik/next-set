package io.github.zzunsik.nextset.data.repository

import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.TestExercises.SQUAT
import io.github.zzunsik.nextset.domain.model.HomeSnapshot
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 홈이 한 시점에 읽은 데이터(#97). */
@RunWith(RobolectricTestRunner::class)
class HomeSnapshotTest {
    private lateinit var db: TestDb
    private val collector = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Before
    fun setUp() = runBlocking { db = TestDb().seed(); Unit }

    @After
    fun tearDown() {
        runBlocking { collector.coroutineContext.job.cancelAndJoin() }
        db.close()
    }

    private fun <T> db(block: suspend TestDb.() -> T): T = runBlocking { db.block() }

    private fun <T> await(flow: Flow<T>, predicate: (T) -> Boolean): T = runBlocking {
        withContext(Dispatchers.Default) { withTimeout(10_000) { flow.first(predicate) } }
    }

    private fun routine(name: String, vararg exerciseIds: String): Long = db {
        routines.addRoutine(name).also { id -> exerciseIds.forEach { routines.addRoutineExercise(id, it, 2) } }
    }

    @Test
    fun readsRoutinesNamesLastDoneTopAndPin() {
        val a = routine("가슴", BENCH, PULL_UP)
        val b = routine("등", PULL_UP)
        db {
            val id = workouts.startFromRoutine(a, at(0))
            val set = workouts.observeWorkout(id).first()!!.exercises.first().sets.first()
            workouts.completeSet(set.id, at(1))
            workouts.finishWorkout(id, at(30))
            settings.setPinnedRoutine(a)
            workouts.startEmpty(at(100))
        }

        val home = db { routines.observeHome().first() }

        assertEquals(listOf(a, b), home.routines.map { it.routine.id })
        assertEquals(listOf("테스트 벤치", "테스트 풀업"), home.routines[0].exerciseNames)
        assertEquals(at(0), home.routines[0].lastStartedAt)
        assertNull("한 적이 없으면 비어 있다", home.routines[1].lastStartedAt)
        assertEquals("직접 고른 루틴이 맨 위", a, home.top?.id)
        assertEquals(a, home.pinnedRoutineId)
        assertEquals("진행 중인 빈 운동", null, home.inProgress?.routineId)
        assertTrue(home.inProgress!!.isInProgress)
    }

    /**
     * 운동을 끝내는 동안 내보낸 값이 모두 한 시점의 조합이다. 진행 중인 운동이 있으면 맨 위가 a, 없으면 b다.
     * 흐름을 따로 합치던 때는 맨 위만 먼저 b로 바뀐 값이 나올 수 있었다(#97).
     */
    @Test
    fun finishingEmitsOnlyConsistentSnapshots() {
        val a = routine("가슴", BENCH)
        val b = routine("등", PULL_UP)
        routine("하체", SQUAT)
        val workoutId = db {
            val id = workouts.startFromRoutine(a, at(0))
            val set = workouts.observeWorkout(id).first()!!.exercises.first().sets.first()
            workouts.completeSet(set.id, at(1))
            id
        }
        val emitted = Collections.synchronizedList(mutableListOf<HomeSnapshot>())
        val latest = MutableStateFlow<HomeSnapshot?>(null)
        collector.launch { db.routines.observeHome().collect { emitted += it; latest.value = it } }
        await(latest) { it?.inProgress != null }

        db { workouts.finishWorkout(workoutId, at(30)) }
        await(latest) { it?.inProgress == null }

        val wrong = synchronized(emitted) { emitted.toList() }.filter { home ->
            if (home.inProgress != null) home.top?.id != a else home.top?.id != b
        }
        assertEquals("어긋난 조합", emptyList<HomeSnapshot>(), wrong)
        assertEquals(b, latest.value?.top?.id)
    }
}
