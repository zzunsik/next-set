package io.github.zzunsik.nextset.ui.settings

import io.github.zzunsik.nextset.domain.model.Settings
import io.github.zzunsik.nextset.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import io.github.zzunsik.nextset.ui.theme.ThemeMode as UiThemeMode

class SettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun stateFollowsRepository() = runTest {
        val repository = FakeSettingsRepository(initial = null)
        val viewModel = SettingsViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        assertNull(viewModel.uiState.value)

        repository.settings.value = Settings(defaultRestSeconds = 90, themeMode = ThemeMode.DARK)
        assertEquals(SettingsUiState(defaultRestSeconds = 90, themeMode = ThemeMode.DARK), viewModel.uiState.value)
    }

    @Test
    fun choosingDefaultRestSavesIt() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onDefaultRestChange(75)

        assertEquals(75, repository.observeSettings().first().defaultRestSeconds)
        assertEquals(75, viewModel.uiState.value?.defaultRestSeconds)
    }

    @Test
    fun invalidDefaultRestIsIgnored() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)

        listOf(15, 100, 315).forEach(viewModel::onDefaultRestChange)

        assertEquals(Settings.DEFAULT_REST_SECONDS, repository.observeSettings().first().defaultRestSeconds)
    }

    @Test
    fun choosingThemeModeSavesEachMode() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        for (mode in listOf(ThemeMode.DARK, ThemeMode.LIGHT, ThemeMode.SYSTEM)) {
            viewModel.onThemeModeChange(mode)
            assertEquals(mode, repository.observeSettings().first().themeMode)
            assertEquals(mode, viewModel.uiState.value?.themeMode)
        }
    }
}

class AppThemeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun waitsForStoredModeThenFollowsChanges() = runTest {
        val repository = FakeSettingsRepository(initial = null)
        val viewModel = AppThemeViewModel(repository)
        assertNull(viewModel.themeMode.value)

        repository.settings.value = Settings(themeMode = ThemeMode.DARK)
        assertEquals(UiThemeMode.Dark, viewModel.themeMode.value)

        // 설정 화면에서 고르면 저장소를 거쳐 바로 바뀐다.
        val settings = SettingsViewModel(repository)
        settings.onThemeModeChange(ThemeMode.LIGHT)
        assertEquals(UiThemeMode.Light, viewModel.themeMode.value)
        settings.onThemeModeChange(ThemeMode.SYSTEM)
        assertEquals(UiThemeMode.System, viewModel.themeMode.value)
    }

    @Test
    fun mapsEveryStoredMode() {
        assertEquals(
            listOf(UiThemeMode.System, UiThemeMode.Light, UiThemeMode.Dark),
            ThemeMode.entries.map { it.toUiThemeMode() },
        )
    }
}

class LicensesViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun loadsNoticesAndDetail() = runTest {
        val repository = FakeLicenseRepository()

        assertEquals(repository.notices, LicensesViewModel(repository).notices.value)

        val detail = LicenseDetailViewModel(repository, "lib").uiState.value
        assertEquals(repository.notices[1], detail?.notice)
        assertEquals(repository.licenseText("lib"), detail?.text)
    }
}
