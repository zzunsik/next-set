package io.github.zzunsik.nextset.data.backup

import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import io.github.zzunsik.nextset.data.db.ExerciseEntity
import io.github.zzunsik.nextset.data.db.NextSetDatabase
import io.github.zzunsik.nextset.data.db.RoutineEntity
import io.github.zzunsik.nextset.data.db.RoutineExerciseEntity
import io.github.zzunsik.nextset.data.db.Tables
import io.github.zzunsik.nextset.data.db.WorkoutEntity
import io.github.zzunsik.nextset.data.db.WorkoutExerciseEntity
import io.github.zzunsik.nextset.data.db.WorkoutSetEntity
import io.github.zzunsik.nextset.data.repository.getOrDefault
import io.github.zzunsik.nextset.data.repository.joinAliases
import io.github.zzunsik.nextset.data.repository.splitAliases
import io.github.zzunsik.nextset.data.seed.BuiltinExerciseSeeder
import io.github.zzunsik.nextset.domain.model.BackupSummary
import io.github.zzunsik.nextset.domain.model.CheckedBackup
import io.github.zzunsik.nextset.domain.repository.BackupRepository
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** 검사를 마친 백업 파일. [BackupCodec.decode]를 거친 것만 담는다. */
internal class DecodedBackup(val file: BackupFile) : CheckedBackup {
    override val summary: BackupSummary = BackupCodec.summarize(file)
}

/**
 * 백업 파일 내보내기와 가져오기(docs/architecture.md "백업 파일 형식").
 *
 * 가져오기는 한 트랜잭션에서 모든 테이블을 비우고 파일의 내용을 넣은 뒤 내장 종목을 다시 반영한다.
 * 도중에 실패하면 트랜잭션이 되돌아가 기존 기록이 그대로 남는다.
 *
 * 가져온 화면 모드는 `SystemNightModeSync`(#54)가 설정의 Flow로 받아 시스템 쪽 사본에 맞춘다.
 * @param appVersion 파일의 `app`에 적을 앱 버전.
 */
internal class RoomBackupRepository(
    private val database: NextSetDatabase,
    private val seeder: BuiltinExerciseSeeder,
    private val appVersion: BackupApp,
) : BackupRepository {
    private val backupDao = database.backupDao()
    private val settingsDao = database.settingsDao()
    private val routineDao = database.routineDao()
    private val workoutDao = database.workoutDao()

    override fun observeLastExportedAt(): Flow<Instant?> = database.observe(Tables.APP_SETTINGS) {
        settingsDao.getOrDefault().lastBackupExportedAt?.let(Instant::ofEpochMilli)
    }

    override suspend fun exportText(now: Instant): String {
        val file = database.withReadTransaction { snapshot(now) }
        return withContext(Dispatchers.Default) { BackupCodec.encode(file) }
    }

    override suspend fun markExported(at: Instant) {
        database.withWriteTransaction {
            settingsDao.upsert(settingsDao.getOrDefault().copy(lastBackupExportedAt = at.toEpochMilli()))
        }
    }

    override suspend fun check(text: String): CheckedBackup =
        withContext(Dispatchers.Default) { DecodedBackup(BackupCodec.decode(text)) }

    override suspend fun currentSummary(): BackupSummary = database.withReadTransaction {
        BackupSummary(
            workoutCount = backupDao.countFinishedWorkouts(),
            routineCount = backupDao.countRoutines(),
            hasWorkoutInProgress = backupDao.hasWorkoutInProgress(),
        )
    }

    override suspend fun replaceAll(backup: CheckedBackup) {
        val file = requireNotNull(backup as? DecodedBackup) { "check()로 검사한 백업만 가져온다" }.file
        // 파일을 읽는 일은 트랜잭션 밖에서 끝낸다.
        val builtins = seeder.readFile()
        database.withWriteTransaction {
            val kept = settingsDao.getOrDefault()

            backupDao.deleteWorkouts()
            backupDao.deleteRoutines()
            backupDao.deleteExercises()

            backupDao.insertExercises(file.exercises.map { it.toEntity() })
            val routineIds = HashMap<Long, Long>()
            file.routines.sortedBy { it.position }.forEachIndexed { index, routine ->
                val id = routineDao.insert(RoutineEntity(name = routine.name, position = index))
                routineIds[routine.id] = id
                routine.exercises.forEachIndexed { position, item ->
                    routineDao.insertItem(
                        RoutineExerciseEntity(
                            routineId = id,
                            exerciseId = item.exerciseId,
                            position = position,
                            setCount = item.setCount,
                            targetWeightG = item.targetWeightG,
                            targetRepsMin = item.targetRepsMin,
                            targetRepsMax = item.targetRepsMax,
                            targetDurationS = item.targetDurationS,
                        ),
                    )
                }
            }
            file.workouts.forEach { workout ->
                val workoutId = workoutDao.insertWorkout(
                    WorkoutEntity(
                        routineId = workout.routineId?.let(routineIds::getValue),
                        routineName = workout.routineName,
                        startedAt = workout.startedAt,
                        finishedAt = workout.finishedAt,
                        note = workout.note,
                        // 가져온 운동은 사용자의 기록이라 손댄 것으로 본다. 뒤로 가기만으로 버려지지 않는다(#73).
                        isEdited = true,
                    ),
                )
                workout.exercises.forEachIndexed { position, item ->
                    val workoutExerciseId = workoutDao.insertExercise(
                        WorkoutExerciseEntity(
                            workoutId = workoutId,
                            exerciseId = item.exerciseId,
                            position = position,
                            recordType = item.recordType,
                            note = item.note,
                            targetWeightG = item.targetWeightG,
                            targetRepsMin = item.targetRepsMin,
                            targetRepsMax = item.targetRepsMax,
                            targetDurationS = item.targetDurationS,
                        ),
                    )
                    item.sets.forEachIndexed { setPosition, set ->
                        workoutDao.insertSet(
                            WorkoutSetEntity(
                                workoutExerciseId = workoutExerciseId,
                                position = setPosition,
                                weightG = set.weightG,
                                reps = set.reps,
                                durationS = set.durationS,
                                completedAt = set.completedAt,
                            ),
                        )
                    }
                }
            }

            // 알림 켜기 안내를 거절했는지와 마지막으로 내보낸 시각은 이 기기의 상태라 그대로 둔다.
            // 내장 종목 판 번호를 0으로 돌려 아래에서 앱의 내장 종목 파일을 다시 반영하게 한다.
            settingsDao.upsert(
                kept.copy(
                    defaultRestSeconds = file.settings.defaultRestSeconds,
                    themeMode = file.settings.themeMode,
                    pinnedRoutineId = file.settings.pinnedRoutineId?.let(routineIds::getValue),
                    builtinSeedVersion = 0,
                ),
            )
            // 옛 백업의 내장 종목 이름 같은 값을 이 앱의 내장 종목 파일로 다시 맞추고, 파일에 없던 내장 종목을 더한다.
            seeder.seed(builtins)
        }
    }

    /** 데이터베이스 전체를 [BackupFile]로 옮긴다. 읽기 트랜잭션 안에서 부른다. */
    private suspend fun snapshot(now: Instant): BackupFile {
        val settings = settingsDao.getOrDefault()
        val itemsByRoutine = backupDao.getRoutineExercises().groupBy { it.routineId }
        val setsByExercise = backupDao.getSets().groupBy { it.workoutExerciseId }
        val exercisesByWorkout = backupDao.getWorkoutExercises().groupBy { it.workoutId }
        return BackupFile(
            format = BackupCodec.FORMAT,
            formatVersion = BackupCodec.CURRENT_VERSION,
            exportedAt = now.toEpochMilli(),
            app = appVersion,
            settings = BackupSettings(
                defaultRestSeconds = settings.defaultRestSeconds,
                themeMode = settings.themeMode,
                pinnedRoutineId = settings.pinnedRoutineId,
            ),
            exercises = backupDao.getExercises().map { it.toBackup() },
            routines = backupDao.getRoutines().map { routine ->
                BackupRoutine(
                    id = routine.id,
                    name = routine.name,
                    position = routine.position,
                    exercises = itemsByRoutine[routine.id].orEmpty().map { item ->
                        BackupRoutineExercise(
                            exerciseId = item.exerciseId,
                            setCount = item.setCount,
                            targetWeightG = item.targetWeightG,
                            targetRepsMin = item.targetRepsMin,
                            targetRepsMax = item.targetRepsMax,
                            targetDurationS = item.targetDurationS,
                        )
                    },
                )
            },
            workouts = backupDao.getWorkouts().map { workout ->
                BackupWorkout(
                    routineId = workout.routineId,
                    routineName = workout.routineName,
                    startedAt = workout.startedAt,
                    finishedAt = workout.finishedAt,
                    note = workout.note,
                    exercises = exercisesByWorkout[workout.id].orEmpty().map { item ->
                        BackupWorkoutExercise(
                            exerciseId = item.exerciseId,
                            recordType = item.recordType,
                            note = item.note,
                            targetWeightG = item.targetWeightG,
                            targetRepsMin = item.targetRepsMin,
                            targetRepsMax = item.targetRepsMax,
                            targetDurationS = item.targetDurationS,
                            sets = setsByExercise[item.id].orEmpty().map { set ->
                                BackupSet(
                                    weightG = set.weightG,
                                    reps = set.reps,
                                    durationS = set.durationS,
                                    completedAt = set.completedAt,
                                )
                            },
                        )
                    },
                )
            },
        )
    }
}

private fun ExerciseEntity.toBackup() = BackupExercise(
    id = id,
    builtin = isBuiltin,
    nameKo = nameKo,
    nameEn = nameEn,
    aliases = aliases.splitAliases(),
    bodyPart = bodyPart,
    subPart = subPart,
    equipment = equipment,
    recordType = recordType,
    archived = isArchived,
    restSeconds = restSeconds,
)

private fun BackupExercise.toEntity() = ExerciseEntity(
    id = id,
    nameKo = nameKo,
    nameEn = nameEn,
    aliases = aliases.joinAliases(),
    bodyPart = bodyPart,
    subPart = subPart,
    equipment = equipment,
    recordType = recordType,
    isBuiltin = builtin,
    isArchived = archived,
    restSeconds = restSeconds,
)
