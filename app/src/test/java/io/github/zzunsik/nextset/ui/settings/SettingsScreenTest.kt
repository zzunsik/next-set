package io.github.zzunsik.nextset.ui.settings

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.annotation.StringRes
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.ThemeMode
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private fun text(@StringRes id: Int, vararg args: Any) = app.getString(id, *args)

    private val restChanges = mutableListOf<Int>()
    private val themeChanges = mutableListOf<ThemeMode>()
    private val opened = mutableListOf<String>()

    private fun show(
        state: SettingsUiState? = SettingsUiState(defaultRestSeconds = 120, themeMode = ThemeMode.SYSTEM),
        onManageExercises: (() -> Unit)? = null,
    ) {
        compose.setContent {
            NextSetTheme {
                SettingsScreen(
                    state = state,
                    versionName = "9.8.7",
                    onDefaultRestChange = { restChanges += it },
                    onThemeModeChange = { themeChanges += it },
                    onOpenLicenses = { opened += "licenses" },
                    onOpenSourceCode = { opened += "source" },
                    onManageExercises = onManageExercises,
                )
            }
        }
    }

    @Test
    fun showsCurrentValuesAndVersion() {
        show(SettingsUiState(defaultRestSeconds = 90, themeMode = ThemeMode.DARK))

        compose.onNodeWithText(text(R.string.settings_title)).assertIsDisplayed()
        compose.onNodeWithText("1분 30초").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.theme_mode_dark)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.settings_version, "9.8.7")).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun restDurationIsWrittenInMinutesAndSeconds() {
        show(SettingsUiState(defaultRestSeconds = 120, themeMode = ThemeMode.SYSTEM))
        compose.onNodeWithText("2분").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.theme_mode_system)).assertIsDisplayed()
    }

    @Test
    fun choosingDefaultRestInDialogSavesAndCloses() {
        show()
        compose.onNodeWithText(text(R.string.settings_default_rest)).performClick()
        compose.onNode(isDialog()).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.settings_default_rest_note)).assertIsDisplayed()
        compose.onNodeWithText("30초").assertIsDisplayed()

        compose.onNode(hasText("1분 15초") and hasClickAction() and hasAnyAncestor(isDialog())).performScrollTo().performClick()

        assertEquals(listOf(75), restChanges)
        compose.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun choosingEachThemeModeInDialogSavesIt() {
        show()
        for ((label, mode) in listOf(
            R.string.theme_mode_dark to ThemeMode.DARK,
            R.string.theme_mode_light to ThemeMode.LIGHT,
            R.string.theme_mode_system to ThemeMode.SYSTEM,
        )) {
            compose.onNodeWithText(text(R.string.settings_theme_mode)).performClick()
            compose.onNode(hasText(text(label)) and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
            compose.onNode(isDialog()).assertDoesNotExist()
        }
        assertEquals(listOf(ThemeMode.DARK, ThemeMode.LIGHT, ThemeMode.SYSTEM), themeChanges)
    }

    @Test
    fun closeButtonDismissesWithoutChange() {
        show()
        compose.onNodeWithText(text(R.string.settings_theme_mode)).performClick()
        compose.onNodeWithText(text(R.string.action_close)).performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        assertEquals(emptyList<ThemeMode>(), themeChanges)
    }

    @Test
    fun rowsWithoutScreenSayNotReadyAndAreNotClickable() {
        show()
        compose.onNodeWithText(text(R.string.settings_manage_exercises)).assertHasNoClickAction()
        compose.onNodeWithText(text(R.string.settings_backup)).assertHasNoClickAction()
        assertEquals(2, compose.onAllNodes(hasText(text(R.string.settings_not_ready))).fetchSemanticsNodes().size)
    }

    @Test
    fun manageExercisesRowOpensWhenScreenIsGiven() {
        var managed = 0
        show(onManageExercises = { managed++ })
        compose.onNodeWithText(text(R.string.settings_manage_exercises)).performClick()
        assertEquals(1, managed)
        assertEquals(1, compose.onAllNodes(hasText(text(R.string.settings_not_ready))).fetchSemanticsNodes().size)
    }

    @Test
    fun licensesAndSourceCodeRowsCallBack() {
        show()
        compose.onNodeWithText(text(R.string.settings_licenses)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.settings_source_code)).performScrollTo().performClick()
        assertEquals(listOf("licenses", "source"), opened)
    }
}
