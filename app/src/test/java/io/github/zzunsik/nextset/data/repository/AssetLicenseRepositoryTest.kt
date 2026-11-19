package io.github.zzunsik.nextset.data.repository

import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 오픈소스 라이선스(SET-03). 앱에 넣은 `assets/licenses/`를 그대로 읽어 본다.
 * 앱이 쓰는 라이브러리가 목록에서 빠지면 실패한다(목록은 손으로 적는다. docs/architecture.md "글꼴과 라이선스 고지").
 */
class AssetLicenseRepositoryTest {
    private val folder = File("src/main/assets/licenses")
    private val repository = AssetLicenseRepository { name -> File(folder, name).readText() }

    @Test
    fun listsFontIconsAndLibrariesWithTheirFullText() = runTest {
        val notices = repository.notices()
        assertEquals(listOf("pretendard", "material-symbols"), notices.take(2).map { it.id })

        val pretendard = repository.licenseText("pretendard")
        assertTrue(pretendard.startsWith("Copyright (c) 2021, Kil Hyung-jin"))
        assertTrue("SIL OPEN FONT LICENSE Version 1.1" in pretendard)

        notices.filter { it.licenseName == "Apache License 2.0" }.forEach { notice ->
            val text = repository.licenseText(notice.id)
            assertTrue(notice.id, text.contains("Apache License") && text.contains("Version 2.0, January 2004"))
            assertTrue(notice.id, "END OF TERMS AND CONDITIONS" in text)
        }
    }

    @Test
    fun unknownIdIsRejected() = runTest {
        assertThrows(NoSuchElementException::class.java) { kotlinx.coroutines.runBlocking { repository.licenseText("none") } }
    }

    @Test
    fun indexIsValid() {
        val index = LicenseIndex.parse(File(folder, LicenseIndex.FILE_NAME).readText())
        index.notices.forEach { entry ->
            assertTrue("${entry.id}: 전문 파일이 없다: ${entry.file}", File(folder, entry.file).isFile)
            assertTrue(entry.id, entry.name.isNotBlank() && entry.author.isNotBlank() && entry.license.isNotBlank())
        }
        val duplicate = """{"notices": [
            {"id":"a","name":"A","author":"x","license":"l","file":"f"},
            {"id":"a","name":"B","author":"x","license":"l","file":"f"}]}"""
        assertThrows(IllegalArgumentException::class.java) { LicenseIndex.parse(duplicate) }
    }

    /** `app/build.gradle.kts`에서 앱에 들어가는(`implementation`) 라이브러리가 모두 목록에 있는지. */
    @Test
    fun everyAppLibraryHasANotice() {
        val catalog = parseCatalogModules(File("../gradle/libs.versions.toml").readText())
        val appModules = Regex("""^\s*implementation\(libs\.([\w.]+)\)""", RegexOption.MULTILINE)
            .findAll(File("build.gradle.kts").readText())
            .map { it.groupValues[1].replace('.', '-') }
            .map { alias -> catalog[alias] ?: error("버전 목록에 없는 별칭: $alias") }
            .toSet()
        assertTrue("build.gradle.kts에서 라이브러리를 찾지 못했다", appModules.size > 5)

        val noticed = LicenseIndex.parse(File(folder, LicenseIndex.FILE_NAME).readText())
            .notices.flatMap { it.modules }.toSet()
        assertEquals("라이선스 목록(assets/licenses/index.json)에 없는 라이브러리", emptySet<String>(), appModules - noticed)
        assertEquals("앱이 쓰지 않는 라이브러리", emptySet<String>(), noticed - appModules - IMPLICIT_MODULES)
    }

    /** Material Symbols 아이콘을 넣었으면 그 고지가 있어야 한다. */
    @Test
    fun iconsHaveANotice() = runTest {
        val icons = File("src/main/res/drawable").listFiles().orEmpty()
            .filter { it.name.startsWith("ic_") && it.name != "ic_launcher_foreground.xml" }
        assertTrue(icons.isNotEmpty())
        assertTrue(repository.notices().any { it.id == "material-symbols" })
    }

    private fun parseCatalogModules(toml: String): Map<String, String> =
        Regex("""^([\w-]+)\s*=\s*\{\s*module\s*=\s*"([^"]+)"""", RegexOption.MULTILINE)
            .findAll(toml)
            .associate { it.groupValues[1] to it.groupValues[2] }

    private companion object {
        /** `implementation`으로 적지 않았지만 앱에 늘 들어가는 것. Kotlin 플러그인이 더한다. */
        val IMPLICIT_MODULES = setOf("org.jetbrains.kotlin:kotlin-stdlib")
    }
}
