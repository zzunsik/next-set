package io.github.zzunsik.nextset.ui.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.BackupSummary
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BackupScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private fun text(@StringRes id: Int, vararg args: Any) = app.getString(id, *args)

    private val calls = mutableListOf<String>()

    private fun show(state: BackupUiState) {
        compose.setContent {
            NextSetTheme {
                BackupScreen(
                    state = state,
                    onBack = { calls += "back" },
                    onExport = { calls += "export" },
                    onImport = { calls += "import" },
                    onConfirmImport = { calls += "confirm" },
                    onCancelImport = { calls += "cancel" },
                    onMessageShown = { calls += "shown" },
                )
            }
        }
    }

    @Test
    fun buttonsCallTheirActions() {
        show(BackupUiState())
        compose.onNodeWithText(text(R.string.backup_description)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.backup_unencrypted_notice)).assertIsDisplayed()

        compose.onNodeWithText(text(R.string.backup_export)).performClick()
        compose.onNodeWithText(text(R.string.backup_import)).performClick()
        compose.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()

        assertEquals(listOf("export", "import", "back"), calls)
    }

    @Test
    fun lastExportedDateIsShownOnlyAfterExporting() {
        show(BackupUiState())
        compose.onNode(hasText(text(R.string.backup_last_exported, ""), substring = true)).assertDoesNotExist()
    }

    @Test
    fun lastExportedDateIsWrittenWithWeekday() {
        show(BackupUiState(lastExported = ExportDate(LocalDate.of(2026, 11, 10), showYear = false)))
        compose.onNodeWithText(text(R.string.backup_last_exported, "11월 10일 (화)")).assertIsDisplayed()
    }

    @Test
    fun lastExportedDateFromAnotherYearHasYear() {
        show(BackupUiState(lastExported = ExportDate(LocalDate.of(2025, 12, 28), showYear = true)))
        compose.onNodeWithText(text(R.string.backup_last_exported, "2025년 12월 28일 (일)")).assertIsDisplayed()
    }

    @Test
    fun busyDisablesBothButtons() {
        show(BackupUiState(busy = true))
        compose.onNodeWithText(text(R.string.backup_export)).assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.backup_import)).assertIsNotEnabled()
    }

    @Test
    fun confirmationShowsBothSummariesAndInProgressWarning() {
        show(
            BackupUiState(
                confirmation = ImportConfirmation(
                    file = BackupSummary(workoutCount = 42, routineCount = 4, hasWorkoutInProgress = false),
                    device = BackupSummary(workoutCount = 3, routineCount = 1, hasWorkoutInProgress = true),
                ),
            ),
        )
        compose.onNode(isDialog()).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.backup_confirm_file, 42, 4)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.backup_confirm_device, 3, 1)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.backup_confirm_in_progress)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.backup_confirm_question)).assertIsDisplayed()

        compose.onNode(hasText(text(R.string.action_cancel)) and hasAnyAncestor(isDialog())).performClick()
        compose.onNode(hasText(text(R.string.backup_confirm_replace)) and hasAnyAncestor(isDialog())).performClick()
        assertEquals(listOf("cancel", "confirm"), calls)
    }

    @Test
    fun confirmationWithoutWorkoutInProgressHasNoWarning() {
        val summary = BackupSummary(workoutCount = 0, routineCount = 0, hasWorkoutInProgress = false)
        show(BackupUiState(confirmation = ImportConfirmation(file = summary.copy(hasWorkoutInProgress = true), device = summary)))
        compose.onNodeWithText(text(R.string.backup_confirm_in_progress)).assertDoesNotExist()
    }

    @Test
    fun eachErrorHasItsOwnMessage() {
        val messages = mapOf(
            BackupError.NOT_BACKUP to R.string.backup_error_not_backup,
            BackupError.NEWER_VERSION to R.string.backup_error_newer_version,
            BackupError.BROKEN to R.string.backup_error_broken,
            BackupError.READ_FAILED to R.string.backup_error_read_failed,
            BackupError.WRITE_FAILED to R.string.backup_error_write_failed,
            BackupError.IMPORT_FAILED to R.string.backup_error_import_failed,
            BackupError.PICKER_UNAVAILABLE to R.string.backup_error_picker_unavailable,
        )
        assertEquals(BackupError.entries.toSet(), messages.keys)
        assertEquals("오류마다 문구가 다르다", messages.size, messages.values.map(::text).toSet().size)
        var error by mutableStateOf(BackupError.NOT_BACKUP)
        compose.setContent {
            NextSetTheme {
                BackupScreen(
                    state = BackupUiState(error = error),
                    onBack = {},
                    onExport = {},
                    onImport = {},
                    onConfirmImport = {},
                    onCancelImport = {},
                    onMessageShown = {},
                )
            }
        }
        messages.forEach { (shown, id) ->
            error = shown
            compose.onNodeWithText(text(id)).assertIsDisplayed()
        }
        // 오류가 있어도 다시 시도할 수 있다.
        compose.onNodeWithText(text(R.string.backup_import)).assertIsEnabled()
    }

    @Test
    fun messageIsShownOnceAsSnackbar() {
        show(BackupUiState(message = BackupMessage.EXPORTED))
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText(text(R.string.backup_exported))).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(listOf("shown"), calls)
    }
}
