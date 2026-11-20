package io.github.zzunsik.nextset.domain.model

import java.time.Instant

/** 세트에 적는 값. 어느 값을 쓰는지는 기록 방식이 정한다. */
data class SetValue(
    val weight: Weight? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
) {
    companion object {
        val EMPTY = SetValue()
    }
}

data class WorkoutSet(
    val id: Long,
    val workoutExerciseId: Long,
    /** 순번. 0부터 센다. */
    val position: Int,
    val value: SetValue,
    /** 비어 있으면 아직 완료하지 않은 세트다. */
    val completedAt: Instant?,
) {
    val isCompleted: Boolean get() = completedAt != null
}

data class WorkoutExercise(
    val id: Long,
    val workoutId: Long,
    val exercise: Exercise,
    val position: Int,
    /** 이 행을 만들 때의 기록 방식 사본. 세트는 이 방식으로 읽는다. */
    val recordType: RecordType,
    val note: String?,
    /** 시작할 때의 목표 사본. */
    val target: Target,
    val sets: List<WorkoutSet>,
)

data class Workout(
    val id: Long,
    val routineId: Long?,
    /** 시작할 때의 루틴 이름 사본. 빈 운동이면 비어 있다. */
    val routineName: String?,
    val startedAt: Instant,
    /** 비어 있으면 진행 중이다. */
    val finishedAt: Instant?,
    val note: String?,
    val exercises: List<WorkoutExercise>,
    /**
     * 시작한 뒤 사용자가 고친 것이 있다. 세트 값을 바꾸거나, 세트를 완료하거나, 세트·종목을 더하고 빼고 옮기거나,
     * 메모를 적으면 참이 된다. 미리 채운 값은 저장하지 않아 나중에 견줄 수 없으므로 고칠 때 저장해 둔다.
     * 손대지 않았는지는 [io.github.zzunsik.nextset.domain.rule.isUntouched]가 정한다.
     */
    val isEdited: Boolean = false,
) {
    val isInProgress: Boolean get() = finishedAt == null
}

/** 기록 목록(HIST-01)의 한 줄. */
data class WorkoutSummary(
    val id: Long,
    val routineId: Long?,
    val routineName: String?,
    val startedAt: Instant,
    val finishedAt: Instant,
    val note: String?,
)

/** 종목의 최고 기록 세트(HIST-04). */
data class PersonalRecord(
    val setId: Long,
    val workoutId: Long,
    val recordType: RecordType,
    val value: SetValue,
)

/** 세트를 완료한 결과. [isNewRecord]가 참이면 그 자리에서 알린다(LOG-14). */
data class SetCompletion(val isNewRecord: Boolean)

/** 운동을 끝낸 결과. 완료한 세트가 하나도 없으면 기록을 남기지 않고 버린다. */
enum class FinishResult { FINISHED, DISCARDED_EMPTY }

/** 진행 중인 운동이 이미 있어 새 운동을 시작하지 못했다. 진행 중인 운동은 많아야 하나다. */
class WorkoutInProgressException(val workoutId: Long) :
    IllegalStateException("진행 중인 운동($workoutId)이 이미 있다")
