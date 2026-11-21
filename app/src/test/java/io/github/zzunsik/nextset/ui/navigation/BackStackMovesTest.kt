package io.github.zzunsik.nextset.ui.navigation

import androidx.navigation3.runtime.NavKey
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 화면 이동 함수([navigate], [close], [replace])와, 화면들이 뒤로 가기 스택을 직접 바꾸지 않는지(#103).
 * 이동을 부른 화면이 맨 위일 때만 움직여야 버튼을 두 번 눌러도 화면이 두 번 열리거나 두 화면이 닫히지 않는다.
 */
class BackStackMovesTest {
    private data class Key(val name: String) : NavKey

    private val home = Key("home")
    private val edit = Key("edit")
    private val picker = Key("picker")

    /** 스택을 직접 바꾸는 호출. */
    private val direct = Regex("""backStack\.(add|addAll|remove\w*|clear|set)\(""")

    @Test
    fun navigateOnlyFromTheTopScreen() {
        val stack = mutableListOf<NavKey>(home)
        assertTrue(stack.navigate(home, edit))
        assertFalse("두 번째 누르기는 홈이 이미 맨 위가 아니라 무시한다", stack.navigate(home, edit))
        assertFalse("빠르게 다른 버튼을 눌러도 같다", stack.navigate(home, picker))
        assertFalse("맨 위 화면을 그 위에 다시 열지 않는다", stack.navigate(edit, edit))
        assertEquals(listOf(home, edit), stack)
    }

    @Test
    fun closeOnlyTheTopScreen() {
        val stack = mutableListOf<NavKey>(home, edit, picker)
        assertTrue(stack.close(picker))
        assertFalse("두 번 불려도 아래 화면은 닫지 않는다", stack.close(picker))
        assertEquals(listOf(home, edit), stack)
    }

    @Test
    fun replaceOnlyTheTopScreen() {
        val stack = mutableListOf<NavKey>(home, edit)
        assertTrue(stack.replace(edit, picker))
        assertFalse(stack.replace(edit, picker))
        assertEquals(listOf(home, picker), stack)
    }

    /** 화면 코드는 스택을 직접 add·remove하지 않고 위 함수만 쓴다. 이동 함수와 탭, 시스템 뒤로 가기를 다루는 곳만 예외다. */
    @Test
    fun screensDoNotChangeTheBackStackDirectly() {
        val root = File("src/main/java/io/github/zzunsik/nextset/ui")
        assertTrue("소스 폴더를 찾지 못했다: ${root.absolutePath}", root.isDirectory)
        val allowed = setOf("navigation/BackStackMoves.kt", "navigation/TopLevelNavigation.kt", "navigation/NextSetNavDisplay.kt")
        val violations = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.relativeTo(root).invariantSeparatorsPath in allowed }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (direct.containsMatchIn(line)) "${file.relativeTo(root).invariantSeparatorsPath}:${index + 1}: ${line.trim()}" else null
                }
            }
            .toList()
        assertEquals("뒤로 가기 스택을 직접 바꾸는 곳", emptyList<String>(), violations)
    }

    /** 위 검사가 실제로 잡는지. */
    @Test
    fun directChangesAreDetected() {
        assertTrue(direct.containsMatchIn("onClick = { backStack.add(LicensesKey) },"))
        assertTrue(direct.containsMatchIn("onBack = { backStack.removeLastOrNull() },"))
        assertFalse(direct.containsMatchIn("onBack = { backStack.close(key) },"))
    }
}
