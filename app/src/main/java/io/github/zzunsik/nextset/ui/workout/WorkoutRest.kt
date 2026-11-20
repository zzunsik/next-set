package io.github.zzunsik.nextset.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import io.github.zzunsik.nextset.domain.rest.RestTimer
import io.github.zzunsik.nextset.domain.rest.RestTimerRules
import io.github.zzunsik.nextset.domain.rest.RestTimerState
import io.github.zzunsik.nextset.domain.rule.restSecondsFor
import io.github.zzunsik.nextset.ui.rest.rememberNotificationPermissionState
import io.github.zzunsik.nextset.ui.theme.NextSetMotion
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import kotlinx.coroutines.delay

/**
 * 세트를 완료했을 때 시작할 휴식 시간(초). 시작하지 않으면 null이다.
 *
 * - 종목에 정한 값이 있으면 그 값, 없으면 설정의 기본값이다(REST-03).
 * - 완료한 세트 말고는 남은 세트가 없으면(운동의 마지막 세트) 시작하지 않는다(제안).
 */
internal fun restSecondsToStart(event: WorkoutEvent.SetCompleted, state: WorkoutUiState): Int? {
    val othersLeft = state.exercises.any { block -> block.sets.any { !it.completed && it.id != event.setId } }
    if (!othersLeft) return null
    return restSecondsFor(event.restSeconds, state.defaultRestSeconds)
}

/** 남은 시간을 위로 올린 초. "1:44.3"이 남았으면 "1:45"로 보인다. */
internal fun remainingSecondsText(remainingMillis: Long): String =
    durationText(((remainingMillis + 999) / 1000).toInt())

/** 휴식 바와 알림에 적는 다음 세트. "벤치 프레스 60kg × 10". 값이 없으면 종목 이름만이다. */
@Composable
internal fun restNextSetText(next: NextSetUi?): String? {
    next ?: return null
    val value = shortSetText(next.recordType, next.value) ?: return next.exerciseName
    return stringResource(R.string.rest_next_set_value, next.exerciseName, value)
}

/** 종목 머리의 "휴식 2:00"(docs/DESIGN.md `ExerciseHeader`). 누르면 이 종목의 휴식 시간을 고친다. */
@Composable
internal fun RestTimeButton(seconds: Int, exerciseName: String, onClick: () -> Unit) {
    val description = stringResource(R.string.rest_time_button_description, exerciseName, durationText(seconds))
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = NextSetTheme.sizes.minTouchTarget)
            .semantics { contentDescription = description },
        shape = NextSetTheme.radius.md,
    ) {
        Text(
            text = stringResource(R.string.rest_time_button, durationText(seconds)),
            style = NextSetTheme.typography.label,
            color = NextSetTheme.colors.accent,
            maxLines = 1,
        )
    }
}

/**
 * 운동 중 화면의 휴식 바를 [restTimer]에 잇는다. 이 운동의 휴식일 때만 보인다. 남은 시간은 끝나는 시각에서 지금을 빼서 그린다.
 *
 * @param showNotificationButton 휴식 바에 '알림 켜기'를 보일지(REST-09).
 */
@Composable
fun WorkoutRestBar(
    restTimer: RestTimer,
    workoutId: Long,
    compact: Boolean,
    showNotificationButton: Boolean,
    onEnableNotifications: () -> Unit,
) {
    val current by restTimer.state.collectAsStateWithLifecycle()
    val state = current?.takeIf { it.workoutId == workoutId }
    var now by remember { mutableLongStateOf(restTimer.now()) }
    LaunchedEffect(state) {
        while (state != null) {
            now = restTimer.now()
            delay(TICK_MILLIS)
        }
    }
    // 내려가는 동안에도 마지막 모습을 그린다.
    val last = remember { arrayOfNulls<RestTimerState>(1) }
    if (state != null) last[0] = state
    val motion = NextSetMotion.standardSpatialDefault
    AnimatedVisibility(
        visible = state != null,
        enter = slideInVertically(motion.spec(IntOffset.VisibilityThreshold)) { it },
        exit = slideOutVertically(motion.spec(IntOffset.VisibilityThreshold)) { it },
    ) {
        val shown = state ?: last[0] ?: return@AnimatedVisibility
        RestTimerBar(
            remainingMillis = shown.remainingMillis(now),
            progress = shown.progress(now),
            nextSet = shown.nextSet,
            compact = compact,
            enabled = !shown.finished,
            canIncrease = shown.canIncrease(now),
            showNotificationButton = showNotificationButton,
            onDecrease = { restTimer.adjust(-RestTimerRules.STEP_SECONDS) },
            onIncrease = { restTimer.adjust(RestTimerRules.STEP_SECONDS) },
            onSkip = restTimer::skip,
            onEnableNotifications = onEnableNotifications,
        )
    }
}

private const val TICK_MILLIS = 200L

/**
 * 휴식 타이머 하단 바(docs/DESIGN.md `RestTimerBar`, REST-02). 윗변의 진행 막대, 윗줄의 남은 시간과 다음 세트와 '알림 켜기',
 * 아랫줄의 조절 버튼 셋. 숫자판이 열리면([compact]) 남은 시간과 조절 버튼만 한 줄로 남는다.
 *
 * @param nextSet "벤치 프레스 60kg × 10". "다음"은 여기서 붙인다.
 * @param enabled 끝나 "0:00"을 보이는 동안에는 버튼을 누를 수 없다.
 */
@Composable
fun RestTimerBar(
    remainingMillis: Long,
    progress: Float,
    nextSet: String?,
    compact: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    canIncrease: Boolean = true,
    showNotificationButton: Boolean = false,
    onEnableNotifications: () -> Unit = {},
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    Column(modifier = modifier.fillMaxWidth().background(colors.surface)) {
        ProgressLine(progress)
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                RemainingTime(remainingMillis)
                AdjustButtons(enabled, canIncrease, onDecrease, onIncrease, onSkip)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = NextSetTheme.sizes.minTouchTarget)
                    .padding(start = spacing.lg, end = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                RemainingTime(remainingMillis)
                if (nextSet != null) {
                    Text(
                        text = stringResource(R.string.rest_next_set, nextSet),
                        style = NextSetTheme.typography.caption,
                        color = colors.text2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (showNotificationButton) {
                    TextButton(
                        onClick = onEnableNotifications,
                        modifier = Modifier.heightIn(min = NextSetTheme.sizes.minTouchTarget),
                        shape = NextSetTheme.radius.md,
                    ) {
                        Text(
                            text = stringResource(R.string.notifications_enable),
                            style = NextSetTheme.typography.label,
                            color = colors.accent,
                            maxLines = 1,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                AdjustButtons(enabled, canIncrease, onDecrease, onIncrease, onSkip)
            }
            // 숫자판이 닫혀 있으면 바가 화면 맨 아래라 내비게이션 바만큼 띄운다. 숫자판이 열리면 숫자판이 띄운다.
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/** 진행 막대. 바의 윗변을 따라 4dp, 남은 비율만큼 `accent`, 나머지는 `line`. 장식이라 읽지 않는다. */
@Composable
private fun ProgressLine(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PROGRESS_HEIGHT)
            .background(NextSetTheme.colors.line)
            .clearAndSetSemantics {},
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(PROGRESS_HEIGHT)
                .background(NextSetTheme.colors.accent),
        )
    }
}

private val PROGRESS_HEIGHT = 4.dp

@Composable
private fun RemainingTime(remainingMillis: Long) {
    val text = remainingSecondsText(remainingMillis)
    val description = stringResource(R.string.rest_remaining_description, text)
    Text(
        text = text,
        style = NextSetTheme.typography.numeralMedium,
        color = NextSetTheme.colors.text,
        maxLines = 1,
        modifier = Modifier.semantics { contentDescription = description },
    )
}

@Composable
private fun RowScope.AdjustButtons(
    enabled: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onSkip: () -> Unit,
) {
    AdjustButton(
        text = stringResource(R.string.rest_decrease, RestTimerRules.STEP_SECONDS),
        description = stringResource(R.string.rest_decrease_description, RestTimerRules.STEP_SECONDS),
        enabled = enabled,
        onClick = onDecrease,
    )
    AdjustButton(
        text = stringResource(R.string.rest_increase, RestTimerRules.STEP_SECONDS),
        description = stringResource(R.string.rest_increase_description, RestTimerRules.STEP_SECONDS),
        enabled = enabled && canIncrease,
        onClick = onIncrease,
    )
    AdjustButton(
        text = stringResource(R.string.rest_skip),
        description = stringResource(R.string.rest_skip_description),
        enabled = enabled,
        onClick = onSkip,
    )
}

/** 조절 버튼. 폭을 똑같이 나눠 쓰고 높이는 48dp다. 아이콘 없이 값을 글자로 적는다. */
@Composable
private fun RowScope.AdjustButton(text: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = NextSetTheme.sizes.buttonHeight)
            .semantics { contentDescription = description },
        shape = NextSetTheme.radius.md,
        contentPadding = PaddingValues(horizontal = NextSetTheme.spacing.xs),
    ) {
        // 셋이 폭을 똑같이 나눠 쓴다. 글자를 키워 한 줄에 들어가지 않으면 `caption` 크기까지 줄이고, 그래도 넘치면 두 줄로 쓴다.
        Text(
            text = text,
            style = NextSetTheme.typography.label,
            maxLines = 2,
            textAlign = TextAlign.Center,
            autoSize = TextAutoSize.StepBased(
                minFontSize = NextSetTheme.typography.caption.fontSize,
                maxFontSize = NextSetTheme.typography.label.fontSize,
            ),
        )
    }
}

/**
 * [WorkoutRestBar]에 운동 중 화면의 상태를 잇는다. 다음 세트가 바뀌면 타이머(알림)에 알리고, 숫자판이 열리면 바를 줄인다.
 * [settingsRepository]가 있으면 '알림 켜기'를 다룬다(REST-09).
 */
@Composable
internal fun WorkoutRestBarHost(
    restTimer: RestTimer,
    viewModel: WorkoutViewModel,
    workoutId: Long,
    settingsRepository: SettingsRepository?,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rest by restTimer.state.collectAsStateWithLifecycle()
    val nextText = restNextSetText(state.nextSet)
    val restSetId = rest?.takeIf { it.workoutId == workoutId }?.setId
    LaunchedEffect(nextText, restSetId) {
        if (restSetId != null) restTimer.setNextSet(nextText)
    }
    val permission = settingsRepository?.let {
        rememberNotificationPermissionState(it, onAllowed = restTimer::onNotificationsAllowed)
    }
    WorkoutRestBar(
        restTimer = restTimer,
        workoutId = workoutId,
        compact = state.numberPad != null,
        showNotificationButton = permission?.showRestBarButton == true,
        onEnableNotifications = { permission?.requestFromRestBar() },
    )
}
