package io.github.zzunsik.nextset.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.component.PrimaryButton
import io.github.zzunsik.nextset.ui.component.TextInputField
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

const val REPORT_LIST_TAG = "reportList"

/**
 * 운동 요약(docs/DESIGN.md "운동 요약", LOG-15). 운동을 끝내면 이 화면으로 온다.
 * 위에서부터 결론 문장, 숫자 셋, 최고 기록 목록, 종목별 세트 기록, 운동 메모 입력 칸, 아래에 고정한 "확인".
 * 기록 상세와 같은 내용을 같은 배치로 보인다(#27). "확인"과 뒤로 가기는 홈으로 간다.
 */
@Composable
fun WorkoutSummaryScreen(
    state: ReportUiState,
    noteText: String,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        val report = state.report
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().testTag(REPORT_LIST_TAG)) {
            if (report != null) {
                item(key = "conclusion") { SummaryConclusion(report) }
                reportNumbers(report)
                reportRecords(report)
                reportExercises(report, editing = false, numberPad = null, actions = ReportEditActions())
                noteInput(noteText, onNoteChange)
                bottomSpace()
            }
        }
        PrimaryButton(
            text = stringResource(R.string.action_confirm),
            onClick = onConfirm,
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
        )
    }
}

/**
 * 결론 문장(`headline`). "가슴 루틴을 끝냈어요." 조사를 정할 수 없는 이름은 "Push Day 루틴을 끝냈어요."(#93). 최고 기록이 있으면 "최고 기록 2개를 세웠어요."(원칙 5)와 그 아래에 루틴 이름.
 */
@Composable
private fun SummaryConclusion(report: WorkoutReportUi) {
    val spacing = NextSetTheme.spacing
    val routine = report.routineName
    Column(
        modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.xl, bottom = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        val conclusion = when {
            report.records.isNotEmpty() -> stringResource(R.string.summary_records, report.records.size)
            routine != null -> withObjectParticle(routine)?.let { stringResource(R.string.summary_done_routine, it) }
                ?: stringResource(R.string.summary_done_routine_unknown_final, routine.trimEnd())
            else -> stringResource(R.string.workout_done_title)
        }
        Text(
            text = conclusion,
            style = NextSetTheme.typography.headline,
            color = NextSetTheme.colors.text,
            modifier = Modifier.semantics { heading() },
        )
        if (report.records.isNotEmpty()) {
            Text(
                text = routine ?: stringResource(R.string.workout_empty_title),
                style = NextSetTheme.typography.body,
                color = NextSetTheme.colors.text2,
            )
        }
    }
}

/** 운동 메모 입력 칸(LOG-11). 글자를 칠 때마다 저장한다(NF-09). */
internal fun LazyListScope.noteInput(noteText: String, onNoteChange: (String) -> Unit) {
    item(key = "note") {
        Column(modifier = Modifier.padding(top = NextSetTheme.spacing.xl)) {
            SectionTitle(stringResource(R.string.report_note_title))
            TextInputField(
                value = noteText,
                onValueChange = onNoteChange,
                placeholder = stringResource(R.string.note_hint),
                modifier = Modifier.padding(horizontal = NextSetTheme.spacing.lg),
            )
        }
    }
}
