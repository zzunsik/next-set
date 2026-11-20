package io.github.zzunsik.nextset.data.seed

import androidx.room3.withWriteTransaction
import io.github.zzunsik.nextset.data.db.ExerciseEntity
import io.github.zzunsik.nextset.data.db.NextSetDatabase
import io.github.zzunsik.nextset.data.repository.fitRoutineTargets
import io.github.zzunsik.nextset.data.repository.getOrDefault
import io.github.zzunsik.nextset.data.repository.joinAliases
import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SubPart
import io.github.zzunsik.nextset.domain.rule.isValidSubPart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** `app/src/main/assets/exercises.json`의 형식. 원본은 docs/exercise-list.md의 표다. */
@Serializable
internal data class BuiltinExerciseFile(
    /** 판 번호. 종목을 고치면 올린다. */
    val version: Int,
    val exercises: List<BuiltinExercise>,
) {
    /** 식별자가 겹치지 않고 슬러그 형식이며 세부 부위가 부위와 맞는지. 어긋나면 [IllegalArgumentException]. */
    fun validate() {
        require(version >= 1) { "판 번호는 1 이상이어야 한다" }
        val duplicated = exercises.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicated.isEmpty()) { "식별자가 겹친다: $duplicated" }
        exercises.forEach { exercise ->
            require(SLUG.matches(exercise.id)) { "슬러그 형식이 아니다: ${exercise.id}" }
            require(exercise.nameKo.isNotBlank()) { "한국어 이름이 비어 있다: ${exercise.id}" }
            require(isValidSubPart(exercise.bodyPart, exercise.subPart)) { "세부 부위가 부위와 맞지 않는다: ${exercise.id}" }
            require(exercise.bodyPart !in PARTS_WITH_SUB_PART || exercise.subPart != null) {
                "하체와 팔 종목에는 세부 부위가 있어야 한다: ${exercise.id}"
            }
            require(exercise.aliases.none { it.isBlank() || '\n' in it }) { "별칭이 비었거나 줄바꿈이 있다: ${exercise.id}" }
        }
    }

    companion object {
        private val SLUG = Regex("[a-z0-9]+(-[a-z0-9]+)*")
        private val PARTS_WITH_SUB_PART = setOf(BodyPart.LEGS, BodyPart.ARMS)
        private val json = Json { ignoreUnknownKeys = false }

        fun parse(text: String): BuiltinExerciseFile =
            json.decodeFromString(serializer(), text).also { it.validate() }
    }
}

@Serializable
internal data class BuiltinExercise(
    val id: String,
    val nameKo: String,
    val nameEn: String,
    val aliases: List<String> = emptyList(),
    val bodyPart: BodyPart,
    val subPart: SubPart? = null,
    val equipment: Equipment,
    val recordType: RecordType,
    /** 뺀 종목. 파일에서 지우지 않고 이 값을 붙인다. 행은 숨겨지고 지난 기록에는 남는다. */
    val retired: Boolean = false,
)

/**
 * 내장 종목을 데이터베이스에 반영한다(docs/architecture.md "내장 종목").
 * 파일의 판 번호가 `app_settings.builtin_seed_version`과 다르면 한 트랜잭션에서 식별자 기준으로 반영한다.
 * 사용자의 기록, 직접 만든 종목, 종목별 휴식 시간은 건드리지 않는다.
 */
internal class BuiltinExerciseSeeder(
    private val database: NextSetDatabase,
    private val loadFile: suspend () -> String,
) {
    private val mutex = Mutex()

    @Volatile
    private var done = false

    /** 앱이 켜진 뒤 처음 부를 때 한 번 반영한다. */
    suspend fun ensureSeeded() {
        if (done) return
        mutex.withLock {
            if (!done) {
                seed(BuiltinExerciseFile.parse(loadFile()))
                done = true
            }
        }
    }

    /** 앱에 든 내장 종목 파일을 읽는다. 백업을 가져올 때 트랜잭션 밖에서 미리 읽어 [seed]에 넘긴다. */
    suspend fun readFile(): BuiltinExerciseFile = BuiltinExerciseFile.parse(loadFile())

    /** 판 번호가 다를 때만 반영한다. 반영했으면 참이다. */
    suspend fun seed(file: BuiltinExerciseFile): Boolean = database.withWriteTransaction {
        val settingsDao = database.settingsDao()
        val exerciseDao = database.exerciseDao()
        val settings = settingsDao.getOrDefault()
        if (settings.builtinSeedVersion == file.version) return@withWriteTransaction false
        file.exercises.forEach { builtin ->
            val current = exerciseDao.getById(builtin.id)
            if (current == null) {
                exerciseDao.insert(
                    ExerciseEntity(
                        id = builtin.id,
                        nameKo = builtin.nameKo,
                        nameEn = builtin.nameEn,
                        aliases = builtin.aliases.joinAliases(),
                        bodyPart = builtin.bodyPart,
                        subPart = builtin.subPart,
                        equipment = builtin.equipment,
                        recordType = builtin.recordType,
                        isBuiltin = true,
                        isArchived = builtin.retired,
                    ),
                )
            } else {
                check(current.isBuiltin) { "내장 종목 식별자 ${builtin.id} 를 직접 만든 종목이 쓰고 있다" }
                exerciseDao.update(
                    current.copy(
                        nameKo = builtin.nameKo,
                        nameEn = builtin.nameEn,
                        aliases = builtin.aliases.joinAliases(),
                        bodyPart = builtin.bodyPart,
                        subPart = builtin.subPart,
                        equipment = builtin.equipment,
                        recordType = builtin.recordType,
                        isArchived = builtin.retired,
                        // rest_seconds는 사용자가 정한 값이라 그대로 둔다.
                    ),
                )
                if (current.recordType != builtin.recordType) {
                    fitRoutineTargets(database, builtin.id, builtin.recordType)
                }
            }
        }
        settingsDao.upsert(settings.copy(builtinSeedVersion = file.version))
        true
    }

    companion object {
        const val ASSET_NAME = "exercises.json"
    }
}
