package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.Target

/** 루틴 종목의 세트 수와 목표를 확인한다(RTN-03, RTN-04). 어긋나면 [IllegalArgumentException]. */
fun validateRoutineExercise(setCount: Int, target: Target) {
    require(setCount >= 1) { "세트 수는 1 이상이어야 한다" }
    target.validate()
}

fun Target.validate() {
    require(weight == null || weight.grams >= 0) { "목표 무게는 0 이상이어야 한다" }
    require(repsMin == null || repsMin >= 0) { "목표 횟수는 0 이상이어야 한다" }
    require(repsMax == null || repsMax >= 0) { "목표 횟수는 0 이상이어야 한다" }
    require(repsMin == null || repsMax == null || repsMin <= repsMax) { "목표 횟수 하한이 상한보다 크다" }
    require(durationSeconds == null || durationSeconds >= 0) { "목표 시간은 0 이상이어야 한다" }
}

/** 순서를 바꾼 결과가 같은 항목을 빠짐없이 한 번씩 담는지 확인한다. */
fun <T> requireSameItems(current: Collection<T>, reordered: List<T>) {
    require(reordered.size == current.size && reordered.toSet() == current.toSet()) {
        "순서를 바꿀 항목이 지금 목록과 다르다"
    }
}
