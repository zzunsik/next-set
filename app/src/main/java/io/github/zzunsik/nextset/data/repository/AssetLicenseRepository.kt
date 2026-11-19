package io.github.zzunsik.nextset.data.repository

import io.github.zzunsik.nextset.domain.model.OpenSourceNotice
import io.github.zzunsik.nextset.domain.repository.LicenseRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * `app/src/main/assets/licenses/`의 목록(`index.json`)과 라이선스 전문을 읽는다
 * (docs/architecture.md "글꼴과 라이선스 고지"). [readAsset]은 `licenses/` 아래 파일 이름을 받아 내용을 돌려준다.
 */
internal class AssetLicenseRepository(
    private val readAsset: suspend (fileName: String) -> String,
) : LicenseRepository {
    private val mutex = Mutex()
    private var index: LicenseIndex? = null

    override suspend fun notices(): List<OpenSourceNotice> = loadIndex().notices.map { it.toModel() }

    override suspend fun licenseText(id: String): String {
        val entry = loadIndex().notices.firstOrNull { it.id == id }
            ?: throw NoSuchElementException("라이선스 항목 $id 가 없다")
        return readAsset(entry.file)
    }

    private suspend fun loadIndex(): LicenseIndex = mutex.withLock {
        index ?: LicenseIndex.parse(readAsset(LicenseIndex.FILE_NAME)).also { index = it }
    }
}

/** `assets/licenses/index.json`의 형식. */
@Serializable
internal data class LicenseIndex(val notices: List<Entry>) {
    @Serializable
    data class Entry(
        val id: String,
        val name: String,
        val author: String,
        val license: String,
        /** 같은 폴더의 라이선스 전문 파일. */
        val file: String,
        /**
         * 이 항목이 고지하는 Maven 모듈(`그룹:아티팩트`). 글꼴과 아이콘은 비어 있다.
         * `OpenSourceNoticeFileTest`가 앱이 쓰는 라이브러리가 모두 여기에 있는지 본다.
         */
        val modules: List<String> = emptyList(),
    ) {
        fun toModel() = OpenSourceNotice(id = id, name = name, author = author, licenseName = license)
    }

    companion object {
        const val FILE_NAME = "index.json"
        private val json = Json { ignoreUnknownKeys = false }

        fun parse(text: String): LicenseIndex = json.decodeFromString(serializer(), text).also {
            val duplicated = it.notices.groupBy(Entry::id).filterValues { list -> list.size > 1 }.keys
            require(duplicated.isEmpty()) { "라이선스 항목의 id가 겹친다: $duplicated" }
        }
    }
}
