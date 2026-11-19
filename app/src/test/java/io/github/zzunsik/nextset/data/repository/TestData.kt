package io.github.zzunsik.nextset.data.repository

import io.github.zzunsik.nextset.data.db.NextSetDatabase
import io.github.zzunsik.nextset.data.seed.BuiltinExerciseFile
import io.github.zzunsik.nextset.data.seed.BuiltinExerciseSeeder
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Weight
import java.time.Instant
import org.robolectric.RuntimeEnvironment

/** 테스트용 종목. 이름과 값은 지어냈다. */
internal object TestExercises {
    const val BENCH = "test-bench"
    const val SQUAT = "test-squat"
    const val PULL_UP = "test-pull-up"
    const val PLANK = "test-plank"

    val FILE_TEXT = """
        {"version": 1, "exercises": [
          {"id":"$BENCH","nameKo":"테스트 벤치","nameEn":"Test Bench","aliases":["벤치"],"bodyPart":"CHEST","equipment":"BARBELL","recordType":"WEIGHT_REPS"},
          {"id":"$SQUAT","nameKo":"테스트 스쿼트","nameEn":"Test Squat","bodyPart":"LEGS","subPart":"QUADS","equipment":"BARBELL","recordType":"WEIGHT_REPS"},
          {"id":"$PULL_UP","nameKo":"테스트 풀업","nameEn":"Test Pull-Up","bodyPart":"BACK","equipment":"BODYWEIGHT","recordType":"REPS"},
          {"id":"$PLANK","nameKo":"테스트 플랭크","nameEn":"Test Plank","bodyPart":"CORE","equipment":"BODYWEIGHT","recordType":"DURATION"}
        ]}
    """.trimIndent()
}

/** 메모리 데이터베이스와 저장소 구현을 묶는다. 테스트마다 새로 만든다. */
internal class TestDb(val database: NextSetDatabase = NextSetDatabase.inMemory(RuntimeEnvironment.getApplication())) :
    AutoCloseable {
    val seeder = BuiltinExerciseSeeder(database) { TestExercises.FILE_TEXT }
    val exercises = RoomExerciseRepository(database, seeder)
    val routines = RoomRoutineRepository(database)
    val workouts = RoomWorkoutRepository(database)
    val settings = RoomSettingsRepository(database)

    suspend fun seed(): TestDb = apply { seeder.seed(BuiltinExerciseFile.parse(TestExercises.FILE_TEXT)) }

    override fun close() = database.close()
}

internal fun at(minutes: Long): Instant = Instant.parse("2026-01-01T00:00:00Z").plusSeconds(minutes * 60)

internal fun wr(grams: Int, reps: Int) = SetValue(weight = Weight(grams), reps = reps)

internal fun reps(reps: Int) = SetValue(reps = reps)

internal fun seconds(seconds: Int) = SetValue(durationSeconds = seconds)

/**
 * 끝난 운동 하나를 만든다. [sets]는 종목마다 완료한 세트 값이다.
 * [incomplete]에 든 종목에는 완료하지 않은 세트를 하나 더한다.
 */
internal suspend fun TestDb.finishedWorkout(
    start: Instant,
    sets: Map<String, List<SetValue>>,
    incomplete: Set<String> = emptySet(),
): Long {
    val workoutId = workouts.addPastWorkout(start, start.plusSeconds(3600))
    sets.forEach { (exerciseId, values) ->
        val weId = workouts.addExercise(workoutId, exerciseId, setCount = 0)
        values.forEach { value ->
            val setId = workouts.addSet(weId)
            workouts.updateSet(setId, value)
            workouts.completeSet(setId, start.plusSeconds(60))
        }
        if (exerciseId in incomplete) workouts.updateSet(workouts.addSet(weId), wr(999_000, 99))
    }
    return workoutId
}
