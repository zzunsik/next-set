package io.github.zzunsik.nextset.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zzunsik.nextset.domain.model.Routine
import io.github.zzunsik.nextset.domain.model.WorkoutInProgressException
import io.github.zzunsik.nextset.domain.model.WorkoutSummary
import io.github.zzunsik.nextset.domain.repository.RoutineRepository
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import io.github.zzunsik.nextset.domain.repository.WorkoutRepository
import io.github.zzunsik.nextset.ui.routine.SerialWriter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 진행 중인 운동의 이어서 하기 카드(HOME-06, LOG-13). */
data class InProgressWorkoutUi(
    val workoutId: Long,
    val routineName: String?,
    val totalSets: Int,
    val completedSets: Int,
    val startedAt: Instant,
)

/** 홈의 루틴 하나. 카드와 행이 함께 쓴다. */
data class HomeRoutineUi(
    val routine: Routine,
    /** 루틴의 종목 이름. 루틴 순서대로. */
    val exerciseNames: List<String>,
    /** 이 루틴으로 마지막으로 운동한 날. 한 적이 없으면 null이다. */
    val lastDoneOn: LocalDate?,
)

/** 운동을 시작하려다 진행 중인 운동 때문에 멈춘 시작. */
sealed interface PendingStart {
    data class FromRoutine(val routineId: Long) : PendingStart

    data object Empty : PendingStart
}

sealed interface HomeDialog {
    /** "진행 중인 운동이 있어요."(docs/DESIGN.md "진행 중인 운동이 있을 때 다른 루틴을 시작하려 하면"). */
    data class InProgress(val workout: InProgressWorkoutUi, val pending: PendingStart) : HomeDialog

    /** 루틴 지우기 확인. */
    data class DeleteRoutine(val routineId: Long, val name: String) : HomeDialog
}

data class HomeUiState(
    val loading: Boolean = true,
    val inProgress: InProgressWorkoutUi? = null,
    /** 맨 위 카드의 루틴(HOME-01~03). 직접 고른 루틴이 있으면 그것, 없으면 다음 차례다. 루틴이 없으면 null이다. */
    val top: HomeRoutineUi? = null,
    /** 맨 위 루틴을 사용자가 직접 골랐다(HOME-03). */
    val topPinned: Boolean = false,
    /** 맨 위 카드 아래의 루틴 목록(HOME-04). 맨 위 카드의 루틴은 뺀다. 이어서 하기 카드가 맨 위면 모든 루틴이다. */
    val others: List<HomeRoutineUi> = emptyList(),
    /** 모든 루틴. 목록 순서대로(RTN-05). 순서 편집에서 쓴다. */
    val routines: List<HomeRoutineUi> = emptyList(),
    val dialog: HomeDialog? = null,
)

/**
 * 홈의 화면 상태(docs/DESIGN.md "홈"). 운동 중 화면(#24)이 쓰던 임시 홈 입구의 화면 상태를 넓힌 것이다.
 *
 * - 맨 위 카드, 루틴 행, "빈 운동 시작"은 한 번 눌러 운동을 시작한다(HOME-01, HOME-04, HOME-05).
 * - 진행 중인 운동이 있으면 맨 위가 이어서 하기 카드다(HOME-06). 이때 루틴 행이나 "빈 운동 시작"을 누르면
 *   바로 시작하지 않고 묻는다(HOME-01의 예외).
 *
 * @param newRoutineName "새 루틴"으로 만든 루틴의 이름. 문자열 리소스에서 받는다(NF-03).
 */
class HomeViewModel(
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val settingsRepository: SettingsRepository,
    private val newRoutineName: String,
    private val clock: () -> Instant = Instant::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val started = Channel<Long>(Channel.BUFFERED)
    private val writes = SerialWriter(viewModelScope)
    private val dialog = MutableStateFlow<HomeDialog?>(null)

    /** 시작했거나 이어서 할 운동. 화면은 이 운동의 운동 중 화면으로 간다. */
    val openWorkout: Flow<Long> = started.receiveAsFlow()

    /** 루틴마다 종목 이름. 루틴 목록이 바뀌면 다시 구독한다. */
    private val exerciseNames: Flow<Map<Long, List<String>>> = channelFlow {
        routineRepository.observeRoutines().collectLatest { routines ->
            if (routines.isEmpty()) {
                send(emptyMap())
                return@collectLatest
            }
            combine(routines.map { routineRepository.observeRoutine(it.id) }) { details ->
                details.filterNotNull().associate { detail ->
                    detail.routine.id to detail.exercises.map { it.exercise.nameKo }
                }
            }.collect { send(it) }
        }
    }

    /** 루틴마다 마지막으로 운동한 날. 기록은 최근 것이 앞이다(HIST-01). */
    private val lastDone: Flow<Map<Long, LocalDate>> = workoutRepository.observeHistory().map { history ->
        history.filter { it.routineId != null }
            .groupBy { it.routineId!! }
            .mapValues { (_, workouts) -> workouts.maxOf(WorkoutSummary::startedAt).atZone(zone).toLocalDate() }
    }

    private val routinesUi: Flow<List<HomeRoutineUi>> =
        combine(routineRepository.observeRoutines(), exerciseNames, lastDone) { routines, names, last ->
            routines.map { HomeRoutineUi(it, names[it.id].orEmpty(), last[it.id]) }
        }

    private val top: Flow<Pair<Long?, Boolean>> =
        combine(routineRepository.observeTopRoutine(), settingsRepository.observeSettings()) { top, settings ->
            top?.id to (top != null && top.id == settings.pinnedRoutineId)
        }

    val uiState: StateFlow<HomeUiState> = combine(
        workoutRepository.observeInProgressWorkout(),
        routinesUi,
        top,
        dialog,
    ) { workout, routines, (topId, pinned), dialog ->
        val inProgress = workout?.let {
            val sets = it.exercises.flatMap { exercise -> exercise.sets }
            InProgressWorkoutUi(it.id, it.routineName, sets.size, sets.count { set -> set.isCompleted }, it.startedAt)
        }
        val topRoutine = routines.firstOrNull { it.routine.id == topId }
        HomeUiState(
            loading = false,
            inProgress = inProgress,
            top = topRoutine,
            topPinned = pinned,
            others = if (inProgress != null) routines else routines.filterNot { it.routine.id == topId },
            routines = routines,
            dialog = dialog,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    fun onResume(workoutId: Long) {
        started.trySend(workoutId)
    }

    fun onStartRoutine(routineId: Long) = startOrAsk(PendingStart.FromRoutine(routineId))

    fun onStartEmpty() = startOrAsk(PendingStart.Empty)

    /** 진행 중인 운동이 있으면 묻고, 없으면 바로 시작한다. */
    private fun startOrAsk(pending: PendingStart) {
        val inProgress = uiState.value.inProgress
        if (inProgress != null) {
            dialog.value = HomeDialog.InProgress(inProgress, pending)
        } else {
            start(pending)
        }
    }

    /** 물음의 "이어서 하기". */
    fun onPromptResume() {
        val prompt = dialog.value as? HomeDialog.InProgress ?: return
        dialog.value = null
        started.trySend(prompt.workout.workoutId)
    }

    /**
     * 물음의 "끝내고 새로 시작". 진행 중인 운동을 완료한 세트만으로 저장하고(완료한 세트가 없으면 기록이 지워진다)
     * 새 운동을 시작한다. 운동 요약과 루틴 반영 물음(LOG-10)은 건너뛰고 루틴은 고치지 않는다.
     */
    fun onPromptFinishAndStart() {
        val prompt = dialog.value as? HomeDialog.InProgress ?: return
        dialog.value = null
        viewModelScope.launch {
            workoutRepository.finishWorkout(prompt.workout.workoutId, clock())
            start(prompt.pending)
        }
    }

    fun dismissDialog() = dialog.update { null }

    /** 루틴 행 메뉴의 "맨 위에 올리기"(HOME-03). */
    fun onPin(routineId: Long) {
        writes.enqueue { settingsRepository.setPinnedRoutine(routineId) }
    }

    /** 카드의 "자동 순서로"(HOME-03). */
    fun onUnpin() {
        writes.enqueue { settingsRepository.setPinnedRoutine(null) }
    }

    fun onDeleteRequest(routineId: Long) {
        val routine = uiState.value.routines.firstOrNull { it.routine.id == routineId } ?: return
        dialog.value = HomeDialog.DeleteRoutine(routineId, routine.routine.name)
    }

    /** 루틴을 지운다. 지난 운동 기록은 남고, 직접 고른 루틴이었으면 고른 것이 풀린다(저장소가 한다). */
    fun onDeleteConfirm() {
        val prompt = dialog.value as? HomeDialog.DeleteRoutine ?: return
        dialog.value = null
        writes.enqueue { routineRepository.deleteRoutine(prompt.routineId) }
    }

    /**
     * "새 루틴"과 "루틴 만들기". 목록 끝에 "새 루틴"이라는 이름으로 바로 저장하고(NF-09) 만든 루틴의 식별자를
     * [onCreated]로 넘긴다. 이름도 종목도 없이 루틴 편집을 나가면 루틴 편집이 이 루틴을 버린다.
     */
    fun createRoutine(onCreated: (Long) -> Unit) {
        writes.enqueue { onCreated(routineRepository.addRoutine(newRoutineName)) }
    }

    /** 순서 편집(RTN-05). 다음 차례 계산(HOME-02)이 이 순서를 쓴다. */
    fun reorder(orderedIds: List<Long>) {
        if (orderedIds == uiState.value.routines.map { it.routine.id }) return
        writes.enqueue { routineRepository.reorderRoutines(orderedIds) }
    }

    /** 그 사이에 다른 곳에서 운동이 시작됐으면 새로 시작하지 않고 그 운동을 이어서 한다. */
    private fun start(pending: PendingStart) {
        viewModelScope.launch {
            val id = try {
                when (pending) {
                    is PendingStart.FromRoutine -> workoutRepository.startFromRoutine(pending.routineId, clock())
                    PendingStart.Empty -> workoutRepository.startEmpty(clock())
                }
            } catch (e: WorkoutInProgressException) {
                e.workoutId
            }
            started.send(id)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
