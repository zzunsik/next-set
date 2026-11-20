package io.github.zzunsik.nextset

import android.app.UiModeManager
import io.github.zzunsik.nextset.domain.model.ThemeMode
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 저장한 화면 모드(SET-02)를 시스템 쪽 사본에 맞춘다(#54).
 *
 * 시스템은 앱을 켤 때 앱 코드가 돌기 전에 시작 화면을 그리므로 데이터베이스의 값을 읽지 못한다.
 * Android 12(API 31)부터 `UiModeManager.setApplicationNightMode`로 알려 두면 시스템이 앱별로 기억했다가
 * 시작 화면과 앱의 `Configuration`에 적용한다.
 *
 * 기준 값은 데이터베이스의 `app_settings.theme_mode`이고 시스템 쪽은 사본이다. 사본은 읽는 공개 API가 없어
 * 비교하지 않고 덮어쓴다. 저장소의 Flow를 보고 있으므로 앱이 시작할 때(첫 값)와 값이 바뀔 때마다
 * 바뀐 경로(설정 화면, 백업 가져오기 등)와 상관없이 맞춰진다.
 */
internal class SystemNightModeSync(
    private val settingsRepository: SettingsRepository,
    private val setApplicationNightMode: (Int) -> Unit,
) {
    fun start(scope: CoroutineScope): Job = scope.launch {
        settingsRepository.observeSettings()
            .map { it.themeMode.toApplicationNightMode() }
            .distinctUntilChanged()
            .collect { setApplicationNightMode(it) }
    }
}

/** 저장하는 화면 모드를 `UiModeManager.setApplicationNightMode`가 받는 값으로 옮긴다. */
internal fun ThemeMode.toApplicationNightMode(): Int = when (this) {
    // 시스템이 앱별 값을 지우고 기기 설정을 따른다.
    ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
    ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
    ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
}
