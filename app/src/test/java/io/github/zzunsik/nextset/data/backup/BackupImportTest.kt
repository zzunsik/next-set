package io.github.zzunsik.nextset.data.backup

import io.github.zzunsik.nextset.data.repository.TestDb
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PLANK
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.TestExercises.SQUAT
import io.github.zzunsik.nextset.data.repository.at
import io.github.zzunsik.nextset.data.repository.finishedWorkout
import io.github.zzunsik.nextset.data.repository.reps
import io.github.zzunsik.nextset.data.repository.seconds
import io.github.zzunsik.nextset.data.repository.wr
import io.github.zzunsik.nextset.domain.model.BackupProblem
import io.github.zzunsik.nextset.domain.model.BackupSummary
import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.ExerciseDeleteResult
import io.github.zzunsik.nextset.domain.model.ExerciseDraft
import io.github.zzunsik.nextset.domain.model.InvalidBackupException
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.Settings
import io.github.zzunsik.nextset.domain.model.SubPart
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.ThemeMode
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 백업 내보내기와 가져오기를 실제 Room 데이터베이스(메모리)로 확인한다(DATA-03, DATA-04).
 * 기록과 이름은 지어냈다.
 */
@RunWith(RobolectricTestRunner::class)
class BackupImportTest {
    @Test
    fun exportWipeImportGivesTheSameContentIncludingMixedRecordTypes() = runTest {
        TestDb().seed().use { source ->
            val custom = source.fillWithMadeUpRecords()
            val exported = source.backup().exportText(NOW)

            // 종목의 지금 방식(횟수만)과 지난 기록의 방식(무게와 횟수)이 다른 종목이 파일에 그대로 들어간다.
            val file = BackupCodec.decode(exported)
            assertEquals(RecordType.REPS, file.exercises.single { it.id == custom }.recordType)
            assertEquals(
                listOf(RecordType.WEIGHT_REPS, RecordType.REPS),
                file.workouts.flatMap { it.exercises }.filter { it.exerciseId == custom }.map { it.recordType },
            )
            // 기기 상태는 넣지 않는다.
            assertFalse(exported.contains("notification", ignoreCase = true))
            assertFalse(exported.contains("seed", ignoreCase = true))

            // 새 기기(빈 데이터베이스)에 가져온다.
            TestDb().use { target ->
                target.backup().replaceAll(target.backup().check(exported))
                assertEquals(normalized(exported), normalized(target.backup().exportText(NOW)))

                assertEquals(2, target.workouts.observeHistory().first().size)
                assertEquals("상체", target.routines.observeTopRoutine().first()?.name)
                assertEquals("하체", target.workouts.observeInProgressWorkout().first()?.routineName)
                assertTrue(target.exercises.getExercise(custom)!!.isArchived)
                assertEquals(150, target.exercises.getExercise(SQUAT)!!.restSeconds)
                val settings = target.settings.observeSettings().first()
                assertEquals(150, settings.defaultRestSeconds)
                assertEquals(ThemeMode.DARK, settings.themeMode)
                assertEquals("상체", target.routines.observeRoutine(settings.pinnedRoutineId!!).first()?.routine?.name)
                // 최고 기록은 가져온 세트에서 다시 계산된다.
                assertEquals(reps(15), target.workouts.observePersonalRecord(custom).first()?.value)
            }

            // 같은 기기에서 기록을 바꾼 뒤 가져오면 바꾼 것이 사라지고 파일과 같아진다(합치지 않는다).
            source.routines.addRoutine("나중에 더한 루틴")
            source.finishedWorkout(at(60 * 24 * 5), mapOf(BENCH to listOf(wr(100_000, 1))))
            source.backup().replaceAll(source.backup().check(exported))
            assertEquals(normalized(exported), normalized(source.backup().exportText(NOW)))
        }
    }

    @Test
    fun sampleOfEachFormatVersionImports() = runTest {
        for (version in 1..BackupCodec.CURRENT_VERSION) {
            TestDb().seed().use { db ->
                db.backup().replaceAll(db.backup().check(BackupSamples.read(version)))
                assertEquals(2, db.workouts.observeHistory().first().size)
                assertEquals(listOf("가슴과 삼두", "등"), db.routines.observeRoutines().first().map { it.name })
            }
        }
    }

    @Test
    fun oldBackupImportsAfterBuiltinExercisesChanged() = runTest {
        // 표본은 내장 종목 test-bench의 옛 이름을 갖고, test-squat와 test-plank가 없고,
        // 이 앱의 내장 종목 파일에 없는 test-retired-curl을 갖는다.
        TestDb().seed().use { db ->
            db.backup().replaceAll(db.backup().check(BackupSamples.read(1)))

            val bench = db.exercises.getExercise(BENCH)!!
            // 이름 같은 값은 이 앱의 내장 종목 파일로 다시 맞추고, 사용자가 정한 휴식 시간은 둔다.
            assertEquals("테스트 벤치", bench.nameKo)
            assertEquals(listOf("벤치"), bench.aliases)
            assertEquals(180, bench.restSeconds)
            // 파일에 없던 내장 종목이 생긴다.
            assertNotNull(db.exercises.getExercise(SQUAT))
            assertNotNull(db.exercises.getExercise(PLANK))
            // 이 앱이 모르는 내장 종목과 그 기록은 그대로 남는다.
            val curl = db.exercises.getExercise("test-retired-curl")!!
            assertTrue(curl.isBuiltin)
            assertEquals("다른 판의 컬", curl.nameKo)
            assertEquals(wr(12_500, 10), db.workouts.observePersonalRecord("test-retired-curl").first()?.value)

            val settings = db.settings.observeSettings().first()
            assertEquals(ThemeMode.DARK, settings.themeMode)
            assertEquals(90, settings.defaultRestSeconds)
            assertEquals("가슴과 삼두", db.routines.observeRoutine(settings.pinnedRoutineId!!).first()?.routine?.name)

            val inProgress = db.workouts.observeInProgressWorkout().first()!!
            assertEquals("등", inProgress.routineName)
            assertEquals(listOf(true, false), inProgress.exercises.single().sets.map { it.isCompleted })
            assertEquals(
                listOf(reps(9)),
                db.workouts.getPreviousSets(PULL_UP, RecordType.REPS, Instant.ofEpochMilli(1794990000000)),
            )
            assertEquals(
                BackupSummary(workoutCount = 2, routineCount = 2, hasWorkoutInProgress = true),
                db.backup().currentSummary(),
            )
        }
    }

    @Test
    fun rejectedFilesLeaveExistingRecordsUntouched() = runTest {
        val sample = BackupSamples.read(1)
        val rejected = mapOf(
            BackupProblem.NEWER_VERSION to sample.replace("\"formatVersion\": 1", "\"formatVersion\": 2"),
            BackupProblem.BROKEN to sample.replace("\"exerciseId\": \"test-retired-curl\"", "\"exerciseId\": \"nowhere\""),
            BackupProblem.BROKEN to sample.substring(0, 500),
            BackupProblem.NOT_BACKUP to "{\"format\": \"other-app\", \"formatVersion\": 1}",
        )
        TestDb().seed().use { db ->
            db.fillWithMadeUpRecords()
            val before = db.backup().exportText(NOW)
            rejected.forEach { (problem, text) ->
                try {
                    db.backup().check(text)
                    fail("거부하지 않았다: $problem")
                } catch (e: InvalidBackupException) {
                    assertEquals(problem, e.problem)
                }
                assertEquals(before, db.backup().exportText(NOW))
            }
        }
    }

    @Test
    fun failureInTheMiddleOfImportRollsBack() = runTest {
        // 검사를 통과하지만 넣는 도중에 실패하는 파일: 직접 만든 종목이 이 앱의 내장 종목 식별자를 쓴다.
        // 테이블을 비우고 파일의 행을 넣은 뒤 내장 종목을 반영하다가 실패한다.
        val clash = BackupSamples.read(1).replace("custom-0b9f6c1e-3a57-4d2c-9e41-7f2a5c8d1e60", SQUAT)
        TestDb().seed().use { db ->
            db.fillWithMadeUpRecords()
            db.settings.setNotificationPromptDeclined(true)
            val before = db.backup().exportText(NOW)
            val beforeSettings = db.settings.observeSettings().first()

            val checked = db.backup().check(clash)
            try {
                db.backup().replaceAll(checked)
                fail("가져오기가 실패해야 한다")
            } catch (_: IllegalStateException) {
            }

            assertEquals(before, db.backup().exportText(NOW))
            assertEquals(beforeSettings, db.settings.observeSettings().first())
        }
    }

    @Test
    fun deviceStateIsKeptAcrossImport() = runTest {
        TestDb().seed().use { db ->
            db.settings.setNotificationPromptDeclined(true)
            val exportedAt = Instant.parse("2026-11-10T09:30:00Z")
            db.backup().markExported(exportedAt)
            val recorder = RecordingSettings(db.settings)

            db.backup(recorder).replaceAll(db.backup().check(BackupSamples.read(1)))

            // 알림 켜기 안내를 거절했는지와 마지막으로 내보낸 시각은 이 기기의 값을 그대로 둔다.
            assertTrue(db.settings.observeSettings().first().notificationPromptDeclined)
            assertEquals(exportedAt, db.backup().observeLastExportedAt().first())
            // 가져온 화면 모드는 설정 저장소의 쓰기 경로로도 쓴다.
            assertEquals(listOf(ThemeMode.DARK), recorder.themeModes)
        }
    }

    @Test
    fun lastExportedAtStartsEmptyAndFollowsMarkExported() = runTest {
        TestDb().use { db ->
            assertNull(db.backup().observeLastExportedAt().first())
            db.backup().markExported(NOW)
            assertEquals(NOW, db.backup().observeLastExportedAt().first())
        }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-11-18T00:30:00Z")
    }
}

internal fun TestDb.backup(settings: SettingsRepository = this.settings) =
    RoomBackupRepository(database, seeder, settings, BackupApp(versionName = "0.1.0", versionCode = 1))

/**
 * 지어낸 기록을 넣는다. 직접 만든 종목은 무게와 횟수로 한 번 기록한 뒤 횟수만으로 바꾸고 다시 기록하고 지워서(숨김) 둔다.
 * 직접 만든 종목의 식별자를 돌려준다.
 */
private suspend fun TestDb.fillWithMadeUpRecords(): String {
    val draft = ExerciseDraft(
        nameKo = "지어낸 케이블 컬",
        aliases = listOf("케이블컬"),
        bodyPart = BodyPart.ARMS,
        subPart = SubPart.BICEPS,
        equipment = Equipment.CABLE,
        recordType = RecordType.WEIGHT_REPS,
        restSeconds = 75,
    )
    val custom = exercises.addCustomExercise(draft)
    exercises.setRestSeconds(SQUAT, 150)
    val legs = routines.addRoutine("하체")
    routines.addRoutineExercise(legs, SQUAT, 3, Target(weight = Weight(100_000), repsMin = 5, repsMax = 8))
    routines.addRoutineExercise(legs, PLANK, 2, Target(durationSeconds = 60))
    val upper = routines.addRoutine("상체")
    routines.addRoutineExercise(upper, BENCH, 4)
    routines.addRoutineExercise(upper, custom, 3, Target(weight = Weight(20_000), repsMin = 10, repsMax = 12))

    finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10), wr(62_500, 8)), custom to listOf(wr(20_000, 12))))
    exercises.updateCustomExercise(custom, draft.copy(recordType = RecordType.REPS))
    val second = finishedWorkout(
        at(60 * 24),
        mapOf(custom to listOf(reps(15)), PULL_UP to listOf(reps(8)), PLANK to listOf(seconds(45))),
        incomplete = setOf(PULL_UP),
    )
    workouts.updateWorkoutNote(second, "지어낸 메모")
    workouts.startFromRoutine(legs, at(60 * 48))
    check(exercises.deleteCustomExercise(custom) == ExerciseDeleteResult.ARCHIVED)

    settings.setPinnedRoutine(upper)
    settings.setThemeMode(ThemeMode.DARK)
    settings.setDefaultRestSeconds(150)
    return custom
}

/** 루틴 번호는 데이터베이스가 새로 매기므로 순서대로 다시 매겨 견준다. */
private fun normalized(text: String): BackupFile {
    val file = BackupCodec.decode(text)
    val ids = file.routines.mapIndexed { index, routine -> routine.id to index + 1L }.toMap()
    return file.copy(
        settings = file.settings.copy(pinnedRoutineId = file.settings.pinnedRoutineId?.let(ids::getValue)),
        routines = file.routines.map { it.copy(id = ids.getValue(it.id)) },
        workouts = file.workouts.map { it.copy(routineId = it.routineId?.let(ids::getValue)) },
    )
}

/** 설정 저장소의 쓰기를 기록하고 그대로 넘긴다. */
private class RecordingSettings(private val delegate: SettingsRepository) : SettingsRepository by delegate {
    val themeModes = mutableListOf<ThemeMode>()

    override fun observeSettings(): Flow<Settings> = delegate.observeSettings()

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeModes += mode
        delegate.setThemeMode(mode)
    }
}
