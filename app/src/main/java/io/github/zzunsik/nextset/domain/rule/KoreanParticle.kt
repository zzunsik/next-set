package io.github.zzunsik.nextset.domain.rule

/**
 * 이름 뒤에 붙는 조사. 앞말 끝소리에 받침이 있을 때와 없을 때의 꼴이다.
 * "(으)로"만 받침이 ㄹ이면 받침이 없을 때와 같이 "로"를 쓴다("팔로", "풀로").
 */
enum class KoreanParticle(val afterFinal: String, val afterVowel: String) {
    /** 을/를 */
    OBJECT("을", "를"),

    /** 이/가 */
    SUBJECT("이", "가"),

    /** 은/는 */
    TOPIC("은", "는"),

    /** 과/와 */
    AND("과", "와"),

    /** 으로/로 */
    DIRECTION("으로", "로"),
}

/** 앞말 끝소리. */
enum class FinalSound { NONE, RIEUL, OTHER }

/**
 * [text]를 소리 내어 읽을 때의 끝소리. 정할 수 없으면 null이다(docs/DESIGN.md "문구"의 조사 규칙).
 *
 * - 끝의 공백과 닫는 괄호·따옴표는 건너뛰고 그 앞 글자를 본다("가슴 (B)"는 B).
 * - 한글 음절: 받침으로 정한다.
 * - 숫자: 마지막 숫자를 읽은 소리로 정한다. 영·일·삼·육·칠·팔은 받침이 있고 일·칠·팔은 ㄹ이다.
 *   "10", "100"처럼 0으로 끝나도 십·백·천·만 모두 ㄹ이 아닌 받침이라 영과 같다.
 * - 영어 글자: 앞이 영어 글자가 아닌 한 글자("Day A", "Plan X")만 글자 이름으로 읽는다.
 *   L(엘)·R(알)은 ㄹ, M(엠)·N(엔)은 다른 받침, 나머지는 받침이 없다(X는 엑스, Z는 지·제트 어느 쪽이든 받침이 없다).
 * - 그 밖(영어 낱말, 한글 자모, 기호, 이모지 등): 읽는 소리를 글자만으로 알 수 없어 null이다.
 *   "Push"(푸시)와 "Back"(백)처럼 영어 낱말은 철자로 받침을 가릴 수 없다.
 */
fun finalSoundOf(text: String): FinalSound? {
    val trimmed = text.trimEnd { it.isWhitespace() || it in CLOSING_MARKS }
    val last = trimmed.lastOrNull() ?: return null
    return when (last) {
        in '가'..'힣' -> when ((last - '가') % JONGSEONG_COUNT) {
            0 -> FinalSound.NONE
            JONGSEONG_RIEUL -> FinalSound.RIEUL
            else -> FinalSound.OTHER
        }
        in '0'..'9' -> when (last) {
            in DIGITS_WITH_RIEUL -> FinalSound.RIEUL
            in DIGITS_WITH_OTHER_FINAL -> FinalSound.OTHER
            else -> FinalSound.NONE
        }
        in 'A'..'Z', in 'a'..'z' -> {
            val before = trimmed.getOrNull(trimmed.length - 2)
            if (before != null && before.isLatinLetter()) return null
            when (last.uppercaseChar()) {
                in LETTERS_WITH_RIEUL -> FinalSound.RIEUL
                in LETTERS_WITH_OTHER_FINAL -> FinalSound.OTHER
                else -> FinalSound.NONE
            }
        }
        else -> null
    }
}

/** [text]의 끝소리에 맞는 [particle]의 꼴. 끝소리를 정할 수 없으면 null이다. */
fun particleFor(text: String, particle: KoreanParticle): String? = when (finalSoundOf(text)) {
    null -> null
    FinalSound.NONE -> particle.afterVowel
    FinalSound.RIEUL -> if (particle == KoreanParticle.DIRECTION) particle.afterVowel else particle.afterFinal
    FinalSound.OTHER -> particle.afterFinal
}

/**
 * [text] 끝의 공백을 지우고 [particle]을 붙인다. "가슴 루틴" → "가슴 루틴을".
 * 끝소리를 정할 수 없으면 null이다. 그때 화면은 "(을)를" 꼴 대신 조사가 필요 없는 문장으로 바꿔 적는다.
 */
fun withParticle(text: String, particle: KoreanParticle): String? =
    particleFor(text, particle)?.let { text.trimEnd() + it }

private fun Char.isLatinLetter() = this in 'A'..'Z' || this in 'a'..'z'

/** 한글 음절 하나에 들어가는 받침의 가짓수(받침 없음 포함). */
private const val JONGSEONG_COUNT = 28

/** 받침 순서에서 ㄹ의 번호. */
private const val JONGSEONG_RIEUL = 8

/** 일, 칠, 팔. */
private const val DIGITS_WITH_RIEUL = "178"

/** 영, 삼, 육. */
private const val DIGITS_WITH_OTHER_FINAL = "036"

/** 엘, 알. */
private const val LETTERS_WITH_RIEUL = "LR"

/** 엠, 엔. */
private const val LETTERS_WITH_OTHER_FINAL = "MN"

/** 끝소리를 볼 때 건너뛰는 닫는 괄호와 따옴표. */
private const val CLOSING_MARKS = ")]}\"'”’」』〉》"
