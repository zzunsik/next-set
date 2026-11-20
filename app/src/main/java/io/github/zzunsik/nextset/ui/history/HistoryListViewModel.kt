package io.github.zzunsik.nextset.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zzunsik.nextset.domain.model.Routine
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.repository.RoutineRepository
import io.github.zzunsik.nextset.domain.repository.WorkoutRepository
import io.github.zzunsik.nextset.domain.rule.WorkoutStats
import io.github.zzunsik.nextset.domain.rule.isFilled
import io.github.zzunsik.nextset.domain.rule.recordSetIds
import io.github.zzunsik.nextset.domain.rule.stats
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 기록 행 하나(docs/DESIGN.md "기록 목록"). 날짜, 루틴 이름, "48분 · 15세트 · 4,250kg", 최고 기록이 있으면 배지. */
data class HistoryRowUi(
    val id: Long,
    val routineName: String?,
    val date: LocalDate,
    val stats: WorkoutStats,
    val hasRecord: Boolean,
)

/** 달 머리 "2026년 11월"과 그 달의 기록 행. */
data class HistoryMonthUi(val month: YearMonth, val rows: List<HistoryRowUi>)

/** "지난 운동 적기"에서 날짜를 고른 뒤 루틴이나 "빈 기록"을 고르는 대화상자. */
data class AddPastDialog(val date: LocalDate, val routines: List<Routine>)

data class HistoryUiState(
    val loading: Boolean = true,
    /** 최근 달이 위다. 달 안에서도 최근 기록이 위다(HIST-01). */
    val months: List<HistoryMonthUi> = emptyList(),
    /** 날짜에 연도를 붙일지 가르는 올해. */
    val today: LocalDate = LocalDate.MIN,
    val addPast: AddPastDialog? = null,
)

/**
 * 기록 목록(HIST-01)과 지난 운동 적기(HIST-06)의 상태.
 * 목록은 끝난 운동 전부에서 계산한다. 기록을 고치거나 지우면 행의 숫자와 배지가 저절로 바뀐다.
 */
class HistoryListViewModel(
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val clock: () -> Instant = Instant::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val addPast = MutableStateFlow<AddPastDialog?>(null)
    private val openChannel = Channel<Long>(Channel.BUFFERED)

    /** 지난 운동을 만들었다. 기록 상세를 수정 상태로 연다. */
    val openCreated: Flow<Long> = openChannel.receiveAsFlow()

    val uiState: StateFlow<HistoryUiState> = combine(workoutRepository.observeFinishedWorkouts(), addPast) { workouts, dialog ->
        val records = recordSetIds(workouts)
        val rows = workouts
            .sortedWith(compareByDescending<Workout> { it.startedAt }.thenByDescending { it.id })
            .map { workout ->
                HistoryRowUi(
                    id = workout.id,
                    routineName = workout.routineName,
                    date = workout.startedAt.atZone(zone).toLocalDate(),
                    stats = workout.stats(),
                    hasRecord = workout.exercises.any { exercise -> exercise.sets.any { it.id in records } },
                )
            }
        HistoryUiState(
            loading = false,
            months = rows.groupBy { YearMonth.from(it.date) }.map { (month, monthRows) -> HistoryMonthUi(month, monthRows) },
            today = clock().atZone(zone).toLocalDate(),
            addPast = dialog,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HistoryUiState())

    /** 날짜를 골랐다. 루틴이나 "빈 기록"을 고르는 대화상자를 연다. 오늘보다 뒤의 날짜는 받지 않는다. */
    fun onDatePicked(date: LocalDate) {
        val today = clock().atZone(zone).toLocalDate()
        if (date.isAfter(today)) return
        viewModelScope.launch {
            addPast.value = AddPastDialog(date, routineRepository.observeRoutines().first())
        }
    }

    fun dismissAddPast() = addPast.update { null }

    /**
     * 지난 운동을 만든다(HIST-06). [routineId]가 null이면 빈 기록이다.
     * 시작 시각은 고른 날의 지금 시각에서 한 시간 앞, 운동 시간은 한 시간이다. 그러면 오늘을 골라도 끝난 시각이 지금보다
     * 뒤가 아니다(제안). 시각은 기록 상세에서 고친다. 루틴에서 미리 채운 세트 가운데 값이 찬 세트는 완료로 둔다.
     */
    fun onAddPast(routineId: Long?) {
        val dialog = addPast.value ?: return
        addPast.value = null
        viewModelScope.launch {
            val now = ZonedDateTime.ofInstant(clock(), zone)
            val finishedAt = ZonedDateTime.of(dialog.date, now.toLocalTime(), zone).toInstant()
            val startedAt = finishedAt.minus(PAST_WORKOUT_LENGTH)
            val id = workoutRepository.addPastWorkout(startedAt, finishedAt, routineId)
            workoutRepository.observeWorkout(id).first()?.exercises?.forEach { exercise ->
                exercise.sets.filter { it.value.isFilled(exercise.recordType) }.forEach {
                    workoutRepository.completeSet(it.id, finishedAt)
                }
            }
            openChannel.send(id)
        }
    }

    private companion object {
        val PAST_WORKOUT_LENGTH: Duration = Duration.ofHours(1)
    }
}
