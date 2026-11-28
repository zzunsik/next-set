package io.github.zzunsik.nextset.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 화면 모드(SET-02)를 바깥에서 받아 적용하는지, 다이내믹 컬러 없이 고정 색을 쓰는지(NF-05) 본다. */
@RunWith(RobolectricTestRunner::class)
class ThemeModeTest {
    @get:Rule
    val compose = createComposeRule()

    private fun resolve(mode: ThemeMode): Pair<NextSetColors, Color> {
        var colors: NextSetColors? = null
        var primary = Color.Unspecified
        compose.setContent {
            NextSetTheme(themeMode = mode) {
                colors = NextSetTheme.colors
                primary = MaterialTheme.colorScheme.primary
            }
        }
        compose.waitForIdle()
        return colors!! to primary
    }

    @Test
    fun systemFollowsLightSystemSetting() {
        assertEquals(LightColors to LightColors.accentText, resolve(ThemeMode.System))
    }

    @Test
    @Config(qualifiers = "+night")
    fun systemFollowsDarkSystemSetting() {
        assertEquals(DarkColors to DarkColors.accentText, resolve(ThemeMode.System))
    }

    @Test
    @Config(qualifiers = "+night")
    fun lightOverridesDarkSystemSetting() {
        assertEquals(LightColors to LightColors.accentText, resolve(ThemeMode.Light))
    }

    @Test
    fun darkOverridesLightSystemSetting() {
        assertEquals(DarkColors to DarkColors.accentText, resolve(ThemeMode.Dark))
    }
}
