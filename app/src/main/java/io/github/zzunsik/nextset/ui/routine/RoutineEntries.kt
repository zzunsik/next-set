package io.github.zzunsik.nextset.ui.routine

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.repository.ExerciseRepository
import io.github.zzunsik.nextset.domain.repository.RoutineRepository
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerKey
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerMode
import io.github.zzunsik.nextset.ui.navigation.NavResults
import io.github.zzunsik.nextset.ui.navigation.ResultEffect
import io.github.zzunsik.nextset.ui.navigation.ResultKey
import io.github.zzunsik.nextset.ui.navigation.RoutineEditKey
import io.github.zzunsik.nextset.ui.navigation.RoutineExerciseKey
import io.github.zzunsik.nextset.ui.navigation.pickedExercisesKey
import kotlinx.coroutines.launch

/** 루틴 편집이 종목 고르기에 넘기는 결과 이름. 루틴마다 다르게 한다. */
internal fun routinePickerResultName(routineId: Long) = "routine-edit/$routineId/picked"

/** 종목 설정에서 뺀 종목을 루틴 편집에 돌려주는 키. */
internal fun removedRoutineExerciseKey(routineId: Long) = ResultKey<RemovedRoutineExercise>("routine-edit/$routineId/removed")

/**
 * 루틴 편집과 종목 설정을 [backStack]에 잇는다. 루틴 편집은 홈의 "새 루틴"과 메뉴의 "편집"이 연다.
 *
 * - 루틴 편집의 "종목 추가"는 종목 고르기를 고르기로 열고, 고른 종목을 [NavResults]로 받아 루틴 끝에 더한다.
 * - 종목 설정의 "루틴에서 빼기"는 뺀 종목을 루틴 편집에 돌려주고, 루틴 편집이 "되돌리기"가 있는 알림 줄을 보인다.
 * - 루틴 편집을 나갈 때(상단 바의 뒤로 가기, 시스템 뒤로 가기)는 앞서 고친 내용을 저장한 뒤 닫는다.
 *   이름도 종목도 없는 새 루틴은 이때 버린다.
 */
fun EntryProviderScope<NavKey>.routineEntries(
    backStack: MutableList<NavKey>,
    results: NavResults,
    routineRepository: RoutineRepository,
    exerciseRepository: ExerciseRepository,
    settingsRepository: SettingsRepository,
) {
    /** [key] 화면이 맨 위에 있을 때만 닫는다. 두 번 불려도 한 화면만 닫힌다. */
    fun close(key: NavKey) {
        if (backStack.lastOrNull() == key) backStack.removeAt(backStack.lastIndex)
    }

    entry<RoutineEditKey> { key ->
        val newRoutineName = stringResource(R.string.routine_new)
        val viewModel = viewModel { RoutineEditViewModel(routineRepository, key.routineId, key.isNew, newRoutineName) }
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        // 알림 줄 문구. 뺀 종목 이름은 돌아온 값에서 채운다.
        val removedMessage = stringResource(R.string.routine_exercise_removed)
        val undoLabel = stringResource(R.string.action_undo)
        results.ResultEffect(pickedExercisesKey(routinePickerResultName(key.routineId))) { ids ->
            viewModel.addExercises(ids)
        }
        results.ResultEffect(removedRoutineExerciseKey(key.routineId)) { removed ->
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = removedMessage.format(removed.exerciseName),
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.restore(removed)
            }
        }
        val leave = { viewModel.leave { close(key) } }
        BackHandler(onBack = leave)
        RoutineEditScreen(
            // 이름 칸은 화면 상태가 아니라 화면 상태 객체의 Compose 상태를 바로 읽는다(RoutineEditViewModel.nameText).
            state = state.copy(name = viewModel.nameText),
            onBack = leave,
            onNameChange = viewModel::onNameChange,
            onAddExercise = {
                backStack.add(ExercisePickerKey(ExercisePickerMode.Pick, routinePickerResultName(key.routineId)))
            },
            onOpenExercise = { backStack.add(RoutineExerciseKey(key.routineId, it.id)) },
            onReorder = viewModel::reorder,
            onDelete = { viewModel.delete { close(key) } },
            snackbarHostState = snackbarHostState,
        )
    }
    entry<RoutineExerciseKey> { key ->
        val viewModel = viewModel {
            RoutineExerciseViewModel(
                routineRepository,
                exerciseRepository,
                settingsRepository,
                key.routineId,
                key.routineExerciseId,
            )
        }
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        RoutineExerciseScreen(
            state = state,
            onBack = { close(key) },
            onSetCountChange = viewModel::onSetCountChange,
            onFieldClick = viewModel::onFieldClick,
            onNumberPadKey = viewModel::onNumberPadKey,
            onRestChange = viewModel::onRestChange,
            onRemove = {
                viewModel.remove { removed ->
                    results.put(removedRoutineExerciseKey(key.routineId), removed)
                    close(key)
                }
            },
        )
    }
}
