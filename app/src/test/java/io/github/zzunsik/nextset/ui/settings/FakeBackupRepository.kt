package io.github.zzunsik.nextset.ui.settings

import android.net.Uri
import io.github.zzunsik.nextset.domain.model.BackupProblem
import io.github.zzunsik.nextset.domain.model.BackupSummary
import io.github.zzunsik.nextset.domain.model.CheckedBackup
import io.github.zzunsik.nextset.domain.model.InvalidBackupException
import io.github.zzunsik.nextset.domain.repository.BackupRepository
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 화면 상태 테스트용 백업 저장소. 파일의 글이 [VALID]면 받고, [problems]에 든 글이면 그 문제로 거부한다.
 * 가져오기는 [replaced]에 남긴다.
 */
class FakeBackupRepository(
    var device: BackupSummary = BackupSummary(workoutCount = 3, routineCount = 1, hasWorkoutInProgress = false),
) : BackupRepository {
    val lastExportedAt = MutableStateFlow<Instant?>(null)
    val exported = mutableListOf<Instant>()
    val replaced = mutableListOf<CheckedBackup>()
    var failReplace = false

    val problems = mapOf(
        "not-backup" to BackupProblem.NOT_BACKUP,
        "newer" to BackupProblem.NEWER_VERSION,
        "broken" to BackupProblem.BROKEN,
    )

    override fun observeLastExportedAt(): Flow<Instant?> = lastExportedAt

    override suspend fun exportText(now: Instant): String = "backup at ${now.toEpochMilli()}"

    override suspend fun markExported(at: Instant) {
        exported += at
        lastExportedAt.value = at
    }

    override suspend fun check(text: String): CheckedBackup {
        problems[text]?.let { throw InvalidBackupException(it, text) }
        require(text == VALID) { "알 수 없는 글: $text" }
        return object : CheckedBackup {
            override val summary = FILE_SUMMARY
        }
    }

    override suspend fun currentSummary(): BackupSummary = device

    override suspend fun replaceAll(backup: CheckedBackup) {
        if (failReplace) throw IllegalStateException("지어낸 실패")
        replaced += backup
    }

    companion object {
        const val VALID = "valid"
        val FILE_SUMMARY = BackupSummary(workoutCount = 42, routineCount = 4, hasWorkoutInProgress = false)
    }
}

/** 화면 상태 테스트용 문서. 글은 [files]에 둔다. [failing]에 든 주소는 읽고 쓸 때 실패한다. */
class FakeBackupDocuments : BackupDocuments {
    val files = mutableMapOf<Uri, String>()
    val failing = mutableSetOf<Uri>()
    val tooLarge = mutableSetOf<Uri>()

    override suspend fun write(uri: Uri, text: String) {
        if (uri in failing) throw IOException("지어낸 쓰기 실패")
        files[uri] = text
    }

    override suspend fun read(uri: Uri): String {
        if (uri in failing) throw IOException("지어낸 읽기 실패")
        if (uri in tooLarge) throw BackupDocuments.TooLargeException()
        return files.getValue(uri)
    }
}
