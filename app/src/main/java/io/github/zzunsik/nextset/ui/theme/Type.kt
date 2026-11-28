package io.github.zzunsik.nextset.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.github.zzunsik.nextset.R

/**
 * Pretendard v1.3.9 정적 OTF 넷(docs/DESIGN.md "글꼴"). 쓰는 굵기는 400, 600, 700, 800이다.
 * 라이선스 전문은 `assets/licenses/pretendard.txt`에 있다.
 *
 * 글꼴 파일을 읽지 못하면 Compose가 예외 없이 시스템 기본 글꼴로 그린다(docs/DESIGN.md "대체 글꼴").
 * `FontTest`가 이 동작을 확인한다.
 */
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
)

/** docs/DESIGN.md "타이포"의 토큰. 화면 코드는 이 스타일만 꺼내 쓰고 새로 만들지 않는다. */
@Immutable
data class NextSetTypography(
    val numeralHero: TextStyle,
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val numeralRow: TextStyle,
    val heading: TextStyle,
    val bodyStrong: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
) {
    /** 모든 토큰. 스타일 공통 규칙(폭이 같은 숫자, 자간 0)을 테스트에서 훑을 때 쓴다. */
    val all: Map<String, TextStyle>
        get() = mapOf(
            "numeralHero" to numeralHero,
            "display" to display,
            "headline" to headline,
            "title" to title,
            "numeralRow" to numeralRow,
            "heading" to heading,
            "bodyStrong" to bodyStrong,
            "body" to body,
            "label" to label,
            "caption" to caption,
        )
}

/**
 * 한국어를 어절(띄어쓰기) 단위로 줄바꿈한다(docs/DESIGN.md "줄바꿈"). 줄을 고르는 방식(`Strategy`)과
 * 금칙 강도(`Strictness`)는 Compose 기본값인 [LineBreak.Simple]과 같고 단어 경계만 `Phrase`로 바꾼다.
 * - 한국어 `Phrase`는 Android 15(API 35)부터 듣는다. API 33–34는 값을 받지만 일본어에만 쓰고,
 *   API 32 아래에서는 Compose가 값을 넘기지 않는다. 두 경우 모두 지금처럼 글자 단위로 끊고 넘치지 않는다.
 * - `Phrase`는 글자의 언어가 한국어(또는 일본어)일 때만 듣는다. 그래서 [KOREAN]을 함께 둔다.
 * - 한 어절이 한 줄보다 길면 그 어절 안에서 끊는다. 넘치지 않는다. 영어와 URL은 지금과 같이 끊는다.
 */
val KoreanPhraseLineBreak = LineBreak(
    strategy = LineBreak.Strategy.Simple,
    strictness = LineBreak.Strictness.Normal,
    wordBreak = LineBreak.WordBreak.Phrase,
)

/**
 * 글자의 언어. 앱 문구는 한국어뿐이다. 기기 언어가 영어여도 한국어 규칙으로 줄을 바꾸려고 스타일에 적는다.
 * 사용자가 영어로 적은 종목 이름도 띄어쓰기에서 끊기므로 달라지지 않는다.
 */
val KOREAN = LocaleList("ko-KR")

/**
 * 모든 스타일에 폭이 같은 숫자(`tnum`)를 켜고 자간을 0으로 둔다. 한국어는 어절 단위로 줄을 바꾼다.
 * 크기는 sp라서 기기의 글자 크기 설정을 따른다(NF-10).
 */
private fun style(fontFamily: FontFamily, size: TextUnit, lineHeight: TextUnit, weight: FontWeight) =
    TextStyle(
        fontFamily = fontFamily,
        fontWeight = weight,
        fontSize = size,
        lineHeight = lineHeight,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
        localeList = KOREAN,
        lineBreak = KoreanPhraseLineBreak,
    )

fun nextSetTypography(fontFamily: FontFamily = Pretendard) = NextSetTypography(
    numeralHero = style(fontFamily, 56.sp, 68.sp, FontWeight.ExtraBold),
    display = style(fontFamily, 44.sp, 54.sp, FontWeight.ExtraBold),
    headline = style(fontFamily, 36.sp, 44.sp, FontWeight.ExtraBold),
    title = style(fontFamily, 26.sp, 32.sp, FontWeight.ExtraBold),
    numeralRow = style(fontFamily, 20.sp, 24.sp, FontWeight.ExtraBold),
    heading = style(fontFamily, 17.sp, 24.sp, FontWeight.ExtraBold),
    bodyStrong = style(fontFamily, 15.sp, 22.sp, FontWeight.Bold),
    body = style(fontFamily, 15.sp, 22.sp, FontWeight.Normal),
    label = style(fontFamily, 13.sp, 18.sp, FontWeight.ExtraBold),
    caption = style(fontFamily, 13.sp, 18.sp, FontWeight.SemiBold),
)

/**
 * M3 컴포넌트가 쓰는 타입 역할을 이 앱의 토큰으로 채운다.
 * M3 기본 `Typography`에서 글꼴만 바꾸면 M3의 자간 토큰이 남으므로 15개 역할을 모두 덮어쓴다.
 * - display 계열: 이 앱은 쓰지 않는다. 가장 큰 `numeralHero`로 둔다.
 * - headline 계열: `headline`, `title`.
 * - title 계열: 상단 바 제목(titleLarge)은 `title`, 목록·카드 제목은 `heading`, 작은 제목은 `label`.
 * - body 계열: `body`, 작은 본문은 `caption`.
 * - label 계열: 버튼과 칩(labelLarge)은 `label`, 작은 라벨은 `caption`.
 */
fun NextSetTypography.toMaterialTypography() = Typography(
    displayLarge = numeralHero,
    displayMedium = display,
    displaySmall = display,
    headlineLarge = headline,
    headlineMedium = headline,
    headlineSmall = title,
    titleLarge = title,
    titleMedium = heading,
    titleSmall = label,
    bodyLarge = body,
    bodyMedium = body,
    bodySmall = caption,
    labelLarge = bodyStrong,
    labelMedium = caption,
    labelSmall = caption,
)
