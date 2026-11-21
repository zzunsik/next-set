package io.github.zzunsik.nextset.ui.routine

import io.github.zzunsik.nextset.ui.exercise.MainDispatcherRule
import io.github.zzunsik.nextset.ui.exercise.TestExercises.benchPress
import io.github.zzunsik.nextset.ui.exercise.TestExercises.pushUp
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 같은 루틴의 루틴 편집이 둘 열렸을 때(#103) 아래에 남은 예전 편집 화면이 위 화면에서 고친 내용을 지우거나 덮어쓰지 않는다.
 * 예전 화면은 처음 읽은 상태(이름 없음, 종목 없음)를 들고 있다.
 */
class RoutineEditStaleEditorTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakeRoutineRepository()

    private fun TestScope.edit(routineId: Long, isNew: Boolean): RoutineEditViewModel =
        RoutineEditViewModel(repository, routineId, isNew, NEW_NAME).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        }

    private fun names() = repository.routines.value.map { it.name }

    @Test
    fun staleEditorDoesNotDiscardANewRoutineNamedInAnotherEditor() = runTest {
        val id = repository.addRoutine(NEW_NAME)
        val stale = edit(id, isNew = true)
        val current = edit(id, isNew = true)

        current.onNameChange("어깨")
        current.leave {}
        var closed = 0
        stale.leave { closed++ }

        assertEquals("다른 편집 화면에서 이름을 붙인 루틴은 남는다", listOf("어깨"), names())
        assertEquals(1, closed)
    }

    @Test
    fun staleEditorDoesNotDiscardANewRoutineWithExercisesAddedInAnotherEditor() = runTest {
        val id = repository.addRoutine(NEW_NAME)
        val stale = edit(id, isNew = true)
        val current = edit(id, isNew = true)

        current.addExercises(listOf(pushUp.id))
        stale.leave {}

        assertEquals(listOf(NEW_NAME), names())
        assertEquals(listOf(pushUp.id), repository.itemsOf(id).map { it.exerciseId })
    }

    /** 예전 화면이 나가도 위 화면에서 더한 종목은 그대로 남는다. */
    @Test
    fun staleEditorKeepsExercisesAddedInAnotherEditor() = runTest {
        val id = repository.addRoutine("가슴")
        repository.addRoutineExercise(id, benchPress.id, 3)
        val stale = edit(id, isNew = false)
        val current = edit(id, isNew = false)

        current.addExercises(listOf(pushUp.id))
        stale.leave {}

        assertEquals(listOf(benchPress.id, pushUp.id), repository.itemsOf(id).map { it.exerciseId })
        assertTrue(names().contains("가슴"))
    }

    private companion object {
        const val NEW_NAME = "새 루틴"
    }
}
