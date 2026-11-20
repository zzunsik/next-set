package io.github.zzunsik.nextset.data.repository

import io.github.zzunsik.nextset.data.db.NextSetDatabase
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.rule.isUntouched
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** 저장소가 손댄 운동을 적는다(#73). 운동마다 새로 시작해 한 가지만 하고 본다. */
@RunWith(RobolectricTestRunner::class)
class UntouchedWorkoutRepositoryTest {
    private lateinit var db: TestDb
    private var routineId = 0L

    @Before
    fun setUp() = runTest {
        db = TestDb().seed()
        routineId = db.routines.addRoutine("지어낸 루틴")
        db.routines.addRoutineExercise(routineId, BENCH, 2, Target(weight = Weight.kg(50), repsMin = 10))
        db.routines.addRoutineExercise(routineId, PULL_UP, 1)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun read(id: Long): Workout = db.workouts.observeWorkout(id).first()!!

    /** 루틴으로 운동을 시작해 [action]을 한 뒤 손대지 않았는지 돌려준다. 끝나면 운동을 버린다. */
    private suspend fun untouchedAfter(action: suspend (Workout) -> Unit): Boolean {
        val id = db.workouts.startFromRoutine(routineId, at(0))
        val started = read(id)
        assertTrue("시작한 직후는 손대지 않았다", started.isUntouched())
        action(started)
        return read(id).isUntouched().also { db.workouts.discardWorkout(id) }
    }

    @Test
    fun writingThePrefilledValueAgainIsNotAnEdit() = runTest {
        assertTrue(untouchedAfter { w -> w.exercises[0].sets[0].let { db.workouts.updateSet(it.id, it.value) } })
    }

    @Test
    fun everyUserEditMakesTheWorkoutTouched() = runTest {
        val bench = { w: Workout -> w.exercises[0] }
        assertFalse("값을 바꿈", untouchedAfter { w -> db.workouts.updateSet(bench(w).sets[0].id, wr(55_000, 10)) })
        assertFalse("세트 완료", untouchedAfter { w -> db.workouts.completeSet(bench(w).sets[0].id, at(1)) })
        assertFalse("완료했다 풂", untouchedAfter { w ->
            db.workouts.completeSet(bench(w).sets[0].id, at(1))
            db.workouts.uncompleteSet(bench(w).sets[0].id)
        })
        assertFalse("세트 더함", untouchedAfter { w -> db.workouts.addSet(bench(w).id) })
        assertFalse("세트 지움", untouchedAfter { w -> db.workouts.deleteSet(bench(w).sets[1].id) })
        assertFalse("종목 더함", untouchedAfter { w -> db.workouts.addExercise(w.id, PULL_UP) })
        assertFalse("종목 뺌", untouchedAfter { w -> db.workouts.removeExercise(bench(w).id) })
        assertFalse("종목 순서", untouchedAfter { w -> db.workouts.reorderExercises(w.id, w.exercises.map { it.id }.reversed()) })
        assertFalse("운동 메모", untouchedAfter { w -> db.workouts.updateWorkoutNote(w.id, "지어낸 메모") })
        assertFalse("종목 메모를 적었다 지움", untouchedAfter { w ->
            db.workouts.updateExerciseNote(bench(w).id, "지어낸 메모")
            db.workouts.updateExerciseNote(bench(w).id, "")
        })
    }

    @Test
    fun sameOrderIsNotAnEdit() = runTest {
        assertTrue(untouchedAfter { w -> db.workouts.reorderExercises(w.id, w.exercises.map { it.id }) })
    }

    @Test
    fun emptyWorkoutIsTouchedOnceAnExerciseIsAdded() = runTest {
        val id = db.workouts.startEmpty(at(0))
        assertTrue(read(id).isUntouched())
        db.workouts.addExercise(id, BENCH)
        assertFalse(read(id).isUntouched())
    }

    /** 앱이 꺼졌다 켜져도 같은 판단이 나온다(LOG-13). 파일 데이터베이스를 닫았다 다시 연다. */
    @Test
    fun theJudgementSurvivesReopeningTheDatabase() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val first = TestDb(NextSetDatabase.build(context)).seed()
        val id = try {
            val routine = first.routines.addRoutine("지어낸 루틴")
            first.routines.addRoutineExercise(routine, BENCH, 2)
            first.workouts.startFromRoutine(routine, at(0)).also { id ->
                val set = first.workouts.observeWorkout(id).first()!!.exercises[0].sets[0]
                first.workouts.updateSet(set.id, wr(70_000, 5))
            }
        } finally {
            first.close()
        }
        val reopened = TestDb(NextSetDatabase.build(context))
        try {
            assertFalse(reopened.workouts.observeWorkout(id).first()!!.isUntouched())
        } finally {
            reopened.close()
        }
    }
}
