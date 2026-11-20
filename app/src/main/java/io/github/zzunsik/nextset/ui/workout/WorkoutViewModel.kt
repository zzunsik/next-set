package io.github.zzunsik.nextset.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zzunsik.nextset.domain.model.FinishResult
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Settings
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.repository.ExerciseRepository
import io.github.zzunsik.nextset.domain.repository.RoutineRepository
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import io.github.zzunsik.nextset.domain.repository.WorkoutRepository
import io.github.zzunsik.nextset.domain.rule.RoutineUpdatePlan
import io.github.zzunsik.nextset.domain.rule.isFilled
import io.github.zzunsik.nextset.domain.rule.planRoutineUpdate
import io.github.zzunsik.nextset.domain.rule.suggestedValue
import io.github.zzunsik.nextset.ui.component.NumberFieldKind
import io.github.zzunsik.nextset.ui.component.NumberPadEvent
import io.github.zzunsik.nextset.ui.component.NumberPadField
import io.github.zzunsik.nextset.ui.component.NumberPadKey
import io.github.zzunsik.nextset.ui.component.NumberPadSession
import io.github.zzunsik.nextset.ui.component.numberFieldKinds
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 운동 중 화면이 한 번만 처리하는 일. */
sealed interface WorkoutEvent {
    /** 최고 기록을 넘는 세트를 완료했다(LOG-14). 판정은 저장소가 했다. */
    data class NewRecord(val exerciseName: String) : WorkoutEvent

    /**
     * 세트를 완료했다. 휴식 타이머(#26)가 이 신호로 시작한다(REST-01).
     * [restSeconds]는 종목의 휴식 시간이고 null이면 설정의 기본값을 쓴다(REST-03).
     */
    data class SetCompleted(val setId: Long, val workoutExerciseId: Long, val restSeconds: Int?) : WorkoutEvent

    /** 완료를 풀었다. 그 세트로 시작한 휴식 타이머가 멈춘다(docs/DESIGN.md `SetRow`). */
    data class SetUncompleted(val setId: Long) : WorkoutEvent

    /** 세트를 지웠다. "되돌리기"가 있는 알림 줄을 보인다. */
    data object SetDeleted : WorkoutEvent

    /** 화면을 떠난다. */
    data class Exit(val destination: WorkoutExit) : WorkoutEvent
}

sealed interface WorkoutExit {
    /** 버렸거나 운동이 없어졌다. 홈으로 간다. */
    data object Home : WorkoutExit

    /** 끝냈다. 운동 요약으로 간다. */
    data class Finished(val workoutId: Long) : WorkoutExit
}

/**
 * 운동 중 화면의 상태(LOG-01~LOG-14). 화면은 데이터베이스에서 흘러온 운동을 그린다.
 * 입력 한 번이 쓰기 한 번이고(NF-09) 쓰기는 누른 순서대로 한다([writes]).
 *
 * 화면이 들고 있는 것은 열린 숫자판, 대화상자, 이번에 세운 최고 기록 표시뿐이다. 값과 완료 여부는 모두 저장돼 있어서
 * 화면을 없앴다 다시 만들어도, 프로세스가 죽었다 살아나도 그대로다(LOG-13).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val workoutId: Long,
    private val clock: () -> Instant = Instant::now,
    /** 기본 휴식 시간을 읽는다. 없으면 기본값(2분)을 쓴다. */
    private val settingsRepository: SettingsRepository? = null,
    /** 종목의 휴식 시간을 고친다(REST-03). 없으면 고르기만 하고 저장하지 않는다. */
    private val exerciseRepository: ExerciseRepository? = null,
) : ViewModel() {
    private val writes = Mutex()
    private val previousCache = mutableMapOf<Pair<String, RecordType>, List<SetValue>>()
    private val local = MutableStateFlow(LocalState())
    private val eventChannel = Channel<WorkoutEvent>(Channel.BUFFERED)
    private var lastDeleted: DeletedSet? = null
    private var exited = false

    val events: Flow<WorkoutEvent> = eventChannel.receiveAsFlow()

    private val defaultRest: Flow<Int> = settingsRepository?.observeSettings()?.map { it.defaultRestSeconds }
        ?.onStart { emit(Settings.DEFAULT_REST_SECONDS) }
        ?: flowOf(Settings.DEFAULT_REST_SECONDS)

    /** 저장된 운동과 그 종목들의 직전 기록. 운동이 없어지면 null이다. */
    private val stored: StateFlow<Stored?> = workoutRepository.observeWorkout(workoutId)
        .mapLatest { workout -> workout?.let { Stored(it, previousOf(it)) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<WorkoutUiState> = combine(stored, local, defaultRest) { stored, local, defaultRest ->
        if (stored == null) {
            WorkoutUiState(loading = true, dialog = local.dialog, defaultRestSeconds = defaultRest)
        } else {
            buildWorkoutUiState(stored.workout, stored.previous, local.numberPad, local.records, local.dialog)
                .copy(defaultRestSeconds = defaultRest)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WorkoutUiState())

    init {
        // 운동이 지워졌거나(다른 곳에서 버림) 이미 끝났으면 홈으로 간다.
        viewModelScope.launch {
            workoutRepository.observeWorkout(workoutId).collect { workout ->
                if (workout == null || !workout.isInProgress) exit(WorkoutExit.Home)
            }
        }
    }

    private suspend fun previousOf(workout: Workout): Map<Long, List<SetValue>> =
        workout.exercises.associate { exercise ->
            val key = exercise.exercise.id to exercise.recordType
            exercise.id to previousCache.getOrPut(key) {
                workoutRepository.getPreviousSets(exercise.exercise.id, exercise.recordType, workout.startedAt)
            }
        }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch { writes.withLock { block() } }
    }

    private fun exit(destination: WorkoutExit) {
        if (exited) return
        exited = true
        eventChannel.trySend(WorkoutEvent.Exit(destination))
    }

    // ---- 세트 칸과 숫자판 ----

    /** 칸을 눌렀다. 같은 행이 열려 있으면 그 칸으로 옮기고, 아니면 그 행의 숫자판을 연다. */
    fun onFieldClick(setId: Long, kind: NumberFieldKind) {
        val (block, row) = uiState.value.findSet(setId) ?: return
        local.update { state ->
            val open = state.numberPad
            val kinds = block.recordType.numberFieldKinds()
            val index = kinds.indexOf(kind)
            val session = if (open != null && open.focused.id.setId == setId) {
                open.focus(index)
            } else {
                NumberPadSession.open(fieldsOf(row.id, row.value, block.recordType), index)
            }
            state.copy(numberPad = session)
        }
    }

    fun onNumberPadKey(key: NumberPadKey) {
        val session = local.value.numberPad ?: return
        val result = session.press(key)
        local.update { it.copy(numberPad = result.session) }
        result.events.forEach { event ->
            when (event) {
                is NumberPadEvent.ValueChanged -> {
                    val fields = result.session?.fields ?: session.fields
                    val setId = event.id.setId
                    val value = valueOf(fields)
                    write { workoutRepository.updateSet(setId, value) }
                }
                // "완료"는 세트를 완료하고 숫자판을 닫는다(docs/DESIGN.md `NumberPad`).
                NumberPadEvent.Finished -> completeSet(session.focused.id.setId, session.fields)
                is NumberPadEvent.FocusMoved, NumberPadEvent.Closed -> Unit
            }
        }
    }

    // ---- 완료 체크 ----

    /** 체크 버튼. 안 한 세트는 완료하고(LOG-04), 완료한 세트는 완료를 푼다(제안). */
    fun onCheck(setId: Long) {
        val (_, row) = uiState.value.findSet(setId) ?: return
        if (row.completed) {
            local.update { it.copy(records = it.records - setId) }
            write { workoutRepository.uncompleteSet(setId) }
            eventChannel.trySend(WorkoutEvent.SetUncompleted(setId))
        } else {
            val pad = local.value.numberPad?.takeIf { it.focused.id.setId == setId }
            completeSet(setId, pad?.fields)
        }
    }

    /**
     * 세트를 완료한다. 값을 고치지 않았으면 미리 채운 값이 그대로 기록이 된다(LOG-04).
     * 빈 칸이 있으면 완료하지 않고 첫 빈 칸의 숫자판을 연다(docs/DESIGN.md `SetRow` "값 없음").
     */
    private fun completeSet(setId: Long, editing: List<NumberPadField<SetFieldId>>?) {
        val (block, row) = uiState.value.findSet(setId) ?: return
        val value = editing?.let(::valueOf) ?: row.value
        if (!value.isFilled(block.recordType)) {
            val fields = editing ?: fieldsOf(setId, value, block.recordType)
            val empty = fields.indexOfFirst { it.amount == null }
            local.update { it.copy(numberPad = NumberPadSession.open(fields, empty)) }
            return
        }
        local.update { state -> state.copy(numberPad = state.numberPad?.takeIf { it.focused.id.setId != setId }) }
        val restSeconds = storedExercise(block.id)?.exercise?.restSeconds
        write {
            val completion = workoutRepository.completeSet(setId, clock())
            if (completion.isNewRecord) {
                local.update { it.copy(records = it.records + setId) }
                eventChannel.send(WorkoutEvent.NewRecord(block.name))
            }
            eventChannel.send(WorkoutEvent.SetCompleted(setId, block.id, restSeconds))
        }
    }

    // ---- 세트 더하고 지우기(LOG-09) ----

    /**
     * 세트를 더한다. 값은 직전 기록, 목표 순으로 미리 채우고(LOG-03) 그래도 빈 칸은 바로 위 행의 값으로 채운다
     * (docs/DESIGN.md `SetRow`).
     */
    fun onAddSet(workoutExerciseId: Long) {
        val block = uiState.value.exercises.firstOrNull { it.id == workoutExerciseId } ?: return
        val above = block.sets.lastOrNull()?.value
        val previous = stored.value?.previous?.get(workoutExerciseId)?.getOrNull(block.sets.size)
        val suggestion = suggestedValue(block.recordType, previous, block.target, above)
        write {
            val id = workoutRepository.addSet(workoutExerciseId)
            val created = workoutRepository.observeWorkout(workoutId).first()
                ?.exercises?.firstOrNull { it.id == workoutExerciseId }?.sets?.firstOrNull { it.id == id }
            if (created != null && created.value != suggestion) workoutRepository.updateSet(id, suggestion)
        }
    }

    /** 세트를 지운다. "되돌리기"로 같은 자리에 되살릴 수 있다. */
    fun onDeleteSet(setId: Long) {
        val (block, row) = uiState.value.findSet(setId) ?: return
        val stored = storedExercise(block.id)?.sets?.firstOrNull { it.id == setId } ?: return
        lastDeleted = DeletedSet(block.id, row.position, stored.value, stored.completedAt, row.isRecord)
        local.update { it.copy(records = it.records - setId) }
        write { workoutRepository.deleteSet(setId) }
        eventChannel.trySend(WorkoutEvent.SetDeleted)
    }

    fun onDeleteLastSet(workoutExerciseId: Long) {
        val last = uiState.value.exercises.firstOrNull { it.id == workoutExerciseId }?.sets?.lastOrNull() ?: return
        onDeleteSet(last.id)
    }

    fun onUndoDelete() {
        val deleted = lastDeleted ?: return
        lastDeleted = null
        write {
            val id = workoutRepository.restoreSet(deleted.workoutExerciseId, deleted.position, deleted.value, deleted.completedAt)
            if (deleted.wasRecord) local.update { it.copy(records = it.records + id) }
        }
    }

    // ---- 종목(LOG-10) ----

    /** 종목 고르기에서 고른 종목을 고른 순서대로 끝에 더한다. 세트 하나를 미리 채워 만든다. */
    fun onExercisesPicked(exerciseIds: List<String>) {
        write { exerciseIds.forEach { workoutRepository.addExercise(workoutId, it) } }
    }

    fun onRemoveExerciseRequest(workoutExerciseId: Long) {
        val block = uiState.value.exercises.firstOrNull { it.id == workoutExerciseId } ?: return
        showDialog(WorkoutDialog.RemoveExercise(block.id, block.name))
    }

    fun onRemoveExerciseConfirm(workoutExerciseId: Long) {
        dismissDialog()
        write { workoutRepository.removeExercise(workoutExerciseId) }
    }

    /** 종목을 [delta]칸 옮긴다(-1 위로, +1 아래로). */
    fun onMoveExercise(workoutExerciseId: Long, delta: Int) {
        val ids = uiState.value.exercises.map { it.id }.toMutableList()
        val from = ids.indexOf(workoutExerciseId)
        val to = from + delta
        if (from < 0 || to !in ids.indices) return
        ids.add(to, ids.removeAt(from))
        write { workoutRepository.reorderExercises(workoutId, ids) }
    }

    /**
     * 종목 순서 바꾸기 대화상자에서 끌어 놓은 순서([orderedIds])를 저장한다. 바로 저장한다(NF-09).
     * 순서가 그대로이거나 지금 종목과 다른 목록(그 사이에 종목을 더하거나 뺐다)이면 쓰지 않는다.
     */
    fun onReorderExercises(orderedIds: List<Long>) {
        val ids = uiState.value.exercises.map { it.id }
        if (orderedIds == ids || orderedIds.size != ids.size || orderedIds.toSet() != ids.toSet()) return
        write { workoutRepository.reorderExercises(workoutId, orderedIds) }
    }

    // ---- 메모(LOG-11) ----

    fun onExerciseNoteRequest(workoutExerciseId: Long) {
        val block = uiState.value.exercises.firstOrNull { it.id == workoutExerciseId } ?: return
        showDialog(WorkoutDialog.ExerciseNote(block.id, block.name, block.note.orEmpty()))
    }

    /** 메모는 글자를 칠 때마다 저장한다(NF-09). */
    fun onExerciseNoteChange(workoutExerciseId: Long, note: String) {
        local.update { state ->
            val dialog = state.dialog
            if (dialog is WorkoutDialog.ExerciseNote && dialog.workoutExerciseId == workoutExerciseId) {
                state.copy(dialog = dialog.copy(note = note))
            } else {
                state
            }
        }
        write { workoutRepository.updateExerciseNote(workoutExerciseId, note) }
    }

    fun onWorkoutNoteRequest() = showDialog(WorkoutDialog.WorkoutNote(uiState.value.note.orEmpty()))

    fun onWorkoutNoteChange(note: String) {
        local.update { state ->
            if (state.dialog is WorkoutDialog.WorkoutNote) state.copy(dialog = WorkoutDialog.WorkoutNote(note)) else state
        }
        write { workoutRepository.updateWorkoutNote(workoutId, note) }
    }

    fun onReorderRequest() = showDialog(WorkoutDialog.Reorder)

    // ---- 휴식 시간(REST-03) ----

    /** 종목 머리의 "휴식 2:00". 이 종목의 휴식 시간을 고르는 대화상자를 연다. */
    fun onRestTimeRequest(workoutExerciseId: Long) {
        val block = uiState.value.exercises.firstOrNull { it.id == workoutExerciseId } ?: return
        showDialog(WorkoutDialog.RestTime(block.exerciseId, block.name, block.restSeconds))
    }

    /** 고르면 바로 저장하고 닫는다. null이면 기본값을 따른다. 지금 도는 휴식은 그대로다. */
    fun onRestTimeChange(exerciseId: String, seconds: Int?) {
        dismissDialog()
        val repository = exerciseRepository ?: return
        write { repository.setRestSeconds(exerciseId, seconds) }
    }

    fun dismissDialog() = local.update { it.copy(dialog = null) }

    private fun showDialog(dialog: WorkoutDialog) = local.update { it.copy(dialog = dialog, numberPad = null) }

    // ---- 끝내기와 버리기 ----

    /**
     * 끝내기. docs/DESIGN.md "끝내기의 순서"대로 맞는 조건만 차례로 묻는다.
     * 1. 완료한 세트가 없다 → 버릴지 묻는다(제안: 빈 기록을 남기지 않는다).
     * 2. 완료하지 않은 세트가 있다 → 기록에 남지 않는다고 알린다.
     * 3. 종목 구성이나 세트 수가 루틴과 다르다 → 루틴에 반영할지 묻는다(LOG-10).
     */
    fun onFinishRequest() {
        val workout = stored.value?.workout ?: return
        val sets = workout.exercises.flatMap { it.sets }
        val completed = sets.count { it.isCompleted }
        when {
            completed == 0 -> showDialog(WorkoutDialog.NoCompletedSets)
            completed < sets.size -> showDialog(WorkoutDialog.IncompleteSets(sets.size - completed))
            else -> askRoutineChange()
        }
    }

    /** 2번 물음에서 "끝내기". */
    fun onIncompleteConfirm() = askRoutineChange()

    private fun askRoutineChange() {
        dismissDialog()
        write {
            if (routinePlan() != null) showDialog(WorkoutDialog.RoutineChange) else finish(applyToRoutine = false)
        }
    }

    /** 3번 물음의 답. [apply]가 참이면 "루틴에 반영"이다. */
    fun onRoutineChangeAnswer(apply: Boolean) {
        dismissDialog()
        write { finish(applyToRoutine = apply) }
    }

    /** 루틴과 달라진 내용. 빈 운동이거나 루틴이 지워졌거나 달라진 것이 없으면 null이다. */
    private suspend fun routinePlan(): RoutineUpdatePlan? {
        val workout = workoutRepository.observeWorkout(workoutId).first() ?: return null
        val routineId = workout.routineId ?: return null
        val routine = routineRepository.observeRoutine(routineId).first() ?: return null
        return planRoutineUpdate(workout, routine)
    }

    private suspend fun finish(applyToRoutine: Boolean) {
        if (applyToRoutine) routinePlan()?.let { routineRepository.applyRoutineUpdate(it) }
        exited = true
        val result = workoutRepository.finishWorkout(workoutId, clock())
        eventChannel.send(
            WorkoutEvent.Exit(
                if (result == FinishResult.FINISHED) WorkoutExit.Finished(workoutId) else WorkoutExit.Home,
            ),
        )
    }

    fun onDiscardRequest() = showDialog(WorkoutDialog.Discard)

    /** 운동을 버린다. 기록을 남기지 않는다. 확인을 받은 뒤에 부른다. */
    fun onDiscardConfirm() {
        dismissDialog()
        write {
            exited = true
            workoutRepository.discardWorkout(workoutId)
            eventChannel.send(WorkoutEvent.Exit(WorkoutExit.Home))
        }
    }

    // ---- 도움 ----

    private fun storedExercise(workoutExerciseId: Long) =
        stored.value?.workout?.exercises?.firstOrNull { it.id == workoutExerciseId }

    private fun fieldsOf(setId: Long, value: SetValue, type: RecordType): List<NumberPadField<SetFieldId>> =
        type.numberFieldKinds().map { kind -> NumberPadField(SetFieldId(setId, kind), kind, value.amountOf(kind)) }

    private fun valueOf(fields: List<NumberPadField<SetFieldId>>): SetValue =
        fields.fold(SetValue.EMPTY) { value, field ->
            when (field.kind) {
                NumberFieldKind.WEIGHT -> value.copy(weight = field.amount?.let { Weight(it) })
                NumberFieldKind.REPS -> value.copy(reps = field.amount)
                NumberFieldKind.DURATION -> value.copy(durationSeconds = field.amount)
            }
        }

    private data class Stored(val workout: Workout, val previous: Map<Long, List<SetValue>>)

    private data class LocalState(
        val numberPad: NumberPadSession<SetFieldId>? = null,
        val dialog: WorkoutDialog? = null,
        /** 이번 화면에서 최고 기록을 넘은 세트. 판정은 저장소가 한다(LOG-14). */
        val records: Set<Long> = emptySet(),
    )

    private data class DeletedSet(
        val workoutExerciseId: Long,
        val position: Int,
        val value: SetValue,
        val completedAt: Instant?,
        val wasRecord: Boolean,
    )
}
