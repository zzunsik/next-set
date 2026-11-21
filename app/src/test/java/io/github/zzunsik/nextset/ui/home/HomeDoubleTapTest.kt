package io.github.zzunsik.nextset.ui.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zzunsik.nextset.data.repository.TestDb
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.at
import io.github.zzunsik.nextset.ui.ViewModelJobs
import io.github.zzunsik.nextset.ui.exercise.MainDispatcherRule
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 홈에서 데이터를 만드는 버튼을 빠르게 두 번 눌러도 한 번만 만든다(#103). 홈이 다시 보이기 전([HomeViewModel.onShown])에 온
 * 두 번째 누르기는 무시한다. 메모리 데이터베이스와 진짜 저장소 구현으로 돌린다.
 */
@RunWith(RobolectricTestRunner::class)
class HomeDoubleTapTest {
    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var db: TestDb
    private var clockMinutes = 600L
    private val store = ViewModelStore()
    private val viewModelJobs = ViewModelJobs()

    @Before
    fun setUp() = runBlocking { db = TestDb().seed(); Unit }

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

    private fun <T> await(flow: Flow<T>, predicate: (T) -> Boolean = { true }): T = runBlocking {
        withContext(Dispatchers.Default) { withTimeout(TIMEOUT) { flow.first(predicate) } }
    }

    private fun HomeViewModel.awaitState(predicate: (HomeUiState) -> Boolean) = await(uiState) { !it.loading && predicate(it) }

    private fun HomeViewModel.nextOpened(): Long = await(openWorkout)

    /** 운동 중 화면을 여는 값이 더 오지 않는다. 오지 않음을 보려고 잠깐 기다린다. */
    private fun HomeViewModel.noMoreOpened(): Long? = runBlocking {
        withContext(Dispatchers.Default) { withTimeoutOrNull(QUIET_MS) { openWorkout.first() } }
    }

    private fun routineNames() = runBlocking { db.routines.observeRoutines().first().map { it.name } }

    private fun inProgressId() = runBlocking { db.workouts.observeInProgressWorkout().first()?.id }

    private fun HomeViewModel.createAndAwait(): Long {
        val created = CompletableDeferred<Long>()
        createRoutine { created.complete(it) }
        return runBlocking { withContext(Dispatchers.Default) { withTimeout(TIMEOUT) { created.await() } } }
    }

    @Test
    fun createRoutineTwiceCreatesOneRoutine() {
        val vm = viewModel()
        vm.awaitState { true }
        val opened = mutableListOf<Long>()
        val first = CompletableDeferred<Unit>()
        vm.createRoutine { opened += it; first.complete(Unit) }
        vm.createRoutine { opened += it }
        runBlocking { withContext(Dispatchers.Default) { withTimeout(TIMEOUT) { first.await() } } }

        // 홈이 다시 보인 뒤에는 다시 만들 수 있다. 쓰기는 부른 순서대로라 두 번째 누르기가 만들었다면 이미 끝나 있다.
        vm.onShown()
        val again = vm.createAndAwait()

        assertEquals("두 번째 누르기는 루틴을 만들지 않는다", listOf(NEW_NAME, NEW_NAME), routineNames())
        assertEquals("화면도 한 번만 연다", 1, opened.size)
        assertEquals(2, (opened + again).toSet().size)
    }

    @Test
    fun startEmptyTwiceStartsOneWorkoutAndOpensItOnce() {
        val vm = viewModel()
        vm.awaitState { true }
        vm.onStartEmpty()
        vm.onStartEmpty()

        val opened = vm.nextOpened()
        assertEquals(opened, inProgressId())
        assertNull("운동 중 화면을 두 번 열지 않는다", vm.noMoreOpened())
        assertNull(vm.uiState.value.dialog)
    }

    /**
     * 홈 화면 상태가 막 시작한 운동(손대지 않은 운동)을 받은 뒤에 두 번째 누르기가 와도, 그 운동을 "손대지 않은 운동"으로 보고
     * 버린 뒤 새로 만들지 않는다.
     */
    @Test
    fun secondTapAfterTheStartedWorkoutArrivesKeepsIt() {
        val routineId = runBlocking { db.routines.addRoutine("가슴").also { db.routines.addRoutineExercise(it, BENCH, 2) } }
        val vm = viewModel()
        vm.awaitState { it.top != null }
        vm.onStartRoutine(routineId)
        val opened = vm.nextOpened()
        vm.awaitState { it.inProgress?.workoutId == opened }

        vm.onStartRoutine(routineId)

        assertNull(vm.noMoreOpened())
        assertEquals("막 시작한 운동이 그대로다", opened, inProgressId())
        assertNull(vm.uiState.value.dialog)
    }

    private companion object {
        const val NEW_NAME = "새 루틴"
        const val TIMEOUT = 5_000L
        const val QUIET_MS = 500L
    }
}
