package io.github.zzunsik.nextset.data.repository

import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.TestExercises.SQUAT
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 직전 기록과 미리 채울 값(LOG-03). */
@RunWith(RobolectricTestRunner::class)
class PreviousRecordTest {
    private lateinit var db: TestDb

    @Before
    fun setUp() = runTest { db = TestDb().seed() }

    @After
    fun tearDown() = db.close()

    @Test
    fun takesTheMostRecentWorkoutOfThatExercise() = runTest {
        db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(50_000, 10), wr(55_000, 8))))
        db.finishedWorkout(at(100), mapOf(BENCH to listOf(wr(60_000, 10), wr(62_500, 8))))
        // 더 늦은 운동이지만 다른 종목만 했다.
        db.finishedWorkout(at(200), mapOf(SQUAT to listOf(wr(80_000, 5))))

        assertEquals(
            listOf(wr(60_000, 10), wr(62_500, 8)),
            db.workouts.getPreviousSets(BENCH, RecordType.WEIGHT_REPS, before = at(300)),
        )
        // 기준 시각보다 먼저 시작한 운동만 본다. 지난 날짜를 적어 넣을 때 그 날짜에 맞는 기록이 나온다.
        assertEquals(
            listOf(wr(50_000, 10), wr(55_000, 8)),
            db.workouts.getPreviousSets(BENCH, RecordType.WEIGHT_REPS, before = at(50)),
        )
    }

    @Test
    fun takesTheSetWithTheSamePositionOrNothing() = runTest {
        db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10), wr(62_500, 8))))

        assertEquals(wr(60_000, 10), db.workouts.getPreviousSet(BENCH, RecordType.WEIGHT_REPS, at(10), position = 0))
        assertEquals(wr(62_500, 8), db.workouts.getPreviousSet(BENCH, RecordType.WEIGHT_REPS, at(10), position = 1))
        assertNull(db.workouts.getPreviousSet(BENCH, RecordType.WEIGHT_REPS, at(10), position = 2))
        assertNull(db.workouts.getPreviousSet(SQUAT, RecordType.WEIGHT_REPS, at(10), position = 0))
    }

    @Test
    fun inProgressWorkoutAndIncompleteSetsDoNotCount() = runTest {
        db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10))), incomplete = setOf(BENCH))

        // 진행 중인 운동에 완료한 세트를 적는다.
        val inProgress = db.workouts.startEmpty(at(100))
        val weId = db.workouts.addExercise(inProgress, BENCH, setCount = 0)
        val setId = db.workouts.addSet(weId)
        db.workouts.updateSet(setId, wr(100_000, 1))
        db.workouts.completeSet(setId, at(101))

        // 끝난 운동에서 완료한 세트만 나온다. 완료하지 않은 둘째 세트(999kg)는 빠진다.
        assertEquals(listOf(wr(60_000, 10)), db.workouts.getPreviousSets(BENCH, RecordType.WEIGHT_REPS, at(200)))
        assertNull(db.workouts.getPreviousSet(BENCH, RecordType.WEIGHT_REPS, at(200), position = 1))
    }

    @Test
    fun workoutWithOnlyIncompleteSetsIsSkipped() = runTest {
        db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10))))
        // 지난 기록을 고쳐 세트를 모두 완료 취소한 운동은 건너뛰고 그보다 앞선 기록을 찾는다.
        val later = db.finishedWorkout(at(100), mapOf(BENCH to listOf(wr(70_000, 5))))
        val setId = db.workouts.observeWorkout(later).first()!!.exercises.single().sets.single().id
        db.workouts.uncompleteSet(setId)

        assertEquals(listOf(wr(60_000, 10)), db.workouts.getPreviousSets(BENCH, RecordType.WEIGHT_REPS, at(200)))
    }

    @Test
    fun onlySetsWrittenWithTheSameRecordTypeCount() = runTest {
        db.finishedWorkout(at(0), mapOf(PULL_UP to listOf(reps(12))))
        assertEquals(listOf(reps(12)), db.workouts.getPreviousSets(PULL_UP, RecordType.REPS, at(10)))
        assertEquals(emptyList<SetValue>(), db.workouts.getPreviousSets(PULL_UP, RecordType.WEIGHT_REPS, at(10)))
    }

    @Test
    fun prefillUsesPreviousRecordThenTargetThenBlank() = runTest {
        val routineId = db.routines.addRoutine("가슴 날")
        db.routines.addRoutineExercise(routineId, BENCH, setCount = 3, target = Target(weight = Weight.kg(40), repsMin = 8, repsMax = 12))
        db.routines.addRoutineExercise(routineId, SQUAT, setCount = 2)
        db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10), wr(62_500, 8))))

        val workoutId = db.workouts.startFromRoutine(routineId, at(100))
        val workout = db.workouts.observeWorkout(workoutId).first()!!
        val bench = workout.exercises[0].sets.map { it.value }
        val squat = workout.exercises[1].sets.map { it.value }

        // 1, 2세트는 직전 기록, 3세트는 직전 기록에 없어 목표(횟수는 하한).
        assertEquals(listOf(wr(60_000, 10), wr(62_500, 8), wr(40_000, 8)), bench)
        // 직전 기록도 목표도 없으면 빈칸이다.
        assertEquals(listOf(SetValue.EMPTY, SetValue.EMPTY), squat)

        // 운동 중에 더한 세트도 같은 순서로 채운다.
        val added = db.workouts.addSet(workout.exercises[1].id)
        assertEquals(SetValue.EMPTY, db.workouts.observeWorkout(workoutId).first()!!.exercises[1].sets.single { it.id == added }.value)
    }
}
