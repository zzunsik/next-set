package io.github.zzunsik.nextset.ui.exercise

import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.ExerciseDeleteResult
import io.github.zzunsik.nextset.domain.model.ExerciseDraft
import io.github.zzunsik.nextset.domain.model.ExerciseFilter
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SubPart
import io.github.zzunsik.nextset.domain.repository.ExerciseRepository
import io.github.zzunsik.nextset.domain.rule.filterBy
import io.github.zzunsik.nextset.domain.rule.normalized
import io.github.zzunsik.nextset.domain.rule.validate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * 화면 상태 테스트의 가짜 종목 저장소. 메모리 목록을 쓰고 검색과 거르기는 실제 domain 규칙([filterBy])으로 한다.
 * 내장 종목을 고치거나 지우면 Room 구현처럼 [IllegalArgumentException]을 던진다.
 */
class FakeExerciseRepository(initial: List<Exercise> = TestExercises.all) : ExerciseRepository {
    val exercises = MutableStateFlow(initial.sortedBy { it.nameKo })

    /** 운동 기록에 쓰인 종목. 지우면 숨기기만 한다. */
    val usedInWorkouts = mutableSetOf<String>()

    private var nextId = 1

    override fun observeExercises(filter: ExerciseFilter): Flow<List<Exercise>> =
        exercises.map { it.filterBy(filter) }

    override suspend fun getExercise(id: String): Exercise? = exercises.value.find { it.id == id }

    override suspend fun addCustomExercise(draft: ExerciseDraft): String {
        val clean = draft.normalized().also { it.validate() }
        val id = "custom-${nextId++}"
        val exercise = Exercise(
            id = id,
            nameKo = clean.nameKo,
            nameEn = clean.nameEn,
            aliases = clean.aliases,
            bodyPart = clean.bodyPart,
            subPart = clean.subPart,
            equipment = clean.equipment,
            recordType = clean.recordType,
            isBuiltin = false,
            isArchived = false,
            restSeconds = clean.restSeconds,
        )
        exercises.update { (it + exercise).sortedBy(Exercise::nameKo) }
        return id
    }

    override suspend fun updateCustomExercise(id: String, draft: ExerciseDraft) {
        val clean = draft.normalized().also { it.validate() }
        requireCustom(id)
        exercises.update { list ->
            list.map {
                if (it.id != id) {
                    it
                } else {
                    it.copy(
                        nameKo = clean.nameKo,
                        nameEn = clean.nameEn,
                        aliases = clean.aliases,
                        bodyPart = clean.bodyPart,
                        subPart = clean.subPart,
                        equipment = clean.equipment,
                        recordType = clean.recordType,
                        restSeconds = clean.restSeconds,
                    )
                }
            }.sortedBy(Exercise::nameKo)
        }
    }

    override suspend fun deleteCustomExercise(id: String): ExerciseDeleteResult {
        requireCustom(id)
        return if (id in usedInWorkouts) {
            exercises.update { list -> list.map { if (it.id == id) it.copy(isArchived = true) else it } }
            ExerciseDeleteResult.ARCHIVED
        } else {
            exercises.update { list -> list.filterNot { it.id == id } }
            ExerciseDeleteResult.DELETED
        }
    }

    override suspend fun isUsedInWorkouts(id: String): Boolean = id in usedInWorkouts

    override suspend fun setRestSeconds(id: String, seconds: Int?) {
        exercises.update { list -> list.map { if (it.id == id) it.copy(restSeconds = seconds) else it } }
    }

    private fun requireCustom(id: String) {
        val exercise = requireNotNull(exercises.value.find { it.id == id }) { "종목 $id 가 없다" }
        require(!exercise.isBuiltin) { "내장 종목은 고치거나 지울 수 없다" }
    }
}

/** 화면 테스트의 종목. 내장 종목 파일(`exercises.json`)의 몇 개를 옮기고 직접 추가한 종목 하나를 지어 넣었다. */
object TestExercises {
    private fun builtin(
        id: String,
        nameKo: String,
        nameEn: String,
        aliases: List<String>,
        bodyPart: BodyPart,
        subPart: SubPart?,
        equipment: Equipment,
        recordType: RecordType = RecordType.WEIGHT_REPS,
    ) = Exercise(id, nameKo, nameEn, aliases, bodyPart, subPart, equipment, recordType, true, false, null)

    val benchPress = builtin(
        "barbell-bench-press", "바벨 벤치 프레스", "Barbell Bench Press",
        listOf("벤치프레스", "벤치 프레스", "벤치", "바벨 벤치프레스"), BodyPart.CHEST, null, Equipment.BARBELL,
    )
    val dumbbellBenchPress = builtin(
        "dumbbell-bench-press", "덤벨 벤치 프레스", "Dumbbell Bench Press",
        listOf("덤벨 벤치프레스", "덤벨 프레스", "덤벨 벤치"), BodyPart.CHEST, null, Equipment.DUMBBELL,
    )
    val pushUp = builtin(
        "push-up", "푸시업", "Push-Up", listOf("팔굽혀펴기", "푸쉬업", "푸시 업"),
        BodyPart.CHEST, null, Equipment.BODYWEIGHT, RecordType.REPS,
    )
    val latPulldown = builtin(
        "lat-pulldown", "랫 풀다운", "Lat Pulldown", listOf("랫풀다운", "랫풀", "렛 풀다운", "렛풀다운"),
        BodyPart.BACK, null, Equipment.CABLE,
    )
    val backSquat = builtin(
        "back-squat", "백 스쿼트", "Back Squat", listOf("스쿼트", "백스쿼트", "바벨 스쿼트"),
        BodyPart.LEGS, SubPart.QUADS, Equipment.BARBELL,
    )
    val lyingLegCurl = builtin(
        "lying-leg-curl", "라잉 레그 컬", "Lying Leg Curl", listOf("라잉 레그컬", "레그 컬", "레그컬"),
        BodyPart.LEGS, SubPart.HAMSTRINGS, Equipment.MACHINE,
    )
    val closeGripBenchPress = builtin(
        "close-grip-bench-press", "클로즈 그립 벤치 프레스", "Close Grip Bench Press",
        listOf("클로즈그립 벤치프레스", "내로우 벤치프레스", "내로우 그립 벤치 프레스"),
        BodyPart.ARMS, SubPart.TRICEPS, Equipment.BARBELL,
    )
    val plank = builtin(
        "plank", "플랭크", "Plank", listOf("프론트 플랭크", "엘보 플랭크"),
        BodyPart.CORE, null, Equipment.BODYWEIGHT, RecordType.DURATION,
    )

    /** 직접 추가한 종목. 세부 부위를 정하지 않은 하체 종목이다. */
    val customSledPush = Exercise(
        id = "custom-sled", nameKo = "썰매 밀기", nameEn = null, aliases = emptyList(),
        bodyPart = BodyPart.LEGS, subPart = null, equipment = Equipment.OTHER, recordType = RecordType.DURATION,
        isBuiltin = false, isArchived = false, restSeconds = 90,
    )

    /** 지웠지만 기록이 있어 숨긴 종목. 목록에 나오지 않는다. */
    val archivedCustom = Exercise(
        id = "custom-hidden", nameKo = "숨긴 종목", nameEn = null, aliases = emptyList(),
        bodyPart = BodyPart.CHEST, subPart = null, equipment = Equipment.OTHER, recordType = RecordType.REPS,
        isBuiltin = false, isArchived = true, restSeconds = null,
    )

    val all = listOf(
        benchPress, dumbbellBenchPress, pushUp, latPulldown, backSquat, lyingLegCurl, closeGripBenchPress, plank,
        customSledPush, archivedCustom,
    )
}
