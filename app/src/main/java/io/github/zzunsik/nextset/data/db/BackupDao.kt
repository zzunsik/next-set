package io.github.zzunsik.nextset.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query

/** 백업 내보내기와 가져오기(DATA-03, DATA-04)가 테이블 전체를 한 번에 읽고 비우는 질의. */
@Dao
internal interface BackupDao {
    @Query("SELECT * FROM exercise ORDER BY id")
    suspend fun getExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM routine ORDER BY position, id")
    suspend fun getRoutines(): List<RoutineEntity>

    @Query("SELECT * FROM routine_exercise ORDER BY routine_id, position, id")
    suspend fun getRoutineExercises(): List<RoutineExerciseEntity>

    @Query("SELECT * FROM workout ORDER BY started_at, id")
    suspend fun getWorkouts(): List<WorkoutEntity>

    @Query("SELECT * FROM workout_exercise ORDER BY workout_id, position, id")
    suspend fun getWorkoutExercises(): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_set ORDER BY workout_exercise_id, position, id")
    suspend fun getSets(): List<WorkoutSetEntity>

    @Query("SELECT COUNT(*) FROM workout WHERE finished_at IS NOT NULL")
    suspend fun countFinishedWorkouts(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM workout WHERE finished_at IS NULL)")
    suspend fun hasWorkoutInProgress(): Boolean

    @Query("SELECT COUNT(*) FROM routine")
    suspend fun countRoutines(): Int

    // 지우는 순서: 운동 기록(종목과 세트가 함께 지워진다), 루틴(루틴 종목이 함께 지워지고 직접 고른 루틴이 비워진다), 종목.
    // 운동 기록 종목이 종목을 RESTRICT로 가리키므로 종목을 마지막에 지운다.

    @Query("DELETE FROM workout")
    suspend fun deleteWorkouts()

    @Query("DELETE FROM routine")
    suspend fun deleteRoutines()

    @Query("DELETE FROM exercise")
    suspend fun deleteExercises()

    @Insert
    suspend fun insertExercises(exercises: List<ExerciseEntity>)
}
