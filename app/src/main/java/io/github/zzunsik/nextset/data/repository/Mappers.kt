package io.github.zzunsik.nextset.data.repository

import io.github.zzunsik.nextset.data.db.BestSetRow
import io.github.zzunsik.nextset.data.db.ExerciseEntity
import io.github.zzunsik.nextset.data.db.RoutineEntity
import io.github.zzunsik.nextset.data.db.RoutineExerciseEntity
import io.github.zzunsik.nextset.data.db.WorkoutExerciseEntity
import io.github.zzunsik.nextset.data.db.WorkoutSetEntity
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.Routine
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.domain.model.WorkoutSet
import java.time.Instant

// 엔티티와 domain 모델을 서로 바꾼다. 엔티티는 data 밖으로 내보내지 않는다.

private const val ALIAS_SEPARATOR = "\n"

internal fun List<String>.joinAliases(): String = joinToString(ALIAS_SEPARATOR)

internal fun String.splitAliases(): List<String> = if (isEmpty()) emptyList() else split(ALIAS_SEPARATOR)

internal fun ExerciseEntity.toModel() = Exercise(
    id = id,
    nameKo = nameKo,
    nameEn = nameEn,
    aliases = aliases.splitAliases(),
    bodyPart = bodyPart,
    subPart = subPart,
    equipment = equipment,
    recordType = recordType,
    isBuiltin = isBuiltin,
    isArchived = isArchived,
    restSeconds = restSeconds,
)

internal fun RoutineEntity.toModel() = Routine(id = id, name = name, position = position)

internal val RoutineExerciseEntity.target: Target
    get() = Target(targetWeightG?.let(::Weight), targetRepsMin, targetRepsMax, targetDurationS)

internal fun RoutineExerciseEntity.withTarget(target: Target) = copy(
    targetWeightG = target.weight?.grams,
    targetRepsMin = target.repsMin,
    targetRepsMax = target.repsMax,
    targetDurationS = target.durationSeconds,
)

internal val WorkoutExerciseEntity.target: Target
    get() = Target(targetWeightG?.let(::Weight), targetRepsMin, targetRepsMax, targetDurationS)

internal val WorkoutSetEntity.value: SetValue
    get() = SetValue(weightG?.let(::Weight), reps, durationS)

internal val BestSetRow.value: SetValue
    get() = SetValue(weightG?.let(::Weight), reps, durationS)

internal fun WorkoutSetEntity.toModel() = WorkoutSet(
    id = id,
    workoutExerciseId = workoutExerciseId,
    position = position,
    value = value,
    completedAt = completedAt?.let(Instant::ofEpochMilli),
)
