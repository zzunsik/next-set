package io.github.zzunsik.nextset.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SubPart
import io.github.zzunsik.nextset.domain.model.ThemeMode

// 테이블과 열은 docs/architecture.md "저장 스키마"를 그대로 옮긴다.
// 열거 값은 상수 이름을 TEXT로, 참과 거짓은 INTEGER 0과 1로, 시각은 UTC 밀리초로 저장한다.

@Entity(tableName = "exercise")
internal data class ExerciseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name_ko") val nameKo: String,
    @ColumnInfo(name = "name_en") val nameEn: String?,
    /** 줄바꿈으로 구분한다. 없으면 빈 문자열. */
    val aliases: String,
    @ColumnInfo(name = "body_part") val bodyPart: BodyPart,
    @ColumnInfo(name = "sub_part") val subPart: SubPart?,
    val equipment: Equipment,
    @ColumnInfo(name = "record_type") val recordType: RecordType,
    @ColumnInfo(name = "is_builtin") val isBuiltin: Boolean,
    @ColumnInfo(name = "is_archived", defaultValue = "0") val isArchived: Boolean = false,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int? = null,
)

@Entity(tableName = "routine")
internal data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: Int,
)

@Entity(
    tableName = "routine_exercise",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routine_id", "position"), Index("exercise_id")],
)
internal data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "routine_id") val routineId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    val position: Int,
    @ColumnInfo(name = "set_count") val setCount: Int,
    @ColumnInfo(name = "target_weight_g") val targetWeightG: Int?,
    @ColumnInfo(name = "target_reps_min") val targetRepsMin: Int?,
    @ColumnInfo(name = "target_reps_max") val targetRepsMax: Int?,
    @ColumnInfo(name = "target_duration_s") val targetDurationS: Int?,
)

@Entity(
    tableName = "workout",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("started_at"), Index("routine_id")],
)
internal data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "routine_id") val routineId: Long?,
    @ColumnInfo(name = "routine_name") val routineName: String?,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    /** 비어 있으면 진행 중이다. */
    @ColumnInfo(name = "finished_at") val finishedAt: Long?,
    val note: String? = null,
)

@Entity(
    tableName = "workout_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        // 기록에 쓰인 종목 행은 지우지 못한다. 숨긴다.
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("workout_id", "position"), Index("exercise_id")],
)
internal data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "workout_id") val workoutId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    val position: Int,
    /** 이 행을 만들 때의 종목 기록 방식 사본. */
    @ColumnInfo(name = "record_type") val recordType: RecordType,
    val note: String? = null,
    @ColumnInfo(name = "target_weight_g") val targetWeightG: Int?,
    @ColumnInfo(name = "target_reps_min") val targetRepsMin: Int?,
    @ColumnInfo(name = "target_reps_max") val targetRepsMax: Int?,
    @ColumnInfo(name = "target_duration_s") val targetDurationS: Int?,
)

@Entity(
    tableName = "workout_set",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workout_exercise_id", "position")],
)
internal data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "workout_exercise_id") val workoutExerciseId: Long,
    val position: Int,
    @ColumnInfo(name = "weight_g") val weightG: Int?,
    val reps: Int?,
    @ColumnInfo(name = "duration_s") val durationS: Int?,
    /** 비어 있으면 완료하지 않은 세트다. */
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
)

@Entity(
    tableName = "app_settings",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["pinned_routine_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("pinned_routine_id")],
)
internal data class AppSettingsEntity(
    /** 항상 [SINGLE_ROW_ID]. */
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    @ColumnInfo(name = "default_rest_seconds") val defaultRestSeconds: Int,
    @ColumnInfo(name = "theme_mode") val themeMode: ThemeMode,
    @ColumnInfo(name = "pinned_routine_id") val pinnedRoutineId: Long?,
    @ColumnInfo(name = "builtin_seed_version", defaultValue = "0") val builtinSeedVersion: Int = 0,
    @ColumnInfo(name = "notification_prompt_declined", defaultValue = "0")
    val notificationPromptDeclined: Boolean = false,
) {
    companion object {
        const val SINGLE_ROW_ID = 1
    }
}
