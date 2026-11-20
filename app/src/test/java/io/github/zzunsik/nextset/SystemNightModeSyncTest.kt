package io.github.zzunsik.nextset

import android.app.UiModeManager
import io.github.zzunsik.nextset.domain.model.Settings
import io.github.zzunsik.nextset.domain.model.ThemeMode
import io.github.zzunsik.nextset.ui.settings.FakeSettingsRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 데이터베이스의 화면 모드를 시스템 쪽 사본에 맞추는 규칙(#54). 실제 `UiModeManager`까지 가는 경로는
 * `SystemNightModeTest`가 본다.
 */
class SystemNightModeSyncTest {
    @Test
    fun copiesStoredValueAtStartEvenIfItIsTheDefault() = runTest(UnconfinedTestDispatcher()) {
        // 앱 데이터를 지워 데이터베이스가 기본값으로 돌아가도 시스템 쪽에는 옛 값이 남는다. 시작할 때 늘 덮어쓴다.
        val applied = mutableListOf<Int>()
        val job = SystemNightModeSync(FakeSettingsRepository(Settings()), applied::add).start(backgroundScope)

        assertEquals(listOf(UiModeManager.MODE_NIGHT_AUTO), applied)
        job.cancel()
    }

    @Test
    fun followsEveryChangeOfStoredMode() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeSettingsRepository(Settings(themeMode = ThemeMode.LIGHT))
        val applied = mutableListOf<Int>()
        SystemNightModeSync(repository, applied::add).start(backgroundScope)

        repository.setThemeMode(ThemeMode.DARK)
        // 화면 모드 말고 다른 설정이 바뀌면 다시 부르지 않는다.
        repository.setDefaultRestSeconds(90)
        repository.setThemeMode(ThemeMode.DARK)
        repository.setThemeMode(ThemeMode.SYSTEM)

        assertEquals(
            listOf(UiModeManager.MODE_NIGHT_NO, UiModeManager.MODE_NIGHT_YES, UiModeManager.MODE_NIGHT_AUTO),
            applied,
        )
    }

    @Test
    fun everyStoredModeMapsToItsOwnNightMode() {
        assertEquals(
            mapOf(
                ThemeMode.SYSTEM to UiModeManager.MODE_NIGHT_AUTO,
                ThemeMode.LIGHT to UiModeManager.MODE_NIGHT_NO,
                ThemeMode.DARK to UiModeManager.MODE_NIGHT_YES,
            ),
            ThemeMode.entries.associateWith { it.toApplicationNightMode() },
        )
    }
}
