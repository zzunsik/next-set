package io.github.zzunsik.nextset.ui.settings

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 시스템 파일 선택기가 돌려준 문서를 읽고 쓴다. 파일 선택기를 거친 문서는 저장소 권한 없이 읽고 쓴다
 * (docs/architecture.md "백업 파일 형식").
 */
interface BackupDocuments {
    /** [uri]에 [text]를 UTF-8로 쓴다. 실패하면 예외. */
    suspend fun write(uri: Uri, text: String)

    /** [uri]의 글을 UTF-8로 읽는다. [MAX_BYTES]보다 크면 [TooLargeException]. 그 밖의 실패도 예외. */
    suspend fun read(uri: Uri): String

    /** 백업 파일로 보기에 너무 큰 파일. 잘못 고른 동영상 같은 파일을 메모리에 다 올리지 않는다. */
    class TooLargeException : IOException("파일이 너무 크다")

    companion object {
        /**
         * 읽을 수 있는 가장 큰 파일. 세트 2만 개가 1.5MB 안팎이라는 계산(docs/architecture.md)에
         * 줄바꿈과 들여쓰기를 더해도 넉넉하다.
         */
        const val MAX_BYTES = 64L * 1024 * 1024
    }
}

/** [ContentResolver]로 문서를 읽고 쓴다. */
class ContentResolverBackupDocuments(private val resolver: ContentResolver) : BackupDocuments {
    override suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        // 파일 저장 화면이 새로 만든 빈 문서다. 덮어쓰는 경우에도 남는 꼬리가 없게 자르기(t)를 붙인다.
        // 자르기를 받지 않는 문서 제공자도 있어 그때는 쓰기(w)로 연다.
        val stream = try {
            resolver.openOutputStream(uri, "wt")
        } catch (_: IllegalArgumentException) {
            resolver.openOutputStream(uri, "w")
        } catch (_: FileNotFoundException) {
            resolver.openOutputStream(uri, "w")
        } ?: throw IOException("문서를 열지 못했다")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    override suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = resolver.openInputStream(uri) ?: throw IOException("문서를 열지 못했다")
        stream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                if (output.size() > BackupDocuments.MAX_BYTES) throw BackupDocuments.TooLargeException()
            }
            output.toString(Charsets.UTF_8.name())
        }
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}
