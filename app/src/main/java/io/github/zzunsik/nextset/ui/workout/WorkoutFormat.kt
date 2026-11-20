package io.github.zzunsik.nextset.ui.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.ui.component.NumberEntry
import io.github.zzunsik.nextset.ui.component.NumberFieldKind
import java.time.Duration
import java.time.Instant

/** 시간 "1:30"(docs/DESIGN.md "숫자와 단위"). */
fun durationText(seconds: Int): String = NumberEntry.displayTextOf(NumberFieldKind.DURATION, seconds)

/**
 * 좁은 자리의 세트 값. 직전 기록 열과 줄어든 다음 세트 표시가 쓴다. 횟수 단위를 뗀다("60kg × 10").
 * 횟수만 종목은 "12회", 시간 종목은 "1:00"이다. 값이 하나도 없으면 null이다.
 */
@Composable
fun shortSetText(type: RecordType, value: SetValue): String? = when (type) {
    RecordType.WEIGHT_REPS -> when {
        value.weight != null && value.reps != null ->
            stringResource(R.string.set_value_weight_reps, value.weight.toKgText(), value.reps.toString())
        value.weight != null -> value.weight.toKgText() + stringResource(R.string.unit_kg)
        value.reps != null -> stringResource(R.string.set_value_reps, value.reps.toString())
        else -> null
    }
    RecordType.REPS -> value.reps?.let { stringResource(R.string.set_value_reps, it.toString()) }
    RecordType.DURATION -> value.durationSeconds?.let(::durationText)
}

/** 목표 횟수 범위(LOG-05). "8~12", 같으면 "10", 한쪽만 있으면 그 값. 없으면 null이다. */
@Composable
fun repsRangeText(target: Target): String? {
    val min = target.repsMin
    val max = target.repsMax
    return when {
        min != null && max != null && min != max -> stringResource(R.string.target_range, min, max)
        min != null -> min.toString()
        max != null -> max.toString()
        else -> null
    }
}

/** 운동 시간. 분 단위이고 한 시간을 넘으면 시간과 분이다(docs/DESIGN.md "숫자와 단위"). */
@Composable
fun elapsedText(startedAt: Instant, now: Instant): String {
    val minutes = Duration.between(startedAt, now).toMinutes().coerceAtLeast(0)
    return if (minutes < 60) {
        stringResource(R.string.workout_elapsed_minutes, minutes.toInt())
    } else {
        stringResource(R.string.workout_elapsed_hours_minutes, (minutes / 60).toInt(), (minutes % 60).toInt())
    }
}

@Composable
fun workoutDurationText(minutes: Long): String = if (minutes < 60) {
    stringResource(R.string.workout_duration_minutes, minutes.toInt())
} else {
    stringResource(R.string.workout_duration_hours_minutes, (minutes / 60).toInt(), (minutes % 60).toInt())
}
