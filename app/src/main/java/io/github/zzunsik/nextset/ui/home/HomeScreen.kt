package io.github.zzunsik.nextset.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.component.PrimaryButton
import io.github.zzunsik.nextset.ui.routine.ReorderableRow
import io.github.zzunsik.nextset.ui.routine.moveByKey
import io.github.zzunsik.nextset.ui.routine.moved
import io.github.zzunsik.nextset.ui.routine.rememberDragOrder
import io.github.zzunsik.nextset.ui.settings.ScreenTitleBar
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** 홈 목록의 테스트 태그. 테스트가 아래 항목까지 내려 볼 때 쓴다. */
const val HOME_LIST_TAG = "homeList"

/** 홈에서 일어나는 일. 화면 상태([HomeViewModel])와 화면 이동이 나눠 맡는다. */
data class HomeActions(
    val onStartRoutine: (Long) -> Unit = {},
    val onStartEmpty: () -> Unit = {},
    val onResume: (Long) -> Unit = {},
    val onPin: (Long) -> Unit = {},
    val onUnpin: () -> Unit = {},
    val onEditRoutine: (Long) -> Unit = {},
    val onDeleteRequest: (Long) -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onNewRoutine: () -> Unit = {},
    val onReorder: (List<Long>) -> Unit = {},
    val onPromptResume: () -> Unit = {},
    val onPromptFinishAndStart: () -> Unit = {},
    val onDismissDialog: () -> Unit = {},
)

/**
 * [HomeViewModel]의 상태로 [HomeScreen]을 그린다. 시작한 운동은 [onOpenWorkout]으로,
 * "새 루틴"으로 만든 루틴과 메뉴의 "편집"은 [onOpenRoutine]으로 연다.
 */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onOpenWorkout: (Long) -> Unit,
    onOpenRoutine: (routineId: Long, isNew: Boolean) -> Unit,
    now: Instant = Instant.now(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnOpen by rememberUpdatedState(onOpenWorkout)
    LaunchedEffect(viewModel) {
        // 홈이 화면에 들어올 때마다(다른 화면에서 돌아올 때도) 다시 그려지며 여기를 지난다. 떠나는 동작을 다시 받는다(#103).
        viewModel.onShown()
        viewModel.openWorkout.collect { currentOnOpen(it) }
    }
    HomeScreen(
        state = state,
        now = now,
        today = now.atZone(ZoneId.systemDefault()).toLocalDate(),
        actions = HomeActions(
            onStartRoutine = viewModel::onStartRoutine,
            onStartEmpty = viewModel::onStartEmpty,
            onResume = viewModel::onResume,
            onPin = viewModel::onPin,
            onUnpin = viewModel::onUnpin,
            onEditRoutine = { onOpenRoutine(it, false) },
            onDeleteRequest = viewModel::onDeleteRequest,
            onDeleteConfirm = viewModel::onDeleteConfirm,
            onNewRoutine = { viewModel.createRoutine { id -> onOpenRoutine(id, true) } },
            onReorder = viewModel::reorder,
            onPromptResume = viewModel::onPromptResume,
            onPromptFinishAndStart = viewModel::onPromptFinishAndStart,
            onDismissDialog = viewModel::dismissDialog,
        ),
    )
}

/**
 * 홈(docs/DESIGN.md "홈"). 위에서부터 상단 바, 맨 위 카드(다음 차례 또는 이어서 하기), "다른 루틴"과 "순서 편집",
 * 루틴 행, "새 루틴", "빈 운동 시작". 하단 내비게이션은 화면 이동(`NextSetNavDisplay`)이 붙인다.
 *
 * - 루틴이 없으면 카드와 목록 자리에 "아직 루틴이 없어요."와 "루틴 만들기"를 보인다.
 * - 맨 위 카드 아래에 남은 루틴이 없으면(루틴이 하나뿐이면) "다른 루틴"과 행을 보이지 않는다.
 * - "순서 편집"을 누르면 목록이 맨 위 카드의 루틴까지 넣은 전체 목록이 되고 행이 순서 편집 상태가 된다(RTN-05).
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    now: Instant,
    today: LocalDate,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    initialReordering: Boolean = false,
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    var reordering by rememberSaveable { mutableStateOf(initialReordering) }
    // 루틴이 둘보다 적어지면 순서 편집을 끝낸다.
    if (reordering && state.routines.size < 2) reordering = false
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = { ScreenTitleBar(title = stringResource(R.string.app_name), modifier = Modifier.statusBarsPadding()) },
    ) { innerPadding ->
        if (state.loading) return@Scaffold
        val order = rememberDragOrder(state.routines)
        val listState = rememberLazyListState()
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            moveByKey(order, { it.routine.id }, from, to)
        }
        val menuLabels = MenuLabels(
            pin = stringResource(R.string.home_pin),
            edit = stringResource(R.string.home_edit),
            delete = stringResource(R.string.action_delete),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .testTag(HOME_LIST_TAG),
        ) {
            item(key = "top") {
                TopItem(state, now, today, actions, menuLabels)
            }
            val showList = reordering || state.others.isNotEmpty()
            if (showList) {
                item(key = "header") {
                    ListHeader(
                        reordering = reordering,
                        canReorder = state.routines.size >= 2,
                        onToggle = { reordering = !reordering },
                    )
                }
            }
            if (reordering) {
                val routines = order.value
                items(routines, key = { it.routine.id }) { routine ->
                    val index = routines.indexOf(routine)
                    ReorderableItem(reorderState, key = routine.routine.id) { isDragging ->
                        ReorderableRow(
                            title = routine.routine.name,
                            caption = null,
                            onClick = { actions.onEditRoutine(routine.routine.id) },
                            onClickLabel = stringResource(R.string.routine_edit_action),
                            canMoveUp = index > 0,
                            canMoveDown = index < routines.lastIndex,
                            onMove = { offset ->
                                routines.map { it.routine.id }.moved(index, offset)?.let(actions.onReorder)
                            },
                            onDragStopped = { actions.onReorder(order.value.map { it.routine.id }) },
                            isDragging = isDragging,
                            trailing = {
                                IconButton(
                                    onClick = { actions.onDeleteRequest(routine.routine.id) },
                                    modifier = Modifier.size(NextSetTheme.sizes.minTouchTarget),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = stringResource(R.string.home_routine_delete, routine.routine.name),
                                        tint = colors.text2,
                                    )
                                }
                            },
                        )
                    }
                }
            } else if (showList) {
                items(state.others, key = { it.routine.id }) { routine ->
                    Column {
                        RoutineRow(
                            routine = routine,
                            today = today,
                            onStart = { actions.onStartRoutine(routine.routine.id) },
                            menu = rowMenu(routine.routine.id, actions, menuLabels),
                        )
                        HorizontalDivider(color = colors.line)
                    }
                }
            }
            item(key = "buttons") {
                Column(modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.sm)) {
                    if (state.routines.isNotEmpty()) {
                        HomeTextButton(text = stringResource(R.string.routine_new), onClick = actions.onNewRoutine)
                    }
                    HomeTextButton(text = stringResource(R.string.home_start_empty), onClick = actions.onStartEmpty)
                }
            }
        }
    }
    HomeDialogs(state.dialog, actions)
}

private data class MenuLabels(val pin: String, val edit: String, val delete: String)

private fun rowMenu(routineId: Long, actions: HomeActions, labels: MenuLabels) = listOf(
    MenuAction(labels.pin) { actions.onPin(routineId) },
    MenuAction(labels.edit) { actions.onEditRoutine(routineId) },
    MenuAction(labels.delete) { actions.onDeleteRequest(routineId) },
)

/** 맨 위 자리. 이어서 하기 카드, 다음 차례 카드, 빈 상태 가운데 하나다. */
@Composable
private fun TopItem(state: HomeUiState, now: Instant, today: LocalDate, actions: HomeActions, labels: MenuLabels) {
    val spacing = NextSetTheme.spacing
    val cardModifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = spacing.xl)
    val inProgress = state.inProgress
    val top = state.top
    when {
        inProgress != null -> ResumeCard(
            workout = inProgress,
            now = now,
            onResume = { actions.onResume(inProgress.workoutId) },
            modifier = cardModifier,
        )
        top != null -> NextRoutineCard(
            routine = top,
            pinned = state.topPinned,
            today = today,
            onStart = { actions.onStartRoutine(top.routine.id) },
            onUnpin = actions.onUnpin,
            menu = listOf(
                MenuAction(labels.edit) { actions.onEditRoutine(top.routine.id) },
                MenuAction(labels.delete) { actions.onDeleteRequest(top.routine.id) },
            ),
            modifier = cardModifier,
        )
        else -> EmptyState(onCreate = actions.onNewRoutine)
    }
}

/** 루틴이 없을 때. 한 문장과 버튼 하나(docs/DESIGN.md "문구"의 빈 상태). */
@Composable
private fun EmptyState(onCreate: () -> Unit) {
    val spacing = NextSetTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_empty),
            style = NextSetTheme.typography.body,
            color = NextSetTheme.colors.text2,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(text = stringResource(R.string.routine_create), onClick = onCreate)
    }
}

/** 구역 머리 "다른 루틴"과 "순서 편집"(편집 중에는 "완료"). */
@Composable
private fun ListHeader(reordering: Boolean, canReorder: Boolean, onToggle: () -> Unit) {
    val spacing = NextSetTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.lg, end = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_other_routines),
            style = NextSetTheme.typography.heading,
            color = NextSetTheme.colors.text,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (canReorder) {
            HomeTextButton(
                text = stringResource(if (reordering) R.string.home_reorder_done else R.string.home_reorder),
                onClick = onToggle,
            )
        } else {
            // 버튼이 없어도 머리 높이를 같게 둔다.
            Spacer(Modifier.height(NextSetTheme.sizes.minTouchTarget))
        }
    }
}

@Composable
private fun HomeTextButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight),
        shape = NextSetTheme.radius.md,
    ) {
        Text(text = text, style = NextSetTheme.typography.label)
    }
}

/**
 * 홈의 대화상자. "진행 중인 운동이 있어요."는 docs/DESIGN.md가 하단 시트로 정했지만 `ModalBottomSheet`이
 * Material 3 안정판에서 실험 API라(NF-07) 대화상자로 묻는다. 설정과 루틴 편집이 시트 대신 화면이나 대화상자를 쓴 것과 같다.
 */
@Composable
private fun HomeDialogs(dialog: HomeDialog?, actions: HomeActions) {
    val colors = NextSetTheme.colors
    when (dialog) {
        null -> Unit
        is HomeDialog.InProgress -> AlertDialog(
            onDismissRequest = actions.onDismissDialog,
            title = { InProgressPromptTitle() },
            text = { InProgressPromptText(dialog.workout) },
            confirmButton = { InProgressPromptButtons(actions) },
            containerColor = colors.surface,
            titleContentColor = colors.text,
            textContentColor = colors.text,
        )
        is HomeDialog.DeleteRoutine -> AlertDialog(
            onDismissRequest = actions.onDismissDialog,
            text = { Text(text = stringResource(R.string.routine_delete_confirm), style = NextSetTheme.typography.body) },
            confirmButton = {
                TextButton(
                    onClick = actions.onDeleteConfirm,
                    modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight),
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                ) {
                    Text(text = stringResource(R.string.action_delete), style = NextSetTheme.typography.label)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = actions.onDismissDialog,
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

@Composable
internal fun InProgressPromptTitle() {
    Text(text = stringResource(R.string.home_in_progress_title), style = NextSetTheme.typography.heading)
}

/** "가슴 루틴 · 5세트 완료". */
@Composable
internal fun InProgressPromptText(workout: InProgressWorkoutUi) {
    Text(
        text = stringResource(
            R.string.home_in_progress_detail,
            workout.routineName ?: stringResource(R.string.workout_empty_title),
            workout.completedSets,
        ),
        style = NextSetTheme.typography.body,
    )
}

/** 선택 셋을 세로로 놓는다. 채운 버튼이 "이어서 하기"다. */
@Composable
internal fun InProgressPromptButtons(actions: HomeActions) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.xs),
    ) {
        PrimaryButton(text = stringResource(R.string.home_resume), onClick = actions.onPromptResume)
        DialogTextButton(stringResource(R.string.home_finish_and_start), actions.onPromptFinishAndStart)
        DialogTextButton(stringResource(R.string.action_cancel), actions.onDismissDialog)
    }
}

@Composable
private fun DialogTextButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NextSetTheme.sizes.buttonHeight),
        shape = NextSetTheme.radius.md,
    ) {
        Text(text = text, style = NextSetTheme.typography.label, textAlign = TextAlign.Center)
    }
}
