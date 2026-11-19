package io.github.zzunsik.nextset.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zzunsik.nextset.domain.model.Settings
import io.github.zzunsik.nextset.domain.model.ThemeMode
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 설정 화면에 보일 값. */
data class SettingsUiState(
    val defaultRestSeconds: Int,
    val themeMode: ThemeMode,
)

/** 설정 화면의 상태(SET-01, SET-02). 고른 값은 바로 저장하고, 저장한 값이 다시 상태로 돌아온다. */
class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    /** 저장소에서 값을 읽기 전에는 null이다. */
    val uiState: StateFlow<SettingsUiState?> = settingsRepository.observeSettings()
        .map { SettingsUiState(defaultRestSeconds = it.defaultRestSeconds, themeMode = it.themeMode) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** 고를 수 있는 값이 아니면 무시한다. 화면은 고를 수 있는 값만 보인다. */
    fun onDefaultRestChange(seconds: Int) {
        if (!Settings.isValidDefaultRest(seconds)) return
        viewModelScope.launch { settingsRepository.setDefaultRestSeconds(seconds) }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    private companion object {
        /** 화면 회전처럼 잠깐 구독이 끊길 때는 저장소 구독을 유지한다. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
