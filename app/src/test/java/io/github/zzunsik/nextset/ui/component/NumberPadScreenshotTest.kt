package io.github.zzunsik.nextset.ui.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.zzunsik.nextset.domain.model.RecordType
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
 * 숫자판을 칸 종류마다 연 화면을 찍고 접근성을 검사한다.
 * 칸 종류 셋 × (밝은, 어두운) × (글자 기본, 200%)다. 값은 지어낸 것이다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class NumberPadScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun capture(kind: NumberFieldKind) {
        val (recordType, rows) = when (kind) {
            // 무게 칸을 고른 상태. 다음 칸(횟수)이 있어 "다음" 키가 보인다.
            NumberFieldKind.WEIGHT -> RecordType.WEIGHT_REPS to listOf(listOf(60_000, 10), listOf(62_500, 8), listOf(null, null))
            // 칸이 하나라 "완료" 키가 보인다.
            NumberFieldKind.REPS -> RecordType.REPS to listOf(listOf(12), listOf(10), listOf(null))
            NumberFieldKind.DURATION -> RecordType.DURATION to listOf(listOf(60), listOf(45), listOf(null))
        }
        compose.setContent {
            NextSetTheme { NumberPadTestHost(recordType, rows, openAt = HostFieldId(row = 1, index = 0)) }
        }
        // 접근성 검사를 먼저 돌려 접근성 문제를 스크린샷 차이보다 먼저 알린다.
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun weight() = capture(NumberFieldKind.WEIGHT)

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun weightLargeFont() = capture(NumberFieldKind.WEIGHT)

    @Test
    @Config(qualifiers = "+night")
    fun weightDark() = capture(NumberFieldKind.WEIGHT)

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun weightDarkLargeFont() = capture(NumberFieldKind.WEIGHT)

    @Test
    fun reps() = capture(NumberFieldKind.REPS)

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun repsLargeFont() = capture(NumberFieldKind.REPS)

    @Test
    @Config(qualifiers = "+night")
    fun repsDark() = capture(NumberFieldKind.REPS)

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun repsDarkLargeFont() = capture(NumberFieldKind.REPS)

    @Test
    fun duration() = capture(NumberFieldKind.DURATION)

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun durationLargeFont() = capture(NumberFieldKind.DURATION)

    @Test
    @Config(qualifiers = "+night")
    fun durationDark() = capture(NumberFieldKind.DURATION)

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun durationDarkLargeFont() = capture(NumberFieldKind.DURATION)
}
