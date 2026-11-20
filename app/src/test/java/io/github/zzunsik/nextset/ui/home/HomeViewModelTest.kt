package io.github.zzunsik.nextset.ui.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zzunsik.nextset.data.repository.TestDb
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.TestExercises.SQUAT
import io.github.zzunsik.nextset.data.repository.at
import io.github.zzunsik.nextset.data.repository.wr
import io.github.zzunsik.nextset.ui.ViewModelJobs
import io.github.zzunsik.nextset.ui.exercise.MainDispatcherRule
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 홈의 화면 상태(HOME-01~HOME-06, RTN-01, RTN-05). 메모리 데이터베이스와 진짜 저장소 구현으로 돌린다.
 * 저장은 데이터베이스 스레드에서 끝나므로 상태가 바뀔 때까지 기다린 뒤 견준다([awaitState]).
 */
@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var db: TestDb
    private var clockMinutes = 600L

    /** 화면 상태를 담는 곳. 데이터베이스를 닫기 전에 비워 화면 상태의 구독을 끝낸다. */
    private val store = ViewModelStore()

    @Before
    fun setUp() = runBlocking { db = TestDb().seed(); Unit }

    private val viewModelJobs = ViewModelJobs()

    @After
    fun tearDown() {
        viewModelJobs.clearAndAwait(store)
        db.close()
    }

    private fun viewModel(): HomeViewModel {
        val factory = viewModelFactory {
            initializer {
                HomeViewModel(db.workouts, db.routines, db.settings, NEW_NAME, clock = { at(clockMinutes++) }, zone = ZoneOffset.UTC)
            }
        }
        return viewModelJobs.track(ViewModelProvider.create(store, factory)[HomeViewModel::class])
    }

    private fun HomeViewModel.awaitState(predicate: (HomeUiState) -> Boolean): HomeUiState = await(uiState) { !it.loading && predicate(it) }

    /**
     * 물음(dialog)을 띄우거나 닫은 뒤 화면 상태에 들어올 때까지 기다린다. 물음은 바로 바뀌지만 화면 상태는 저장소의 흐름과
     * 합쳐 만들므로 늦게 따라올 수 있다. 기다리지 않고 `uiState.value`를 읽으면 이전 상태를 보고 실패했다(#75).
     */
    private fun HomeViewModel.awaitDialog(shown: Boolean) {
        awaitState { (it.dialog != null) == shown }
    }

    private fun <T> await(flow: Flow<T>, predicate: (T) -> Boolean = { true }): T = runBlocking {
        withContext(Dispatchers.Default) { withTimeout(TIMEOUT) { flow.first(predicate) } }
    }

    private fun HomeViewModel.nextOpened(): Long = await(openWorkout)

    private fun <T> db(block: suspend TestDb.() -> T): T = runBlocking { db.block() }

    private fun inProgress() = db { workouts.observeInProgressWorkout().first() }

    private fun routine(name: String, vararg exerciseIds: String): Long = db {
        val id = routines.addRoutine(name)
        exerciseIds.forEach { routines.addRoutineExercise(id, it, 2) }
        id
    }

    /** [routineId]로 운동해 세트 하나를 완료하고 끝낸다. 시작은 [minutes]분이다. */
    private fun finishWith(routineId: Long, minutes: Long) = db {
        val id = workouts.startFromRoutine(routineId, at(minutes))
        val set = workouts.observeWorkout(id).first()!!.exercises.first().sets.first()
        workouts.completeSet(set.id, at(minutes + 1))
        workouts.finishWorkout(id, at(minutes + 30))
    }

    /** [routineId]로 운동을 시작해 값을 하나 바꾼다. 손댄 운동이라 홈이 묻는다(#73). */
    private suspend fun TestDb.touchedWorkout(routineId: Long): Long {
        val id = workouts.startFromRoutine(routineId, at(0))
        val set = workouts.observeWorkout(id).first()!!.exercises.first().sets.first()
        workouts.updateSet(set.id, wr(77_500, 3))
        return id
    }

    private fun HomeUiState.otherNames() = others.map { it.routine.name }

    // HOME-01

    @Test
    fun startingTheTopRoutineCreatesAWorkoutAndOpensIt() {
        val chest = routine("가슴", BENCH, PULL_UP)
        val vm = viewModel()
        val state = vm.awaitState { it.top != null }
        assertEquals(chest, state.top?.routine?.id)

        vm.onStartRoutine(chest)
        val opened = vm.nextOpened()
        val workout = inProgress()!!
        assertEquals(workout.id, opened)
        assertEquals(chest, workout.routineId)
        assertEquals("물음 없이 바로 시작한다", null, vm.uiState.value.dialog)
    }

    // HOME-02

    @Test
    fun topIsTheRoutineAfterTheLastOneAndWrapsAround() {
        val a = routine("가슴", BENCH)
        val b = routine("등", PULL_UP)
        val c = routine("하체", SQUAT)
        val vm = viewModel()
        assertEquals("한 적이 없으면 첫 루틴", a, vm.awaitState { it.top != null }.top?.routine?.id)

        finishWith(a, 0)
        val afterA = vm.awaitState { it.top?.routine?.id == b }
        assertEquals("맨 위 카드의 루틴은 목록에서 뺀다(HOME-04)", listOf("가슴", "하체"), afterA.otherNames())

        finishWith(c, 100)
        assertEquals("마지막 루틴 다음은 첫 루틴", a, vm.awaitState { it.top?.routine?.id == a }.top?.routine?.id)
    }

    @Test
    fun emptyWorkoutsDoNotMoveTheOrder() {
        val a = routine("가슴", BENCH)
        val b = routine("등", PULL_UP)
        finishWith(a, 0)
        db {
            val id = workouts.startEmpty(at(100))
            val we = workouts.addExercise(id, SQUAT)
            val set = workouts.observeWorkout(id).first()!!.exercises.single { it.id == we }.sets.first()
            workouts.completeSet(set.id, at(101))
            workouts.finishWorkout(id, at(130))
        }
        val vm = viewModel()
        assertEquals(b, vm.awaitState { it.top != null }.top?.routine?.id)
    }

    // HOME-03

    @Test
    fun pinnedRoutineGoesOnTopUntilItIsDoneOrUnpinned() {
        val a = routine("가슴", BENCH)
        val b = routine("등", PULL_UP)
        val c = routine("하체", SQUAT)
        val vm = viewModel()
        vm.awaitState { it.top?.routine?.id == a }

        vm.onPin(c)
        val pinned = vm.awaitState { it.topPinned && it.top?.routine?.id == c }
        assertEquals(c, pinned.top?.routine?.id)
        assertEquals(listOf("가슴", "등"), pinned.otherNames())

        // 다른 루틴을 마쳐도 풀리지 않는다.
        finishWith(a, 0)
        assertEquals(c, vm.awaitState { it.topPinned && it.top?.routine?.id == c }.top?.routine?.id)

        vm.onUnpin()
        val auto = vm.awaitState { !it.topPinned && it.top?.routine?.id == b }
        assertEquals("자동 순서로 돌아가면 다음 차례", b, auto.top?.routine?.id)

        // 고른 루틴으로 운동을 마치면 풀린다.
        vm.onPin(c)
        vm.awaitState { it.topPinned }
        finishWith(c, 200)
        val done = vm.awaitState { !it.topPinned && it.top?.routine?.id != c }
        assertEquals("방금 한 루틴의 다음 순서", a, done.top?.routine?.id)
    }

    // HOME-04: 목록의 루틴도 한 번 눌러 시작한다.

    @Test
    fun startingARoutineFromTheList() {
        routine("가슴", BENCH)
        val b = routine("등", PULL_UP)
        val vm = viewModel()
        vm.awaitState { it.others.isNotEmpty() }

        vm.onStartRoutine(b)
        assertEquals(inProgress()?.id, vm.nextOpened())
        assertEquals(b, inProgress()?.routineId)
    }

    @Test
    fun routinesCarryExerciseNamesAndLastDate() {
        val a = routine("가슴", BENCH, PULL_UP)
        finishWith(a, 0)
        val vm = viewModel()
        val chest = vm.awaitState { s -> s.routines.any { it.lastDoneOn != null } }.routines.single()
        assertEquals(listOf("테스트 벤치", "테스트 풀업"), chest.exerciseNames)
        assertEquals(LocalDate.of(2026, 1, 1), chest.lastDoneOn)
    }

    // HOME-05

    @Test
    fun startingAnEmptyWorkout() {
        val vm = viewModel()
        vm.awaitState { true }
        vm.onStartEmpty()
        val opened = vm.nextOpened()
        val workout = inProgress()!!
        assertEquals(workout.id, opened)
        assertNull(workout.routineId)
    }

    // HOME-06

    @Test
    fun inProgressWorkoutIsShownAndAllRoutinesStayInTheList() {
        val a = routine("가슴", BENCH)
        routine("등", PULL_UP)
        val workoutId = db { workouts.startFromRoutine(a, at(0)) }
        val vm = viewModel()
        val state = vm.awaitState { it.inProgress != null }
        assertEquals(workoutId, state.inProgress?.workoutId)
        assertEquals("가슴", state.inProgress?.routineName)
        assertEquals(2, state.inProgress?.totalSets)
        assertEquals(listOf("가슴", "등"), state.otherNames())

        vm.onResume(workoutId)
        assertEquals(workoutId, vm.nextOpened())
    }

    // HOME-01의 예외: 진행 중인 운동이 있을 때 다른 루틴을 시작하려 하면 묻는다.

    @Test
    fun startingAnotherRoutineWhileInProgressAsksAndCanResume() {
        val a = routine("가슴", BENCH)
        val b = routine("등", PULL_UP)
        val workoutId = db { touchedWorkout(a) }
        val vm = viewModel()
        vm.awaitState { it.inProgress != null }

        vm.onStartRoutine(b)
        vm.awaitDialog(shown = true)
        val dialog = vm.uiState.value.dialog as HomeDialog.InProgress
        assertEquals(PendingStart.FromRoutine(b), dialog.pending)
        assertEquals("새 운동은 아직 시작하지 않는다", workoutId, inProgress()?.id)

        vm.onPromptResume()
        assertEquals(workoutId, vm.nextOpened())
        vm.awaitDialog(shown = false)
        assertNull(vm.uiState.value.dialog)
    }

    @Test
    fun finishAndStartSavesCompletedSetsAndStartsTheNewWorkout() {
        val a = routine("가슴", BENCH)
        val b = routine("등", PULL_UP)
        val oldId = db {
            val id = workouts.startFromRoutine(a, at(0))
            val set = workouts.observeWorkout(id).first()!!.exercises.first().sets.first()
            workouts.completeSet(set.id, at(1))
            id
        }
        val vm = viewModel()
        vm.awaitState { it.inProgress != null }

        vm.onStartEmpty()
        vm.awaitDialog(shown = true)
        assertEquals(PendingStart.Empty, (vm.uiState.value.dialog as HomeDialog.InProgress).pending)
        vm.onStartRoutine(b)
        vm.onPromptFinishAndStart()
        val opened = vm.nextOpened()
        assertNotEquals(oldId, opened)
        assertEquals(b, inProgress()?.routineId)
        val old = db { workouts.observeWorkout(oldId).first()!! }
        assertNotNull("하던 운동은 끝난 기록으로 남는다", old.finishedAt)
        assertEquals("완료한 세트만 남는다", 1, old.exercises.single().sets.size)
    }

    @Test
    fun cancelLeavesEverythingAsItWas() {
        val a = routine("가슴", BENCH)
        val workoutId = db { touchedWorkout(a) }
        val vm = viewModel()
        vm.awaitState { it.inProgress != null }
        vm.onStartEmpty()
        vm.awaitDialog(shown = true)
        vm.dismissDialog()
        vm.awaitDialog(shown = false)
        assertNull(vm.uiState.value.dialog)
        assertEquals(workoutId, inProgress()?.id)
    }

    // RTN-01

    @Test
    fun newRoutineIsSavedAtTheEndWithTheDefaultName() {
        routine("가슴", BENCH)
        val vm = viewModel()
        vm.awaitState { it.routines.isNotEmpty() }
        var opened: Long? = null
        vm.createRoutine { opened = it }

        val state = vm.awaitState { it.routines.size == 2 }
        assertEquals(listOf("가슴", NEW_NAME), state.routines.map { it.routine.name })
        assertEquals(state.routines.last().routine.id, opened)
    }

    @Test
    fun deletingARoutineKeepsTheHistoryAndClearsThePin() {
        val a = routine("가슴", BENCH)
        val b = routine("등", PULL_UP)
        finishWith(b, 0)
        val vm = viewModel()
        vm.onPin(b)
        vm.awaitState { it.topPinned && it.routines.size == 2 }

        vm.onDeleteRequest(b)
        vm.awaitDialog(shown = true)
        assertEquals(HomeDialog.DeleteRoutine(b, "등"), vm.uiState.value.dialog)
        vm.onDeleteConfirm()
        val state = vm.awaitState { it.routines.size == 1 && it.top?.routine?.id == a }
        assertFalse(state.topPinned)
        assertEquals(a, state.top?.routine?.id)
        assertEquals(1, db { workouts.observeHistory().first() }.size)
    }

    // RTN-05

    @Test
    fun routineListCanBeReordered() {
        val a = routine("가슴", BENCH)
        val b = routine("등", PULL_UP)
        val c = routine("하체", SQUAT)
        val vm = viewModel()
        vm.awaitState { it.routines.size == 3 }

        vm.reorder(listOf(c, a, b))
        val state = vm.awaitState { s -> s.routines.map { it.routine.id } == listOf(c, a, b) && s.top?.routine?.id == c }
        assertEquals(listOf(0, 1, 2), state.routines.map { it.routine.position })
        assertTrue("한 적이 없으면 순서상 첫 루틴이 다음 차례", state.top?.routine?.id == c)
    }

    private companion object {
        const val NEW_NAME = "새 루틴"
        const val TIMEOUT = 5_000L
    }
}
