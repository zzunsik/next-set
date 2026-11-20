package io.github.zzunsik.nextset.ui.exercise

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.toSize
import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

/**
 * 칩 하나를 골라도 칩의 폭이 바뀌지 않아 다른 칩이 옆으로 밀리거나 다른 줄로 옮겨 가지 않는지(#56).
 * 고르기 전과 뒤에 각 칩의 배치 영역을 견준다.
 */
// 실제 글꼴로 글자 폭을 재야 칩 폭이 화면과 같다. 기본 그래픽 모드에서는 한글 폭이 0으로 잡힌다.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class SelectChipReflowTest {
    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()

    private fun boundsOf(labels: List<String>): Map<String, Rect> =
        labels.associateWith { label ->
            // 스크롤 밖에 걸친 칩도 제 자리를 견주도록 잘리지 않은 배치 영역을 쓴다.
            val node = compose.onNodeWithText(label).fetchSemanticsNode()
            Rect(node.positionInRoot, node.size.toSize())
        }

    private fun assertSameBounds(before: Map<String, Rect>, after: Map<String, Rect>) {
        before.forEach { (label, rect) ->
            assertEquals("\"$label\" 칩의 자리", rect, after.getValue(label))
        }
    }

    private val bodyPartLabels get() = BodyPart.entries.map { app.getString(it.label) }
    private val equipmentLabels get() = Equipment.entries.map { app.getString(it.label) }
    private val recordTypeLabels get() = RecordType.entries.map { app.getString(it.label) }

    private fun setForm(initial: ExerciseFormUiState): (ExerciseFormUiState) -> Unit {
        var state by mutableStateOf(initial)
        compose.setContent {
            NextSetTheme {
                ExerciseFormScreen(
                    state = state,
                    onBack = {},
                    onNameChange = {},
                    onBodyPartSelect = {},
                    onSubPartToggle = {},
                    onEquipmentSelect = {},
                    onRecordTypeSelect = {},
                    onSave = {},
                    onDelete = {},
                )
            }
        }
        return { state = it }
    }

    @Test
    fun selectingBodyPartKeepsEveryBodyPartChipInPlace() {
        val update = setForm(ExerciseFormUiState(name = "칩 시험"))
        val before = boundsOf(bodyPartLabels)
        // 부위 칩이 여러 줄로 놓이는 폭에서 견준다. 한 줄이면 줄 바뀜을 확인할 수 없다.
        assertTrue("부위 칩이 두 줄 이상", before.values.map { it.top }.distinct().size > 1)

        update(ExerciseFormUiState(name = "칩 시험", bodyPart = BodyPart.LEGS))
        compose.waitForIdle()

        assertSameBounds(before, boundsOf(bodyPartLabels))
    }

    @Test
    fun selectingEquipmentKeepsEquipmentAndRecordTypeChipsInPlace() {
        val update = setForm(ExerciseFormUiState(name = "칩 시험", bodyPart = BodyPart.CHEST))
        val before = boundsOf(equipmentLabels + recordTypeLabels)

        update(ExerciseFormUiState(name = "칩 시험", bodyPart = BodyPart.CHEST, equipment = Equipment.BARBELL))
        compose.waitForIdle()

        assertSameBounds(before, boundsOf(equipmentLabels + recordTypeLabels))
    }

    @Test
    fun selectingFilterChipKeepsPickerRowInPlace() {
        var state by mutableStateOf(ExercisePickerUiState(loading = true))
        compose.setContent {
            NextSetTheme {
                ExercisePickerScreen(
                    state = state,
                    onBack = {},
                    onQueryChange = {},
                    onBodyPartSelect = {},
                    onSubPartToggle = {},
                    onToggle = {},
                    onConfirm = {},
                    onNewExercise = {},
                    onEditExercise = {},
                )
            }
        }
        val labels = listOf(app.getString(io.github.zzunsik.nextset.R.string.body_part_all)) + bodyPartLabels
        val before = boundsOf(labels)

        state = ExercisePickerUiState(loading = true, bodyPart = BodyPart.CHEST)
        compose.waitForIdle()

        assertSameBounds(before, boundsOf(labels))
    }
}
