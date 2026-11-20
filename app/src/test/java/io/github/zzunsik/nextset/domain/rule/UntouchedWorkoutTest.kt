package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.model.WorkoutExercise
import io.github.zzunsik.nextset.domain.model.WorkoutSet
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 손대지 않은 운동의 규칙(docs/DESIGN.md "한 번 눌러 시작", #73). 이름과 값은 지어낸 것이다. */
class UntouchedWorkoutTest {
    private val bench = Exercise(
        id = "bench", nameKo = "벤치", nameEn = null, aliases = emptyList(), bodyPart = BodyPart.CHEST, subPart = null,
        equipment = Equipment.BARBELL, recordType = RecordType.WEIGHT_REPS, isBuiltin = true, isArchived = false,
        restSeconds = null,
    )

    private fun set(id: Long, done: Boolean = false) =
        WorkoutSet(id, 10, id.toInt(), SetValue(Weight.kg(60), 10), if (done) Instant.EPOCH else null)

    private fun exercise(vararg sets: WorkoutSet, note: String? = null) = WorkoutExercise(
        id = 10, workoutId = 1, exercise = bench, position = 0, recordType = RecordType.WEIGHT_REPS,
        note = note, target = Target.NONE, sets = sets.toList(),
    )

    private fun workout(
        vararg exercises: WorkoutExercise,
        edited: Boolean = false,
        note: String? = null,
        finishedAt: Instant? = null,
    ) = Workout(1, 7, "루틴", Instant.EPOCH, finishedAt, note, exercises.toList(), isEdited = edited)

    @Test
    fun freshlyStartedWorkoutIsUntouched() {
        assertTrue("루틴에서 시작해 미리 채운 세트만 있다", workout(exercise(set(0), set(1))).isUntouched())
        assertTrue("빈 운동을 시작한 직후", workout().isUntouched())
    }

    @Test
    fun aCompletedSetMakesItTouched() {
        assertFalse(workout(exercise(set(0, done = true), set(1))).isUntouched())
    }

    @Test
    fun anEditRecordedByTheRepositoryMakesItTouched() {
        // 값 바꾸기, 세트·종목 더하기와 빼기와 옮기기, 완료했다 풀기는 저장소가 isEdited로 적는다.
        assertFalse(workout(exercise(set(0)), edited = true).isUntouched())
        assertFalse("종목을 모두 빼도 손댄 운동이다", workout(edited = true).isUntouched())
    }

    @Test
    fun notesMakeItTouched() {
        assertFalse(workout(exercise(set(0)), note = "지어낸 메모").isUntouched())
        assertFalse(workout(exercise(set(0), note = "지어낸 종목 메모")).isUntouched())
    }

    @Test
    fun finishedWorkoutIsNeverUntouched() {
        assertFalse(workout(exercise(set(0)), finishedAt = Instant.EPOCH).isUntouched())
    }
}
