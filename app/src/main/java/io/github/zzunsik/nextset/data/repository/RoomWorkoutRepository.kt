package io.github.zzunsik.nextset.data.repository

import androidx.room3.withWriteTransaction
import io.github.zzunsik.nextset.data.db.BestSetRow
import io.github.zzunsik.nextset.data.db.ExerciseEntity
import io.github.zzunsik.nextset.data.db.NextSetDatabase
import io.github.zzunsik.nextset.data.db.Tables
import io.github.zzunsik.nextset.data.db.WorkoutDao
import io.github.zzunsik.nextset.data.db.WorkoutEntity
import io.github.zzunsik.nextset.data.db.WorkoutExerciseEntity
import io.github.zzunsik.nextset.data.db.WorkoutSetEntity
import io.github.zzunsik.nextset.domain.model.FinishResult
import io.github.zzunsik.nextset.domain.model.PersonalRecord
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetCompletion
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.model.WorkoutExercise
import io.github.zzunsik.nextset.domain.model.WorkoutInProgressException
import io.github.zzunsik.nextset.domain.model.WorkoutSummary
import io.github.zzunsik.nextset.domain.repository.WorkoutRepository
import io.github.zzunsik.nextset.domain.rule.fitTo
import io.github.zzunsik.nextset.domain.rule.isNewRecord
import io.github.zzunsik.nextset.domain.rule.prefillValue
import io.github.zzunsik.nextset.domain.rule.requireSameItems
import io.github.zzunsik.nextset.domain.rule.validate
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** docs/architecture.md "진행 중인 운동의 저장", "지난 기록이 바뀌지 않게 하는 방법", "직전 기록과 최고 기록". */
internal class RoomWorkoutRepository(private val database: NextSetDatabase) : WorkoutRepository {
    private val workoutDao = database.workoutDao()
    private val routineDao = database.routineDao()
    private val exerciseDao = database.exerciseDao()
    private val settingsDao = database.settingsDao()

    override fun observeInProgressWorkout(): Flow<Workout?> =
        database.observe(*WORKOUT_TABLES) { workoutDao.getInProgressId()?.let { readWorkout(it) } }

    override fun observeWorkout(id: Long): Flow<Workout?> = database.observe(*WORKOUT_TABLES) { readWorkout(id) }

    override fun observeHistory(): Flow<List<WorkoutSummary>> = database.observe(Tables.WORKOUT) {
        workoutDao.getFinishedWorkouts().map { workout ->
            WorkoutSummary(
                id = workout.id,
                routineId = workout.routineId,
                routineName = workout.routineName,
                startedAt = Instant.ofEpochMilli(workout.startedAt),
                finishedAt = Instant.ofEpochMilli(checkNotNull(workout.finishedAt)),
                note = workout.note,
            )
        }
    }

    override suspend fun startFromRoutine(routineId: Long, now: Instant): Long = database.withWriteTransaction {
        requireNoWorkoutInProgress()
        val routine = requireNotNull(routineDao.getById(routineId)) { "루틴 $routineId 가 없다" }
        val workoutId = workoutDao.insertWorkout(
            WorkoutEntity(routineId = routine.id, routineName = routine.name, startedAt = now.toEpochMilli(), finishedAt = null),
        )
        // 루틴 이름, 종목과 순서, 목표, 기록 방식, 세트 수를 복사한다. 그 뒤로 이 운동은 루틴 행을 읽지 않는다.
        routineDao.getItems(routineId).forEachIndexed { index, item ->
            insertExercise(workoutId, item.exerciseId, index, item.target, item.setCount, now.toEpochMilli())
        }
        workoutId
    }

    override suspend fun startEmpty(now: Instant): Long = database.withWriteTransaction {
        requireNoWorkoutInProgress()
        workoutDao.insertWorkout(
            WorkoutEntity(routineId = null, routineName = null, startedAt = now.toEpochMilli(), finishedAt = null),
        )
    }

    override fun observeFinishedWorkouts(): Flow<List<Workout>> = database.observe(*WORKOUT_TABLES) {
        val exercises = workoutDao.getExercisesOfFinishedWorkouts().groupBy { it.workoutId }
        val sets = workoutDao.getSetsOfFinishedWorkouts().groupBy { it.workoutExerciseId }
        val definitions = exerciseDao.getByIds(exercises.values.flatten().map { it.exerciseId }.toSet()).associateBy { it.id }
        workoutDao.getFinishedWorkouts().map { workout -> workout.toModel(exercises[workout.id].orEmpty(), sets, definitions) }
    }

    override suspend fun addPastWorkout(startedAt: Instant, finishedAt: Instant, routineId: Long?): Long {
        require(!finishedAt.isBefore(startedAt)) { "끝난 시각이 시작 시각보다 앞선다" }
        return database.withWriteTransaction {
            val routine = routineId?.let { requireNotNull(routineDao.getById(it)) { "루틴 $it 가 없다" } }
            val workoutId = workoutDao.insertWorkout(
                WorkoutEntity(
                    routineId = routine?.id,
                    routineName = routine?.name,
                    startedAt = startedAt.toEpochMilli(),
                    finishedAt = finishedAt.toEpochMilli(),
                ),
            )
            // 루틴에서 시작할 때와 같이 복사한다. 직전 기록은 이 운동의 시작 시각보다 앞선 것을 본다.
            routine?.let {
                routineDao.getItems(it.id).forEachIndexed { index, item ->
                    insertExercise(workoutId, item.exerciseId, index, item.target, item.setCount, startedAt.toEpochMilli())
                }
            }
            workoutId
        }
    }

    override suspend fun addExercise(workoutId: Long, exerciseId: String, setCount: Int): Long {
        require(setCount >= 0) { "세트 수는 0 이상이어야 한다" }
        return database.withWriteTransaction {
            val workout = requireWorkout(workoutId)
            insertExercise(
                workoutId = workoutId,
                exerciseId = exerciseId,
                position = workoutDao.getExercises(workoutId).size,
                target = Target.NONE,
                setCount = setCount,
                startedAt = workout.startedAt,
            )
        }
    }

    override suspend fun removeExercise(workoutExerciseId: Long) {
        database.withWriteTransaction {
            val exercise = workoutDao.getExercise(workoutExerciseId) ?: return@withWriteTransaction
            workoutDao.deleteExercise(exercise.id) // 세트는 CASCADE로 함께 지워진다.
            renumberExercises(exercise.workoutId)
        }
    }

    override suspend fun reorderExercises(workoutId: Long, orderedIds: List<Long>) {
        database.withWriteTransaction {
            requireSameItems(workoutDao.getExercises(workoutId).map { it.id }, orderedIds)
            orderedIds.forEachIndexed { index, id -> workoutDao.setExercisePosition(id, index) }
        }
    }

    override suspend fun addSet(workoutExerciseId: Long): Long = database.withWriteTransaction {
        val exercise = requireNotNull(workoutDao.getExercise(workoutExerciseId)) { "운동 기록 종목 $workoutExerciseId 가 없다" }
        val workout = requireWorkout(exercise.workoutId)
        val position = workoutDao.getSets(workoutExerciseId).size
        val previous = workoutDao.getPreviousSets(exercise.exerciseId, exercise.recordType, workout.startedAt)
        insertSet(exercise, position, previous)
    }

    override suspend fun deleteSet(setId: Long) {
        database.withWriteTransaction {
            val set = workoutDao.getSet(setId) ?: return@withWriteTransaction
            workoutDao.deleteSet(setId)
            renumberSets(set.workoutExerciseId)
        }
    }

    override suspend fun restoreSet(workoutExerciseId: Long, position: Int, value: SetValue, completedAt: Instant?): Long {
        value.validate()
        return database.withWriteTransaction {
            val exercise = requireNotNull(workoutDao.getExercise(workoutExerciseId)) { "운동 기록 종목 $workoutExerciseId 가 없다" }
            val sets = workoutDao.getSets(workoutExerciseId)
            val at = position.coerceIn(0, sets.size)
            sets.forEachIndexed { index, set ->
                val moved = if (index < at) index else index + 1
                if (set.position != moved) workoutDao.setSetPosition(set.id, moved)
            }
            val fitted = value.fitTo(exercise.recordType)
            workoutDao.insertSet(
                WorkoutSetEntity(
                    workoutExerciseId = workoutExerciseId,
                    position = at,
                    weightG = fitted.weight?.grams,
                    reps = fitted.reps,
                    durationS = fitted.durationSeconds,
                    completedAt = completedAt?.toEpochMilli(),
                ),
            )
        }
    }

    override suspend fun updateSet(setId: Long, value: SetValue) {
        value.validate()
        database.withWriteTransaction {
            val set = requireSet(setId)
            val type = checkNotNull(workoutDao.getExercise(set.workoutExerciseId)).recordType
            val fitted = value.fitTo(type)
            workoutDao.setSetValue(setId, fitted.weight?.grams, fitted.reps, fitted.durationSeconds)
        }
    }

    override suspend fun completeSet(setId: Long, now: Instant): SetCompletion = database.withWriteTransaction {
        val set = requireSet(setId)
        workoutDao.setCompletedAt(setId, now.toEpochMilli())
        val exercise = checkNotNull(workoutDao.getExercise(set.workoutExerciseId))
        val workout = checkNotNull(workoutDao.getWorkout(exercise.workoutId))
        // 지난 기록을 고칠 때는 알리지 않는다. 같은 방식으로 적은 이전 운동이 없어도 알리지 않는다.
        val notify = workout.finishedAt == null &&
            workoutDao.hasEarlierRecord(exercise.exerciseId, exercise.recordType, workout.id, workout.startedAt) &&
            isNewRecord(exercise.recordType, set.value, workoutDao.best(exercise.exerciseId, exercise.recordType, setId)?.value)
        SetCompletion(isNewRecord = notify)
    }

    override suspend fun uncompleteSet(setId: Long) {
        requireSet(setId)
        workoutDao.setCompletedAt(setId, null)
    }

    override suspend fun updateWorkoutNote(workoutId: Long, note: String?) {
        database.withWriteTransaction {
            workoutDao.updateWorkout(requireWorkout(workoutId).copy(note = note?.ifBlank { null }))
        }
    }

    override suspend fun updateExerciseNote(workoutExerciseId: Long, note: String?) {
        require(workoutDao.setExerciseNote(workoutExerciseId, note?.ifBlank { null }) == 1) {
            "운동 기록 종목 $workoutExerciseId 가 없다"
        }
    }

    override suspend fun updateWorkoutTimes(workoutId: Long, startedAt: Instant, finishedAt: Instant) {
        require(!finishedAt.isBefore(startedAt)) { "끝난 시각이 시작 시각보다 앞선다" }
        database.withWriteTransaction {
            val workout = requireWorkout(workoutId)
            require(workout.finishedAt != null) { "진행 중인 운동의 시각은 여기서 고치지 않는다" }
            workoutDao.updateWorkout(workout.copy(startedAt = startedAt.toEpochMilli(), finishedAt = finishedAt.toEpochMilli()))
        }
    }

    override suspend fun finishWorkout(workoutId: Long, now: Instant): FinishResult = database.withWriteTransaction {
        val workout = requireWorkout(workoutId)
        require(workout.finishedAt == null) { "이미 끝난 운동이다" }
        workoutDao.deleteUncompletedSets(workoutId)
        workoutDao.deleteExercisesWithoutSets(workoutId)
        val exercises = workoutDao.getExercises(workoutId)
        if (exercises.isEmpty()) {
            // 완료한 세트가 없으면 빈 기록을 남기지 않는다. 버린 것과 같아 직접 고른 루틴은 그대로 둔다.
            workoutDao.deleteWorkout(workoutId)
            return@withWriteTransaction FinishResult.DISCARDED_EMPTY
        }
        renumberExercises(workoutId)
        exercises.forEach { renumberSets(it.id) }
        workoutDao.updateWorkout(workout.copy(finishedAt = now.toEpochMilli()))
        workout.routineId?.let { settingsDao.unpinIf(it) }
        FinishResult.FINISHED
    }

    override suspend fun discardWorkout(workoutId: Long) {
        database.withWriteTransaction {
            require(requireWorkout(workoutId).finishedAt == null) { "끝난 운동은 버리지 않고 지운다" }
            workoutDao.deleteWorkout(workoutId)
        }
    }

    override suspend fun deleteWorkout(workoutId: Long) {
        workoutDao.deleteWorkout(workoutId)
    }

    override suspend fun getPreviousSets(exerciseId: String, recordType: RecordType, before: Instant): List<SetValue> =
        workoutDao.getPreviousSets(exerciseId, recordType, before.toEpochMilli()).map { it.value }

    override suspend fun getPreviousSet(exerciseId: String, recordType: RecordType, before: Instant, position: Int): SetValue? =
        workoutDao.getPreviousSets(exerciseId, recordType, before.toEpochMilli()).firstOrNull { it.position == position }?.value

    override fun observePersonalRecord(exerciseId: String): Flow<PersonalRecord?> =
        database.observe(*WORKOUT_TABLES) {
            val type = exerciseDao.getById(exerciseId)?.recordType ?: return@observe null
            workoutDao.best(exerciseId, type, WorkoutDao.NO_SET)?.let { row ->
                PersonalRecord(setId = row.setId, workoutId = row.workoutId, recordType = type, value = row.value)
            }
        }

    private suspend fun requireNoWorkoutInProgress() {
        workoutDao.getInProgressId()?.let { throw WorkoutInProgressException(it) }
    }

    private suspend fun requireWorkout(id: Long): WorkoutEntity =
        requireNotNull(workoutDao.getWorkout(id)) { "운동 기록 $id 가 없다" }

    private suspend fun requireSet(id: Long): WorkoutSetEntity =
        requireNotNull(workoutDao.getSet(id)) { "세트 $id 가 없다" }

    /** 종목의 지금 기록 방식을 복사해 운동 기록 종목을 만들고 세트를 미리 채워 만든다. */
    private suspend fun insertExercise(
        workoutId: Long,
        exerciseId: String,
        position: Int,
        target: Target,
        setCount: Int,
        startedAt: Long,
    ): Long {
        val exercise = requireNotNull(exerciseDao.getById(exerciseId)) { "종목 $exerciseId 가 없다" }
        val type = exercise.recordType
        val fitted = target.fitTo(type)
        val entity = WorkoutExerciseEntity(
            workoutId = workoutId,
            exerciseId = exerciseId,
            position = position,
            recordType = type,
            targetWeightG = fitted.weight?.grams,
            targetRepsMin = fitted.repsMin,
            targetRepsMax = fitted.repsMax,
            targetDurationS = fitted.durationSeconds,
        )
        val id = workoutDao.insertExercise(entity)
        val previous = workoutDao.getPreviousSets(exerciseId, type, startedAt)
        repeat(setCount) { setPosition -> insertSet(entity.copy(id = id), setPosition, previous) }
        return id
    }

    /** 채운 값은 세트 행을 만들 때 써 둔다. 체크만 눌러 완료하면 completed_at만 채운다(LOG-04). */
    private suspend fun insertSet(exercise: WorkoutExerciseEntity, position: Int, previous: List<WorkoutSetEntity>): Long {
        val value = prefillValue(
            type = exercise.recordType,
            previous = previous.firstOrNull { it.position == position }?.value,
            target = exercise.target,
        )
        return workoutDao.insertSet(
            WorkoutSetEntity(
                workoutExerciseId = exercise.id,
                position = position,
                weightG = value.weight?.grams,
                reps = value.reps,
                durationS = value.durationSeconds,
                completedAt = null,
            ),
        )
    }

    private suspend fun renumberExercises(workoutId: Long) {
        workoutDao.getExercises(workoutId).forEachIndexed { index, exercise ->
            if (exercise.position != index) workoutDao.setExercisePosition(exercise.id, index)
        }
    }

    private suspend fun renumberSets(workoutExerciseId: Long) {
        workoutDao.getSets(workoutExerciseId).forEachIndexed { index, set ->
            if (set.position != index) workoutDao.setSetPosition(set.id, index)
        }
    }

    private suspend fun readWorkout(id: Long): Workout? {
        val workout = workoutDao.getWorkout(id) ?: return null
        val exercises = workoutDao.getExercises(id)
        val sets = workoutDao.getSetsOfWorkout(id).groupBy { it.workoutExerciseId }
        val definitions = exerciseDao.getByIds(exercises.map { it.exerciseId }.toSet()).associateBy { it.id }
        return workout.toModel(exercises, sets, definitions)
    }

    private fun WorkoutEntity.toModel(
        exercises: List<WorkoutExerciseEntity>,
        sets: Map<Long, List<WorkoutSetEntity>>,
        definitions: Map<String, ExerciseEntity>,
    ): Workout {
        val workout = this
        return Workout(
            id = workout.id,
            routineId = workout.routineId,
            routineName = workout.routineName,
            startedAt = Instant.ofEpochMilli(workout.startedAt),
            finishedAt = workout.finishedAt?.let(Instant::ofEpochMilli),
            note = workout.note,
            exercises = exercises.map { exercise ->
                WorkoutExercise(
                    id = exercise.id,
                    workoutId = exercise.workoutId,
                    exercise = definitions.getValue(exercise.exerciseId).toModel(),
                    position = exercise.position,
                    recordType = exercise.recordType,
                    note = exercise.note,
                    target = exercise.target,
                    sets = sets[exercise.id].orEmpty().map { it.toModel() },
                )
            },
        )
    }

    private companion object {
        val WORKOUT_TABLES = arrayOf(Tables.WORKOUT, Tables.WORKOUT_EXERCISE, Tables.WORKOUT_SET, Tables.EXERCISE)
    }
}

/** 같은 기록 방식으로 적은 세트 가운데 최고 기록. 어느 질의를 쓸지는 운동 기록 종목의 방식이 정한다. */
internal suspend fun WorkoutDao.best(exerciseId: String, type: RecordType, excludeSetId: Long): BestSetRow? = when (type) {
    RecordType.WEIGHT_REPS -> bestWeightReps(exerciseId, excludeSetId)
    RecordType.REPS -> bestReps(exerciseId, excludeSetId)
    RecordType.DURATION -> bestDuration(exerciseId, excludeSetId)
}
