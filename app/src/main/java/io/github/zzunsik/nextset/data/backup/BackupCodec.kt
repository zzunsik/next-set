package io.github.zzunsik.nextset.data.backup

import io.github.zzunsik.nextset.domain.model.BackupProblem
import io.github.zzunsik.nextset.domain.model.BackupSummary
import io.github.zzunsik.nextset.domain.model.InvalidBackupException
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.Settings
import io.github.zzunsik.nextset.domain.rule.isValidSubPart
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * 백업 파일의 글과 [BackupFile]을 서로 바꾼다. 안드로이드에 기대지 않아 JVM 테스트로 확인한다.
 *
 * 읽기는 docs/architecture.md "가져오기"의 1, 2번이다. `format`, `formatVersion`을 먼저 보고, 옛 형식이면
 * [upgrades]로 한 단계씩 올린 뒤 지금 형식으로 읽고 [validate]로 내용을 검사한다. 데이터베이스는 건드리지 않는다.
 */
internal object BackupCodec {
    const val FORMAT = "next-set-backup"

    /** 이 앱이 쓰는 형식 버전. 데이터베이스 스키마 버전과 따로 센다. */
    const val CURRENT_VERSION = 1

    /**
     * 버전 `n`의 JSON을 `n+1`로 올리는 함수. 키가 `n`이다. 1부터 [CURRENT_VERSION] - 1까지 빠짐없이 있어야 한다.
     * 버전 1뿐이라 아직 없다. 형식 버전마다 `app/src/test/resources/backup/`에 표본을 두고 지우지 않는다.
     */
    private val upgrades: Map<Int, (JsonObject) -> JsonObject> = emptyMap()

    /** 옛 앱이 모르는 필드를 무시한다. 새 필드는 기본값을 두면 버전을 올리지 않아도 된다. */
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    /** 받지 않는 파일이면 [InvalidBackupException]. */
    fun decode(text: String): BackupFile {
        val root = try {
            json.parseToJsonElement(text.removePrefix(BYTE_ORDER_MARK))
        } catch (e: SerializationException) {
            // 이 앱의 형식 이름이 들어 있으면 저장하다 잘린 백업 파일로 본다.
            val problem = if ("\"$FORMAT\"" in text) BackupProblem.BROKEN else BackupProblem.NOT_BACKUP
            throw InvalidBackupException(problem, "JSON으로 읽을 수 없다", e)
        }
        if (root !is JsonObject || (root["format"] as? JsonPrimitive)?.takeIf { it.isString }?.content != FORMAT) {
            throw InvalidBackupException(BackupProblem.NOT_BACKUP, "format이 $FORMAT 이 아니다")
        }
        val version = (root["formatVersion"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
            ?: throw InvalidBackupException(BackupProblem.BROKEN, "formatVersion이 정수가 아니다")
        if (version > CURRENT_VERSION) {
            throw InvalidBackupException(BackupProblem.NEWER_VERSION, "형식 버전 $version 은 이 앱($CURRENT_VERSION)보다 높다")
        }
        if (version < 1) throw InvalidBackupException(BackupProblem.BROKEN, "형식 버전 $version 은 없다")

        var upgraded: JsonObject = root
        for (from in version until CURRENT_VERSION) {
            val upgrade = checkNotNull(upgrades[from]) { "형식 버전 $from 을 올리는 함수가 없다" }
            upgraded = upgrade(upgraded)
        }
        val file = try {
            json.decodeFromJsonElement(BackupFile.serializer(), upgraded)
        } catch (e: SerializationException) {
            throw InvalidBackupException(BackupProblem.BROKEN, "필드가 빠졌거나 값의 형식이 틀렸다: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw InvalidBackupException(BackupProblem.BROKEN, "값의 형식이 틀렸다: ${e.message}", e)
        }
        try {
            validate(file.copy(formatVersion = CURRENT_VERSION))
        } catch (e: IllegalArgumentException) {
            throw InvalidBackupException(BackupProblem.BROKEN, e.message ?: "내용이 규칙에 맞지 않는다", e)
        }
        return file.copy(formatVersion = CURRENT_VERSION)
    }

    /**
     * 가리키는 종목과 루틴이 파일 안에 있는지, 숫자 범위가 맞는지, 세부 부위가 부위와 맞는지,
     * 목표와 세트의 값이 기록 방식과 맞는지 본다(docs/architecture.md "가져오기" 1번). 어긋나면 [IllegalArgumentException].
     */
    fun validate(file: BackupFile) {
        require(file.format == FORMAT) { "format이 다르다" }
        require(file.formatVersion == CURRENT_VERSION) { "지금 형식 버전이 아니다" }
        require(Settings.isValidDefaultRest(file.settings.defaultRestSeconds)) {
            "기본 휴식 시간 ${file.settings.defaultRestSeconds} 초는 고를 수 없는 값이다"
        }

        val exerciseIds = HashSet<String>()
        file.exercises.forEach { exercise ->
            require(exercise.id.isNotBlank()) { "종목 식별자가 비어 있다" }
            require(exerciseIds.add(exercise.id)) { "종목 식별자가 겹친다: ${exercise.id}" }
            require(exercise.nameKo.isNotBlank()) { "종목 이름이 비어 있다: ${exercise.id}" }
            require(isValidSubPart(exercise.bodyPart, exercise.subPart)) { "세부 부위가 부위와 맞지 않는다: ${exercise.id}" }
            require(exercise.aliases.none { it.isEmpty() || '\n' in it }) { "별칭이 비었거나 줄바꿈이 있다: ${exercise.id}" }
            require(exercise.restSeconds == null || exercise.restSeconds > 0) { "종목 휴식 시간은 0보다 커야 한다: ${exercise.id}" }
        }
        val recordTypes = file.exercises.associate { it.id to it.recordType }

        val routineIds = HashSet<Long>()
        file.routines.forEach { routine ->
            require(routineIds.add(routine.id)) { "루틴 번호가 겹친다: ${routine.id}" }
            require(routine.position >= 0) { "루틴 순서는 0 이상이어야 한다: ${routine.id}" }
            routine.exercises.forEach { item ->
                val type = recordTypes[item.exerciseId]
                requireNotNull(type) { "루틴 ${routine.id} 이 파일에 없는 종목 ${item.exerciseId} 를 가리킨다" }
                require(item.setCount >= 1) { "세트 수는 1 이상이어야 한다: 루틴 ${routine.id}" }
                validateTarget(type, item.targetWeightG, item.targetRepsMin, item.targetRepsMax, item.targetDurationS)
            }
        }
        file.settings.pinnedRoutineId?.let { require(it in routineIds) { "직접 고른 루틴 $it 이 파일에 없다" } }

        require(file.workouts.count { it.finishedAt == null } <= 1) { "진행 중인 운동이 둘 이상이다" }
        file.workouts.forEach { workout ->
            require(workout.startedAt >= 0) { "시작 시각이 0보다 작다" }
            require(workout.finishedAt == null || workout.finishedAt >= workout.startedAt) { "끝난 시각이 시작 시각보다 이르다" }
            workout.routineId?.let { require(it in routineIds) { "운동 기록이 파일에 없는 루틴 $it 을 가리킨다" } }
            workout.exercises.forEach { item ->
                require(item.exerciseId in exerciseIds) { "운동 기록이 파일에 없는 종목 ${item.exerciseId} 를 가리킨다" }
                validateTarget(item.recordType, item.targetWeightG, item.targetRepsMin, item.targetRepsMax, item.targetDurationS)
                item.sets.forEach { set -> validateSet(item.recordType, set) }
            }
        }
    }

    private fun validateTarget(type: RecordType, weightG: Int?, repsMin: Int?, repsMax: Int?, durationS: Int?) {
        require(listOfNotNull(weightG, repsMin, repsMax, durationS).all { it >= 0 }) { "목표 값은 0 이상이어야 한다" }
        require(repsMin == null || repsMax == null || repsMin <= repsMax) { "목표 횟수 하한이 상한보다 크다" }
        val fits = when (type) {
            RecordType.WEIGHT_REPS -> durationS == null
            RecordType.REPS -> weightG == null && durationS == null
            RecordType.DURATION -> weightG == null && repsMin == null && repsMax == null
        }
        require(fits) { "목표에 기록 방식 $type 이 쓰지 않는 값이 있다" }
    }

    private fun validateSet(type: RecordType, set: BackupSet) {
        require(listOfNotNull(set.weightG, set.reps, set.durationS).all { it >= 0 }) { "세트 값은 0 이상이어야 한다" }
        require(set.completedAt == null || set.completedAt >= 0) { "완료 시각이 0보다 작다" }
        val fits = when (type) {
            RecordType.WEIGHT_REPS -> set.durationS == null
            RecordType.REPS -> set.weightG == null && set.durationS == null
            RecordType.DURATION -> set.weightG == null && set.reps == null
        }
        require(fits) { "세트에 기록 방식 $type 이 쓰지 않는 값이 있다" }
    }

    fun summarize(file: BackupFile) = BackupSummary(
        workoutCount = file.workouts.count { it.finishedAt != null },
        routineCount = file.routines.size,
        hasWorkoutInProgress = file.workouts.any { it.finishedAt == null },
    )

    // 글자로 적으면 lint(ByteOrderMark)가 파일 중간의 BOM으로 본다. 코드 값으로 만든다.
    private val BYTE_ORDER_MARK = Char(0xFEFF).toString()
}
