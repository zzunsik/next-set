package io.github.zzunsik.nextset.ui.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.repository.WorkoutRepository
import io.github.zzunsik.nextset.domain.rule.isFilled
import io.github.zzunsik.nextset.domain.rule.recordSetIds
import io.github.zzunsik.nextset.domain.rule.suggestedValue
import io.github.zzunsik.nextset.ui.component.NumberFieldKind
import io.github.zzunsik.nextset.ui.component.NumberPadEvent
import io.github.zzunsik.nextset.ui.component.NumberPadField
import io.github.zzunsik.nextset.ui.component.NumberPadKey
import io.github.zzunsik.nextset.ui.component.NumberPadSession
import io.github.zzunsik.nextset.ui.component.numberFieldKinds
import io.github.zzunsik.nextset.ui.workout.amountOf
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 숫자판이 편집하는 칸. 세트의 칸이거나 운동 시간(분)이다. */
sealed interface ReportFieldId {
    data class SetField(val setId: Long, val kind: NumberFieldKind) : ReportFieldId

    /** 운동 시간. 분으로 적는다. 횟수 칸과 같은 숫자판(±1)을 쓴다. */
    data object Minutes : ReportFieldId
}

sealed interface ReportDialog {
    /** "이 기록을 지울까요? 되돌릴 수 없어요." */
    data object Delete : ReportDialog

    data class RemoveExercise(val workoutExerciseId: Long, val name: String) : ReportDialog

    data class ExerciseNote(val workoutExerciseId: Long, val name: String, val note: String) : ReportDialog
}

data class ReportUiState(
    val loading: Boolean = true,
    /** 읽은 뒤에도 null이면 기록이 없어졌다. */
    val report: WorkoutReportUi? = null,
    /** 기록 상세의 수정 상태(HIST-05). */
    val editing: Boolean = false,
    val numberPad: NumberPadSession<ReportFieldId>? = null,
    val dialog: ReportDialog? = null,
)

/** 화면이 한 번만 처리하는 일. */
sealed interface ReportEvent {
    /** 세트를 지웠다. "되돌리기"가 있는 알림 줄을 보인다. */
    data object SetDeleted : ReportEvent

    /** 기록을 지웠다. 화면을 닫는다. */
    data object Deleted : ReportEvent
}

/**
 * 운동 요약(LOG-15)과 기록 상세·수정(HIST-02, HIST-03, HIST-05)의 상태. 두 화면은 같은 내용을 보인다.
 *
 * 숫자와 최고 기록 표시는 끝난 운동 전부([WorkoutRepository.observeFinishedWorkouts])에서 그때그때 계산한다.
 * 그래서 이 기록이나 다른 기록을 고치고 지우면 표시가 저절로 바뀐다(docs/architecture.md "계산해서 구한다").
 *
 * 수정 상태에서 세트는 칸이 모두 차 있으면 완료, 비어 있으면 완료하지 않은 상태로 둔다. 수정 상태에는 체크 버튼이 없기
 * 때문이다(docs/DESIGN.md "기록 상세·수정"). 수정을 마치거나 화면을 나갈 때([leave]) 빈 세트와 세트가 없는 종목을 지운다.
 *
 * @param isNew "지난 운동 적기"로 막 만든 기록. 완료한 세트 없이 나가면 기록을 지운다(제안).
 */
class WorkoutReportViewModel(
    private val workoutRepository: WorkoutRepository,
    private val workoutId: Long,
    startEditing: Boolean = false,
    private val isNew: Boolean = false,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val writes = Mutex()
    private val local = MutableStateFlow(Local(editing = startEditing))
    private val eventChannel = Channel<ReportEvent>(Channel.BUFFERED)
    private var lastDeleted: DeletedSet? = null

    val events: Flow<ReportEvent> = eventChannel.receiveAsFlow()

    /** 끝난 운동 전부와 최고 기록 세트. 화면이 보는 동안에만 읽는다. 화면이 사라지면 바로 멈춰
     * 데이터베이스를 닫은 뒤에 읽지 않는다(Eagerly로 두면 탭을 옮긴 뒤 남은 화면 상태가 닫힌 데이터베이스를 읽었다). */
    private val history: StateFlow<History?> = workoutRepository.observeFinishedWorkouts()
        .map { workouts -> History(workouts, recordSetIds(workouts)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val uiState: StateFlow<ReportUiState> = combine(history, local) { history, local ->
        if (history == null) {
            ReportUiState(loading = true, editing = local.editing)
        } else {
            ReportUiState(
                loading = false,
                report = buildWorkoutReport(workoutId, history.workouts, history.records, includeIncomplete = local.editing),
                editing = local.editing,
                numberPad = local.numberPad,
                dialog = local.dialog,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ReportUiState(editing = startEditing))

    /** 운동 메모 칸의 글자(LOG-11). 처음 한 번만 저장소에서 채우고 그 뒤로는 친 글자를 따른다. */
    var noteText: String by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            val workout = history.first { it != null }?.workouts?.firstOrNull { it.id == workoutId }
            noteText = workout?.note.orEmpty()
        }
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch { writes.withLock { block() } }
    }

    private fun stored(): Workout? = history.value?.workouts?.firstOrNull { it.id == workoutId }

    // ---- 수정 상태 ----

    /** "수정"과 "완료". 수정을 마치면 빈 세트와 세트가 없는 종목을 지운다. */
    fun onEditToggle() {
        val editing = !local.value.editing
        local.update { it.copy(editing = editing, numberPad = null, dialog = null) }
        if (!editing) write { prune() }
    }

    /** 화면을 나간다. 빈 세트를 지우고, 막 만든 기록에 완료한 세트가 없으면 기록을 지운 뒤 [onDone]을 부른다. */
    fun leave(onDone: () -> Unit) {
        local.update { it.copy(numberPad = null) }
        viewModelScope.launch {
            writes.withLock {
                prune()
                if (isNew) {
                    val workout = workoutRepository.observeWorkout(workoutId).first()
                    if (workout != null && workout.exercises.none { exercise -> exercise.sets.any { it.isCompleted } }) {
                        workoutRepository.deleteWorkout(workoutId)
                    }
                }
            }
            onDone()
        }
    }

    private suspend fun prune() {
        val workout = workoutRepository.observeWorkout(workoutId).first() ?: return
        workout.exercises.forEach { exercise ->
            val empty = exercise.sets.filter { !it.isCompleted }
            if (empty.size == exercise.sets.size) {
                workoutRepository.removeExercise(exercise.id)
            } else {
                empty.forEach { workoutRepository.deleteSet(it.id) }
            }
        }
    }

    // ---- 날짜, 시작 시각, 운동 시간 ----

    /** 날짜를 바꾼다. 시작 시각의 시와 분, 운동 시간은 그대로다. */
    fun onDateChange(date: LocalDate) {
        val workout = stored() ?: return
        val start = workout.startedAt.atZone(zone)
        moveStart(workout, ZonedDateTime.of(date, start.toLocalTime(), zone).toInstant())
    }

    /** 시작 시각을 바꾼다. 날짜와 운동 시간은 그대로다. */
    fun onStartTimeChange(time: LocalTime) {
        val workout = stored() ?: return
        val start = workout.startedAt.atZone(zone)
        moveStart(workout, ZonedDateTime.of(start.toLocalDate(), time, zone).toInstant())
    }

    private fun moveStart(workout: Workout, startedAt: Instant) {
        val finishedAt = workout.finishedAt ?: return
        val length = Duration.between(workout.startedAt, finishedAt)
        write { workoutRepository.updateWorkoutTimes(workoutId, startedAt, startedAt.plus(length)) }
    }

    fun onMinutesClick() {
        val report = uiState.value.report ?: return
        val field = NumberPadField<ReportFieldId>(ReportFieldId.Minutes, NumberFieldKind.REPS, report.stats.minutes.toInt())
        local.update { it.copy(numberPad = NumberPadSession.open(listOf(field), 0)) }
    }

    // ---- 세트 칸과 숫자판 ----

    fun onFieldClick(setId: Long, kind: NumberFieldKind) {
        val (block, row) = findSet(setId) ?: return
        local.update { state ->
            val open = state.numberPad
            val kinds = block.recordType.numberFieldKinds()
            val index = kinds.indexOf(kind)
            val focusedSet = (open?.focused?.id as? ReportFieldId.SetField)?.setId
            val session = if (open != null && focusedSet == setId) {
                open.focus(index)
            } else {
                NumberPadSession.open(
                    kinds.map { NumberPadField<ReportFieldId>(ReportFieldId.SetField(setId, it), it, row.value.amountOf(it)) },
                    index,
                )
            }
            state.copy(numberPad = session)
        }
    }

    fun onNumberPadKey(key: NumberPadKey) {
        val session = local.value.numberPad ?: return
        val result = session.press(key)
        local.update { it.copy(numberPad = result.session) }
        result.events.forEach { event ->
            if (event !is NumberPadEvent.ValueChanged) return@forEach
            when (val id = event.id) {
                ReportFieldId.Minutes -> event.amount?.let { minutes ->
                    val workout = stored() ?: return@let
                    write {
                        workoutRepository.updateWorkoutTimes(
                            workoutId,
                            workout.startedAt,
                            workout.startedAt.plus(Duration.ofMinutes(minutes.toLong())),
                        )
                    }
                }
                is ReportFieldId.SetField -> {
                    val fields = result.session?.fields ?: session.fields
                    val value = fields.fold(SetValue.EMPTY) { value, field ->
                        when (field.kind) {
                            NumberFieldKind.WEIGHT -> value.copy(weight = field.amount?.let { Weight(it) })
                            NumberFieldKind.REPS -> value.copy(reps = field.amount)
                            NumberFieldKind.DURATION -> value.copy(durationSeconds = field.amount)
                        }
                    }
                    write { saveSet(id.setId, value) }
                }
            }
        }
    }

    /** 값을 저장하고 칸이 모두 찼는지에 맞춰 완료 여부를 맞춘다. */
    private suspend fun saveSet(setId: Long, value: SetValue) {
        val workout = workoutRepository.observeWorkout(workoutId).first() ?: return
        val exercise = workout.exercises.firstOrNull { e -> e.sets.any { it.id == setId } } ?: return
        val set = exercise.sets.first { it.id == setId }
        if (set.value != value) workoutRepository.updateSet(setId, value)
        val filled = value.isFilled(exercise.recordType)
        when {
            filled && !set.isCompleted -> workoutRepository.completeSet(setId, workout.finishedAt ?: workout.startedAt)
            !filled && set.isCompleted -> workoutRepository.uncompleteSet(setId)
        }
    }

    // ---- 세트와 종목 ----

    /** 세트를 더한다. 값은 직전 기록, 목표, 바로 위 행 순으로 채운다(LOG-03, docs/DESIGN.md `SetRow`). */
    fun onAddSet(workoutExerciseId: Long) = write { addSetTo(workoutExerciseId) }

    private suspend fun addSetTo(workoutExerciseId: Long) {
        val before = workoutRepository.observeWorkout(workoutId).first() ?: return
        val exercise = before.exercises.firstOrNull { it.id == workoutExerciseId } ?: return
        val above = exercise.sets.maxByOrNull { it.position }?.value
        val id = workoutRepository.addSet(workoutExerciseId)
        val previous = workoutRepository.getPreviousSet(
            exercise.exercise.id,
            exercise.recordType,
            before.startedAt,
            exercise.sets.size,
        )
        saveSet(id, suggestedValue(exercise.recordType, previous, exercise.target, above))
    }

    fun onDeleteSet(setId: Long) {
        val workout = stored() ?: return
        val exercise = workout.exercises.firstOrNull { e -> e.sets.any { it.id == setId } } ?: return
        val set = exercise.sets.first { it.id == setId }
        lastDeleted = DeletedSet(exercise.id, exercise.sets.sortedBy { it.position }.indexOf(set), set.value, set.completedAt)
        local.update { state ->
            state.copy(numberPad = state.numberPad?.takeIf { (it.focused.id as? ReportFieldId.SetField)?.setId != setId })
        }
        write { workoutRepository.deleteSet(setId) }
        eventChannel.trySend(ReportEvent.SetDeleted)
    }

    fun onDeleteLastSet(workoutExerciseId: Long) {
        val last = uiState.value.report?.exercises?.firstOrNull { it.id == workoutExerciseId }?.sets?.lastOrNull() ?: return
        onDeleteSet(last.id)
    }

    fun onUndoDelete() {
        val deleted = lastDeleted ?: return
        lastDeleted = null
        write { workoutRepository.restoreSet(deleted.workoutExerciseId, deleted.position, deleted.value, deleted.completedAt) }
    }

    /** 종목 고르기에서 고른 종목을 끝에 더한다. 세트 하나를 미리 채워 만든다. */
    fun onExercisesPicked(exerciseIds: List<String>) {
        write {
            exerciseIds.forEach { exerciseId ->
                val id = workoutRepository.addExercise(workoutId, exerciseId, setCount = 0)
                addSetTo(id)
            }
        }
    }

    fun onRemoveExerciseRequest(workoutExerciseId: Long) {
        val block = uiState.value.report?.exercises?.firstOrNull { it.id == workoutExerciseId } ?: return
        showDialog(ReportDialog.RemoveExercise(block.id, block.name))
    }

    fun onRemoveExerciseConfirm(workoutExerciseId: Long) {
        dismissDialog()
        write { workoutRepository.removeExercise(workoutExerciseId) }
    }

    // ---- 메모(LOG-11) ----

    fun onExerciseNoteRequest(workoutExerciseId: Long) {
        val block = uiState.value.report?.exercises?.firstOrNull { it.id == workoutExerciseId } ?: return
        showDialog(ReportDialog.ExerciseNote(block.id, block.name, block.note.orEmpty()))
    }

    fun onExerciseNoteChange(workoutExerciseId: Long, note: String) {
        local.update { state ->
            val dialog = state.dialog
            if (dialog is ReportDialog.ExerciseNote && dialog.workoutExerciseId == workoutExerciseId) {
                state.copy(dialog = dialog.copy(note = note))
            } else {
                state
            }
        }
        write { workoutRepository.updateExerciseNote(workoutExerciseId, note) }
    }

    /** 운동 메모는 글자를 칠 때마다 저장한다(NF-09). */
    fun onNoteChange(note: String) {
        noteText = note
        write { workoutRepository.updateWorkoutNote(workoutId, note) }
    }

    // ---- 지우기 ----

    fun onDeleteRequest() = showDialog(ReportDialog.Delete)

    fun onDeleteConfirm() {
        dismissDialog()
        write {
            workoutRepository.deleteWorkout(workoutId)
            eventChannel.send(ReportEvent.Deleted)
        }
    }

    fun dismissDialog() = local.update { it.copy(dialog = null) }

    private fun showDialog(dialog: ReportDialog) = local.update { it.copy(dialog = dialog, numberPad = null) }

    private fun findSet(setId: Long): Pair<ReportExerciseUi, ReportSetUi>? {
        uiState.value.report?.exercises?.forEach { block ->
            block.sets.firstOrNull { it.id == setId }?.let { return block to it }
        }
        return null
    }

    private data class History(val workouts: List<Workout>, val records: Set<Long>)

    private data class Local(
        val editing: Boolean = false,
        val numberPad: NumberPadSession<ReportFieldId>? = null,
        val dialog: ReportDialog? = null,
    )

    private data class DeletedSet(
        val workoutExerciseId: Long,
        val position: Int,
        val value: SetValue,
        val completedAt: Instant?,
    )
}
