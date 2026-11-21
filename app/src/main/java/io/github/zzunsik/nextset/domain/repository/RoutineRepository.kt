package io.github.zzunsik.nextset.domain.repository

import io.github.zzunsik.nextset.domain.model.HomeSnapshot
import io.github.zzunsik.nextset.domain.model.Routine
import io.github.zzunsik.nextset.domain.model.RoutineDetail
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.rule.RoutineUpdatePlan
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    /** 루틴 목록 순서대로(RTN-05). */
    fun observeRoutines(): Flow<List<Routine>>

    fun observeRoutine(id: Long): Flow<RoutineDetail?>

    /**
     * 홈 맨 위에 띄울 루틴. 직접 고른 루틴(HOME-03)이 있으면 그것, 없으면 다음 차례(HOME-02)다.
     * 루틴이 없으면 null이다.
     */
    fun observeTopRoutine(): Flow<Routine?>

    /**
     * 홈 화면이 쓰는 데이터(진행 중인 운동, 루틴과 종목 이름, 루틴마다 마지막 운동, 맨 위 루틴, 직접 고른 루틴)를
     * 관련 테이블이 바뀔 때마다 한 읽기 트랜잭션에서 함께 읽어 내보낸다. 흐름 여럿을 따로 합치면 같은 쓰기 뒤에도 흐름마다
     * 따로 다시 읽어 어긋난 중간 조합이 나오므로(#97) 홈은 이것 하나만 본다.
     */
    fun observeHome(): Flow<HomeSnapshot>

    /** 목록 끝에 더하고 식별자를 돌려준다(RTN-01). */
    suspend fun addRoutine(name: String): Long

    suspend fun renameRoutine(id: Long, name: String)

    /** 루틴과 루틴 종목을 지운다. 지난 운동 기록은 이름과 종목, 세트가 그대로 남는다. */
    suspend fun deleteRoutine(id: Long)

    /**
     * 이름이 [placeholderName] 그대로이고 종목이 없을 때만 루틴을 지운다. 지웠으면 true. 확인과 지우기를 한 번에 해서,
     * 그 사이에 다른 곳(같은 루틴의 다른 편집 화면 등)에서 이름을 붙이거나 종목을 더했으면 지우지 않는다(#103).
     */
    suspend fun discardIfBlank(id: Long, placeholderName: String): Boolean

    /** 루틴 목록의 순서를 [orderedIds] 순서로 바꾼다(RTN-05). */
    suspend fun reorderRoutines(orderedIds: List<Long>)

    /** 루틴 끝에 종목을 더한다(RTN-02, RTN-03). 목표는 종목의 기록 방식에 맞춰 정리한다. */
    suspend fun addRoutineExercise(routineId: Long, exerciseId: String, setCount: Int, target: Target = Target.NONE): Long

    suspend fun removeRoutineExercise(routineExerciseId: Long)

    suspend fun reorderRoutineExercises(routineId: Long, orderedIds: List<Long>)

    /** 세트 수와 목표를 저장한다(RTN-03, RTN-04). */
    suspend fun updateRoutineExercise(routineExerciseId: Long, setCount: Int, target: Target)

    /**
     * 운동을 끝낼 때 [plan]을 루틴에 반영한다(LOG-10). 한 트랜잭션이라 도중에 실패하면 루틴은 반영 전 그대로다.
     * 남는 종목은 세트 수만 바꾸고 목표는 그대로 둔다. 세트 수가 0인 종목(운동에서 세트를 모두 지운 종목)은 뺀다.
     * 더하는 종목의 목표는 비운다(RTN-04). 순서는 [RoutineUpdatePlan.order]대로 0부터 다시 매긴다.
     */
    suspend fun applyRoutineUpdate(plan: RoutineUpdatePlan)
}
