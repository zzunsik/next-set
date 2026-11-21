package io.github.zzunsik.nextset.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import io.github.zzunsik.nextset.R
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
import io.github.zzunsik.nextset.domain.repository.RoutineRepository
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import io.github.zzunsik.nextset.domain.repository.WorkoutRepository
import io.github.zzunsik.nextset.domain.rest.RestTimer
import io.github.zzunsik.nextset.ui.exercise.exerciseEntries
import io.github.zzunsik.nextset.ui.history.historyEntries
import io.github.zzunsik.nextset.ui.home.HomeRoute
import io.github.zzunsik.nextset.ui.home.HomeViewModel
import io.github.zzunsik.nextset.ui.workout.workoutEntries
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.zzunsik.nextset.ui.routine.routineEntries
import io.github.zzunsik.nextset.ui.settings.backupEntries
import io.github.zzunsik.nextset.ui.settings.settingsEntries

/**
 * 앱의 화면 이동. 뒤로 가기 스택은 화면 키의 목록이다.
 * 화면을 더할 때는 [NavKeys.kt]에 키를 만들고 [NextSetNavHost]의 entryProvider에 연결한다.
 *
 * 탭 화면(홈, 기록, 설정) 아래에는 하단 내비게이션을 붙인다([TopLevelScaffold]). 운동 중과 하위 화면에서는 숨긴다.
 */
@Composable
fun NextSetNavDisplay(
    settingsRepository: SettingsRepository,
    licenseRepository: LicenseRepository,
    exerciseRepository: ExerciseRepository,
    workoutRepository: WorkoutRepository,
    routineRepository: RoutineRepository,
    backupRepository: BackupRepository,
    modifier: Modifier = Modifier,
    restTimer: RestTimer? = null,
) {
    val backStack = rememberNavBackStack(HomeKey)
    val results = remember { NavResults() }
    val entries: EntryProviderScope<NavKey>.() -> Unit = {
        entry<HomeKey> {
            val newRoutineName = stringResource(R.string.routine_new)
            val viewModel = viewModel {
                HomeViewModel(workoutRepository, routineRepository, settingsRepository, newRoutineName)
            }
            HomeRoute(
                viewModel = viewModel,
                onOpenWorkout = { backStack.navigate(HomeKey, WorkoutKey(it)) },
                onOpenRoutine = { id, isNew -> backStack.navigate(HomeKey, RoutineEditKey(id, isNew)) },
            )
        }
        workoutEntries(backStack, results, workoutRepository, routineRepository, settingsRepository, exerciseRepository, restTimer)
        historyEntries(backStack, results, workoutRepository, routineRepository)
        routineEntries(backStack, results, routineRepository, exerciseRepository, settingsRepository)
        settingsEntries(
            backStack = backStack,
            settingsRepository = settingsRepository,
            licenseRepository = licenseRepository,
            onManageExercises = { backStack.navigate(SettingsKey, ExercisePickerKey(mode = ExercisePickerMode.Manage)) },
            onBackup = { backStack.navigate(SettingsKey, BackupKey) },
            onNotificationsAllowed = { restTimer?.onNotificationsAllowed() },
        )
        backupEntries(backStack = backStack, backupRepository = backupRepository)
    }
    TopLevelScaffold(
        current = backStack.lastOrNull(),
        onSelect = { backStack.selectTab(it) },
        modifier = modifier,
    ) { contentModifier ->
        NextSetNavHost(
            backStack = backStack,
            results = results,
            exerciseRepository = exerciseRepository,
            modifier = contentModifier,
            entries = entries,
        )
    }
}

/**
 * [backStack]을 그리는 곳. 여러 화면이 함께 쓰는 화면(종목 고르기 등)은 여기서 잇고,
 * 부르는 쪽 화면은 [entries]로 더한다. 테스트도 이 함수로 같은 화면을 띄운다.
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
    // Android 8.x에서 화면이 바뀔 때 입력 칸이 저절로 포커스를 잡아 키보드가 뜨지 않게 한다(#105).
    KeepWindowRefocusOffTextFields()
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
