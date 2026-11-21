package io.github.zzunsik.nextset.ui.routine

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.data.repository.TestDbRule
import io.github.zzunsik.nextset.ui.exercise.EXERCISE_LIST_TAG
import io.github.zzunsik.nextset.ui.exercise.TestExercises.pushUp
import io.github.zzunsik.nextset.ui.home.HOME_LIST_TAG
import io.github.zzunsik.nextset.ui.navigation.NextSetNavDisplay
import io.github.zzunsik.nextset.ui.settings.FakeBackupRepository
import io.github.zzunsik.nextset.ui.settings.FakeLicenseRepository
import io.github.zzunsik.nextset.ui.settings.FakeSettingsRepository
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 루틴 이름을 쓰다가 종목 고르기에 다녀와도 입력 칸이 저절로 포커스를 잡지 않는다(#105).
 *
 * Android 8.x(API 26·27)는 포커스를 가진 뷰가 포커스를 놓으면 창이 첫 번째 포커스 가능한 뷰에 포커스를 다시 준다(API 28부터는 터치
 * 모드에서 다시 주지 않는다). 화면이 바뀌어 포커스를 가진 이름 칸이 사라지면 Compose가 뷰의 포커스를 놓고, 창이 Compose 뷰에 포커스를
 * 다시 주면 기기에서는 Compose가 그 화면의 첫 입력 칸(종목 고르기의 검색 칸, 돌아온 뒤에는 이름 칸)에 포커스를 넣어 키보드가 떴다.
 * 그래서 돌아온 뒤 첫 뒤로 가기가 키보드를 닫는 데 쓰여 편집 화면이 닫히지 않았다(에뮬레이터 Android 8.0에서 확인).
 *
 * Robolectric에서는 창이 Compose 뷰에 포커스를 다시 주는 데까지만 일어나고 Compose가 입력 칸으로 옮기지는 않는다. 그래서 입력 칸의
 * 포커스와 함께, 화면 이동 뒤 창이 앱 화면(Compose 뷰)에 포커스를 다시 주지 않는지 본다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class RoutineEditFocusAfterPickerTest {
    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule(order = 0)
    internal val dbRule = TestDbRule(seed = false)

    private val app get() = compose.activity
    private val repository = FakeRoutineRepository()

    private fun text(id: Int, vararg args: Any) = app.getString(id, *args)

    private fun show() {
        compose.setContent {
            NextSetTheme {
                NextSetNavDisplay(
                    settingsRepository = FakeSettingsRepository(),
                    licenseRepository = FakeLicenseRepository(),
                    exerciseRepository = repository.exercises,
                    workoutRepository = dbRule.db.workouts,
                    routineRepository = repository,
                    backupRepository = FakeBackupRepository(),
                )
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag(HOME_LIST_TAG)).fetchSemanticsNodes().isNotEmpty() }
    }

    /** 앱 화면(`android.R.id.content` 아래, 곧 Compose 뷰) 안에서 창의 포커스를 가진 뷰. 입력 칸이 포커스를 가지면 Compose 뷰다. */
    private fun focusedViewInContent(): View? {
        var focused: View? = null
        compose.runOnUiThread { focused = app.findViewById<View>(android.R.id.content).findFocus() }
        return focused
    }

    /** 새 루틴에 이름을 쓰고(이름 칸에 포커스) "종목 추가"로 종목 고르기를 연다. */
    private fun typeNameAndOpenPicker() {
        show()
        compose.onNodeWithText(text(R.string.routine_create)).performClick()
        compose.onNodeWithText(text(R.string.routine_no_exercises)).assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("가슴")
        compose.onNode(hasSetTextAction()).assertIsFocused()
        compose.onNodeWithText(text(R.string.routine_add_exercise)).performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag(EXERCISE_LIST_TAG)).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    /** 종목 고르기에서 한 종목을 골라 "1개 추가"로 루틴 편집에 돌아온다. */
    private fun pickOneAndComeBack() {
        compose.onNodeWithTag(EXERCISE_LIST_TAG).performScrollToNode(hasText(pushUp.nameKo))
        compose.onNodeWithText(pushUp.nameKo).performClick()
        compose.onNodeWithText(text(R.string.exercise_add_selected, 1)).performClick()
        compose.onNodeWithText(pushUp.nameKo).assertIsDisplayed()
        compose.waitForIdle()
    }

    @Test
    fun openingThePickerDoesNotFocusItsSearchField() {
        typeNameAndOpenPicker()
        compose.onNode(hasSetTextAction()).assertIsNotFocused()
        assertNull("창이 종목 고르기 화면에 포커스를 다시 줬다", focusedViewInContent())
    }

    @Test
    fun comingBackFromThePickerDoesNotFocusTheNameFieldAndOneBackClosesTheEditor() {
        typeNameAndOpenPicker()
        pickOneAndComeBack()

        compose.onNode(hasSetTextAction()).assertIsNotFocused()
        assertNull("창이 루틴 편집 화면에 포커스를 다시 줬다", focusedViewInContent())
        compose.runOnUiThread { app.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag(HOME_LIST_TAG)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text(R.string.routine_add_exercise)).assertDoesNotExist()
        compose.onNodeWithText("가슴").assertIsDisplayed()
    }

    /** 고친 뒤에도 입력 칸을 누르면 포커스를 받고 글자를 칠 수 있다. */
    @Test
    fun tappingTheNameFieldAfterComingBackStillFocusesIt() {
        typeNameAndOpenPicker()
        pickOneAndComeBack()

        compose.onNode(hasSetTextAction()).performClick()
        compose.onNode(hasSetTextAction()).assertIsFocused()
        compose.onNode(hasSetTextAction()).performTextReplacement("가슴과 삼두")
        compose.onNodeWithText("가슴과 삼두").assertIsDisplayed()
    }
}
