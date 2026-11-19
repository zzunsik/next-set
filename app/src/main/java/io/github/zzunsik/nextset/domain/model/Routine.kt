package io.github.zzunsik.nextset.domain.model

data class Routine(
    val id: Long,
    val name: String,
    /** 루틴 목록에서의 순서. 0부터 센다. */
    val position: Int,
)

/** 목표(RTN-03). 기록 방식에 쓰지 않는 값은 비운다. 모두 비어 있어도 된다(RTN-04). */
data class Target(
    val weight: Weight? = null,
    val repsMin: Int? = null,
    val repsMax: Int? = null,
    val durationSeconds: Int? = null,
) {
    companion object {
        val NONE = Target()
    }
}

data class RoutineExercise(
    val id: Long,
    val routineId: Long,
    val exercise: Exercise,
    val position: Int,
    val setCount: Int,
    val target: Target,
)

data class RoutineDetail(
    val routine: Routine,
    val exercises: List<RoutineExercise>,
)
