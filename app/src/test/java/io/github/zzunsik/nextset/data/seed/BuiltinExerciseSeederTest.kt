package io.github.zzunsik.nextset.data.seed

import io.github.zzunsik.nextset.data.db.NextSetDatabase
import io.github.zzunsik.nextset.data.repository.RoomExerciseRepository
import io.github.zzunsik.nextset.data.repository.RoomRoutineRepository
import io.github.zzunsik.nextset.data.repository.RoomWorkoutRepository
import io.github.zzunsik.nextset.data.repository.at
import io.github.zzunsik.nextset.data.repository.wr
import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.ExerciseDraft
import io.github.zzunsik.nextset.domain.model.ExerciseFilter
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** 내장 종목을 넣고 앱이 올라갈 때 반영한다(EX-01, docs/architecture.md "앱이 올라갈 때"). */
@RunWith(RobolectricTestRunner::class)
class BuiltinExerciseSeederTest {
    private val context = RuntimeEnvironment.getApplication()
    private val database = NextSetDatabase.inMemory(context)

    @After
    fun tearDown() = database.close()

    private fun file(version: Int, vararg exercises: String) =
        BuiltinExerciseFile.parse("""{"version":$version,"exercises":[${exercises.joinToString(",")}]}""")

    private fun exercise(
        id: String,
        nameKo: String,
        recordType: String = "WEIGHT_REPS",
        extra: String = "",
    ) = """{"id":"$id","nameKo":"$nameKo","nameEn":"$id","bodyPart":"CHEST","equipment":"BARBELL","recordType":"$recordType"$extra}"""

    @Test
    fun firstRunInsertsTheBundledFile() = runTest {
        // 앱의 실제 assets 파일을 읽는다.
        val seeder = BuiltinExerciseSeeder(database) {
            context.assets.open(BuiltinExerciseSeeder.ASSET_NAME).bufferedReader().use { it.readText() }
        }
        val repository = RoomExerciseRepository(database, seeder)
        val all = repository.observeExercises().first()
        assertEquals(130, all.size)
        assertTrue(all.all { it.isBuiltin && !it.isArchived })
        // 검색이 별칭을 본다.
        assertTrue(repository.observeExercises(ExerciseFilter(query = "벤치프레스")).first().isNotEmpty())

        // 같은 판 번호면 다시 반영하지 않는다.
        assertFalse(seeder.seed(BuiltinExerciseFile.parse(context.assets.open("exercises.json").bufferedReader().readText())))
    }

    @Test
    fun upgradeKeepsUserDataAndAppliesChanges() = runTest {
        // 저장소가 처음 읽을 때 부르는 ensureSeeded도 마지막으로 반영한 파일을 읽게 한다.
        var current = ""
        val seeder = BuiltinExerciseSeeder(database) { current }
        suspend fun seed(version: Int, vararg exercises: String): Boolean {
            current = """{"version":$version,"exercises":[${exercises.joinToString(",")}]}"""
            return seeder.seed(file(version, *exercises))
        }
        val exercises = RoomExerciseRepository(database, seeder)
        val routines = RoomRoutineRepository(database)
        val workouts = RoomWorkoutRepository(database)

        assertTrue(seed(1, exercise("press", "프레스"), exercise("row", "로우"), exercise("dip", "딥스")))

        // 사용자 데이터: 직접 만든 종목, 휴식 시간, 루틴 목표, 기록.
        val customId = exercises.addCustomExercise(
            ExerciseDraft(nameKo = "내 종목", bodyPart = BodyPart.CORE, equipment = Equipment.OTHER, recordType = RecordType.REPS),
        )
        exercises.setRestSeconds("press", 180)
        val routineId = routines.addRoutine("루틴")
        routines.addRoutineExercise(routineId, "row", setCount = 3, target = Target(weight = Weight.kg(40), repsMin = 8, repsMax = 12))
        val workoutId = workouts.addPastWorkout(at(0), at(60))
        val pressSet = workouts.addSet(workouts.addExercise(workoutId, "press", setCount = 0))
        workouts.updateSet(pressSet, wr(60_000, 5))
        workouts.completeSet(pressSet, at(1))
        val before = workouts.observeWorkout(workoutId).first()!!

        // 판 2: 이름 바꿈, 기록 방식 바꿈, 은퇴, 새 종목.
        assertTrue(
            seed(
                2,
                exercise("press", "새 프레스", extra = ""","aliases":["프레스"]"""),
                exercise("row", "로우", recordType = "REPS"),
                exercise("dip", "딥스", extra = ""","retired":true"""),
                exercise("fly", "플라이"),
            ),
        )

        val press = exercises.getExercise("press")!!
        assertEquals("새 프레스", press.nameKo)
        assertEquals(listOf("프레스"), press.aliases)
        assertEquals("사용자가 정한 휴식 시간은 그대로", 180, press.restSeconds)
        assertEquals(RecordType.REPS, exercises.getExercise("row")!!.recordType)
        assertTrue(exercises.getExercise("dip")!!.isArchived)
        assertEquals(
            listOf("새 프레스", "로우", "내 종목", "플라이").sorted(),
            exercises.observeExercises().first().map { it.nameKo }.sorted(),
        )
        assertTrue(exercises.getExercise(customId)!!.let { !it.isBuiltin && it.nameKo == "내 종목" })

        // 지난 기록은 식별자로 이어져 새 이름으로 보이고 값과 방식은 그대로다.
        val after = workouts.observeWorkout(workoutId).first()!!
        assertEquals("새 프레스", after.exercises.single().exercise.nameKo)
        assertEquals(before.exercises.single().sets, after.exercises.single().sets)
        assertEquals(RecordType.WEIGHT_REPS, after.exercises.single().recordType)

        // 기록 방식이 바뀐 종목의 루틴 목표에서 맞지 않는 열이 빈다.
        val rowTarget = routines.observeRoutine(routineId).first()!!.exercises.single().target
        assertEquals(Target(repsMin = 8, repsMax = 12), rowTarget)

        // 은퇴한 종목을 되살리면 다시 보인다.
        seed(3, exercise("press", "새 프레스"), exercise("row", "로우", "REPS"), exercise("dip", "딥스"), exercise("fly", "플라이"))
        assertFalse(exercises.getExercise("dip")!!.isArchived)
    }
}
