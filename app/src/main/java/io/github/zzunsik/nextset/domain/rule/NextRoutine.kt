package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.Routine

/**
 * 다음 차례 루틴(HOME-02). [routines]는 목록 순서대로다.
 * [lastRoutineId]는 루틴에서 출발해 끝난 운동 가운데 가장 늦게 시작한 것의 루틴이다.
 * 없거나 목록에 없으면 첫 루틴, 마지막 루틴 다음은 첫 루틴이다.
 */
fun nextRoutine(routines: List<Routine>, lastRoutineId: Long?): Routine? {
    if (routines.isEmpty()) return null
    val index = routines.indexOfFirst { it.id == lastRoutineId }
    return routines[(index + 1) % routines.size]
}

/** 홈 맨 위에 띄울 루틴. 직접 고른 루틴(HOME-03)이 있으면 그것, 없으면 다음 차례다. */
fun topRoutine(routines: List<Routine>, pinnedRoutineId: Long?, lastRoutineId: Long?): Routine? =
    routines.firstOrNull { it.id == pinnedRoutineId } ?: nextRoutine(routines, lastRoutineId)
