package io.github.zzunsik.nextset.ui.history

import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.rule.recordSetIds
import io.github.zzunsik.nextset.ui.workout.WorkoutSamples
import io.github.zzunsik.nextset.ui.workout.WorkoutSamples.done
import io.github.zzunsik.nextset.ui.workout.WorkoutSamples.todo
import io.github.zzunsik.nextset.ui.workout.WorkoutSamples.wr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 요약과 기록 상세가 함께 쓰는 내용(HIST-02, HIST-03)과 숫자 적는 법. 값은 지어낸 것이다. */
class WorkoutReportTest {
    private fun finished(id: Long, startMinutes: Long, routineId: Long?, vararg blocks: io.github.zzunsik.nextset.domain.model.WorkoutExercise) =
        Workout(
            id = id,
            routineId = routineId,
            routineName = routineId?.let { "가슴 루틴" },
            startedAt = WorkoutSamples.startedAt.plusSeconds(startMinutes * 60),
            finishedAt = WorkoutSamples.startedAt.plusSeconds((startMinutes + 48) * 60),
            note = null,
            exercises = blocks.toList(),
        )

    private fun bench(id: Long, sets: List<WorkoutSamples.SampleSet>) =
        WorkoutSamples.block(id, "벤치 프레스", sets).copy(exercise = WorkoutSamples.exercise("bench", "벤치 프레스"))

    @Test
    fun reportShowsCompletedSetsRecordsAndVolumeAgainstTheSameRoutine() {
        val before = finished(1, 0, 1, bench(1, listOf(done(60.0, 10))))
        val now = finished(
            2, 10_000, 1,
            bench(2, listOf(done(62.5, 8), done(65.0, 5), todo(70.0, 3))),
            WorkoutSamples.block(3, "풀업", listOf(WorkoutSamples.SampleSet(SetValue(reps = 12), done = true)), RecordType.REPS),
        )
        val workouts = listOf(now, before)
        val report = buildWorkoutReport(2, workouts, recordSetIds(workouts), includeIncomplete = false)!!

        assertEquals(48L, report.stats.minutes)
        assertEquals(3, report.stats.setCount)
        assertEquals(62_500L * 8 + 65_000L * 5, report.stats.volumeGrams)
        assertEquals("지난번 600kg에서 825kg", 825_000L - 600_000L, report.volumeDeltaGrams)
        assertEquals(listOf(2, 1), report.exercises.map { it.sets.size })
        assertEquals(listOf(true, true), report.exercises[0].sets.map { it.isRecord })
        // 최고 기록 목록은 종목마다 그 운동에서 가장 좋은 기록 하나다. 풀업은 처음이라 없다.
        assertEquals(listOf(wr(65.0, 5)), report.records.map { it.value })

        val editing = buildWorkoutReport(2, workouts, recordSetIds(workouts), includeIncomplete = true)!!
        assertEquals("수정 상태는 완료하지 않은 세트도 보인다", listOf(3, 1), editing.exercises.map { it.sets.size })
        assertEquals(3, editing.stats.setCount)
    }

    @Test
    fun noDeltaWithoutAnEarlierWorkoutOfTheSameRoutine() {
        val empty = finished(1, 0, null, WorkoutSamples.block(1, "벤치 프레스", listOf(done(60.0, 10))))
        val report = buildWorkoutReport(1, listOf(empty), emptySet(), includeIncomplete = false)!!
        assertNull(report.volumeDeltaGrams)
        assertNull(buildWorkoutReport(99, listOf(empty), emptySet(), includeIncomplete = false))
    }

    @Test
    fun volumeNumbersUseThousandsSeparatorsAndKeepFractions() {
        assertEquals("4,250", volumeNumberText(4_250_000))
        assertEquals("187.5", volumeNumberText(187_500))
        assertEquals("1,234,567.25", volumeNumberText(1_234_567_250))
        assertEquals("0", volumeNumberText(0))
        assertEquals("-120", volumeNumberText(-120_000))
    }

    @Test
    fun objectParticleFollowsTheLastSyllable() {
        assertEquals("가슴 루틴을", withObjectParticle("가슴 루틴"))
        assertEquals("하체 데이를", withObjectParticle("하체 데이"))
        assertEquals("루틴 3을", withObjectParticle("루틴 3"))
        assertEquals("루틴 2를", withObjectParticle("루틴 2"))
        assertNull(withObjectParticle("Push"))
    }

    /** #93: "Push Day A을(를) 끝냈어요."로 나왔다. 홀로 선 영어 글자는 글자 이름(에이)으로 읽는다. */
    @Test
    fun objectParticleReadsStandaloneLatinLetters() {
        assertEquals("Push Day A를", withObjectParticle("Push Day A"))
        assertEquals("Leg Day L을", withObjectParticle("Leg Day L"))
        assertEquals("가슴 (B)를", withObjectParticle("가슴 (B)"))
    }
}
