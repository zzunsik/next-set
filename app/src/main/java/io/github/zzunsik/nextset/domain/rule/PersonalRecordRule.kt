package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue

/**
 * [candidate]가 [best]보다 좋은지(HIST-04). 두 세트는 같은 기록 방식([type])으로 적은 것이다.
 * 무게와 횟수는 무게가 무거운 쪽, 무게가 같으면 횟수가 많은 쪽이다. 값이 모두 같으면 좋지 않다.
 * 빈 값은 어떤 값보다도 작게 본다. 데이터베이스의 최고 기록 질의(`ORDER BY ... DESC`)와 같은 순서다.
 */
fun isBetter(type: RecordType, candidate: SetValue, best: SetValue): Boolean = when (type) {
    RecordType.WEIGHT_REPS -> {
        val byWeight = compareNullable(candidate.weight?.grams, best.weight?.grams)
        byWeight > 0 || (byWeight == 0 && compareNullable(candidate.reps, best.reps) > 0)
    }
    RecordType.REPS -> compareNullable(candidate.reps, best.reps) > 0
    RecordType.DURATION -> compareNullable(candidate.durationSeconds, best.durationSeconds) > 0
}

/**
 * 방금 완료한 세트가 최고 기록을 넘었는지(LOG-14).
 * 같은 방식으로 적은 이전 기록이 없으면([best]가 null) 알리지 않는다.
 */
fun isNewRecord(type: RecordType, candidate: SetValue, best: SetValue?): Boolean =
    best != null && isBetter(type, candidate, best)

/** 세트 목록에서 가장 좋은 값. 같으면 앞의 것이 남는다. */
fun bestOf(type: RecordType, values: List<SetValue>): SetValue? =
    values.fold(null as SetValue?) { best, value ->
        if (best == null || isBetter(type, value, best)) value else best
    }

private fun compareNullable(a: Int?, b: Int?): Int = when {
    a == null && b == null -> 0
    a == null -> -1
    b == null -> 1
    else -> a.compareTo(b)
}
