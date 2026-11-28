package io.github.zzunsik.nextset.ui.history

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.ui.component.NumberEntry
import io.github.zzunsik.nextset.ui.component.NumberFieldKind
import io.github.zzunsik.nextset.ui.component.NumberInputField
import io.github.zzunsik.nextset.ui.component.NumberPadSession
import io.github.zzunsik.nextset.ui.component.RecordBadge
import io.github.zzunsik.nextset.ui.component.RecordBadgeIcon
import io.github.zzunsik.nextset.ui.component.numberFieldKinds
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import io.github.zzunsik.nextset.ui.workout.amountOf
import io.github.zzunsik.nextset.ui.workout.fieldWidth
import io.github.zzunsik.nextset.ui.workout.setNumberWidth
import io.github.zzunsik.nextset.ui.workout.workoutDurationText

/** 기록 수정에서 종목과 세트를 고치는 일. 읽기 전용이면 쓰지 않는다. */
data class ReportEditActions(
    val onFieldClick: (setId: Long, kind: NumberFieldKind) -> Unit = { _, _ -> },
    val onAddSet: (workoutExerciseId: Long) -> Unit = {},
    val onDeleteSet: (setId: Long) -> Unit = {},
    val onDeleteLastSet: (workoutExerciseId: Long) -> Unit = {},
    val onExerciseNote: (workoutExerciseId: Long) -> Unit = {},
    val onRemoveExercise: (workoutExerciseId: Long) -> Unit = {},
)

/**
 * 숫자 셋(HIST-02). 운동 시간, 세트 수, 볼륨. 숫자는 `title`, 라벨은 `caption`이다(docs/DESIGN.md "운동 요약").
 * 같은 루틴의 지난 기록이 있으면 아래에 "지난번보다 +120kg"을 견줄 값으로 둔다(원칙 5).
 */
internal fun LazyListScope.reportNumbers(report: WorkoutReportUi) {
    item(key = "numbers") {
        val spacing = NextSetTheme.spacing
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                NumberCell(stringResource(R.string.report_duration), workoutDurationText(report.stats.minutes), Modifier.weight(1f))
                NumberCell(stringResource(R.string.report_sets), report.stats.setCount.toString(), Modifier.weight(1f))
                NumberCell(stringResource(R.string.report_volume), volumeText(report.stats.volumeGrams), Modifier.weight(1f))
            }
            report.volumeDeltaGrams?.let { delta ->
                Text(text = volumeDeltaText(delta), style = NextSetTheme.typography.caption, color = NextSetTheme.colors.text2)
            }
        }
    }
}

@Composable
private fun NumberCell(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = value,
            style = NextSetTheme.typography.title,
            color = NextSetTheme.colors.text,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = NextSetTheme.typography.caption.fontSize,
                maxFontSize = NextSetTheme.typography.title.fontSize,
            ),
        )
        Text(text = label, style = NextSetTheme.typography.caption, color = NextSetTheme.colors.text2)
    }
}

/** 최고 기록 목록(HIST-03). 종목 이름과 값, 최고 기록 배지. 없으면 구역을 보이지 않는다. */
internal fun LazyListScope.reportRecords(report: WorkoutReportUi) {
    if (report.records.isEmpty()) return
    item(key = "records-title") {
        SectionTitle(stringResource(R.string.report_records_title), Modifier.padding(top = NextSetTheme.spacing.md))
    }
    report.records.forEach { record ->
        item(key = "record-${record.setId}") {
            val spacing = NextSetTheme.spacing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = NextSetTheme.sizes.setRowMinHeight)
                    .padding(horizontal = spacing.lg)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = record.exerciseName, style = NextSetTheme.typography.body, color = NextSetTheme.colors.text)
                    Text(
                        text = fullSetText(record.recordType, record.value),
                        style = NextSetTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                        color = NextSetTheme.colors.text,
                    )
                }
                RecordBadge()
            }
        }
    }
}

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = NextSetTheme.typography.heading,
        color = NextSetTheme.colors.text,
        modifier = modifier
            .padding(horizontal = NextSetTheme.spacing.lg, vertical = NextSetTheme.spacing.xs)
            .semantics { heading() },
    )
}

/**
 * 종목별 세트 기록. 종목 머리와 세트 행. [editing]이면 칸을 숫자판으로 고치고 세트와 종목을 더하고 뺀다(HIST-05).
 * 블록 사이는 24dp와 구분선이다(LOG-01과 같은 배치).
 */
internal fun LazyListScope.reportExercises(
    report: WorkoutReportUi,
    editing: Boolean,
    numberPad: NumberPadSession<ReportFieldId>?,
    actions: ReportEditActions,
) {
    report.exercises.forEach { block ->
        item(key = "exercise-${block.id}") {
            ReportExerciseHeader(block = block, editing = editing, actions = actions)
        }
        block.sets.forEach { row ->
            item(key = "set-${row.id}") {
                if (editing) {
                    EditableSetRow(block, row, numberPad, actions)
                } else {
                    ReadOnlySetRow(block, row)
                }
            }
        }
        if (editing) {
            item(key = "add-${block.id}") {
                val description = stringResource(R.string.set_add_description, block.name)
                TextButton(
                    onClick = { actions.onAddSet(block.id) },
                    modifier = Modifier
                        .padding(horizontal = NextSetTheme.spacing.sm)
                        .heightIn(min = NextSetTheme.sizes.buttonHeight)
                        .semantics { contentDescription = description },
                    shape = NextSetTheme.radius.full,
                ) {
                    Text(text = stringResource(R.string.set_add), style = NextSetTheme.typography.label)
                }
            }
        }
    }
}

@Composable
private fun ReportExerciseHeader(block: ReportExerciseUi, editing: Boolean, actions: ReportEditActions) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    var menu by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = colors.line, modifier = Modifier.padding(top = spacing.xl))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = NextSetTheme.sizes.minTouchTarget)
                .padding(start = spacing.lg, end = spacing.xs, top = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = block.name,
                style = NextSetTheme.typography.heading,
                color = colors.text,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            // 읽기 전용에는 더보기 버튼이 없다(docs/DESIGN.md `ExerciseHeader`).
            if (editing) {
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
                        if (block.sets.isNotEmpty()) {
                            MenuItem(R.string.exercise_delete_last_set) { menu = false; actions.onDeleteLastSet(block.id) }
                        }
                        MenuItem(R.string.exercise_remove, destructive = true) { menu = false; actions.onRemoveExercise(block.id) }
                    }
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
        SetTableHeader(block.recordType)
    }
}

@Composable
internal fun MenuItem(text: Int, destructive: Boolean = false, onClick: () -> Unit) {
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

/** 세트 표 머리. 기록 상세와 요약에는 직전 기록 열과 체크 자리가 없다(docs/DESIGN.md `SetRow` 읽기 전용). */
@Composable
private fun SetTableHeader(type: RecordType) {
    val spacing = NextSetTheme.spacing
    val kinds = type.numberFieldKinds()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.Bottom,
    ) {
        HeaderCell(stringResource(R.string.set_table_set), Modifier.width(setNumberWidth()))
        kinds.forEach { kind ->
            val text = when (kind) {
                NumberFieldKind.WEIGHT -> stringResource(R.string.set_table_kg)
                NumberFieldKind.REPS -> stringResource(R.string.set_table_reps)
                NumberFieldKind.DURATION -> stringResource(R.string.set_table_duration)
            }
            HeaderCell(text, Modifier.width(fieldWidth(kind, kinds.size)))
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
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

/** 순번과 그 아래의 아이콘만 있는 최고 기록 배지(docs/DESIGN.md `SetRow` "완료, 최고 기록"). */
@Composable
private fun SetNumber(row: ReportSetUi) {
    val description = stringResource(R.string.set_number_description, row.position + 1)
    Column(modifier = Modifier.width(setNumberWidth()), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${row.position + 1}",
            style = NextSetTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            color = NextSetTheme.colors.text,
            modifier = Modifier.semantics { contentDescription = description },
        )
        if (row.isRecord) RecordBadgeIcon()
    }
}

/** 읽기 전용 세트 행. 값은 `numeralRow`, `text` 색이다. */
@Composable
private fun ReadOnlySetRow(block: ReportExerciseUi, row: ReportSetUi) {
    val spacing = NextSetTheme.spacing
    val kinds = block.recordType.numberFieldKinds()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NextSetTheme.sizes.setRowMinHeight)
            .padding(horizontal = spacing.lg, vertical = spacing.xs)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SetNumber(row)
        kinds.forEach { kind ->
            Text(
                text = NumberEntry.displayTextOf(kind, row.value.amountOf(kind)),
                style = NextSetTheme.typography.numeralRow,
                color = NextSetTheme.colors.text,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = NextSetTheme.typography.caption.fontSize,
                    maxFontSize = NextSetTheme.typography.numeralRow.fontSize,
                ),
                modifier = Modifier.width(fieldWidth(kind, kinds.size)),
            )
        }
    }
}

/** 수정 상태의 세트 행. 칸을 눌러 숫자판으로 고친다. 길게 누르면 "이 세트 지우기"가 뜬다. 직전 기록 열과 체크 버튼은 없다. */
@Composable
private fun EditableSetRow(
    block: ReportExerciseUi,
    row: ReportSetUi,
    numberPad: NumberPadSession<ReportFieldId>?,
    actions: ReportEditActions,
) {
    val spacing = NextSetTheme.spacing
    val kinds = block.recordType.numberFieldKinds()
    val number = row.position + 1
    var menu by remember { mutableStateOf(false) }
    val deleteLabel = stringResource(R.string.set_delete)
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(row.id) { detectTapGestures(onLongPress = { menu = true }) }
                .semantics {
                    customActions = listOf(CustomAccessibilityAction(deleteLabel) { actions.onDeleteSet(row.id); true })
                }
                .heightIn(min = NextSetTheme.sizes.setRowMinHeight)
                .padding(horizontal = spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SetNumber(row)
            kinds.forEach { kind ->
                val id = ReportFieldId.SetField(row.id, kind)
                val selected = numberPad?.focused?.id == id
                val fieldName = stringResource(
                    when (kind) {
                        NumberFieldKind.WEIGHT -> R.string.number_field_weight
                        NumberFieldKind.REPS -> R.string.number_field_reps
                        NumberFieldKind.DURATION -> R.string.number_field_duration
                    },
                )
                NumberInputField(
                    value = if (selected) numberPad.entry.displayText else NumberEntry.displayTextOf(kind, row.value.amountOf(kind)),
                    label = stringResource(R.string.set_field_label, block.name, number, fieldName),
                    selected = selected,
                    onClick = { actions.onFieldClick(row.id, kind) },
                    modifier = Modifier
                        .width(fieldWidth(kind, kinds.size))
                        .testTag(reportFieldTag(row.id, kind)),
                )
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            MenuItem(R.string.set_delete, destructive = true) { menu = false; actions.onDeleteSet(row.id) }
        }
    }
}

fun reportFieldTag(setId: Long, kind: NumberFieldKind) = "report-field-$setId-$kind"

/** 빈 자리. 마지막 항목이 화면 아래 버튼과 붙지 않게 둔다. */
internal fun LazyListScope.bottomSpace() {
    item(key = "bottom") { Spacer(Modifier.heightIn(min = NextSetTheme.spacing.xl)) }
}
