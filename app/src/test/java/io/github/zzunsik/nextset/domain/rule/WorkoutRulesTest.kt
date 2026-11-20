package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.Routine
import io.github.zzunsik.nextset.domain.model.RoutineDetail
import io.github.zzunsik.nextset.domain.model.RoutineExercise
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.model.WorkoutExercise
import io.github.zzunsik.nextset.domain.model.WorkoutSet
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 다음 세트(LOG-06), 세트 행의 제안 값(LOG-03), 루틴 반영(LOG-10). 이름과 값은 지어낸 것이다. */
class WorkoutRulesTest {
    private fun exercise(id: String, type: RecordType = RecordType.WEIGHT_REPS) = Exercise(
        id = id, nameKo = id, nameEn = null, aliases = emptyList(), bodyPart = BodyPart.CHEST, subPart = null,
        equipment = Equipment.BARBELL, recordType = type, isBuiltin = true, isArchived = false, restSeconds = null,
    )

    private var nextId = 1L

    private fun set(position: Int, done: Boolean) =
        WorkoutSet(nextId++, 0, position, SetValue(Weight.kg(60), 10), if (done) Instant.EPOCH else null)

    private fun we(id: Long, exerciseId: String, position: Int, vararg done: Boolean) = WorkoutExercise(
        id = id, workoutId = 1, exercise = exercise(exerciseId), position = position, recordType = RecordType.WEIGHT_REPS,
        note = null, target = Target.NONE, sets = done.mapIndexed { i, d -> set(i, d) },
    )

    private fun workout(vararg exercises: WorkoutExercise, routineId: Long? = 7) =
        Workout(1, routineId, "루틴", Instant.EPOCH, null, null, exercises.toList())

    private fun routine(vararg items: Pair<String, Int>) = RoutineDetail(
        Routine(7, "루틴", 0),
        items.mapIndexed { i, (id, count) -> RoutineExercise(100L + i, 7, exercise(id), i, count, Target(repsMin = 8)) },
    )

    @Test
    fun nextSetIsTheFirstUncompletedSetInOrder() {
        val a = we(1, "a", 0, true, false, false)
        val b = we(2, "b", 1, false)
        assertEquals(a.sets[1], workout(a, b).nextSet()!!.set)
    }

    @Test
    fun nextSetCrossesToTheNextExercise() {
        val a = we(1, "a", 0, true, true)
        val b = we(2, "b", 1, false, false)
        val next = workout(a, b).nextSet()!!
        assertEquals(b, next.exercise)
        assertEquals(b.sets[0], next.set)
    }

    @Test
    fun nextSetFollowsExercisePositionNotListOrder() {
        val first = we(1, "a", 1, false)
        val second = we(2, "b", 0, false)
        assertEquals(second, workout(first, second).nextSet()!!.exercise)
    }

    @Test
    fun noNextSetWhenAllDoneOrEmpty() {
        assertNull(workout(we(1, "a", 0, true)).nextSet())
        assertNull(workout().nextSet())
        assertNull(workout(we(1, "a", 0)).nextSet())
    }

    @Test
    fun suggestedValuePrefersPreviousThenTargetThenAboveRow() {
        val previous = SetValue(Weight.kg(70), 6)
        val target = Target(weight = Weight.kg(50), repsMin = 8, repsMax = 12)
        val above = SetValue(Weight.kg(65), 9)
        assertEquals(previous, suggestedValue(RecordType.WEIGHT_REPS, previous, target, above))
        assertEquals(SetValue(Weight.kg(50), 8), suggestedValue(RecordType.WEIGHT_REPS, null, target, above))
        // 목표에 무게가 없으면 무게만 위 행에서 가져온다.
        assertEquals(SetValue(Weight.kg(65), 8), suggestedValue(RecordType.WEIGHT_REPS, null, Target(repsMin = 8), above))
        assertEquals(above, suggestedValue(RecordType.WEIGHT_REPS, null, Target.NONE, above))
        assertEquals(SetValue.EMPTY, suggestedValue(RecordType.WEIGHT_REPS, null, Target.NONE, null))
        // 기록 방식에 쓰지 않는 값은 비운다.
        assertEquals(SetValue(reps = 9), suggestedValue(RecordType.REPS, null, Target.NONE, above))
    }

    @Test
    fun filledDependsOnRecordType() {
        assertTrue(SetValue(Weight.kg(0), 0).isFilled(RecordType.WEIGHT_REPS))
        assertFalse(SetValue(reps = 10).isFilled(RecordType.WEIGHT_REPS))
        assertTrue(SetValue(reps = 10).isFilled(RecordType.REPS))
        assertFalse(SetValue(reps = 10).isFilled(RecordType.DURATION))
        assertTrue(SetValue(durationSeconds = 60).isFilled(RecordType.DURATION))
    }

    @Test
    fun unchangedWorkoutDoesNotAsk() {
        val w = workout(we(1, "a", 0, true, false), we(2, "b", 1, true))
        assertNull(planRoutineUpdate(w, routine("a" to 2, "b" to 1)))
    }

    @Test
    fun setCountChangeIsAsked() {
        val w = workout(we(1, "a", 0, true, true, true), we(2, "b", 1, true))
        val plan = planRoutineUpdate(w, routine("a" to 2, "b" to 1))!!
        assertEquals(mapOf(100L to 3, 101L to 1), plan.keep)
        assertTrue(plan.add.isEmpty())
        assertTrue(plan.remove.isEmpty())
    }

    @Test
    fun addedRemovedAndReorderedExercisesAreAsked() {
        // 루틴: a, b, c. 운동: c, a, d(새 종목). b를 뺐다.
        val w = workout(we(1, "c", 0, true), we(2, "a", 1, true, true), we(3, "d", 2, true, false))
        val plan = planRoutineUpdate(w, routine("a" to 2, "b" to 3, "c" to 1))!!
        assertEquals(listOf(101L), plan.remove)
        assertEquals(listOf("d" to 2), plan.add)
        assertEquals(
            listOf(
                RoutineUpdatePlan.Slot.Existing(102),
                RoutineUpdatePlan.Slot.Existing(100),
                RoutineUpdatePlan.Slot.New(0),
            ),
            plan.order,
        )
        assertEquals("남는 종목의 목표는 그대로 둔다", Target(repsMin = 8), plan.targets[100])
    }

    @Test
    fun orderOnlyChangeIsAsked() {
        val w = workout(we(1, "b", 0, true), we(2, "a", 1, true))
        val plan = planRoutineUpdate(w, routine("a" to 1, "b" to 1))!!
        assertEquals(listOf(RoutineUpdatePlan.Slot.Existing(101), RoutineUpdatePlan.Slot.Existing(100)), plan.order)
    }

    @Test
    fun sameExerciseTwiceIsMatchedFromTheFront() {
        val w = workout(we(1, "a", 0, true), we(2, "a", 1, true, true))
        assertNull(planRoutineUpdate(w, routine("a" to 1, "a" to 2)))
        val plan = planRoutineUpdate(w, routine("a" to 1))!!
        assertEquals(listOf("a" to 2), plan.add)
    }
}
