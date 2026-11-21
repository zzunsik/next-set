package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.model.WorkoutExercise
import io.github.zzunsik.nextset.domain.model.WorkoutSet
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 기록의 숫자(HIST-02), 최고 기록 표시(HIST-03, HIST-04), 같은 루틴의 지난 기록. 값은 지어낸 것이다. */
class HistoryRulesTest {
    private var nextId = 1L

    private fun wr(kg: Double, reps: Int) = SetValue(weight = Weight((kg * 1000).toInt()), reps = reps)
    private fun reps(reps: Int) = SetValue(reps = reps)
    private fun seconds(s: Int) = SetValue(durationSeconds = s)

    private fun at(minutes: Long): Instant = Instant.parse("2026-01-01T00:00:00Z").plusSeconds(minutes * 60)

    /** 세트 하나. [done]이 거짓이면 완료하지 않은 세트다. */
    private data class S(val value: SetValue, val done: Boolean = true)

    private fun exercise(id: String, type: RecordType, vararg sets: S, position: Int = 0) = WorkoutExercise(
        id = nextId++,
        workoutId = 0,
        exercise = Exercise(id, id, null, emptyList(), BodyPart.CHEST, null, Equipment.BARBELL, type, true, false, null),
        position = position,
        recordType = type,
        note = null,
        target = Target.NONE,
        sets = sets.mapIndexed { index, set ->
            WorkoutSet(nextId++, 0, index, set.value, if (set.done) at(1) else null)
        },
    )

    private fun workout(start: Long, vararg exercises: WorkoutExercise, minutes: Long = 60, routineId: Long? = null) = Workout(
        id = nextId++,
        routineId = routineId,
        routineName = routineId?.let { "루틴 $it" },
        startedAt = at(start),
        finishedAt = at(start + minutes),
        note = null,
        exercises = exercises.mapIndexed { index, e -> e.copy(position = index) },
    )

    private fun Workout.setIds(exerciseIndex: Int) = exercises[exerciseIndex].sets.map { it.id }

    @Test
    fun statsCountOnlyCompletedSetsAndVolumeOnlyWeightReps() {
        val w = workout(
            0,
            exercise("bench", RecordType.WEIGHT_REPS, S(wr(60.0, 10)), S(wr(62.5, 8)), S(wr(100.0, 10), done = false)),
            exercise("pull-up", RecordType.REPS, S(reps(12)), S(reps(10), done = false)),
            exercise("plank", RecordType.DURATION, S(seconds(60))),
            minutes = 72,
        )
        val stats = w.stats()
        assertEquals(72L, stats.minutes)
        assertEquals("완료하지 않은 세트는 세지 않는다", 4, stats.setCount)
        // 60 × 10 + 62.5 × 8 = 1,100kg. 횟수만, 시간 종목과 완료하지 않은 세트는 넣지 않는다.
        assertEquals(1_100_000L, stats.volumeGrams)
    }

    @Test
    fun volumeDoesNotOverflowAndInProgressHasNoDuration() {
        val heavy = List(400) { S(wr(500.0, 50)) }.toTypedArray()
        val w = workout(0, exercise("squat", RecordType.WEIGHT_REPS, *heavy)).copy(finishedAt = null)
        assertEquals(400L * 500_000 * 50, w.stats().volumeGrams)
        assertEquals(0L, w.stats().minutes)
    }

    @Test
    fun firstWorkoutOfAnExerciseHasNoRecord() {
        val first = workout(0, exercise("bench", RecordType.WEIGHT_REPS, S(wr(60.0, 10)), S(wr(70.0, 5))))
        assertEquals(emptySet<Long>(), recordSetIds(listOf(first)))
    }

    @Test
    fun setsBeyondTheEarlierBestAreMarkedInOrder() {
        val first = workout(0, exercise("bench", RecordType.WEIGHT_REPS, S(wr(60.0, 10))))
        // 62.5 × 8은 60 × 10을 넘는다. 62.5 × 8은 다시 같아 넘지 않는다. 62.5 × 9는 무게가 같고 횟수가 많아 넘는다.
        val second = workout(
            100,
            exercise("bench", RecordType.WEIGHT_REPS, S(wr(55.0, 12)), S(wr(62.5, 8)), S(wr(62.5, 8)), S(wr(62.5, 9))),
        )
        val ids = second.setIds(0)
        assertEquals(setOf(ids[1], ids[3]), recordSetIds(listOf(second, first)))
    }

    @Test
    fun tiesWithTheEarlierBestAreNotRecords() {
        val first = workout(0, exercise("bench", RecordType.WEIGHT_REPS, S(wr(60.0, 10))))
        val second = workout(100, exercise("bench", RecordType.WEIGHT_REPS, S(wr(60.0, 10))))
        assertEquals(emptySet<Long>(), recordSetIds(listOf(first, second)))
    }

    @Test
    fun eachRecordTypeComparesItsOwnSets() {
        val first = workout(
            0,
            exercise("pull-up", RecordType.REPS, S(reps(10))),
            exercise("plank", RecordType.DURATION, S(seconds(60))),
        )
        val second = workout(
            100,
            exercise("pull-up", RecordType.REPS, S(reps(10)), S(reps(11))),
            exercise("plank", RecordType.DURATION, S(seconds(59)), S(seconds(75))),
        )
        assertEquals(setOf(second.setIds(0)[1], second.setIds(1)[1]), recordSetIds(listOf(first, second)))
    }

    @Test
    fun setsOfAnotherRecordTypeAreNotCompared() {
        // 같은 종목을 예전에는 횟수만으로 적었다. 무게와 횟수로 처음 적은 운동에는 표시가 없다(HIST-04).
        val asReps = workout(0, exercise("dip", RecordType.REPS, S(reps(30))))
        val asWeight = workout(100, exercise("dip", RecordType.WEIGHT_REPS, S(wr(10.0, 8))))
        val later = workout(200, exercise("dip", RecordType.WEIGHT_REPS, S(wr(12.5, 8))))
        assertEquals(setOf(later.setIds(0)[0]), recordSetIds(listOf(asReps, asWeight, later)))
    }

    @Test
    fun incompleteSetsNeitherCountNorSetTheBest() {
        val first = workout(0, exercise("bench", RecordType.WEIGHT_REPS, S(wr(60.0, 10)), S(wr(200.0, 1), done = false)))
        val second = workout(100, exercise("bench", RecordType.WEIGHT_REPS, S(wr(65.0, 5)), S(wr(300.0, 1), done = false)))
        assertEquals(setOf(second.setIds(0)[0]), recordSetIds(listOf(first, second)))
    }

    @Test
    fun workoutsStartedAtTheSameTimeAreNotEarlierThanEachOther() {
        val a = workout(0, exercise("bench", RecordType.WEIGHT_REPS, S(wr(60.0, 10))))
        val b = workout(0, exercise("bench", RecordType.WEIGHT_REPS, S(wr(70.0, 10))))
        val c = workout(10, exercise("bench", RecordType.WEIGHT_REPS, S(wr(70.0, 11))))
        assertEquals(setOf(c.setIds(0)[0]), recordSetIds(listOf(a, b, c)))
    }

    @Test
    fun recordsFollowTheChronologyNotTheListOrder() {
        // 지난 날짜로 나중에 적은 기록(HIST-06)도 시작 시각의 자리에서 견준다.
        val late = workout(200, exercise("bench", RecordType.WEIGHT_REPS, S(wr(70.0, 5))))
        val early = workout(0, exercise("bench", RecordType.WEIGHT_REPS, S(wr(60.0, 5))))
        val pastAddedLater = workout(100, exercise("bench", RecordType.WEIGHT_REPS, S(wr(80.0, 5))))
        // 80kg 기록이 끼어들면 70kg은 더 이상 최고 기록이 아니다.
        assertEquals(setOf(late.setIds(0)[0]), recordSetIds(listOf(late, early)))
        assertEquals(setOf(pastAddedLater.setIds(0)[0]), recordSetIds(listOf(late, early, pastAddedLater)))
    }

    @Test
    fun editingOrDeletingAnEarlierWorkoutChangesTheMarks() {
        val first = workout(0, exercise("bench", RecordType.WEIGHT_REPS, S(wr(60.0, 10))))
        val second = workout(100, exercise("bench", RecordType.WEIGHT_REPS, S(wr(62.5, 8))))
        val mark = second.setIds(0)[0]
        assertEquals(setOf(mark), recordSetIds(listOf(first, second)))

        // 앞 기록을 65kg으로 고치면 뒤 기록은 최고 기록이 아니다.
        val edited = first.copy(
            exercises = first.exercises.map { e -> e.copy(sets = e.sets.map { it.copy(value = wr(65.0, 10)) }) },
        )
        assertEquals(emptySet<Long>(), recordSetIds(listOf(edited, second)))
        // 앞 기록을 지우면 뒤 기록은 그 종목의 첫 기록이 되어 표시가 없다.
        assertEquals(emptySet<Long>(), recordSetIds(listOf(second)))
    }

    @Test
    fun sameExerciseTwiceInAWorkoutContinuesTheRunningBest() {
        val first = workout(0, exercise("bench", RecordType.WEIGHT_REPS, S(wr(60.0, 10))))
        val second = workout(
            100,
            exercise("bench", RecordType.WEIGHT_REPS, S(wr(65.0, 5))),
            exercise("bench", RecordType.WEIGHT_REPS, S(wr(65.0, 5)), S(wr(67.5, 3))),
        )
        assertEquals(setOf(second.setIds(0)[0], second.setIds(1)[1]), recordSetIds(listOf(first, second)))
    }

    @Test
    fun inProgressWorkoutMarksOnlyItsOwnSetsAgainstEarlierWorkouts() {
        val first = workout(0, exercise("bench", RecordType.WEIGHT_REPS, S(wr(60.0, 10))))
        // 진행 중인 운동보다 나중에 시작한 기록은 견주지 않는다. 그 기록의 표시도 돌려주지 않는다.
        val later = workout(300, exercise("bench", RecordType.WEIGHT_REPS, S(wr(100.0, 1))))
        val current = workout(
            100,
            exercise("bench", RecordType.WEIGHT_REPS, S(wr(60.0, 10)), S(wr(62.5, 8)), S(wr(70.0, 5), done = false)),
            exercise("pull-up", RecordType.REPS, S(reps(20))),
        ).copy(finishedAt = null)
        // 같은 값은 넘은 것이 아니다. 처음 하는 종목(풀업)과 완료하지 않은 세트에는 표시가 없다.
        assertEquals(setOf(current.setIds(0)[1]), current.recordSetIdsAgainst(listOf(later, first)))
        assertEquals("앞선 기록이 없으면 표시가 없다", emptySet<Long>(), current.recordSetIdsAgainst(listOf(later)))
    }

    @Test
    fun previousOfTheSameRoutineIsTheLatestEarlierOne() {
        val a = workout(0, routineId = 1)
        val b = workout(100, routineId = 2)
        val c = workout(200, routineId = 1)
        val d = workout(300, routineId = 1)
        val all = listOf(d, c, b, a)
        assertEquals(c.id, d.previousOfSameRoutine(all)?.id)
        assertEquals(a.id, c.previousOfSameRoutine(all)?.id)
        assertNull(a.previousOfSameRoutine(all))
        assertNull("빈 운동은 견줄 기록이 없다", workout(400).previousOfSameRoutine(all))
    }
}
