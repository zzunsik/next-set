package io.github.zzunsik.nextset.ui.routine

import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.ui.component.NumberPadKey
import io.github.zzunsik.nextset.ui.exercise.MainDispatcherRule
import io.github.zzunsik.nextset.ui.exercise.TestExercises.backSquat
import io.github.zzunsik.nextset.ui.exercise.TestExercises.benchPress
import io.github.zzunsik.nextset.ui.exercise.TestExercises.plank
import io.github.zzunsik.nextset.ui.exercise.TestExercises.pushUp
import io.github.zzunsik.nextset.ui.settings.FakeSettingsRepository
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

/** 루틴 목록, 루틴 편집, 종목 설정의 화면 상태(RTN-01~RTN-05). */
class RoutineViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakeRoutineRepository()
    private val settings = FakeSettingsRepository()

    private fun TestScope.list(): RoutineListViewModel =
        RoutineListViewModel(repository, NEW_NAME).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        }

    private fun TestScope.edit(routineId: Long, isNew: Boolean = false): RoutineEditViewModel =
        RoutineEditViewModel(repository, routineId, isNew, NEW_NAME).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        }

    private fun TestScope.exerciseSettings(routineId: Long, itemId: Long): RoutineExerciseViewModel =
        RoutineExerciseViewModel(repository, repository.exercises, settings, routineId, itemId).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        }

    private fun names() = repository.routines.value.map { it.name }

    private fun exerciseIdsOf(routineId: Long) = repository.itemsOf(routineId).map { it.exerciseId }

    /** 루틴 하나에 종목을 넣어 둔다. */
    private suspend fun routineWith(name: String, vararg exerciseIds: String): Long {
        val id = repository.addRoutine(name)
        exerciseIds.forEach { repository.addRoutineExercise(id, it, 3) }
        return id
    }

    // RTN-01

    @Test
    fun newRoutineIsSavedAtTheEndWithTheDefaultName() = runTest {
        routineWith("가슴")
        var opened: Long? = null
        list().createRoutine { opened = it }

        assertEquals(listOf("가슴", NEW_NAME), names())
        assertEquals(repository.routines.value.last().id, opened)
    }

    @Test
    fun newRoutineShowsAnEmptyNameFieldAndRenamesAsYouType() = runTest {
        val id = repository.addRoutine(NEW_NAME)
        val edit = edit(id, isNew = true)
        assertEquals("새 루틴의 이름 칸은 비어 있다", "", edit.uiState.value.name)
        assertTrue(edit.uiState.value.isNew)

        edit.onNameChange("하체")
        assertEquals(listOf("하체"), names())
        assertEquals("하체", edit.uiState.value.name)
        assertEquals("입력 칸이 바로 읽는 값도 같다", "하체", edit.nameText)
    }

    @Test
    fun existingRoutineShowsItsName() = runTest {
        val id = routineWith("등과 이두")
        assertEquals("등과 이두", edit(id).uiState.value.name)
        assertFalse(edit(id).uiState.value.isNew)
    }

    /** 입력 검증: 이름이 비어 있으면 "새 루틴"으로 저장한다. 루틴 이름은 비지 않는다. */
    @Test
    fun blankNameIsSavedAsTheDefaultName() = runTest {
        val id = routineWith("가슴")
        val edit = edit(id)
        edit.onNameChange("   ")

        assertEquals("   ", edit.uiState.value.name)
        assertEquals(listOf(NEW_NAME), names())
        edit.onNameChange(" 가슴 2 ")
        assertEquals("앞뒤 공백은 떼고 저장한다", listOf("가슴 2"), names())
    }

    @Test
    fun deleteRemovesTheRoutineAndCallsBack() = runTest {
        val keep = routineWith("가슴")
        val gone = routineWith("하체", backSquat.id)
        var closed = 0
        edit(gone).delete { closed++ }

        assertEquals(listOf("가슴"), names())
        assertTrue(repository.itemsOf(gone).isEmpty())
        assertEquals(1, closed)
        assertEquals(0, repository.routines.value.single { it.id == keep }.position)
    }

    @Test
    fun leavingAnUntouchedNewRoutineDiscardsIt() = runTest {
        val id = repository.addRoutine(NEW_NAME)
        var closed = 0
        edit(id, isNew = true).leave { closed++ }

        assertTrue("이름도 종목도 없는 새 루틴은 버린다", names().isEmpty())
        assertEquals(1, closed)
    }

    @Test
    fun leavingANewRoutineWithANameOrExercisesKeepsIt() = runTest {
        val named = repository.addRoutine(NEW_NAME)
        edit(named, isNew = true).apply { onNameChange("어깨") }.leave {}
        val withExercise = repository.addRoutine(NEW_NAME)
        edit(withExercise, isNew = true).apply { addExercises(listOf(pushUp.id)) }.leave {}

        assertEquals(listOf("어깨", NEW_NAME), names())
    }

    @Test
    fun leavingAnExistingRoutineNeverDeletesIt() = runTest {
        val id = repository.addRoutine(NEW_NAME)
        edit(id, isNew = false).leave {}
        assertEquals(listOf(NEW_NAME), names())
    }

    // RTN-02

    @Test
    fun pickedExercisesAreAddedInOrderWithThreeSetsAndNoTarget() = runTest {
        val id = routineWith("가슴", benchPress.id)
        val edit = edit(id)
        edit.addExercises(listOf(pushUp.id, backSquat.id))

        assertEquals(listOf(benchPress.id, pushUp.id, backSquat.id), exerciseIdsOf(id))
        val added = repository.itemsOf(id).drop(1)
        assertTrue(added.all { it.setCount == DEFAULT_SET_COUNT && it.target == Target.NONE })
        assertEquals(listOf(benchPress.id, pushUp.id, backSquat.id), edit.uiState.value.exercises.map { it.exercise.id })
    }

    @Test
    fun exercisesCanBeReordered() = runTest {
        val id = routineWith("전신", benchPress.id, pushUp.id, backSquat.id)
        val ids = repository.itemsOf(id).map { it.id }
        edit(id).reorder(listOf(ids[2], ids[0], ids[1]))

        assertEquals(listOf(backSquat.id, benchPress.id, pushUp.id), exerciseIdsOf(id))
    }

    @Test
    fun removedExerciseComesBackToTheSamePlaceOnUndo() = runTest {
        val id = routineWith("전신", benchPress.id, pushUp.id, backSquat.id)
        val middle = repository.itemsOf(id)[1].id
        var removed: RemovedRoutineExercise? = null
        val settings = exerciseSettings(id, middle)
        settings.onSetCountChange(2)
        settings.remove { removed = it }

        assertEquals(listOf(benchPress.id, backSquat.id), exerciseIdsOf(id))
        val value = checkNotNull(removed)
        assertEquals(pushUp.id, value.exerciseId)
        assertEquals(pushUp.nameKo, value.exerciseName)
        assertEquals(1, value.position)
        assertEquals("뺄 때의 세트 수를 기억한다", 5, value.setCount)

        edit(id).restore(value)
        assertEquals(listOf(benchPress.id, pushUp.id, backSquat.id), exerciseIdsOf(id))
        assertEquals(5, repository.itemsOf(id)[1].setCount)
    }

    // RTN-03, RTN-04

    @Test
    fun targetFieldsFollowTheRecordType() = runTest {
        val id = routineWith("전신", benchPress.id, pushUp.id, plank.id)
        val (weightReps, reps, duration) = repository.itemsOf(id).map { it.id }

        assertEquals(
            listOf(TargetField.WEIGHT, TargetField.REPS_MIN, TargetField.REPS_MAX),
            exerciseSettings(id, weightReps).uiState.value.fields,
        )
        assertEquals(listOf(TargetField.REPS_MIN, TargetField.REPS_MAX), exerciseSettings(id, reps).uiState.value.fields)
        assertEquals(listOf(TargetField.DURATION), exerciseSettings(id, duration).uiState.value.fields)
    }

    @Test
    fun weightAndRepRangeAreTypedWithTheNumberPadAndSaved() = runTest {
        val id = routineWith("가슴", benchPress.id)
        val item = repository.itemsOf(id).single().id
        val vm = exerciseSettings(id, item)

        vm.onFieldClick(TargetField.WEIGHT)
        listOf(6, 2).forEach { vm.onNumberPadKey(NumberPadKey.Digit(it)) }
        vm.onNumberPadKey(NumberPadKey.DecimalPoint)
        vm.onNumberPadKey(NumberPadKey.Digit(5))
        vm.onNumberPadKey(NumberPadKey.Next)
        vm.onNumberPadKey(NumberPadKey.Digit(8))
        vm.onNumberPadKey(NumberPadKey.Next)
        listOf(1, 2).forEach { vm.onNumberPadKey(NumberPadKey.Digit(it)) }
        assertTrue("마지막 칸이라 다음 키가 완료다", vm.uiState.value.numberPad!!.isLastField)
        vm.onNumberPadKey(NumberPadKey.Next)

        assertNull("완료는 숫자판만 닫는다", vm.uiState.value.numberPad)
        assertEquals(Target(weight = Weight(62_500), repsMin = 8, repsMax = 12), repository.itemsOf(id).single().target)
    }

    @Test
    fun durationTargetIsSaved() = runTest {
        val id = routineWith("코어", plank.id)
        val item = repository.itemsOf(id).single().id
        val vm = exerciseSettings(id, item)
        vm.onFieldClick(TargetField.DURATION)
        listOf(1, 3, 0).forEach { vm.onNumberPadKey(NumberPadKey.Digit(it)) }
        vm.onNumberPadKey(NumberPadKey.Close)

        assertNull(vm.uiState.value.numberPad)
        assertEquals(Target(durationSeconds = 90), repository.itemsOf(id).single().target)
    }

    /** 입력 검증의 경계: 세트 수는 1 아래로 내려가지 않는다. */
    @Test
    fun setCountStopsAtOne() = runTest {
        val id = routineWith("가슴", benchPress.id)
        val item = repository.itemsOf(id).single().id
        val vm = exerciseSettings(id, item)
        vm.onSetCountChange(-1)
        vm.onSetCountChange(-1)
        assertEquals(1, repository.itemsOf(id).single().setCount)
        assertFalse(vm.uiState.value.canDecreaseSets)

        vm.onSetCountChange(-1)
        assertEquals("0세트로 내려가지 않는다", 1, repository.itemsOf(id).single().setCount)
        assertEquals(1, vm.uiState.value.setCount)
        vm.onSetCountChange(1)
        assertEquals(2, repository.itemsOf(id).single().setCount)
    }

    @Test
    fun setCountStopsAtTheUpperLimit() = runTest {
        val id = routineWith("가슴", benchPress.id)
        val vm = exerciseSettings(id, repository.itemsOf(id).single().id)
        vm.onSetCountChange(MAX_SET_COUNT + 10)
        assertEquals(MAX_SET_COUNT, repository.itemsOf(id).single().setCount)
        assertFalse(vm.uiState.value.canIncreaseSets)
    }

    /** 입력 검증의 경계: 횟수 하한이 상한보다 크면 저장하지 않고 알린다. 같으면 저장한다. */
    @Test
    fun repRangeWithMinAboveMaxIsNotSaved() = runTest {
        val id = routineWith("등", pushUp.id)
        val vm = exerciseSettings(id, repository.itemsOf(id).single().id)
        vm.onFieldClick(TargetField.REPS_MAX)
        vm.onNumberPadKey(NumberPadKey.Digit(8))
        vm.onFieldClick(TargetField.REPS_MIN)
        vm.onNumberPadKey(NumberPadKey.Digit(9))

        assertTrue(vm.uiState.value.repsRangeInvalid)
        assertEquals("마지막으로 맞았던 목표가 남는다", Target(repsMax = 8), repository.itemsOf(id).single().target)

        vm.onNumberPadKey(NumberPadKey.Backspace)
        vm.onNumberPadKey(NumberPadKey.Digit(8))
        assertFalse(vm.uiState.value.repsRangeInvalid)
        assertEquals("하한과 상한이 같아도 된다", Target(repsMin = 8, repsMax = 8), repository.itemsOf(id).single().target)
    }

    /** 목표를 비운 채 저장할 수 있다(RTN-04). */
    @Test
    fun clearingTheTargetSavesAnEmptyTarget() = runTest {
        val id = repository.addRoutine("가슴")
        val item = repository.addRoutineExercise(id, benchPress.id, 4, Target(weight = Weight.kg(60), repsMin = 8, repsMax = 12))
        val vm = exerciseSettings(id, item)
        TargetField.entries.take(3).forEach { field ->
            vm.onFieldClick(field)
            vm.onNumberPadKey(NumberPadKey.Backspace)
        }

        val saved = repository.itemsOf(id).single()
        assertEquals(Target.NONE, saved.target)
        assertEquals(4, saved.setCount)
    }

    /** 휴식 시간은 종목에 하나 저장된다(REST-03). null이면 기본값을 따른다. */
    @Test
    fun restTimeIsStoredOnTheExercise() = runTest {
        val id = routineWith("가슴", benchPress.id)
        val vm = exerciseSettings(id, repository.itemsOf(id).single().id)
        assertEquals(120, vm.uiState.value.defaultRestSeconds)

        vm.onRestChange(90)
        assertEquals(90, repository.exercises.getExercise(benchPress.id)?.restSeconds)
        assertEquals(90, vm.uiState.value.exercise?.restSeconds)
        vm.onRestChange(null)
        assertNull(repository.exercises.getExercise(benchPress.id)?.restSeconds)
    }

    // RTN-05

    @Test
    fun routineListCanBeReordered() = runTest {
        val a = routineWith("가슴")
        val b = routineWith("등")
        val c = routineWith("하체")
        val list = list()
        list.reorder(listOf(c, a, b))

        assertEquals(listOf("하체", "가슴", "등"), names())
        assertEquals(listOf(0, 1, 2), repository.routines.value.map { it.position })
        assertEquals(listOf(c, a, b), list.uiState.value.routines.map { it.id })
    }

    @Test
    fun movedReturnsNullAtTheEdges() {
        assertEquals(listOf(2, 1, 3), listOf(1, 2, 3).moved(1, -1))
        assertEquals(listOf(1, 3, 2), listOf(1, 2, 3).moved(1, 1))
        assertNull(listOf(1, 2, 3).moved(0, -1))
        assertNull(listOf(1, 2, 3).moved(2, 1))
    }

    private companion object {
        const val NEW_NAME = "새 루틴"
    }
}
