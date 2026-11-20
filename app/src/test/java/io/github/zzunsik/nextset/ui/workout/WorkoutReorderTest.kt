package io.github.zzunsik.nextset.ui.workout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.screenshot.SCREENSHOT_DEVICE
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 운동 중 종목 순서 바꾸기 대화상자(#74). 손잡이를 끌어 옮기고, 화면 낭독기의 "위로/아래로 옮기기"로도 옮긴다.
 * 상태는 [WorkoutSamples]의 지어낸 값이다(벤치 프레스, 풀업, 플랭크).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = SCREENSHOT_DEVICE)
class WorkoutReorderTest {
    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val orders = mutableListOf<List<Long>>()

    private fun show(): WorkoutUiState {
        val state = WorkoutSamples.typical().copy(dialog = WorkoutDialog.Reorder)
        compose.setContent {
            NextSetTheme {
                WorkoutScreen(state = state, now = WorkoutSamples.now, actions = WorkoutActions(onReorderExercises = { orders += it }))
            }
        }
        return state
    }

    /** 행 앞 손잡이(48dp)의 가운데를 잡고 [rows]행만큼 아래로 끌어 놓는다. */
    private fun SemanticsNodeInteraction.dragHandleBy(rows: Float) {
        val height = fetchSemanticsNode().size.height.toFloat()
        performTouchInput {
            // 행 앞 여백(spacing.xs, 4dp) 뒤에 48dp 손잡이가 있다.
            down(Offset((4 + 24).dp.toPx(), centerY))
            val steps = 20
            repeat(steps) { moveBy(Offset(0f, height * rows / steps)) }
            up()
        }
        compose.waitForIdle()
    }

    private fun SemanticsNodeInteraction.customAction(label: String) {
        val actions = fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        val action = actions.firstOrNull { it.label == label } ?: error("'$label' 동작이 없다: ${actions.map { it.label }}")
        compose.runOnIdle { action.action() }
    }

    private fun customActionLabels(tag: String): List<String> =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty().map { it.label }

    @Test
    fun dialogListsExerciseNamesWithDragHandles() {
        val state = show()
        compose.onNodeWithText(app.getString(R.string.workout_reorder)).assertIsDisplayed()
        state.exercises.forEach { compose.onNodeWithTag(reorderRowTag(it.id)).assertIsDisplayed().assert(hasText(it.name)) }
    }

    /** 첫 종목의 손잡이를 한 행 아래로 끌어 놓으면 바뀐 순서를 한 번 저장한다. 끄는 동안에는 저장하지 않는다. */
    @Test
    fun draggingAHandleMovesTheExerciseAndSavesOnRelease() {
        val (bench, pullUp, plank) = show().exercises
        compose.onNodeWithTag(reorderRowTag(bench.id)).dragHandleBy(1.2f)
        assertEquals(listOf(listOf(pullUp.id, bench.id, plank.id)), orders)
    }

    @Test
    fun draggingTheLastExerciseToTheTopSavesThatOrder() {
        val (bench, pullUp, plank) = show().exercises
        compose.onNodeWithTag(reorderRowTag(plank.id)).dragHandleBy(-2.2f)
        assertEquals(listOf(listOf(plank.id, bench.id, pullUp.id)), orders)
    }

    /** 화면 낭독기의 "위로 옮기기", "아래로 옮기기". 맨 위에는 위로, 맨 아래에는 아래로가 없다. */
    @Test
    fun rowsCanBeMovedWithAccessibilityActions() {
        val (bench, pullUp, plank) = show().exercises
        val up = app.getString(R.string.move_up)
        val down = app.getString(R.string.move_down)
        assertEquals(listOf(down), customActionLabels(reorderRowTag(bench.id)))
        assertEquals(listOf(up, down), customActionLabels(reorderRowTag(pullUp.id)))
        assertEquals(listOf(up), customActionLabels(reorderRowTag(plank.id)))

        compose.onNodeWithTag(reorderRowTag(bench.id)).customAction(down)
        compose.onNodeWithTag(reorderRowTag(plank.id)).customAction(up)
        assertEquals(listOf(listOf(pullUp.id, bench.id, plank.id), listOf(bench.id, plank.id, pullUp.id)), orders)
    }
}
