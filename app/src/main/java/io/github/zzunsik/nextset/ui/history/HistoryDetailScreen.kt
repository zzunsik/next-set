package io.github.zzunsik.nextset.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.component.NumberFieldKind
import io.github.zzunsik.nextset.ui.component.NumberInputField
import io.github.zzunsik.nextset.ui.component.NumberPadKey
import io.github.zzunsik.nextset.ui.component.NumberPadScaffold
import io.github.zzunsik.nextset.ui.component.PrimaryButton
import io.github.zzunsik.nextset.ui.component.ScreenTopBar
import io.github.zzunsik.nextset.ui.component.padSpec
import io.github.zzunsik.nextset.ui.settings.dateText
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import io.github.zzunsik.nextset.ui.workout.Confirm
import io.github.zzunsik.nextset.ui.workout.NoteDialog
import io.github.zzunsik.nextset.ui.workout.fieldWidth
import java.time.LocalDate
import java.time.ZoneId

/** 기록 상세·수정이 부르는 일. 테스트와 미리보기는 필요한 것만 채운다. */
data class HistoryDetailActions(
    val onBack: () -> Unit = {},
    val onEditToggle: () -> Unit = {},
    val onDeleteRequest: () -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onDismissDialog: () -> Unit = {},
    /** 날짜 칸을 눌렀다. 화면 밖에서 날짜 고르기 창을 띄운다. */
    val onPickDate: () -> Unit = {},
    /** 시작 시각 칸을 눌렀다. 화면 밖에서 시각 고르기 창을 띄운다. */
    val onPickTime: () -> Unit = {},
    val onMinutesClick: () -> Unit = {},
    val onNumberPadKey: (NumberPadKey) -> Unit = {},
    val onAddExercise: () -> Unit = {},
    val onRemoveExerciseConfirm: (workoutExerciseId: Long) -> Unit = {},
    val onExerciseNoteChange: (workoutExerciseId: Long, note: String) -> Unit = { _, _ -> },
    val onNoteChange: (String) -> Unit = {},
    val edit: ReportEditActions = ReportEditActions(),
)

const val MINUTES_FIELD_TAG = "report-minutes"

/**
 * 기록 상세·수정(docs/DESIGN.md "기록 상세·수정", HIST-02, HIST-03, HIST-05).
 * 상단 바(뒤로 가기, 날짜, "수정", 더보기의 "지우기") 아래에 루틴 이름과 시작 시각, 숫자 셋, 최고 기록 목록,
 * 종목별 세트 기록, 운동 메모를 보인다. 숫자 셋부터는 운동 요약과 같다(#27).
 *
 * 수정 상태에서는 "수정"이 "완료"가 되고 날짜, 시작 시각, 운동 시간을 고치고 세트 칸을 숫자판으로 고친다.
 * 세트 추가와 지우기, 종목 추가와 빼기, 종목 메모가 생긴다. 고친 내용은 바로 저장된다(NF-09).
 *
 * @param today 날짜에 연도를 붙일지 가른다(올해가 아니면 붙인다).
 */
@Composable
fun HistoryDetailScreen(
    state: ReportUiState,
    noteText: String,
    today: LocalDate,
    zone: ZoneId,
    actions: HistoryDetailActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val colors = NextSetTheme.colors
    val report = state.report
    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        val date = report?.startedAt?.atZone(zone)?.toLocalDate()
        ScreenTopBar(
            title = date?.let { dateText(it, showYear = it.year != today.year) }.orEmpty(),
            onBack = actions.onBack,
        ) {
            if (report != null) DetailTopActions(state.editing, actions)
        }
        NumberPadScaffold(
            numberPad = state.numberPad.padSpec,
            onNumberPadKey = actions.onNumberPadKey,
            modifier = Modifier.weight(1f),
        ) { padding ->
            val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val bottom = maxOf(padding.calculateBottomPadding(), navigationBar)
            Box(Modifier.fillMaxSize().padding(bottom = bottom)) {
                if (report != null) {
                    LazyColumn(modifier = Modifier.fillMaxSize().testTag(REPORT_LIST_TAG)) {
                        item(key = "title") { DetailTitle(report, zone) }
                        if (state.editing) timeFields(report, today, zone, state, actions)
                        reportNumbers(report)
                        reportRecords(report)
                        reportExercises(report, state.editing, state.numberPad, actions.edit)
                        if (state.editing) addExercise(report.exercises.isEmpty(), actions.onAddExercise)
                        when {
                            state.editing -> noteInput(noteText, actions.onNoteChange)
                            !report.note.isNullOrBlank() -> noteText(report.note)
                        }
                        bottomSpace()
                    }
                }
                state.dialog?.let { DetailDialog(it, actions) }
                SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun DetailTopActions(editing: Boolean, actions: HistoryDetailActions) {
    var menu by remember { mutableStateOf(false) }
    TextButton(
        onClick = actions.onEditToggle,
        modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight),
        shape = NextSetTheme.radius.md,
    ) {
        Text(
            text = stringResource(if (editing) R.string.report_edit_done else R.string.report_edit),
            style = NextSetTheme.typography.label,
        )
    }
    Box {
        IconButton(onClick = { menu = true }, modifier = Modifier.size(NextSetTheme.sizes.minTouchTarget)) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.report_more),
                tint = NextSetTheme.colors.text,
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            MenuItem(R.string.action_delete, destructive = true) { menu = false; actions.onDeleteRequest() }
        }
    }
}

/** 루틴 이름(`headline`)과 시작 시각. */
@Composable
private fun DetailTitle(report: WorkoutReportUi, zone: ZoneId) {
    val spacing = NextSetTheme.spacing
    Column(
        modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(
            text = report.routineName ?: stringResource(R.string.workout_empty_title),
            style = NextSetTheme.typography.headline,
            color = NextSetTheme.colors.text,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.report_started_at, clockText(report.startedAt, zone)),
            style = NextSetTheme.typography.caption,
            color = NextSetTheme.colors.text2,
        )
    }
}

/** 수정 상태의 날짜, 시작 시각, 운동 시간. 날짜와 시각은 고르기 창으로, 운동 시간은 숫자판으로 고친다. */
private fun LazyListScope.timeFields(
    report: WorkoutReportUi,
    today: LocalDate,
    zone: ZoneId,
    state: ReportUiState,
    actions: HistoryDetailActions,
) {
    item(key = "time-fields") {
        val start = report.startedAt.atZone(zone)
        val date = start.toLocalDate()
        Column(modifier = Modifier.padding(vertical = NextSetTheme.spacing.sm)) {
            FieldRow(stringResource(R.string.report_date)) {
                ValueButton(dateText(date, showYear = date.year != today.year), stringResource(R.string.report_date), actions.onPickDate)
            }
            FieldRow(stringResource(R.string.report_start_time)) {
                val clock = clockText(report.startedAt, zone)
                ValueButton(clock, stringResource(R.string.report_start_time), actions.onPickTime)
            }
            FieldRow(stringResource(R.string.report_minutes)) {
                val pad = state.numberPad
                val selected = pad?.focused?.id == ReportFieldId.Minutes
                NumberInputField(
                    value = if (selected) pad.entry.displayText else report.stats.minutes.toString(),
                    label = stringResource(R.string.report_minutes),
                    selected = selected,
                    onClick = actions.onMinutesClick,
                    modifier = Modifier.width(fieldWidth(NumberFieldKind.REPS, 1)).testTag(MINUTES_FIELD_TAG),
                )
            }
        }
    }
}

@Composable
private fun FieldRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NextSetTheme.sizes.setRowMinHeight)
            .padding(horizontal = NextSetTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.sm),
    ) {
        Text(
            text = label,
            style = NextSetTheme.typography.body,
            color = NextSetTheme.colors.text,
            modifier = Modifier.weight(1f),
        )
        content()
    }
}

@Composable
private fun ValueButton(text: String, label: String, onClick: () -> Unit) {
    val description = stringResource(R.string.number_field_description, label, text)
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = NextSetTheme.sizes.buttonHeight)
            .semantics { contentDescription = description },
        shape = NextSetTheme.radius.md,
    ) {
        Text(text = text, style = NextSetTheme.typography.label)
    }
}

/** "종목 추가". 종목이 없으면 빈 상태 문구와 채운 버튼으로 보인다(docs/DESIGN.md "기록 상세·수정"의 빈 상태). */
private fun LazyListScope.addExercise(empty: Boolean, onAddExercise: () -> Unit) {
    item(key = "add-exercise") {
        val spacing = NextSetTheme.spacing
        if (empty) {
            Column(
                modifier = Modifier
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
        } else {
            TextButton(
                onClick = onAddExercise,
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

/** 읽기 전용의 운동 메모. 메모가 없으면 구역을 보이지 않는다. */
private fun LazyListScope.noteText(note: String) {
    item(key = "note") {
        Column(modifier = Modifier.padding(top = NextSetTheme.spacing.xl)) {
            SectionTitle(stringResource(R.string.report_note_title))
            Text(
                text = note,
                style = NextSetTheme.typography.body,
                color = NextSetTheme.colors.text,
                modifier = Modifier.padding(horizontal = NextSetTheme.spacing.lg),
            )
        }
    }
}

@Composable
private fun DetailDialog(dialog: ReportDialog, actions: HistoryDetailActions) {
    when (dialog) {
        ReportDialog.Delete -> Confirm(
            text = stringResource(R.string.report_delete_confirm),
            confirm = stringResource(R.string.action_delete),
            dismiss = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = actions.onDeleteConfirm,
            onDismiss = actions.onDismissDialog,
        )
        is ReportDialog.RemoveExercise -> Confirm(
            text = stringResource(R.string.remove_exercise_confirm, dialog.name),
            confirm = stringResource(R.string.remove_exercise_action),
            dismiss = stringResource(R.string.action_cancel),
            destructive = true,
            onConfirm = { actions.onRemoveExerciseConfirm(dialog.workoutExerciseId) },
            onDismiss = actions.onDismissDialog,
        )
        is ReportDialog.ExerciseNote -> NoteDialog(
            title = dialog.name,
            note = dialog.note,
            onNoteChange = { actions.onExerciseNoteChange(dialog.workoutExerciseId, it) },
            onDismiss = actions.onDismissDialog,
        )
    }
}
