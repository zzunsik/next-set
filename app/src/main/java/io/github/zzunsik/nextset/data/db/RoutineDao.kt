package io.github.zzunsik.nextset.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update

@Dao
internal interface RoutineDao {
    @Query("SELECT * FROM routine ORDER BY position, id")
    suspend fun getAll(): List<RoutineEntity>

    @Query("SELECT * FROM routine WHERE id = :id")
    suspend fun getById(id: Long): RoutineEntity?

    @Query("SELECT COUNT(*) FROM routine")
    suspend fun count(): Int

    @Insert
    suspend fun insert(routine: RoutineEntity): Long

    @Query("UPDATE routine SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String): Int

    @Query("UPDATE routine SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int)

    @Query("DELETE FROM routine WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM routine_exercise WHERE routine_id = :routineId ORDER BY position, id")
    suspend fun getItems(routineId: Long): List<RoutineExerciseEntity>

    @Query("SELECT * FROM routine_exercise WHERE id = :id")
    suspend fun getItem(id: Long): RoutineExerciseEntity?

    @Query("SELECT * FROM routine_exercise WHERE exercise_id = :exerciseId")
    suspend fun getItemsByExercise(exerciseId: String): List<RoutineExerciseEntity>

    @Insert
    suspend fun insertItem(item: RoutineExerciseEntity): Long

    @Update
    suspend fun updateItem(item: RoutineExerciseEntity)

    @Query("UPDATE routine_exercise SET position = :position WHERE id = :id")
    suspend fun setItemPosition(id: Long, position: Int)

    @Query("DELETE FROM routine_exercise WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM routine_exercise WHERE exercise_id = :exerciseId")
    suspend fun deleteItemsByExercise(exerciseId: String)

    /** 루틴에서 출발해 끝난 운동 가운데 가장 늦게 시작한 것의 루틴(HOME-02). */
    @Query(
        """
        SELECT routine_id FROM workout
        WHERE finished_at IS NOT NULL AND routine_id IS NOT NULL
        ORDER BY started_at DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun lastFinishedRoutineId(): Long?
}
