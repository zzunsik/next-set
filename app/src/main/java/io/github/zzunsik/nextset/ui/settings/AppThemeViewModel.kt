package io.github.zzunsik.nextset.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import io.github.zzunsik.nextset.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import io.github.zzunsik.nextset.domain.model.ThemeMode as StoredThemeMode

/**
 * 앱 전체에 적용할 화면 모드(SET-02). `MainActivity`가 이 값으로 테마와 시스템 바를 정한다.
 * 설정에서 고르면 저장소의 Flow를 거쳐 바로 바뀐다. 앱을 다시 열면 저장한 값으로 시작한다.
 */
class AppThemeViewModel(settingsRepository: SettingsRepository) : ViewModel() {
    /** 저장소에서 값을 읽기 전에는 null이다. 그동안 화면을 그리지 않아 다른 모드가 잠깐 보이지 않게 한다. */
    val themeMode: StateFlow<ThemeMode?> = settingsRepository.observeSettings()
        .map { it.themeMode.toUiThemeMode() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

/** 저장하는 값(domain)을 테마가 받는 값으로 옮긴다. */
fun StoredThemeMode.toUiThemeMode(): ThemeMode = when (this) {
    StoredThemeMode.SYSTEM -> ThemeMode.System
    StoredThemeMode.LIGHT -> ThemeMode.Light
    StoredThemeMode.DARK -> ThemeMode.Dark
}
