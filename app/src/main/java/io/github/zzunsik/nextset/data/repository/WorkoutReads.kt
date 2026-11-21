package io.github.zzunsik.nextset.data.repository

import io.github.zzunsik.nextset.data.db.ExerciseEntity
import io.github.zzunsik.nextset.data.db.NextSetDatabase
import io.github.zzunsik.nextset.data.db.WorkoutEntity
import io.github.zzunsik.nextset.data.db.WorkoutExerciseEntity
import io.github.zzunsik.nextset.data.db.WorkoutSetEntity
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.model.WorkoutExercise
import java.time.Instant

// 운동 기록 하나를 종목과 세트까지 읽는다. 운동 기록 저장소와 홈 데이터(RoomRoutineRepository.observeHome)가 함께 쓴다.

internal suspend fun NextSetDatabase.readWorkout(id: Long): Workout? {
    val workoutDao = workoutDao()
    val workout = workoutDao.getWorkout(id) ?: return null
    val exercises = workoutDao.getExercises(id)
    val sets = workoutDao.getSetsOfWorkout(id).groupBy { it.workoutExerciseId }
    val definitions = exerciseDao().getByIds(exercises.map { it.exerciseId }.toSet()).associateBy { it.id }
    return workout.toModel(exercises, sets, definitions)
}

internal fun WorkoutEntity.toModel(
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
        isEdited = workout.isEdited,
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
