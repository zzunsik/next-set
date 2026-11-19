package io.github.zzunsik.nextset

import android.graphics.drawable.ColorDrawable
import android.os.Looper
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.test.core.app.ActivityScenario
import io.github.zzunsik.nextset.domain.model.ThemeMode
import io.github.zzunsik.nextset.ui.theme.DarkColors
import io.github.zzunsik.nextset.ui.theme.LightColors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * 저장한 화면 모드(SET-02)가 시스템 바 아이콘 색과 창 바탕에도 적용되는지. 시스템 설정과 반대로 고른 경우를 본다.
 * 화면 안의 색은 `ThemeModeTest`가 본다.
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityThemeTest {
    private val app = RuntimeEnvironment.getApplication() as NextSetApplication

    private fun setMode(mode: ThemeMode) = runBlocking { app.container.settingsRepository.setThemeMode(mode) }

    /** 앱이 밝은 모드면 시스템 바 아이콘이 어둡고(appearance light) 창 바탕이 밝은 `bg`다. */
    private fun ActivityScenario<MainActivity>.awaitAppearance(light: Boolean) {
        val expectedBackground = (if (light) LightColors else DarkColors).bg.toArgb()
        val deadline = System.currentTimeMillis() + 5_000
        var last = ""
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            var matched = false
            onActivity { activity ->
                val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                val background = (activity.window.decorView.background as? ColorDrawable)?.color
                last = "statusLight=${controller.isAppearanceLightStatusBars} " +
                    "navLight=${controller.isAppearanceLightNavigationBars} background=${background?.let(Integer::toHexString)}"
                matched = controller.isAppearanceLightStatusBars == light &&
                    controller.isAppearanceLightNavigationBars == light &&
                    background == expectedBackground
            }
            if (matched) return
            Thread.sleep(10)
        }
        fail("시스템 바가 ${if (light) "밝은" else "어두운"} 모드로 바뀌지 않았다: $last")
    }

    @Test
    @Config(qualifiers = "+night")
    fun forcedLightOverridesDarkSystemAndFollowsChanges() {
        setMode(ThemeMode.LIGHT)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.awaitAppearance(light = true)
            // 설정에서 바꾸면 다시 열지 않아도 바로 바뀐다.
            setMode(ThemeMode.DARK)
            scenario.awaitAppearance(light = false)
            setMode(ThemeMode.SYSTEM)
            scenario.awaitAppearance(light = false)
        }
    }

    @Test
    fun forcedDarkOverridesLightSystem() {
        setMode(ThemeMode.DARK)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.awaitAppearance(light = false)
            setMode(ThemeMode.SYSTEM)
            scenario.awaitAppearance(light = true)
        }
    }
}
