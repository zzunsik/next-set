package io.github.zzunsik.nextset.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.github.zzunsik.nextset.domain.model.ThemeMode
import io.github.zzunsik.nextset.domain.rule.defaultRestChoices
import io.github.zzunsik.nextset.ui.a11y.assertAccessible
import io.github.zzunsik.nextset.ui.screenshot.LARGE_FONT_SCALE
import io.github.zzunsik.nextset.ui.screenshot.SCREENSHOT_DEVICE
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 설정의 대화상자 안 고르기 목록의 접근성 검사. 대화상자는 창이 따로 떠서 `assertAccessible()`(화면 하나를 본다)이
 * 닿지 않으므로 대화상자 안에 넣는 목록을 그대로 그려 검사한다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class OptionListAccessibilityTest {
    @get:Rule
    val compose = createComposeRule()

    private fun check() {
        compose.setContent {
            NextSetTheme {
                // 대화상자의 바탕은 M3 surfaceContainerHigh, 이 앱에서는 `surface`다.
                Surface(color = NextSetTheme.colors.surface) {
                    Column {
                        OptionList(
                            options = ThemeMode.entries,
                            selected = ThemeMode.SYSTEM,
                            label = { themeModeLabel(it) },
                            onSelect = {},
                        )
                        OptionList(
                            options = defaultRestChoices.take(4),
                            selected = 45,
                            label = { restDurationText(it) },
                            onSelect = {},
                        )
                    }
                }
            }
        }
        compose.assertAccessible()
    }

    @Test
    fun light() = check()

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun darkLargeFont() = check()
}
