package io.github.zzunsik.nextset

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zzunsik.nextset.ui.navigation.NextSetNavDisplay
import io.github.zzunsik.nextset.ui.settings.AppThemeViewModel
import io.github.zzunsik.nextset.ui.theme.DarkColors
import io.github.zzunsik.nextset.ui.theme.LightColors
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import io.github.zzunsik.nextset.ui.theme.isDark

class MainActivity : ComponentActivity() {
    private val container: AppContainer
        get() = (application as NextSetApplication).container

    private val appThemeViewModel: AppThemeViewModel by viewModels {
        viewModelFactory { initializer { AppThemeViewModel(container.settingsRepository) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by appThemeViewModel.themeMode.collectAsStateWithLifecycle()
            // 저장한 화면 모드를 읽기 전에는 그리지 않는다. 그동안은 창 바탕(시스템 설정을 따르는 XML 테마)이 보인다.
            val mode = themeMode ?: return@setContent
            ApplySystemBars(dark = mode.isDark())
            NextSetTheme(themeMode = mode) {
                NextSetNavDisplay(
                    settingsRepository = container.settingsRepository,
                    licenseRepository = container.licenseRepository,
                    exerciseRepository = container.exerciseRepository,
                    routineRepository = container.routineRepository,
                    backupRepository = container.backupRepository,
                )
            }
        }
    }

    /**
     * 시스템 바 아이콘 색과 창 바탕을 앱의 화면 모드에 맞춘다(SET-02, NF-04).
     *
     * `enableEdgeToEdge()`의 기본값은 시스템 설정을 보고 아이콘 색을 정하므로, 시스템이 어두운데 앱을 밝게 고르면
     * 밝은 바탕 위에 밝은 아이콘이 그려진다. 여기서 앱의 모드로 다시 부른다.
     * 내비게이션 바의 바탕은 Android 8~9의 버튼 내비게이션에서만 보이고 `bg` 색으로 둔다. Android 10부터는 투명하고
     * 시스템이 대비를 맞춘다(`SystemBarStyle.auto`). 창 바탕도 `bg`로 바꿔 화면 전환 사이에 시스템 쪽 색이 비치지 않게 한다.
     */
    @Composable
    private fun ApplySystemBars(dark: Boolean) {
        DisposableEffect(dark) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                navigationBarStyle = SystemBarStyle.auto(LightColors.bg.toArgb(), DarkColors.bg.toArgb()) { dark },
            )
            window.setBackgroundDrawable((if (dark) DarkColors else LightColors).bg.toArgb().toDrawable())
            onDispose {}
        }
    }
}
