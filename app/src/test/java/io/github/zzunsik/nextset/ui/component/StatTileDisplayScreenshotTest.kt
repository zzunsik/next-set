package io.github.zzunsik.nextset.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class StatTileDisplayScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun capture() {
        compose.setContent {
            NextSetTheme {
              Box(Modifier.background(NextSetTheme.colors.bg).padding(NextSetTheme.spacing.lg)) {
                StatTile(
                    label = "볼륨", value = "4,820", unit = "kg",
                    kind = StatTileKind.Progress, valueSize = StatTileValueSize.Display,
                    minHeight = 136.dp,
                )
              }
            }
        }
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
    }

    @Test fun light() = capture()
    @Test @Config(fontScale = LARGE_FONT_SCALE) fun lightLargeFont() = capture()
    @Test @Config(qualifiers = "+night") fun dark() = capture()
    @Test @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE) fun darkLargeFont() = capture()
}
