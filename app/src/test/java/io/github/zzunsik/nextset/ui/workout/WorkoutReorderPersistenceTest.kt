package io.github.zzunsik.nextset.ui.workout

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zzunsik.nextset.data.db.NextSetDatabase
import io.github.zzunsik.nextset.data.repository.TestDb
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PLANK
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.TestExercises.SQUAT
import io.github.zzunsik.nextset.data.repository.at
import io.github.zzunsik.nextset.ui.exercise.MainDispatcherRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * 종목 순서 바꾸기 대화상자에서 끌어 놓은 순서를 저장한다(#74, NF-09).
 * 파일 데이터베이스를 닫았다 다시 열어 앱이 꺼졌다 켜져도 순서가 남는지 본다.
 */
@RunWith(RobolectricTestRunner::class)
class WorkoutReorderPersistenceTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val context = RuntimeEnvironment.getApplication()

    private fun viewModel(store: ViewModelStore, db: TestDb, workoutId: Long): WorkoutViewModel {
        val factory = viewModelFactory {
            initializer { WorkoutViewModel(db.workouts, db.routines, workoutId, clock = { at(60) }) }
        }
        return ViewModelProvider.create(store, factory)["vm", WorkoutViewModel::class]
    }

    private fun WorkoutViewModel.awaitState(predicate: (WorkoutUiState) -> Boolean): WorkoutUiState = runBlocking {
        withContext(Dispatchers.Default) { withTimeout(TIMEOUT) { uiState.first { !it.loading && predicate(it) } } }
    }

    private fun exerciseOrder(state: WorkoutUiState) = state.exercises.map { it.exerciseId }

    @Test
    fun draggedOrderIsSavedAndSurvivesReopeningTheDatabase() {
        val first = runBlocking { TestDb(NextSetDatabase.build(context)).seed() }
        val store = ViewModelStore()
        val workoutId: Long
        try {
            workoutId = runBlocking {
                val id = first.workouts.startEmpty(at(0))
                listOf(BENCH, PULL_UP, SQUAT, PLANK).forEach { first.workouts.addExercise(id, it) }
                id
            }
            val vm = viewModel(store, first, workoutId)
            val ids = vm.awaitState { it.exercises.size == 4 }.exercises.map { it.id }

            // 플랭크를 맨 위로 끌어 놓았다.
            vm.onReorderExercises(listOf(ids[3], ids[0], ids[1], ids[2]))
            val moved = vm.awaitState { exerciseOrder(it) == listOf(PLANK, BENCH, PULL_UP, SQUAT) }
            assertEquals(listOf(ids[3], ids[0], ids[1], ids[2]), moved.exercises.map { it.id })
        } finally {
            store.clear()
            first.close()
        }

        val reopened = TestDb(NextSetDatabase.build(context))
        try {
            val restored = runBlocking { reopened.workouts.observeInProgressWorkout().first()!! }
            assertEquals(workoutId, restored.id)
            assertEquals(listOf(PLANK, BENCH, PULL_UP, SQUAT), restored.exercises.map { it.exercise.id })
        } finally {
            reopened.close()
        }
    }

    /** 순서가 그대로이거나 지금 종목과 맞지 않는 목록(그 사이에 종목을 뺐다)은 저장하지 않는다. */
    @Test
    fun unchangedOrStaleOrdersAreIgnored() {
        val db = runBlocking { TestDb().seed() }
        val store = ViewModelStore()
        try {
            val workoutId = runBlocking {
                val id = db.workouts.startEmpty(at(0))
                listOf(BENCH, PULL_UP, SQUAT).forEach { db.workouts.addExercise(id, it) }
                id
            }
            val vm = viewModel(store, db, workoutId)
            val ids = vm.awaitState { it.exercises.size == 3 }.exercises.map { it.id }

            vm.onReorderExercises(ids)
            vm.onReorderExercises(listOf(ids[1], ids[0])) // 하나가 빠졌다.
            vm.onReorderExercises(listOf(ids[1], ids[0], ids[2], 9_999L)) // 없는 종목이 있다.
            vm.onReorderExercises(listOf(ids[2], ids[1], ids[0]))

            val state = vm.awaitState { exerciseOrder(it) == listOf(SQUAT, PULL_UP, BENCH) }
            assertEquals(listOf(ids[2], ids[1], ids[0]), state.exercises.map { it.id })
            val stored = runBlocking { db.workouts.observeWorkout(workoutId).first()!! }
            assertEquals(listOf(SQUAT, PULL_UP, BENCH), stored.exercises.map { it.exercise.id })
        } finally {
            store.clear()
            db.close()
        }
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
