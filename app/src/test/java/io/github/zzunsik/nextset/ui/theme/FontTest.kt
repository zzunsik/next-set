package io.github.zzunsik.nextset.ui.theme

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.zzunsik.nextset.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

/**
 * Pretendard가 실제로 쓰이는지, 폭이 같은 숫자가 켜지는지, 글꼴을 못 읽을 때 앱이 멈추지 않는지 본다
 * (docs/DESIGN.md "글꼴"). 글자 폭을 재려면 실제 글꼴 렌더링이 필요해서 NATIVE 그래픽 모드로 돈다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FontTest {
    private val context = RuntimeEnvironment.getApplication()
    private val measurer = TextMeasurer(
        defaultFontFamilyResolver = createFontFamilyResolver(context),
        defaultDensity = Density(1f),
        defaultLayoutDirection = LayoutDirection.Ltr,
    )

    private fun width(text: String, style: TextStyle) = measurer.measure(text, style).size.width

    @Test
    fun everyStyleHasTabularDigits() {
        nextSetTypography().all.forEach { (name, style) ->
            assertEquals("$name: \"111\"과 \"000\"의 폭", width("000", style), width("111", style))
        }
    }

    /** Pretendard의 기본 숫자는 폭이 제각각이다. tnum을 끄면 폭이 달라져야 Pretendard로 그린 것이다. */
    @Test
    fun pretendardIsUsed() {
        val proportional = nextSetTypography().body.copy(fontFeatureSettings = null)
        assertTrue(
            "tnum을 끈 Pretendard에서 \"111\"은 \"000\"보다 좁아야 한다",
            width("111", proportional) < width("000", proportional),
        )
    }

    /**
     * 글꼴이 아닌 리소스를 [Pretendard]와 같은 방식(`Font(리소스, 굵기)`)으로 읽게 해서 읽기 실패를 흉내 낸다.
     * 예외 없이 시스템 기본 글꼴로 그려야 한다. 그래서 따로 대체 처리를 넣지 않았다.
     */
    @Test
    fun unreadableBundledFontFallsBackToSystemFont() {
        val broken = FontFamily(Font(R.drawable.ic_check, FontWeight.Normal))
        val style = TextStyle(fontFamily = broken)
        val fallback = TextStyle(fontFamily = FontFamily.Default)
        assertEquals("시스템 기본 글꼴로 그려야 한다", width("다음 세트 60kg", fallback), width("다음 세트 60kg", style))
    }
}
