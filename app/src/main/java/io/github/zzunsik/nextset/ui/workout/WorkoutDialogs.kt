package io.github.zzunsik.nextset.ui.workout

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.rule.defaultRestChoices
import io.github.zzunsik.nextset.ui.component.TextInputField
import io.github.zzunsik.nextset.ui.routine.ReorderableRow
import io.github.zzunsik.nextset.ui.routine.moveByKey
import io.github.zzunsik.nextset.ui.routine.moved
import io.github.zzunsik.nextset.ui.routine.rememberDragOrder
import io.github.zzunsik.nextset.ui.settings.OptionDialog
import io.github.zzunsik.nextset.ui.settings.restDurationText
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * 운동 중 화면의 대화상자. 확인은 되돌릴 수 없는 일(버리기, 종목 빼기)과 끝내기의 물음(docs/DESIGN.md "끝내기의 순서")에만 쓴다.
 * 메모는 글자를 칠 때마다 저장하므로(NF-09) "닫기" 하나만 둔다.
 */
@Composable
internal fun WorkoutDialogs(dialog: WorkoutDialog, state: WorkoutUiState, actions: WorkoutActions) {
    when (dialog) {
        WorkoutDialog.NoCompletedSets -> Confirm(
            text = stringResource(R.string.finish_no_completed),
            confirm = stringResource(R.string.finish_discard),
            dismiss = stringResource(R.string.finish_keep_going),
            destructive = true,
            onConfirm = actions.onDiscardConfirm,
            onDismiss = actions.onDismissDialog,
        )
        is WorkoutDialog.IncompleteSets -> Confirm(
            text = stringResource(R.string.finish_incomplete, dialog.count),
            confirm = stringResource(R.string.workout_finish),
            dismiss = stringResource(R.string.finish_keep_going),
            onConfirm = actions.onIncompleteConfirm,
            onDismiss = actions.onDismissDialog,
        )
        // 바깥을 눌러 닫으면 운동을 끝내지 않고 화면에 남는다.
        WorkoutDialog.RoutineChange -> Confirm(
            text = stringResource(R.string.finish_routine_change),
            confirm = stringResource(R.string.finish_apply_routine),
            dismiss = stringResource(R.string.finish_this_time),
            onConfirm = { actions.onRoutineChangeAnswer(true) },
            onDismiss = actions.onDismissDialog,
            onDismissButton = { actions.onRoutineChangeAnswer(false) },
        )
        // 반영이 실패했다(#80). 운동은 진행 중인 채 남아 있다. 바깥을 눌러 닫아도 운동 중 화면에 남는다.
        WorkoutDialog.RoutineUpdateFailed -> Confirm(
            text = stringResource(R.string.finish_routine_update_failed),
            confirm = stringResource(R.string.finish_retry),
            dismiss = stringResource(R.string.finish_without_routine),
            onConfirm = { actions.onRoutineChangeAnswer(true) },
            onDismiss = actions.onDismissDialog,
            onDismissButton = { actions.onRoutineChangeAnswer(false) },
        )
        WorkoutDialog.Discard -> Confirm(
            text = stringResource(R.string.discard_confirm),
            confirm = stringResource(R.string.finish_discard),
            dismiss = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = actions.onDiscardConfirm,
            onDismiss = actions.onDismissDialog,
        )
        is WorkoutDialog.RemoveExercise -> Confirm(
            text = stringResource(R.string.remove_exercise_confirm, dialog.name),
            confirm = stringResource(R.string.remove_exercise_action),
            dismiss = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = { actions.onRemoveExerciseConfirm(dialog.workoutExerciseId) },
            onDismiss = actions.onDismissDialog,
        )
        is WorkoutDialog.ExerciseNote -> NoteDialog(
            title = dialog.name,
            note = dialog.note,
            onNoteChange = { actions.onExerciseNoteChange(dialog.workoutExerciseId, it) },
            onDismiss = actions.onDismissDialog,
        )
        is WorkoutDialog.WorkoutNote -> NoteDialog(
            title = stringResource(R.string.workout_note),
            note = dialog.note,
            onNoteChange = actions.onWorkoutNoteChange,
            onDismiss = actions.onDismissDialog,
        )
        WorkoutDialog.Reorder -> ReorderDialog(state, actions)
        is WorkoutDialog.RestTime -> OptionDialog(
            title = stringResource(R.string.rest_time_dialog_title, dialog.name),
            note = stringResource(R.string.routine_exercise_rest_note),
            options = listOf<Int?>(null) + defaultRestChoices,
            selected = dialog.restSeconds,
            label = { seconds ->
                if (seconds == null) {
                    stringResource(R.string.routine_exercise_rest_default, restDurationText(state.defaultRestSeconds))
                } else {
                    restDurationText(seconds)
                }
            },
            onSelect = { actions.onRestTimeChange(dialog.exerciseId, it) },
            onDismiss = actions.onDismissDialog,
        )
    }
}

@Composable
internal fun Confirm(
    text: String,
    confirm: String,
    dismiss: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    onDismissButton: () -> Unit = onDismiss,
) {
    val colors = NextSetTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        text = { ConfirmText(text) },
        confirmButton = {
            DialogButton(confirm, if (destructive) colors.error else colors.accent, onConfirm)
        },
        dismissButton = { DialogButton(dismiss, colors.accent, onDismissButton) },
    )
}

/** [Confirm]의 글. 대화상자는 창이 따로 떠서 접근성 검사가 닿지 않아 테스트가 이것만 따로 그려 검사한다. */
@Composable
internal fun ConfirmText(text: String) {
    Text(text = text, style = NextSetTheme.typography.body, color = NextSetTheme.colors.text)
}

@Composable
internal fun DialogButton(text: String, color: Color, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight),
        shape = NextSetTheme.radius.md,
    ) {
        Text(text = text, style = NextSetTheme.typography.label, color = color)
    }
}

@Composable
internal fun NoteDialog(title: String, note: String, onNoteChange: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = NextSetTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(text = title, style = NextSetTheme.typography.heading, color = colors.text) },
        text = {
            TextInputField(value = note, onValueChange = onNoteChange, placeholder = stringResource(R.string.note_hint))
        },
        confirmButton = { DialogButton(stringResource(R.string.action_close), colors.accent, onDismiss) },
    )
}

/** 종목 순서 바꾸기의 한 줄. 끄는 동안 다른 상태(세트 값 등)가 바뀌어도 순서를 되돌리지 않도록 이름과 키만 둔다. */
internal data class ReorderEntry(val id: Long, val name: String)

/**
 * 종목 순서 바꾸기(docs/DESIGN.md "운동 중"). 종목 이름만 나열한 대화상자다.
 * 손잡이를 끌어 옮기고 놓으면 바로 저장한다(NF-09). 화면 낭독기에는 행마다 "위로/아래로 옮기기" 동작이 있다.
 * `ModalBottomSheet`가 M3 안정판에서 실험 API라 시트 대신 `AlertDialog`다.
 */
@Composable
private fun ReorderDialog(state: WorkoutUiState, actions: WorkoutActions) {
    val colors = NextSetTheme.colors
    AlertDialog(
        onDismissRequest = actions.onDismissDialog,
        containerColor = colors.surface,
        title = { ReorderTitle() },
        text = { ReorderList(entries = state.exercises.map { ReorderEntry(it.id, it.name) }, onReorder = actions.onReorderExercises) },
        confirmButton = { DialogButton(stringResource(R.string.action_close), colors.accent, actions.onDismissDialog) },
    )
}

@Composable
internal fun ReorderTitle() {
    Text(
        text = stringResource(R.string.workout_reorder),
        style = NextSetTheme.typography.heading,
        color = NextSetTheme.colors.text,
    )
}

/**
 * 종목 순서 바꾸기 목록. 루틴 편집, 홈 순서 편집과 같은 [ReorderableRow]를 쓴다.
 * 끄는 동안에는 화면에 보이는 순서만 바꾸고 손을 놓을 때 [onReorder]로 한 번 저장한다.
 * 화면 낭독기의 "위로/아래로 옮기기"는 한 칸 옮긴 순서를 바로 [onReorder]로 저장한다.
 */
@Composable
internal fun ReorderList(entries: List<ReorderEntry>, onReorder: (List<Long>) -> Unit, modifier: Modifier = Modifier) {
    val colors = NextSetTheme.colors
    val order = rememberDragOrder(entries)
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        moveByKey(order, ReorderEntry::id, from, to)
    }
    val current = order.value
    LazyColumn(state = listState, modifier = modifier.testTag(REORDER_LIST_TAG)) {
        items(current, key = { it.id }) { entry ->
            val index = current.indexOf(entry)
            ReorderableItem(reorderState, key = entry.id) { isDragging ->
                ReorderableRow(
                    title = entry.name,
                    caption = null,
                    onClick = null,
                    onClickLabel = null,
                    canMoveUp = index > 0,
                    canMoveDown = index < current.lastIndex,
                    onMove = { offset -> current.map { it.id }.moved(index, offset)?.let(onReorder) },
                    onDragStopped = { onReorder(order.value.map { it.id }) },
                    isDragging = isDragging,
                    // 대화상자 바탕이 `surface`라 끄는 행은 `bg`로 띄운다.
                    containerColor = colors.surface,
                    draggingColor = colors.bg,
                    modifier = Modifier.testTag(reorderRowTag(entry.id)),
                )
            }
        }
    }
}

internal const val REORDER_LIST_TAG = "reorderList"

internal fun reorderRowTag(workoutExerciseId: Long) = "reorderRow-$workoutExerciseId"
