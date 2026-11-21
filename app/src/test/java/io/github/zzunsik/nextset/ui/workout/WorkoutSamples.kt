package io.github.zzunsik.nextset.ui.workout

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
import io.github.zzunsik.nextset.ui.component.NumberFieldKind
import io.github.zzunsik.nextset.ui.component.NumberPadField
import io.github.zzunsik.nextset.ui.component.NumberPadSession
import io.github.zzunsik.nextset.ui.component.numberFieldKinds
import java.time.Instant

/** 운동 중 화면 테스트의 견본. 종목 이름과 값은 지어낸 것이다. */
object WorkoutSamples {
    val startedAt: Instant = Instant.parse("2026-03-14T09:00:00Z")
    val now: Instant = startedAt.plusSeconds(32 * 60)

    fun exercise(id: String, name: String, type: RecordType = RecordType.WEIGHT_REPS) = Exercise(
        id = id, nameKo = name, nameEn = null, aliases = emptyList(), bodyPart = BodyPart.CHEST, subPart = null,
        equipment = Equipment.BARBELL, recordType = type, isBuiltin = true, isArchived = false, restSeconds = null,
    )

    private var nextSetId = 1L

    data class SampleSet(val value: SetValue, val done: Boolean = false)

    fun done(weightKg: Double, reps: Int) = SampleSet(wr(weightKg, reps), done = true)

    fun todo(weightKg: Double?, reps: Int?) = SampleSet(SetValue(weightKg?.let { kg(it) }, reps))

    fun wr(weightKg: Double, reps: Int) = SetValue(kg(weightKg), reps)

    fun kg(value: Double) = Weight((value * 1000).toInt())

    fun block(
        id: Long,
        name: String,
        sets: List<SampleSet>,
        type: RecordType = RecordType.WEIGHT_REPS,
        target: Target = Target.NONE,
        note: String? = null,
    ) = WorkoutExercise(
        id = id,
        workoutId = 1,
        exercise = exercise("sample-$id", name, type),
        position = id.toInt(),
        recordType = type,
        note = note,
        target = target,
        sets = sets.mapIndexed { index, set ->
            WorkoutSet(nextSetId++, id, index, set.value, if (set.done) startedAt.plusSeconds(60L * index) else null)
        },
    )

    fun workout(vararg exercises: WorkoutExercise, routineName: String? = "가슴 루틴") =
        Workout(1, if (routineName == null) null else 1, routineName, startedAt, null, null, exercises.toList())

    fun state(
        workout: Workout,
        previous: Map<Long, List<SetValue>> = emptyMap(),
        records: Set<Long> = emptySet(),
        numberPadOn: Pair<Long, NumberFieldKind>? = null,
        dialog: WorkoutDialog? = null,
    ): WorkoutUiState {
        val pad = numberPadOn?.let { (setId, kind) ->
            val exercise = workout.exercises.first { e -> e.sets.any { it.id == setId } }
            val set = exercise.sets.first { it.id == setId }
            val kinds = exercise.recordType.numberFieldKinds()
            NumberPadSession.open(
                kinds.map { NumberPadField(SetFieldId(setId, it), it, set.value.amountOf(it)) },
                kinds.indexOf(kind),
            )
        }
        return buildWorkoutUiState(workout, previous, pad, records, dialog)
    }

    /** 벤치 두 세트 완료(둘째가 최고 기록), 셋째가 다음 세트. 풀업(횟수만), 플랭크(시간)가 뒤따른다. */
    fun typical(numberPadOn: Pair<Long, NumberFieldKind>? = null): WorkoutUiState {
        nextSetId = 1
        val bench = block(
            1, "벤치 프레스",
            listOf(done(60.0, 10), done(62.5, 8), todo(62.5, 8), todo(62.5, 8)),
            target = Target(weight = kg(60.0), repsMin = 8, repsMax = 12),
            note = "그립 한 뼘 넓게",
        )
        val pullUp = block(2, "풀업", listOf(todo(null, 8), todo(null, null)), RecordType.REPS)
        val plank = block(3, "플랭크", listOf(SampleSet(SetValue(durationSeconds = 60))), RecordType.DURATION)
        val previous = mapOf(
            1L to listOf(wr(60.0, 10), wr(60.0, 9), wr(60.0, 8)),
            2L to listOf(SetValue(reps = 8)),
            3L to listOf(SetValue(durationSeconds = 45)),
        )
        return state(workout(bench, pullUp, plank), previous, records = setOf(2L), numberPadOn = numberPadOn)
    }

    fun empty(): WorkoutUiState = state(workout(routineName = null))

    fun many(): WorkoutUiState {
        nextSetId = 1
        val names = listOf("스쿼트", "레그 프레스", "루마니안 데드리프트", "레그 컬", "레그 익스텐션", "카프 레이즈")
        val blocks = names.mapIndexed { i, name ->
            block(i.toLong() + 1, name, List(3) { todo(40.0 + i * 10, 10) })
        }
        return state(workout(*blocks.toTypedArray(), routineName = "하체 루틴"))
    }

    fun allDone(): WorkoutUiState {
        nextSetId = 1
        val bench = block(1, "벤치 프레스", listOf(done(60.0, 10), done(62.5, 8)))
        val fly = block(2, "덤벨 플라이", listOf(done(14.0, 12), done(14.0, 12)))
        return state(workout(bench, fly))
    }
}
