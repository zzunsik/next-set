package io.github.zzunsik.nextset.ui.workout

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zzunsik.nextset.data.repository.TestDb
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.at
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.ui.exercise.MainDispatcherRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 운동 중 종목 머리의 "휴식 2:00"으로 종목의 휴식 시간을 바꾸고(REST-03), 완료 신호가 그 시간을 싣는지 본다.
 * 메모리 데이터베이스와 진짜 저장소로 돌린다.
 */
@RunWith(RobolectricTestRunner::class)
class WorkoutRestTimeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var db: TestDb
    private val store = ViewModelStore()

    @Before
    fun setUp() = runBlocking { db = TestDb().seed(); Unit }

    @After
    fun tearDown() {
        store.clear()
        db.close()
    }

    private fun viewModel(workoutId: Long): WorkoutViewModel {
        val factory = viewModelFactory {
            initializer {
                WorkoutViewModel(
                    workoutRepository = db.workouts,
                    routineRepository = db.routines,
                    workoutId = workoutId,
                    clock = { at(70) },
                    settingsRepository = db.settings,
                    exerciseRepository = db.exercises,
                )
            }
        }
        return ViewModelProvider.create(store, factory)["vm", WorkoutViewModel::class]
    }

    private fun WorkoutViewModel.awaitState(predicate: (WorkoutUiState) -> Boolean): WorkoutUiState = runBlocking {
        withContext(Dispatchers.Default) { withTimeout(5_000) { uiState.first { !it.loading && predicate(it) } } }
    }

    private fun start(): Long = runBlocking {
        val routine = db.routines.addRoutine("휴식 루틴")
        db.routines.addRoutineExercise(routine, BENCH, 2, Target(weight = Weight.kg(60), repsMin = 8, repsMax = 8))
        db.routines.addRoutineExercise(routine, PULL_UP, 1, Target())
        db.workouts.startFromRoutine(routine, at(60))
    }

    @Test
    fun headerShowsDefaultFromSettingsUntilExerciseHasItsOwn() {
        runBlocking { db.settings.setDefaultRestSeconds(150) }
        val vm = viewModel(start())
        val state = vm.awaitState { it.exercises.size == 2 && it.defaultRestSeconds == 150 }
        assertEquals(150, state.exercises[0].restSecondsOr(state.defaultRestSeconds))

        vm.onRestTimeRequest(state.exercises[0].id)
        val dialog = vm.awaitState { it.dialog != null }.dialog
        assertEquals(WorkoutDialog.RestTime(BENCH, state.exercises[0].name, null), dialog)

        vm.onRestTimeChange(BENCH, 90)
        val changed = vm.awaitState { it.exercises[0].restSeconds == 90 && it.dialog == null }
        assertEquals(90, changed.exercises[0].restSecondsOr(changed.defaultRestSeconds))
        assertEquals("다른 종목은 기본값 그대로", 150, changed.exercises[1].restSecondsOr(changed.defaultRestSeconds))
        assertEquals(90, runBlocking { db.exercises.getExercise(BENCH)!!.restSeconds })

        // 완료 신호가 종목의 시간을 싣는다.
        vm.onCheck(changed.exercises[0].sets[0].id)
        val event = runBlocking { withContext(Dispatchers.Default) { withTimeout(5_000) { vm.events.first() } } }
        assertEquals(90, (event as WorkoutEvent.SetCompleted).restSeconds)

        // "기본값 따름"으로 되돌린다.
        vm.onRestTimeChange(BENCH, null)
        vm.awaitState { it.exercises[0].restSeconds == null }
    }
}
