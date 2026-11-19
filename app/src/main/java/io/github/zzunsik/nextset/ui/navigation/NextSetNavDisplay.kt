package io.github.zzunsik.nextset.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.zzunsik.nextset.domain.repository.LicenseRepository
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import io.github.zzunsik.nextset.ui.home.HomeScreen
import io.github.zzunsik.nextset.ui.settings.settingsEntries

/**
 * 앱의 화면 이동. 뒤로 가기 스택은 화면 키의 목록이다.
 * 화면을 더할 때는 [NavKeys.kt]에 키를 만들고 아래 entryProvider에 연결한다.
 */
@Composable
fun NextSetNavDisplay(
    settingsRepository: SettingsRepository,
    licenseRepository: LicenseRepository,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(HomeKey)
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            // 화면마다 ViewModel 범위를 따로 잡는다.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeKey> { HomeScreen(onOpenSettings = { backStack.add(SettingsKey) }) }
            settingsEntries(backStack, settingsRepository, licenseRepository)
        },
    )
}
