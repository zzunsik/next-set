package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Target

/**
 * 입력 칸에 미리 채울 값(LOG-03).
 * 1. 직전 기록에 같은 순번의 세트가 있으면 그 값
 * 2. 없으면 목표. 횟수는 목표 하한을 쓴다
 * 3. 그것도 없으면 빈칸
 * 기록 방식에 쓰지 않는 값은 비운다.
 */
fun prefillValue(type: RecordType, previous: SetValue?, target: Target): SetValue {
    val value = previous ?: SetValue(
        weight = target.weight,
        reps = target.repsMin ?: target.repsMax,
        durationSeconds = target.durationSeconds,
    )
    return value.fitTo(type)
}

/** 기록 방식에 쓰지 않는 값을 비운다. */
fun SetValue.fitTo(type: RecordType): SetValue = when (type) {
    RecordType.WEIGHT_REPS -> SetValue(weight = weight, reps = reps)
    RecordType.REPS -> SetValue(reps = reps)
    RecordType.DURATION -> SetValue(durationSeconds = durationSeconds)
}

/** 기록 방식에 쓰지 않는 목표를 비운다. 종목의 방식을 바꾸면 루틴 목표에 적용한다. */
fun Target.fitTo(type: RecordType): Target = when (type) {
    RecordType.WEIGHT_REPS -> Target(weight = weight, repsMin = repsMin, repsMax = repsMax)
    RecordType.REPS -> Target(repsMin = repsMin, repsMax = repsMax)
    RecordType.DURATION -> Target(durationSeconds = durationSeconds)
}

/** 세트 값은 0 이상이다. 어긋나면 [IllegalArgumentException]. */
fun SetValue.validate() {
    require(weight == null || weight.grams >= 0) { "무게는 0 이상이어야 한다" }
    require(reps == null || reps >= 0) { "횟수는 0 이상이어야 한다" }
    require(durationSeconds == null || durationSeconds >= 0) { "시간은 0 이상이어야 한다" }
}
