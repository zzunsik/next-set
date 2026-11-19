package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.ExerciseDraft
import io.github.zzunsik.nextset.domain.model.ExerciseFilter
import io.github.zzunsik.nextset.domain.model.SubPart

/** 세부 부위가 부위와 맞는지. 하체와 팔이 아니면 세부 부위가 비어 있어야 한다. */
fun isValidSubPart(bodyPart: BodyPart, subPart: SubPart?): Boolean =
    subPart == null || subPart.bodyPart == bodyPart

/** 부위를 바꿔 세부 부위가 맞지 않게 되면 세부 부위를 비운다. */
fun ExerciseDraft.normalized(): ExerciseDraft = copy(
    nameKo = nameKo.trim(),
    nameEn = nameEn?.trim()?.ifEmpty { null },
    aliases = aliases.map(String::trim).filter(String::isNotEmpty).distinct(),
    subPart = subPart?.takeIf { isValidSubPart(bodyPart, it) },
)

/** 저장하기 전에 확인한다. 어긋나면 [IllegalArgumentException]. */
fun ExerciseDraft.validate() {
    require(nameKo.isNotBlank()) { "종목 이름이 비어 있다" }
    require(isValidSubPart(bodyPart, subPart)) { "세부 부위 $subPart 는 $bodyPart 에 속하지 않는다" }
    require(aliases.none { '\n' in it }) { "별칭에 줄바꿈을 넣을 수 없다" }
    require(restSeconds == null || restSeconds > 0) { "휴식 시간은 0보다 커야 한다" }
}

/** 검색용으로 공백을 빼고 소문자로 맞춘다. "벤치프레스"로 "벤치 프레스"를 찾는다. */
fun normalizeForSearch(text: String): String =
    text.filterNot(Char::isWhitespace).lowercase()

/** 이름(한국어, 영어)이나 별칭에 [query]가 들어 있는지(EX-02). 빈 질의는 모두 맞는다. */
fun Exercise.matchesQuery(query: String): Boolean {
    val needle = normalizeForSearch(query)
    if (needle.isEmpty()) return true
    return sequenceOf(nameKo, nameEn.orEmpty()).plus(aliases)
        .any { normalizeForSearch(it).contains(needle) }
}

/**
 * 부위와 세부 부위로 거른다(EX-03). 세부 부위를 고르면 그 값이 같은 종목만 남고,
 * 고르지 않으면 세부 부위가 빈 종목까지 그 부위 전체가 남는다.
 */
fun Exercise.matchesPart(bodyPart: BodyPart?, subPart: SubPart?): Boolean {
    if (bodyPart != null && this.bodyPart != bodyPart) return false
    if (subPart != null && this.subPart != subPart) return false
    return true
}

/** 숨긴 종목을 빼고 [filter]로 거른다. 순서는 그대로 둔다. */
fun List<Exercise>.filterBy(filter: ExerciseFilter): List<Exercise> = filter { exercise ->
    !exercise.isArchived &&
        exercise.matchesPart(filter.bodyPart, filter.subPart) &&
        exercise.matchesQuery(filter.query)
}

/**
 * [name]이 숨기지 않은 다른 종목의 이름과 겹치는지(EX-04). 검색과 같이 공백과 대소문자를 빼고
 * 한국어 이름과 영어 이름에 견준다. "벤치프레스"는 "벤치 프레스"와 겹친다.
 * 숨긴 종목은 목록에 나오지 않으므로 견주지 않는다. 고치는 종목 자신([exceptId])도 뺀다.
 */
fun List<Exercise>.hasNameConflict(name: String, exceptId: String? = null): Boolean {
    val needle = normalizeForSearch(name)
    if (needle.isEmpty()) return false
    return any { exercise ->
        !exercise.isArchived && exercise.id != exceptId &&
            (normalizeForSearch(exercise.nameKo) == needle || normalizeForSearch(exercise.nameEn.orEmpty()) == needle)
    }
}
