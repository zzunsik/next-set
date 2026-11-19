package io.github.zzunsik.nextset.domain.repository

import io.github.zzunsik.nextset.domain.model.Routine
import io.github.zzunsik.nextset.domain.model.RoutineDetail
import io.github.zzunsik.nextset.domain.model.Target
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

    /** 목록 끝에 더하고 식별자를 돌려준다(RTN-01). */
    suspend fun addRoutine(name: String): Long

    suspend fun renameRoutine(id: Long, name: String)

    /** 루틴과 루틴 종목을 지운다. 지난 운동 기록은 이름과 종목, 세트가 그대로 남는다. */
    suspend fun deleteRoutine(id: Long)

    /** 루틴 목록의 순서를 [orderedIds] 순서로 바꾼다(RTN-05). */
    suspend fun reorderRoutines(orderedIds: List<Long>)

    /** 루틴 끝에 종목을 더한다(RTN-02, RTN-03). 목표는 종목의 기록 방식에 맞춰 정리한다. */
    suspend fun addRoutineExercise(routineId: Long, exerciseId: String, setCount: Int, target: Target = Target.NONE): Long

    suspend fun removeRoutineExercise(routineExerciseId: Long)

    suspend fun reorderRoutineExercises(routineId: Long, orderedIds: List<Long>)

    /** 세트 수와 목표를 저장한다(RTN-03, RTN-04). */
    suspend fun updateRoutineExercise(routineExerciseId: Long, setCount: Int, target: Target)
}
