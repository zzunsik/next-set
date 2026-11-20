package io.github.zzunsik.nextset.ui.settings

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.zzunsik.nextset.domain.repository.BackupRepository
import io.github.zzunsik.nextset.ui.navigation.BackupKey

/** 내보낼 파일의 형식. */
const val BACKUP_MIME_TYPE = "application/json"

/**
 * 가져올 때 파일 선택 화면에 보일 형식. 문서 제공자에 따라 `.json` 파일을 `application/json`이 아닌
 * `application/octet-stream`이나 `text/plain`으로 알리는 곳이 있어 함께 받는다. 이 앱의 백업인지는 파일 내용의
 * `format` 값으로 가린다(docs/architecture.md "확인하지 못한 것"의 파일 선택기 형식 필터).
 */
val BACKUP_IMPORT_MIME_TYPES = arrayOf(BACKUP_MIME_TYPE, "application/octet-stream", "text/plain")

/**
 * 백업 화면을 [backStack]에 잇는다. 파일은 시스템 파일 선택기(Storage Access Framework)로 고른다.
 * 저장소 권한을 요청하지 않는다.
 */
fun EntryProviderScope<NavKey>.backupEntries(
    backStack: MutableList<NavKey>,
    backupRepository: BackupRepository,
) {
    entry<BackupKey> {
        val context = LocalContext.current
        val viewModel = viewModel {
            BackupViewModel(backupRepository, ContentResolverBackupDocuments(context.applicationContext.contentResolver))
        }
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)) {
            viewModel.onExportTo(it)
        }
        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            viewModel.onImportFrom(it)
        }
        BackupScreen(
            state = state,
            onBack = { backStack.removeLastOrNull() },
            onExport = {
                try {
                    exportLauncher.launch(viewModel.suggestedFileName())
                } catch (_: ActivityNotFoundException) {
                    viewModel.onPickerUnavailable()
                }
            },
            onImport = {
                try {
                    importLauncher.launch(BACKUP_IMPORT_MIME_TYPES)
                } catch (_: ActivityNotFoundException) {
                    viewModel.onPickerUnavailable()
                }
            },
            onConfirmImport = viewModel::onConfirmImport,
            onCancelImport = viewModel::onCancelImport,
            onMessageShown = viewModel::onMessageShown,
        )
    }
}
