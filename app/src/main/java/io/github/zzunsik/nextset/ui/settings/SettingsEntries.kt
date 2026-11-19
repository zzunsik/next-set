package io.github.zzunsik.nextset.ui.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.repository.LicenseRepository
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import io.github.zzunsik.nextset.ui.navigation.LicenseDetailKey
import io.github.zzunsik.nextset.ui.navigation.LicensesKey
import io.github.zzunsik.nextset.ui.navigation.SettingsKey
import kotlinx.coroutines.launch

/**
 * 설정과 오픈소스 라이선스 화면을 [backStack]에 잇는다.
 *
 * [onManageExercises]는 종목 고르기를 관리로 연다(#21, `NextSetNavDisplay`가 넘긴다). [onBackup]은 백업(#29) 화면이 생기면 넘긴다.
 * null이면 그 행은 "준비 중이에요."를 보이고 누르는 요소가 아니다.
 */
fun EntryProviderScope<NavKey>.settingsEntries(
    backStack: MutableList<NavKey>,
    settingsRepository: SettingsRepository,
    licenseRepository: LicenseRepository,
    onManageExercises: (() -> Unit)? = null,
    onBackup: (() -> Unit)? = null,
) {
    entry<SettingsKey> {
        val viewModel = viewModel { SettingsViewModel(settingsRepository) }
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val versionName = remember(context) {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }
        val uriHandler = LocalUriHandler.current
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val sourceCodeUrl = stringResource(R.string.settings_source_code_url)
        val openLinkFailed = stringResource(R.string.settings_open_link_failed)
        SettingsScreen(
            state = state,
            versionName = versionName,
            onDefaultRestChange = viewModel::onDefaultRestChange,
            onThemeModeChange = viewModel::onThemeModeChange,
            onOpenLicenses = { backStack.add(LicensesKey) },
            onOpenSourceCode = {
                // 기기의 브라우저로 연다(ACTION_VIEW). 앱은 네트워크에 접속하지 않는다(DATA-02).
                // 열 앱이 없으면 AndroidUriHandler가 IllegalArgumentException을 던진다.
                try {
                    uriHandler.openUri(sourceCodeUrl)
                } catch (_: IllegalArgumentException) {
                    scope.launch { snackbarHostState.showSnackbar(openLinkFailed) }
                }
            },
            onManageExercises = onManageExercises,
            onBackup = onBackup,
            snackbarHostState = snackbarHostState,
        )
    }
    entry<LicensesKey> {
        val viewModel = viewModel { LicensesViewModel(licenseRepository) }
        val notices by viewModel.notices.collectAsStateWithLifecycle()
        LicensesScreen(
            notices = notices,
            onOpen = { backStack.add(LicenseDetailKey(it.id)) },
            onBack = { backStack.removeLastOrNull() },
        )
    }
    entry<LicenseDetailKey> { key ->
        val viewModel = viewModel { LicenseDetailViewModel(licenseRepository, key.id) }
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        LicenseDetailScreen(state = state, onBack = { backStack.removeLastOrNull() })
    }
}
