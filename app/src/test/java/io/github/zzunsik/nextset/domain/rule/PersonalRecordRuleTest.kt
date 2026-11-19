package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Weight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalRecordRuleTest {
    private fun wr(kg: Int, reps: Int) = SetValue(weight = Weight.kg(kg), reps = reps)
    private fun reps(reps: Int) = SetValue(reps = reps)
    private fun seconds(s: Int) = SetValue(durationSeconds = s)

    @Test
    fun weightRepsHeavierWinsAndSameWeightMoreRepsWins() {
        val type = RecordType.WEIGHT_REPS
        assertTrue(isBetter(type, wr(62, 5), wr(60, 10)))
        assertFalse(isBetter(type, wr(60, 12), wr(62, 5)))
        // 무게가 같으면 횟수가 많은 쪽이다(확인이 남은 항목 3번).
        assertTrue(isBetter(type, wr(60, 9), wr(60, 8)))
        assertFalse(isBetter(type, wr(60, 7), wr(60, 8)))
        // 모두 같으면 좋지 않다. 먼저 세운 쪽이 남는다.
        assertFalse(isBetter(type, wr(60, 8), wr(60, 8)))
    }

    @Test
    fun repsOnlyMoreRepsWins() {
        val type = RecordType.REPS
        assertTrue(isBetter(type, reps(13), reps(12)))
        assertFalse(isBetter(type, reps(12), reps(12)))
        assertFalse(isBetter(type, reps(11), reps(12)))
    }

    @Test
    fun durationLongerWins() {
        val type = RecordType.DURATION
        assertTrue(isBetter(type, seconds(91), seconds(90)))
        assertFalse(isBetter(type, seconds(90), seconds(90)))
        assertFalse(isBetter(type, seconds(30), seconds(90)))
    }

    @Test
    fun emptyValuesAreLowest() {
        assertFalse(isBetter(RecordType.WEIGHT_REPS, SetValue(reps = 10), wr(20, 1)))
        assertTrue(isBetter(RecordType.WEIGHT_REPS, wr(20, 1), SetValue(reps = 10)))
        assertFalse(isBetter(RecordType.REPS, SetValue.EMPTY, reps(0)))
    }

    @Test
    fun noEarlierRecordMeansNoNotification() {
        assertFalse(isNewRecord(RecordType.WEIGHT_REPS, wr(100, 1), best = null))
        assertTrue(isNewRecord(RecordType.WEIGHT_REPS, wr(100, 1), best = wr(90, 5)))
        assertFalse(isNewRecord(RecordType.WEIGHT_REPS, wr(90, 5), best = wr(90, 5)))
    }

    @Test
    fun bestOfKeepsTheFirstOnTies() {
        val first = wr(60, 8)
        val sameAsFirst = wr(60, 8)
        val best = bestOf(RecordType.WEIGHT_REPS, listOf(wr(50, 10), first, sameAsFirst, wr(60, 7)))
        assertTrue(best === first)
        assertNull(bestOf(RecordType.REPS, emptyList()))
        assertEquals(seconds(60), bestOf(RecordType.DURATION, listOf(seconds(30), seconds(60), seconds(45))))
    }
}
