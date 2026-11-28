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
    val surface2: Color,
    val line: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val onAccent: Color,
    val accentDim: Color,
    val accentText: Color,
    val accentSoft: Color,
    val go: Color,
    val onGo: Color,
    val goMark: Color,
    val record: Color,
    val recordText: Color,
    val ink: Color,
    val error: Color,
)

val LightColors = NextSetColors(
    bg = Color(0xFFF1F1EC),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFE4E4DD),
    line = Color(0xFFD3D3CA),
    text = Color(0xFF0A0A0A),
    text2 = Color(0xFF5A5A5A),
    text3 = Color(0xFF65655F),
    accent = Color(0xFF5B3DF5),
    onAccent = Color(0xFFFFFFFF),
    accentDim = Color(0xFF422CB0),
    accentText = Color(0xFF4B2FE0),
    accentSoft = Color(0xFFE1DBFF),
    go = Color(0xFF0A0A0A),
    onGo = Color(0xFFFFFFFF),
    goMark = Color(0xFFD4FF3A),
    record = Color(0xFFFF7A45),
    recordText = Color(0xFFA8410F),
    ink = Color(0xFF0A0A0A),
    error = Color(0xFFB3261E),
)

val DarkColors = NextSetColors(
    bg = Color(0xFF0A0A0A),
    surface = Color(0xFF242424),
    surface2 = Color(0xFF343434),
    line = Color(0xFF454545),
    text = Color(0xFFFFFFFF),
    text2 = Color(0xFFA8A8A8),
    text3 = Color(0xFF969696),
    accent = Color(0xFF6A4DFF),
    onAccent = Color(0xFFFFFFFF),
    accentDim = Color(0xFF4C37B8),
    accentText = Color(0xFFA999FF),
    accentSoft = Color(0xFF322C50),
    go = Color(0xFFD4FF3A),
    onGo = Color(0xFF0A0A0A),
    goMark = Color(0xFF0A0A0A),
    record = Color(0xFFFF7A45),
    recordText = Color(0xFFFF7A45),
    ink = Color(0xFF0A0A0A),
    error = Color(0xFFFF8A80),
)

/**
 * docs/DESIGN.md "M3 색 역할과의 대응"대로 M3 역할을 채운다.
 *
 * 표에 없는 역할도 모두 채운다. 비워 두면 M3 기본 보라색이 나온다.
 * - 강조 계열(`primary`, `secondary`, `tertiary`)은 `accentText`로 둔다. 완료 색은 넣지 않는다.
 *   M3 컴포넌트가 완료와 상관없는 곳에 초록을 쓰지 않게 한다.
 * - 바탕 계열(`surfaceContainer*`, `surfaceVariant`, `surfaceBright`)은 `surface`,
 *   `surfaceDim`은 `bg`로 둔다.
 * - 오류 칸(`errorContainer`)은 바탕을 오류 색으로 채우지 않는다는 규칙에 따라 `surface` 위 `error` 글자로 둔다.
 * - `scrim`은 M3 기본(검정)을 쓴다.
 */
fun NextSetColors.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accentText,
        onPrimary = bg,
        primaryContainer = accentSoft,
        onPrimaryContainer = text,
        // 반전 바탕(알림 줄 등) 위의 글자 버튼. 반대 화면의 강조색이 반전 바탕과 대비가 맞는다.
        inversePrimary = (if (dark) LightColors else DarkColors).accentText,
        secondary = accentText,
        onSecondary = bg,
        secondaryContainer = accentSoft,
        onSecondaryContainer = text,
        tertiary = accentText,
        onTertiary = bg,
        tertiaryContainer = accentSoft,
        onTertiaryContainer = text,
        background = bg,
        onBackground = text,
        surface = bg,
        onSurface = text,
        surfaceVariant = surface,
        onSurfaceVariant = text2,
        surfaceTint = accentText,
        inverseSurface = text,
        inverseOnSurface = bg,
        error = error,
        onError = bg,
        errorContainer = surface,
        onErrorContainer = error,
        outline = text3,
        outlineVariant = line,
        surfaceBright = surface,
        surfaceDim = bg,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface2,
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
