package io.github.zzunsik.nextset.ui.theme

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * docs/DESIGN.md "모션"의 스프링 표와 [NextSetMotion]의 값이 같은지 본다.
 * 같은 값이 문서와 코드 두 곳에 있으므로 한쪽만 바뀌면 실패한다(AGENTS.md "작업 범위").
 */
class MotionTokenSyncTest {
    @Test
    fun springsMatchDesignTable() {
        val file = File("../docs/DESIGN.md")
        assertTrue("디자인 문서를 찾지 못했다: ${file.absolutePath}", file.isFile)
        val spring = """감쇠 ([\d.]+), 강성 (\d+)"""
        val row = Regex("""^\| (standard|expressive|둘 다) \| (spatial|effects) \| $spring \| $spring \| $spring \|""")
        val documented = file.readLines().mapNotNull { row.find(it)?.groupValues }.flatMap { r ->
            val name = if (r[2] == "effects") "effects" else "${r[1]}Spatial"
            listOf("Fast" to 3, "Default" to 5, "Slow" to 7).map { (speed, i) ->
                "$name$speed" to NextSetMotion.Spring(r[i].toFloat(), r[i + 1].toFloat())
            }
        }.toMap()

        val code = with(NextSetMotion) {
            mapOf(
                "standardSpatialFast" to standardSpatialFast,
                "standardSpatialDefault" to standardSpatialDefault,
                "standardSpatialSlow" to standardSpatialSlow,
                "expressiveSpatialFast" to expressiveSpatialFast,
                "expressiveSpatialDefault" to expressiveSpatialDefault,
                "expressiveSpatialSlow" to expressiveSpatialSlow,
                "effectsFast" to effectsFast,
                "effectsDefault" to effectsDefault,
                "effectsSlow" to effectsSlow,
            )
        }
        assertEquals(code, documented)
    }
}
