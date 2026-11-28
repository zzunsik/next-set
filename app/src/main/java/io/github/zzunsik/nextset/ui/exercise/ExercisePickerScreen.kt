package io.github.zzunsik.nextset.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clip
import io.github.zzunsik.nextset.ui.component.groupedRowShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.SubPart
import io.github.zzunsik.nextset.ui.component.PrimaryButton
import io.github.zzunsik.nextset.ui.component.ScreenTopBar
import io.github.zzunsik.nextset.ui.component.SearchField
import io.github.zzunsik.nextset.ui.component.SelectChip
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerMode
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/** 화면 상태를 [ExercisePickerViewModel]에서 받아 [ExercisePickerScreen]을 그린다. */
@Composable
fun ExercisePickerRoute(
    viewModel: ExercisePickerViewModel,
    onBack: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    onNewExercise: (initialName: String) -> Unit,
    onEditExercise: (exerciseId: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExercisePickerScreen(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onBodyPartSelect = viewModel::onBodyPartSelect,
        onSubPartToggle = viewModel::onSubPartToggle,
        onToggle = viewModel::onToggle,
        onConfirm = { onConfirm(state.selectedIds) },
        onNewExercise = onNewExercise,
        onEditExercise = onEditExercise,
    )
}

/**
 * 종목 고르기(docs/DESIGN.md "종목 고르기"). 상단 바, 검색 칸, 부위 필터, 종목 목록 행, "n개 추가" 버튼.
 * 관리로 열면 제목이 "종목 관리"이고 고르지 않는다. 직접 추가한 종목의 행을 누르면 수정이 열린다.
 */
@Composable
fun ExercisePickerScreen(
    state: ExercisePickerUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onBodyPartSelect: (BodyPart?) -> Unit,
    onSubPartToggle: (SubPart) -> Unit,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onNewExercise: (initialName: String) -> Unit,
    onEditExercise: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    val picking = state.mode == ExercisePickerMode.Pick
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = colors.bg,
        topBar = {
            ScreenTopBar(
                title = stringResource(if (picking) R.string.exercise_picker_title else R.string.exercise_manage_title),
                onBack = onBack,
            ) {
                TextButton(
                    onClick = { onNewExercise("") },
                    modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight),
                    shape = NextSetTheme.radius.full,
                ) {
                    Text(text = stringResource(R.string.exercise_new), style = NextSetTheme.typography.label)
                }
            }
        },
        bottomBar = {
            if (picking && state.selectedIds.isNotEmpty()) {
                PrimaryButton(
                    text = stringResource(R.string.exercise_add_selected, state.selectedIds.size),
                    onClick = onConfirm,
                    modifier = Modifier
                        .background(colors.bg)
                        .navigationBarsPadding()
                        .padding(horizontal = spacing.lg, vertical = spacing.sm),
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SearchField(
                query = state.query,
                onQueryChange = onQueryChange,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
            )
            BodyPartFilter(
                bodyPart = state.bodyPart,
                subPart = state.subPart,
                onBodyPartSelect = onBodyPartSelect,
                onSubPartToggle = onSubPartToggle,
            )
            when {
                state.loading -> Unit
                state.showsNoResults -> NoResults(query = state.query.trim(), onCreate = onNewExercise)
                else -> ExerciseList(state = state, onToggle = onToggle, onEditExercise = onEditExercise)
            }
        }
    }
}

/** 부위 칩 한 줄. 하체나 팔을 고르면 세부 부위 칩 줄이 한 줄 더 나온다(docs/DESIGN.md `BodyPartFilter`). */
@Composable
private fun BodyPartFilter(
    bodyPart: BodyPart?,
    subPart: SubPart?,
    onBodyPartSelect: (BodyPart?) -> Unit,
    onSubPartToggle: (SubPart) -> Unit,
) {
    val partsLabel = stringResource(R.string.exercise_body_part_filter)
    ChipRow(modifier = Modifier.semantics { contentDescription = partsLabel }) {
        SelectChip(
            text = stringResource(R.string.body_part_all),
            selected = bodyPart == null,
            onClick = { onBodyPartSelect(null) },
        )
        BodyPart.entries.forEach { part ->
            SelectChip(
                text = stringResource(part.label),
                selected = bodyPart == part,
                onClick = { onBodyPartSelect(part) },
            )
        }
    }
    val subParts = bodyPart?.subParts.orEmpty()
    if (subParts.isNotEmpty()) {
        val subPartsLabel = stringResource(R.string.exercise_sub_part_filter)
        ChipRow(modifier = Modifier.semantics { contentDescription = subPartsLabel }) {
            subParts.forEach { sub ->
                SelectChip(
                    text = stringResource(sub.label),
                    selected = subPart == sub,
                    onClick = { onSubPartToggle(sub) },
                )
            }
        }
    }
}

/** 칩을 가로로 한 줄 놓고 넘치면 옆으로 민다. */
@Composable
private fun ChipRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val spacing = NextSetTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        content()
    }
}

@Composable
private fun ExerciseList(
    state: ExercisePickerUiState,
    onToggle: (String) -> Unit,
    onEditExercise: (String) -> Unit,
) {
    val picking = state.mode == ExercisePickerMode.Pick
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(EXERCISE_LIST_TAG),
        contentPadding = PaddingValues(horizontal = NextSetTheme.spacing.lg, vertical = NextSetTheme.spacing.sm),
    ) {
        itemsIndexed(state.exercises, key = { _, it -> it.id }) { index, exercise ->
            val mode = if (picking) {
                ExerciseRowMode.Pick(selected = exercise.id in state.selectedIds, onToggle = { onToggle(exercise.id) })
            } else {
                // 내장 종목은 고치거나 지우지 않는다. 행이 누르는 요소가 아니다.
                ExerciseRowMode.Manage(onEdit = if (exercise.isBuiltin) null else ({ onEditExercise(exercise.id) }))
            }
            Column(Modifier.clip(groupedRowShape(index == 0, index == state.exercises.lastIndex)).background(NextSetTheme.colors.surface)) {
                ExerciseRow(exercise = exercise, mode = mode)
                if (index < state.exercises.lastIndex) HorizontalDivider(color = NextSetTheme.colors.line)
            }
        }
    }
}

/** 검색 결과가 없을 때. 지금 상태를 한 문장으로 적고 그 이름으로 종목을 만드는 버튼을 준다. */
@Composable
private fun NoResults(query: String, onCreate: (String) -> Unit) {
    val spacing = NextSetTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Text(
            text = if (query.isEmpty()) {
                stringResource(R.string.exercise_no_results_filter)
            } else {
                stringResource(R.string.exercise_no_results, query)
            },
            style = NextSetTheme.typography.body,
            color = NextSetTheme.colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            text = if (query.isEmpty()) {
                stringResource(R.string.exercise_new)
            } else {
                stringResource(R.string.exercise_create_named, query)
            },
            onClick = { onCreate(query) },
        )
    }
}

/** 화면 테스트가 목록을 찾을 때 쓴다. */
const val EXERCISE_LIST_TAG = "exercise_list"
