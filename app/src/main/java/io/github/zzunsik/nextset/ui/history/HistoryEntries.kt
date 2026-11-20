package io.github.zzunsik.nextset.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.repository.RoutineRepository
import io.github.zzunsik.nextset.domain.repository.WorkoutRepository
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerKey
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerMode
import io.github.zzunsik.nextset.ui.navigation.HistoryDetailKey
import io.github.zzunsik.nextset.ui.navigation.HistoryKey
import io.github.zzunsik.nextset.ui.navigation.NavResults
import io.github.zzunsik.nextset.ui.navigation.ResultEffect
import io.github.zzunsik.nextset.ui.navigation.WorkoutSummaryKey
import io.github.zzunsik.nextset.ui.navigation.pickedExercisesKey
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch

/** 기록 수정이 종목 고르기에 넘기는 결과 이름. 기록마다 다르게 한다. */
internal fun historyPickerResultName(workoutId: Long) = "history/$workoutId/add-exercise"

/**
 * 운동 요약, 기록 목록, 기록 상세·수정을 [backStack]에 잇는다(docs/DESIGN.md "이동 구조").
 *
 * - 운동 요약: "확인"과 뒤로 가기 모두 앞 화면(홈)으로 간다. 운동 중 화면은 이미 스택에서 빠졌다.
 * - 기록 목록: 기록 행은 기록 상세를 연다. "지난 운동 적기"는 날짜와 루틴을 고른 뒤 기록 상세를 수정 상태로 연다.
 *   [historyBack]이 null이 아니면 상단 바에 뒤로 가기를 둔다. 하단 내비게이션(#23)이 생기기 전까지는 홈의 임시 버튼으로
 *   들어오므로 뒤로 가기가 있다. 탭으로 열면 null을 넘긴다.
 * - 기록 상세: 나갈 때(상단 바의 뒤로 가기, 시스템 뒤로 가기) 빈 세트를 정리한 뒤 닫는다. 종목 추가는 종목 고르기를 연다.
 */
fun EntryProviderScope<NavKey>.historyEntries(
    backStack: MutableList<NavKey>,
    results: NavResults,
    workoutRepository: WorkoutRepository,
    routineRepository: RoutineRepository,
    zone: ZoneId = ZoneId.systemDefault(),
    historyBack: Boolean = true,
) {
    /** [key] 화면이 맨 위에 있을 때만 닫는다. 두 번 불려도 한 화면만 닫힌다. */
    fun close(key: NavKey) {
        if (backStack.lastOrNull() == key) backStack.removeAt(backStack.lastIndex)
    }

    entry<WorkoutSummaryKey> { key ->
        val viewModel = viewModel { WorkoutReportViewModel(workoutRepository, key.workoutId, zone = zone) }
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        WorkoutSummaryScreen(
            state = state,
            noteText = viewModel.noteText,
            onNoteChange = viewModel::onNoteChange,
            onConfirm = { close(key) },
        )
    }
    entry<HistoryKey> {
        val viewModel = viewModel { HistoryListViewModel(workoutRepository, routineRepository, zone = zone) }
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        LaunchedEffect(viewModel) {
            viewModel.openCreated.collect { id -> backStack.add(HistoryDetailKey(id, editing = true, isNew = true)) }
        }
        HistoryListScreen(
            state = state,
            actions = HistoryListActions(
                onBack = if (historyBack) ({ close(HistoryKey) }) else null,
                onOpen = { backStack.add(HistoryDetailKey(it)) },
                onAddPast = {
                    val today = LocalDate.now(zone)
                    showDatePicker(context, today, today, zone, viewModel::onDatePicked)
                },
                onAddPastChoice = viewModel::onAddPast,
                onDismissAddPast = viewModel::dismissAddPast,
                onGoHome = { backStack.retainAll(listOf(backStack.first())) },
            ),
        )
    }
    entry<HistoryDetailKey> { key ->
        val viewModel = viewModel {
            WorkoutReportViewModel(workoutRepository, key.workoutId, startEditing = key.editing, isNew = key.isNew, zone = zone)
        }
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val resources = LocalResources.current
        val context = LocalContext.current
        results.ResultEffect(pickedExercisesKey(historyPickerResultName(key.workoutId))) { ids ->
            viewModel.onExercisesPicked(ids)
        }
        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    ReportEvent.Deleted -> close(key)
                    ReportEvent.SetDeleted -> launch {
                        val result = snackbarHostState.showSnackbar(
                            message = resources.getString(R.string.set_deleted),
                            actionLabel = resources.getString(R.string.action_undo),
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete()
                    }
                }
            }
        }
        // 다른 곳에서 지워져 기록이 없어졌으면 닫는다.
        LaunchedEffect(state.loading, state.report == null) {
            if (!state.loading && state.report == null) close(key)
        }
        val leave = { viewModel.leave { close(key) } }
        BackHandler(onBack = leave)
        HistoryDetailScreen(
            state = state,
            noteText = viewModel.noteText,
            today = LocalDate.now(zone),
            zone = zone,
            snackbarHostState = snackbarHostState,
            actions = HistoryDetailActions(
                onBack = leave,
                onEditToggle = viewModel::onEditToggle,
                onDeleteRequest = viewModel::onDeleteRequest,
                onDeleteConfirm = viewModel::onDeleteConfirm,
                onDismissDialog = viewModel::dismissDialog,
                onPickDate = {
                    state.report?.let { report ->
                        val today = LocalDate.now(zone)
                        showDatePicker(context, report.startedAt.atZone(zone).toLocalDate(), today, zone, viewModel::onDateChange)
                    }
                },
                onPickTime = {
                    state.report?.let { report ->
                        showTimePicker(context, report.startedAt.atZone(zone).toLocalTime(), viewModel::onStartTimeChange)
                    }
                },
                onMinutesClick = viewModel::onMinutesClick,
                onNumberPadKey = viewModel::onNumberPadKey,
                onAddExercise = {
                    backStack.add(ExercisePickerKey(ExercisePickerMode.Pick, historyPickerResultName(key.workoutId)))
                },
                onRemoveExerciseConfirm = viewModel::onRemoveExerciseConfirm,
                onExerciseNoteChange = viewModel::onExerciseNoteChange,
                onNoteChange = viewModel::onNoteChange,
                edit = ReportEditActions(
                    onFieldClick = viewModel::onFieldClick,
                    onAddSet = viewModel::onAddSet,
                    onDeleteSet = viewModel::onDeleteSet,
                    onDeleteLastSet = viewModel::onDeleteLastSet,
                    onExerciseNote = viewModel::onExerciseNoteRequest,
                    onRemoveExercise = viewModel::onRemoveExerciseRequest,
                ),
            ),
        )
    }
}
