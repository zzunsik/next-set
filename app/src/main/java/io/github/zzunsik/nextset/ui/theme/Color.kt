package io.github.zzunsik.nextset.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * docs/DESIGN.md "색 / 토큰"의 값. 이름도 문서의 토큰 이름을 따른다.
 * 값을 바꾸면 문서의 토큰 표와 대비 표를 함께 고친다. `ThemeContrastTest`가 대비 기준을 검사한다.
 */
@Immutable
data class NextSetColors(
    val bg: Color,
    val surface: Color,
    val text: Color,
    val text2: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val done: Color,
    val onDone: Color,
    val doneSoft: Color,
    val error: Color,
)

val LightColors = NextSetColors(
    bg = Color(0xFFF6F7F9),
    surface = Color(0xFFFFFFFF),
    text = Color(0xFF16181D),
    text2 = Color(0xFF5A5F6B),
    line = Color(0xFFE0E3EA),
    accent = Color(0xFF3F3DB8),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFE6E6F8),
    done = Color(0xFF1A7A48),
    onDone = Color(0xFFFFFFFF),
    doneSoft = Color(0xFFDCF0E4),
    error = Color(0xFFB3261E),
)

val DarkColors = NextSetColors(
    bg = Color(0xFF101116),
    surface = Color(0xFF191B22),
    text = Color(0xFFECEDF2),
    text2 = Color(0xFFA2A6B4),
    line = Color(0xFF2B2E39),
    accent = Color(0xFFA9A8FF),
    onAccent = Color(0xFF14124A),
    accentSoft = Color(0xFF25254A),
    done = Color(0xFF6FD39A),
    onDone = Color(0xFF06301B),
    doneSoft = Color(0xFF163726),
    error = Color(0xFFF2B8B5),
)

/**
 * docs/DESIGN.md "M3 색 역할과의 대응"대로 M3 역할을 채운다.
 *
 * 표에 없는 역할도 모두 채운다. 비워 두면 M3 기본 보라색이 나온다.
 * - 강조 계열(`secondary`, `tertiary`)은 `accent`로 둔다. `tertiary`에 완료 색을 넣지 않는다.
 *   M3 컴포넌트가 완료와 상관없는 곳에 초록을 쓰지 않게 한다.
 * - 바탕 계열(`surfaceContainer*`, `surfaceVariant`, `surfaceBright`)은 `surface`,
 *   `surfaceDim`은 `bg`로 둔다.
 * - 오류 칸(`errorContainer`)은 바탕을 오류 색으로 채우지 않는다는 규칙에 따라 `surface` 위 `error` 글자로 둔다.
 * - `scrim`은 M3 기본(검정)을 쓴다.
 */
fun NextSetColors.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = text,
        // 반전 바탕(알림 줄 등) 위의 글자 버튼. 반대 화면의 강조색이 반전 바탕과 대비가 맞는다.
        inversePrimary = (if (dark) LightColors else DarkColors).accent,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = accentSoft,
        onSecondaryContainer = text,
        tertiary = accent,
        onTertiary = onAccent,
        tertiaryContainer = accentSoft,
        onTertiaryContainer = text,
        background = bg,
        onBackground = text,
        surface = bg,
        onSurface = text,
        surfaceVariant = surface,
        onSurfaceVariant = text2,
        surfaceTint = accent,
        inverseSurface = text,
        inverseOnSurface = bg,
        error = error,
        onError = bg,
        errorContainer = surface,
        onErrorContainer = error,
        outline = text2,
        outlineVariant = line,
        surfaceBright = surface,
        surfaceDim = bg,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface,
        primaryFixed = accentSoft,
        primaryFixedDim = accentSoft,
        onPrimaryFixed = text,
        onPrimaryFixedVariant = text,
        secondaryFixed = accentSoft,
        secondaryFixedDim = accentSoft,
        onSecondaryFixed = text,
        onSecondaryFixedVariant = text,
        tertiaryFixed = accentSoft,
        tertiaryFixedDim = accentSoft,
        onTertiaryFixed = text,
        onTertiaryFixedVariant = text,
    )
}
