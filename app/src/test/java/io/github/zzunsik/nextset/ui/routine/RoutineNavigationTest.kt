package io.github.zzunsik.nextset.ui.routine

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.activity.ComponentActivity
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.ui.exercise.EXERCISE_LIST_TAG
import io.github.zzunsik.nextset.ui.exercise.TestExercises.benchPress
import io.github.zzunsik.nextset.ui.exercise.TestExercises.pushUp
import io.github.zzunsik.nextset.ui.navigation.NextSetNavDisplay
import io.github.zzunsik.nextset.ui.settings.FakeLicenseRepository
import io.github.zzunsik.nextset.ui.settings.FakeSettingsRepository
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 앱의 화면 이동 그대로 루틴을 만들고 고친다. 홈의 임시 "루틴" 버튼 → 루틴 목록 → 루틴 편집 → 종목 고르기 → 종목 설정.
 * 저장소는 메모리 가짜를 쓴다.
 */
@RunWith(RobolectricTestRunner::class)
class RoutineNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = compose.activity
    private val repository = FakeRoutineRepository()

    private fun show() {
        compose.setContent {
            NextSetTheme {
                NextSetNavDisplay(
                    settingsRepository = FakeSettingsRepository(),
                    licenseRepository = FakeLicenseRepository(),
                    exerciseRepository = repository.exercises,
                    routineRepository = repository,
                )
            }
        }
        compose.onNodeWithText(app.getString(R.string.home_open_routines)).performClick()
    }

    private fun text(id: Int, vararg args: Any) = app.getString(id, *args)

    private fun pickExercises(vararg names: String) {
        compose.onNodeWithText(text(R.string.routine_add_exercise)).performScrollTo().performClick()
        names.forEach { name ->
            compose.onNodeWithTag(EXERCISE_LIST_TAG).performScrollToNode(hasText(name))
            compose.onNodeWithText(name).performClick()
        }
        compose.onNodeWithText(text(R.string.exercise_add_selected, names.size)).performClick()
    }

    private fun back() = compose.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()

    /**
     * 저장소 쓰기가 끝나기를 기다린다. 이 테스트에서는 `waitUntil`만으로 "되돌리기" 뒤의 순서 바꾸기가 끝나지 않아
     * 매번 화면을 쉬게 하며(`waitForIdle`) 조건을 본다.
     */
    private fun settle(condition: () -> Boolean) {
        repeat(100) {
            compose.waitForIdle()
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("조건을 만족하지 않았다")
    }

    private fun waitForText(value: String) = compose.waitUntil(5_000) {
        compose.onAllNodes(hasText(value)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun createRoutineWithExercisesAndComeBackToTheList() {
        show()
        compose.onNodeWithText(text(R.string.routine_list_empty)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.routine_create)).performClick()

        // 새 루틴: 제목이 "새 루틴"이고 지우기 버튼이 없다.
        compose.onNodeWithText(text(R.string.routine_no_exercises)).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("가슴과 삼두")
        pickExercises(pushUp.nameKo, benchPress.nameKo)

        compose.onNodeWithText(pushUp.nameKo).assertIsDisplayed()
        compose.onNodeWithText(benchPress.nameKo).assertIsDisplayed()
        val routine = repository.routines.value.single()
        assertEquals("가슴과 삼두", routine.name)
        assertEquals(listOf(pushUp.id, benchPress.id), repository.itemsOf(routine.id).map { it.exerciseId })

        back()
        compose.onNodeWithText("가슴과 삼두").assertIsDisplayed()
    }

    @Test
    fun leavingAnUntouchedNewRoutineLeavesNothingBehind() {
        show()
        compose.onNodeWithText(text(R.string.routine_create)).performClick()
        compose.onNodeWithText(text(R.string.routine_no_exercises)).assertIsDisplayed()
        assertEquals(1, repository.routines.value.size)

        back()
        compose.onNodeWithText(text(R.string.routine_list_empty)).assertIsDisplayed()
        assertTrue(repository.routines.value.isEmpty())
    }

    /** 시스템 뒤로 가기도 상단 바의 뒤로 가기처럼 빈 새 루틴을 버린다. */
    @Test
    fun systemBackAlsoDiscardsAnUntouchedNewRoutine() {
        show()
        compose.onNodeWithText(text(R.string.routine_create)).performClick()
        compose.onNodeWithText(text(R.string.routine_no_exercises)).assertIsDisplayed()

        compose.runOnUiThread { app.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText(text(R.string.routine_list_empty)).assertIsDisplayed()
        assertTrue(repository.routines.value.isEmpty())
    }

    @Test
    fun exerciseSettingsSaveTargetsAndRemoveWithUndo() {
        val id = runBlocking {
            repository.addRoutine("가슴").also {
                repository.addRoutineExercise(it, benchPress.id, 3)
                repository.addRoutineExercise(it, pushUp.id, 3)
            }
        }
        show()
        compose.onNodeWithText("가슴").performClick()
        compose.onNodeWithText(benchPress.nameKo).performClick()

        // 종목 설정: 세트 수를 늘리고 목표 무게를 적는다.
        compose.onNodeWithContentDescription(text(R.string.routine_exercise_sets_increase)).performClick()
        compose.onNodeWithTag(targetFieldTag(TargetField.WEIGHT)).performClick()
        compose.onNodeWithText("6").performClick()
        compose.onNodeWithText("0").performClick()
        compose.onNodeWithContentDescription(text(R.string.number_pad_close)).performClick()
        back()

        compose.onNodeWithText("4세트 · 60kg").assertIsDisplayed()
        val bench = repository.itemsOf(id).first()
        assertEquals(4, bench.setCount)
        assertEquals(Target(weight = io.github.zzunsik.nextset.domain.model.Weight.kg(60)), bench.target)

        // 루틴에서 빼고 되돌린다.
        compose.onNodeWithText(benchPress.nameKo).performClick()
        compose.onNodeWithText(text(R.string.routine_exercise_remove)).performScrollTo().performClick()
        waitForText(text(R.string.routine_exercise_removed, benchPress.nameKo))
        assertEquals(listOf(pushUp.id), repository.itemsOf(id).map { it.exerciseId })

        compose.onNodeWithText(text(R.string.action_undo)).performClick()
        settle { repository.itemsOf(id).map { it.exerciseId } == listOf(benchPress.id, pushUp.id) }
        assertEquals(4, repository.itemsOf(id).first().setCount)
    }

    /** RTN-05: 루틴 목록의 순서를 바꾼다. 화면 낭독기 동작으로 옮긴다. */
    @Test
    fun routineListOrderCanBeChanged() {
        runBlocking {
            repository.addRoutine("가슴")
            repository.addRoutine("등")
            repository.addRoutine("하체")
        }
        show()
        val node = compose.onNodeWithText("하체").fetchSemanticsNode()
        val moveUp = node.config.getOrNull(SemanticsActions.CustomActions).orEmpty()
            .single { it.label == text(R.string.move_up) }
        compose.runOnIdle { moveUp.action() }

        settle { repository.routines.value.map { it.name } == listOf("가슴", "하체", "등") }
        compose.onNodeWithTag(ROUTINE_LIST_TAG).assertIsDisplayed()
    }
}
