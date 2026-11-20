package io.github.zzunsik.nextset.ui.workout

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zzunsik.nextset.R
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** 최고 기록 알림 줄을 보이는 시간(docs/DESIGN.md `RecordBadge`: 3초). */
internal const val RECORD_SNACKBAR_MILLIS = 3_000L

/**
 * [WorkoutViewModel]의 상태로 [WorkoutScreen]을 그리고 한 번만 처리할 일을 받는다.
 *
 * @param onExit 운동을 끝내거나 버려 화면을 떠날 때.
 * @param onSetCompleted 세트를 완료했을 때. 휴식 타이머(#26)가 여기에 붙는다. 지금은 아무것도 하지 않는다.
 * @param onSetUncompleted 완료를 풀었을 때. 휴식 타이머(#26)가 그 세트로 시작한 타이머를 멈춘다.
 * @param restTimerBar 화면 아래 고정 바의 자리(#26). 숫자판 바로 위에 붙는다.
 */
@Composable
fun WorkoutRoute(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    onAddExercise: () -> Unit,
    onExit: (WorkoutExit) -> Unit,
    onSetCompleted: (WorkoutEvent.SetCompleted) -> Unit = {},
    onSetUncompleted: (WorkoutEvent.SetUncompleted) -> Unit = {},
    restTimerBar: @Composable () -> Unit = {},
    clock: () -> Instant = Instant::now,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val resources = LocalResources.current
    val currentOnExit by rememberUpdatedState(onExit)
    val currentOnSetCompleted by rememberUpdatedState(onSetCompleted)
    val currentOnSetUncompleted by rememberUpdatedState(onSetUncompleted)

    var now by remember { mutableStateOf(clock()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = clock()
            delay(TICK_MILLIS)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is WorkoutEvent.NewRecord -> {
                    // 진동 한 번과 알림 줄 3초(LOG-14, docs/DESIGN.md `RecordBadge`).
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val message = resources.getString(R.string.record_new, event.exerciseName)
                    launch {
                        val shown = launch { snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Indefinite) }
                        delay(RECORD_SNACKBAR_MILLIS)
                        shown.cancel()
                    }
                }
                WorkoutEvent.SetDeleted -> launch {
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.set_deleted),
                        actionLabel = resources.getString(R.string.action_undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete()
                }
                is WorkoutEvent.SetCompleted -> currentOnSetCompleted(event)
                is WorkoutEvent.SetUncompleted -> currentOnSetUncompleted(event)
                is WorkoutEvent.Exit -> currentOnExit(event.destination)
            }
        }
    }

    // 세트를 완료하면 목록이 다음 세트 행이 보이는 곳으로 움직인다(docs/DESIGN.md "운동 중").
    LaunchedEffect(listState) {
        snapshotFlow { viewModel.uiState.value.nextSet?.setId }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { setId ->
                val index = viewModel.uiState.value.listIndexOf(setId) ?: return@collect
                val visible = listState.layoutInfo.visibleItemsInfo
                val fullyVisible = visible.any { item ->
                    item.index == index && item.offset >= 0 &&
                        item.offset + item.size <= listState.layoutInfo.viewportEndOffset - listState.layoutInfo.afterContentPadding
                }
                if (!fullyVisible) listState.animateScrollToItem((index - 1).coerceAtLeast(0))
            }
    }

    WorkoutScreen(
        state = state,
        now = now,
        snackbarHostState = snackbarHostState,
        listState = listState,
        restTimerBar = restTimerBar,
        actions = WorkoutActions(
            onBack = onBack,
            onFinish = viewModel::onFinishRequest,
            onWorkoutNote = viewModel::onWorkoutNoteRequest,
            onReorder = viewModel::onReorderRequest,
            onDiscard = viewModel::onDiscardRequest,
            onAddExercise = onAddExercise,
            onFieldClick = viewModel::onFieldClick,
            onNumberPadKey = viewModel::onNumberPadKey,
            onCheck = viewModel::onCheck,
            onAddSet = viewModel::onAddSet,
            onDeleteSet = viewModel::onDeleteSet,
            onDismissDialog = viewModel::dismissDialog,
            onDiscardConfirm = viewModel::onDiscardConfirm,
            onIncompleteConfirm = viewModel::onIncompleteConfirm,
            onRoutineChangeAnswer = viewModel::onRoutineChangeAnswer,
            onRemoveExerciseConfirm = viewModel::onRemoveExerciseConfirm,
            onExerciseNoteChange = viewModel::onExerciseNoteChange,
            onWorkoutNoteChange = viewModel::onWorkoutNoteChange,
            onDeleteLastSet = viewModel::onDeleteLastSet,
            onExerciseNote = viewModel::onExerciseNoteRequest,
            onMoveExercise = viewModel::onMoveExercise,
            onRemoveExercise = viewModel::onRemoveExerciseRequest,
            onRestTime = viewModel::onRestTimeRequest,
            onRestTimeChange = viewModel::onRestTimeChange,
        ),
    )
}

private const val TICK_MILLIS = 1_000L
