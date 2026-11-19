package io.github.zzunsik.nextset.ui.component

import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.ui.component.NumberFieldKind.DURATION
import io.github.zzunsik.nextset.ui.component.NumberFieldKind.REPS
import io.github.zzunsik.nextset.ui.component.NumberFieldKind.WEIGHT
import io.github.zzunsik.nextset.ui.component.NumberPadKey.Backspace
import io.github.zzunsik.nextset.ui.component.NumberPadKey.ClearAll
import io.github.zzunsik.nextset.ui.component.NumberPadKey.Close
import io.github.zzunsik.nextset.ui.component.NumberPadKey.DecimalPoint
import io.github.zzunsik.nextset.ui.component.NumberPadKey.Digit
import io.github.zzunsik.nextset.ui.component.NumberPadKey.Next
import io.github.zzunsik.nextset.ui.component.NumberPadKey.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 숫자판의 입력 규칙(LOG-07, LOG-08). 화면 없이 JVM에서 확인한다. */
class NumberPadInputTest {
    private fun NumberEntry.pressAll(vararg keys: NumberPadKey) = keys.fold(this) { entry, key -> entry.press(key) }

    private fun empty(kind: NumberFieldKind) = NumberEntry.of(kind, null)

    // 증감

    @Test
    fun weightStepsCombineToHalfKilogram() {
        val entry = empty(WEIGHT).pressAll(Step(2500), Step(-1000), Step(-1000))
        assertEquals(Weight(500).grams, entry.amount)
        assertEquals("0.5", entry.displayText)
    }

    @Test
    fun weightStepsUseExactGrams() {
        // 0.1kg 단위를 실수로 더하면 오차가 생기는 조합이다. 정수 그램이라 그대로 맞는다.
        val entry = NumberEntry.of(WEIGHT, 61_250).pressAll(Step(2500), Step(2500), Step(-1000))
        assertEquals(65_250, entry.amount)
        assertEquals("65.25", entry.displayText)
    }

    @Test
    fun emptyFieldStepStartsFromZero() {
        assertEquals(1000, empty(WEIGHT).press(Step(1000)).amount)
        assertEquals(2500, empty(WEIGHT).press(Step(2500)).amount)
        assertEquals(1, empty(REPS).press(Step(1)).amount)
        assertEquals(5, empty(DURATION).press(Step(5)).amount)
        assertEquals("0:05", empty(DURATION).press(Step(5)).displayText)
    }

    @Test
    fun stepNeverGoesBelowZero() {
        // 빈칸에서 빼기를 누르면 0이다.
        assertEquals(0, empty(WEIGHT).press(Step(-2500)).amount)
        assertEquals("0", empty(WEIGHT).press(Step(-1000)).displayText)
        assertEquals(0, empty(REPS).press(Step(-1)).amount)
        assertEquals(0, empty(DURATION).press(Step(-15)).amount)
        assertEquals("0:00", empty(DURATION).press(Step(-15)).displayText)
        // 값이 빼는 양보다 작으면 0에서 멈춘다.
        assertEquals(0, NumberEntry.of(WEIGHT, 500).press(Step(-1000)).amount)
        assertEquals(0, NumberEntry.of(DURATION, 10).press(Step(-15)).amount)
    }

    @Test
    fun stepNeverGoesAboveRange() {
        assertEquals(999_990, NumberEntry.of(WEIGHT, 999_000).press(Step(2500)).amount)
        assertEquals(999, NumberEntry.of(REPS, 999).press(Step(1)).amount)
        assertEquals(59 * 60 + 59, NumberEntry.of(DURATION, 59 * 60 + 50).press(Step(15)).amount)
    }

    @Test
    fun durationStepsCarryIntoMinutes() {
        val entry = NumberEntry.of(DURATION, 50).pressAll(Step(15))
        assertEquals(65, entry.amount)
        assertEquals("1:05", entry.displayText)
    }

    // 값 전체 선택

    @Test
    fun openingFieldSelectsWholeValue() {
        val entry = NumberEntry.of(WEIGHT, 60_000)
        assertTrue(entry.wholeSelected)
        assertEquals("60", entry.displayText)
    }

    @Test
    fun typingOnSelectedValueReplacesIt() {
        assertEquals(5000, NumberEntry.of(WEIGHT, 60_000).press(Digit(5)).amount)
        assertEquals(8, NumberEntry.of(REPS, 10).press(Digit(8)).amount)
        assertEquals("0:01", NumberEntry.of(DURATION, 90).press(Digit(1)).displayText)
        // 소수점을 먼저 치면 "0."에서 시작한다.
        assertEquals("0.", NumberEntry.of(WEIGHT, 60_000).press(DecimalPoint).displayText)
    }

    @Test
    fun stepOnSelectedValueAddsToIt() {
        val entry = NumberEntry.of(WEIGHT, 60_000).press(Step(2500))
        assertEquals(62_500, entry.amount)
        // 증감 뒤에도 값 전체가 선택돼 있어 숫자를 치면 새 값이 된다.
        assertTrue(entry.wholeSelected)
        assertEquals(7000, entry.press(Digit(7)).amount)
        assertEquals(11, NumberEntry.of(REPS, 10).press(Step(1)).amount)
    }

    @Test
    fun typingAfterFirstDigitAppends() {
        val entry = NumberEntry.of(WEIGHT, 60_000).pressAll(Digit(6), Digit(2), DecimalPoint, Digit(5))
        assertFalse(entry.wholeSelected)
        assertEquals(62_500, entry.amount)
        assertEquals("62.5", entry.displayText)
    }

    @Test
    fun backspaceOnSelectedValueClearsIt() {
        assertNull(NumberEntry.of(WEIGHT, 60_000).press(Backspace).amount)
        val typed = NumberEntry.of(WEIGHT, null).pressAll(Digit(6), Digit(2), DecimalPoint, Digit(5))
        assertEquals("62.", typed.press(Backspace).displayText)
        assertEquals("6", typed.pressAll(Backspace, Backspace, Backspace).displayText)
        assertNull(typed.press(ClearAll).amount)
    }

    // 입력 범위

    @Test
    fun weightAcceptsUpToTwoDecimalsAndThreeIntegerDigits() {
        val entry = empty(WEIGHT).pressAll(Digit(9), Digit(9), Digit(9), Digit(9), DecimalPoint, Digit(9), Digit(9), Digit(9))
        assertEquals("999.99", entry.displayText)
        assertEquals(999_990, entry.amount)
        assertEquals("1.25", empty(WEIGHT).pressAll(Digit(1), DecimalPoint, Digit(2), Digit(5)).displayText)
        assertEquals("1.", empty(WEIGHT).pressAll(Digit(1), DecimalPoint, DecimalPoint).displayText)
        // 맨 앞의 0은 다음 숫자로 바뀐다.
        assertEquals("5", empty(WEIGHT).pressAll(Digit(0), Digit(5)).displayText)
    }

    @Test
    fun repsAcceptUpToThreeDigitsWithoutDecimalPoint() {
        assertEquals(999, empty(REPS).pressAll(Digit(9), Digit(9), Digit(9), Digit(9)).amount)
        assertEquals("1", empty(REPS).pressAll(Digit(1), DecimalPoint).displayText)
        assertEquals("7", empty(REPS).pressAll(Digit(0), Digit(7)).displayText)
    }

    @Test
    fun durationFillsFromTheRight() {
        val entry = empty(DURATION).pressAll(Digit(1), Digit(3), Digit(0))
        assertEquals("1:30", entry.displayText)
        assertEquals(90, entry.amount)
        assertEquals("12:34", empty(DURATION).pressAll(Digit(1), Digit(2), Digit(3), Digit(4), Digit(5)).displayText)
        assertEquals("0:00", empty(DURATION).press(Digit(0)).displayText)
        // 지우기는 가장 오른쪽 숫자를 뺀다.
        assertEquals("0:13", entry.press(Backspace).displayText)
    }

    @Test
    fun durationSecondsOver59CarryIntoMinutesAndCapAt5959() {
        val entry = empty(DURATION).pressAll(Digit(7), Digit(5))
        assertEquals("0:75", entry.displayText)
        assertEquals(75, entry.amount)
        assertEquals(59 * 60 + 59, empty(DURATION).pressAll(Digit(9), Digit(9), Digit(9), Digit(9)).amount)
    }

    @Test
    fun displayTextOfFormatsStoredValues() {
        assertEquals("62.5", NumberEntry.displayTextOf(WEIGHT, 62_500))
        assertEquals("61.25", NumberEntry.displayTextOf(WEIGHT, 61_250))
        assertEquals("60", NumberEntry.displayTextOf(WEIGHT, 60_000))
        assertEquals("1:00", NumberEntry.displayTextOf(DURATION, 60))
        assertEquals("", NumberEntry.displayTextOf(REPS, null))
    }

    // 칸 종류별 키 구성

    @Test
    fun keySetDependsOnFieldKind() {
        assertEquals(listOf(-2500, -1000, 1000, 2500), WEIGHT.steps)
        assertTrue(WEIGHT.hasDecimalPoint)
        assertEquals(listOf(-1, 1), REPS.steps)
        assertFalse(REPS.hasDecimalPoint)
        assertEquals(listOf(-15, -5, 5, 15), DURATION.steps)
        assertFalse(DURATION.hasDecimalPoint)
    }

    @Test
    fun fieldsFollowRecordType() {
        assertEquals(listOf(WEIGHT, REPS), RecordType.WEIGHT_REPS.numberFieldKinds())
        assertEquals(listOf(REPS), RecordType.REPS.numberFieldKinds())
        assertEquals(listOf(DURATION), RecordType.DURATION.numberFieldKinds())
    }

    // 다음 칸 이동

    private val row = listOf(
        NumberPadField(id = "weight", kind = WEIGHT, amount = 60_000),
        NumberPadField(id = "reps", kind = REPS, amount = 10),
    )

    @Test
    fun nextMovesWeightToRepsThenFinishesOnLastField() {
        val opened = NumberPadSession.open(row, 0)
        assertFalse(opened.isLastField)

        val moved = opened.press(Next)
        val atReps = requireNotNull(moved.session)
        assertEquals(listOf(NumberPadEvent.FocusMoved("reps")), moved.events)
        assertEquals("reps", atReps.focused.id)
        assertTrue(atReps.isLastField)
        // 옮긴 칸도 값 전체가 선택된다.
        assertTrue(atReps.entry.wholeSelected)
        assertEquals("10", atReps.entry.displayText)

        val finished = atReps.press(Next)
        assertNull(finished.session)
        assertEquals(listOf(NumberPadEvent.Finished), finished.events)
    }

    @Test
    fun singleFieldRowFinishesOnFirstNext() {
        val session = NumberPadSession.open(listOf(NumberPadField(1L, DURATION, 60)), 0)
        assertTrue(session.isLastField)
        assertEquals(listOf(NumberPadEvent.Finished), session.press(Next).events)
    }

    @Test
    fun closeKeyClosesWithoutFinishing() {
        val result = NumberPadSession.open(row, 0).press(Close)
        assertNull(result.session)
        assertEquals(listOf(NumberPadEvent.Closed), result.events)
    }

    @Test
    fun everyValueChangeIsReported() {
        val session = NumberPadSession.open(row, 0)
        val stepped = session.press(Step(2500))
        assertEquals(listOf(NumberPadEvent.ValueChanged("weight", 62_500)), stepped.events)

        // 값이 같으면(62 → 62.) 알리지 않는다.
        val typed = requireNotNull(session.press(Digit(6)).session).press(Digit(2))
        val point = requireNotNull(typed.session).press(DecimalPoint)
        assertEquals(emptyList<NumberPadEvent<String>>(), point.events)

        // 다음 칸으로 옮긴 뒤에도 고친 값이 칸 목록에 남는다.
        val back = requireNotNull(requireNotNull(stepped.session).press(Next).session).focus(0)
        assertEquals("62.5", back.entry.displayText)
    }
}
