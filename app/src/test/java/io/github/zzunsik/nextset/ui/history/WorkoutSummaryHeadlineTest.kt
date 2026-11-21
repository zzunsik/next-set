package io.github.zzunsik.nextset.ui.history

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 최고 기록이 없는 운동의 요약 결론 문장(#93). 루틴 이름 끝소리에 맞는 조사를 붙이고,
 * 끝소리를 정할 수 없는 이름은 "루틴을"로 받는다. 값은 지어낸 것이다.
 */
@RunWith(RobolectricTestRunner::class)
class WorkoutSummaryHeadlineTest {
    @get:Rule
    val compose = createComposeRule()

    private fun showSummary(routineName: String) {
        val state = HistorySamples.report()
        val report = requireNotNull(state.report).copy(routineName = routineName, records = emptyList())
        compose.setContent {
            NextSetTheme {
                WorkoutSummaryScreen(state = state.copy(report = report), noteText = "", onNoteChange = {}, onConfirm = {})
            }
        }
    }

    @Test
    fun hangulNameWithFinalConsonantTakesEul() {
        showSummary("가슴 루틴")
        compose.onNodeWithText("가슴 루틴을 끝냈어요.").assertExists()
    }

    @Test
    fun hangulNameWithoutFinalConsonantTakesReul() {
        showSummary("하체 데이")
        compose.onNodeWithText("하체 데이를 끝냈어요.").assertExists()
    }

    @Test
    fun nameEndingInLatinLetterReadsTheLetterName() {
        showSummary("Push Day A")
        compose.onNodeWithText("Push Day A를 끝냈어요.").assertExists()
    }

    @Test
    fun nameEndingInDigitReadsTheNumber() {
        showSummary("Upper 3")
        compose.onNodeWithText("Upper 3을 끝냈어요.").assertExists()
    }

    @Test
    fun nameEndingInLatinWordAvoidsGuessingTheParticle() {
        showSummary("Push Day")
        compose.onNodeWithText("Push Day 루틴을 끝냈어요.").assertExists()
    }

    @Test
    fun nameEndingInSymbolAvoidsGuessingTheParticle() {
        showSummary("등 운동 ★")
        compose.onNodeWithText("등 운동 ★ 루틴을 끝냈어요.").assertExists()
    }
}
