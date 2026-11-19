package io.github.zzunsik.nextset.data.repository

import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PLANK
import io.github.zzunsik.nextset.data.repository.TestExercises.SQUAT
import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.ExerciseDeleteResult
import io.github.zzunsik.nextset.domain.model.ExerciseDraft
import io.github.zzunsik.nextset.domain.model.ExerciseFilter
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.Settings
import io.github.zzunsik.nextset.domain.model.SubPart
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.ThemeMode
import io.github.zzunsik.nextset.domain.model.Weight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 종목(EX-02 ~ EX-04), 루틴(RTN-01 ~ RTN-05), 설정(SET-01, SET-02, HOME-03) 저장소. */
@RunWith(RobolectricTestRunner::class)
class ExerciseAndRoutineRepositoryTest {
    private lateinit var db: TestDb

    @Before
    fun setUp() = runTest { db = TestDb().seed() }

    @After
    fun tearDown() = db.close()

    private val draft = ExerciseDraft(
        nameKo = "원암 푸시다운",
        aliases = listOf("한팔 푸시다운"),
        bodyPart = BodyPart.ARMS,
        subPart = SubPart.TRICEPS,
        equipment = Equipment.CABLE,
        recordType = RecordType.WEIGHT_REPS,
    )

    @Test
    fun searchAndFilterExercises() = runTest {
        assertEquals(4, db.exercises.observeExercises().first().size)
        assertEquals(listOf(BENCH), db.exercises.observeExercises(ExerciseFilter(query = "벤치")).first().map { it.id })
        assertEquals(listOf(SQUAT), db.exercises.observeExercises(ExerciseFilter(query = "test squat")).first().map { it.id })
        assertEquals(
            listOf(SQUAT),
            db.exercises.observeExercises(ExerciseFilter(bodyPart = BodyPart.LEGS, subPart = SubPart.QUADS)).first().map { it.id },
        )
    }

    @Test
    fun customExerciseAddUpdateDelete() = runTest {
        val id = db.exercises.addCustomExercise(draft)
        assertTrue(id.startsWith("custom-"))
        assertEquals("원암 푸시다운", db.exercises.getExercise(id)!!.nameKo)
        assertEquals(listOf(id), db.exercises.observeExercises(ExerciseFilter(query = "한팔")).first().map { it.id })

        // 부위를 바꿔 세부 부위가 맞지 않으면 비운다.
        db.exercises.updateCustomExercise(id, draft.copy(nameKo = "케이블 푸시다운 변형", bodyPart = BodyPart.CHEST))
        val updated = db.exercises.getExercise(id)!!
        assertEquals("케이블 푸시다운 변형", updated.nameKo)
        assertNull(updated.subPart)

        assertEquals(ExerciseDeleteResult.DELETED, db.exercises.deleteCustomExercise(id))
        assertNull(db.exercises.getExercise(id))
    }

    @Test
    fun deletingAUsedCustomExerciseHidesItAndRemovesItFromRoutines() = runTest {
        val id = db.exercises.addCustomExercise(draft)
        val routineId = db.routines.addRoutine("팔")
        db.routines.addRoutineExercise(routineId, BENCH, setCount = 3)
        db.routines.addRoutineExercise(routineId, id, setCount = 3)
        db.routines.addRoutineExercise(routineId, SQUAT, setCount = 3)
        val workoutId = db.finishedWorkout(at(0), mapOf(id to listOf(wr(20_000, 12))))

        assertEquals(ExerciseDeleteResult.ARCHIVED, db.exercises.deleteCustomExercise(id))
        assertTrue(db.exercises.getExercise(id)!!.isArchived)
        assertTrue(db.exercises.observeExercises().first().none { it.id == id })
        val items = db.routines.observeRoutine(routineId).first()!!.exercises
        assertEquals(listOf(BENCH, SQUAT), items.map { it.exercise.id })
        assertEquals(listOf(0, 1), items.map { it.position })
        // 지난 기록과 최고 기록에는 남는다.
        assertEquals(id, db.workouts.observeWorkout(workoutId).first()!!.exercises.single().exercise.id)
        assertEquals(wr(20_000, 12), db.workouts.observePersonalRecord(id).first()!!.value)
    }

    @Test
    fun builtinExercisesCannotBeEditedOrDeleted() = runTest {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { db.exercises.deleteCustomExercise(BENCH) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { db.exercises.updateCustomExercise(BENCH, draft) } }
        // 종목별 휴식 시간은 내장 종목에도 정한다(REST-03).
        db.exercises.setRestSeconds(BENCH, 150)
        assertEquals(150, db.exercises.getExercise(BENCH)!!.restSeconds)
        db.exercises.setRestSeconds(BENCH, null)
        assertNull(db.exercises.getExercise(BENCH)!!.restSeconds)
    }

    @Test
    fun recordTypeChangeClearsMismatchedRoutineTargets() = runTest {
        val id = db.exercises.addCustomExercise(draft)
        val routineId = db.routines.addRoutine("팔")
        db.routines.addRoutineExercise(routineId, id, setCount = 3, target = Target(weight = Weight.kg(20), repsMin = 10, repsMax = 15))

        db.exercises.updateCustomExercise(id, draft.copy(recordType = RecordType.REPS))
        assertEquals(Target(repsMin = 10, repsMax = 15), db.routines.observeRoutine(routineId).first()!!.exercises.single().target)
        db.exercises.updateCustomExercise(id, draft.copy(recordType = RecordType.WEIGHT_REPS))
        assertEquals("되돌려도 비운 목표는 돌아오지 않는다", Target(repsMin = 10, repsMax = 15), db.routines.observeRoutine(routineId).first()!!.exercises.single().target)
    }

    @Test
    fun routineCrud() = runTest {
        val push = db.routines.addRoutine(" 밀기 ")
        val pull = db.routines.addRoutine("당기기")
        val legs = db.routines.addRoutine("하체")
        assertEquals(listOf("밀기", "당기기", "하체"), db.routines.observeRoutines().first().map { it.name })

        db.routines.renameRoutine(pull, "등")
        db.routines.reorderRoutines(listOf(legs, push, pull))
        assertEquals(listOf("하체", "밀기", "등"), db.routines.observeRoutines().first().map { it.name })
        assertThrows(IllegalArgumentException::class.java) { runBlocking { db.routines.reorderRoutines(listOf(legs, push)) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { db.routines.addRoutine("  ") } }

        val bench = db.routines.addRoutineExercise(push, BENCH, setCount = 3, target = Target(weight = Weight.kg(60), repsMin = 8, repsMax = 10))
        val plank = db.routines.addRoutineExercise(push, PLANK, setCount = 2, target = Target(weight = Weight.kg(5), durationSeconds = 60))
        val squat = db.routines.addRoutineExercise(push, SQUAT, setCount = 1)
        var detail = db.routines.observeRoutine(push).first()!!
        assertEquals("시간 종목에는 시간 목표만 남는다", Target(durationSeconds = 60), detail.exercises[1].target)

        db.routines.reorderRoutineExercises(push, listOf(squat, bench, plank))
        db.routines.removeRoutineExercise(bench)
        db.routines.updateRoutineExercise(squat, setCount = 5, target = Target(weight = Weight(102_500), repsMin = 3, repsMax = 5))
        detail = db.routines.observeRoutine(push).first()!!
        assertEquals(listOf(SQUAT, PLANK), detail.exercises.map { it.exercise.id })
        assertEquals(listOf(0, 1), detail.exercises.map { it.position })
        assertEquals(5, detail.exercises[0].setCount)
        assertEquals(Weight(102_500), detail.exercises[0].target.weight)

        // 세트 수는 1 이상, 목표 하한은 상한 이하(RTN-03, RTN-04).
        assertThrows(IllegalArgumentException::class.java) { runBlocking { db.routines.updateRoutineExercise(squat, 0, Target.NONE) } }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { db.routines.updateRoutineExercise(squat, 3, Target(repsMin = 10, repsMax = 5)) }
        }

        db.routines.deleteRoutine(push)
        assertNull(db.routines.observeRoutine(push).first())
        assertEquals(listOf(0, 1), db.routines.observeRoutines().first().map { it.position })
    }

    @Test
    fun settingsReadAndWrite() = runTest {
        assertEquals(Settings(), db.settings.observeSettings().first())

        db.settings.setDefaultRestSeconds(90)
        db.settings.setThemeMode(ThemeMode.DARK)
        db.settings.setNotificationPromptDeclined(true)
        val routineId = db.routines.addRoutine("A")
        db.settings.setPinnedRoutine(routineId)
        assertEquals(
            Settings(defaultRestSeconds = 90, themeMode = ThemeMode.DARK, pinnedRoutineId = routineId, notificationPromptDeclined = true),
            db.settings.observeSettings().first(),
        )

        // SET-01: 30초부터 5분까지 15초 단위.
        listOf(15, 100, 315).forEach { seconds ->
            assertThrows(IllegalArgumentException::class.java) { runBlocking { db.settings.setDefaultRestSeconds(seconds) } }
        }
        db.settings.setDefaultRestSeconds(30)
        db.settings.setDefaultRestSeconds(300)
        assertEquals(300, db.settings.observeSettings().first().defaultRestSeconds)
    }
}
