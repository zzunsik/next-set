package io.github.zzunsik.nextset.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.RoutineExercise
import io.github.zzunsik.nextset.domain.model.Settings
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.domain.repository.ExerciseRepository
import io.github.zzunsik.nextset.domain.repository.RoutineRepository
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import io.github.zzunsik.nextset.ui.component.NumberFieldKind
import io.github.zzunsik.nextset.ui.component.NumberPadEvent
import io.github.zzunsik.nextset.ui.component.NumberPadField
import io.github.zzunsik.nextset.ui.component.NumberPadKey
import io.github.zzunsik.nextset.ui.component.NumberPadSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 목표 입력 칸. 기록 방식에 따라 보이는 칸이 다르다([targetFields]). */
enum class TargetField(val kind: NumberFieldKind) {
    WEIGHT(NumberFieldKind.WEIGHT),
    REPS_MIN(NumberFieldKind.REPS),
    REPS_MAX(NumberFieldKind.REPS),
    DURATION(NumberFieldKind.DURATION),
}

/** 기록 방식에 맞는 목표 칸(RTN-03). 무게와 횟수는 무게, 횟수 하한, 상한. 횟수만은 하한, 상한. 시간은 시간. */
fun RecordType.targetFields(): List<TargetField> = when (this) {
    RecordType.WEIGHT_REPS -> listOf(TargetField.WEIGHT, TargetField.REPS_MIN, TargetField.REPS_MAX)
    RecordType.REPS -> listOf(TargetField.REPS_MIN, TargetField.REPS_MAX)
    RecordType.DURATION -> listOf(TargetField.DURATION)
}

/** 목표의 칸 값. 무게는 그램, 횟수는 회, 시간은 초다. */
fun Target.amountOf(field: TargetField): Int? = when (field) {
    TargetField.WEIGHT -> weight?.grams
    TargetField.REPS_MIN -> repsMin
    TargetField.REPS_MAX -> repsMax
    TargetField.DURATION -> durationSeconds
}

fun Target.with(field: TargetField, amount: Int?): Target = when (field) {
    TargetField.WEIGHT -> copy(weight = amount?.let(::Weight))
    TargetField.REPS_MIN -> copy(repsMin = amount)
    TargetField.REPS_MAX -> copy(repsMax = amount)
    TargetField.DURATION -> copy(durationSeconds = amount)
}

/** 종목 설정 화면이 그리는 상태. */
data class RoutineExerciseUiState(
    val exercise: Exercise? = null,
    val setCount: Int = DEFAULT_SET_COUNT,
    val target: Target = Target.NONE,
    /** 설정의 휴식 시간 기본값(SET-01). 종목에 따로 정하지 않으면 이 값을 쓴다. */
    val defaultRestSeconds: Int = Settings.DEFAULT_REST_SECONDS,
    /** 열린 숫자판. 닫혀 있으면 null. */
    val numberPad: NumberPadSession<TargetField>? = null,
    val loading: Boolean = true,
    /** 루틴 종목이 없다(뺐거나 루틴을 지웠다). */
    val missing: Boolean = false,
) {
    val fields: List<TargetField> get() = exercise?.recordType?.targetFields().orEmpty()

    /** 목표 횟수 하한이 상한보다 크다. 이 상태의 목표는 저장하지 않는다. */
    val repsRangeInvalid: Boolean get() = !target.hasValidRepsRange
    val canDecreaseSets: Boolean get() = setCount > MIN_SET_COUNT
    val canIncreaseSets: Boolean get() = setCount < MAX_SET_COUNT
}

/**
 * 루틴 종목 하나의 종목 설정(RTN-03, RTN-04, REST-03). 세트 수, 목표, 종목의 휴식 시간, "루틴에서 빼기".
 *
 * 세트 수와 목표는 화면에서 고친 값([draft])을 들고 있다가 고칠 때마다 저장한다(NF-09).
 * 목표 횟수 하한이 상한보다 크면 고칠 때까지 저장하지 않는다. 그동안 저장소에는 마지막으로 맞았던 값이 남는다.
 */
class RoutineExerciseViewModel(
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    settingsRepository: SettingsRepository,
    private val routineId: Long,
    private val routineExerciseId: Long,
) : ViewModel() {
    private data class Draft(val setCount: Int, val target: Target)

    private val writes = SerialWriter(viewModelScope)
    private val item: Flow<RoutineExercise?> = routineRepository.observeRoutine(routineId)
        .map { detail -> detail?.exercises?.find { it.id == routineExerciseId } }

    /** 화면에서 고친 세트 수와 목표. 저장소에서 처음 읽기 전에는 null이다. */
    private val draft = MutableStateFlow<Draft?>(null)
    private val numberPad = MutableStateFlow<NumberPadSession<TargetField>?>(null)

    /** 저장소에서 마지막으로 읽은 종목. 기록 방식과 휴식 시간을 고칠 대상을 안다. */
    private var exercise: Exercise? = null

    init {
        viewModelScope.launch {
            item.collect { latest ->
                if (latest == null) return@collect
                exercise = latest.exercise
                if (draft.value == null) draft.value = Draft(latest.setCount, latest.target)
            }
        }
    }

    val uiState: StateFlow<RoutineExerciseUiState> = combine(
        item,
        draft,
        settingsRepository.observeSettings().map { it.defaultRestSeconds }.onStart { emit(Settings.DEFAULT_REST_SECONDS) },
        numberPad,
    ) { item, draft, defaultRest, pad ->
        RoutineExerciseUiState(
            exercise = item?.exercise,
            setCount = draft?.setCount ?: item?.setCount ?: DEFAULT_SET_COUNT,
            target = draft?.target ?: item?.target ?: Target.NONE,
            defaultRestSeconds = defaultRest,
            numberPad = pad,
            loading = item != null && draft == null,
            missing = item == null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RoutineExerciseUiState())

    /** 세트 수를 [delta]만큼 바꾼다. 1 아래나 [MAX_SET_COUNT] 위로는 가지 않는다(RTN-04). */
    fun onSetCountChange(delta: Int) {
        val current = draft.value ?: return
        val next = (current.setCount + delta).coerceIn(MIN_SET_COUNT, MAX_SET_COUNT)
        if (next != current.setCount) update(current.copy(setCount = next))
    }

    /** 목표 칸을 눌렀다. 숫자판을 열거나, 열려 있으면 그 칸으로 옮긴다. */
    fun onFieldClick(field: TargetField) {
        val current = draft.value ?: return
        val recordType = currentRecordType() ?: return
        val fields = recordType.targetFields()
        val index = fields.indexOf(field).takeIf { it >= 0 } ?: return
        val open = numberPad.value
        numberPad.value = if (open != null && open.fields.map { it.id } == fields) {
            open.focus(index)
        } else {
            NumberPadSession.open(fields.map { NumberPadField(it, it.kind, current.target.amountOf(it)) }, index)
        }
    }

    /** 숫자판 키. 값이 바뀌면 저장한다. "완료"와 "숫자판 닫기"는 숫자판만 닫는다(docs/DESIGN.md `NumberPad`). */
    fun onNumberPadKey(key: NumberPadKey) {
        val session = numberPad.value ?: return
        val result = session.press(key)
        var current = draft.value ?: return
        result.events.forEach { event ->
            if (event is NumberPadEvent.ValueChanged) current = current.copy(target = current.target.with(event.id, event.amount))
        }
        numberPad.value = result.session
        if (current != draft.value) update(current)
    }

    /** 종목의 휴식 시간(REST-03). null이면 설정의 기본값을 따른다. 값은 종목에 하나라 다른 루틴과 운동 중에도 같다. */
    fun onRestChange(seconds: Int?) {
        val exerciseId = exercise?.id ?: return
        writes.enqueue { exerciseRepository.setRestSeconds(exerciseId, seconds) }
    }

    /** 루틴에서 뺀다(RTN-02). 끝나면 되돌릴 때 쓸 값을 [onRemoved]로 넘긴다. */
    fun remove(onRemoved: (RemovedRoutineExercise) -> Unit) {
        writes.enqueue {
            val removed = item.first() ?: return@enqueue
            val current = draft.value
            routineRepository.removeRoutineExercise(routineExerciseId)
            numberPad.value = null
            onRemoved(
                RemovedRoutineExercise(
                    exerciseId = removed.exercise.id,
                    exerciseName = removed.exercise.nameKo,
                    setCount = current?.setCount ?: removed.setCount,
                    target = current?.target?.takeIf { it.hasValidRepsRange } ?: removed.target,
                    position = removed.position,
                ),
            )
        }
    }

    private fun currentRecordType(): RecordType? = exercise?.recordType

    private fun update(next: Draft) {
        draft.value = next
        if (!next.target.hasValidRepsRange) return
        writes.enqueue { routineRepository.updateRoutineExercise(routineExerciseId, next.setCount, next.target) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
