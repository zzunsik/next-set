package io.github.zzunsik.nextset.data.repository

import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PLANK
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.TestExercises.SQUAT
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.domain.rule.planRoutineUpdate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 운동 중 화면(#24)이 더 쓰는 저장소 동작. 지운 세트 되살리기(LOG-09)와 루틴 반영(LOG-10). */
@RunWith(RobolectricTestRunner::class)
class WorkoutEditingTest {
    private lateinit var db: TestDb

    @Before
    fun setUp() = runTest { db = TestDb().seed() }

    @After
    fun tearDown() = db.close()

    @Test
    fun restoredSetGoesBackToItsPosition() = runTest {
        val workoutId = db.workouts.startEmpty(at(0))
        val weId = db.workouts.addExercise(workoutId, BENCH, setCount = 3)
        val sets = db.workouts.observeWorkout(workoutId).first()!!.exercises.single().sets
        db.workouts.updateSet(sets[1].id, wr(62_500, 8))
        db.workouts.completeSet(sets[1].id, at(5))
        db.workouts.deleteSet(sets[1].id)

        val restored = db.workouts.restoreSet(weId, 1, wr(62_500, 8), at(5))

        val after = db.workouts.observeWorkout(workoutId).first()!!.exercises.single().sets
        assertEquals(listOf(sets[0].id, restored, sets[2].id), after.map { it.id })
        assertEquals(listOf(0, 1, 2), after.map { it.position })
        assertEquals(wr(62_500, 8), after[1].value)
        assertEquals(at(5), after[1].completedAt)
    }

    @Test
    fun restoreBeyondTheEndAppendsAndFitsTheRecordType() = runTest {
        val workoutId = db.workouts.startEmpty(at(0))
        val weId = db.workouts.addExercise(workoutId, PULL_UP, setCount = 1)
        db.workouts.restoreSet(weId, 5, wr(10_000, 12), null)
        val sets = db.workouts.observeWorkout(workoutId).first()!!.exercises.single().sets
        assertEquals(listOf(0, 1), sets.map { it.position })
        assertEquals("횟수만 종목은 무게를 비운다", reps(12), sets[1].value)
        assertNull(sets[1].completedAt)
    }

    @Test
    fun routineReceivesTheWorkoutLayout() = runTest {
        val routineId = db.routines.addRoutine("테스트 루틴")
        db.routines.addRoutineExercise(routineId, BENCH, 3, Target(weight = Weight.kg(60), repsMin = 8, repsMax = 12))
        db.routines.addRoutineExercise(routineId, SQUAT, 2)
        db.routines.addRoutineExercise(routineId, PLANK, 1)
        val workoutId = db.workouts.startFromRoutine(routineId, at(0))
        val start = db.workouts.observeWorkout(workoutId).first()!!
        val (bench, squat, plank) = start.exercises
        // 벤치 세트 하나 더, 스쿼트 빼기, 풀업 더하기, 플랭크를 맨 위로.
        db.workouts.addSet(bench.id)
        db.workouts.removeExercise(squat.id)
        val pullUp = db.workouts.addExercise(workoutId, PULL_UP, setCount = 2)
        db.workouts.reorderExercises(workoutId, listOf(plank.id, bench.id, pullUp))

        val workout = db.workouts.observeWorkout(workoutId).first()!!
        val plan = planRoutineUpdate(workout, db.routines.observeRoutine(routineId).first()!!)!!
        db.routines.applyRoutineUpdate(plan)

        val routine = db.routines.observeRoutine(routineId).first()!!
        assertEquals(listOf(PLANK, BENCH, PULL_UP), routine.exercises.map { it.exercise.id })
        assertEquals(listOf(1, 4, 2), routine.exercises.map { it.setCount })
        assertEquals("남은 종목의 목표는 그대로다", Target(weight = Weight.kg(60), repsMin = 8, repsMax = 12), routine.exercises[1].target)
        assertEquals("더한 종목의 목표는 비어 있다", Target.NONE, routine.exercises[2].target)
        assertNull("반영한 뒤에는 다를 것이 없다", planRoutineUpdate(workout, routine))
    }

    @Test
    fun exerciseWithoutSetsIsRemovedFromTheRoutine() = runTest {
        val routineId = db.routines.addRoutine("테스트 루틴")
        db.routines.addRoutineExercise(routineId, BENCH, 1)
        db.routines.addRoutineExercise(routineId, SQUAT, 1)
        val workoutId = db.workouts.startFromRoutine(routineId, at(0))
        val squat = db.workouts.observeWorkout(workoutId).first()!!.exercises[1]
        db.workouts.deleteSet(squat.sets.single().id)

        val workout = db.workouts.observeWorkout(workoutId).first()!!
        db.routines.applyRoutineUpdate(planRoutineUpdate(workout, db.routines.observeRoutine(routineId).first()!!)!!)

        assertEquals(listOf(BENCH), db.routines.observeRoutine(routineId).first()!!.exercises.map { it.exercise.id })
    }
}
