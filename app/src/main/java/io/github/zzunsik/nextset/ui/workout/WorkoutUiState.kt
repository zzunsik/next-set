package io.github.zzunsik.nextset.ui.workout

import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.rule.nextSet
import io.github.zzunsik.nextset.domain.rule.suggestedValue
import io.github.zzunsik.nextset.ui.component.NumberFieldKind
import io.github.zzunsik.nextset.ui.component.NumberPadSession
import io.github.zzunsik.nextset.ui.component.numberFieldKinds
import java.time.Instant

/** 숫자판이 편집하는 칸. 세트와 칸 종류로 알아본다. */
data class SetFieldId(val setId: Long, val kind: NumberFieldKind)

/** 세트 행 하나(docs/DESIGN.md `SetRow`). */
data class SetRowUi(
    val id: Long,
    /** 0부터 센다. 화면에는 1부터 적는다. */
    val position: Int,
    val value: SetValue,
    val completed: Boolean,
    /** 직전 기록의 같은 순번 세트(LOG-02). 없으면 빈칸이다. */
    val previous: SetValue?,
    /** 미리 채운 값이 그대로 든 칸. 직접 고친 칸과 구분해 보여 준다(LOG-03). */
    val suggested: Set<NumberFieldKind>,
    /** 이번 운동에서 최고 기록을 넘은 세트(LOG-14). */
    val isRecord: Boolean,
    /** 다음 세트(LOG-06)인가. */
    val isNext: Boolean,
)

/** 종목 블록 하나(docs/DESIGN.md `ExerciseHeader`와 세트 행들). */
data class ExerciseBlockUi(
    val id: Long,
    val exerciseId: String,
    val name: String,
    val recordType: RecordType,
    val note: String?,
    val target: Target,
    val sets: List<SetRowUi>,
) {
    val allDone: Boolean get() = sets.isNotEmpty() && sets.all { it.completed }
}

/** 다음 세트 표시(docs/DESIGN.md `NextSetDisplay`). 값은 세트 행에 든 값과 같다. */
data class NextSetUi(
    val workoutExerciseId: Long,
    val setId: Long,
    val exerciseName: String,
    val recordType: RecordType,
    /** 1부터 센다. */
    val setNumber: Int,
    val setCount: Int,
    val value: SetValue,
    val previous: SetValue?,
)

/** 화면에 띄운 대화상자. */
sealed interface WorkoutDialog {
    /** 끝내기 1: 완료한 세트가 없다. "계속 운동", "버리기". */
    data object NoCompletedSets : WorkoutDialog

    /** 끝내기 2: 완료하지 않은 세트가 [count]개 있다. "계속 운동", "끝내기". */
    data class IncompleteSets(val count: Int) : WorkoutDialog

    /** 끝내기 3: 종목 구성이나 세트 수가 루틴과 다르다(LOG-10). "이번만", "루틴에 반영". */
    data object RoutineChange : WorkoutDialog

    /** 운동 버리기 확인. */
    data object Discard : WorkoutDialog

    data class RemoveExercise(val workoutExerciseId: Long, val name: String) : WorkoutDialog

    data class ExerciseNote(val workoutExerciseId: Long, val name: String, val note: String) : WorkoutDialog

    data class WorkoutNote(val note: String) : WorkoutDialog

    /** 종목 순서 바꾸기. 종목 이름만 나열한다(docs/DESIGN.md "운동 중"). */
    data object Reorder : WorkoutDialog
}

data class WorkoutUiState(
    val loading: Boolean = true,
    /** 시작할 때의 루틴 이름. 빈 운동이면 null이다. */
    val routineName: String? = null,
    val startedAt: Instant = Instant.EPOCH,
    val note: String? = null,
    val exercises: List<ExerciseBlockUi> = emptyList(),
    val nextSet: NextSetUi? = null,
    val numberPad: NumberPadSession<SetFieldId>? = null,
    val dialog: WorkoutDialog? = null,
) {
    /** 모든 세트를 끝냈다. 다음 세트 자리에 운동 끝내기를 내세운다. */
    val allDone: Boolean get() = exercises.isNotEmpty() && nextSet == null && exercises.any { it.sets.isNotEmpty() }

    fun findSet(setId: Long): Pair<ExerciseBlockUi, SetRowUi>? {
        exercises.forEach { block -> block.sets.firstOrNull { it.id == setId }?.let { return block to it } }
        return null
    }
}

/** 칸 종류가 세트 값에서 가리키는 양(그램, 회, 초). */
fun SetValue.amountOf(kind: NumberFieldKind): Int? = when (kind) {
    NumberFieldKind.WEIGHT -> weight?.grams
    NumberFieldKind.REPS -> reps
    NumberFieldKind.DURATION -> durationSeconds
}

/**
 * 저장된 운동에서 화면 상태를 만든다. 숫자판이 열려 있으면 편집 중인 값을 행과 다음 세트 표시에 겹쳐 보인다.
 * 그래서 칸에서 값을 고치면 다음 세트 표시도 바로 바뀐다(docs/DESIGN.md `NextSetDisplay`).
 *
 * @param previousSets 운동 기록 종목마다 직전 기록(LOG-03).
 */
fun buildWorkoutUiState(
    workout: Workout,
    previousSets: Map<Long, List<SetValue>>,
    numberPad: NumberPadSession<SetFieldId>?,
    records: Set<Long>,
    dialog: WorkoutDialog?,
): WorkoutUiState {
    val editing = numberPad?.fields?.associate { it.id to it.amount }.orEmpty()
    val editingSetId = numberPad?.focused?.id?.setId
    fun valueOf(setId: Long, stored: SetValue): SetValue {
        if (setId != editingSetId) return stored
        return editing.entries.fold(stored) { value, (id, amount) ->
            when (id.kind) {
                NumberFieldKind.WEIGHT -> value.copy(weight = amount?.let { Weight(it) })
                NumberFieldKind.REPS -> value.copy(reps = amount)
                NumberFieldKind.DURATION -> value.copy(durationSeconds = amount)
            }
        }
    }

    val next = workout.nextSet()
    val blocks = workout.exercises.sortedBy { it.position }.map { exercise ->
        val previous = previousSets[exercise.id].orEmpty()
        val sets = exercise.sets.sortedBy { it.position }
        val kinds = exercise.recordType.numberFieldKinds()
        ExerciseBlockUi(
            id = exercise.id,
            exerciseId = exercise.exercise.id,
            name = exercise.exercise.nameKo,
            recordType = exercise.recordType,
            note = exercise.note,
            target = exercise.target,
            sets = sets.mapIndexed { index, set ->
                val value = valueOf(set.id, set.value)
                val suggestion = suggestedValue(
                    type = exercise.recordType,
                    previous = previous.getOrNull(index),
                    target = exercise.target,
                    above = sets.getOrNull(index - 1)?.let { valueOf(it.id, it.value) },
                )
                SetRowUi(
                    id = set.id,
                    position = index,
                    value = value,
                    completed = set.isCompleted,
                    previous = previous.getOrNull(index),
                    suggested = kinds.filter { kind ->
                        value.amountOf(kind) != null && value.amountOf(kind) == suggestion.amountOf(kind)
                    }.toSet(),
                    isRecord = set.isCompleted && set.id in records,
                    isNext = set.id == next?.set?.id,
                )
            },
        )
    }
    val nextUi = next?.let { ref ->
        val block = blocks.first { it.id == ref.exercise.id }
        val row = block.sets.first { it.id == ref.set.id }
        NextSetUi(
            workoutExerciseId = block.id,
            setId = row.id,
            exerciseName = block.name,
            recordType = block.recordType,
            setNumber = row.position + 1,
            setCount = block.sets.size,
            value = row.value,
            previous = row.previous,
        )
    }
    // 편집하던 세트가 지워졌으면 숫자판을 닫는다.
    val pad = numberPad?.takeIf { session -> blocks.any { block -> block.sets.any { it.id == session.focused.id.setId } } }
    return WorkoutUiState(
        loading = false,
        routineName = workout.routineName,
        startedAt = workout.startedAt,
        note = workout.note,
        exercises = blocks,
        nextSet = nextUi,
        numberPad = pad,
        dialog = dialog,
    )
}
