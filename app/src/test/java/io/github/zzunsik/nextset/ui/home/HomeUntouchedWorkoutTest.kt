package io.github.zzunsik.nextset.ui.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zzunsik.nextset.data.repository.TestDb
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.at
import io.github.zzunsik.nextset.ui.exercise.MainDispatcherRule
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 홈의 "진행 중인 운동이 있어요." 물음은 손댄 운동일 때만 뜬다(docs/DESIGN.md, #73). 손대지 않은 운동은 묻지 않고 버리고
 * 새로 시작한다. 손댄 운동일 때 묻는 것은 HomeViewModelTest가 본다. 메모리 데이터베이스와 진짜 저장소 구현으로 돌린다.
 */
@RunWith(RobolectricTestRunner::class)
class HomeUntouchedWorkoutTest {
    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var db: TestDb
    private var clockMinutes = 600L
    private val store = ViewModelStore()

    @Before
    fun setUp() = runBlocking { db = TestDb().seed(); Unit }

    @After
    fun tearDown() {
        store.clear()
        db.close()
    }

    private fun viewModel(): HomeViewModel {
        val factory = viewModelFactory {
            initializer {
                HomeViewModel(db.workouts, db.routines, db.settings, "새 루틴", clock = { at(clockMinutes++) }, zone = ZoneOffset.UTC)
            }
        }
        return ViewModelProvider.create(store, factory)[HomeViewModel::class]
    }

    private fun <T> await(flow: Flow<T>, predicate: (T) -> Boolean = { true }): T = runBlocking {
        withContext(Dispatchers.Default) { withTimeout(TIMEOUT) { flow.first(predicate) } }
    }

    private fun <T> db(block: suspend TestDb.() -> T): T = runBlocking { db.block() }

    private fun routine(name: String, exerciseId: String): Long = db {
        routines.addRoutine(name).also { routines.addRoutineExercise(it, exerciseId, 2) }
    }

    @Test
    fun startingAnotherRoutineOverAnUntouchedWorkoutDiscardsItWithoutAsking() {
        val a = routine("가슴", BENCH)
        val b = routine("등", PULL_UP)
        val oldId = db { workouts.startFromRoutine(a, at(0)) }
        val vm = viewModel()
        val state = await(vm.uiState) { !it.loading && it.inProgress != null }
        assertTrue(state.inProgress!!.untouched)

        vm.onStartRoutine(b)
        val opened = await(vm.openWorkout)
        assertNull("묻지 않는다", vm.uiState.value.dialog)
        assertNotEquals(oldId, opened)
        assertEquals(b, db { workouts.observeInProgressWorkout().first() }?.routineId)
        assertNull("손대지 않은 운동은 기록 없이 버려진다", db { workouts.observeWorkout(oldId).first() })
        assertTrue(db { workouts.observeHistory().first() }.isEmpty())
    }

    @Test
    fun emptyStartOverAnUntouchedEmptyWorkoutAlsoDoesNotAsk() {
        val oldId = db { workouts.startEmpty(at(0)) }
        val vm = viewModel()
        await(vm.uiState) { !it.loading && it.inProgress?.untouched == true }

        vm.onStartEmpty()
        val opened = await(vm.openWorkout)
        assertNull(vm.uiState.value.dialog)
        assertNotEquals(oldId, opened)
        assertNull(db { workouts.observeWorkout(oldId).first() })
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
