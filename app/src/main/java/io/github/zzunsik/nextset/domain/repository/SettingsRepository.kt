package io.github.zzunsik.nextset.domain.repository

import io.github.zzunsik.nextset.domain.model.Settings
import io.github.zzunsik.nextset.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** 저장한 값이 없으면 기본값을 내보낸다. */
    fun observeSettings(): Flow<Settings>

    /** SET-01. 30초부터 5분까지 15초 단위가 아니면 [IllegalArgumentException]. */
    suspend fun setDefaultRestSeconds(seconds: Int)

    suspend fun setThemeMode(mode: ThemeMode)

    /** 맨 위에 띄울 루틴을 고른다(HOME-03). null이면 다음 차례로 돌아간다. */
    suspend fun setPinnedRoutine(routineId: Long?)

    suspend fun setNotificationPromptDeclined(declined: Boolean)
}
