package io.github.zzunsik.nextset.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.component.TextInputField
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

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
        text = { Text(text = text, style = NextSetTheme.typography.body, color = colors.text) },
        confirmButton = {
            DialogButton(confirm, if (destructive) colors.error else colors.accent, onConfirm)
        },
        dismissButton = { DialogButton(dismiss, colors.accent, onDismissButton) },
    )
}

@Composable
private fun DialogButton(text: String, color: Color, onClick: () -> Unit) {
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

/** 종목 순서 바꾸기. 종목 이름만 나열하고 위로, 아래로 버튼으로 옮긴다. 옮기면 바로 저장한다. */
@Composable
private fun ReorderDialog(state: WorkoutUiState, actions: WorkoutActions) {
    val colors = NextSetTheme.colors
    AlertDialog(
        onDismissRequest = actions.onDismissDialog,
        containerColor = colors.surface,
        title = {
            Text(text = stringResource(R.string.workout_reorder), style = NextSetTheme.typography.heading, color = colors.text)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                state.exercises.forEachIndexed { index, block ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.xs),
                    ) {
                        Text(
                            text = block.name,
                            style = NextSetTheme.typography.body,
                            color = colors.text,
                            modifier = Modifier.weight(1f),
                        )
                        MoveButton(
                            icon = R.drawable.ic_keyboard_arrow_up,
                            description = stringResource(R.string.reorder_move_up, block.name),
                            enabled = index > 0,
                            onClick = { actions.onMoveExercise(block.id, -1) },
                        )
                        MoveButton(
                            icon = R.drawable.ic_keyboard_arrow_down,
                            description = stringResource(R.string.reorder_move_down, block.name),
                            enabled = index < state.exercises.lastIndex,
                            onClick = { actions.onMoveExercise(block.id, 1) },
                        )
                    }
                }
            }
        },
        confirmButton = { DialogButton(stringResource(R.string.action_close), colors.accent, actions.onDismissDialog) },
    )
}

@Composable
private fun MoveButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(NextSetTheme.sizes.minTouchTarget)) {
        Icon(painter = painterResource(icon), contentDescription = description)
    }
}
