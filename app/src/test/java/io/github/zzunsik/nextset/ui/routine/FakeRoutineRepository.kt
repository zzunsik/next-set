package io.github.zzunsik.nextset.ui.routine

import io.github.zzunsik.nextset.domain.model.HomeRoutine
import io.github.zzunsik.nextset.domain.model.HomeSnapshot
import io.github.zzunsik.nextset.domain.model.Routine
import io.github.zzunsik.nextset.domain.model.RoutineDetail
import io.github.zzunsik.nextset.domain.model.RoutineExercise
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.repository.RoutineRepository
import io.github.zzunsik.nextset.domain.rule.RoutineUpdatePlan
import io.github.zzunsik.nextset.domain.rule.fitTo
import io.github.zzunsik.nextset.domain.rule.requireSameItems
import io.github.zzunsik.nextset.domain.rule.validateRoutineExercise
import io.github.zzunsik.nextset.ui.exercise.FakeExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * 화면 상태 테스트의 가짜 루틴 저장소. 메모리에 두고 검증은 Room 구현과 같은 domain 규칙
 * ([validateRoutineExercise], [fitTo], [requireSameItems])으로 한다. 종목은 [exercises]에서 찾는다.
 */
class FakeRoutineRepository(
    val exercises: FakeExerciseRepository = FakeExerciseRepository(),
) : RoutineRepository {
    /** 루틴 이름. 목록 순서다. */
    val routines = MutableStateFlow<List<Routine>>(emptyList())

    /** 루틴 종목. 루틴 안의 순서는 [Item.position]이다. */
    val items = MutableStateFlow<List<Item>>(emptyList())

    data class Item(
        val id: Long,
        val routineId: Long,
        val exerciseId: String,
        val position: Int,
        val setCount: Int,
        val target: Target,
    )

    private var nextId = 1L

    override fun observeRoutines(): Flow<List<Routine>> = routines

    override fun observeRoutine(id: Long): Flow<RoutineDetail?> =
        combine(routines, items, exercises.exercises) { routines, items, exercises ->
            val routine = routines.find { it.id == id } ?: return@combine null
            RoutineDetail(
                routine = routine,
                exercises = items.filter { it.routineId == id }.sortedBy { it.position }.map { item ->
                    RoutineExercise(
                        id = item.id,
                        routineId = id,
                        exercise = exercises.first { it.id == item.exerciseId },
                        position = item.position,
                        setCount = item.setCount,
                        target = item.target,
                    )
                },
            )
        }

    override fun observeTopRoutine(): Flow<Routine?> = routines.map { it.firstOrNull() }

    /** 운동 기록과 설정을 모르는 가짜라 진행 중인 운동, 마지막 운동, 직접 고른 루틴은 늘 비어 있다. 맨 위는 첫 루틴이다. */
    override fun observeHome(): Flow<HomeSnapshot> =
        combine(routines, items, exercises.exercises) { routines, items, exercises ->
            HomeSnapshot(
                inProgress = null,
                routines = routines.map { routine ->
                    HomeRoutine(
                        routine = routine,
                        exerciseNames = items.filter { it.routineId == routine.id }.sortedBy { it.position }
                            .map { item -> exercises.first { it.id == item.exerciseId }.nameKo },
                        lastStartedAt = null,
                    )
                },
                top = routines.firstOrNull(),
                pinnedRoutineId = null,
            )
        }

    override suspend fun addRoutine(name: String): Long {
        require(name.isNotBlank()) { "루틴 이름이 비어 있다" }
        val id = nextId++
        routines.value = routines.value + Routine(id, name.trim(), routines.value.size)
        return id
    }

    override suspend fun renameRoutine(id: Long, name: String) {
        require(name.isNotBlank()) { "루틴 이름이 비어 있다" }
        require(routines.value.any { it.id == id }) { "루틴 $id 가 없다" }
        routines.value = routines.value.map { if (it.id == id) it.copy(name = name.trim()) else it }
    }

    override suspend fun deleteRoutine(id: Long) {
        items.value = items.value.filterNot { it.routineId == id }
        routines.value = routines.value.filterNot { it.id == id }.mapIndexed { index, it -> it.copy(position = index) }
    }

    override suspend fun reorderRoutines(orderedIds: List<Long>) {
        requireSameItems(routines.value.map { it.id }, orderedIds)
        val byId = routines.value.associateBy { it.id }
        routines.value = orderedIds.mapIndexed { index, id -> byId.getValue(id).copy(position = index) }
    }

    override suspend fun addRoutineExercise(routineId: Long, exerciseId: String, setCount: Int, target: Target): Long {
        validateRoutineExercise(setCount, target)
        require(routines.value.any { it.id == routineId }) { "루틴 $routineId 가 없다" }
        val exercise = requireNotNull(exercises.getExercise(exerciseId)) { "종목 $exerciseId 가 없다" }
        val id = nextId++
        val position = items.value.count { it.routineId == routineId }
        items.value = items.value + Item(id, routineId, exerciseId, position, setCount, target.fitTo(exercise.recordType))
        return id
    }

    override suspend fun removeRoutineExercise(routineExerciseId: Long) {
        val removed = items.value.find { it.id == routineExerciseId } ?: return
        items.value = items.value.filterNot { it.id == routineExerciseId }
        renumber(removed.routineId)
    }

    override suspend fun reorderRoutineExercises(routineId: Long, orderedIds: List<Long>) {
        requireSameItems(itemsOf(routineId).map { it.id }, orderedIds)
        items.value = items.value.map { item ->
            if (item.routineId == routineId) item.copy(position = orderedIds.indexOf(item.id)) else item
        }
    }

    override suspend fun updateRoutineExercise(routineExerciseId: Long, setCount: Int, target: Target) {
        validateRoutineExercise(setCount, target)
        val item = requireNotNull(items.value.find { it.id == routineExerciseId }) { "루틴 종목 $routineExerciseId 가 없다" }
        val exercise = checkNotNull(exercises.getExercise(item.exerciseId))
        items.value = items.value.map {
            if (it.id == routineExerciseId) it.copy(setCount = setCount, target = target.fitTo(exercise.recordType)) else it
        }
    }

    /** Room 구현처럼 한 번에 반영한다. 도중에 실패하면 반영 전 종목으로 되돌린다. */
    override suspend fun applyRoutineUpdate(plan: RoutineUpdatePlan) {
        val before = items.value
        try {
            plan.remove.forEach { removeRoutineExercise(it) }
            val kept = mutableSetOf<Long>()
            plan.keep.forEach { (id, setCount) ->
                if (setCount < 1) {
                    removeRoutineExercise(id)
                } else {
                    updateRoutineExercise(id, setCount, plan.targets[id] ?: Target.NONE)
                    kept += id
                }
            }
            val added = plan.add.map { (exerciseId, setCount) ->
                if (setCount < 1) null else addRoutineExercise(plan.routineId, exerciseId, setCount)
            }
            val ordered = plan.order.mapNotNull { slot ->
                when (slot) {
                    is RoutineUpdatePlan.Slot.Existing -> slot.routineExerciseId.takeIf { it in kept }
                    is RoutineUpdatePlan.Slot.New -> added[slot.addIndex]
                }
            }
            reorderRoutineExercises(plan.routineId, ordered)
        } catch (e: Exception) {
            items.value = before
            throw e
        }
    }

    /** 루틴 [routineId]의 종목. 순서대로. */
    fun itemsOf(routineId: Long): List<Item> = items.value.filter { it.routineId == routineId }.sortedBy { it.position }

    private fun renumber(routineId: Long) {
        val ordered = itemsOf(routineId).map { it.id }
        items.value = items.value.map { if (it.routineId == routineId) it.copy(position = ordered.indexOf(it.id)) else it }
    }
}
