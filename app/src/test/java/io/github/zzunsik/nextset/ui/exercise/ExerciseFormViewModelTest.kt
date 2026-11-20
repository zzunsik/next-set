package io.github.zzunsik.nextset.ui.exercise

import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.ExerciseDeleteResult
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SubPart
import io.github.zzunsik.nextset.ui.exercise.TestExercises.benchPress
import io.github.zzunsik.nextset.ui.exercise.TestExercises.customSledPush
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** 종목 추가·수정의 화면 상태(EX-04, LOG-08). */
class ExerciseFormViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakeExerciseRepository()

    private fun TestScope.form(exerciseId: String? = null, initialName: String = ""): ExerciseFormViewModel {
        val viewModel = ExerciseFormViewModel(repository, exerciseId, initialName)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun ExerciseFormViewModel.fillValid(name: String = "케틀벨 스윙") {
        onNameChange(name)
        onBodyPartSelect(BodyPart.LEGS)
        onEquipmentSelect(Equipment.KETTLEBELL)
    }

    @Test
    fun addsACustomExercise() = runTest {
        val form = form(initialName = "케틀벨 스윙")
        assertEquals("검색어가 이름 칸에 채워진다", "케틀벨 스윙", form.uiState.value.name)
        assertFalse("부위와 장비를 고르기 전에는 저장하지 않는다", form.uiState.value.canSave)

        form.onBodyPartSelect(BodyPart.LEGS)
        form.onSubPartToggle(SubPart.GLUTES)
        form.onEquipmentSelect(Equipment.KETTLEBELL)
        form.onRecordTypeSelect(RecordType.REPS)
        assertTrue(form.uiState.value.canSave)
        form.save()

        val outcome = form.uiState.value.outcome as ExerciseFormOutcome.Created
        val saved = repository.getExercise(outcome.exerciseId)!!
        assertEquals("케틀벨 스윙", saved.nameKo)
        assertEquals(BodyPart.LEGS, saved.bodyPart)
        assertEquals(SubPart.GLUTES, saved.subPart)
        assertEquals(Equipment.KETTLEBELL, saved.equipment)
        assertEquals(RecordType.REPS, saved.recordType)
        assertFalse(saved.isBuiltin)
        assertFalse("저장한 뒤에는 다시 저장하지 않는다", form.uiState.value.canSave)
    }

    @Test
    fun recordTypeDefaultsToWeightAndReps() = runTest {
        val form = form()
        assertEquals(RecordType.WEIGHT_REPS, form.uiState.value.recordType)
        assertFalse(form.uiState.value.isEdit)
        assertFalse(form.uiState.value.showsRecordTypeNotice)
    }

    @Test
    fun emptyNameCannotBeSaved() = runTest {
        val form = form()
        form.fillValid(name = "   ")
        assertFalse(form.uiState.value.canSave)
        assertFalse("빈 이름은 겹침 오류가 아니다", form.uiState.value.nameConflict)

        form.save()
        assertNull(form.uiState.value.outcome)
        assertEquals(TestExercises.all.size, repository.exercises.value.size)
    }

    @Test
    fun duplicateNameCannotBeSaved() = runTest {
        val form = form()
        form.fillValid(name = "바벨벤치프레스")
        assertTrue("띄어쓰기만 다른 내장 종목 이름과 겹친다", form.uiState.value.nameConflict)
        assertFalse(form.uiState.value.canSave)
        form.save()
        assertNull(form.uiState.value.outcome)

        form.onNameChange("Barbell Bench Press")
        assertTrue("영어 이름과도 겹친다", form.uiState.value.nameConflict)

        form.onNameChange(customSledPush.nameKo)
        assertTrue("직접 추가한 종목과도 겹친다", form.uiState.value.nameConflict)

        form.onNameChange(TestExercises.archivedCustom.nameKo)
        assertFalse("숨긴 종목의 이름은 다시 쓸 수 있다", form.uiState.value.nameConflict)

        form.onNameChange("벤치")
        assertFalse("별칭과 같은 이름은 쓸 수 있다", form.uiState.value.nameConflict)
        assertTrue(form.uiState.value.canSave)
    }

    @Test
    fun editsACustomExerciseKeepingFieldsTheFormDoesNotShow() = runTest {
        val form = form(exerciseId = customSledPush.id)
        val loaded = form.uiState.value
        assertTrue(loaded.isEdit)
        assertFalse(loaded.loading)
        assertEquals(customSledPush.nameKo, loaded.name)
        assertEquals(BodyPart.LEGS, loaded.bodyPart)
        assertEquals(Equipment.OTHER, loaded.equipment)
        assertEquals(RecordType.DURATION, loaded.recordType)
        assertFalse("자기 이름은 겹침이 아니다", loaded.nameConflict)
        assertTrue(loaded.canSave)

        form.onNameChange("썰매 끌기")
        form.onSubPartToggle(SubPart.QUADS)
        form.save()

        assertEquals(ExerciseFormOutcome.Updated, form.uiState.value.outcome)
        val saved = repository.getExercise(customSledPush.id)!!
        assertEquals("썰매 끌기", saved.nameKo)
        assertEquals(SubPart.QUADS, saved.subPart)
        assertEquals("휴식 시간은 그대로다", 90, saved.restSeconds)
    }

    @Test
    fun editRejectsDuplicateNames() = runTest {
        val form = form(exerciseId = customSledPush.id)
        form.onNameChange(benchPress.nameKo)
        assertTrue(form.uiState.value.nameConflict)
        form.save()
        assertNull(form.uiState.value.outcome)
        assertEquals(customSledPush.nameKo, repository.getExercise(customSledPush.id)!!.nameKo)
    }

    @Test
    fun changingBodyPartClearsMismatchedSubPart() = runTest {
        val form = form()
        form.onBodyPartSelect(BodyPart.ARMS)
        form.onSubPartToggle(SubPart.BICEPS)
        assertEquals(SubPart.BICEPS, form.uiState.value.subPart)
        form.onSubPartToggle(SubPart.BICEPS)
        assertNull("고른 칩을 다시 누르면 풀린다", form.uiState.value.subPart)

        form.onSubPartToggle(SubPart.TRICEPS)
        form.onBodyPartSelect(BodyPart.CHEST)
        assertNull(form.uiState.value.subPart)
        form.onSubPartToggle(SubPart.QUADS)
        assertNull("부위와 맞지 않는 세부 부위는 고르지 않는다", form.uiState.value.subPart)
    }

    @Test
    fun recordTypeNoticeShowsOnlyWhenAnExerciseWithRecordsChangesType() = runTest {
        val withoutRecords = form(exerciseId = customSledPush.id)
        withoutRecords.onRecordTypeSelect(RecordType.REPS)
        assertFalse("기록이 없으면 안내하지 않는다", withoutRecords.uiState.value.showsRecordTypeNotice)

        repository.usedInWorkouts += customSledPush.id
        val withRecords = form(exerciseId = customSledPush.id)
        assertFalse(withRecords.uiState.value.showsRecordTypeNotice)
        withRecords.onRecordTypeSelect(RecordType.REPS)
        assertTrue(withRecords.uiState.value.showsRecordTypeNotice)
        withRecords.onRecordTypeSelect(RecordType.DURATION)
        assertFalse("처음 방식으로 되돌리면 안내가 사라진다", withRecords.uiState.value.showsRecordTypeNotice)

        withRecords.onRecordTypeSelect(RecordType.WEIGHT_REPS)
        withRecords.save()
        assertEquals("기록이 있어도 방식을 바꿔 저장한다", RecordType.WEIGHT_REPS, repository.getExercise(customSledPush.id)!!.recordType)
    }

    @Test
    fun deletesUnusedCustomExercise() = runTest {
        val form = form(exerciseId = customSledPush.id)
        form.delete()
        assertEquals(ExerciseFormOutcome.Deleted(ExerciseDeleteResult.DELETED), form.uiState.value.outcome)
        assertNull(repository.getExercise(customSledPush.id))
    }

    @Test
    fun deletingAnExerciseWithRecordsOnlyHidesIt() = runTest {
        repository.usedInWorkouts += customSledPush.id
        val form = form(exerciseId = customSledPush.id)
        form.delete()
        assertEquals(ExerciseFormOutcome.Deleted(ExerciseDeleteResult.ARCHIVED), form.uiState.value.outcome)
        assertTrue(repository.getExercise(customSledPush.id)!!.isArchived)
    }

    @Test
    fun builtinOrMissingExercisesAreNotEditable() = runTest {
        val builtin = form(exerciseId = benchPress.id)
        assertEquals(ExerciseFormOutcome.Unavailable, builtin.uiState.value.outcome)
        assertFalse(builtin.uiState.value.canSave)
        builtin.delete()
        assertEquals("내장 종목은 지우지 않는다", benchPress, repository.getExercise(benchPress.id))

        val missing = form(exerciseId = "custom-none")
        assertEquals(ExerciseFormOutcome.Unavailable, missing.uiState.value.outcome)
    }

    @Test
    fun newFormCannotDelete() = runTest {
        val form = form()
        form.fillValid()
        form.delete()
        assertNull(form.uiState.value.outcome)
    }

    /** 이름을 적은 뒤 빠진 부위와 장비를 알린다. 고를 때마다 알림이 줄어 저장할 수 있게 되면 사라진다(#55). */
    @Test
    fun tellsWhichChoicesAreMissingOnceNameIsFilled() = runTest {
        val form = form()
        assertFalse("이름이 비면 빈 이름 칸이 이유다", form.uiState.value.missingBodyPart)
        assertFalse(form.uiState.value.missingEquipment)

        form.onNameChange("스레드 풀다운")
        assertTrue(form.uiState.value.missingBodyPart)
        assertTrue(form.uiState.value.missingEquipment)
        assertFalse(form.uiState.value.canSave)

        form.onBodyPartSelect(BodyPart.BACK)
        assertFalse(form.uiState.value.missingBodyPart)
        assertTrue(form.uiState.value.missingEquipment)

        form.onEquipmentSelect(Equipment.CABLE)
        assertFalse(form.uiState.value.missingEquipment)
        assertTrue(form.uiState.value.canSave)
    }
}
