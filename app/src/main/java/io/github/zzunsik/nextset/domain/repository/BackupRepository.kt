package io.github.zzunsik.nextset.domain.repository

import io.github.zzunsik.nextset.domain.model.BackupSummary
import io.github.zzunsik.nextset.domain.model.CheckedBackup
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * 백업 파일 내보내기(DATA-03)와 가져오기(DATA-04). 형식은 docs/architecture.md "백업 파일 형식"을 따른다.
 * 파일을 어디에 쓰고 어디서 읽는지는 화면이 시스템 파일 선택기로 정한다. 이 저장소는 파일의 글만 다룬다.
 */
interface BackupRepository {
    /** 마지막으로 내보낸 시각. 내보낸 적이 없으면 null이다. 이 기기의 상태라 가져오기로 바뀌지 않는다. */
    fun observeLastExportedAt(): Flow<Instant?>

    /** 기기의 기록 전체를 백업 파일의 글로 만든다. 진행 중인 운동도 넣는다. */
    suspend fun exportText(now: Instant): String

    /** 파일을 다 쓴 뒤 부른다. */
    suspend fun markExported(at: Instant)

    /**
     * 파일의 글을 검사한다. 데이터베이스를 건드리지 않는다.
     * 받지 않는 파일이면 [io.github.zzunsik.nextset.domain.model.InvalidBackupException].
     */
    suspend fun check(text: String): CheckedBackup

    /** 지금 기기에 든 기록의 양. */
    suspend fun currentSummary(): BackupSummary

    /**
     * 기기의 기록 전체를 [backup]의 내용으로 바꾼다. 합치지 않는다.
     * 한 트랜잭션이라 도중에 실패하면 기존 기록이 그대로 남고 예외가 나간다.
     * 알림 켜기 안내를 거절했는지와 마지막으로 내보낸 시각은 이 기기의 값을 그대로 둔다.
     */
    suspend fun replaceAll(backup: CheckedBackup)
}
