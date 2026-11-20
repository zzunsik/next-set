package io.github.zzunsik.nextset.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zzunsik.nextset.domain.model.BackupProblem
import io.github.zzunsik.nextset.domain.model.BackupSummary
import io.github.zzunsik.nextset.domain.model.CheckedBackup
import io.github.zzunsik.nextset.domain.model.InvalidBackupException
import io.github.zzunsik.nextset.domain.repository.BackupRepository
import io.github.zzunsik.nextset.domain.rule.backupFileName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 백업 화면에 보이는 오류. 무엇이 문제인지에 따라 문구가 다르다. 기존 기록은 어느 경우에도 그대로다. */
enum class BackupError {
    NOT_BACKUP,
    NEWER_VERSION,
    BROKEN,
    READ_FAILED,
    WRITE_FAILED,
    IMPORT_FAILED,
    PICKER_UNAVAILABLE,
}

/** 일을 마친 뒤 알림 줄로 한 번 보이는 문구. */
enum class BackupMessage { EXPORTED, IMPORTED }

/** 가져오기 확인에 보일 파일과 기기의 기록 양(docs/architecture.md "가져오기" 3번). */
data class ImportConfirmation(val file: BackupSummary, val device: BackupSummary)

/** 마지막으로 내보낸 날. 올해가 아니면 연도를 붙인다. */
data class ExportDate(val date: LocalDate, val showYear: Boolean)

data class BackupUiState(
    /** 내보낸 적이 없으면 null이다. 날짜 줄을 보이지 않는다. */
    val lastExported: ExportDate? = null,
    /** 파일을 읽고 쓰는 동안 참이다. 버튼을 막는다. */
    val busy: Boolean = false,
    val confirmation: ImportConfirmation? = null,
    val error: BackupError? = null,
    val message: BackupMessage? = null,
)

/**
 * 백업 화면의 상태(DATA-03, DATA-04, docs/DESIGN.md "백업").
 *
 * 내보내기: 파일 저장 화면이 고른 문서에 기록 전체를 쓰고, 다 쓴 뒤에 마지막으로 내보낸 시각을 남긴다.
 * 가져오기: 파일을 읽어 검사하고, 파일과 기기의 기록 양을 보여 확인을 받은 뒤 전체를 바꾼다.
 * 진행 중인 운동이 기기에 있으면 확인 문구에 함께 지워진다고 적는다(가져오기를 막지 않고 묻는다).
 */
class BackupViewModel(
    private val backupRepository: BackupRepository,
    private val documents: BackupDocuments,
    private val clock: () -> Instant = Instant::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) : ViewModel() {
    private val local = MutableStateFlow(BackupUiState())

    /** 확인을 기다리는 파일. 화면 상태에는 넣지 않는다. */
    private var pending: CheckedBackup? = null

    val uiState: StateFlow<BackupUiState> = local.asStateFlow()

    init {
        viewModelScope.launch {
            backupRepository.observeLastExportedAt().collect { at ->
                local.update { it.copy(lastExported = at?.let(::exportDate)) }
            }
        }
    }

    /** 파일 저장 화면에 처음 채울 이름. */
    fun suggestedFileName(): String = backupFileName(clock().atZone(zone()).toLocalDateTime())

    /** 파일 저장 화면에서 고른 문서. 고르지 않고 닫았으면 null이다. */
    fun onExportTo(uri: Uri?) {
        if (uri == null) return
        startWork {
            val now = clock()
            try {
                documents.write(uri, backupRepository.exportText(now))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                fail(BackupError.WRITE_FAILED)
                return@startWork
            }
            backupRepository.markExported(now)
            local.update { it.copy(busy = false, message = BackupMessage.EXPORTED) }
        }
    }

    /** 파일 선택 화면에서 고른 문서. 고르지 않고 닫았으면 null이다. */
    fun onImportFrom(uri: Uri?) {
        if (uri == null) return
        startWork {
            val confirmation = try {
                val backup = backupRepository.check(documents.read(uri))
                pending = backup
                ImportConfirmation(file = backup.summary, device = backupRepository.currentSummary())
            } catch (e: InvalidBackupException) {
                fail(e.problem.toError())
                return@startWork
            } catch (_: BackupDocuments.TooLargeException) {
                fail(BackupError.NOT_BACKUP)
                return@startWork
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                fail(BackupError.READ_FAILED)
                return@startWork
            }
            local.update { it.copy(busy = false, confirmation = confirmation) }
        }
    }

    /** 확인에서 "바꾸기". */
    fun onConfirmImport() {
        val backup = pending ?: return
        pending = null
        local.update { it.copy(confirmation = null) }
        startWork {
            try {
                backupRepository.replaceAll(backup)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                fail(BackupError.IMPORT_FAILED)
                return@startWork
            }
            local.update { it.copy(busy = false, message = BackupMessage.IMPORTED) }
        }
    }

    /** 확인에서 "취소". 기록을 건드리지 않는다. */
    fun onCancelImport() {
        pending = null
        local.update { it.copy(confirmation = null) }
    }

    /** 기기에 파일 선택 화면을 열 앱이 없다. */
    fun onPickerUnavailable() = fail(BackupError.PICKER_UNAVAILABLE)

    fun onMessageShown() = local.update { it.copy(message = null) }

    private fun startWork(work: suspend () -> Unit) {
        if (local.value.busy) return
        local.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch { work() }
    }

    private fun fail(error: BackupError) = local.update { it.copy(busy = false, error = error) }

    private fun exportDate(at: Instant): ExportDate {
        val zone = zone()
        val date = at.atZone(zone).toLocalDate()
        return ExportDate(date = date, showYear = date.year != clock().atZone(zone).year)
    }

    private fun BackupProblem.toError() = when (this) {
        BackupProblem.NOT_BACKUP -> BackupError.NOT_BACKUP
        BackupProblem.NEWER_VERSION -> BackupError.NEWER_VERSION
        BackupProblem.BROKEN -> BackupError.BROKEN
    }
}
