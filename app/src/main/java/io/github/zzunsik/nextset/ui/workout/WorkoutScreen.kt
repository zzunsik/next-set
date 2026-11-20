package io.github.zzunsik.nextset.ui.workout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.flow.collectLatest
import io.github.zzunsik.nextset.ui.component.LocalNumberPadOverlap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.ui.component.DoneCheckButton
import io.github.zzunsik.nextset.ui.component.NumberEntry
import io.github.zzunsik.nextset.ui.component.NumberFieldKind
import io.github.zzunsik.nextset.ui.component.NumberInputField
import io.github.zzunsik.nextset.ui.component.NumberPadKey
import io.github.zzunsik.nextset.ui.component.NumberPadScaffold
import io.github.zzunsik.nextset.ui.component.PrimaryButton
import io.github.zzunsik.nextset.ui.component.RecordBadgeIcon
import io.github.zzunsik.nextset.ui.component.numberFieldKinds
import io.github.zzunsik.nextset.ui.component.padSpec
import io.github.zzunsik.nextset.ui.theme.NextSetMotion
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import java.time.Instant

/** 운동 중 화면이 부르는 일. 테스트와 미리보기는 필요한 것만 채운다. */
data class WorkoutActions(
    val onBack: () -> Unit = {},
    val onFinish: () -> Unit = {},
    val onWorkoutNote: () -> Unit = {},
    val onReorder: () -> Unit = {},
    val onDiscard: () -> Unit = {},
    val onAddExercise: () -> Unit = {},
    val onFieldClick: (setId: Long, kind: NumberFieldKind) -> Unit = { _, _ -> },
    val onNumberPadKey: (NumberPadKey) -> Unit = {},
    val onCheck: (setId: Long) -> Unit = {},
    val onAddSet: (workoutExerciseId: Long) -> Unit = {},
    val onDeleteSet: (setId: Long) -> Unit = {},
    val onDismissDialog: () -> Unit = {},
    val onDiscardConfirm: () -> Unit = {},
    val onIncompleteConfirm: () -> Unit = {},
    val onRoutineChangeAnswer: (apply: Boolean) -> Unit = {},
    val onRemoveExerciseConfirm: (workoutExerciseId: Long) -> Unit = {},
    val onExerciseNoteChange: (workoutExerciseId: Long, note: String) -> Unit = { _, _ -> },
    val onWorkoutNoteChange: (note: String) -> Unit = {},
    val onDeleteLastSet: (workoutExerciseId: Long) -> Unit = {},
    val onExerciseNote: (workoutExerciseId: Long) -> Unit = {},
    val onMoveExercise: (workoutExerciseId: Long, delta: Int) -> Unit = { _, _ -> },
    val onReorderExercises: (orderedIds: List<Long>) -> Unit = {},
    val onRemoveExercise: (workoutExerciseId: Long) -> Unit = {},
    val onRestTime: (workoutExerciseId: Long) -> Unit = {},
    val onRestTimeChange: (exerciseId: String, seconds: Int?) -> Unit = { _, _ -> },
)

const val WORKOUT_LIST_TAG = "workoutList"
const val NEXT_SET_TAG = "nextSet"

/** 목록에서 세트 행의 자리. 종목 블록마다 머리 하나, 세트 행들, "세트 추가" 하나가 이어진다. */
internal fun WorkoutUiState.listIndexOf(setId: Long): Int? {
    var index = 0
    exercises.forEach { block ->
        index += 1
        val position = block.sets.indexOfFirst { it.id == setId }
        if (position >= 0) return index + position
        index += block.sets.size + 1
    }
    return null
}

/**
 * 운동 중 화면(docs/DESIGN.md "운동 중"). 위에서부터 상단 바, 다음 세트 표시(고정), 종목 블록 목록, "종목 추가".
 * 아래에는 휴식 타이머 바 자리([restTimerBar], #26)와 자체 숫자판이 붙는다.
 *
 * 화면이 보이는 동안 화면이 저절로 꺼지지 않는다(LOG-12).
 *
 * @param now 경과 시간을 셀 지금 시각. 화면 밖에서 흘려 준다.
 */
@Composable
fun WorkoutScreen(
    state: WorkoutUiState,
    now: Instant,
    actions: WorkoutActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    listState: LazyListState = rememberLazyListState(),
    restTimerBar: @Composable () -> Unit = {},
) {
    val colors = NextSetTheme.colors
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        WorkoutTopBar(state = state, now = now, actions = actions)
        if (state.loading) return@Column
        if (state.exercises.isNotEmpty()) {
            NextSetDisplay(
                state = state,
                collapsed = state.numberPad != null,
                onShow = { setId -> state.listIndexOf(setId)?.let { index -> scope.launch { listState.animateScrollToItem(index) } } },
                onEnterValue = { next ->
                    val block = state.exercises.first { it.id == next.workoutExerciseId }
                    val row = block.sets.first { it.id == next.setId }
                    val kind = block.recordType.numberFieldKinds().first { row.value.amountOf(it) == null }
                    actions.onFieldClick(next.setId, kind)
                },
                onFinish = actions.onFinish,
            )
            HorizontalDivider(color = colors.line)
        }
        NumberPadScaffold(
            numberPad = state.numberPad.padSpec,
            onNumberPadKey = actions.onNumberPadKey,
            modifier = Modifier.weight(1f),
            bottomBar = restTimerBar,
        ) { padding ->
            val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val bottom = maxOf(padding.calculateBottomPadding(), navigationBar)
            // 목록은 숫자판과 하단 바 위에서 끝난다(contentPadding이 아니라 바깥 여백). 목록 행이 숫자판 밑에 깔리면
            // 가려진 칸과 체크 버튼이 접근성 검사에 잘린 크기로 잡히기 때문이다. 고른 칸이 가려지지 않게 하는 일은
            // 숫자판 높이를 보는 입력 칸 대신 [KeepSelectedRowVisible]이 한다.
            Box(Modifier.fillMaxSize().padding(bottom = bottom)) {
                if (state.exercises.isEmpty()) {
                    EmptyWorkout(onAddExercise = actions.onAddExercise)
                } else {
                    CompositionLocalProvider(LocalNumberPadOverlap provides null) {
                        ExerciseList(
                            state = state,
                            actions = actions,
                            listState = listState,
                            contentPadding = PaddingValues(bottom = NextSetTheme.spacing.lg),
                        )
                    }
                    KeepSelectedRowVisible(state, listState)
                }
                state.dialog?.let { WorkoutDialogs(it, state, actions) }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/**
 * 숫자판이 열린 동안 고른 칸의 행이 목록 안에 보이게 한다. 숫자판이 올라오며 목록이 줄어들 때마다 다시 맞춘다.
 */
@Composable
private fun KeepSelectedRowVisible(state: WorkoutUiState, listState: LazyListState) {
    val setId = state.numberPad?.focused?.id?.setId ?: return
    val index = state.listIndexOf(setId) ?: return
    LaunchedEffect(setId, index) {
        snapshotFlow { listState.layoutInfo.viewportEndOffset }.collectLatest { end ->
            val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            when {
                item == null -> listState.scrollToItem(index)
                item.offset + item.size > end -> listState.scrollBy((item.offset + item.size - end).toFloat())
                item.offset < 0 -> listState.scrollBy(item.offset.toFloat())
            }
        }
    }
}

@Composable
private fun WorkoutTopBar(state: WorkoutUiState, now: Instant, actions: WorkoutActions) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        IconButton(onClick = actions.onBack, modifier = Modifier.size(NextSetTheme.sizes.minTouchTarget)) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.navigate_back),
                tint = colors.text,
            )
        }
        Column(modifier = Modifier.weight(1f).padding(vertical = spacing.xs)) {
            Text(
                text = state.routineName ?: stringResource(R.string.workout_empty_title),
                style = NextSetTheme.typography.heading,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (!state.loading) {
                Text(
                    text = elapsedText(state.startedAt, now),
                    style = NextSetTheme.typography.caption,
                    color = colors.text2,
                )
            }
        }
        TextButton(
            onClick = actions.onFinish,
            enabled = !state.loading,
            modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight),
            shape = NextSetTheme.radius.md,
        ) {
            Text(text = stringResource(R.string.workout_finish), style = NextSetTheme.typography.label)
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(NextSetTheme.sizes.minTouchTarget)) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = stringResource(R.string.workout_more),
                    tint = colors.text,
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                MenuItem(R.string.workout_note) { menu = false; actions.onWorkoutNote() }
                if (state.exercises.size > 1) {
                    MenuItem(R.string.workout_reorder) { menu = false; actions.onReorder() }
                }
                MenuItem(R.string.workout_discard, destructive = true) { menu = false; actions.onDiscard() }
            }
        }
    }
}

@Composable
private fun MenuItem(text: Int, destructive: Boolean = false, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(text),
                style = NextSetTheme.typography.body,
                color = if (destructive) NextSetTheme.colors.error else NextSetTheme.colors.text,
            )
        },
        onClick = onClick,
    )
}

/**
 * 다음 세트 표시(docs/DESIGN.md `NextSetDisplay`, LOG-06). 상단 바 아래에 고정한다. 숫자는 `numeralLarge`로
 * 화면에서 가장 크다. 숫자판이 열리면 한 줄로 줄어든다. 모두 완료하면 "운동 끝내기"를 내세운다.
 */
@Composable
private fun NextSetDisplay(
    state: WorkoutUiState,
    collapsed: Boolean,
    onShow: (setId: Long) -> Unit,
    onEnterValue: (NextSetUi) -> Unit,
    onFinish: () -> Unit,
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    val next = state.nextSet
    Box(
        modifier = Modifier
            .testTag(NEXT_SET_TAG)
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
    ) {
        when {
            state.allDone -> Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    text = stringResource(R.string.next_set_all_done),
                    style = NextSetTheme.typography.heading,
                    color = colors.text,
                )
                PrimaryButton(text = stringResource(R.string.next_set_finish), onClick = onFinish)
            }
            next == null -> Unit
            collapsed -> Text(
                text = stringResource(
                    R.string.next_set_collapsed,
                    next.exerciseName,
                    shortSetText(next.recordType, next.value).orEmpty(),
                ),
                style = NextSetTheme.typography.heading,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            else -> ExpandedNextSet(next, onShow = { onShow(next.setId) }, onEnterValue = { onEnterValue(next) })
        }
    }
}

@Composable
private fun ExpandedNextSet(next: NextSetUi, onShow: () -> Unit, onEnterValue: () -> Unit) {
    val colors = NextSetTheme.colors
    val typography = NextSetTheme.typography
    val filled = next.recordType.numberFieldKinds().all { next.value.amountOf(it) != null }
    val valueText = shortSetText(next.recordType, next.value).orEmpty()
    val label = stringResource(R.string.next_set_label, next.setCount, next.setNumber)
    val previous = next.previous?.let { shortSetText(next.recordType, it) }
    val previousText = previous?.let { stringResource(R.string.next_set_previous, it) }
    val description = stringResource(R.string.next_set_description, label, next.exerciseName, valueText) +
        (previousText?.let { ", $it" } ?: "")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onShow)
            .then(if (filled) Modifier.clearAndSetSemantics { contentDescription = description } else Modifier),
    ) {
        Text(
            text = label,
            style = typography.caption,
            color = colors.text2,
        )
        Text(
            text = next.exerciseName,
            style = typography.heading,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // 옛 값이 사라지고 새 값이 나타난다(docs/DESIGN.md "모션": effects 기본).
        AnimatedContent(
            targetState = next.value to filled,
            transitionSpec = {
                fadeIn(NextSetMotion.effectsDefault.spec()) togetherWith fadeOut(NextSetMotion.effectsDefault.spec())
            },
            label = "nextSetValue",
        ) { (value, isFilled) ->
            if (isFilled) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.xs)) {
                    when (next.recordType) {
                        RecordType.WEIGHT_REPS -> {
                            BigNumber(value.weight!!.toKgText(), Modifier.weight(1f, fill = false))
                            UnitText(stringResource(R.string.unit_kg))
                            UnitText(stringResource(R.string.unit_times))
                            BigNumber(value.reps.toString(), Modifier.weight(1f, fill = false))
                            UnitText(stringResource(R.string.unit_reps))
                        }
                        RecordType.REPS -> {
                            BigNumber(value.reps.toString(), Modifier.weight(1f, fill = false))
                            UnitText(stringResource(R.string.unit_reps))
                        }
                        RecordType.DURATION -> BigNumber(durationText(value.durationSeconds!!), Modifier.weight(1f, fill = false))
                    }
                }
            } else {
                TextButton(
                    onClick = onEnterValue,
                    modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight),
                    shape = NextSetTheme.radius.md,
                ) {
                    Text(text = stringResource(R.string.next_set_enter_value), style = typography.label)
                }
            }
        }
        if (previousText != null) {
            Text(
                text = previousText,
                style = typography.caption,
                color = colors.text2,
            )
        }
    }
}

/** `numeralLarge`. 한 줄에 들어가지 않으면 28sp까지 줄인다(docs/DESIGN.md "타이포"). */
@Composable
private fun RowScope.BigNumber(text: String, modifier: Modifier = Modifier) {
    val style = NextSetTheme.typography.numeralLarge
    Text(
        text = text,
        style = style,
        color = NextSetTheme.colors.text,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = NUMERAL_LARGE_MIN, maxFontSize = style.fontSize),
        modifier = modifier.alignByBaseline(),
    )
}

@Composable
private fun RowScope.UnitText(text: String) {
    Text(
        text = text,
        style = NextSetTheme.typography.label,
        color = NextSetTheme.colors.text2,
        modifier = Modifier.alignByBaseline(),
    )
}

private val NUMERAL_LARGE_MIN = 28.sp

@Composable
private fun EmptyWorkout(onAddExercise: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = NextSetTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.workout_empty),
            style = NextSetTheme.typography.body,
            color = NextSetTheme.colors.text,
        )
        PrimaryButton(text = stringResource(R.string.workout_add_exercise), onClick = onAddExercise)
    }
}

@Composable
private fun ExerciseList(
    state: WorkoutUiState,
    actions: WorkoutActions,
    listState: LazyListState,
    contentPadding: PaddingValues,
) {
    val spacing = NextSetTheme.spacing
    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize().testTag(WORKOUT_LIST_TAG),
    ) {
        state.exercises.forEachIndexed { index, block ->
            item(key = "exercise-${block.id}", contentType = "header") {
                ExerciseHeader(
                    block = block,
                    defaultRestSeconds = state.defaultRestSeconds,
                    first = index == 0,
                    canMoveUp = index > 0,
                    canMoveDown = index < state.exercises.lastIndex,
                    actions = actions,
                    modifier = Modifier.animateItem(),
                )
            }
            block.sets.forEach { row ->
                item(key = "set-${row.id}", contentType = "set") {
                    SetRow(block = block, row = row, state = state, actions = actions, modifier = Modifier.animateItem())
                }
            }
            item(key = "add-${block.id}", contentType = "addSet") {
                val description = stringResource(R.string.set_add_description, block.name)
                TextButton(
                    onClick = { actions.onAddSet(block.id) },
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = spacing.sm)
                        .heightIn(min = NextSetTheme.sizes.buttonHeight)
                        .semantics { contentDescription = description },
                    shape = NextSetTheme.radius.md,
                ) {
                    Text(text = stringResource(R.string.set_add), style = NextSetTheme.typography.label)
                }
            }
        }
        item(key = "addExercise", contentType = "addExercise") {
            Column(Modifier.animateItem()) {
                HorizontalDivider(color = NextSetTheme.colors.line, modifier = Modifier.padding(top = spacing.xl))
                TextButton(
                    onClick = actions.onAddExercise,
                    modifier = Modifier
                        .padding(horizontal = spacing.sm, vertical = spacing.sm)
                        .heightIn(min = NextSetTheme.sizes.buttonHeight),
                    shape = NextSetTheme.radius.md,
                ) {
                    Text(text = stringResource(R.string.workout_add_exercise), style = NextSetTheme.typography.label)
                }
            }
        }
    }
}

/** 종목 머리(docs/DESIGN.md `ExerciseHeader`). 종목 이름, 메모, 더보기 메뉴, 세트 표 머리. */
@Composable
private fun ExerciseHeader(
    block: ExerciseBlockUi,
    defaultRestSeconds: Int,
    first: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    actions: WorkoutActions,
    modifier: Modifier = Modifier,
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    var menu by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        if (!first) {
            // 블록 사이는 24dp와 구분선(LOG-01).
            HorizontalDivider(color = colors.line, modifier = Modifier.padding(top = spacing.xl))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.lg, end = spacing.xs, top = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            // 이름과 체크 아이콘이 남는 폭을 모두 쓴다. 휴식 시간 버튼과 더보기 버튼은 제 폭만 쓴다.
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Text(
                    text = block.name,
                    style = NextSetTheme.typography.heading,
                    color = colors.text,
                    modifier = Modifier.weight(1f, fill = false).semantics { heading() },
                )
                if (block.allDone) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.exercise_header_all_done),
                        tint = colors.done,
                        modifier = Modifier.size(NextSetTheme.sizes.icon),
                    )
                }
            }
            RestTimeButton(
                seconds = block.restSecondsOr(defaultRestSeconds),
                exerciseName = block.name,
                onClick = { actions.onRestTime(block.id) },
            )
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(NextSetTheme.sizes.minTouchTarget)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.exercise_header_more, block.name),
                        tint = colors.text,
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    MenuItem(R.string.exercise_note) { menu = false; actions.onExerciseNote(block.id) }
                    if (canMoveUp) MenuItem(R.string.exercise_move_up) { menu = false; actions.onMoveExercise(block.id, -1) }
                    if (canMoveDown) MenuItem(R.string.exercise_move_down) { menu = false; actions.onMoveExercise(block.id, 1) }
                    if (block.sets.isNotEmpty()) {
                        MenuItem(R.string.exercise_delete_last_set) { menu = false; actions.onDeleteLastSet(block.id) }
                    }
                    MenuItem(R.string.exercise_remove, destructive = true) { menu = false; actions.onRemoveExercise(block.id) }
                }
            }
        }
        if (!block.note.isNullOrBlank()) {
            Text(
                text = block.note,
                style = NextSetTheme.typography.caption,
                color = colors.text2,
                modifier = Modifier.padding(horizontal = spacing.lg),
            )
        }
        SetTableHeader(block)
    }
}

@Composable
private fun SetTableHeader(block: ExerciseBlockUi) {
    val spacing = NextSetTheme.spacing
    val kinds = block.recordType.numberFieldKinds()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.Bottom,
    ) {
        HeaderCell(stringResource(R.string.set_table_set), Modifier.width(setNumberWidth()))
        HeaderCell(stringResource(R.string.set_table_previous), Modifier.weight(1f))
        kinds.forEach { kind ->
            val text = when (kind) {
                NumberFieldKind.WEIGHT -> stringResource(R.string.set_table_kg)
                NumberFieldKind.REPS -> repsRangeText(block.target)
                    ?.let { stringResource(R.string.set_table_reps_range, it) }
                    ?: stringResource(R.string.set_table_reps)
                NumberFieldKind.DURATION -> stringResource(R.string.set_table_duration)
            }
            HeaderCell(text, Modifier.width(fieldWidth(kind, kinds.size)))
        }
        Spacer(Modifier.width(NextSetTheme.sizes.minTouchTarget))
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    // 입력 칸 위의 좁은 열이라 글자가 커지면 두 줄까지 쓰고 그래도 넘치면 `caption` 크기까지 줄인다("회 8~12").
    Text(
        text = text,
        style = NextSetTheme.typography.label,
        color = NextSetTheme.colors.text2,
        maxLines = 2,
        autoSize = TextAutoSize.StepBased(
            minFontSize = NextSetTheme.typography.caption.fontSize,
            maxFontSize = NextSetTheme.typography.label.fontSize,
        ),
        modifier = modifier,
    )
}

private val SET_NUMBER_WIDTH = 32.dp

/**
 * 순번 열의 폭. 32dp이고, 글자를 키워 머리의 "세트"가 32dp에 들어가지 않으면 그 글자 폭만큼 넓힌다.
 * 남는 폭은 직전 기록 열이 내준다. 입력 칸과 체크 버튼의 폭은 그대로다.
 */
@Composable
internal fun setNumberWidth(): Dp {
    val measurer = rememberTextMeasurer()
    val style = NextSetTheme.typography.label
    val text = stringResource(R.string.set_table_set)
    val density = LocalDensity.current
    val width = remember(text, style, density) { with(density) { measurer.measure(text, style).size.width.toDp() } }
    return maxOf(SET_NUMBER_WIDTH, width)
}

/** 입력 칸의 폭(docs/DESIGN.md `SetRow`). 칸이 하나인 종목은 88dp다. */
internal fun fieldWidth(kind: NumberFieldKind, count: Int) = when {
    count == 1 -> 88.dp
    kind == NumberFieldKind.WEIGHT -> 72.dp
    else -> 64.dp
}

/** 세트 행(docs/DESIGN.md `SetRow`, LOG-02). 순번, 직전 기록, 입력 칸, 완료 체크. 길게 누르면 지우기 메뉴가 뜬다. */
@Composable
private fun SetRow(
    block: ExerciseBlockUi,
    row: SetRowUi,
    state: WorkoutUiState,
    actions: WorkoutActions,
    modifier: Modifier = Modifier,
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    val number = row.position + 1
    val kinds = block.recordType.numberFieldKinds()
    val pad = state.numberPad
    var menu by remember { mutableStateOf(false) }
    // 세트 체크는 색만 바뀐다(docs/DESIGN.md "모션": effects 빠름).
    val background by animateColorAsState(
        targetValue = if (row.completed) colors.doneSoft else colors.bg,
        animationSpec = NextSetMotion.effectsFast.spec(),
        label = "setRowBackground",
    )
    val deleteLabel = stringResource(R.string.set_delete)
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .pointerInput(row.id) { detectTapGestures(onLongPress = { menu = true }) }
                .semantics {
                    customActions = listOf(CustomAccessibilityAction(deleteLabel) { actions.onDeleteSet(row.id); true })
                }
                .heightIn(min = NextSetTheme.sizes.setRowMinHeight)
                // 위아래 여백을 두지 않는다. 입력 칸(누르는 영역 48dp)이 행 높이를 채워, 숫자판이 올라와 칸을 밀어 올렸을 때
                // 아래 행이 숫자판 윗변에 걸치지 않는다.
                .padding(horizontal = spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val numberDescription = stringResource(R.string.set_number_description, number)
            Column(modifier = Modifier.width(setNumberWidth()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$number",
                    style = NextSetTheme.typography.body.copy(
                        fontWeight = if (row.isNext) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = colors.text,
                    modifier = Modifier.semantics { contentDescription = numberDescription },
                )
                RecordBadge(visible = row.isRecord)
            }
            // 직전 기록이 없으면 빈칸이다(원칙 2). 빈 글자 요소는 화면 읽기 프로그램에 이름 없는 요소로 잡혀 두지 않는다.
            val previous = row.previous?.let { shortSetText(block.recordType, it) }
            if (previous != null) {
                Text(
                    text = previous,
                    style = NextSetTheme.typography.caption,
                    color = colors.text2,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            kinds.forEach { kind ->
                val selected = pad?.focused?.id == SetFieldId(row.id, kind)
                val fieldName = stringResource(
                    when (kind) {
                        NumberFieldKind.WEIGHT -> R.string.number_field_weight
                        NumberFieldKind.REPS -> R.string.number_field_reps
                        NumberFieldKind.DURATION -> R.string.number_field_duration
                    },
                )
                NumberInputField(
                    value = if (selected) pad.entry.displayText else NumberEntry.displayTextOf(kind, row.value.amountOf(kind)),
                    label = stringResource(R.string.set_field_label, block.name, number, fieldName),
                    selected = selected,
                    onClick = { actions.onFieldClick(row.id, kind) },
                    // 미리 채운 값은 직접 입력한 값과 구분해 `text2`로 쓴다(LOG-03). 완료한 행은 모두 `text`다.
                    dimmed = !row.completed && kind in row.suggested,
                    modifier = Modifier
                        .width(fieldWidth(kind, kinds.size))
                        .testTag(fieldTag(row.id, kind)),
                )
            }
            DoneCheckButton(
                done = row.completed,
                onDoneChange = { actions.onCheck(row.id) },
                label = stringResource(R.string.set_done_label, block.name, number),
                modifier = Modifier.testTag(checkTag(row.id)),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            MenuItem(R.string.set_delete, destructive = true) { menu = false; actions.onDeleteSet(row.id) }
        }
    }
}

fun fieldTag(setId: Long, kind: NumberFieldKind) = "field-$setId-$kind"

fun checkTag(setId: Long) = "check-$setId"

/**
 * 아이콘만 있는 최고 기록 배지(docs/DESIGN.md `RecordBadge`). 지름 24dp 원, `text` 바탕에 `bg` 색 트로피.
 * 최고 기록을 넘은 순간 작게 시작해 살짝 넘쳤다가 제 크기가 된다(expressive spatial 빠름).
 * 처음부터 보이는 배지(목록을 내렸다 올림)는 움직이지 않는다.
 */
@Composable
private fun RecordBadge(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(NextSetMotion.expressiveSpatialFast.spec()),
        exit = scaleOut(NextSetMotion.standardSpatialFast.spec()),
    ) {
        RecordBadgeIcon()
    }
}
