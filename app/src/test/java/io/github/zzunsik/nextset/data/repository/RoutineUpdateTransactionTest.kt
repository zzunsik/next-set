package io.github.zzunsik.nextset.data.repository

import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PLANK
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.TestExercises.SQUAT
import io.github.zzunsik.nextset.domain.model.RoutineDetail
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.domain.rule.RoutineUpdatePlan
import io.github.zzunsik.nextset.domain.rule.applyRoutineUpdate
import io.github.zzunsik.nextset.domain.rule.planRoutineUpdate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 운동을 끝낼 때 루틴 반영(LOG-10)이 한 트랜잭션인지 본다(#72).
 * 반영 도중 한 단계를 SQLite 트리거로 실패시키고, 루틴이 반영 전과 같은지 확인한다.
 */
@RunWith(RobolectricTestRunner::class)
class RoutineUpdateTransactionTest {
    private lateinit var db: TestDb
    private var routineId = 0L

    @Before
    fun setUp() = runTest {
        db = TestDb().seed()
        routineId = db.routines.addRoutine("테스트 루틴")
        db.routines.addRoutineExercise(routineId, BENCH, 3, Target(weight = Weight.kg(60), repsMin = 8, repsMax = 12))
        db.routines.addRoutineExercise(routineId, SQUAT, 2)
        db.routines.addRoutineExercise(routineId, PLANK, 1)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun failureWhileAddingLeavesTheRoutineUnchanged() = runTest {
        val plan = changedPlan()
        val before = routine()
        failOn("BEFORE INSERT ON routine_exercise")

        assertFails { db.routines.applyRoutineUpdate(plan) }

        assertEquals("더하다 실패하면 앞서 뺀 종목과 바꾼 세트 수도 되돌아간다", before, routine())
    }

    @Test
    fun failureWhileReorderingLeavesTheRoutineUnchanged() = runTest {
        val plan = changedPlan()
        val before = routine()
        // 더한 종목(풀업)의 자리를 매길 때 실패한다. 빼기, 세트 수 바꾸기, 더하기는 이미 지난 뒤다.
        failOn("BEFORE UPDATE OF position ON routine_exercise WHEN OLD.exercise_id = '$PULL_UP'")

        assertFails { db.routines.applyRoutineUpdate(plan) }

        assertEquals("마지막 순서 매기기에서 실패해도 루틴은 반영 전과 같다", before, routine())
    }

    @Test
    fun applyingAfterAFailedAttemptStillWorks() = runTest {
        val plan = changedPlan()
        failOn("BEFORE INSERT ON routine_exercise")
        assertFails { db.routines.applyRoutineUpdate(plan) }
        exec("DROP TRIGGER fail_step")

        db.routines.applyRoutineUpdate(plan)

        val routine = routine()
        assertEquals(listOf(PLANK, BENCH, PULL_UP), routine.exercises.map { it.exercise.id })
        assertEquals(listOf(1, 4, 2), routine.exercises.map { it.setCount })
        assertEquals(listOf(0, 1, 2), routine.exercises.map { it.position })
    }

    /** 벤치 세트 하나 더, 스쿼트 빼기, 풀업 더하기, 플랭크를 맨 위로. 빼기, 세트 수 바꾸기, 더하기, 순서 바꾸기를 모두 거친다. */
    private suspend fun changedPlan(): RoutineUpdatePlan {
        val workoutId = db.workouts.startFromRoutine(routineId, at(0))
        val (bench, squat, plank) = db.workouts.observeWorkout(workoutId).first()!!.exercises
        db.workouts.addSet(bench.id)
        db.workouts.removeExercise(squat.id)
        val pullUp = db.workouts.addExercise(workoutId, PULL_UP, setCount = 2)
        db.workouts.reorderExercises(workoutId, listOf(plank.id, bench.id, pullUp))
        val workout = db.workouts.observeWorkout(workoutId).first()!!
        return planRoutineUpdate(workout, routine())!!
    }

    private suspend fun routine(): RoutineDetail = db.routines.observeRoutine(routineId).first()!!

    /** [event]에서 쓰기를 실패시키는 트리거를 건다. 지어낸 실패다. */
    private suspend fun failOn(event: String) =
        exec("CREATE TRIGGER fail_step $event BEGIN SELECT RAISE(ABORT, 'injected failure'); END")

    private suspend fun exec(sql: String) = db.database.useWriterConnection { it.executeSQL(sql) }

    private suspend fun assertFails(block: suspend () -> Unit) {
        val failure = runCatching { block() }.exceptionOrNull()
        assertTrue("반영이 실패해야 한다", failure != null)
    }
}
