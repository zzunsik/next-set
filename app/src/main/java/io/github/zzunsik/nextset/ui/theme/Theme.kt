package io.github.zzunsik.nextset.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 화면 모드(SET-02). 값은 바깥(설정)에서 받는다. 기본은 시스템 설정을 따른다.
 * 고르는 화면과 저장은 `ui/settings`에 있다. 저장하는 값은 `domain.model.ThemeMode`이고 `toUiThemeMode()`로 옮긴다.
 */
enum class ThemeMode {
    System,
    Light,
    Dark,
}

/** 이 모드에서 어두운 화면을 쓰는지. [ThemeMode.System]은 기기의 설정을 따른다. */
@Composable
@ReadOnlyComposable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

// 토큰은 바뀌지 않으므로 한 번만 만든다.
private val AppTypography = nextSetTypography()
private val AppMaterialTypography = AppTypography.toMaterialTypography()
private val AppRadius = NextSetRadius()
private val AppMaterialShapes = AppRadius.toMaterialShapes()
private val LightColorScheme = LightColors.toColorScheme(dark = false)
private val DarkColorScheme = DarkColors.toColorScheme(dark = true)

private val LocalNextSetColors = staticCompositionLocalOf { LightColors }
private val LocalNextSetTypography = staticCompositionLocalOf { AppTypography }
private val LocalNextSetSpacing = staticCompositionLocalOf { NextSetSpacing() }
private val LocalNextSetRadius = staticCompositionLocalOf { AppRadius }
private val LocalNextSetSizes = staticCompositionLocalOf { NextSetSizes() }

/**
 * 앱의 테마. docs/DESIGN.md의 토큰을 M3 테마와 앱 확장 토큰([NextSetTheme.colors] 등)으로 함께 넣는다.
 * 다이내믹 컬러는 쓰지 않는다(NF-05). 색은 [LightColors], [DarkColors]로 고정이다.
 */
@Composable
fun NextSetTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = themeMode.isDark()
    CompositionLocalProvider(LocalNextSetColors provides if (dark) DarkColors else LightColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkColorScheme else LightColorScheme,
            typography = AppMaterialTypography,
            shapes = AppMaterialShapes,
            content = content,
        )
    }
}

/** 화면 코드가 토큰을 꺼내는 곳. M3 역할에 없는 토큰(`done` 등)도 여기서 꺼낸다. */
object NextSetTheme {
    val colors: NextSetColors
        @Composable @ReadOnlyComposable get() = LocalNextSetColors.current
    val typography: NextSetTypography
        @Composable @ReadOnlyComposable get() = LocalNextSetTypography.current
    val spacing: NextSetSpacing
        @Composable @ReadOnlyComposable get() = LocalNextSetSpacing.current
    val radius: NextSetRadius
        @Composable @ReadOnlyComposable get() = LocalNextSetRadius.current
    val sizes: NextSetSizes
        @Composable @ReadOnlyComposable get() = LocalNextSetSizes.current
}
