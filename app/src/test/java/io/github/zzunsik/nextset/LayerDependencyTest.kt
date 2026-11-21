package io.github.zzunsik.nextset

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 모듈이 하나라 컴파일러가 계층 사이 의존 방향을 지켜 주지 않는다.
 * 소스 파일의 import를 읽어 docs/architecture.md "계층과 의존 방향"의 규칙을 검사한다.
 */
class LayerDependencyTest {
    @Test
    fun sourcesFollowLayerRules() {
        val root = File("src/main/java/io/github/zzunsik/nextset")
        assertTrue("소스 폴더를 찾지 못했다: ${root.absolutePath}", root.isDirectory)

        val violations = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val path = file.relativeTo(root).invariantSeparatorsPath
                violationsOf(path, file.readLines())
            }
            .toList()

        assertEquals("계층 규칙을 어긴 import", emptyList<String>(), violations)
    }

    @Test
    fun detectsForbiddenImports() {
        val domain = listOf(
            "import android.content.Context",
            "import androidx.room3.Entity",
            "import io.github.zzunsik.nextset.ui.home.HomeScreen",
            "import io.github.zzunsik.nextset.data.db.NextSetDatabase",
            "import java.time.Instant",
            "import kotlinx.coroutines.flow.Flow",
        )
        assertEquals(4, violationsOf("domain/model/Weight.kt", domain).size)

        val ui = listOf(
            "import io.github.zzunsik.nextset.data.db.NextSetDatabase",
            "import io.github.zzunsik.nextset.domain.model.Weight",
            "import androidx.compose.runtime.Composable",
        )
        assertEquals(1, violationsOf("ui/home/HomeScreen.kt", ui).size)
    }

    private fun violationsOf(path: String, lines: List<String>): List<String> {
        val forbidden = when {
            path.startsWith("domain/") -> listOf("android.", "androidx.", "$PACKAGE.ui.", "$PACKAGE.data.")
            path.startsWith("ui/") -> listOf("$PACKAGE.data.")
            else -> return emptyList()
        }
        return lines
            .map(String::trim)
            .filter { it.startsWith("import ") }
            .map { it.removePrefix("import ").trim() }
            .filter { imported -> forbidden.any(imported::startsWith) }
            .map { "$path: $it" }
    }

    private companion object {
        const val PACKAGE = "io.github.zzunsik.nextset"
    }
}
