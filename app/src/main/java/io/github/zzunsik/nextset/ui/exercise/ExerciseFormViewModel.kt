package io.github.zzunsik.nextset.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.ExerciseDeleteResult
import io.github.zzunsik.nextset.domain.model.ExerciseDraft
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SubPart
import io.github.zzunsik.nextset.domain.repository.ExerciseRepository
import io.github.zzunsik.nextset.domain.rule.hasNameConflict
import io.github.zzunsik.nextset.domain.rule.isValidSubPart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 종목 추가·수정 화면이 그리는 상태. */
data class ExerciseFormUiState(
    val isEdit: Boolean = false,
    /** 고칠 종목을 아직 읽지 못했다. */
    val loading: Boolean = false,
    val name: String = "",
    val bodyPart: BodyPart? = null,
    val subPart: SubPart? = null,
    val equipment: Equipment? = null,
    val recordType: RecordType = RecordType.WEIGHT_REPS,
    /** 이름이 숨기지 않은 다른 종목과 겹친다. */
    val nameConflict: Boolean = false,
    /** 고치는 종목이 운동 기록에 쓰였고 기록 방식을 처음과 다르게 골랐다. */
    val showsRecordTypeNotice: Boolean = false,
    val busy: Boolean = false,
    /** 저장이나 지우기가 끝났다. 화면은 이 값을 보고 닫는다. */
    val outcome: ExerciseFormOutcome? = null,
) {
    /** 이름, 부위, 장비가 정해지고 이름이 겹치지 않아야 저장한다. 이름이 비면 "저장"이 비활성이다. */
    val canSave: Boolean
        get() = !loading && !busy && outcome == null &&
            name.isNotBlank() && bodyPart != null && equipment != null && !nameConflict

    /**
     * 이름을 적었는데 부위를 고르지 않았다. "저장" 위에 고를 항목을 알린다(#55).
     * 이름이 비었을 때는 빈 이름 칸이 이유이므로 알리지 않는다.
     */
    val missingBodyPart: Boolean
        get() = !loading && name.isNotBlank() && bodyPart == null

    /** 이름을 적었는데 장비를 고르지 않았다. [missingBodyPart]와 같은 때에 알린다. */
    val missingEquipment: Boolean
        get() = !loading && name.isNotBlank() && equipment == null
}

sealed interface ExerciseFormOutcome {
    /** 새 종목을 만들었다. 종목 고르기에 돌려줘 고른 상태로 넣는다. */
    data class Created(val exerciseId: String) : ExerciseFormOutcome

    data object Updated : ExerciseFormOutcome

    data class Deleted(val result: ExerciseDeleteResult) : ExerciseFormOutcome

    /** 고칠 수 없는 종목이다(없거나 내장 종목). 화면을 닫는다. */
    data object Unavailable : ExerciseFormOutcome
}

/**
 * 직접 추가하는 종목의 추가·수정·지우기(EX-04, LOG-08).
 * 내장 종목은 고치거나 지우지 않는다. 저장소도 내장 종목이면 거부한다.
 */
class ExerciseFormViewModel(
    private val repository: ExerciseRepository,
    private val exerciseId: String?,
    initialName: String = "",
) : ViewModel() {
    private data class Form(
        val loading: Boolean,
        val name: String,
        val bodyPart: BodyPart? = null,
        val subPart: SubPart? = null,
        val equipment: Equipment? = null,
        val recordType: RecordType = RecordType.WEIGHT_REPS,
        val original: Exercise? = null,
        val hasRecords: Boolean = false,
        val busy: Boolean = false,
        val outcome: ExerciseFormOutcome? = null,
    )

    private val form = MutableStateFlow(Form(loading = exerciseId != null, name = initialName))

    val uiState: StateFlow<ExerciseFormUiState> = combine(repository.observeExercises(), form) { all, form ->
        ExerciseFormUiState(
            isEdit = exerciseId != null,
            loading = form.loading,
            name = form.name,
            bodyPart = form.bodyPart,
            subPart = form.subPart,
            equipment = form.equipment,
            recordType = form.recordType,
            nameConflict = all.hasNameConflict(form.name, exceptId = exerciseId),
            showsRecordTypeNotice = form.hasRecords && form.original != null &&
                form.original.recordType != form.recordType,
            busy = form.busy,
            outcome = form.outcome,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        ExerciseFormUiState(isEdit = exerciseId != null, loading = exerciseId != null, name = initialName),
    )

    init {
        if (exerciseId != null) {
            viewModelScope.launch {
                val exercise = repository.getExercise(exerciseId)
                if (exercise == null || exercise.isBuiltin || exercise.isArchived) {
                    form.update { it.copy(loading = false, outcome = ExerciseFormOutcome.Unavailable) }
                    return@launch
                }
                val hasRecords = repository.isUsedInWorkouts(exerciseId)
                form.update {
                    it.copy(
                        loading = false,
                        name = exercise.nameKo,
                        bodyPart = exercise.bodyPart,
                        subPart = exercise.subPart,
                        equipment = exercise.equipment,
                        recordType = exercise.recordType,
                        original = exercise,
                        hasRecords = hasRecords,
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) = form.update { it.copy(name = name) }

    /** 부위를 바꾸면 맞지 않는 세부 부위는 풀린다. */
    fun onBodyPartSelect(bodyPart: BodyPart) = form.update {
        it.copy(bodyPart = bodyPart, subPart = it.subPart?.takeIf { sub -> isValidSubPart(bodyPart, sub) })
    }

    /** 세부 부위는 고르지 않아도 된다. 고른 칩을 다시 누르면 풀린다. */
    fun onSubPartToggle(subPart: SubPart) = form.update {
        when {
            it.bodyPart != subPart.bodyPart -> it
            it.subPart == subPart -> it.copy(subPart = null)
            else -> it.copy(subPart = subPart)
        }
    }

    fun onEquipmentSelect(equipment: Equipment) = form.update { it.copy(equipment = equipment) }

    fun onRecordTypeSelect(recordType: RecordType) = form.update { it.copy(recordType = recordType) }

    fun save() {
        val state = uiState.value
        if (!state.canSave) return
        val current = form.value
        val draft = ExerciseDraft(
            nameKo = current.name,
            // 이 화면에서 적지 않는 값(영어 이름, 별칭, 휴식 시간)은 고치는 종목의 값을 그대로 둔다.
            nameEn = current.original?.nameEn,
            aliases = current.original?.aliases.orEmpty(),
            bodyPart = checkNotNull(current.bodyPart),
            subPart = current.subPart,
            equipment = checkNotNull(current.equipment),
            recordType = current.recordType,
            restSeconds = current.original?.restSeconds,
        )
        form.update { it.copy(busy = true) }
        viewModelScope.launch {
            val outcome = if (exerciseId == null) {
                ExerciseFormOutcome.Created(repository.addCustomExercise(draft))
            } else {
                repository.updateCustomExercise(exerciseId, draft)
                ExerciseFormOutcome.Updated
            }
            form.update { it.copy(busy = false, outcome = outcome) }
        }
    }

    /** 확인을 받은 뒤 부른다. 기록에 쓰인 종목은 저장소가 숨기기만 한다. */
    fun delete() {
        if (exerciseId == null || form.value.busy || form.value.original == null) return
        form.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = repository.deleteCustomExercise(exerciseId)
            form.update { it.copy(busy = false, outcome = ExerciseFormOutcome.Deleted(result)) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
