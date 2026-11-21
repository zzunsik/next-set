package io.github.zzunsik.nextset.ui.history

import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.model.WorkoutExercise
import io.github.zzunsik.nextset.domain.rule.WorkoutStats
import io.github.zzunsik.nextset.domain.rule.recordSetIds
import io.github.zzunsik.nextset.ui.workout.WorkoutSamples
import io.github.zzunsik.nextset.ui.workout.WorkoutSamples.SampleSet
import io.github.zzunsik.nextset.ui.workout.WorkoutSamples.done
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/** 기록 화면 테스트의 견본. 루틴, 종목 이름과 값은 지어낸 것이다. 시간대는 UTC다. */
object HistorySamples {
    val zone: ZoneOffset = ZoneOffset.UTC
    val today: LocalDate = LocalDate.of(2026, 3, 20)

    fun list(): HistoryUiState = HistoryUiState(
        loading = false,
        today = today,
        months = listOf(
            HistoryMonthUi(
                YearMonth.of(2026, 3),
                listOf(
                    row(3, "가슴 루틴", LocalDate.of(2026, 3, 14), 48, 15, 4_250_000, record = true),
                    row(2, null, LocalDate.of(2026, 3, 12), 25, 6, 0),
                    row(1, "하체 루틴", LocalDate.of(2026, 3, 9), 72, 18, 12_337_500),
                ),
            ),
            HistoryMonthUi(
                YearMonth.of(2025, 12),
                listOf(row(0, "등과 이두 루틴", LocalDate.of(2025, 12, 30), 55, 16, 5_120_000)),
            ),
        ),
    )

    private fun row(id: Long, name: String?, date: LocalDate, minutes: Long, sets: Int, volume: Long, record: Boolean = false) =
        HistoryRowUi(id, name, date, WorkoutStats(minutes, sets, volume), record)

    private fun bench(id: Long, sets: List<SampleSet>) =
        WorkoutSamples.block(id, "벤치 프레스", sets).copy(exercise = WorkoutSamples.exercise("bench", "벤치 프레스"))

    private fun pullUp(id: Long, vararg reps: Int) = WorkoutSamples.block(
        id, "풀업", reps.map { SampleSet(SetValue(reps = it), done = true) }, RecordType.REPS,
    ).copy(exercise = WorkoutSamples.exercise("pull-up", "풀업", RecordType.REPS))

    private fun finished(id: Long, daysAgo: Long, minutes: Long, note: String?, vararg blocks: WorkoutExercise): Workout {
        val start = WorkoutSamples.startedAt.minusSeconds(daysAgo * 24 * 3600)
        return Workout(id, 1, "가슴 루틴", start, start.plusSeconds(minutes * 60), note, blocks.mapIndexed { i, b -> b.copy(position = i) })
    }

    /** 지난번 기록이 있는 가슴 루틴. 벤치 둘째, 셋째 세트와 풀업 둘째 세트가 최고 기록이다. */
    fun workouts(note: String? = "그립을 한 뼘 넓게 잡았다."): List<Workout> {
        val before = finished(1, 7, 50, null, bench(1, listOf(done(60.0, 10), done(60.0, 8))), pullUp(2, 10, 9))
        val now = finished(
            2, 0, 52, note,
            bench(3, listOf(done(60.0, 10), done(62.5, 8), done(65.0, 5))),
            pullUp(4, 10, 11),
        )
        return listOf(now, before)
    }

    fun report(editing: Boolean = false, note: String? = "그립을 한 뼘 넓게 잡았다."): ReportUiState {
        val workouts = workouts(note)
        return ReportUiState(
            loading = false,
            report = buildWorkoutReport(2, workouts, recordSetIds(workouts), includeIncomplete = editing),
            editing = editing,
        )
    }
}
