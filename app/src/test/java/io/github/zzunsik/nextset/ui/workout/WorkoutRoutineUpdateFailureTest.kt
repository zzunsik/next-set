package io.github.zzunsik.nextset.ui.workout

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import io.github.zzunsik.nextset.data.repository.TestDb
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.at
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.ui.ViewModelJobs
import io.github.zzunsik.nextset.ui.exercise.MainDispatcherRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 끝내기에서 루틴 반영(LOG-10)이 실패했을 때(#80). 반영 도중의 쓰기를 SQLite 트리거로 실패시킨다(#72의
 * `RoutineUpdateTransactionTest`와 같은 방법). 운동은 진행 중인 채 남아 기록을 잃지 않고, 화면은
 * "루틴에 반영하지 못했어요." 대화상자로 알린 뒤 다시 시도하거나 반영하지 않고 끝내게 한다.
 */
@RunWith(RobolectricTestRunner::class)
class WorkoutRoutineUpdateFailureTest {
    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var db: TestDb
    private var clockMinutes = 60L
    private val store = ViewModelStore()
    private val viewModelJobs = ViewModelJobs()
    private var routineId = 0L
    private var workoutId = 0L

    @Before
    fun setUp() = runBlocking {
        db = TestDb().seed()
        routineId = db.routines.addRoutine("테스트 루틴")
        db.routines.addRoutineExercise(routineId, BENCH, 3, Target(weight = Weight.kg(60), repsMin = 8, repsMax = 12))
        db.routines.addRoutineExercise(routineId, PULL_UP, 2)
        workoutId = db.workouts.startFromRoutine(routineId, at(60))
    }

    @After
    fun tearDown() {
        viewModelJobs.clearAndAwait(store)
        db.close()
    }

    @Test
    fun failedRoutineUpdateKeepsTheWorkoutAndSaysSo() {
        val vm = askAfterRemovingPullUp()
        failOn("BEFORE DELETE ON routine_exercise")

        vm.onRoutineChangeAnswer(apply = true)

        assertEquals(WorkoutDialog.RoutineUpdateFailed, vm.awaitState { it.dialog == WorkoutDialog.RoutineUpdateFailed }.dialog)
        assertNull("화면을 떠나지 않는다", vm.eventOrNull())
        val workout = stored()
        assertNull("운동은 끝나지 않고 진행 중이다", workout.finishedAt)
        assertEquals("완료한 세트가 그대로 남는다", 3, workout.exercises.single().sets.count { it.isCompleted })
        assertEquals("루틴은 반영 전과 같다", listOf(BENCH to 3, PULL_UP to 2), routineItems())
    }

    @Test
    fun retryAfterAFailureAppliesAndFinishes() {
        val vm = askAfterRemovingPullUp()
        failOn("BEFORE DELETE ON routine_exercise")
        vm.onRoutineChangeAnswer(apply = true)
        vm.awaitState { it.dialog == WorkoutDialog.RoutineUpdateFailed }
        exec("DROP TRIGGER fail_step")

        vm.onRoutineChangeAnswer(apply = true) // "다시 시도"

        assertEquals(WorkoutEvent.Exit(WorkoutExit.Finished(workoutId)), vm.nextEvent())
        assertEquals(listOf(BENCH to 3), routineItems())
        assertNotNull(stored().finishedAt)
    }

    @Test
    fun finishingWithoutTheRoutineAfterAFailureKeepsTheRecord() {
        val vm = askAfterRemovingPullUp()
        failOn("BEFORE DELETE ON routine_exercise")
        vm.onRoutineChangeAnswer(apply = true)
        vm.awaitState { it.dialog == WorkoutDialog.RoutineUpdateFailed }

        vm.onRoutineChangeAnswer(apply = false) // "이번만 끝내기"

        assertEquals(WorkoutEvent.Exit(WorkoutExit.Finished(workoutId)), vm.nextEvent())
        val finished = stored()
        assertNotNull(finished.finishedAt)
        assertEquals("완료한 세트가 기록에 남는다", 3, finished.exercises.single().sets.size)
        assertEquals("루틴은 그대로다", listOf(BENCH to 3, PULL_UP to 2), routineItems())
    }

    @Test
    fun dismissingTheFailureStaysInTheWorkout() {
        val vm = askAfterRemovingPullUp()
        failOn("BEFORE DELETE ON routine_exercise")
        vm.onRoutineChangeAnswer(apply = true)
        vm.awaitState { it.dialog == WorkoutDialog.RoutineUpdateFailed }

        vm.dismissDialog()

        assertNull(vm.awaitState { it.dialog == null }.dialog)
        assertNull(vm.eventOrNull())
        assertNull(stored().finishedAt)
    }

    /** 풀업을 빼고 벤치 세 세트를 모두 완료한 뒤 끝내기를 눌러 루틴 반영 물음까지 간다. */
    private fun askAfterRemovingPullUp(): WorkoutViewModel {
        val vm = viewModel()
        val state = vm.awaitState { it.exercises.size == 2 }
        vm.onRemoveExerciseConfirm(state.exercises[1].id)
        vm.awaitState { it.exercises.size == 1 }
        state.exercises[0].sets.forEach { set ->
            vm.onCheck(set.id)
            vm.awaitState { s -> s.exercises[0].sets.first { it.id == set.id }.completed }
        }
        // 세트 완료 알림(휴식 타이머 신호)을 비운다.
        repeat(3) { vm.nextEvent() }
        vm.onFinishRequest()
        vm.awaitState { it.dialog == WorkoutDialog.RoutineChange }
        return vm
    }

    private fun viewModel(): WorkoutViewModel {
        val factory = viewModelFactory {
            initializer { WorkoutViewModel(db.workouts, db.routines, workoutId, clock = { at(clockMinutes++) }) }
        }
        return viewModelJobs.track(ViewModelProvider.create(store, factory)["vm", WorkoutViewModel::class])
    }

    private fun WorkoutViewModel.awaitState(predicate: (WorkoutUiState) -> Boolean): WorkoutUiState = runBlocking {
        withContext(Dispatchers.Default) { withTimeout(TIMEOUT) { uiState.first { !it.loading && predicate(it) } } }
    }

    private fun WorkoutViewModel.nextEvent(): WorkoutEvent = runBlocking {
        withContext(Dispatchers.Default) { withTimeout(TIMEOUT) { events.first() } }
    }

    private fun WorkoutViewModel.eventOrNull(): WorkoutEvent? = runBlocking {
        withContext(Dispatchers.Default) { withTimeoutOrNull(300) { events.first() } }
    }

    private fun stored() = runBlocking { db.workouts.observeWorkout(workoutId).first()!! }

    private fun routineItems() = runBlocking {
        db.routines.observeRoutine(routineId).first()!!.exercises.map { it.exercise.id to it.setCount }
    }

    /** [event]에서 쓰기를 실패시키는 트리거를 건다. 지어낸 실패다. */
    private fun failOn(event: String) =
        exec("CREATE TRIGGER fail_step $event BEGIN SELECT RAISE(ABORT, 'injected failure'); END")

    private fun exec(sql: String) = runBlocking { db.database.useWriterConnection { it.executeSQL(sql) } }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
