package io.github.zzunsik.nextset.ui.routine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.rule.defaultRestChoices
import io.github.zzunsik.nextset.ui.component.NumberEntry
import io.github.zzunsik.nextset.ui.component.NumberInputField
import io.github.zzunsik.nextset.ui.component.NumberPadKey
import io.github.zzunsik.nextset.ui.component.NumberPadScaffold
import io.github.zzunsik.nextset.ui.component.ScreenTopBar
import io.github.zzunsik.nextset.ui.component.padSpec
import io.github.zzunsik.nextset.ui.settings.OptionDialog
import io.github.zzunsik.nextset.ui.settings.restDurationText
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/** 목표 칸의 테스트 태그. */
fun targetFieldTag(field: TargetField) = "target-${field.name}"

/**
 * 종목 설정(docs/DESIGN.md "루틴 편집"의 종목 설정). 세트 수(− 3 +), 목표, 휴식 시간, "루틴에서 빼기"(RTN-03, RTN-04, REST-03).
 *
 * 목표 칸은 기록 방식에 따라 바뀐다. 무게와 횟수는 목표 무게와 목표 횟수 범위, 횟수만은 목표 횟수 범위, 시간은 목표 시간이다.
 * 목표 칸은 자체 숫자판으로 적는다(LOG-07). 숫자판의 "완료"는 숫자판만 닫는다.
 */
@Composable
fun RoutineExerciseScreen(
    state: RoutineExerciseUiState,
    onBack: () -> Unit,
    onSetCountChange: (Int) -> Unit,
    onFieldClick: (TargetField) -> Unit,
    onNumberPadKey: (NumberPadKey) -> Unit,
    onRestChange: (Int?) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    val exercise = state.exercise
    var choosingRest by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        ScreenTopBar(title = exercise?.nameKo.orEmpty(), onBack = onBack)
        if (state.loading || exercise == null) return@Column
        NumberPadScaffold(
            numberPad = state.numberPad.padSpec,
            onNumberPadKey = onNumberPadKey,
            modifier = Modifier.weight(1f),
        ) { padPadding ->
            Column(
                modifier = Modifier
                    .padding(padPadding)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xl),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    SectionTitle(stringResource(R.string.routine_exercise_sets))
                    SetCountStepper(state, onSetCountChange)
                }
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    SectionTitle(stringResource(R.string.routine_exercise_target))
                    Text(
                        text = stringResource(R.string.routine_exercise_target_note),
                        style = NextSetTheme.typography.caption,
                        color = colors.text2,
                    )
                    TargetFields(state, onFieldClick)
                    if (state.repsRangeInvalid) {
                        Text(
                            text = stringResource(R.string.routine_target_reps_invalid),
                            style = NextSetTheme.typography.caption,
                            color = colors.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    SectionTitle(stringResource(R.string.routine_exercise_rest))
                    val rest = exercise.restSeconds
                    Text(
                        text = if (rest == null) {
                            stringResource(R.string.routine_exercise_rest_default, restDurationText(state.defaultRestSeconds))
                        } else {
                            restDurationText(rest)
                        },
                        style = NextSetTheme.typography.body,
                        color = colors.accentText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = NextSetTheme.sizes.minTouchTarget)
                            .clickable(onClickLabel = stringResource(R.string.routine_exercise_rest), onClick = { choosingRest = true })
                            .padding(vertical = spacing.sm),
                    )
                }
                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight),
                    shape = NextSetTheme.radius.full,
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                ) {
                    Text(text = stringResource(R.string.routine_exercise_remove), style = NextSetTheme.typography.label)
                }
            }
        }
    }
    if (choosingRest && exercise != null) {
        OptionDialog(
            title = stringResource(R.string.routine_exercise_rest),
            note = stringResource(R.string.routine_exercise_rest_note),
            options = listOf<Int?>(null) + defaultRestChoices,
            selected = exercise.restSeconds,
            label = { seconds ->
                if (seconds == null) {
                    stringResource(R.string.routine_exercise_rest_default, restDurationText(state.defaultRestSeconds))
                } else {
                    restDurationText(seconds)
                }
            },
            onSelect = onRestChange,
            onDismiss = { choosingRest = false },
        )
    }
}

/** 세트 수 "− 3 +". 1에서는 "−"가 비활성이다(RTN-04). */
@Composable
private fun SetCountStepper(state: RoutineExerciseUiState, onSetCountChange: (Int) -> Unit) {
    val spacing = NextSetTheme.spacing
    val count = stringResource(R.string.routine_set_count, state.setCount)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
        StepButton(
            text = "−",
            description = stringResource(R.string.routine_exercise_sets_decrease),
            enabled = state.canDecreaseSets,
            onClick = { onSetCountChange(-1) },
        )
        Text(
            text = "${state.setCount}",
            style = NextSetTheme.typography.title,
            color = NextSetTheme.colors.text,
            textAlign = TextAlign.Center,
            // "3"이 숫자판의 "3" 키와 같게 읽히지 않도록 "3세트"로 알린다.
            modifier = Modifier
                .widthIn(min = NextSetTheme.sizes.minTouchTarget)
                .clearAndSetSemantics {
                    contentDescription = count
                    liveRegion = LiveRegionMode.Polite
                },
        )
        StepButton(
            text = "+",
            description = stringResource(R.string.routine_exercise_sets_increase),
            enabled = state.canIncreaseSets,
            onClick = { onSetCountChange(1) },
        )
    }
}

@Composable
private fun StepButton(text: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(NextSetTheme.sizes.minTouchTarget)
            .semantics { contentDescription = description },
        shape = NextSetTheme.radius.full,
        border = BorderStroke(1.dp, if (enabled) NextSetTheme.colors.text2 else NextSetTheme.colors.line),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
    ) {
        Text(text = text, style = NextSetTheme.typography.title)
    }
}

/** 기록 방식에 맞춘 목표 칸(RTN-03). 값은 숫자판으로 적고, 고른 칸은 숫자판이 고친 글자를 그대로 보인다. */
@Composable
private fun TargetFields(state: RoutineExerciseUiState, onFieldClick: (TargetField) -> Unit) {
    val spacing = NextSetTheme.spacing
    val fields = state.fields
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        if (TargetField.WEIGHT in fields) {
            TargetRow(stringResource(R.string.routine_target_weight_label)) {
                TargetInput(state, TargetField.WEIGHT, stringResource(R.string.routine_target_weight_label), 72.dp, onFieldClick)
                UnitText(stringResource(R.string.unit_kg))
            }
        }
        if (TargetField.REPS_MIN in fields) {
            TargetRow(stringResource(R.string.routine_target_reps_label)) {
                TargetInput(state, TargetField.REPS_MIN, stringResource(R.string.routine_target_reps_min), 64.dp, onFieldClick)
                UnitText(stringResource(R.string.range_separator))
                TargetInput(state, TargetField.REPS_MAX, stringResource(R.string.routine_target_reps_max), 64.dp, onFieldClick)
                UnitText(stringResource(R.string.unit_reps))
            }
        }
        if (TargetField.DURATION in fields) {
            TargetRow(stringResource(R.string.routine_target_duration_label)) {
                TargetInput(state, TargetField.DURATION, stringResource(R.string.routine_target_duration_label), 88.dp, onFieldClick)
            }
        }
    }
}

@Composable
private fun TargetRow(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.xs)) {
        Text(text = label, style = NextSetTheme.typography.body, color = NextSetTheme.colors.text)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.sm),
        ) { content() }
    }
}

@Composable
private fun TargetInput(
    state: RoutineExerciseUiState,
    field: TargetField,
    label: String,
    width: Dp,
    onFieldClick: (TargetField) -> Unit,
) {
    val pad = state.numberPad
    val selected = pad?.focused?.id == field
    NumberInputField(
        value = if (selected) pad.entry.displayText else NumberEntry.displayTextOf(field.kind, state.target.amountOf(field)),
        label = label,
        selected = selected,
        onClick = { onFieldClick(field) },
        modifier = Modifier
            .testTag(targetFieldTag(field))
            // 세트 행의 칸 폭을 최소로 두고, 글자가 커지면 넓어진다. 이 화면은 옆에 자리가 남는다.
            .widthIn(min = width)
            .width(IntrinsicSize.Max),
    )
}

/** 칸 옆의 단위와 범위 표시(`caption`, `text2`). */
@Composable
private fun UnitText(text: String) {
    Text(text = text, style = NextSetTheme.typography.caption, color = NextSetTheme.colors.text2)
}

