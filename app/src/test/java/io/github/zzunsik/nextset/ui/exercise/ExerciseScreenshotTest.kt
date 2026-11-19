package io.github.zzunsik.nextset.ui.exercise

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SubPart
import io.github.zzunsik.nextset.ui.a11y.assertAccessible
import io.github.zzunsik.nextset.ui.exercise.TestExercises.backSquat
import io.github.zzunsik.nextset.ui.exercise.TestExercises.customSledPush
import io.github.zzunsik.nextset.ui.exercise.TestExercises.lyingLegCurl
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerMode
import io.github.zzunsik.nextset.ui.screenshot.LARGE_FONT_SCALE
import io.github.zzunsik.nextset.ui.screenshot.SCREENSHOT_DEVICE
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner

/** 종목 고르기의 스크린샷 비교와 접근성 검사. 화면 모드는 시스템 설정(`+night`)을 따른다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class ExercisePickerScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /** 하체를 골라 세부 부위 줄이 나온 상태. 두 종목을 골라 "2개 추가"가 보인다. */
    private val picking = ExercisePickerUiState(
        mode = ExercisePickerMode.Pick,
        bodyPart = BodyPart.LEGS,
        exercises = listOf(lyingLegCurl, backSquat, customSledPush),
        selectedIds = listOf(backSquat.id, customSledPush.id),
        loading = false,
    )

    private fun capture(state: ExercisePickerUiState) {
        compose.setContent { NextSetTheme { Picker(state) } }
        // 접근성 검사를 먼저 돌려 접근성 문제를 스크린샷 차이보다 먼저 알린다.
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Picker(state: ExercisePickerUiState) {
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

    @Test
    fun light() = capture(picking)

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun lightLargeFont() = capture(picking)

    @Test
    @Config(qualifiers = "+night")
    fun dark() = capture(picking)

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun darkLargeFont() = capture(picking)

    /** 검색 결과 없음. 그 이름으로 종목을 만드는 버튼이 보인다. */
    @Test
    fun noResults() = capture(ExercisePickerUiState(query = "케틀벨 스윙", loading = false))

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun noResultsDarkLargeFont() = capture(ExercisePickerUiState(query = "케틀벨 스윙", loading = false))
}

/** 종목 추가·수정의 스크린샷 비교와 접근성 검사. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class ExerciseFormScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /** 기록이 있는 직접 추가 종목을 고치며 기록 방식을 바꾼 상태. 세부 부위 줄과 안내 문구, 지우기 버튼이 보인다. */
    private val editing = ExerciseFormUiState(
        isEdit = true,
        name = customSledPush.nameKo,
        bodyPart = BodyPart.LEGS,
        subPart = SubPart.QUADS,
        equipment = Equipment.OTHER,
        recordType = RecordType.REPS,
        showsRecordTypeNotice = true,
    )

    private fun capture(state: ExerciseFormUiState = editing) {
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
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun light() = capture()

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun lightLargeFont() = capture()

    @Test
    @Config(qualifiers = "+night")
    fun dark() = capture()

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun darkLargeFont() = capture()
}
