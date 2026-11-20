package io.github.zzunsik.nextset.ui.routine

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.RoutineExercise
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.ui.component.NumberEntry
import io.github.zzunsik.nextset.ui.component.NumberFieldKind

/**
 * 루틴 편집의 종목 행 보조 줄(docs/DESIGN.md `ExerciseRow`의 루틴 편집 상태). "3세트 · 60kg × 8~12회".
 * 목표가 비어 있으면 "3세트"만 적는다.
 */
@Composable
fun RoutineExercise.summaryText(): String {
    val sets = stringResource(R.string.routine_set_count, setCount)
    val target = target.summaryText(exercise.recordType)
    return if (target == null) sets else stringResource(R.string.routine_row_detail, sets, target)
}

/** 목표를 "60kg × 8~12회", "8~12회", "1:00"처럼 적는다. 적을 값이 없으면 null. */
@Composable
fun Target.summaryText(recordType: RecordType): String? {
    val weight = weight?.let { stringResource(R.string.routine_target_weight, it.toKgText()) }
    val reps = repsText()
    return when (recordType) {
        RecordType.WEIGHT_REPS -> when {
            weight != null && reps != null -> stringResource(R.string.routine_target_weight_reps, weight, reps)
            else -> weight ?: reps
        }
        RecordType.REPS -> reps
        RecordType.DURATION -> durationSeconds?.let { NumberEntry.displayTextOf(NumberFieldKind.DURATION, it) }
    }
}

@Composable
private fun Target.repsText(): String? {
    val min = repsMin
    val max = repsMax
    return when {
        min != null && max != null && min == max -> stringResource(R.string.routine_target_reps, min)
        min != null && max != null -> stringResource(R.string.routine_target_reps_range, min, max)
        min != null -> stringResource(R.string.routine_target_reps_at_least, min)
        max != null -> stringResource(R.string.routine_target_reps_at_most, max)
        else -> null
    }
}
