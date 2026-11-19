package io.github.zzunsik.nextset.ui.exercise

import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.ExerciseDraft
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SubPart
import io.github.zzunsik.nextset.ui.exercise.TestExercises.backSquat
import io.github.zzunsik.nextset.ui.exercise.TestExercises.benchPress
import io.github.zzunsik.nextset.ui.exercise.TestExercises.closeGripBenchPress
import io.github.zzunsik.nextset.ui.exercise.TestExercises.customSledPush
import io.github.zzunsik.nextset.ui.exercise.TestExercises.dumbbellBenchPress
import io.github.zzunsik.nextset.ui.exercise.TestExercises.latPulldown
import io.github.zzunsik.nextset.ui.exercise.TestExercises.lyingLegCurl
import io.github.zzunsik.nextset.ui.exercise.TestExercises.pushUp
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** 종목 고르기의 화면 상태. 검색(EX-02), 부위 필터(EX-03), 고르기. */
class ExercisePickerViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakeExerciseRepository()

    private fun TestScope.picker(mode: ExercisePickerMode = ExercisePickerMode.Pick): ExercisePickerViewModel {
        val viewModel = ExercisePickerViewModel(repository, mode)
        // WhileSubscribed라 구독해야 상태가 돈다.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun ExercisePickerViewModel.ids() = uiState.value.exercises.map { it.id }

    private fun ExercisePickerViewModel.searchIds(query: String): List<String> {
        onQueryChange(query)
        return ids()
    }

    @Test
    fun showsBuiltinAndCustomExercisesTogetherWithoutHidden() = runTest {
        val picker = picker()
        val state = picker.uiState.value
        assertFalse(state.loading)
        assertTrue(customSledPush.id in picker.ids())
        assertTrue(benchPress.id in picker.ids())
        assertFalse("숨긴 종목은 나오지 않는다", TestExercises.archivedCustom.id in picker.ids())
        assertEquals(state.exercises.sortedBy { it.nameKo }, state.exercises)
    }

    @Test
    fun searchFindsAliasesSpacedAndUnspacedFormsAndEnglishNames() = runTest {
        val picker = picker()
        val benches = listOf(benchPress.id, dumbbellBenchPress.id, closeGripBenchPress.id).sorted()

        assertEquals("붙여 쓴 형태", benches, picker.searchIds("벤치프레스").sorted())
        assertEquals("띄어 쓴 형태", benches, picker.searchIds("벤치 프레스").sorted())
        assertEquals("영어 이름, 대소문자 무시", listOf(benchPress.id), picker.searchIds("barbell BENCH"))
        assertEquals("영어 이름 붙여 쓰기", listOf(latPulldown.id), picker.searchIds("latpulldown"))
        assertEquals("별칭", listOf(pushUp.id), picker.searchIds("팔굽혀펴기"))
        assertEquals("별칭 띄어쓰기", listOf(latPulldown.id), picker.searchIds("렛 풀 다운"))
        assertEquals("직접 추가한 종목도 찾는다", listOf(customSledPush.id), picker.searchIds("썰매"))
    }

    @Test
    fun noResultsKeepsQueryForCreatingACustomExercise() = runTest {
        val picker = picker()
        picker.onQueryChange("케틀벨 스윙")
        val state = picker.uiState.value
        assertTrue(state.exercises.isEmpty())
        assertTrue(state.showsNoResults)
        assertEquals("케틀벨 스윙", state.query)

        picker.onQueryChange("")
        assertFalse(picker.uiState.value.showsNoResults)
    }

    @Test
    fun bodyPartFilterAndSearchApplyTogether() = runTest {
        val picker = picker()
        picker.onBodyPartSelect(BodyPart.CHEST)
        assertEquals(listOf(dumbbellBenchPress.id, benchPress.id, pushUp.id), picker.ids())

        picker.onQueryChange("벤치프레스")
        assertEquals("팔 종목인 클로즈 그립 벤치 프레스는 빠진다", listOf(dumbbellBenchPress.id, benchPress.id), picker.ids())

        picker.onBodyPartSelect(BodyPart.ARMS)
        assertEquals(listOf(closeGripBenchPress.id), picker.ids())

        picker.onQueryChange("스쿼트")
        assertTrue(picker.uiState.value.showsNoResults)

        picker.onBodyPartSelect(null)
        assertEquals("전체로 돌아가면 검색만 남는다", listOf(backSquat.id), picker.ids())
    }

    @Test
    fun subPartFilterNarrowsLegsAndClearsWhenBodyPartChanges() = runTest {
        val picker = picker()
        picker.onBodyPartSelect(BodyPart.LEGS)
        assertEquals(
            "세부 부위를 고르지 않으면 세부 부위가 없는 직접 추가 종목까지 보인다",
            listOf(lyingLegCurl.id, backSquat.id, customSledPush.id),
            picker.ids(),
        )

        picker.onSubPartToggle(SubPart.HAMSTRINGS)
        assertEquals(listOf(lyingLegCurl.id), picker.ids())

        picker.onSubPartToggle(SubPart.BICEPS)
        assertEquals("하체에 없는 세부 부위는 무시한다", SubPart.HAMSTRINGS, picker.uiState.value.subPart)

        picker.onSubPartToggle(SubPart.HAMSTRINGS)
        assertEquals("고른 칩을 다시 누르면 풀린다", null, picker.uiState.value.subPart)

        picker.onSubPartToggle(SubPart.QUADS)
        picker.onBodyPartSelect(BodyPart.ARMS)
        assertEquals("부위를 바꾸면 세부 부위가 풀린다", null, picker.uiState.value.subPart)
        assertEquals(listOf(closeGripBenchPress.id), picker.ids())
    }

    @Test
    fun selectionKeepsPickOrderAndSurvivesFiltering() = runTest {
        val picker = picker()
        picker.onToggle(pushUp.id)
        picker.onToggle(benchPress.id)
        picker.onToggle(latPulldown.id)
        picker.onToggle(benchPress.id)
        assertEquals(listOf(pushUp.id, latPulldown.id), picker.uiState.value.selectedIds)

        picker.onQueryChange("스쿼트")
        assertEquals("목록에 안 보여도 고른 것은 남는다", listOf(pushUp.id, latPulldown.id), picker.uiState.value.selectedIds)
    }

    @Test
    fun createdExerciseIsAddedAsSelected() = runTest {
        val picker = picker()
        picker.onToggle(pushUp.id)
        picker.onExerciseCreated("custom-new")
        picker.onExerciseCreated("custom-new")
        assertEquals(listOf(pushUp.id, "custom-new"), picker.uiState.value.selectedIds)
    }

    @Test
    fun createdExerciseIsShownEvenWhenFiltersWouldHideIt() = runTest {
        val picker = picker()
        picker.onBodyPartSelect(BodyPart.LEGS)
        picker.onSubPartToggle(SubPart.GLUTES)
        picker.onQueryChange("sled push")
        val id = repository.addCustomExercise(
            ExerciseDraft(
                nameKo = "Sled Push",
                bodyPart = BodyPart.LEGS,
                equipment = Equipment.OTHER,
                recordType = RecordType.DURATION,
            ),
        )
        picker.onExerciseCreated(id)

        val state = picker.uiState.value
        assertEquals(null, state.bodyPart)
        assertEquals(null, state.subPart)
        assertEquals("Sled Push", state.query)
        assertEquals(listOf(id), state.exercises.map { it.id })
        assertEquals(listOf(id), state.selectedIds)
    }

    @Test
    fun manageModeDoesNotSelect() = runTest {
        val picker = picker(ExercisePickerMode.Manage)
        picker.onToggle(pushUp.id)
        picker.onExerciseCreated("custom-new")
        assertEquals(ExercisePickerMode.Manage, picker.uiState.value.mode)
        assertTrue(picker.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun listFollowsRepositoryChanges() = runTest {
        val picker = picker()
        picker.onQueryChange("케틀벨")
        assertTrue(picker.uiState.value.showsNoResults)

        repository.exercises.value = repository.exercises.value + customSledPush.copy(id = "custom-kb", nameKo = "케틀벨 스윙")
        assertEquals(listOf("custom-kb"), picker.ids())
    }
}
