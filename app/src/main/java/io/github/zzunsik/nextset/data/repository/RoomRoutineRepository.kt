package io.github.zzunsik.nextset.data.repository

import androidx.room3.withWriteTransaction
import io.github.zzunsik.nextset.data.db.NextSetDatabase
import io.github.zzunsik.nextset.data.db.RoutineEntity
import io.github.zzunsik.nextset.data.db.RoutineExerciseEntity
import io.github.zzunsik.nextset.data.db.Tables
import io.github.zzunsik.nextset.domain.model.Routine
import io.github.zzunsik.nextset.domain.model.RoutineDetail
import io.github.zzunsik.nextset.domain.model.RoutineExercise
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.repository.RoutineRepository
import io.github.zzunsik.nextset.domain.rule.RoutineUpdatePlan
import io.github.zzunsik.nextset.domain.rule.fitTo
import io.github.zzunsik.nextset.domain.rule.requireSameItems
import io.github.zzunsik.nextset.domain.rule.topRoutine
import io.github.zzunsik.nextset.domain.rule.validateRoutineExercise
import kotlinx.coroutines.flow.Flow

internal class RoomRoutineRepository(private val database: NextSetDatabase) : RoutineRepository {
    private val routineDao = database.routineDao()
    private val exerciseDao = database.exerciseDao()
    private val settingsDao = database.settingsDao()

    override fun observeRoutines(): Flow<List<Routine>> =
        database.observe(Tables.ROUTINE) { routineDao.getAll().map { it.toModel() } }

    override fun observeRoutine(id: Long): Flow<RoutineDetail?> =
        database.observe(Tables.ROUTINE, Tables.ROUTINE_EXERCISE, Tables.EXERCISE) { readDetail(id) }

    override fun observeTopRoutine(): Flow<Routine?> =
        database.observe(Tables.ROUTINE, Tables.WORKOUT, Tables.APP_SETTINGS) {
            topRoutine(
                routines = routineDao.getAll().map { it.toModel() },
                pinnedRoutineId = settingsDao.get()?.pinnedRoutineId,
                lastRoutineId = routineDao.lastFinishedRoutineId(),
            )
        }

    override suspend fun addRoutine(name: String): Long {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "루틴 이름이 비어 있다" }
        return database.withWriteTransaction {
            routineDao.insert(RoutineEntity(name = clean, position = routineDao.count()))
        }
    }

    override suspend fun renameRoutine(id: Long, name: String) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "루틴 이름이 비어 있다" }
        require(routineDao.rename(id, clean) == 1) { "루틴 $id 가 없다" }
    }

    override suspend fun deleteRoutine(id: Long) {
        database.withWriteTransaction {
            // 루틴 종목은 CASCADE로 지워지고 운동 기록의 routine_id와 직접 고른 루틴은 SET NULL로 빈다.
            routineDao.deleteById(id)
            routineDao.getAll().forEachIndexed { index, routine ->
                if (routine.position != index) routineDao.setPosition(routine.id, index)
            }
        }
    }

    override suspend fun reorderRoutines(orderedIds: List<Long>) {
        database.withWriteTransaction {
            requireSameItems(routineDao.getAll().map { it.id }, orderedIds)
            orderedIds.forEachIndexed { index, id -> routineDao.setPosition(id, index) }
        }
    }

    override suspend fun addRoutineExercise(routineId: Long, exerciseId: String, setCount: Int, target: Target): Long {
        validateRoutineExercise(setCount, target)
        return database.withWriteTransaction {
            requireNotNull(routineDao.getById(routineId)) { "루틴 $routineId 가 없다" }
            val exercise = requireNotNull(exerciseDao.getById(exerciseId)) { "종목 $exerciseId 가 없다" }
            val fitted = target.fitTo(exercise.recordType)
            routineDao.insertItem(
                RoutineExerciseEntity(
                    routineId = routineId,
                    exerciseId = exerciseId,
                    position = routineDao.getItems(routineId).size,
                    setCount = setCount,
                    targetWeightG = null,
                    targetRepsMin = null,
                    targetRepsMax = null,
                    targetDurationS = null,
                ).withTarget(fitted),
            )
        }
    }

    override suspend fun removeRoutineExercise(routineExerciseId: Long) {
        database.withWriteTransaction {
            val item = routineDao.getItem(routineExerciseId) ?: return@withWriteTransaction
            routineDao.deleteItem(item.id)
            renumberRoutineItems(database, item.routineId)
        }
    }

    override suspend fun reorderRoutineExercises(routineId: Long, orderedIds: List<Long>) {
        database.withWriteTransaction {
            requireSameItems(routineDao.getItems(routineId).map { it.id }, orderedIds)
            orderedIds.forEachIndexed { index, id -> routineDao.setItemPosition(id, index) }
        }
    }

    override suspend fun updateRoutineExercise(routineExerciseId: Long, setCount: Int, target: Target) {
        validateRoutineExercise(setCount, target)
        database.withWriteTransaction {
            val item = requireNotNull(routineDao.getItem(routineExerciseId)) { "루틴 종목 $routineExerciseId 가 없다" }
            val exercise = checkNotNull(exerciseDao.getById(item.exerciseId))
            routineDao.updateItem(item.copy(setCount = setCount).withTarget(target.fitTo(exercise.recordType)))
        }
    }

    override suspend fun applyRoutineUpdate(plan: RoutineUpdatePlan) {
        // 빼기, 세트 수 바꾸기, 더하기, 순서 매기기를 한 트랜잭션에서 한다(#72). 어느 단계가 실패해도 모두 되돌아간다.
        database.withWriteTransaction {
            val routineId = plan.routineId
            requireNotNull(routineDao.getById(routineId)) { "루틴 $routineId 가 없다" }
            val current = routineDao.getItems(routineId).associateBy { it.id }
            plan.remove.forEach { id -> if (id in current) routineDao.deleteItem(id) }
            val keptIds = mutableSetOf<Long>()
            plan.keep.forEach { (id, setCount) ->
                val item = requireNotNull(current[id]) { "루틴 $routineId 에 루틴 종목 $id 가 없다" }
                if (setCount < 1) {
                    routineDao.deleteItem(id)
                } else {
                    val target = plan.targets[id] ?: Target.NONE
                    validateRoutineExercise(setCount, target)
                    val exercise = checkNotNull(exerciseDao.getById(item.exerciseId))
                    routineDao.updateItem(item.copy(setCount = setCount).withTarget(target.fitTo(exercise.recordType)))
                    keptIds += id
                }
            }
            val addedIds = plan.add.map { (exerciseId, setCount) ->
                if (setCount < 1) return@map null
                validateRoutineExercise(setCount, Target.NONE)
                requireNotNull(exerciseDao.getById(exerciseId)) { "종목 $exerciseId 가 없다" }
                routineDao.insertItem(
                    RoutineExerciseEntity(
                        routineId = routineId,
                        exerciseId = exerciseId,
                        position = Int.MAX_VALUE, // 아래에서 순서를 다시 매긴다.
                        setCount = setCount,
                        targetWeightG = null,
                        targetRepsMin = null,
                        targetRepsMax = null,
                        targetDurationS = null,
                    ),
                )
            }
            val ordered = plan.order.mapNotNull { slot ->
                when (slot) {
                    is RoutineUpdatePlan.Slot.Existing -> slot.routineExerciseId.takeIf { it in keptIds }
                    is RoutineUpdatePlan.Slot.New -> addedIds[slot.addIndex]
                }
            }
            requireSameItems(routineDao.getItems(routineId).map { it.id }, ordered)
            ordered.forEachIndexed { index, id -> routineDao.setItemPosition(id, index) }
        }
    }

    private suspend fun readDetail(id: Long): RoutineDetail? {
        val routine = routineDao.getById(id) ?: return null
        val items = routineDao.getItems(id)
        val exercises = exerciseDao.getByIds(items.map { it.exerciseId }.toSet()).associateBy { it.id }
        return RoutineDetail(
            routine = routine.toModel(),
            exercises = items.map { item ->
                RoutineExercise(
                    id = item.id,
                    routineId = item.routineId,
                    exercise = exercises.getValue(item.exerciseId).toModel(),
                    position = item.position,
                    setCount = item.setCount,
                    target = item.target,
                )
            },
        )
    }
}
