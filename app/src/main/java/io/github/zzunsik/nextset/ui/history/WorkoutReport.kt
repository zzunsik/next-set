package io.github.zzunsik.nextset.ui.history

import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.rule.WorkoutStats
import io.github.zzunsik.nextset.domain.rule.previousOfSameRoutine
import io.github.zzunsik.nextset.domain.rule.stats
import java.time.Instant

/** 세트 행 하나(docs/DESIGN.md `SetRow` 읽기 전용, 기록 수정). */
data class ReportSetUi(
    val id: Long,
    /** 0부터 센다. 화면에는 1부터 적는다. */
    val position: Int,
    val value: SetValue,
    /** 기록 수정에서만 완료하지 않은 세트(값이 빈 세트)가 보인다. */
    val completed: Boolean,
    /** 최고 기록을 세운 세트(HIST-03). */
    val isRecord: Boolean,
)

/** 종목 블록 하나. 세트는 적을 당시의 기록 방식([recordType])대로 보인다(docs/DESIGN.md "기록 상세·수정"). */
data class ReportExerciseUi(
    val id: Long,
    val name: String,
    val recordType: RecordType,
    val note: String?,
    val sets: List<ReportSetUi>,
)

/** 최고 기록 목록의 한 줄. 종목 블록마다 그 운동에서 세운 가장 좋은 기록이다. */
data class ReportRecordUi(val setId: Long, val exerciseName: String, val recordType: RecordType, val value: SetValue)

/**
 * 운동 요약과 기록 상세가 함께 쓰는 내용(HIST-02, HIST-03). 두 화면은 같은 구성이다(#27).
 *
 * @property volumeDeltaGrams 같은 루틴의 바로 앞 기록과 견준 볼륨 차이. 견줄 기록이 없거나 둘 다 볼륨이 없으면 null이다.
 */
data class WorkoutReportUi(
    val workoutId: Long,
    val routineName: String?,
    val startedAt: Instant,
    val finishedAt: Instant,
    val stats: WorkoutStats,
    val volumeDeltaGrams: Long?,
    val records: List<ReportRecordUi>,
    val exercises: List<ReportExerciseUi>,
    val note: String?,
)

/**
 * [workouts](끝난 운동 전부)에서 [workoutId]의 요약을 만든다. 없으면 null이다.
 *
 * @param records 최고 기록을 세운 세트([io.github.zzunsik.nextset.domain.rule.recordSetIds]).
 * @param includeIncomplete 참이면 완료하지 않은 세트와 그런 세트만 있는 종목도 넣는다(기록 수정).
 *   거짓이면 완료한 세트만 보인다. 숫자는 어느 쪽이든 완료한 세트만 센다.
 */
fun buildWorkoutReport(
    workoutId: Long,
    workouts: List<Workout>,
    records: Set<Long>,
    includeIncomplete: Boolean,
): WorkoutReportUi? {
    val workout = workouts.firstOrNull { it.id == workoutId } ?: return null
    val finishedAt = workout.finishedAt ?: return null
    val stats = workout.stats()
    val previous = workout.previousOfSameRoutine(workouts)?.stats()
    val exercises = workout.exercises.sortedBy { it.position }.mapNotNull { exercise ->
        val sets = exercise.sets.sortedBy { it.position }.filter { includeIncomplete || it.isCompleted }
        if (sets.isEmpty() && !includeIncomplete) return@mapNotNull null
        ReportExerciseUi(
            id = exercise.id,
            name = exercise.exercise.nameKo,
            recordType = exercise.recordType,
            note = exercise.note,
            sets = sets.mapIndexed { index, set ->
                ReportSetUi(
                    id = set.id,
                    position = index,
                    value = set.value,
                    completed = set.isCompleted,
                    isRecord = set.isCompleted && set.id in records,
                )
            },
        )
    }
    return WorkoutReportUi(
        workoutId = workout.id,
        routineName = workout.routineName,
        startedAt = workout.startedAt,
        finishedAt = finishedAt,
        stats = stats,
        volumeDeltaGrams = previous
            ?.takeIf { it.volumeGrams > 0 || stats.volumeGrams > 0 }
            ?.let { stats.volumeGrams - it.volumeGrams },
        // 블록 안에서 표시는 앞의 최고를 넘을 때마다 붙으므로 마지막 표시가 그 운동의 가장 좋은 기록이다.
        records = exercises.mapNotNull { block ->
            block.sets.lastOrNull { it.isRecord }?.let { ReportRecordUi(it.id, block.name, block.recordType, it.value) }
        },
        exercises = exercises,
        note = workout.note,
    )
}
