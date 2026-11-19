package io.github.zzunsik.nextset.ui.exercise

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.navigation3.runtime.NavKey
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.ui.exercise.TestExercises.benchPress
import io.github.zzunsik.nextset.ui.exercise.TestExercises.pushUp
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerKey
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerMode
import io.github.zzunsik.nextset.ui.navigation.NavResults
import io.github.zzunsik.nextset.ui.navigation.NextSetNavHost
import io.github.zzunsik.nextset.ui.navigation.ResultEffect
import io.github.zzunsik.nextset.ui.navigation.pickedExercisesKey
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * 종목 고르기가 고른 종목을 부른 화면에 돌려주는지. 루틴 편집(#22)과 운동 중(#24)이 쓰는 방식 그대로
 * 가짜 "부르는 화면"을 스택 맨 아래에 두고 [NextSetNavHost]로 띄운다.
 */
@RunWith(RobolectricTestRunner::class)
class ExerciseNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val repository = FakeExerciseRepository()
    private val received = mutableListOf<List<String>>()
    private lateinit var backStack: MutableList<NavKey>

    @Serializable
    private data object CallerKey : NavKey

    /** 목록을 그 행까지 내린 뒤 누른다. 행은 목록에 보일 때만 그려진다. */
    private fun clickRow(name: String) {
        compose.onNodeWithTag(EXERCISE_LIST_TAG).performScrollToNode(hasText(name))
        compose.onNodeWithText(name).performClick()
    }

    private fun launch(mode: ExercisePickerMode = ExercisePickerMode.Pick) {
        compose.setContent {
            NextSetTheme {
                val stack = remember { mutableStateListOf<NavKey>(CallerKey) }
                backStack = stack
                val results = remember { NavResults() }
                NextSetNavHost(backStack = stack, results = results, exerciseRepository = repository) {
                    entry<CallerKey> {
                        results.ResultEffect(pickedExercisesKey(RESULT)) { received += it }
                        Column {
                            Button(onClick = { stack.add(ExercisePickerKey(mode, resultName = RESULT)) }) {
                                Text(OPEN)
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText(OPEN).performClick()
    }

    @Test
    fun pickedExercisesReturnToTheCaller() {
        launch()
        clickRow(pushUp.nameKo)
        clickRow(benchPress.nameKo)
        compose.onNodeWithText(app.getString(R.string.exercise_add_selected, 2)).performClick()

        compose.onNodeWithText(OPEN).assertIsDisplayed()
        assertEquals("고른 순서대로 한 번 돌려준다", listOf(listOf(pushUp.id, benchPress.id)), received)
        assertEquals(listOf<NavKey>(CallerKey), backStack.toList())
    }

    @Test
    fun backDiscardsTheSelection() {
        launch()
        clickRow(pushUp.nameKo)
        compose.onNodeWithContentDescription(app.getString(R.string.navigate_back)).performClick()

        compose.onNodeWithText(OPEN).assertIsDisplayed()
        assertTrue(received.isEmpty())
    }

    @Test
    fun exerciseCreatedFromNoResultsComesBackSelected() {
        launch()
        compose.onNode(hasSetTextAction()).performTextInput("케틀벨 스윙")
        compose.onNodeWithText(app.getString(R.string.exercise_create_named, "케틀벨 스윙")).performClick()

        // 종목 추가·수정: 이름이 채워져 있다. 부위와 장비를 고르고 저장한다.
        compose.onNodeWithText(app.getString(R.string.body_part_legs)).performScrollTo().performClick()
        compose.onNodeWithText(app.getString(R.string.equipment_kettlebell)).performScrollTo().performClick()
        compose.onNodeWithText(app.getString(R.string.exercise_form_save)).performClick()

        // 종목 고르기로 돌아와 새 종목이 고른 상태로 보인다.
        compose.onNode(hasText("케틀벨 스윙") and isToggleable()).assertIsOn()
        compose.onNodeWithText(app.getString(R.string.exercise_add_selected, 1)).performClick()
        compose.onNodeWithText(OPEN).assertIsDisplayed()

        val created = repository.exercises.value.single { it.nameKo == "케틀벨 스윙" }
        assertEquals(BodyPart.LEGS, created.bodyPart)
        assertEquals(listOf(listOf(created.id)), received)
    }

    @Test
    fun manageModeEditsAndDeletesCustomExercises() {
        launch(ExercisePickerMode.Manage)
        clickRow(TestExercises.customSledPush.nameKo)
        compose.onNodeWithText(app.getString(R.string.exercise_form_edit_title)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.exercise_form_delete)).performScrollTo().performClick()
        compose.onNodeWithText(app.getString(R.string.action_delete)).performClick()

        compose.onNodeWithText(app.getString(R.string.exercise_manage_title)).assertIsDisplayed()
        compose.onNodeWithText(TestExercises.customSledPush.nameKo).assertDoesNotExist()
        assertTrue("관리로 열면 아무것도 돌려주지 않는다", received.isEmpty())
    }

    private companion object {
        const val RESULT = "test-caller"
        const val OPEN = "open picker"
    }
}
