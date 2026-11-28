package io.github.zzunsik.nextset.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import kotlin.math.floor
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * docs/DESIGN.md의 토큰 표와 테마 코드의 값이 같은지 본다.
 * 같은 값이 문서와 코드 두 곳에 있으므로 한쪽만 바뀌면 실패한다(AGENTS.md "작업 범위").
 * 대비 표도 테마의 색으로 다시 계산해 문서 값과 기준을 확인한다(DESIGN.md "대비").
 */
class DesignTokenSyncTest {
    private val design: List<String> by lazy {
        val file = File("../docs/DESIGN.md")
        assertTrue("디자인 문서를 찾지 못했다: ${file.absolutePath}", file.isFile)
        file.readLines()
    }

    private fun NextSetColors.byToken(): Map<String, Color> = mapOf(
        "bg" to bg, "surface" to surface, "text" to text, "text2" to text2, "line" to line,
        "accent" to accent, "onAccent" to onAccent, "accentSoft" to accentSoft,
        "surface2" to surface2, "text3" to text3, "accentDim" to accentDim,
        "accentText" to accentText, "go" to go, "onGo" to onGo, "goMark" to goMark,
        "record" to record, "recordText" to recordText, "ink" to ink, "error" to error,
    )

    private fun Color.hex() = "#%06X".format(toArgb() and 0xFFFFFF)

    @Test
    fun colorsMatchDesignTable() {
        val row = Regex("""^\| `(\w+)` \| [^|]+ \| `(#[0-9A-F]{6})` \| `(#[0-9A-F]{6})` \|""")
        val documented = design.mapNotNull { row.find(it) }.associate {
            it.groupValues[1] to (it.groupValues[2] to it.groupValues[3])
        }
        val light = LightColors.byToken()
        val dark = DarkColors.byToken()
        assertEquals("색 토큰 이름", documented.keys, light.keys)
        documented.forEach { (token, values) ->
            assertEquals("$token 밝은 화면", values.first, light.getValue(token).hex())
            assertEquals("$token 어두운 화면", values.second, dark.getValue(token).hex())
        }
    }

    @Test
    fun contrastMatchesDesignTableAndMeetsThreshold() {
        val row = Regex("""^\| `(\w+)` / `(\w+)` \| [^|]+ \| ([\d.]+) \| ([\d.]+) \| ([\d.]+) \|""")
        val rows = design.mapNotNull { row.find(it)?.groupValues }
        assertTrue("대비 표를 찾지 못했다", rows.size >= 19)
        val light = LightColors.byToken()
        val dark = DarkColors.byToken()
        for (r in rows) {
            val (fg, bg, threshold) = Triple(r[1], r[2], r[3].toDouble())
            for ((name, colors, documented) in listOf(
                Triple("밝은", light, r[4].toDouble()),
                Triple("어두운", dark, r[5].toDouble()),
            )) {
                val ratio = floor(contrast(colors.getValue(fg), colors.getValue(bg)) * 100) / 100
                assertEquals("$fg / $bg $name 화면 대비가 문서와 다르다", documented, ratio, 0.0)
                assertTrue("$fg / $bg $name 화면 대비 $ratio < $threshold", ratio >= threshold)
            }
        }
    }

    @Test
    fun typographyMatchesDesignTable() {
        val row = Regex("""^\| `(\w+)` \| (\d+)sp / (\d+)sp \| \w+\((\d{3})\) \|""")
        val documented = design.mapNotNull { row.find(it)?.groupValues }
        val styles = nextSetTypography().all
        assertEquals("타입 토큰 이름", documented.map { it[1] }.toSet(), styles.keys)
        for (r in documented) {
            val style = styles.getValue(r[1])
            assertEquals("${r[1]} 크기", r[2].toFloat(), style.fontSize.value)
            assertEquals("${r[1]} 줄 높이", r[3].toFloat(), style.lineHeight.value)
            assertEquals("${r[1]} 굵기", r[4].toInt(), style.fontWeight?.weight)
        }
    }

    @Test
    fun everyTextStyleHasTabularNumbersAndNoLetterSpacing() {
        nextSetTypography().all.forEach { (name, style) ->
            assertEquals("$name 폭이 같은 숫자", "tnum", style.fontFeatureSettings)
            assertEquals("$name 자간", 0f, style.letterSpacing.value)
            assertEquals("$name 글꼴", Pretendard, style.fontFamily)
        }
    }

    @Test
    fun spacingAndRadiusMatchDesignTable() {
        val space = Regex("""^\| `space\.(\w+)` \| (\d+)dp \|""")
        val documented = design.mapNotNull { space.find(it)?.groupValues }.associate { it[1] to it[2].toFloat() }
        val s = NextSetSpacing()
        val code = mapOf("xs" to s.xs, "sm" to s.sm, "md" to s.md, "lg" to s.lg, "xl" to s.xl, "xxl" to s.xxl)
            .mapValues { it.value.value }
        assertEquals("간격 토큰", documented, code)

        val radius = Regex("""^\| `radius\.(\w+)` \| (\d+)dp \|""")
        val documentedRadius = design.mapNotNull { radius.find(it)?.groupValues }
            .associate { it[1] to it[2].toFloat() }
        val r = NextSetRadius()
        assertEquals("모서리 토큰", documentedRadius, mapOf("row" to r.rowSize.value, "card" to r.cardSize.value, "hero" to r.heroSize.value))
    }

    /** WCAG 2.2 상대 휘도 대비. DESIGN.md "대비"와 같은 식이다. */
    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun luminance(color: Color): Double {
        val argb = color.toArgb()
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
