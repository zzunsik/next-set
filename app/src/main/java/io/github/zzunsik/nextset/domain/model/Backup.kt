package io.github.zzunsik.nextset.domain.model

/**
 * 백업 파일이나 기기에 든 기록의 양. 가져오기 확인 화면에 보인다(docs/architecture.md "가져오기" 3번).
 * [workoutCount]는 끝난 운동 기록의 수다. 진행 중인 운동은 [hasWorkoutInProgress]로 따로 센다.
 */
data class BackupSummary(
    val workoutCount: Int,
    val routineCount: Int,
    val hasWorkoutInProgress: Boolean,
)

/** 가져오지 않는 백업 파일의 종류. 사용자에게 무엇이 문제인지 알린다. */
enum class BackupProblem {
    /** JSON이 아니거나 이 앱의 백업 형식(`format`)이 아니다. */
    NOT_BACKUP,

    /** 이 앱이 아는 것보다 높은 형식 버전이다. 앱을 업데이트해야 한다. */
    NEWER_VERSION,

    /** 이 앱의 백업 형식이지만 내용이 깨졌거나 규칙에 맞지 않는다. */
    BROKEN,
}

/** 백업 파일을 검사하다 거부했다. 기기의 기록은 건드리지 않았다. */
class InvalidBackupException(
    val problem: BackupProblem,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** 검사를 마친 백업 파일. 내용은 저장소 구현만 읽는다. */
interface CheckedBackup {
    val summary: BackupSummary
}
