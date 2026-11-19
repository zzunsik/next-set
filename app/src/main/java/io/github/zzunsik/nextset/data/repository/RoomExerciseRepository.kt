package io.github.zzunsik.nextset.data.repository

import androidx.room3.withWriteTransaction
import io.github.zzunsik.nextset.data.db.ExerciseEntity
import io.github.zzunsik.nextset.data.db.NextSetDatabase
import io.github.zzunsik.nextset.data.db.Tables
import io.github.zzunsik.nextset.data.seed.BuiltinExerciseSeeder
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.ExerciseDeleteResult
import io.github.zzunsik.nextset.domain.model.ExerciseDraft
import io.github.zzunsik.nextset.domain.model.ExerciseFilter
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.repository.ExerciseRepository
import io.github.zzunsik.nextset.domain.rule.filterBy
import io.github.zzunsik.nextset.domain.rule.fitTo
import io.github.zzunsik.nextset.domain.rule.normalized
import io.github.zzunsik.nextset.domain.rule.validate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class RoomExerciseRepository(
    private val database: NextSetDatabase,
    private val seeder: BuiltinExerciseSeeder,
) : ExerciseRepository {
    private val exerciseDao = database.exerciseDao()
    private val routineDao = database.routineDao()

    override fun observeExercises(filter: ExerciseFilter): Flow<List<Exercise>> = flow {
        seeder.ensureSeeded()
        // 검색은 질의로 하지 않는다. 전체(수백 행)를 읽어 domain 규칙으로 거른다(docs/architecture.md "exercise").
        emitAll(database.observe(Tables.EXERCISE) { exerciseDao.getAll().map { it.toModel() }.filterBy(filter) })
    }

    override suspend fun getExercise(id: String): Exercise? {
        seeder.ensureSeeded()
        return exerciseDao.getById(id)?.toModel()
    }

    override suspend fun addCustomExercise(draft: ExerciseDraft): String {
        val clean = draft.normalized().also { it.validate() }
        val id = CUSTOM_PREFIX + UUID.randomUUID()
        exerciseDao.insert(
            ExerciseEntity(
                id = id,
                nameKo = clean.nameKo,
                nameEn = clean.nameEn,
                aliases = clean.aliases.joinAliases(),
                bodyPart = clean.bodyPart,
                subPart = clean.subPart,
                equipment = clean.equipment,
                recordType = clean.recordType,
                isBuiltin = false,
                restSeconds = clean.restSeconds,
            ),
        )
        return id
    }

    override suspend fun updateCustomExercise(id: String, draft: ExerciseDraft) {
        val clean = draft.normalized().also { it.validate() }
        database.withWriteTransaction {
            val current = requireCustom(id)
            exerciseDao.update(
                current.copy(
                    nameKo = clean.nameKo,
                    nameEn = clean.nameEn,
                    aliases = clean.aliases.joinAliases(),
                    bodyPart = clean.bodyPart,
                    subPart = clean.subPart,
                    equipment = clean.equipment,
                    recordType = clean.recordType,
                    restSeconds = clean.restSeconds,
                ),
            )
            if (current.recordType != clean.recordType) {
                fitRoutineTargets(database, id, clean.recordType)
            }
        }
    }

    override suspend fun deleteCustomExercise(id: String): ExerciseDeleteResult = database.withWriteTransaction {
        requireCustom(id)
        val affectedRoutines = routineDao.getItemsByExercise(id).map { it.routineId }.toSet()
        val result = if (exerciseDao.isUsedInWorkouts(id)) {
            // 기록에 쓰인 종목은 지우지 않고 숨긴다. 루틴에서는 뺀다.
            exerciseDao.setArchived(id, true)
            routineDao.deleteItemsByExercise(id)
            ExerciseDeleteResult.ARCHIVED
        } else {
            exerciseDao.deleteById(id) // 루틴 종목은 외래 키(CASCADE)로 함께 지워진다.
            ExerciseDeleteResult.DELETED
        }
        affectedRoutines.forEach { renumberRoutineItems(database, it) }
        result
    }

    override suspend fun setRestSeconds(id: String, seconds: Int?) {
        require(seconds == null || seconds > 0) { "휴식 시간은 0보다 커야 한다" }
        require(exerciseDao.setRestSeconds(id, seconds) == 1) { "종목 $id 가 없다" }
    }

    private suspend fun requireCustom(id: String): ExerciseEntity {
        val exercise = requireNotNull(exerciseDao.getById(id)) { "종목 $id 가 없다" }
        require(!exercise.isBuiltin) { "내장 종목은 고치거나 지울 수 없다" }
        return exercise
    }

    companion object {
        const val CUSTOM_PREFIX = "custom-"
    }
}

/** 종목의 기록 방식이 바뀌면 루틴에서 새 방식에 쓰지 않는 목표를 비운다(docs/architecture.md "기록 방식을 바꿀 때"). */
internal suspend fun fitRoutineTargets(database: NextSetDatabase, exerciseId: String, type: RecordType) {
    val routineDao = database.routineDao()
    routineDao.getItemsByExercise(exerciseId).forEach { item ->
        val fitted = item.target.fitTo(type)
        if (fitted != item.target) routineDao.updateItem(item.withTarget(fitted))
    }
}

/** 루틴 안의 종목 순서를 0부터 다시 매긴다. */
internal suspend fun renumberRoutineItems(database: NextSetDatabase, routineId: Long) {
    val routineDao = database.routineDao()
    routineDao.getItems(routineId).forEachIndexed { index, item ->
        if (item.position != index) routineDao.setItemPosition(item.id, index)
    }
}
