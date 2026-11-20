package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.RoutineDetail
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.model.WorkoutExercise
import io.github.zzunsik.nextset.domain.model.WorkoutSet

/** 다음 세트(LOG-06)의 자리. 순서대로 봤을 때 완료하지 않은 첫 세트다(docs/architecture.md "진행 중인 운동의 저장"). */
data class NextSetRef(val exercise: WorkoutExercise, val set: WorkoutSet)

fun Workout.nextSet(): NextSetRef? {
    exercises.sortedBy { it.position }.forEach { exercise ->
        exercise.sets.sortedBy { it.position }.firstOrNull { !it.isCompleted }?.let { return NextSetRef(exercise, it) }
    }
    return null
}

/**
 * 세트 행에 제안하는 값. 미리 채우기(LOG-03, [prefillValue])가 비워 둔 칸은 바로 위 행([above])의 값으로 채운다
 * (docs/DESIGN.md `SetRow`: "새 행의 값은 바로 위 행의 값이다"). 직전 기록과 목표가 위 행보다 먼저다.
 */
fun suggestedValue(type: RecordType, previous: SetValue?, target: Target, above: SetValue?): SetValue {
    val base = prefillValue(type, previous, target)
    val fallback = above?.fitTo(type) ?: return base
    return SetValue(
        weight = base.weight ?: fallback.weight,
        reps = base.reps ?: fallback.reps,
        durationSeconds = base.durationSeconds ?: fallback.durationSeconds,
    ).fitTo(type)
}

/** 기록 방식이 쓰는 칸이 모두 채워졌는지. 빈 칸이 있으면 체크해도 완료하지 않는다(docs/DESIGN.md `SetRow` "값 없음"). */
fun SetValue.isFilled(type: RecordType): Boolean = when (type) {
    RecordType.WEIGHT_REPS -> weight != null && reps != null
    RecordType.REPS -> reps != null
    RecordType.DURATION -> durationSeconds != null
}

/**
 * 운동을 끝낼 때 루틴에 반영할 내용(LOG-10). 운동의 종목 순서와 세트 수를 루틴에 옮긴다.
 *
 * @property keep 루틴에 남는 루틴 종목과 새 세트 수.
 * @property targets 남는 루틴 종목의 지금 목표. 반영할 때 목표는 그대로 둔다.
 * @property add 루틴에 더할 종목(종목 식별자, 세트 수). 목표는 비워 둔다(RTN-04).
 * @property remove 루틴에서 뺄 루틴 종목.
 * @property order 반영한 뒤의 순서. 남는 루틴 종목은 [Slot.Existing], 더할 종목은 [Slot.New]([add]의 순번)다.
 */
data class RoutineUpdatePlan(
    val routineId: Long,
    val keep: Map<Long, Int>,
    val targets: Map<Long, Target>,
    val add: List<Pair<String, Int>>,
    val remove: List<Long>,
    val order: List<Slot>,
) {
    sealed interface Slot {
        data class Existing(val routineExerciseId: Long) : Slot

        data class New(val addIndex: Int) : Slot
    }
}

/**
 * 운동이 루틴과 달라졌는지 보고 반영할 내용을 만든다. 같으면 null이다(묻지 않는다).
 *
 * - 견주는 것은 종목 구성(어떤 종목이 어떤 순서로 있는가)과 종목마다의 세트 수다. 세트 수는 완료하지 않은 세트도 센다.
 *   완료하지 않은 세트는 끝낼 때 지워지지만 사용자가 더하고 지운 계획이 루틴과 견줄 대상이라서다(제안).
 * - 같은 종목이 여러 번 있으면 앞에서부터 짝을 짓는다.
 * - 빈 운동(루틴 없음)과 지워진 루틴은 이 함수까지 오지 않는다. 부르는 쪽이 [routine]을 구하지 못하면 묻지 않는다.
 */
fun planRoutineUpdate(workout: Workout, routine: RoutineDetail): RoutineUpdatePlan? {
    val routineItems = routine.exercises.sortedBy { it.position }
    val workoutItems = workout.exercises.sortedBy { it.position }
    val unmatched = routineItems.toMutableList()
    val keep = linkedMapOf<Long, Int>()
    val add = mutableListOf<Pair<String, Int>>()
    val order = mutableListOf<RoutineUpdatePlan.Slot>()
    workoutItems.forEach { item ->
        val match = unmatched.firstOrNull { it.exercise.id == item.exercise.id }
        if (match != null) {
            unmatched.remove(match)
            keep[match.id] = item.sets.size
            order += RoutineUpdatePlan.Slot.Existing(match.id)
        } else {
            order += RoutineUpdatePlan.Slot.New(add.size)
            add += item.exercise.id to item.sets.size
        }
    }
    val sameLayout = add.isEmpty() && unmatched.isEmpty() &&
        order.map { (it as RoutineUpdatePlan.Slot.Existing).routineExerciseId } == routineItems.map { it.id } &&
        routineItems.all { keep[it.id] == it.setCount }
    if (sameLayout) return null
    return RoutineUpdatePlan(
        routineId = routine.routine.id,
        keep = keep,
        targets = routineItems.filter { it.id in keep }.associate { it.id to it.target },
        add = add,
        remove = unmatched.map { it.id },
        order = order,
    )
}
