package io.github.zzunsik.nextset.ui.exercise

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.ui.exercise.TestExercises.benchPress
import io.github.zzunsik.nextset.ui.exercise.TestExercises.customSledPush
import io.github.zzunsik.nextset.ui.exercise.TestExercises.pushUp
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerMode
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** 종목 고르기와 종목 추가·수정 화면이 상태대로 그려지고 누르면 콜백이 불리는지. */
@RunWith(RobolectricTestRunner::class)
class ExerciseScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()

    private fun string(id: Int, vararg args: Any) = app.getString(id, *args)

    private fun pickerState(
        mode: ExercisePickerMode = ExercisePickerMode.Pick,
        query: String = "",
        selected: List<String> = emptyList(),
        exercises: List<Exercise> = listOf(benchPress, pushUp, customSledPush),
    ) = ExercisePickerUiState(mode = mode, query = query, exercises = exercises, selectedIds = selected, loading = false)

    private fun setPicker(
        state: ExercisePickerUiState,
        onToggle: (String) -> Unit = {},
        onConfirm: () -> Unit = {},
        onNewExercise: (String) -> Unit = {},
        onEditExercise: (String) -> Unit = {},
    ) {
        compose.setContent {
            NextSetTheme {
                ExercisePickerScreen(
                    state = state,
                    onBack = {},
                    onQueryChange = {},
                    onBodyPartSelect = {},
                    onSubPartToggle = {},
                    onToggle = onToggle,
                    onConfirm = onConfirm,
                    onNewExercise = onNewExercise,
                    onEditExercise = onEditExercise,
                )
            }
        }
    }

    @Test
    fun rowsShowNamePartAndEquipmentAsText() {
        setPicker(pickerState())
        compose.onNodeWithText(benchPress.nameKo).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.exercise_row_detail, "가슴", "바벨")).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.exercise_row_detail_custom, "하체", "기타")).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.exercise_picker_title)).assertIsDisplayed()
    }

    @Test
    fun pickingTogglesRowsAndConfirmsWithCount() {
        val toggled = mutableListOf<String>()
        var confirmed = 0
        setPicker(
            pickerState(selected = listOf(pushUp.id, customSledPush.id)),
            onToggle = { toggled += it },
            onConfirm = { confirmed++ },
        )
        compose.onNodeWithText(pushUp.nameKo, useUnmergedTree = false).assertIsOn()
        compose.onNodeWithText(benchPress.nameKo).assertIsOff().performClick()
        assertEquals(listOf(benchPress.id), toggled)

        compose.onNodeWithText(string(R.string.exercise_add_selected, 2)).performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun confirmButtonIsHiddenUntilSomethingIsPicked() {
        setPicker(pickerState())
        compose.onNodeWithText(string(R.string.exercise_add_selected, 0)).assertDoesNotExist()
    }

    @Test
    fun noResultsOffersToCreateAnExerciseWithTheQuery() {
        var created: String? = null
        setPicker(pickerState(query = " 케틀벨 스윙 ", exercises = emptyList()), onNewExercise = { created = it })
        compose.onNodeWithText(string(R.string.exercise_no_results, "케틀벨 스윙")).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.exercise_create_named, "케틀벨 스윙")).performClick()
        assertEquals("케틀벨 스윙", created)
    }

    @Test
    fun manageModeEditsOnlyCustomExercises() {
        val edited = mutableListOf<String>()
        setPicker(pickerState(mode = ExercisePickerMode.Manage), onEditExercise = { edited += it })
        compose.onNodeWithText(string(R.string.exercise_manage_title)).assertIsDisplayed()
        compose.onNodeWithText(benchPress.nameKo).assertHasNoClickAction()
        compose.onNodeWithText(customSledPush.nameKo).assertHasClickAction().performClick()
        assertEquals(listOf(customSledPush.id), edited)
    }

    private fun setForm(state: ExerciseFormUiState, onDelete: () -> Unit = {}, onSave: () -> Unit = {}) {
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
                    onSave = onSave,
                    onDelete = onDelete,
                )
            }
        }
    }

    @Test
    fun formShowsConflictAndDisablesSave() {
        setForm(
            ExerciseFormUiState(
                name = benchPress.nameKo,
                bodyPart = BodyPart.CHEST,
                equipment = Equipment.BARBELL,
                nameConflict = true,
            ),
        )
        compose.onNodeWithText(string(R.string.exercise_form_name_conflict)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.exercise_form_save)).assertIsNotEnabled()
        compose.onNodeWithText(string(R.string.exercise_form_delete)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.exercise_new)).assertIsDisplayed()
    }

    @Test
    fun formShowsSubPartsOnlyForLegsAndArms() {
        setForm(ExerciseFormUiState(name = "a", bodyPart = BodyPart.CHEST))
        compose.onNodeWithText(string(R.string.exercise_form_sub_part)).assertDoesNotExist()
    }

    @Test
    fun deletingAsksForConfirmation() {
        var deleted = 0
        setForm(
            ExerciseFormUiState(
                isEdit = true,
                name = customSledPush.nameKo,
                bodyPart = BodyPart.LEGS,
                equipment = Equipment.OTHER,
                recordType = RecordType.REPS,
                showsRecordTypeNotice = true,
            ),
            onDelete = { deleted++ },
        )
        compose.onNodeWithText(string(R.string.exercise_form_edit_title)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.exercise_form_sub_part)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(string(R.string.exercise_form_record_type_notice)).performScrollTo().assertIsDisplayed()

        compose.onNodeWithText(string(R.string.exercise_form_delete)).performScrollTo().performClick()
        assertEquals("확인 전에는 지우지 않는다", 0, deleted)
        compose.onNodeWithText(string(R.string.exercise_delete_confirm)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.action_delete)).performClick()
        assertEquals(1, deleted)
    }

    /** 이름만 적고 부위와 장비를 고르지 않으면 "저장"이 왜 비활성인지 화면에 보인다(#55). */
    @Test
    fun formTellsWhichChoicesAreMissingWhenSaveIsDisabled() {
        setForm(ExerciseFormUiState(name = "스레드 풀다운"))
        compose.onNodeWithText(string(R.string.exercise_form_save)).assertIsNotEnabled()
        compose.onNodeWithText(string(R.string.exercise_form_missing_both)).assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun formTellsWhenOnlyEquipmentIsMissing() {
        setForm(ExerciseFormUiState(name = "스레드 풀다운", bodyPart = BodyPart.BACK))
        compose.onNodeWithText(string(R.string.exercise_form_missing_equipment)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.exercise_form_missing_body_part)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.exercise_form_missing_both)).assertDoesNotExist()
    }

    @Test
    fun formTellsWhenOnlyBodyPartIsMissing() {
        setForm(ExerciseFormUiState(name = "스레드 풀다운", equipment = Equipment.CABLE))
        compose.onNodeWithText(string(R.string.exercise_form_missing_body_part)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.exercise_form_missing_equipment)).assertDoesNotExist()
    }

    /** 이름이 비었을 때는 빈 이름 칸이 이유다. 부위와 장비만 빠졌다고 잘못 알리지 않는다. */
    @Test
    fun formShowsNoMissingChoiceHintWhileNameIsBlank() {
        setForm(ExerciseFormUiState())
        compose.onNodeWithText(string(R.string.exercise_form_missing_both)).assertDoesNotExist()
    }

    @Test
    fun formShowsNoMissingChoiceHintWhenEverythingIsChosen() {
        setForm(ExerciseFormUiState(name = "스레드 풀다운", bodyPart = BodyPart.BACK, equipment = Equipment.CABLE))
        compose.onNodeWithText(string(R.string.exercise_form_missing_both)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.exercise_form_missing_body_part)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.exercise_form_missing_equipment)).assertDoesNotExist()
    }
}
