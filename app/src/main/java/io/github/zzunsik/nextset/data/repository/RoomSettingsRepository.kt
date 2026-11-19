package io.github.zzunsik.nextset.data.repository

import androidx.room3.withWriteTransaction
import io.github.zzunsik.nextset.data.db.AppSettingsEntity
import io.github.zzunsik.nextset.data.db.NextSetDatabase
import io.github.zzunsik.nextset.data.db.SettingsDao
import io.github.zzunsik.nextset.data.db.Tables
import io.github.zzunsik.nextset.domain.model.Settings
import io.github.zzunsik.nextset.domain.model.ThemeMode
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

internal class RoomSettingsRepository(private val database: NextSetDatabase) : SettingsRepository {
    private val settingsDao = database.settingsDao()

    override fun observeSettings(): Flow<Settings> =
        database.observe(Tables.APP_SETTINGS) { settingsDao.getOrDefault().toModel() }

    override suspend fun setDefaultRestSeconds(seconds: Int) {
        require(Settings.isValidDefaultRest(seconds)) { "기본 휴식 시간 $seconds 초는 고를 수 없는 값이다" }
        update { it.copy(defaultRestSeconds = seconds) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    override suspend fun setPinnedRoutine(routineId: Long?) = update { it.copy(pinnedRoutineId = routineId) }

    override suspend fun setNotificationPromptDeclined(declined: Boolean) =
        update { it.copy(notificationPromptDeclined = declined) }

    private suspend fun update(change: (AppSettingsEntity) -> AppSettingsEntity) {
        database.withWriteTransaction { settingsDao.upsert(change(settingsDao.getOrDefault())) }
    }
}

/** 행이 없으면 기본값. 처음 쓸 때 행을 만든다(docs/architecture.md "app_settings"). */
internal suspend fun SettingsDao.getOrDefault(): AppSettingsEntity = get() ?: AppSettingsEntity(
    defaultRestSeconds = Settings.DEFAULT_REST_SECONDS,
    themeMode = ThemeMode.SYSTEM,
    pinnedRoutineId = null,
)

private fun AppSettingsEntity.toModel() = Settings(
    defaultRestSeconds = defaultRestSeconds,
    themeMode = themeMode,
    pinnedRoutineId = pinnedRoutineId,
    notificationPromptDeclined = notificationPromptDeclined,
)
