package io.github.zzunsik.nextset.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import io.github.zzunsik.nextset.domain.model.RecordType

/** 최고 기록 질의의 한 행. */
internal data class BestSetRow(
    val setId: Long,
    val workoutId: Long,
    val weightG: Int?,
    val reps: Int?,
    val durationS: Int?,
)

@Dao
internal interface WorkoutDao {
    @Query("SELECT id FROM workout WHERE finished_at IS NULL ORDER BY started_at DESC, id DESC LIMIT 1")
    suspend fun getInProgressId(): Long?

    @Query("SELECT * FROM workout WHERE id = :id")
    suspend fun getWorkout(id: Long): WorkoutEntity?

    @Query("SELECT * FROM workout WHERE finished_at IS NOT NULL ORDER BY started_at DESC, id DESC")
    suspend fun getFinishedWorkouts(): List<WorkoutEntity>

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Query("DELETE FROM workout WHERE id = :id")
    suspend fun deleteWorkout(id: Long): Int

    @Query("SELECT * FROM workout_exercise WHERE workout_id = :workoutId ORDER BY position, id")
    suspend fun getExercises(workoutId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercise WHERE id = :id")
    suspend fun getExercise(id: Long): WorkoutExerciseEntity?

    @Insert
    suspend fun insertExercise(exercise: WorkoutExerciseEntity): Long

    @Query("UPDATE workout_exercise SET position = :position WHERE id = :id")
    suspend fun setExercisePosition(id: Long, position: Int)

    @Query("UPDATE workout_exercise SET note = :note WHERE id = :id")
    suspend fun setExerciseNote(id: Long, note: String?): Int

    @Query("DELETE FROM workout_exercise WHERE id = :id")
    suspend fun deleteExercise(id: Long)

    @Query(
        """
        SELECT s.* FROM workout_set s
        JOIN workout_exercise we ON we.id = s.workout_exercise_id
        WHERE we.workout_id = :workoutId
        ORDER BY s.workout_exercise_id, s.position, s.id
        """,
    )
    suspend fun getSetsOfWorkout(workoutId: Long): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_set WHERE workout_exercise_id = :workoutExerciseId ORDER BY position, id")
    suspend fun getSets(workoutExerciseId: Long): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_set WHERE id = :id")
    suspend fun getSet(id: Long): WorkoutSetEntity?

    @Insert
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Query("UPDATE workout_set SET weight_g = :weightG, reps = :reps, duration_s = :durationS WHERE id = :id")
    suspend fun setSetValue(id: Long, weightG: Int?, reps: Int?, durationS: Int?)

    @Query("UPDATE workout_set SET completed_at = :completedAt WHERE id = :id")
    suspend fun setCompletedAt(id: Long, completedAt: Long?)

    @Query("UPDATE workout_set SET position = :position WHERE id = :id")
    suspend fun setSetPosition(id: Long, position: Int)

    @Query("DELETE FROM workout_set WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query(
        """
        DELETE FROM workout_set
        WHERE completed_at IS NULL
          AND workout_exercise_id IN (SELECT id FROM workout_exercise WHERE workout_id = :workoutId)
        """,
    )
    suspend fun deleteUncompletedSets(workoutId: Long)

    @Query(
        """
        DELETE FROM workout_exercise
        WHERE workout_id = :workoutId
          AND NOT EXISTS (SELECT 1 FROM workout_set s WHERE s.workout_exercise_id = workout_exercise.id)
        """,
    )
    suspend fun deleteExercisesWithoutSets(workoutId: Long)

    /**
     * 직전 기록(LOG-03, docs/architecture.md "직전 기록").
     * [startedAt]보다 먼저 시작해 끝난 운동 가운데 [exerciseId]를 [recordType]으로 적고 완료한 세트가 있는
     * 가장 늦은 운동에서 완료한 세트를 순번 순서로 가져온다. 한 운동에 같은 종목이 둘이면 앞의 것이다.
     */
    @Query(
        """
        SELECT s.* FROM workout_set s
        WHERE s.completed_at IS NOT NULL
          AND s.workout_exercise_id = (
            SELECT we.id
            FROM workout_exercise we
            JOIN workout w ON w.id = we.workout_id
            WHERE we.exercise_id = :exerciseId
              AND we.record_type = :recordType
              AND w.finished_at IS NOT NULL
              AND w.started_at < :startedAt
              AND EXISTS (
                SELECT 1 FROM workout_set s2
                WHERE s2.workout_exercise_id = we.id AND s2.completed_at IS NOT NULL
              )
            ORDER BY w.started_at DESC, w.id DESC, we.position ASC, we.id ASC
            LIMIT 1
          )
        ORDER BY s.position, s.id
        """,
    )
    suspend fun getPreviousSets(exerciseId: String, recordType: RecordType, startedAt: Long): List<WorkoutSetEntity>

    /**
     * [startedAt]보다 먼저 시작한 다른 운동에 같은 방식으로 완료한 세트가 있는지.
     * 없으면 최고 기록을 알리지 않는다(docs/architecture.md "최고 기록").
     */
    @Query(
        """
        SELECT EXISTS(
          SELECT 1 FROM workout_set s
          JOIN workout_exercise we ON we.id = s.workout_exercise_id
          JOIN workout w ON w.id = we.workout_id
          WHERE we.exercise_id = :exerciseId
            AND we.record_type = :recordType
            AND s.completed_at IS NOT NULL
            AND w.id != :workoutId
            AND w.started_at < :startedAt
        )
        """,
    )
    suspend fun hasEarlierRecord(exerciseId: String, recordType: RecordType, workoutId: Long, startedAt: Long): Boolean

    // 최고 기록(HIST-04). 비교 규칙은 domain/rule/PersonalRecordRule.kt의 isBetter와 같다.
    // 값이 모두 같으면 먼저 시작한 운동, 앞 순번의 세트가 남는다. :excludeSetId는 방금 완료한 세트다.

    @Query(
        """
        SELECT s.id AS setId, w.id AS workoutId, s.weight_g AS weightG, s.reps AS reps, s.duration_s AS durationS
        FROM workout_set s
        JOIN workout_exercise we ON we.id = s.workout_exercise_id
        JOIN workout w ON w.id = we.workout_id
        WHERE we.exercise_id = :exerciseId
          AND we.record_type = 'WEIGHT_REPS'
          AND s.completed_at IS NOT NULL
          AND s.id != :excludeSetId
        ORDER BY s.weight_g DESC, s.reps DESC, w.started_at ASC, w.id ASC, we.position ASC, s.position ASC, s.id ASC
        LIMIT 1
        """,
    )
    suspend fun bestWeightReps(exerciseId: String, excludeSetId: Long): BestSetRow?

    @Query(
        """
        SELECT s.id AS setId, w.id AS workoutId, s.weight_g AS weightG, s.reps AS reps, s.duration_s AS durationS
        FROM workout_set s
        JOIN workout_exercise we ON we.id = s.workout_exercise_id
        JOIN workout w ON w.id = we.workout_id
        WHERE we.exercise_id = :exerciseId
          AND we.record_type = 'REPS'
          AND s.completed_at IS NOT NULL
          AND s.id != :excludeSetId
        ORDER BY s.reps DESC, w.started_at ASC, w.id ASC, we.position ASC, s.position ASC, s.id ASC
        LIMIT 1
        """,
    )
    suspend fun bestReps(exerciseId: String, excludeSetId: Long): BestSetRow?

    @Query(
        """
        SELECT s.id AS setId, w.id AS workoutId, s.weight_g AS weightG, s.reps AS reps, s.duration_s AS durationS
        FROM workout_set s
        JOIN workout_exercise we ON we.id = s.workout_exercise_id
        JOIN workout w ON w.id = we.workout_id
        WHERE we.exercise_id = :exerciseId
          AND we.record_type = 'DURATION'
          AND s.completed_at IS NOT NULL
          AND s.id != :excludeSetId
        ORDER BY s.duration_s DESC, w.started_at ASC, w.id ASC, we.position ASC, s.position ASC, s.id ASC
        LIMIT 1
        """,
    )
    suspend fun bestDuration(exerciseId: String, excludeSetId: Long): BestSetRow?

    companion object {
        /** 빼는 세트가 없을 때 쓰는 값. 자동 증가 식별자는 1부터라 겹치지 않는다. */
        const val NO_SET = 0L
    }
}
