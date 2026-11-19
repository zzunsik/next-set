package io.github.zzunsik.nextset.domain.repository

import io.github.zzunsik.nextset.domain.model.FinishResult
import io.github.zzunsik.nextset.domain.model.PersonalRecord
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetCompletion
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.model.WorkoutSummary
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * 운동 기록. 입력 한 번이 쓰기 한 번이다(NF-09). 진행 중인 운동도 끝난 기록과 같은 곳에 바로 쓴다(LOG-13).
 */
interface WorkoutRepository {
    /** 진행 중인 운동. 없으면 null이다(HOME-06, LOG-13). */
    fun observeInProgressWorkout(): Flow<Workout?>

    fun observeWorkout(id: Long): Flow<Workout?>

    /** 끝난 운동을 시작 시각의 역순으로(HIST-01). */
    fun observeHistory(): Flow<List<WorkoutSummary>>

    /**
     * 루틴에서 운동을 시작한다(HOME-01). 루틴 이름, 종목, 목표, 기록 방식을 복사하고
     * 세트 수만큼 세트를 만들어 미리 채운 값(LOG-03)을 써 둔다.
     * 진행 중인 운동이 있으면 [io.github.zzunsik.nextset.domain.model.WorkoutInProgressException].
     */
    suspend fun startFromRoutine(routineId: Long, now: Instant): Long

    /** 빈 운동을 시작한다(HOME-05). 진행 중인 운동이 있으면 위와 같은 예외다. */
    suspend fun startEmpty(now: Instant): Long

    /**
     * 지난 날짜의 운동을 끝난 기록으로 바로 만든다(HIST-06). 종목과 세트는 아래 함수로 더한다.
     * 세트는 완료([completeSet])해야 기록으로 친다.
     */
    suspend fun addPastWorkout(startedAt: Instant, finishedAt: Instant): Long

    /** 운동 끝에 종목을 더한다. 종목의 지금 기록 방식을 복사하고 [setCount]만큼 세트를 미리 채워 만든다. */
    suspend fun addExercise(workoutId: Long, exerciseId: String, setCount: Int = 1): Long

    suspend fun removeExercise(workoutExerciseId: Long)

    suspend fun reorderExercises(workoutId: Long, orderedIds: List<Long>)

    /** 종목 끝에 세트를 더한다(LOG-09). 값은 미리 채운다(LOG-03). */
    suspend fun addSet(workoutExerciseId: Long): Long

    suspend fun deleteSet(setId: Long)

    /** 세트 값을 저장한다. 기록 방식에 쓰지 않는 값은 비운다. */
    suspend fun updateSet(setId: Long, value: SetValue)

    /**
     * 세트를 완료한다(LOG-04). 진행 중인 운동에서 최고 기록을 넘으면 [SetCompletion.isNewRecord]가 참이다(LOG-14).
     * 지난 기록을 고칠 때는 알리지 않는다.
     */
    suspend fun completeSet(setId: Long, now: Instant): SetCompletion

    suspend fun uncompleteSet(setId: Long)

    suspend fun updateWorkoutNote(workoutId: Long, note: String?)

    suspend fun updateExerciseNote(workoutExerciseId: Long, note: String?)

    /** 지난 기록의 시각을 고친다(HIST-05). */
    suspend fun updateWorkoutTimes(workoutId: Long, startedAt: Instant, finishedAt: Instant)

    /**
     * 운동을 끝낸다. 완료하지 않은 세트와 세트가 남지 않은 종목을 지우고 끝난 시각을 쓴다.
     * 완료한 세트가 없으면 기록을 지운다. 그 루틴이 직접 고른 루틴이면 고른 것을 푼다(HOME-03).
     */
    suspend fun finishWorkout(workoutId: Long, now: Instant): FinishResult

    /** 진행 중인 운동을 버린다. 직접 고른 루틴은 그대로 둔다. */
    suspend fun discardWorkout(workoutId: Long)

    /** 지난 기록을 지운다(HIST-05). */
    suspend fun deleteWorkout(workoutId: Long)

    /**
     * 직전 기록(LOG-03). [before]보다 먼저 시작해 끝난 운동 가운데 [exerciseId]를 [recordType]으로 적고
     * 완료한 세트가 있는 가장 늦은 것에서, 완료한 세트를 순번 순서로 돌려준다. 없으면 빈 목록이다.
     */
    suspend fun getPreviousSets(exerciseId: String, recordType: RecordType, before: Instant): List<SetValue>

    /** 직전 기록에서 순번 [position](0부터)의 세트. 없으면 null이다. */
    suspend fun getPreviousSet(exerciseId: String, recordType: RecordType, before: Instant, position: Int): SetValue?

    /** 종목의 지금 기록 방식으로 적은 세트 가운데 최고 기록(HIST-04). 없으면 null이다. */
    fun observePersonalRecord(exerciseId: String): Flow<PersonalRecord?>
}
