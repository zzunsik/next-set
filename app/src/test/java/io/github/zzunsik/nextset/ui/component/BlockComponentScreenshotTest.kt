package io.github.zzunsik.nextset.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

/** 기본 컴포넌트(주요 버튼, 완료 체크 버튼)를 한 장에 모아 찍고 접근성을 검사한다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class BlockComponentScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Composable
    private fun Sheet() {
        val spacing = NextSetTheme.spacing
        Column(
            modifier = Modifier
                .testTag(SHEET)
                .fillMaxWidth()
                .background(NextSetTheme.colors.bg)
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            // 테스트 전용 문구다. 앱 문구는 문자열 리소스에 둔다(NF-03).
            StatTile(label = "이번 주", value = "2", unit = "회", kind = StatTileKind.Progress)
            StatTile(label = "최고 기록", value = "65", unit = "kg", kind = StatTileKind.Record)
            GroupedCard {
                androidx.compose.material3.Text("묶음 카드", color = NextSetTheme.colors.text, modifier = Modifier.padding(NextSetTheme.spacing.lg))
            }
            VolumeBars(values = listOf(0.0, 120.0, 60.0, 180.0), description = "요일별 볼륨: 월요일 0, 화요일 120, 수요일 60, 목요일 180kg", modifier = Modifier.fillMaxWidth())
            VolumeBars(values = listOf(0.0, 0.0), description = "운동 기록 없음", modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                DoneCheckButton(done = false, onDoneChange = {})
                DoneCheckButton(done = true, onDoneChange = {})
            }
        }
    }

    private fun capture() {
        compose.setContent { NextSetTheme { Sheet() } }
        // 접근성 검사를 먼저 돌려 접근성 문제를 스크린샷 차이보다 먼저 알린다.
        compose.assertAccessible()
        compose.waitForIdle()
        compose.onNodeWithTag(SHEET).captureRoboImage()
    }

    @Test
    fun light() = capture()

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun lightLargeFont() = capture()

    @Test
    @Config(qualifiers = "+night")
    fun dark() = capture()

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun darkLargeFont() = capture()

    private companion object {
        const val SHEET = "block-components"
    }
}
