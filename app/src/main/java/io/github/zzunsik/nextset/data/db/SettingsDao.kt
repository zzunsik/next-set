package io.github.zzunsik.nextset.data.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
internal interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettingsEntity?

    @Upsert
    suspend fun upsert(settings: AppSettingsEntity)

    /** 그 루틴으로 운동을 마치면 직접 고른 루틴을 푼다(HOME-03). */
    @Query("UPDATE app_settings SET pinned_routine_id = NULL WHERE pinned_routine_id = :routineId")
    suspend fun unpinIf(routineId: Long)
}
