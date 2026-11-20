package io.github.zzunsik.nextset.data.backup

import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SubPart
import io.github.zzunsik.nextset.domain.model.ThemeMode
import kotlinx.serialization.Serializable

// 백업 파일의 지금 형식(formatVersion = BackupCodec.CURRENT_VERSION). docs/architecture.md "백업 파일 형식"의 예시와 같다.
// 데이터베이스 엔티티가 아니라 domain 값을 적는다. 스키마가 바뀌어도 이 형식은 그대로 둔다.
// 필드의 뜻을 바꾸거나 필수 필드나 열거 값을 더하면 형식 버전을 올리고 BackupCodec.upgrades에 변환 함수를 더한다.

@Serializable
internal data class BackupFile(
    val format: String,
    val formatVersion: Int,
    /** UTC 밀리초. */
    val exportedAt: Long,
    val app: BackupApp,
    val settings: BackupSettings,
    val exercises: List<BackupExercise>,
    val routines: List<BackupRoutine>,
    val workouts: List<BackupWorkout>,
)

/** 파일을 만든 앱. 읽을 때는 쓰지 않고 문제를 살필 때 본다. */
@Serializable
internal data class BackupApp(
    val versionName: String,
    val versionCode: Long,
)

@Serializable
internal data class BackupSettings(
    val defaultRestSeconds: Int,
    val themeMode: ThemeMode,
    /** 파일 안의 [BackupRoutine.id]. */
    val pinnedRoutineId: Long?,
)

@Serializable
internal data class BackupExercise(
    val id: String,
    val builtin: Boolean,
    val nameKo: String,
    val nameEn: String?,
    val aliases: List<String>,
    val bodyPart: BodyPart,
    val subPart: SubPart?,
    val equipment: Equipment,
    /** 종목의 지금 기록 방식. */
    val recordType: RecordType,
    val archived: Boolean,
    val restSeconds: Int?,
)

@Serializable
internal data class BackupRoutine(
    /** 파일 안에서 [BackupWorkout.routineId]와 [BackupSettings.pinnedRoutineId]가 가리키는 번호로만 쓴다. */
    val id: Long,
    val name: String,
    val position: Int,
    /** 순서는 배열의 순서다. */
    val exercises: List<BackupRoutineExercise>,
)

@Serializable
internal data class BackupRoutineExercise(
    val exerciseId: String,
    val setCount: Int,
    val targetWeightG: Int?,
    val targetRepsMin: Int?,
    val targetRepsMax: Int?,
    val targetDurationS: Int?,
)

@Serializable
internal data class BackupWorkout(
    val routineId: Long?,
    val routineName: String?,
    val startedAt: Long,
    /** 비어 있으면 진행 중인 운동이다. */
    val finishedAt: Long?,
    val note: String?,
    val exercises: List<BackupWorkoutExercise>,
)

@Serializable
internal data class BackupWorkoutExercise(
    val exerciseId: String,
    /** 적을 당시의 기록 방식. 종목의 지금 방식과 달라도 된다. */
    val recordType: RecordType,
    val note: String?,
    val targetWeightG: Int?,
    val targetRepsMin: Int?,
    val targetRepsMax: Int?,
    val targetDurationS: Int?,
    val sets: List<BackupSet>,
)

@Serializable
internal data class BackupSet(
    val weightG: Int?,
    val reps: Int?,
    val durationS: Int?,
    /** 비어 있으면 완료하지 않은 세트다. */
    val completedAt: Long?,
)
