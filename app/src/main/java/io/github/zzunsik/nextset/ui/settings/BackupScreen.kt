package io.github.zzunsik.nextset.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.BackupSummary
import io.github.zzunsik.nextset.ui.component.PrimaryButton
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import java.time.LocalDate
import kotlinx.coroutines.launch
import io.github.zzunsik.nextset.ui.theme.ThemeMode as UiThemeMode

/**
 * 백업 화면(docs/DESIGN.md "백업"). 설명, "백업 파일 내보내기"와 마지막으로 내보낸 날, "백업 파일에서 가져오기".
 * 가져오기 확인은 대화상자, 오류는 버튼 아래의 글, 마친 일은 알림 줄로 알린다.
 */
@Composable
fun BackupScreen(
    state: BackupUiState,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val spacing = NextSetTheme.spacing
    val colors = NextSetTheme.colors
    val typography = NextSetTheme.typography
    val message = state.message?.let { backupMessageText(it) }
    // 알림 줄은 상태와 따로 떠 있어야 한다. 문구를 비우면 이 효과가 다시 시작되므로 화면의 범위에서 띄운다.
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.message) {
        if (message != null) {
            scope.launch { snackbarHostState.showSnackbar(message) }
            onMessageShown()
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenTitleBar(title = stringResource(R.string.backup_title), onBack = onBack)
            Column(
                modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.xl),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(text = stringResource(R.string.backup_description), style = typography.body, color = colors.text)
                    Text(
                        text = stringResource(R.string.backup_unencrypted_notice),
                        style = typography.caption,
                        color = colors.text2,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    PrimaryButton(
                        text = stringResource(R.string.backup_export),
                        onClick = onExport,
                        enabled = !state.busy,
                    )
                    state.lastExported?.let { exported ->
                        Text(
                            text = stringResource(R.string.backup_last_exported, dateText(exported.date, exported.showYear)),
                            style = typography.caption,
                            color = colors.text2,
                        )
                    }
                }
                OutlinedButton(
                    onClick = onImport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = NextSetTheme.sizes.buttonHeight),
                    enabled = !state.busy,
                    shape = NextSetTheme.radius.md,
                    // 글자는 누르는 요소의 색(`accent`)이다. 테두리는 M3 기본(`line`)으로 장식이다.
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                ) {
                    Text(
                        text = stringResource(R.string.backup_import),
                        style = typography.label,
                        textAlign = TextAlign.Center,
                    )
                }
                state.error?.let { error ->
                    Text(
                        text = backupErrorText(error),
                        style = typography.body,
                        color = colors.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }
    }

    state.confirmation?.let { confirmation ->
        ImportConfirmDialog(confirmation = confirmation, onConfirm = onConfirmImport, onDismiss = onCancelImport)
    }
}

/** 가져오기 확인. 되돌릴 수 없는 일이라 대화상자로 묻고 확인 버튼에 하는 일("바꾸기")을 적는다. */
@Composable
private fun ImportConfirmDialog(
    confirmation: ImportConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = NextSetTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.backup_confirm_title), style = NextSetTheme.typography.title) },
        text = { ImportConfirmText(confirmation, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { ImportReplaceButton(onClick = onConfirm) },
        dismissButton = { ImportCancelButton(onClick = onDismiss) },
        containerColor = colors.surface,
        titleContentColor = colors.text,
        textContentColor = colors.text,
    )
}

/** 확인 대화상자의 본문. 파일과 기기의 기록 양, 진행 중인 운동이 지워진다는 안내, 물음. */
@Composable
internal fun ImportConfirmText(confirmation: ImportConfirmation, modifier: Modifier = Modifier) {
    val typography = NextSetTheme.typography
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.sm)) {
        Text(text = summaryText(R.string.backup_confirm_file, confirmation.file), style = typography.body)
        Text(text = summaryText(R.string.backup_confirm_device, confirmation.device), style = typography.body)
        if (confirmation.device.hasWorkoutInProgress) {
            Text(text = stringResource(R.string.backup_confirm_in_progress), style = typography.body)
        }
        Text(text = stringResource(R.string.backup_confirm_question), style = typography.body)
    }
}

/** "바꾸기". 지우는 일이라 `error` 색 글자 버튼이다. */
@Composable
internal fun ImportReplaceButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight),
        colors = ButtonDefaults.textButtonColors(contentColor = NextSetTheme.colors.error),
    ) {
        Text(text = stringResource(R.string.backup_confirm_replace), style = NextSetTheme.typography.label)
    }
}

@Composable
internal fun ImportCancelButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight)) {
        Text(text = stringResource(R.string.action_cancel), style = NextSetTheme.typography.label)
    }
}

@Composable
private fun summaryText(format: Int, summary: BackupSummary): String =
    stringResource(format, summary.workoutCount, summary.routineCount)

/** "11월 14일 (토)". [showYear]면 앞에 연도를 붙인다(docs/DESIGN.md "숫자와 단위"). */
@Composable
internal fun dateText(date: LocalDate, showYear: Boolean): String {
    val weekday = stringArrayResource(R.array.weekdays_short)[date.dayOfWeek.value - 1]
    return if (showYear) {
        stringResource(R.string.date_year_month_day, date.year, date.monthValue, date.dayOfMonth, weekday)
    } else {
        stringResource(R.string.date_month_day, date.monthValue, date.dayOfMonth, weekday)
    }
}

@Composable
private fun backupMessageText(message: BackupMessage): String = stringResource(
    when (message) {
        BackupMessage.EXPORTED -> R.string.backup_exported
        BackupMessage.IMPORTED -> R.string.backup_imported
    },
)

@Composable
internal fun backupErrorText(error: BackupError): String = stringResource(
    when (error) {
        BackupError.NOT_BACKUP -> R.string.backup_error_not_backup
        BackupError.NEWER_VERSION -> R.string.backup_error_newer_version
        BackupError.BROKEN -> R.string.backup_error_broken
        BackupError.READ_FAILED -> R.string.backup_error_read_failed
        BackupError.WRITE_FAILED -> R.string.backup_error_write_failed
        BackupError.IMPORT_FAILED -> R.string.backup_error_import_failed
        BackupError.PICKER_UNAVAILABLE -> R.string.backup_error_picker_unavailable
    },
)

@Preview(showBackground = true)
@Composable
private fun BackupScreenPreview() {
    NextSetTheme(themeMode = UiThemeMode.Light) {
        BackupScreen(
            state = BackupUiState(lastExported = ExportDate(LocalDate.of(2026, 11, 10), showYear = false)),
            onBack = {},
            onExport = {},
            onImport = {},
            onConfirmImport = {},
            onCancelImport = {},
            onMessageShown = {},
        )
    }
}
