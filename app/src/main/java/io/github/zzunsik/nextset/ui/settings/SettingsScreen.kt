package io.github.zzunsik.nextset.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.ThemeMode
import io.github.zzunsik.nextset.domain.rule.defaultRestChoices
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import io.github.zzunsik.nextset.ui.theme.ThemeMode as UiThemeMode

/** 설정 화면에서 열리는 대화상자. */
enum class SettingsDialog { DefaultRest, Theme }

/**
 * 설정 화면(docs/DESIGN.md "설정").
 *
 * 아직 만들지 않은 화면으로 가는 행([onManageExercises], [onBackup]이 null)은 "준비 중이에요."를 보이고 누르는 요소가 아니다.
 * 알림 행과 하단 내비게이션은 휴식 타이머(#26)와 홈(#23)이 생길 때 더한다.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState?,
    versionName: String,
    onDefaultRestChange: (Int) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenSourceCode: () -> Unit,
    modifier: Modifier = Modifier,
    onManageExercises: (() -> Unit)? = null,
    onBackup: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    initialDialog: SettingsDialog? = null,
) {
    var dialog by rememberSaveable { mutableStateOf(initialDialog) }
    val notReady = stringResource(R.string.settings_not_ready)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenTitleBar(title = stringResource(R.string.settings_title))
            SettingsRow(
                title = stringResource(R.string.settings_default_rest),
                caption = state?.let { restDurationText(it.defaultRestSeconds) },
                onClick = { dialog = SettingsDialog.DefaultRest },
            )
            RowDivider()
            SettingsRow(
                title = stringResource(R.string.settings_theme_mode),
                caption = state?.let { themeModeLabel(it.themeMode) },
                onClick = { dialog = SettingsDialog.Theme },
            )
            RowDivider()
            SettingsRow(
                title = stringResource(R.string.settings_manage_exercises),
                caption = if (onManageExercises == null) notReady else null,
                onClick = onManageExercises,
            )
            RowDivider()
            SettingsRow(
                title = stringResource(R.string.settings_backup),
                caption = if (onBackup == null) notReady else null,
                onClick = onBackup,
            )
            RowDivider()
            SettingsRow(title = stringResource(R.string.settings_licenses), onClick = onOpenLicenses)
            RowDivider()
            SettingsRow(
                title = stringResource(R.string.settings_source_code),
                caption = stringResource(R.string.settings_source_code_caption),
                onClick = onOpenSourceCode,
            )
            Text(
                text = stringResource(R.string.settings_version, versionName),
                style = NextSetTheme.typography.caption,
                color = NextSetTheme.colors.text2,
                modifier = Modifier.padding(horizontal = NextSetTheme.spacing.lg, vertical = NextSetTheme.spacing.xl),
            )
        }
    }

    when (dialog) {
        SettingsDialog.DefaultRest -> OptionDialog(
            title = stringResource(R.string.settings_default_rest),
            note = stringResource(R.string.settings_default_rest_note),
            options = defaultRestChoices,
            selected = state?.defaultRestSeconds,
            label = { restDurationText(it) },
            onSelect = onDefaultRestChange,
            onDismiss = { dialog = null },
        )
        SettingsDialog.Theme -> OptionDialog(
            title = stringResource(R.string.settings_theme_mode),
            options = ThemeMode.entries,
            selected = state?.themeMode,
            label = { themeModeLabel(it) },
            onSelect = onThemeModeChange,
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

/** 휴식 시간을 "2분", "45초", "1분 30초"로 적는다. */
@Composable
internal fun restDurationText(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60
    return when {
        minutes == 0 -> stringResource(R.string.rest_seconds, rest)
        rest == 0 -> stringResource(R.string.rest_minutes, minutes)
        else -> stringResource(R.string.rest_minutes_seconds, minutes, rest)
    }
}

@Composable
internal fun themeModeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_mode_system
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
    },
)

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    NextSetTheme(themeMode = UiThemeMode.Light) {
        SettingsScreen(
            state = SettingsUiState(defaultRestSeconds = 120, themeMode = ThemeMode.SYSTEM),
            versionName = "0.1.0",
            onDefaultRestChange = {},
            onThemeModeChange = {},
            onOpenLicenses = {},
            onOpenSourceCode = {},
        )
    }
}
