package io.github.zzunsik.nextset.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.component.PrimaryButton
import io.github.zzunsik.nextset.ui.settings.dateText
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import io.github.zzunsik.nextset.ui.workout.elapsedText
import java.time.Instant
import java.time.LocalDate

/** 더보기 메뉴의 항목 하나. */
internal data class MenuAction(val label: String, val onClick: () -> Unit)

/**
 * 다음 차례 루틴 카드(docs/DESIGN.md `NextRoutineCard`, HOME-01~03).
 * 더보기 버튼을 뺀 카드 전체가 "운동 시작"과 같은 일을 한다.
 */
@Composable
internal fun NextRoutineCard(
    routine: HomeRoutineUi,
    pinned: Boolean,
    today: LocalDate,
    onStart: () -> Unit,
    onUnpin: () -> Unit,
    menu: List<MenuAction>,
    modifier: Modifier = Modifier,
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    val startLabel = stringResource(R.string.home_start)
    HomeCard(onClick = onStart, onClickLabel = startLabel, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(if (pinned) R.string.home_pinned_label else R.string.home_next_label),
                    style = NextSetTheme.typography.caption,
                    color = colors.text2,
                )
                if (pinned) {
                    TextButton(
                        onClick = onUnpin,
                        modifier = Modifier
                            .padding(start = spacing.xs)
                            .heightIn(min = NextSetTheme.sizes.minTouchTarget),
                    ) {
                        Text(text = stringResource(R.string.home_unpin), style = NextSetTheme.typography.label)
                    }
                }
            }
            MoreMenu(description = stringResource(R.string.home_routine_more, routine.routine.name), actions = menu)
        }
        Text(
            text = routine.routine.name,
            style = NextSetTheme.typography.headline,
            color = colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        exercisePreview(routine.exerciseNames)?.let {
            Text(
                text = it,
                style = NextSetTheme.typography.body,
                color = colors.text2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        routine.lastDoneOn?.let {
            Text(
                text = stringResource(R.string.home_last_done, dateText(it, showYear = it.year != today.year)),
                style = NextSetTheme.typography.caption,
                color = colors.text2,
            )
        }
        val startDescription = stringResource(R.string.home_start_routine_description, routine.routine.name)
        PrimaryButton(
            text = startLabel,
            onClick = onStart,
            modifier = Modifier
                .padding(top = spacing.sm)
                .semantics { contentDescription = startDescription },
        )
    }
}

/** 이어서 하기 카드(`NextRoutineCard`의 "진행 중" 상태, HOME-06). */
@Composable
internal fun ResumeCard(workout: InProgressWorkoutUi, now: Instant, onResume: () -> Unit, modifier: Modifier = Modifier) {
    val colors = NextSetTheme.colors
    val resumeLabel = stringResource(R.string.home_resume)
    HomeCard(onClick = onResume, onClickLabel = resumeLabel, modifier = modifier) {
        Text(stringResource(R.string.home_resume_label), style = NextSetTheme.typography.caption, color = colors.text2)
        Text(
            text = workout.routineName ?: stringResource(R.string.workout_empty_title),
            style = NextSetTheme.typography.headline,
            color = colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.home_resume_progress, workout.totalSets, workout.completedSets) +
                " · " + elapsedText(workout.startedAt, now),
            style = NextSetTheme.typography.caption,
            color = colors.text2,
        )
        PrimaryButton(text = resumeLabel, onClick = onResume, modifier = Modifier.padding(top = NextSetTheme.spacing.sm))
    }
}

@Composable
private fun HomeCard(
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = NextSetTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NextSetTheme.colors.surface, NextSetTheme.radius.card)
            .clickable(onClickLabel = onClickLabel, onClick = onClick)
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) { content() }
}

/**
 * 루틴 목록 행(docs/DESIGN.md `RoutineRow`, HOME-04). 행을 누르면 바로 시작한다.
 * 이름(`body` SemiBold), 보조 줄(`caption`, `text2`), 시작 표시(`accent`), 더보기 버튼.
 */
@Composable
internal fun RoutineRow(
    routine: HomeRoutineUi,
    today: LocalDate,
    onStart: () -> Unit,
    menu: List<MenuAction>,
    modifier: Modifier = Modifier,
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bg)
            .heightIn(min = NextSetTheme.sizes.listRowMinHeight)
            .clickable(onClickLabel = stringResource(R.string.home_start), onClick = onStart)
            .padding(start = spacing.lg, end = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text = routine.routine.name,
                style = NextSetTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.text,
            )
            Text(text = routineRowDetail(routine, today), style = NextSetTheme.typography.caption, color = colors.text2)
        }
        Icon(
            painter = painterResource(R.drawable.ic_play_arrow),
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(NextSetTheme.sizes.icon),
        )
        MoreMenu(description = stringResource(R.string.home_routine_more, routine.routine.name), actions = menu)
    }
}

/** 오른쪽 위의 더보기 버튼과 메뉴. 누르는 영역은 48dp다(NF-10). */
@Composable
private fun MoreMenu(description: String, actions: List<MenuAction>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(NextSetTheme.sizes.minTouchTarget)) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = description,
                tint = NextSetTheme.colors.text2,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(text = action.label, style = NextSetTheme.typography.body) },
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}

/** "벤치 프레스, 인클라인 덤벨 프레스 외 3개". 종목이 없으면 null이다. */
@Composable
internal fun exercisePreview(names: List<String>): String? = when {
    names.isEmpty() -> null
    names.size <= PREVIEW_COUNT -> names.joinToString(", ")
    else -> stringResource(R.string.home_exercises_more, names.take(PREVIEW_COUNT).joinToString(", "), names.size - PREVIEW_COUNT)
}

/** "종목 5개 · 지난번 11월 12일 (목)". 한 적이 없으면 종목 수만 적는다. */
@Composable
private fun routineRowDetail(routine: HomeRoutineUi, today: LocalDate): String {
    val count = stringResource(R.string.home_exercise_count, routine.exerciseNames.size)
    val last = routine.lastDoneOn ?: return count
    return stringResource(
        R.string.home_row_detail,
        count,
        stringResource(R.string.home_last_done, dateText(last, showYear = last.year != today.year)),
    )
}

private const val PREVIEW_COUNT = 2
