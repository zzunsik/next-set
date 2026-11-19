package io.github.zzunsik.nextset.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.github.zzunsik.nextset.R

/**
 * Pretendard v1.3.9 정적 OTF 셋(docs/DESIGN.md "글꼴"). 쓰는 굵기는 400, 600, 700뿐이다.
 * 라이선스 전문은 `assets/licenses/pretendard.txt`에 있다.
 *
 * 글꼴 파일을 읽지 못하면 Compose가 예외 없이 시스템 기본 글꼴로 그린다(docs/DESIGN.md "대체 글꼴").
 * `FontTest`가 이 동작을 확인한다.
 */
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

/** docs/DESIGN.md "타이포"의 토큰. 화면 코드는 이 스타일만 꺼내 쓰고 새로 만들지 않는다. */
@Immutable
data class NextSetTypography(
    val numeralLarge: TextStyle,
    val numeralMedium: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val numeralRow: TextStyle,
    val heading: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
) {
    /** 모든 토큰. 스타일 공통 규칙(폭이 같은 숫자, 자간 0)을 테스트에서 훑을 때 쓴다. */
    val all: Map<String, TextStyle>
        get() = mapOf(
            "numeralLarge" to numeralLarge,
            "numeralMedium" to numeralMedium,
            "headline" to headline,
            "title" to title,
            "numeralRow" to numeralRow,
            "heading" to heading,
            "body" to body,
            "label" to label,
            "caption" to caption,
        )
}

/**
 * 모든 스타일에 폭이 같은 숫자(`tnum`)를 켜고 자간을 0으로 둔다.
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
    )

fun nextSetTypography(fontFamily: FontFamily = Pretendard) = NextSetTypography(
    numeralLarge = style(fontFamily, 40.sp, 48.sp, FontWeight.Bold),
    numeralMedium = style(fontFamily, 28.sp, 36.sp, FontWeight.Bold),
    headline = style(fontFamily, 28.sp, 36.sp, FontWeight.Bold),
    title = style(fontFamily, 22.sp, 28.sp, FontWeight.Bold),
    numeralRow = style(fontFamily, 20.sp, 24.sp, FontWeight.Bold),
    heading = style(fontFamily, 18.sp, 24.sp, FontWeight.SemiBold),
    body = style(fontFamily, 16.sp, 24.sp, FontWeight.Normal),
    label = style(fontFamily, 14.sp, 20.sp, FontWeight.SemiBold),
    caption = style(fontFamily, 13.sp, 18.sp, FontWeight.Normal),
)

/**
 * M3 컴포넌트가 쓰는 타입 역할을 이 앱의 토큰으로 채운다.
 * M3 기본 `Typography`에서 글꼴만 바꾸면 M3의 자간 토큰이 남으므로 15개 역할을 모두 덮어쓴다.
 * - display 계열: 이 앱은 쓰지 않는다. 가장 큰 `numeralLarge`로 둔다.
 * - headline 계열: `headline`, `title`.
 * - title 계열: 상단 바 제목(titleLarge)은 `title`, 목록·카드 제목은 `heading`, 작은 제목은 `label`.
 * - body 계열: `body`, 작은 본문은 `caption`.
 * - label 계열: 버튼과 칩(labelLarge)은 `label`, 작은 라벨은 `caption`.
 */
fun NextSetTypography.toMaterialTypography() = Typography(
    displayLarge = numeralLarge,
    displayMedium = numeralLarge,
    displaySmall = numeralLarge,
    headlineLarge = headline,
    headlineMedium = headline,
    headlineSmall = title,
    titleLarge = title,
    titleMedium = heading,
    titleSmall = label,
    bodyLarge = body,
    bodyMedium = body,
    bodySmall = caption,
    labelLarge = label,
    labelMedium = caption,
    labelSmall = caption,
)
