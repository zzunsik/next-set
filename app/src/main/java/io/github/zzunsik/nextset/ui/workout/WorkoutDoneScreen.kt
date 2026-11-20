package io.github.zzunsik.nextset.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.repository.WorkoutRepository
import io.github.zzunsik.nextset.ui.component.PrimaryButton
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import java.time.Duration
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 끝낸 운동의 간단한 결과. */
data class WorkoutDoneUi(val routineName: String?, val completedSets: Int, val minutes: Long)

class WorkoutDoneViewModel(workoutRepository: WorkoutRepository, workoutId: Long) : ViewModel() {
    /** 읽기 전이나 기록이 없으면 null이다. */
    val uiState: StateFlow<WorkoutDoneUi?> = workoutRepository.observeWorkout(workoutId)
        .map { workout ->
            val finishedAt = workout?.finishedAt ?: return@map null
            WorkoutDoneUi(
                routineName = workout.routineName,
                completedSets = workout.exercises.sumOf { exercise -> exercise.sets.count { it.isCompleted } },
                minutes = Duration.between(workout.startedAt, finishedAt).toMinutes().coerceAtLeast(0),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/**
 * 운동을 끝낸 뒤의 완료 화면. 운동 요약(#27, LOG-15)이 만들어지기 전까지 두는 간단한 화면이다(#24 범위).
 * 결론 문장, 루틴 이름, 세트 수와 운동 시간, "확인". "확인"과 뒤로 가기 모두 홈으로 간다.
 */
@Composable
fun WorkoutDoneScreen(state: WorkoutDoneUi?, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = spacing.lg, vertical = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.workout_done_title),
            style = NextSetTheme.typography.headline,
            color = colors.text,
            modifier = Modifier.semantics { heading() },
        )
        if (state != null) {
            Text(
                text = state.routineName ?: stringResource(R.string.workout_empty_title),
                style = NextSetTheme.typography.body,
                color = colors.text,
            )
            Text(
                text = stringResource(R.string.workout_done_detail, state.completedSets, workoutDurationText(state.minutes)),
                style = NextSetTheme.typography.body,
                color = colors.text2,
            )
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = stringResource(R.string.action_confirm), onClick = onConfirm)
    }
}
