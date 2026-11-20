package io.github.zzunsik.nextset.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.zzunsik.nextset.domain.repository.BackupRepository
import io.github.zzunsik.nextset.domain.repository.ExerciseRepository
import io.github.zzunsik.nextset.domain.repository.LicenseRepository
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import io.github.zzunsik.nextset.ui.exercise.exerciseEntries
import io.github.zzunsik.nextset.ui.home.HomeScreen
import io.github.zzunsik.nextset.ui.settings.backupEntries
import io.github.zzunsik.nextset.ui.settings.settingsEntries

/**
 * 앱의 화면 이동. 뒤로 가기 스택은 화면 키의 목록이다.
 * 화면을 더할 때는 [NavKeys.kt]에 키를 만들고 [NextSetNavHost]의 entryProvider에 연결한다.
 */
@Composable
fun NextSetNavDisplay(
    settingsRepository: SettingsRepository,
    licenseRepository: LicenseRepository,
    exerciseRepository: ExerciseRepository,
    backupRepository: BackupRepository,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(HomeKey)
    val results = remember { NavResults() }
    NextSetNavHost(
        backStack = backStack,
        results = results,
        exerciseRepository = exerciseRepository,
        modifier = modifier,
    ) {
        entry<HomeKey> { HomeScreen(onOpenSettings = { backStack.add(SettingsKey) }) }
        settingsEntries(
            backStack = backStack,
            settingsRepository = settingsRepository,
            licenseRepository = licenseRepository,
            onManageExercises = { backStack.add(ExercisePickerKey(mode = ExercisePickerMode.Manage)) },
            onBackup = { backStack.add(BackupKey) },
        )
        backupEntries(backStack = backStack, backupRepository = backupRepository)
    }
}

/**
 * [backStack]을 그리는 곳. 여러 화면이 함께 쓰는 화면(종목 고르기 등)은 여기서 잇고,
 * 부르는 쪽 화면은 [entries]로 더한다. 테스트와 디버그 빌드의 시험 화면도 이 함수로 같은 화면을 띄운다.
 * [results]는 뒤 화면이 앞 화면에 값을 돌려줄 때 쓴다.
 */
@Composable
fun NextSetNavHost(
    backStack: MutableList<NavKey>,
    results: NavResults,
    exerciseRepository: ExerciseRepository,
    modifier: Modifier = Modifier,
    entries: EntryProviderScope<NavKey>.() -> Unit,
) {
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
            entries()
            exerciseEntries(backStack, results, exerciseRepository)
        },
    )
}
