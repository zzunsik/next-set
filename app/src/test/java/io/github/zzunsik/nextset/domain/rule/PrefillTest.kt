package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import org.junit.Assert.assertEquals
import org.junit.Test

class PrefillTest {
    private val target = Target(weight = Weight.kg(50), repsMin = 8, repsMax = 12)

    @Test
    fun previousRecordComesFirst() {
        val previous = SetValue(weight = Weight(52_500), reps = 10)
        assertEquals(previous, prefillValue(RecordType.WEIGHT_REPS, previous, target))
    }

    @Test
    fun withoutPreviousRecordUsesTargetWithLowerReps() {
        assertEquals(
            SetValue(weight = Weight.kg(50), reps = 8),
            prefillValue(RecordType.WEIGHT_REPS, previous = null, target = target),
        )
        assertEquals(SetValue(reps = 12), prefillValue(RecordType.REPS, null, Target(repsMax = 12)))
        assertEquals(SetValue(durationSeconds = 60), prefillValue(RecordType.DURATION, null, Target(durationSeconds = 60)))
    }

    @Test
    fun withoutPreviousRecordAndTargetLeavesBlank() {
        assertEquals(SetValue.EMPTY, prefillValue(RecordType.WEIGHT_REPS, null, Target.NONE))
        assertEquals(SetValue.EMPTY, prefillValue(RecordType.DURATION, null, Target.NONE))
    }

    @Test
    fun valuesOutsideTheRecordTypeAreDropped() {
        val mixed = SetValue(weight = Weight.kg(10), reps = 5, durationSeconds = 30)
        assertEquals(SetValue(reps = 5), prefillValue(RecordType.REPS, mixed, Target.NONE))
        assertEquals(SetValue(durationSeconds = 30), prefillValue(RecordType.DURATION, mixed, Target.NONE))
        assertEquals(Target(repsMin = 8, repsMax = 12), target.fitTo(RecordType.REPS))
        assertEquals(Target.NONE, target.fitTo(RecordType.DURATION))
    }
}
