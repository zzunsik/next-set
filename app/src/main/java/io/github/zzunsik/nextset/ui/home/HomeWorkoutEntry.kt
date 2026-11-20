package io.github.zzunsik.nextset.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.Routine
import io.github.zzunsik.nextset.domain.model.WorkoutInProgressException
import io.github.zzunsik.nextset.domain.repository.RoutineRepository
import io.github.zzunsik.nextset.domain.repository.WorkoutRepository
import io.github.zzunsik.nextset.ui.component.PrimaryButton
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import io.github.zzunsik.nextset.ui.workout.elapsedText
import java.time.Instant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 진행 중인 운동의 이어서 하기 카드(HOME-06, LOG-13). */
data class InProgressWorkoutUi(
    val workoutId: Long,
    val routineName: String?,
    val totalSets: Int,
    val completedSets: Int,
    val startedAt: Instant,
)

data class HomeWorkoutUiState(
    val loading: Boolean = true,
    val inProgress: InProgressWorkoutUi? = null,
    val routines: List<Routine> = emptyList(),
)

/**
 * 홈의 임시 운동 입구. 진짜 홈(#23)이 다음 차례 루틴 카드, 루틴 목록, 이어서 하기 카드, "진행 중인 운동이 있어요" 시트를
 * 만들 때까지 운동을 시작하고 이어서 할 길만 둔다(#24).
 *
 * - 진행 중인 운동이 있으면 이어서 하기만 보인다. 다른 운동을 시작하는 물음(HOME-01의 예외)은 #23이 만든다.
 * - 없으면 루틴마다 시작 버튼과 "빈 운동 시작"을 보인다.
 */
class HomeWorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    routineRepository: RoutineRepository,
    private val clock: () -> Instant = Instant::now,
) : ViewModel() {
    private val started = Channel<Long>(Channel.BUFFERED)

    /** 시작했거나 이어서 할 운동. 화면은 이 운동의 운동 중 화면으로 간다. */
    val openWorkout: Flow<Long> = started.receiveAsFlow()

    val uiState: StateFlow<HomeWorkoutUiState> = combine(
        workoutRepository.observeInProgressWorkout(),
        routineRepository.observeRoutines(),
    ) { workout, routines ->
        HomeWorkoutUiState(
            loading = false,
            inProgress = workout?.let {
                val sets = it.exercises.flatMap { exercise -> exercise.sets }
                InProgressWorkoutUi(it.id, it.routineName, sets.size, sets.count { set -> set.isCompleted }, it.startedAt)
            },
            routines = routines,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeWorkoutUiState())

    fun onResume(workoutId: Long) {
        started.trySend(workoutId)
    }

    fun onStartEmpty() = start { workoutRepository.startEmpty(clock()) }

    fun onStartRoutine(routineId: Long) = start { workoutRepository.startFromRoutine(routineId, clock()) }

    /** 그 사이에 다른 곳에서 운동이 시작됐으면 새로 시작하지 않고 그 운동을 이어서 한다. */
    private fun start(block: suspend () -> Long) {
        viewModelScope.launch {
            val id = try {
                block()
            } catch (e: WorkoutInProgressException) {
                e.workoutId
            }
            started.send(id)
        }
    }
}

/** [HomeWorkoutViewModel]의 상태로 [HomeWorkoutEntry]를 그리고 운동 중 화면으로 보낸다. */
@Composable
fun HomeWorkoutEntryRoute(viewModel: HomeWorkoutViewModel, onOpenWorkout: (Long) -> Unit, now: Instant = Instant.now()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnOpen by rememberUpdatedState(onOpenWorkout)
    LaunchedEffect(viewModel) { viewModel.openWorkout.collect { currentOnOpen(it) } }
    HomeWorkoutEntry(
        state = state,
        now = now,
        onResume = viewModel::onResume,
        onStartRoutine = viewModel::onStartRoutine,
        onStartEmpty = viewModel::onStartEmpty,
    )
}

@Composable
fun HomeWorkoutEntry(
    state: HomeWorkoutUiState,
    now: Instant,
    onResume: (Long) -> Unit,
    onStartRoutine: (Long) -> Unit,
    onStartEmpty: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.loading) return
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    val inProgress = state.inProgress
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        if (inProgress != null) {
            // 이어서 하기 카드(docs/DESIGN.md `NextRoutineCard`의 "진행 중" 상태).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, NextSetTheme.radius.md)
                    .padding(spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(stringResource(R.string.home_resume_label), style = NextSetTheme.typography.caption, color = colors.text2)
                Text(
                    text = inProgress.routineName ?: stringResource(R.string.workout_empty_title),
                    style = NextSetTheme.typography.headline,
                    color = colors.text,
                )
                Text(
                    text = stringResource(R.string.home_resume_progress, inProgress.totalSets, inProgress.completedSets) +
                        " · " + elapsedText(inProgress.startedAt, now),
                    style = NextSetTheme.typography.caption,
                    color = colors.text2,
                )
                PrimaryButton(
                    text = stringResource(R.string.home_resume),
                    onClick = { onResume(inProgress.workoutId) },
                    modifier = Modifier.padding(top = spacing.sm),
                )
            }
        } else {
            state.routines.forEach { routine ->
                val description = stringResource(R.string.home_start_routine_description, routine.name)
                TextButton(
                    onClick = { onStartRoutine(routine.id) },
                    modifier = Modifier
                        .heightIn(min = NextSetTheme.sizes.buttonHeight)
                        .semantics { contentDescription = description },
                    shape = NextSetTheme.radius.md,
                ) {
                    Text(text = routine.name, style = NextSetTheme.typography.label)
                }
            }
            TextButton(
                onClick = onStartEmpty,
                modifier = Modifier.heightIn(min = NextSetTheme.sizes.buttonHeight),
                shape = NextSetTheme.radius.md,
            ) {
                Text(text = stringResource(R.string.home_start_empty), style = NextSetTheme.typography.label)
            }
        }
    }
}
