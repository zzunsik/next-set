package io.github.zzunsik.nextset.domain.model

/** 부위. 이름은 저장 값이라 배포한 뒤에는 바꾸지 않는다. */
enum class BodyPart { CHEST, BACK, SHOULDERS, ARMS, LEGS, CORE }

/** 세부 부위. 하체와 팔에만 있다. */
enum class SubPart(val bodyPart: BodyPart) {
    QUADS(BodyPart.LEGS),
    HAMSTRINGS(BodyPart.LEGS),
    GLUTES(BodyPart.LEGS),
    CALVES(BodyPart.LEGS),
    BICEPS(BodyPart.ARMS),
    TRICEPS(BodyPart.ARMS),
    FOREARMS(BodyPart.ARMS),
}

enum class Equipment { BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT, KETTLEBELL, BAND, OTHER }

/** 기록 방식(LOG-08). */
enum class RecordType { WEIGHT_REPS, REPS, DURATION }

data class Exercise(
    val id: String,
    val nameKo: String,
    val nameEn: String?,
    val aliases: List<String>,
    val bodyPart: BodyPart,
    val subPart: SubPart?,
    val equipment: Equipment,
    val recordType: RecordType,
    val isBuiltin: Boolean,
    val isArchived: Boolean,
    /** 이 종목의 휴식 시간. null이면 설정의 기본값을 쓴다(REST-03). */
    val restSeconds: Int?,
)

/** 직접 만드는 종목의 입력 값(EX-04). */
data class ExerciseDraft(
    val nameKo: String,
    val nameEn: String? = null,
    val aliases: List<String> = emptyList(),
    val bodyPart: BodyPart,
    val subPart: SubPart? = null,
    val equipment: Equipment,
    val recordType: RecordType,
    val restSeconds: Int? = null,
)

/** 종목 목록을 거르는 조건(EX-02, EX-03). 비어 있으면 숨기지 않은 종목 전체다. */
data class ExerciseFilter(
    val query: String = "",
    val bodyPart: BodyPart? = null,
    val subPart: SubPart? = null,
)

/** 직접 만든 종목을 지운 결과. 기록에 쓰인 종목은 숨긴다. */
enum class ExerciseDeleteResult { DELETED, ARCHIVED }
