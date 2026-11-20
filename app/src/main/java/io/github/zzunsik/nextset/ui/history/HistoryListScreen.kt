package io.github.zzunsik.nextset.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.component.PrimaryButton
import io.github.zzunsik.nextset.ui.component.RecordBadgeIcon
import io.github.zzunsik.nextset.ui.settings.dateText
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import io.github.zzunsik.nextset.ui.workout.workoutDurationText

/** 기록 목록이 부르는 일. */
data class HistoryListActions(
    /** 상단 바의 뒤로 가기. 하단 내비게이션의 탭으로 열면 null이라 버튼이 없다. */
    val onBack: (() -> Unit)? = null,
    val onOpen: (workoutId: Long) -> Unit = {},
    /** "지난 운동 적기". 화면 밖에서 날짜 고르기 창을 띄운다. */
    val onAddPast: () -> Unit = {},
    val onAddPastChoice: (routineId: Long?) -> Unit = {},
    val onDismissAddPast: () -> Unit = {},
    val onGoHome: () -> Unit = {},
)

const val HISTORY_LIST_TAG = "historyList"

/**
 * 기록 목록(docs/DESIGN.md "기록 목록", HIST-01). 상단 바(제목 "기록", "지난 운동 적기"), 달 머리, 기록 행.
 * 최근 날짜가 위다. 기록 행은 날짜, 루틴 이름, "48분 · 15세트 · 4,250kg", 최고 기록이 있으면 배지 아이콘이다.
 */
@Composable
fun HistoryListScreen(state: HistoryUiState, actions: HistoryListActions, modifier: Modifier = Modifier) {
    val colors = NextSetTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        HistoryTopBar(actions)
        when {
            state.loading -> Unit
            state.months.isEmpty() -> EmptyHistory(actions.onGoHome)
            else -> LazyColumn(modifier = Modifier.fillMaxSize().testTag(HISTORY_LIST_TAG)) {
                state.months.forEach { month ->
                    item(key = "month-${month.month}") {
                        Text(
                            text = stringResource(R.string.history_month, month.month.year, month.month.monthValue),
                            style = NextSetTheme.typography.label,
                            color = colors.text2,
                            modifier = Modifier
                                .padding(
                                    start = NextSetTheme.spacing.lg,
                                    end = NextSetTheme.spacing.lg,
                                    top = NextSetTheme.spacing.lg,
                                    bottom = NextSetTheme.spacing.xs,
                                )
                                .semantics { heading() },
                        )
                    }
                    month.rows.forEachIndexed { index, row ->
                        item(key = "row-${row.id}") {
                            Column {
                                if (index > 0) HorizontalDivider(color = colors.line)
                                HistoryRow(row, state, onClick = { actions.onOpen(row.id) })
                            }
                        }
                    }
                }
            }
        }
    }
    state.addPast?.let { AddPastChooser(it, state, actions) }
}

@Composable
private fun HistoryTopBar(actions: HistoryListActions) {
    val spacing = NextSetTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NextSetTheme.sizes.minTouchTarget)
            .padding(start = if (actions.onBack == null) spacing.lg else spacing.xs, end = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        actions.onBack?.let { onBack ->
            IconButton(onClick = onBack, modifier = Modifier.size(NextSetTheme.sizes.minTouchTarget)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.navigate_back),
                    tint = NextSetTheme.colors.text,
                )
            }
        }
        Text(
            text = stringResource(R.string.history_title),
            style = NextSetTheme.typography.title,
            color = NextSetTheme.colors.text,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        TextButton(
            onClick = actions.onAddPast,
            modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight),
            shape = NextSetTheme.radius.md,
        ) {
            Text(text = stringResource(R.string.history_add_past), style = NextSetTheme.typography.label)
        }
    }
}

@Composable
private fun HistoryRow(row: HistoryRowUi, state: HistoryUiState, onClick: () -> Unit) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = NextSetTheme.sizes.listRowMinHeight)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dateText(row.date, showYear = row.date.year != state.today.year),
                style = NextSetTheme.typography.caption,
                color = colors.text2,
            )
            Text(
                text = row.routineName ?: stringResource(R.string.workout_empty_title),
                style = NextSetTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = rowDetailText(row), style = NextSetTheme.typography.caption, color = colors.text2)
        }
        if (row.hasRecord) RecordBadgeIcon()
    }
}

/** "48분 · 15세트 · 4,250kg". 볼륨이 없는 기록은 볼륨을 뺀다. */
@Composable
private fun rowDetailText(row: HistoryRowUi): String {
    val duration = workoutDurationText(row.stats.minutes)
    return if (row.stats.volumeGrams > 0) {
        stringResource(R.string.history_row_detail, duration, row.stats.setCount, volumeText(row.stats.volumeGrams))
    } else {
        stringResource(R.string.history_row_detail_no_volume, duration, row.stats.setCount)
    }
}

/** 빈 상태. 지금 상태를 한 문장으로 적고 다음에 할 일을 버튼 하나로 준다(docs/DESIGN.md "문구"). */
@Composable
private fun EmptyHistory(onGoHome: () -> Unit) {
    val spacing = NextSetTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.history_empty), style = NextSetTheme.typography.body, color = NextSetTheme.colors.text)
        PrimaryButton(text = stringResource(R.string.history_go_home), onClick = onGoHome)
    }
}

/** 지난 운동 적기: 날짜를 고른 뒤 루틴이나 "빈 기록"을 고른다(HIST-06). */
@Composable
private fun AddPastChooser(dialog: AddPastDialog, state: HistoryUiState, actions: HistoryListActions) {
    val colors = NextSetTheme.colors
    AlertDialog(
        onDismissRequest = actions.onDismissAddPast,
        containerColor = colors.surface,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(
                        R.string.history_add_past_question,
                        dateText(dialog.date, showYear = dialog.date.year != state.today.year),
                    ),
                    style = NextSetTheme.typography.body,
                    color = colors.text,
                    modifier = Modifier.padding(bottom = NextSetTheme.spacing.sm),
                )
                dialog.routines.forEach { routine ->
                    ChoiceButton(routine.name) { actions.onAddPastChoice(routine.id) }
                }
                ChoiceButton(stringResource(R.string.history_add_past_blank)) { actions.onAddPastChoice(null) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = actions.onDismissAddPast,
                modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight),
                shape = NextSetTheme.radius.md,
            ) {
                Text(text = stringResource(R.string.action_cancel), style = NextSetTheme.typography.label)
            }
        },
    )
}

@Composable
private fun ChoiceButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = NextSetTheme.sizes.buttonHeight),
        shape = NextSetTheme.radius.md,
    ) {
        Text(
            text = text,
            style = NextSetTheme.typography.body,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
