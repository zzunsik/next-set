package io.github.zzunsik.nextset.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
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
class HorizontalVolumeBarScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun capture() {
        compose.setContent {
            NextSetTheme {
                Column(
                    Modifier.fillMaxWidth().background(NextSetTheme.colors.surface).padding(NextSetTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.lg),
                ) {
                    HorizontalVolumeBar("벤치 프레스", 2400.0, 2400.0, "2,400kg")
                    HorizontalVolumeBar("덤벨 프레스", 1200.0, 2400.0, "1,200kg")
                    HorizontalVolumeBar("기록 없음", 0.0, 0.0, "0kg")
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
