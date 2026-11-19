package io.github.zzunsik.nextset.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update

@Dao
internal interface ExerciseDao {
    @Query("SELECT * FROM exercise ORDER BY name_ko, id")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercise WHERE id IN (:ids)")
    suspend fun getByIds(ids: Collection<String>): List<ExerciseEntity>

    @Insert
    suspend fun insert(exercise: ExerciseEntity)

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("DELETE FROM exercise WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE exercise SET is_archived = :archived WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Query("UPDATE exercise SET rest_seconds = :seconds WHERE id = :id")
    suspend fun setRestSeconds(id: String, seconds: Int?): Int

    /** 운동 기록에 한 번이라도 쓰였는지. 쓰였으면 지우지 않고 숨긴다. */
    @Query("SELECT EXISTS(SELECT 1 FROM workout_exercise WHERE exercise_id = :id)")
    suspend fun isUsedInWorkouts(id: String): Boolean
}
