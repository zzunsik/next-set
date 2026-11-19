package io.github.zzunsik.nextset.domain.repository

import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.ExerciseDeleteResult
import io.github.zzunsik.nextset.domain.model.ExerciseDraft
import io.github.zzunsik.nextset.domain.model.ExerciseFilter
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    /** 숨기지 않은 종목을 [filter]로 걸러 한국어 이름순으로 내보낸다(EX-02, EX-03). */
    fun observeExercises(filter: ExerciseFilter = ExerciseFilter()): Flow<List<Exercise>>

    /** 숨긴 종목도 찾는다. 지난 기록에서 쓴다. */
    suspend fun getExercise(id: String): Exercise?

    /** 직접 만든 종목을 더하고 식별자(`custom-` + UUID)를 돌려준다(EX-04). */
    suspend fun addCustomExercise(draft: ExerciseDraft): String

    /**
     * 직접 만든 종목을 고친다(EX-04). 기록 방식이 바뀌면 같은 트랜잭션에서 루틴의 맞지 않는 목표를 비운다.
     * 지난 세트와 진행 중인 운동의 방식은 그대로다. 내장 종목이면 [IllegalArgumentException].
     */
    suspend fun updateCustomExercise(id: String, draft: ExerciseDraft)

    /**
     * 직접 만든 종목을 지운다(EX-04). 기록에 쓰인 종목은 지우지 않고 숨기며 루틴에서 뺀다.
     * 내장 종목이면 [IllegalArgumentException].
     */
    suspend fun deleteCustomExercise(id: String): ExerciseDeleteResult

    /** 종목별 휴식 시간(REST-03). null이면 설정의 기본값을 쓴다. 내장 종목에도 쓸 수 있다. */
    suspend fun setRestSeconds(id: String, seconds: Int?)
}
