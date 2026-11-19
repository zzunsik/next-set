package io.github.zzunsik.nextset.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightTest {
    @Test
    fun repeatedStepsDoNotDrift() {
        var weight = Weight.parseKg("60")!!
        // ±2.5를 1000번 더하고 같은 수만큼 뺀다. 실수였다면 오차가 쌓일 횟수다.
        repeat(1000) { weight += Weight.STEP_2_5KG }
        assertEquals(Weight(2_560_000), weight)
        repeat(1000) { weight -= Weight.STEP_2_5KG }
        assertEquals(Weight(60_000), weight)

        // ±1과 ±2.5를 섞어 오르내린다.
        repeat(333) {
            weight += Weight.STEP_1KG
            weight += Weight.STEP_2_5KG
            weight -= Weight.STEP_1KG
        }
        assertEquals(Weight(60_000 + 333 * 2_500), weight)
        repeat(333) { weight -= Weight.STEP_2_5KG }
        assertEquals("60", weight.toKgText())

        // 0.1 단위처럼 2진 실수로 정확히 담기지 않는 값에서 출발해도 그대로 돌아온다.
        var odd = Weight.parseKg("61.3")!!
        repeat(10) { odd += Weight.STEP_2_5KG }
        repeat(10) { odd -= Weight.STEP_2_5KG }
        repeat(7) { odd -= Weight.STEP_1KG }
        repeat(7) { odd += Weight.STEP_1KG }
        assertEquals(Weight(61_300), odd)
        assertEquals("61.3", odd.toKgText())
    }

    @Test
    fun parsesKgTextWithoutFloatingPoint() {
        assertEquals(Weight(62_500), Weight.parseKg("62.5"))
        assertEquals(Weight(60_000), Weight.parseKg("60"))
        assertEquals(Weight(60_000), Weight.parseKg("60."))
        assertEquals(Weight(61_250), Weight.parseKg("61.25"))
        assertEquals(Weight(1), Weight.parseKg("0.001"))
        assertEquals(Weight(0), Weight.parseKg("0"))
        assertNull(Weight.parseKg(""))
        assertNull(Weight.parseKg("."))
        assertNull(Weight.parseKg("1.2345"))
        assertNull(Weight.parseKg("-5"))
        assertNull(Weight.parseKg("1,5"))
        assertNull(Weight.parseKg("abc"))
        assertNull(Weight.parseKg("9999999"))
    }

    @Test
    fun formatsKgTextWithoutTrailingZeros() {
        assertEquals("62.5", Weight(62_500).toKgText())
        assertEquals("60", Weight(60_000).toKgText())
        assertEquals("61.25", Weight(61_250).toKgText())
        assertEquals("0.005", Weight(5).toKgText())
        assertEquals("0", Weight(0).toKgText())
        assertEquals("-2.5", Weight(-2_500).toKgText())
        assertEquals(Weight(80_000), Weight.kg(80))
    }
}
