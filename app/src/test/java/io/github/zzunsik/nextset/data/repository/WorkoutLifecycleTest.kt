package io.github.zzunsik.nextset.data.repository

import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PLANK
import io.github.zzunsik.nextset.data.repository.TestExercises.SQUAT
import io.github.zzunsik.nextset.domain.model.FinishResult
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.domain.model.WorkoutInProgressException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 운동 시작과 끝, 다음 차례 루틴(HOME-02, HOME-03), 지난 기록 보존. */
@RunWith(RobolectricTestRunner::class)
class WorkoutLifecycleTest {
    private lateinit var db: TestDb

    @Before
    fun setUp() = runTest { db = TestDb().seed() }

    @After
    fun tearDown() = db.close()

    /** 루틴으로 운동을 시작해 첫 세트만 완료하고 끝낸다. */
    private suspend fun doRoutine(routineId: Long, minute: Long): Long {
        val workoutId = db.workouts.startFromRoutine(routineId, at(minute))
        val firstSet = db.workouts.observeWorkout(workoutId).first()!!.exercises.first().sets.first()
        db.workouts.completeSet(firstSet.id, at(minute + 1))
        assertEquals(FinishResult.FINISHED, db.workouts.finishWorkout(workoutId, at(minute + 30)))
        return workoutId
    }

    private suspend fun routine(name: String): Long =
        db.routines.addRoutine(name).also { db.routines.addRoutineExercise(it, BENCH, setCount = 2) }

    private suspend fun top() = db.routines.observeTopRoutine().first()?.name

    @Test
    fun nextRoutineFollowsListOrderAndWrapsAround() = runTest {
        assertNull("루틴이 없다", top())
        val a = routine("A")
        assertEquals("루틴이 하나", "A", top())
        doRoutine(a, 0)
        assertEquals("루틴이 하나면 계속 그 루틴", "A", top())

        val b = routine("B")
        val c = routine("C")
        assertEquals("B", top())
        doRoutine(b, 100)
        assertEquals("C", top())
        doRoutine(c, 200)
        assertEquals("마지막 루틴 다음은 첫 루틴", "A", top())

        db.routines.reorderRoutines(listOf(c, b, a))
        assertEquals("순서는 루틴 목록을 따른다. C 다음은 B", "B", top())
    }

    @Test
    fun emptyWorkoutDoesNotChangeTheOrder() = runTest {
        val a = routine("A")
        routine("B")
        doRoutine(a, 0)
        assertEquals("B", top())

        val empty = db.workouts.startEmpty(at(100))
        val weId = db.workouts.addExercise(empty, SQUAT)
        db.workouts.completeSet(db.workouts.observeWorkout(empty).first()!!.exercises.single { it.id == weId }.sets.single().id, at(101))
        db.workouts.finishWorkout(empty, at(130))
        assertEquals("B", top())

        // 지난 날짜로 적어 넣은 운동도 루틴이 없으니 차례를 바꾸지 않는다.
        db.finishedWorkout(at(300), mapOf(SQUAT to listOf(wr(80_000, 5))))
        assertEquals("B", top())
    }

    @Test
    fun deletedRoutineFallsBackToTheRoutineDoneBeforeIt() = runTest {
        routine("A")
        val b = routine("B")
        val c = routine("C")
        routine("D")
        doRoutine(b, 0)
        doRoutine(c, 100)
        assertEquals("D", top())

        // 마지막으로 한 C를 지우면 그 기록의 routine_id가 비어 그보다 앞서 한 B가 기준이 된다.
        db.routines.deleteRoutine(c)
        assertEquals("D", top())
        assertEquals(listOf("A", "B", "D"), db.routines.observeRoutines().first().map { it.name })
        assertEquals(listOf(0, 1, 2), db.routines.observeRoutines().first().map { it.position })

        // 한 번도 한 적 없는 루틴만 남으면 첫 루틴이다.
        db.routines.deleteRoutine(b)
        assertEquals("A", top())
    }

    @Test
    fun discardedWorkoutDoesNotChangeTheOrder() = runTest {
        val a = routine("A")
        routine("B")
        val workoutId = db.workouts.startFromRoutine(a, at(0))
        db.workouts.discardWorkout(workoutId)
        assertEquals("A", top())
        assertNull(db.workouts.observeWorkout(workoutId).first())
    }

    @Test
    fun pinnedRoutineStaysUntilAWorkoutWithItIsFinished() = runTest {
        val a = routine("A")
        routine("B")
        val c = routine("C")
        db.settings.setPinnedRoutine(c)
        assertEquals("C", top())

        // 다른 루틴으로 운동해도 고른 루틴이 맨 위에 남는다.
        doRoutine(a, 0)
        assertEquals("C", top())

        // 고른 루틴으로 시작했다가 버리면 그대로 남는다.
        db.workouts.discardWorkout(db.workouts.startFromRoutine(c, at(50)))
        assertEquals("C", top())

        // 그 루틴으로 운동을 마치면 풀리고 다음 차례로 돌아간다.
        doRoutine(c, 100)
        assertNull(db.settings.observeSettings().first().pinnedRoutineId)
        assertEquals("A", top())

        // 고른 루틴을 지우면 외래 키가 값을 비운다.
        db.settings.setPinnedRoutine(a)
        db.routines.deleteRoutine(a)
        assertNull(db.settings.observeSettings().first().pinnedRoutineId)
    }

    @Test
    fun onlyOneWorkoutInProgress() = runTest {
        val a = routine("A")
        val first = db.workouts.startEmpty(at(0))
        val error = assertThrows(WorkoutInProgressException::class.java) {
            kotlinx.coroutines.runBlocking { db.workouts.startFromRoutine(a, at(1)) }
        }
        assertEquals(first, error.workoutId)
        assertThrows(WorkoutInProgressException::class.java) {
            kotlinx.coroutines.runBlocking { db.workouts.startEmpty(at(2)) }
        }
        assertEquals(first, db.workouts.observeInProgressWorkout().first()!!.id)
    }

    @Test
    fun finishingRemovesIncompleteSetsAndEmptyExercises() = runTest {
        val routineId = db.routines.addRoutine("A")
        db.routines.addRoutineExercise(routineId, BENCH, setCount = 3)
        db.routines.addRoutineExercise(routineId, SQUAT, setCount = 2)
        val workoutId = db.workouts.startFromRoutine(routineId, at(0))
        val bench = db.workouts.observeWorkout(workoutId).first()!!.exercises[0]
        db.workouts.updateSet(bench.sets[0].id, wr(60_000, 10))
        db.workouts.completeSet(bench.sets[0].id, at(1))
        db.workouts.updateSet(bench.sets[2].id, wr(65_000, 5))
        db.workouts.completeSet(bench.sets[2].id, at(2))

        assertEquals(FinishResult.FINISHED, db.workouts.finishWorkout(workoutId, at(30)))
        val finished = db.workouts.observeWorkout(workoutId).first()!!
        assertEquals(at(30), finished.finishedAt)
        assertEquals(listOf(BENCH), finished.exercises.map { it.exercise.id })
        assertEquals(listOf(0, 1), finished.exercises.single().sets.map { it.position })
        assertEquals(listOf(wr(60_000, 10), wr(65_000, 5)), finished.exercises.single().sets.map { it.value })
        assertNull(db.workouts.observeInProgressWorkout().first())
    }

    @Test
    fun finishingWithoutCompletedSetsLeavesNoRecord() = runTest {
        val workoutId = db.workouts.startEmpty(at(0))
        db.workouts.addExercise(workoutId, BENCH, setCount = 2)
        assertEquals(FinishResult.DISCARDED_EMPTY, db.workouts.finishWorkout(workoutId, at(30)))
        assertNull(db.workouts.observeWorkout(workoutId).first())
        assertEquals(emptyList<Any>(), db.workouts.observeHistory().first())
    }

    @Test
    fun editingOrDeletingTheRoutineKeepsPastWorkouts() = runTest {
        val routineId = db.routines.addRoutine("가슴 날")
        val item = db.routines.addRoutineExercise(
            routineId,
            BENCH,
            setCount = 2,
            target = Target(weight = Weight.kg(60), repsMin = 8, repsMax = 10),
        )
        db.routines.addRoutineExercise(routineId, PLANK, setCount = 1, target = Target(durationSeconds = 60))
        val workoutId = doRoutine(routineId, 0)
        val before = db.workouts.observeWorkout(workoutId).first()!!

        db.routines.renameRoutine(routineId, "새 이름")
        db.routines.updateRoutineExercise(item, setCount = 5, target = Target(weight = Weight.kg(100), repsMin = 1, repsMax = 3))
        db.routines.reorderRoutineExercises(routineId, db.routines.observeRoutine(routineId).first()!!.exercises.map { it.id }.reversed())
        db.routines.addRoutineExercise(routineId, SQUAT, setCount = 3)
        assertEquals(before, db.workouts.observeWorkout(workoutId).first())

        db.routines.deleteRoutine(routineId)
        val after = db.workouts.observeWorkout(workoutId).first()!!
        assertEquals(before.copy(routineId = null), after)
        assertEquals("가슴 날", after.routineName)
        assertEquals(Target(weight = Weight.kg(60), repsMin = 8, repsMax = 10), after.exercises[0].target)
        assertEquals(RecordType.WEIGHT_REPS, after.exercises[0].recordType)
        assertNotNull(db.workouts.observeHistory().first().single { it.id == workoutId })
    }

    @Test
    fun exercisesAndSetsCanBeAddedRemovedAndReordered() = runTest {
        val workoutId = db.workouts.startEmpty(at(0))
        val bench = db.workouts.addExercise(workoutId, BENCH, setCount = 2)
        val squat = db.workouts.addExercise(workoutId, SQUAT, setCount = 1)
        val plank = db.workouts.addExercise(workoutId, PLANK, setCount = 1)

        db.workouts.reorderExercises(workoutId, listOf(plank, bench, squat))
        db.workouts.removeExercise(bench)
        var workout = db.workouts.observeWorkout(workoutId).first()!!
        assertEquals(listOf(PLANK, SQUAT), workout.exercises.map { it.exercise.id })
        assertEquals(listOf(0, 1), workout.exercises.map { it.position })

        val extra = db.workouts.addSet(squat)
        val firstSquatSet = workout.exercises[1].sets.single().id
        db.workouts.deleteSet(firstSquatSet)
        workout = db.workouts.observeWorkout(workoutId).first()!!
        assertEquals(listOf(extra), workout.exercises[1].sets.map { it.id })
        assertEquals(listOf(0), workout.exercises[1].sets.map { it.position })

        db.workouts.updateWorkoutNote(workoutId, "지어낸 메모")
        db.workouts.updateExerciseNote(plank, "자세 확인")
        workout = db.workouts.observeWorkout(workoutId).first()!!
        assertEquals("지어낸 메모", workout.note)
        assertEquals("자세 확인", workout.exercises[0].note)

        // 시간 종목에는 시간만 저장한다.
        val plankSet = workout.exercises[0].sets.single().id
        db.workouts.updateSet(plankSet, wr(10_000, 3).copy(durationSeconds = 45))
        assertEquals(seconds(45), db.workouts.observeWorkout(workoutId).first()!!.exercises[0].sets.single().value)
    }

    @Test
    fun pastWorkoutTimesCanBeEdited() = runTest {
        val id = db.finishedWorkout(at(100), mapOf(BENCH to listOf(wr(60_000, 10))))
        db.workouts.updateWorkoutTimes(id, at(10), at(70))
        val summary = db.workouts.observeHistory().first().single()
        assertEquals(at(10), summary.startedAt)
        assertEquals(at(70), summary.finishedAt)
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { db.workouts.updateWorkoutTimes(id, at(70), at(10)) }
        }
    }
}
