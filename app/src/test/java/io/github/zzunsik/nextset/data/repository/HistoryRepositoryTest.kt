package io.github.zzunsik.nextset.data.repository

import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.TestExercises.SQUAT
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.domain.rule.recordSetIds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 기록 화면(#27)이 쓰는 저장소 동작. 끝난 운동 전부 읽기(HIST-01~03)와 지난 운동 적기(HIST-06). */
@RunWith(RobolectricTestRunner::class)
class HistoryRepositoryTest {
    private lateinit var db: TestDb

    @Before
    fun setUp() = runTest { db = TestDb().seed() }

    @After
    fun tearDown() = db.close()

    private suspend fun finished() = db.workouts.observeFinishedWorkouts().first()

    @Test
    fun finishedWorkoutsComeNewestFirstWithTheirSetsAndWithoutTheOneInProgress() = runTest {
        val old = db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10))))
        val new = db.finishedWorkout(at(1_000), mapOf(BENCH to listOf(wr(62_500, 8)), PULL_UP to listOf(reps(10))))
        db.workouts.startEmpty(at(2_000))

        val workouts = finished()
        assertEquals(listOf(new, old), workouts.map { it.id })
        assertEquals(listOf(BENCH, PULL_UP), workouts[0].exercises.map { it.exercise.id })
        assertEquals(listOf(wr(62_500, 8)), workouts[0].exercises[0].sets.map { it.value })
        assertEquals(RecordType.REPS, workouts[0].exercises[1].recordType)
        assertEquals("목록 한 줄과 같은 순서다", db.workouts.observeHistory().first().map { it.id }, workouts.map { it.id })
    }

    @Test
    fun pastWorkoutTakesItsPlaceAndFeedsThePreviousRecord() = runTest {
        val early = db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10))))
        val late = db.finishedWorkout(at(10_000), mapOf(BENCH to listOf(wr(70_000, 5))))

        // 두 기록 사이의 날짜로 나중에 적어 넣는다(HIST-06).
        val past = db.workouts.addPastWorkout(at(5_000), at(5_060))
        val weId = db.workouts.addExercise(past, BENCH, setCount = 0)
        val setId = db.workouts.addSet(weId)
        assertEquals("미리 채운 값은 그 날짜보다 앞선 기록에서 온다", wr(60_000, 10), setValue(past, setId))
        db.workouts.updateSet(setId, wr(65_000, 8))
        db.workouts.completeSet(setId, at(5_030))

        assertEquals(listOf(late, past, early), finished().map { it.id })
        // 뒤 기록의 직전 기록이 끼어든 기록으로 바뀐다(LOG-03).
        assertEquals(listOf(wr(65_000, 8)), db.workouts.getPreviousSets(BENCH, RecordType.WEIGHT_REPS, at(10_000)))
        assertEquals(listOf(wr(60_000, 10)), db.workouts.getPreviousSets(BENCH, RecordType.WEIGHT_REPS, at(5_000)))
        // 최고 기록 표시도 시간 순서로 다시 맞춰진다. 65kg이 60kg을, 70kg이 65kg을 넘는다.
        val marks = recordSetIds(finished())
        assertEquals(setOf(setId, finished().first { it.id == late }.exercises.single().sets.single().id), marks)
    }

    @Test
    fun pastWorkoutFromARoutineCopiesTheRoutine() = runTest {
        db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10), wr(60_000, 9))))
        val routineId = db.routines.addRoutine("테스트 루틴")
        db.routines.addRoutineExercise(routineId, BENCH, 3, Target(weight = Weight.kg(50), repsMin = 8))
        db.routines.addRoutineExercise(routineId, SQUAT, 1)

        val id = db.workouts.addPastWorkout(at(1_000), at(1_060), routineId)

        val workout = db.workouts.observeWorkout(id).first()!!
        assertEquals("테스트 루틴", workout.routineName)
        assertEquals(routineId, workout.routineId)
        assertEquals(at(1_060), workout.finishedAt)
        assertEquals(listOf(BENCH, SQUAT), workout.exercises.map { it.exercise.id })
        // 직전 기록, 목표 순으로 미리 채운다. 완료하지 않은 상태다.
        assertEquals(
            listOf(wr(60_000, 10), wr(60_000, 9), wr(50_000, 8)),
            workout.exercises[0].sets.map { it.value },
        )
        assertEquals(listOf(null, null, null), workout.exercises[0].sets.map { it.completedAt })
        assertNull("진행 중인 운동이 아니다", db.workouts.observeInProgressWorkout().first())
    }

    @Test
    fun editingAndDeletingChangeTheMarks() = runTest {
        val first = db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10))))
        db.finishedWorkout(at(1_000), mapOf(BENCH to listOf(wr(62_500, 8))))
        val secondSet = finished()[0].exercises.single().sets.single().id
        assertEquals(setOf(secondSet), recordSetIds(finished()))

        // 앞 기록을 고쳐 더 무겁게 하면 뒤 기록의 표시가 사라진다(HIST-05).
        val firstSet = finished()[1].exercises.single().sets.single().id
        db.workouts.updateSet(firstSet, wr(65_000, 10))
        assertEquals(emptySet<Long>(), recordSetIds(finished()))

        // 되돌리면 다시 붙는다.
        db.workouts.updateSet(firstSet, wr(60_000, 10))
        assertEquals(setOf(secondSet), recordSetIds(finished()))

        // 앞 기록을 지우면 뒤 기록이 그 종목의 첫 기록이라 표시가 없다.
        db.workouts.deleteWorkout(first)
        assertEquals(emptySet<Long>(), recordSetIds(finished()))
        assertEquals(1, finished().size)
    }

    private suspend fun setValue(workoutId: Long, setId: Long) =
        db.workouts.observeWorkout(workoutId).first()!!.exercises.flatMap { it.sets }.first { it.id == setId }.value
}
