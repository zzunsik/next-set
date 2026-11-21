package io.github.zzunsik.nextset.ui.theme

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

/**
 * 한국어를 어절 단위로 줄바꿈하는지 본다(docs/DESIGN.md "줄바꿈"). 실제 줄 나눔을 재려고 NATIVE 그래픽 모드로 돈다.
 * 테스트 SDK(36)는 한국어 `Phrase`가 듣는 API 35 이상이다. Robolectric의 기본 언어는 영어(en-US)라서
 * 스타일에 한국어를 적지 않으면 `Phrase`가 듣지 않는 것도 여기서 확인한다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LineBreakTest {
    private val measurer = TextMeasurer(
        defaultFontFamilyResolver = createFontFamilyResolver(RuntimeEnvironment.getApplication()),
        defaultDensity = Density(1f),
        defaultLayoutDirection = LayoutDirection.Ltr,
    )

    private val body = nextSetTypography().body

    private fun layout(text: String, style: TextStyle, width: Int) =
        measurer.measure(text, style, constraints = Constraints(maxWidth = width))

    private fun TextLayoutResult.lines(text: String) =
        (0 until lineCount).map { text.substring(getLineStart(it), getLineEnd(it)) }

    /** 마지막 줄을 빼고 모든 줄이 띄어쓰기에서 끝나야 어절 중간에서 끊지 않은 것이다. */
    private fun assertBreaksOnlyAtSpaces(text: String, style: TextStyle, width: Int) {
        val lines = layout(text, style, width).lines(text)
        assertTrue("두 줄 이상이어야 줄바꿈을 볼 수 있다: $lines", lines.size > 1)
        lines.dropLast(1).forEach { assertTrue("어절 중간에서 끊었다: $lines", it.endsWith(" ")) }
    }

    @Test
    fun everyStyleBreaksKoreanByPhrase() {
        nextSetTypography().all.forEach { (name, style) ->
            assertEquals("$name 줄바꿈", KoreanPhraseLineBreak, style.lineBreak)
            assertEquals("$name 단어 경계", LineBreak.WordBreak.Phrase, style.lineBreak.wordBreak)
            assertEquals("$name 줄 고르기", LineBreak.Simple.strategy, style.lineBreak.strategy)
            assertEquals("$name 금칙 강도", LineBreak.Simple.strictness, style.lineBreak.strictness)
            assertEquals("$name 언어", LocaleList("ko-KR"), style.localeList)
        }
    }

    /** #71의 예: 백업 화면 설명이 "다/른", "수/있는"처럼 끊겼다. 여러 폭에서 띄어쓰기에서만 끊는지 본다. */
    @Test
    fun koreanSentenceBreaksBetweenWords() {
        val sentences = listOf(
            "기록은 이 기기에만 저장돼요. 파일로 내보내 두면 다른 기기로 옮길 수 있어요.",
            "백업 파일에는 운동 기록이 암호화되지 않은 채 들어가요. 믿을 수 있는 곳에 저장해 주세요.",
        )
        for (text in sentences) for (width in listOf(120, 150, 200, 260)) {
            assertBreaksOnlyAtSpaces(text, body, width)
        }
    }

    /** 언어를 적지 않으면(기기 언어가 영어면) `Phrase`가 듣지 않아 글자 단위로 끊는다. 그래서 스타일에 한국어를 적는다. */
    @Test
    fun phraseNeedsKoreanLocale() {
        val text = "기록은 이 기기에만 저장돼요. 파일로 내보내 두면 다른 기기로 옮길 수 있어요."
        val withoutLocale = body.copy(localeList = null)
        val lines = layout(text, withoutLocale, 120).lines(text)
        assertTrue("언어가 없으면 어절 중간에서 끊는다: $lines", lines.dropLast(1).any { !it.endsWith(" ") })
    }

    /** 한 줄보다 긴 어절, URL, 숫자와 단위는 넘치지 않고 줄 안에 들어가야 한다(라이선스 화면, 긴 종목 이름). */
    @Test
    fun longWordsAndUrlsDoNotOverflow() {
        val texts = listOf(
            "Copyright (c) 2021, Kil Hyung-jin (https://github.com/orioncactus/pretendard),",
            "http://www.apache.org/licenses/LICENSE-2.0",
            "얼터네이팅덤벨인클라인바이셉스컬",
            "벤치프레스 60kg×10회, 100.5kg 5세트",
        )
        for (text in texts) for (width in listOf(80, 120, 200)) {
            val result = layout(text, body, width)
            assertFalse("넘친다: $text ($width)", result.hasVisualOverflow)
            (0 until result.lineCount).forEach {
                assertTrue("줄이 폭보다 넓다: $text ($width)", result.getLineRight(it) <= width)
            }
            assertEquals("글자가 빠졌다: $text", text, result.lines(text).joinToString(""))
        }
    }
}
