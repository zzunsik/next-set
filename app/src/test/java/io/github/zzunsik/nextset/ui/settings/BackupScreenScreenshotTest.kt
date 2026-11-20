package io.github.zzunsik.nextset.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.zzunsik.nextset.domain.model.BackupSummary
import io.github.zzunsik.nextset.ui.a11y.assertAccessible
import io.github.zzunsik.nextset.ui.screenshot.LARGE_FONT_SCALE
import io.github.zzunsik.nextset.ui.screenshot.SCREENSHOT_DEVICE
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 백업 화면의 스크린샷 비교와 접근성 검사. 값은 지어낸 것이다.
 * 기본 화면, 가져오기 확인, 오류 안내를 밝은 화면과 어두운 화면, 글자 크기 기본과 200%로 찍는다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class BackupScreenScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val exported = BackupUiState(lastExported = ExportDate(LocalDate.of(2026, 11, 10), showYear = false))

    private val confirmation = ImportConfirmation(
        file = BackupSummary(workoutCount = 42, routineCount = 4, hasWorkoutInProgress = false),
        device = BackupSummary(workoutCount = 3, routineCount = 1, hasWorkoutInProgress = true),
    )

    private fun setScreen(state: BackupUiState) {
        compose.setContent {
            NextSetTheme {
                BackupScreen(
                    state = state,
                    onBack = {},
                    onExport = {},
                    onImport = {},
                    onConfirmImport = {},
                    onCancelImport = {},
                    onMessageShown = {},
                )
            }
        }
    }

    private fun captureScreen(state: BackupUiState = exported) {
        setScreen(state)
        // 접근성 검사를 먼저 돌려 접근성 문제를 스크린샷 차이보다 먼저 알린다.
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
    }

    /**
     * 가져오기 확인. 대화상자는 창이 따로 떠서 `assertAccessible()`(화면 하나를 본다)이 닿지 않으므로
     * 대화상자의 본문과 버튼을 대화상자 바탕(`surface`) 위에 그대로 그려 먼저 검사하고, 화면 전체를 찍는다.
     */
    private fun captureConfirmation() {
        // 한 테스트에서 setContent는 한 번만 부를 수 있어 상태로 바꿔 그린다.
        var showDialog by mutableStateOf(false)
        compose.setContent {
            NextSetTheme {
                if (showDialog) {
                    BackupScreen(
                        state = exported.copy(confirmation = confirmation),
                        onBack = {},
                        onExport = {},
                        onImport = {},
                        onConfirmImport = {},
                        onCancelImport = {},
                        onMessageShown = {},
                    )
                } else {
                    Surface(color = NextSetTheme.colors.surface) {
                        Column {
                            ImportConfirmText(confirmation)
                            Row {
                                ImportCancelButton(onClick = {})
                                ImportReplaceButton(onClick = {})
                            }
                        }
                    }
                }
            }
        }
        compose.assertAccessible()
        showDialog = true
        compose.waitForIdle()
        captureScreenRoboImage()
    }

    @Test
    fun light() = captureScreen()

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun lightLargeFont() = captureScreen()

    @Test
    @Config(qualifiers = "+night")
    fun dark() = captureScreen()

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun darkLargeFont() = captureScreen()

    @Test
    fun neverExported() = captureScreen(BackupUiState())

    @Test
    fun confirmation() = captureConfirmation()

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun confirmationLargeFont() = captureConfirmation()

    @Test
    @Config(qualifiers = "+night")
    fun confirmationDark() = captureConfirmation()

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun confirmationDarkLargeFont() = captureConfirmation()

    @Test
    fun error() = captureScreen(exported.copy(error = BackupError.NOT_BACKUP))

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun errorDarkLargeFont() = captureScreen(exported.copy(error = BackupError.NEWER_VERSION))
}
