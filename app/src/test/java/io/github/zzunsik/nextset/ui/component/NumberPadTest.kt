package io.github.zzunsik.nextset.ui.component

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.ui.a11y.assertTouchTargets
import io.github.zzunsik.nextset.ui.screenshot.SCREENSHOT_DEVICE
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** 숫자판 화면 동작. 키 이름, 칸 종류별 키 구성, 칸을 가리지 않는 배치(LOG-07, LOG-08, NF-10). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = SCREENSHOT_DEVICE)
class NumberPadTest {
    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()

    private fun string(id: Int, vararg args: Any) = app.getString(id, *args)

    private fun setPad(kind: NumberFieldKind, isLastField: Boolean = false, onKey: (NumberPadKey) -> Unit = {}) {
        compose.setContent { NextSetTheme { NumberPad(kind = kind, isLastField = isLastField, onKey = onKey) } }
    }

    private fun exists(description: String) =
        compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(description)))
            .fetchSemanticsNodes().isNotEmpty()

    @Test
    fun weightPadHasDecimalPointAndKilogramSteps() {
        val keys = mutableListOf<NumberPadKey>()
        setPad(NumberFieldKind.WEIGHT) { keys += it }

        compose.onNodeWithContentDescription("2.5 빼기").performClick()
        compose.onNodeWithContentDescription("1 빼기").performClick()
        compose.onNodeWithContentDescription("1 더하기").performClick()
        compose.onNodeWithContentDescription("2.5 더하기").performClick()
        compose.onNodeWithContentDescription(string(R.string.number_pad_decimal_point_description)).performClick()
        assertEquals(
            listOf(NumberPadKey.Step(-2500), NumberPadKey.Step(-1000), NumberPadKey.Step(1000), NumberPadKey.Step(2500), NumberPadKey.DecimalPoint),
            keys,
        )
        compose.assertTouchTargets()
    }

    @Test
    fun repsPadHasOnlyOneStepAndNoDecimalPoint() {
        setPad(NumberFieldKind.REPS)

        assertTrue(exists("1 더하기"))
        assertTrue(exists("1 빼기"))
        assertTrue(!exists("2.5 더하기"))
        assertTrue(!exists(string(R.string.number_pad_decimal_point_description)))
        compose.assertTouchTargets()
    }

    @Test
    fun durationPadHasSecondSteps() {
        val keys = mutableListOf<NumberPadKey>()
        setPad(NumberFieldKind.DURATION) { keys += it }

        compose.onNodeWithContentDescription("15초 빼기").performClick()
        compose.onNodeWithContentDescription("5초 빼기").performClick()
        compose.onNodeWithContentDescription("5초 더하기").performClick()
        compose.onNodeWithContentDescription("15초 더하기").performClick()
        assertEquals(listOf(-15, -5, 5, 15).map(NumberPadKey::Step), keys)
        assertTrue(!exists(string(R.string.number_pad_decimal_point_description)))
        assertTrue(!exists("1 더하기"))
    }

    @Test
    fun everyKeyIsLabelledAndReportsItsKey() {
        val keys = mutableListOf<NumberPadKey>()
        setPad(NumberFieldKind.WEIGHT) { keys += it }

        (0..9).forEach { compose.onNodeWithText("$it").performClick() }
        compose.onNodeWithContentDescription(string(R.string.number_pad_backspace)).performClick()
        compose.onNodeWithContentDescription(string(R.string.number_pad_backspace))
            .performTouchInput { longClick() }
        compose.onNodeWithText(string(R.string.number_pad_next)).performClick()
        compose.onNodeWithContentDescription(string(R.string.number_pad_close)).performClick()

        assertEquals(
            (0..9).map(NumberPadKey::Digit) +
                listOf(NumberPadKey.Backspace, NumberPadKey.ClearAll, NumberPadKey.Next, NumberPadKey.Close),
            keys,
        )
    }

    @Test
    fun nextKeyReadsDoneOnLastField() {
        setPad(NumberFieldKind.REPS, isLastField = true)
        compose.onNodeWithText(string(R.string.number_pad_done)).assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText(string(R.string.number_pad_next)).fetchSemanticsNodes().isEmpty())
    }

    // 화면에 붙였을 때

    @Test
    fun tappingFieldOpensPadWithWholeValueSelectedAndNextMovesThenCloses() {
        val events = mutableListOf<NumberPadEvent<HostFieldId>>()
        compose.setContent {
            NextSetTheme {
                NumberPadTestHost(RecordType.WEIGHT_REPS, listOf(listOf(60_000, 10)), onEvent = { events += it })
            }
        }

        compose.onNodeWithTag(fieldTag(0, 0)).performClick()
        compose.onNodeWithTag(fieldTag(0, 0)).assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        // 값 전체가 선택된 상태에서 증감 키는 지금 값에 더한다.
        compose.onNodeWithContentDescription("2.5 더하기").performClick()
        compose.onNodeWithContentDescription("${string(R.string.number_field_weight)}, 62.5").assertIsDisplayed()

        compose.onNodeWithText(string(R.string.number_pad_next)).performClick()
        compose.onNodeWithTag(fieldTag(0, 1)).assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        // 값 전체가 선택된 상태에서 숫자를 치면 새 값이 된다.
        compose.onNodeWithText("8").performClick()
        compose.onNodeWithContentDescription("${string(R.string.number_field_reps)}, 8").assertIsDisplayed()

        compose.onNodeWithText(string(R.string.number_pad_done)).performClick()
        compose.waitForIdle()
        assertTrue(!exists(string(R.string.number_pad_close)))
        assertEquals(
            listOf(
                NumberPadEvent.ValueChanged(HostFieldId(0, 0), 62_500),
                NumberPadEvent.FocusMoved(HostFieldId(0, 1)),
                NumberPadEvent.ValueChanged(HostFieldId(0, 1), 8),
                NumberPadEvent.Finished,
            ),
            events,
        )
    }

    @Test
    fun padPushesContentUpSoSelectedFieldStaysVisibleBelowRestBar() {
        val rows = List(20) { listOf<Int?>(60_000, 10) }
        compose.setContent { NextSetTheme { NumberPadTestHost(RecordType.WEIGHT_REPS, rows, restBar = true) } }

        // 화면 아래쪽 행을 고른다. 숫자판이 열리기 전에는 보이는 자리다.
        val listBottom = compose.onNodeWithTag(LIST).fetchSemanticsNode().boundsInRoot.bottom
        val lastVisibleRow = rows.indices.last { row ->
            compose.onAllNodesWithTag(fieldTag(row, 1)).fetchSemanticsNodes()
                .firstOrNull()?.boundsInRoot?.let { it.bottom <= listBottom } == true
        }
        assertTrue("목록이 화면보다 길어야 한다", lastVisibleRow < rows.lastIndex)
        compose.onNodeWithTag(fieldTag(lastVisibleRow, 1)).performClick()
        compose.waitForIdle()

        val field = compose.onNodeWithTag(fieldTag(lastVisibleRow, 1)).fetchSemanticsNode().boundsInRoot
        val restBar = compose.onNodeWithTag(REST_BAR).fetchSemanticsNode().boundsInRoot
        val padTop = compose.onNodeWithText(string(R.string.number_pad_done)).fetchSemanticsNode().boundsInRoot.top
        assertTrue("고른 칸($field)이 휴식 바($restBar) 위에 보여야 한다", field.bottom <= restBar.top + 0.5f)
        assertTrue("휴식 바($restBar)가 숫자판 위($padTop)에 있어야 한다", restBar.bottom <= padTop)
    }
}
