package io.github.zzunsik.nextset.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SubPart
import io.github.zzunsik.nextset.ui.component.PrimaryButton
import io.github.zzunsik.nextset.ui.component.ScreenTopBar
import io.github.zzunsik.nextset.ui.component.SelectChip
import io.github.zzunsik.nextset.ui.component.TextInputField
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/** 화면 상태를 [ExerciseFormViewModel]에서 받아 [ExerciseFormScreen]을 그리고, 저장이나 지우기가 끝나면 [onFinished]를 부른다. */
@Composable
fun ExerciseFormRoute(
    viewModel: ExerciseFormViewModel,
    onBack: () -> Unit,
    onFinished: (ExerciseFormOutcome) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnFinished by rememberUpdatedState(onFinished)
    val outcome = state.outcome
    LaunchedEffect(outcome) {
        if (outcome != null) currentOnFinished(outcome)
    }
    ExerciseFormScreen(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onBodyPartSelect = viewModel::onBodyPartSelect,
        onSubPartToggle = viewModel::onSubPartToggle,
        onEquipmentSelect = viewModel::onEquipmentSelect,
        onRecordTypeSelect = viewModel::onRecordTypeSelect,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

/**
 * 종목 추가·수정(docs/DESIGN.md "종목 추가·수정"). 이름, 부위, 세부 부위, 장비, 기록 방식, "저장", "종목 지우기".
 * 직접 추가한 종목만 이 화면으로 온다.
 */
@Composable
fun ExerciseFormScreen(
    state: ExerciseFormUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onBodyPartSelect: (BodyPart) -> Unit,
    onSubPartToggle: (SubPart) -> Unit,
    onEquipmentSelect: (Equipment) -> Unit,
    onRecordTypeSelect: (RecordType) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
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
                title = stringResource(if (state.isEdit) R.string.exercise_form_edit_title else R.string.exercise_new),
                onBack = onBack,
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                // 저장이 왜 비활성인지 버튼 바로 위에 적는다. 글자가 커져 고를 항목이 화면 밖에 있어도 보인다(#55).
                val missingChoice = when {
                    state.missingBodyPart && state.missingEquipment -> R.string.exercise_form_missing_both
                    state.missingBodyPart -> R.string.exercise_form_missing_body_part
                    state.missingEquipment -> R.string.exercise_form_missing_equipment
                    else -> null
                }
                if (missingChoice != null) {
                    Text(
                        text = stringResource(missingChoice),
                        style = NextSetTheme.typography.caption,
                        color = colors.text2,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                PrimaryButton(
                    text = stringResource(R.string.exercise_form_save),
                    onClick = onSave,
                    enabled = state.canSave,
                )
            }
        },
    ) { innerPadding ->
        if (state.loading) return@Scaffold
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            Section(title = stringResource(R.string.exercise_form_name)) {
                TextInputField(
                    value = state.name,
                    onValueChange = onNameChange,
                    placeholder = stringResource(R.string.exercise_form_name_hint),
                )
                if (state.nameConflict) {
                    Text(
                        text = stringResource(R.string.exercise_form_name_conflict),
                        style = NextSetTheme.typography.caption,
                        color = colors.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            Section(title = stringResource(R.string.exercise_form_body_part)) {
                ChipFlow {
                    BodyPart.entries.forEach { part ->
                        SelectChip(
                            text = stringResource(part.label),
                            selected = state.bodyPart == part,
                            onClick = { onBodyPartSelect(part) },
                        )
                    }
                }
            }
            val subParts = state.bodyPart?.subParts.orEmpty()
            if (subParts.isNotEmpty()) {
                Section(title = stringResource(R.string.exercise_form_sub_part)) {
                    ChipFlow {
                        subParts.forEach { sub ->
                            SelectChip(
                                text = stringResource(sub.label),
                                selected = state.subPart == sub,
                                onClick = { onSubPartToggle(sub) },
                            )
                        }
                    }
                }
            }
            Section(title = stringResource(R.string.exercise_form_equipment)) {
                ChipFlow {
                    Equipment.entries.forEach { equipment ->
                        SelectChip(
                            text = stringResource(equipment.label),
                            selected = state.equipment == equipment,
                            onClick = { onEquipmentSelect(equipment) },
                        )
                    }
                }
            }
            Section(title = stringResource(R.string.exercise_form_record_type)) {
                ChipFlow {
                    RecordType.entries.forEach { type ->
                        SelectChip(
                            text = stringResource(type.label),
                            selected = state.recordType == type,
                            onClick = { onRecordTypeSelect(type) },
                        )
                    }
                }
                if (state.showsRecordTypeNotice) {
                    Text(
                        text = stringResource(R.string.exercise_form_record_type_notice),
                        style = NextSetTheme.typography.caption,
                        color = colors.text2,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            if (state.isEdit) {
                TextButton(
                    onClick = { confirmingDelete = true },
                    modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight),
                    enabled = !state.busy,
                    shape = NextSetTheme.radius.full,
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                ) {
                    Text(text = stringResource(R.string.exercise_form_delete), style = NextSetTheme.typography.label)
                }
            }
        }
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            text = { Text(text = stringResource(R.string.exercise_delete_confirm), style = NextSetTheme.typography.body) },
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

/** 구역 제목(`label`, `text2`)과 그 아래 내용. */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.sm)) {
        Text(
            text = title,
            style = NextSetTheme.typography.label,
            color = NextSetTheme.colors.text2,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/** 칩을 줄을 바꿔 가며 놓는다. 글자가 커져도 옆으로 밀지 않고 아래로 이어진다. */
@Composable
private fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.md),
    ) {
        content()
    }
}
