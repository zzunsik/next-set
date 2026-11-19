package io.github.zzunsik.nextset.ui.component

import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.Weight

/*
 * 자체 숫자판(docs/DESIGN.md `NumberPad`)의 입력 규칙. Compose와 Android에 기대지 않아 JVM 테스트로 확인한다.
 *
 * 값은 칸 종류의 단위를 쓴 정수(amount)로 주고받는다. 무게는 그램(docs/architecture.md "무게 저장 형식"),
 * 횟수는 회, 시간은 초다. 빈 칸은 null이다.
 */

/** 숫자판이 편집하는 칸의 종류. 키 구성과 입력 범위가 종류마다 다르다(LOG-07, LOG-08). */
enum class NumberFieldKind(
    /** 증감 키가 더하는 양. 화면 왼쪽부터 놓는 순서다. */
    val steps: List<Int>,
    /** 소수점 키가 있는가. 무게 칸에만 있다. */
    val hasDecimalPoint: Boolean,
    /** 받을 수 있는 가장 큰 값. 가장 작은 값은 0이다. */
    val maxAmount: Int,
) {
    /** 무게. 그램. 0~999.99kg, 소수 둘째 자리까지. */
    WEIGHT(
        steps = listOf(
            -Weight.STEP_2_5KG.grams,
            -Weight.STEP_1KG.grams,
            Weight.STEP_1KG.grams,
            Weight.STEP_2_5KG.grams,
        ),
        hasDecimalPoint = true,
        maxAmount = 999_990,
    ),

    /** 횟수. 0~999. */
    REPS(steps = listOf(-1, 1), hasDecimalPoint = false, maxAmount = 999),

    /** 시간. 초. 0:00~59:59. 숫자는 오른쪽부터 채운다(1, 3, 0 → 1:30). */
    DURATION(steps = listOf(-15, -5, 5, 15), hasDecimalPoint = false, maxAmount = 59 * 60 + 59),
}

/** 기록 방식에 맞춘 한 행의 칸 순서(LOG-08). "다음" 키는 이 순서로 옮긴다(docs/DESIGN.md `NumberPad`). */
fun RecordType.numberFieldKinds(): List<NumberFieldKind> = when (this) {
    RecordType.WEIGHT_REPS -> listOf(NumberFieldKind.WEIGHT, NumberFieldKind.REPS)
    RecordType.REPS -> listOf(NumberFieldKind.REPS)
    RecordType.DURATION -> listOf(NumberFieldKind.DURATION)
}

/** 숫자판의 키. */
sealed interface NumberPadKey {
    data class Digit(val digit: Int) : NumberPadKey {
        init {
            require(digit in 0..9) { "숫자 키는 0~9다: $digit" }
        }
    }

    data object DecimalPoint : NumberPadKey

    /** 지우기를 한 번 누름. 한 글자를 지운다. 값 전체가 선택돼 있으면 값을 지운다. */
    data object Backspace : NumberPadKey

    /** 지우기를 길게 누름. 전부 지운다. */
    data object ClearAll : NumberPadKey

    /** 증감 키. [amount]는 칸 종류의 단위다(그램, 회, 초). */
    data class Step(val amount: Int) : NumberPadKey

    /** "다음". 마지막 칸에서는 "완료"다. */
    data object Next : NumberPadKey

    /** 숫자판 닫기. */
    data object Close : NumberPadKey
}

/**
 * 칸 하나를 편집하는 상태.
 *
 * @property text 친 그대로의 글자. 무게는 "62.5", "62.", 횟수는 "10", 시간은 오른쪽부터 채운 숫자 "130"(1:30)이다.
 * @property wholeSelected 값 전체가 선택된 상태. 칸을 누르면 이 상태가 된다. 숫자를 치면 값이 새 숫자로 바뀌고
 *   증감 키는 지금 값에 더하고 뺀다.
 */
data class NumberEntry(
    val kind: NumberFieldKind,
    val text: String,
    val wholeSelected: Boolean,
) {
    /** 저장할 값(그램, 회, 초). 비어 있으면 null이다. */
    val amount: Int?
        get() = if (text.isEmpty()) null else parse(kind, text)

    /** 칸에 보일 글자. 시간은 "1:30"처럼 적는다. */
    val displayText: String
        get() = when {
            text.isEmpty() -> ""
            kind == NumberFieldKind.DURATION -> text.padStart(3, '0').let {
                "${it.dropLast(2).trimStart('0').ifEmpty { "0" }}:${it.takeLast(2)}"
            }
            else -> text
        }

    /** 편집 키를 누른 결과. "다음"과 "닫기"는 칸을 바꾸지 않는다([NumberPadSession]이 다룬다). */
    fun press(key: NumberPadKey): NumberEntry = when (key) {
        is NumberPadKey.Digit -> typeDigit(key.digit)
        NumberPadKey.DecimalPoint -> typeDecimalPoint()
        NumberPadKey.Backspace -> copy(text = if (wholeSelected) "" else text.dropLast(1), wholeSelected = false)
        NumberPadKey.ClearAll -> copy(text = "", wholeSelected = false)
        is NumberPadKey.Step -> step(key.amount)
        NumberPadKey.Next, NumberPadKey.Close -> this
    }

    private fun typeDigit(digit: Int): NumberEntry {
        val base = if (wholeSelected) "" else text
        val next = when (kind) {
            NumberFieldKind.WEIGHT -> {
                val point = base.indexOf('.')
                when {
                    point >= 0 -> if (base.length - point - 1 < WEIGHT_FRACTION_DIGITS) base + digit else base
                    base == "0" -> "$digit"
                    base.length < WEIGHT_INTEGER_DIGITS -> base + digit
                    else -> base
                }
            }
            NumberFieldKind.REPS -> when {
                base == "0" -> "$digit"
                base.length < REPS_DIGITS -> base + digit
                else -> base
            }
            NumberFieldKind.DURATION -> {
                val digits = (base + digit).trimStart('0').ifEmpty { "0" }
                if (digits.length <= DURATION_DIGITS) digits else base
            }
        }
        return copy(text = next, wholeSelected = false)
    }

    private fun typeDecimalPoint(): NumberEntry {
        if (!kind.hasDecimalPoint) return this
        val base = if (wholeSelected) "" else text
        val next = when {
            base.isEmpty() -> "0."
            '.' in base -> base
            else -> "$base."
        }
        return copy(text = next, wholeSelected = false)
    }

    /** 빈 칸은 0으로 보고 더한다. 0 아래와 [NumberFieldKind.maxAmount] 위로 가지 않는다. 선택 상태는 그대로 둔다. */
    private fun step(delta: Int): NumberEntry {
        val current = amount ?: 0
        val next = when (kind) {
            NumberFieldKind.WEIGHT -> (Weight(current) + Weight(delta)).grams
            else -> Math.addExact(current, delta)
        }.coerceIn(0, kind.maxAmount)
        return copy(text = format(kind, next))
    }

    companion object {
        private const val WEIGHT_INTEGER_DIGITS = 3
        private const val WEIGHT_FRACTION_DIGITS = 2
        private const val REPS_DIGITS = 3
        private const val DURATION_DIGITS = 4

        /** 칸을 눌렀을 때의 상태. 들어 있던 값 전체가 선택된다. */
        fun of(kind: NumberFieldKind, amount: Int?): NumberEntry =
            NumberEntry(kind, if (amount == null) "" else format(kind, amount.coerceIn(0, kind.maxAmount)), true)

        /** 값을 칸에 보일 글자로. 시간은 "1:30", 무게는 뒤따르는 0을 뗀 "62.5"다. 빈 값은 "". */
        fun displayTextOf(kind: NumberFieldKind, amount: Int?): String = of(kind, amount).displayText

        private fun format(kind: NumberFieldKind, amount: Int): String = when (kind) {
            NumberFieldKind.WEIGHT -> Weight(amount).toKgText()
            NumberFieldKind.REPS -> amount.toString()
            NumberFieldKind.DURATION -> (amount / 60 * 100 + amount % 60).toString()
        }

        private fun parse(kind: NumberFieldKind, text: String): Int = when (kind) {
            NumberFieldKind.WEIGHT -> requireNotNull(Weight.parseKg(text)) { "무게 글자: $text" }.grams
            NumberFieldKind.REPS -> text.toInt()
            // 오른쪽 두 자리가 초다. 60 이상이면 분으로 넘긴다(0:75 → 1:15). 59:59를 넘지 않는다.
            NumberFieldKind.DURATION -> text.toInt().let { it / 100 * 60 + it % 100 }.coerceAtMost(kind.maxAmount)
        }
    }
}

/** 숫자판이 편집하는 칸. [id]는 화면이 칸을 알아보는 값이다(예: 세트 id와 칸 종류). */
data class NumberPadField<T>(val id: T, val kind: NumberFieldKind, val amount: Int?)

/** 숫자판이 화면에 알리는 일. */
sealed interface NumberPadEvent<out T> {
    /** 칸의 값이 바뀌었다. 키를 누를 때마다 저장한다(NF-09). */
    data class ValueChanged<T>(val id: T, val amount: Int?) : NumberPadEvent<T>

    /** "다음"으로 다른 칸을 골랐다. */
    data class FocusMoved<T>(val id: T) : NumberPadEvent<T>

    /**
     * 마지막 칸에서 "완료"를 눌러 숫자판이 닫혔다. 운동 중 화면은 세트를 완료하고 휴식 타이머를 시작한다.
     * 루틴 편집과 기록 수정은 숫자판만 닫는다(docs/DESIGN.md `NumberPad`).
     */
    data object Finished : NumberPadEvent<Nothing>

    /** "숫자판 닫기"로 닫혔다. */
    data object Closed : NumberPadEvent<Nothing>
}

/** 키를 누른 결과. 숫자판이 닫혔으면 [session]이 null이다. */
data class NumberPadResult<T>(val session: NumberPadSession<T>?, val events: List<NumberPadEvent<T>>)

/**
 * 숫자판이 열려 있는 동안의 상태. 한 행의 칸들([fields])과 지금 고른 칸을 든다.
 * 바뀌지 않는 값이라 화면 상태(ViewModel의 StateFlow, Compose의 `mutableStateOf`)에 그대로 담는다.
 *
 * 쓰는 법
 * 1. 칸을 누르면 [open]으로 연다. 그 칸의 값 전체가 선택된다.
 * 2. 키를 누를 때마다 [press]를 부르고 [NumberPadResult.events]를 화면 상태에 반영한다.
 * 3. 열린 채 다른 칸을 누르면 [focus]를 부른다.
 */
data class NumberPadSession<T>(
    val fields: List<NumberPadField<T>>,
    val focusedIndex: Int,
    val entry: NumberEntry,
) {
    init {
        require(focusedIndex in fields.indices) { "고른 칸이 칸 목록 밖이다: $focusedIndex" }
        require(entry.kind == fields[focusedIndex].kind) { "편집 상태의 칸 종류가 다르다" }
    }

    val focused: NumberPadField<T> get() = fields[focusedIndex]

    /** 행의 마지막 칸인가. 그러면 "다음" 키가 "완료"로 바뀐다. */
    val isLastField: Boolean get() = focusedIndex == fields.lastIndex

    fun press(key: NumberPadKey): NumberPadResult<T> = when (key) {
        NumberPadKey.Next ->
            if (isLastField) {
                NumberPadResult(null, listOf(NumberPadEvent.Finished))
            } else {
                val moved = focus(focusedIndex + 1)
                NumberPadResult(moved, listOf(NumberPadEvent.FocusMoved(moved.focused.id)))
            }
        NumberPadKey.Close -> NumberPadResult(null, listOf(NumberPadEvent.Closed))
        else -> {
            val edited = entry.press(key)
            val amount = edited.amount
            if (amount == focused.amount) {
                NumberPadResult(copy(entry = edited), emptyList())
            } else {
                val updated = fields.toMutableList().apply { set(focusedIndex, focused.copy(amount = amount)) }
                NumberPadResult(
                    copy(fields = updated, entry = edited),
                    listOf(NumberPadEvent.ValueChanged(focused.id, amount)),
                )
            }
        }
    }

    /** 열린 채 다른 칸을 고른다. 그 칸의 값 전체가 선택된다. */
    fun focus(index: Int): NumberPadSession<T> =
        NumberPadSession(fields, index, NumberEntry.of(fields[index].kind, fields[index].amount))

    companion object {
        /** [fields] 가운데 [index]번째 칸을 눌러 숫자판을 연다. */
        fun <T> open(fields: List<NumberPadField<T>>, index: Int): NumberPadSession<T> =
            NumberPadSession(fields, index, NumberEntry.of(fields[index].kind, fields[index].amount))
    }
}
