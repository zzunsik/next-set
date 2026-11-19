package io.github.zzunsik.nextset.domain.model

// test-guard 확인용 주석(#43). 머지하지 않는다.

/**
 * 무게. 정수 그램으로 들고 다녀 더하고 빼도 오차가 생기지 않는다(docs/architecture.md "무게 저장 형식").
 * 62.5kg은 `Weight(62500)`이다.
 */
@JvmInline
value class Weight(val grams: Int) : Comparable<Weight> {
    operator fun plus(other: Weight): Weight = Weight(Math.addExact(grams, other.grams))

    operator fun minus(other: Weight): Weight = Weight(Math.subtractExact(grams, other.grams))

    override fun compareTo(other: Weight): Int = grams.compareTo(other.grams)

    /** kg 문자열. 뒤의 0을 뗀다. `62500`은 "62.5", `60000`은 "60", `61250`은 "61.25"다. */
    fun toKgText(): String {
        val sign = if (grams < 0) "-" else ""
        val abs = Math.abs(grams.toLong())
        val whole = abs / GRAMS_PER_KG
        val fraction = (abs % GRAMS_PER_KG).toString().padStart(3, '0').trimEnd('0')
        return if (fraction.isEmpty()) "$sign$whole" else "$sign$whole.$fraction"
    }

    override fun toString(): String = "${toKgText()}kg"

    companion object {
        private const val GRAMS_PER_KG = 1000L

        /** 증감 키 ±1. */
        val STEP_1KG = Weight(1000)

        /** 증감 키 ±2.5. */
        val STEP_2_5KG = Weight(2500)

        fun kg(kg: Int): Weight = Weight(Math.multiplyExact(kg, GRAMS_PER_KG.toInt()))

        /**
         * 숫자판에서 친 kg 문자열을 실수를 거치지 않고 그램으로 바꾼다.
         * 소수점 아래는 셋째 자리까지 받는다. 형식이 틀리거나 범위를 넘으면 null이다.
         */
        fun parseKg(text: String): Weight? {
            val match = KG_PATTERN.matchEntire(text.trim()) ?: return null
            val whole = match.groupValues[1].ifEmpty { "0" }
            val fraction = match.groupValues[2].padEnd(3, '0')
            if (match.groupValues[1].isEmpty() && match.groupValues[2].isEmpty()) return null
            val grams = whole.toLongOrNull()?.times(GRAMS_PER_KG)?.plus(fraction.toLong()) ?: return null
            return if (grams > Int.MAX_VALUE) null else Weight(grams.toInt())
        }

        private val KG_PATTERN = Regex("""(\d{0,7})(?:\.(\d{0,3}))?""")
    }
}
