package io.github.zzunsik.nextset.ui.routine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.RoutineExercise
import io.github.zzunsik.nextset.ui.component.ScreenTopBar
import io.github.zzunsik.nextset.ui.component.TextInputField
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** 루틴 편집 목록의 테스트 태그. */
const val ROUTINE_EDIT_LIST_TAG = "routineEditList"

/**
 * 루틴 편집(docs/DESIGN.md "루틴 편집"). 이름 입력 칸, 종목 목록 행(순서 바꾸기 손잡이), "종목 추가", "루틴 지우기".
 * 저장 버튼이 없다. 고친 내용은 바로 저장된다(NF-09).
 *
 * @param onAddExercise 종목 고르기를 고르기로 연다. 고른 종목은 [RoutineEditViewModel.addExercises]로 들어온다.
 * @param onOpenExercise 종목 행을 눌러 종목 설정을 연다.
 */
@Composable
fun RoutineEditScreen(
    state: RoutineEditUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onAddExercise: () -> Unit,
    onOpenExercise: (RoutineExercise) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = colors.bg,
        topBar = {
            ScreenTopBar(
                title = stringResource(if (state.isNew) R.string.routine_new else R.string.routine_edit_title),
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (state.loading || state.missing) return@Scaffold
        val order = rememberDragOrder(state.exercises)
        val listState = rememberLazyListState()
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            moveByKey(order, RoutineExercise::id, from, to)
        }
        val settingsLabel = stringResource(R.string.routine_exercise_settings_action)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .testTag(ROUTINE_EDIT_LIST_TAG),
        ) {
            item(key = "name") {
                Column(
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    SectionTitle(stringResource(R.string.routine_name))
                    TextInputField(
                        value = state.name,
                        onValueChange = onNameChange,
                        placeholder = stringResource(R.string.routine_name_hint),
                    )
                }
            }
            item(key = "exercisesTitle") {
                SectionTitle(
                    text = stringResource(R.string.routine_exercises),
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.xl, bottom = spacing.sm),
                )
            }
            val exercises = order.value
            if (exercises.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.routine_no_exercises),
                        style = NextSetTheme.typography.body,
                        color = colors.text2,
                        modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
                    )
                }
            }
            items(exercises, key = RoutineExercise::id) { item ->
                val index = exercises.indexOf(item)
                ReorderableItem(reorderState, key = item.id) { isDragging ->
                    ReorderableRow(
                        title = item.exercise.nameKo,
                        caption = item.summaryText(),
                        onClick = { onOpenExercise(item) },
                        onClickLabel = settingsLabel,
                        canMoveUp = index > 0,
                        canMoveDown = index < exercises.lastIndex,
                        onMove = { offset -> exercises.map { it.id }.moved(index, offset)?.let(onReorder) },
                        onDragStopped = { onReorder(order.value.map { it.id }) },
                        isDragging = isDragging,
                    )
                }
            }
            item(key = "add") {
                OutlinedButton(
                    onClick = onAddExercise,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.lg, vertical = spacing.sm)
                        .heightIn(min = NextSetTheme.sizes.buttonHeight),
                    shape = NextSetTheme.radius.md,
                    border = BorderStroke(1.dp, colors.text2),
                ) {
                    Text(text = stringResource(R.string.routine_add_exercise), style = NextSetTheme.typography.label)
                }
            }
            if (!state.isNew) {
                item(key = "delete") {
                    TextButton(
                        onClick = { confirmingDelete = true },
                        modifier = Modifier
                            .padding(horizontal = spacing.sm, vertical = spacing.xl)
                            .heightIn(min = NextSetTheme.sizes.buttonHeight),
                        shape = NextSetTheme.radius.md,
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                    ) {
                        Text(text = stringResource(R.string.routine_delete), style = NextSetTheme.typography.label)
                    }
                }
            }
        }
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            text = { Text(text = stringResource(R.string.routine_delete_confirm), style = NextSetTheme.typography.body) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        onDelete()
                    },
                    modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight),
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                ) {
                    Text(text = stringResource(R.string.action_delete), style = NextSetTheme.typography.label)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmingDelete = false },
                    modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight),
                ) {
                    Text(text = stringResource(R.string.action_cancel), style = NextSetTheme.typography.label)
                }
            },
            containerColor = colors.surface,
            textContentColor = colors.text,
        )
    }
}

/** 구역 제목(`label`, `text2`). 화면 낭독기에는 제목으로 알린다. */
@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = NextSetTheme.typography.label,
        color = NextSetTheme.colors.text2,
        modifier = modifier
            .fillMaxWidth()
            .semantics { heading() },
    )
}
