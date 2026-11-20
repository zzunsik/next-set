package io.github.zzunsik.nextset.ui.settings

import android.net.Uri
import io.github.zzunsik.nextset.domain.model.BackupSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 백업 화면의 상태(DATA-03, DATA-04). 파일과 저장소는 지어낸 가짜다. */
@RunWith(RobolectricTestRunner::class)
class BackupViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakeBackupRepository()
    private val documents = FakeBackupDocuments()
    private var now = Instant.parse("2026-11-18T00:30:00Z")
    private val seoul = ZoneId.of("Asia/Seoul")
    private val viewModel = BackupViewModel(repository, documents, clock = { now }, zone = { seoul })

    private val target: Uri = Uri.parse("content://test/backup.json")

    private fun kotlinx.coroutines.test.TestScope.collectState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    @Test
    fun fileNameHasLocalDateAndTime() {
        assertEquals("next-set-backup-20261118-093000.json", viewModel.suggestedFileName())
    }

    @Test
    fun exportWritesFileThenRemembersTheDate() = runTest {
        collectState()
        assertNull(viewModel.uiState.value.lastExported)

        viewModel.onExportTo(target)

        assertEquals("backup at ${now.toEpochMilli()}", documents.files[target])
        assertEquals(listOf(now), repository.exported)
        val state = viewModel.uiState.value
        assertEquals(ExportDate(LocalDate.of(2026, 11, 18), showYear = false), state.lastExported)
        assertEquals(BackupMessage.EXPORTED, state.message)
        assertEquals(false, state.busy)

        viewModel.onMessageShown()
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun lastExportedDateShowsYearWhenNotThisYear() = runTest {
        collectState()
        repository.lastExportedAt.value = Instant.parse("2025-12-31T16:00:00Z")
        // 서울 시각으로 2026년 1월 1일이라 올해다.
        assertEquals(ExportDate(LocalDate.of(2026, 1, 1), showYear = false), viewModel.uiState.value.lastExported)

        repository.lastExportedAt.value = Instant.parse("2025-12-31T14:00:00Z")
        assertEquals(ExportDate(LocalDate.of(2025, 12, 31), showYear = true), viewModel.uiState.value.lastExported)
    }

    @Test
    fun cancelledPickersDoNothing() = runTest {
        collectState()
        viewModel.onExportTo(null)
        viewModel.onImportFrom(null)
        assertEquals(BackupUiState(), viewModel.uiState.value)
        assertTrue(repository.exported.isEmpty())
    }

    @Test
    fun failedWriteShowsErrorAndDoesNotRememberTheDate() = runTest {
        collectState()
        documents.failing += target

        viewModel.onExportTo(target)

        assertEquals(BackupError.WRITE_FAILED, viewModel.uiState.value.error)
        assertTrue(repository.exported.isEmpty())
        assertNull(viewModel.uiState.value.lastExported)
    }

    @Test
    fun importAsksWithBothSummariesThenReplaces() = runTest {
        collectState()
        repository.device = BackupSummary(workoutCount = 3, routineCount = 1, hasWorkoutInProgress = true)
        documents.files[target] = FakeBackupRepository.VALID

        viewModel.onImportFrom(target)

        assertEquals(
            ImportConfirmation(file = FakeBackupRepository.FILE_SUMMARY, device = repository.device),
            viewModel.uiState.value.confirmation,
        )
        assertTrue("확인 전에는 바꾸지 않는다", repository.replaced.isEmpty())

        viewModel.onConfirmImport()

        assertEquals(1, repository.replaced.size)
        assertNull(viewModel.uiState.value.confirmation)
        assertEquals(BackupMessage.IMPORTED, viewModel.uiState.value.message)
    }

    @Test
    fun cancellingTheConfirmationKeepsRecords() = runTest {
        collectState()
        documents.files[target] = FakeBackupRepository.VALID
        viewModel.onImportFrom(target)

        viewModel.onCancelImport()
        // 취소한 뒤 "바꾸기"가 늦게 와도 바꾸지 않는다.
        viewModel.onConfirmImport()

        assertNull(viewModel.uiState.value.confirmation)
        assertTrue(repository.replaced.isEmpty())
    }

    @Test
    fun rejectedFilesShowWhatIsWrong() = runTest {
        collectState()
        val expected = mapOf(
            "not-backup" to BackupError.NOT_BACKUP,
            "newer" to BackupError.NEWER_VERSION,
            "broken" to BackupError.BROKEN,
        )
        expected.forEach { (text, error) ->
            documents.files[target] = text
            viewModel.onImportFrom(target)
            assertEquals(text, error, viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.confirmation)
        }
        assertTrue(repository.replaced.isEmpty())
    }

    @Test
    fun unreadableAndHugeFilesAreRejected() = runTest {
        collectState()
        documents.failing += target
        viewModel.onImportFrom(target)
        assertEquals(BackupError.READ_FAILED, viewModel.uiState.value.error)

        val huge = Uri.parse("content://test/video.mp4")
        documents.tooLarge += huge
        viewModel.onImportFrom(huge)
        assertEquals(BackupError.NOT_BACKUP, viewModel.uiState.value.error)
        assertTrue(repository.replaced.isEmpty())
    }

    @Test
    fun failedReplaceShowsError() = runTest {
        collectState()
        repository.failReplace = true
        documents.files[target] = FakeBackupRepository.VALID
        viewModel.onImportFrom(target)

        viewModel.onConfirmImport()

        assertEquals(BackupError.IMPORT_FAILED, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun startingAnotherActionClearsTheError() = runTest {
        collectState()
        viewModel.onPickerUnavailable()
        assertEquals(BackupError.PICKER_UNAVAILABLE, viewModel.uiState.value.error)

        viewModel.onExportTo(target)
        assertNull(viewModel.uiState.value.error)
    }
}
