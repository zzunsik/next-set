package io.github.zzunsik.nextset.data.repository

import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PLANK
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.ExerciseDraft
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.rule.bestOf
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 최고 기록(HIST-04)과 세트를 완료할 때의 판정(LOG-14). */
@RunWith(RobolectricTestRunner::class)
class PersonalRecordTest {
    private lateinit var db: TestDb

    @Before
    fun setUp() = runTest { db = TestDb().seed() }

    @After
    fun tearDown() = db.close()

    private suspend fun best(exerciseId: String): SetValue? = db.workouts.observePersonalRecord(exerciseId).first()?.value

    @Test
    fun weightRepsHeaviestThenMostReps() = runTest {
        db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10), wr(62_500, 6))))
        db.finishedWorkout(at(100), mapOf(BENCH to listOf(wr(62_500, 8), wr(55_000, 12))))
        assertEquals(wr(62_500, 8), best(BENCH))
    }

    @Test
    fun repsOnlyMostReps() = runTest {
        db.finishedWorkout(at(0), mapOf(PULL_UP to listOf(reps(10), reps(12))))
        db.finishedWorkout(at(100), mapOf(PULL_UP to listOf(reps(11))))
        assertEquals(reps(12), best(PULL_UP))
    }

    @Test
    fun durationLongest() = runTest {
        db.finishedWorkout(at(0), mapOf(PLANK to listOf(seconds(60), seconds(95))))
        db.finishedWorkout(at(100), mapOf(PLANK to listOf(seconds(90))))
        assertEquals(seconds(95), best(PLANK))
    }

    @Test
    fun tiesKeepTheEarliestSet() = runTest {
        val first = db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 8))))
        db.finishedWorkout(at(100), mapOf(BENCH to listOf(wr(60_000, 8))))
        val record = db.workouts.observePersonalRecord(BENCH).first()!!
        assertEquals(first, record.workoutId)
        assertEquals(RecordType.WEIGHT_REPS, record.recordType)
    }

    @Test
    fun recomputedAfterEditAndDelete() = runTest {
        db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10))))
        val recordWorkout = db.finishedWorkout(at(100), mapOf(BENCH to listOf(wr(70_000, 5))))
        assertEquals(wr(70_000, 5), best(BENCH))

        // 지난 기록을 고치면(HIST-05) 다음 조회가 바뀐 세트에서 다시 계산한다.
        val setId = db.workouts.observeWorkout(recordWorkout).first()!!.exercises.single().sets.single().id
        db.workouts.updateSet(setId, wr(57_500, 5))
        assertEquals(wr(60_000, 10), best(BENCH))

        db.workouts.updateSet(setId, wr(60_000, 11))
        assertEquals(wr(60_000, 11), best(BENCH))

        db.workouts.deleteWorkout(recordWorkout)
        assertEquals(wr(60_000, 10), best(BENCH))

        db.workouts.deleteSet(db.workouts.observeHistory().first().single().let { summary ->
            db.workouts.observeWorkout(summary.id).first()!!.exercises.single().sets.single().id
        })
        assertNull(best(BENCH))
    }

    @Test
    fun completingASetReportsNewRecordOnlyAgainstEarlierRecords() = runTest {
        val workoutId = db.workouts.startEmpty(at(0))
        val weId = db.workouts.addExercise(workoutId, BENCH, setCount = 0)

        suspend fun complete(value: SetValue, minute: Long): Boolean {
            val setId = db.workouts.addSet(weId)
            db.workouts.updateSet(setId, value)
            return db.workouts.completeSet(setId, at(minute)).isNewRecord
        }

        // 같은 방식으로 적은 이전 운동이 없으면 알리지 않는다. 처음 하는 종목의 모든 세트가 기록이 되는 것을 막는다.
        assertFalse(complete(wr(60_000, 10), 1))
        assertFalse(complete(wr(70_000, 10), 2))
        db.workouts.finishWorkout(workoutId, at(30))

        val next = db.workouts.startEmpty(at(100))
        val nextWe = db.workouts.addExercise(next, BENCH, setCount = 0)
        suspend fun completeNext(value: SetValue, minute: Long): Boolean {
            val setId = db.workouts.addSet(nextWe)
            db.workouts.updateSet(setId, value)
            return db.workouts.completeSet(setId, at(minute)).isNewRecord
        }
        assertFalse("같으면 알리지 않는다", completeNext(wr(70_000, 10), 101))
        assertTrue("무게가 같아도 횟수가 늘면 알린다", completeNext(wr(70_000, 11), 102))
        assertFalse("방금 세운 기록보다 낮다", completeNext(wr(70_000, 11), 103))
        assertTrue(completeNext(wr(72_500, 1), 104))
    }

    @Test
    fun editingPastWorkoutDoesNotNotify() = runTest {
        db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10))))
        val past = db.workouts.addPastWorkout(at(100), at(160))
        val weId = db.workouts.addExercise(past, BENCH, setCount = 0)
        val setId = db.workouts.addSet(weId)
        db.workouts.updateSet(setId, wr(100_000, 10))
        assertFalse(db.workouts.completeSet(setId, at(120)).isNewRecord)
        assertEquals(wr(100_000, 10), best(BENCH))
    }

    @Test
    fun queryAndDomainRuleAgree() = runTest {
        // 질의의 ORDER BY와 domain의 isBetter가 같은 결과를 내는지 무작위 조합으로 견준다.
        val random = Random(20)
        repeat(5) { round ->
            val values = List(12) { wr(random.nextInt(4, 8) * 2_500, random.nextInt(1, 6)) }
            val db = TestDb().seed()
            try {
                values.chunked(4).forEachIndexed { index, chunk ->
                    db.finishedWorkout(at(index * 100L), mapOf(BENCH to chunk))
                }
                val record = db.workouts.observePersonalRecord(BENCH).first()!!
                val expected = bestOf(RecordType.WEIGHT_REPS, values)
                assertEquals("round $round: $values", expected, record.value)
                // 같은 값이 여럿이면 처음 나온 세트가 남는다.
                val firstIndex = values.indexOfFirst { it == expected }
                val sets = db.workouts.observeHistory().first().sortedBy { it.startedAt }
                    .flatMap { db.workouts.observeWorkout(it.id).first()!!.exercises.single().sets }
                assertEquals(sets[firstIndex].id, record.setId)
            } finally {
                db.close()
            }
        }
    }

    @Test
    fun recordTypeChangeComparesOnlySetsOfTheSameType() = runTest {
        val id = db.exercises.addCustomExercise(
            ExerciseDraft(nameKo = "내 운동", bodyPart = BodyPart.CORE, equipment = Equipment.OTHER, recordType = RecordType.WEIGHT_REPS),
        )
        val draft = db.exercises.getExercise(id)!!.let {
            ExerciseDraft(nameKo = it.nameKo, bodyPart = it.bodyPart, equipment = it.equipment, recordType = RecordType.REPS)
        }
        db.finishedWorkout(at(0), mapOf(id to listOf(wr(20_000, 10))))
        assertEquals(wr(20_000, 10), best(id))

        db.exercises.updateCustomExercise(id, draft)
        assertNull("방식을 바꾸면 새 방식으로 적은 세트만 본다", best(id))
        assertEquals(emptyList<SetValue>(), db.workouts.getPreviousSets(id, RecordType.REPS, at(50)))

        db.finishedWorkout(at(100), mapOf(id to listOf(reps(15))))
        assertEquals(reps(15), best(id))

        db.exercises.updateCustomExercise(id, draft.copy(recordType = RecordType.WEIGHT_REPS))
        assertEquals("되돌리면 예전 세트가 돌아온다", wr(20_000, 10), best(id))
        // 지난 세트는 적을 당시의 방식대로 남는다.
        val types = db.workouts.observeHistory().first()
            .map { db.workouts.observeWorkout(it.id).first()!!.exercises.single().recordType }
        assertEquals(listOf(RecordType.REPS, RecordType.WEIGHT_REPS), types)
    }
}
