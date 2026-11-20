package io.github.zzunsik.nextset

import android.app.UiModeManager
import android.os.Looper
import io.github.zzunsik.nextset.domain.model.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * 저장한 화면 모드(SET-02)가 시스템 쪽 사본(`UiModeManager.setApplicationNightMode`, API 31+)에도 들어가는지(#54).
 * 시스템은 이 값으로 앱을 켤 때의 시작 화면을 그린다. 기준은 데이터베이스의 값이고, 둘이 어긋나면 이 테스트가 실패한다.
 * 테스트 SDK(`robolectric.properties`)가 31 이상이라 실제 앱 객체와 데이터베이스를 거쳐 본다.
 */
@RunWith(RobolectricTestRunner::class)
class SystemNightModeTest {
    private val app = RuntimeEnvironment.getApplication() as NextSetApplication
    private val uiModeManager = shadowOf(app.getSystemService(UiModeManager::class.java))

    private fun setMode(mode: ThemeMode) = runBlocking { app.container.settingsRepository.setThemeMode(mode) }

    private fun awaitApplicationNightMode(expected: Int) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (uiModeManager.applicationNightMode == expected) return
            Thread.sleep(10)
        }
        fail("시스템 쪽 화면 모드가 $expected 이 아니다: ${uiModeManager.applicationNightMode}")
    }

    @Test
    fun storedModeIsCopiedToSystemAndFollowsChanges() {
        setMode(ThemeMode.LIGHT)
        awaitApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
        setMode(ThemeMode.DARK)
        awaitApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
        // "시스템 설정 따름"으로 되돌리면 시작 화면도 시스템을 따른다.
        setMode(ThemeMode.SYSTEM)
        awaitApplicationNightMode(UiModeManager.MODE_NIGHT_AUTO)
    }

    @Test
    fun storedDarkIsCopiedToSystem() {
        setMode(ThemeMode.DARK)
        awaitApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
    }
}
