package io.github.zzunsik.nextset.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.Routine
import io.github.zzunsik.nextset.ui.component.PrimaryButton
import io.github.zzunsik.nextset.ui.component.ScreenTopBar
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** 목록의 테스트 태그. 테스트가 행까지 내려 볼 때 쓴다. */
const val ROUTINE_LIST_TAG = "routineList"

@Composable
fun RoutineListRoute(
    viewModel: RoutineListViewModel,
    onBack: () -> Unit,
    onOpenRoutine: (routineId: Long, isNew: Boolean) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RoutineListScreen(
        state = state,
        onBack = onBack,
        onNewRoutine = { viewModel.createRoutine { id -> onOpenRoutine(id, true) } },
        onOpenRoutine = { onOpenRoutine(it, false) },
        onReorder = viewModel::reorder,
    )
}

/**
 * 루틴 목록. 홈(#23)이 생기기 전까지 쓰는 임시 화면이다. 홈의 "순서 편집" 상태와 같게 그린다
 * (docs/DESIGN.md `RoutineRow`의 순서 편집: 손잡이로 순서를 바꾸고 행을 누르면 루틴 편집을 연다, RTN-05).
 * 루틴이 없으면 홈의 빈 상태처럼 "아직 루틴이 없어요."와 "루틴 만들기"를 보인다.
 */
@Composable
fun RoutineListScreen(
    state: RoutineListUiState,
    onBack: () -> Unit,
    onNewRoutine: () -> Unit,
    onOpenRoutine: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = { ScreenTopBar(title = stringResource(R.string.routine_list_title), onBack = onBack) },
    ) { innerPadding ->
        if (state.loading) return@Scaffold
        if (state.routines.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.routine_list_empty),
                    style = NextSetTheme.typography.body,
                    color = colors.text2,
                    textAlign = TextAlign.Center,
                )
                PrimaryButton(text = stringResource(R.string.routine_create), onClick = onNewRoutine)
            }
            return@Scaffold
        }
        val order = rememberDragOrder(state.routines)
        val listState = rememberLazyListState()
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            moveByKey(order, Routine::id, from, to)
        }
        val editLabel = stringResource(R.string.routine_edit_action)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .testTag(ROUTINE_LIST_TAG),
        ) {
            val routines = order.value
            items(routines, key = Routine::id) { routine ->
                val index = routines.indexOf(routine)
                ReorderableItem(reorderState, key = routine.id) { isDragging ->
                    ReorderableRow(
                        title = routine.name,
                        caption = null,
                        onClick = { onOpenRoutine(routine.id) },
                        onClickLabel = editLabel,
                        canMoveUp = index > 0,
                        canMoveDown = index < routines.lastIndex,
                        onMove = { offset -> routines.map { it.id }.moved(index, offset)?.let(onReorder) },
                        onDragStopped = { onReorder(order.value.map { it.id }) },
                        isDragging = isDragging,
                    )
                }
            }
            item(key = "new") {
                TextButton(
                    onClick = onNewRoutine,
                    modifier = Modifier
                        .padding(horizontal = spacing.sm, vertical = spacing.sm)
                        .heightIn(min = NextSetTheme.sizes.buttonHeight),
                    shape = NextSetTheme.radius.md,
                ) {
                    Text(text = stringResource(R.string.routine_new), style = NextSetTheme.typography.label)
                }
            }
        }
    }
}
