package io.github.zzunsik.nextset.ui.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zzunsik.nextset.data.repository.TestDb
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.at
import io.github.zzunsik.nextset.ui.ViewModelJobs
import io.github.zzunsik.nextset.ui.exercise.MainDispatcherRule
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 운동을 끝내고 홈으로 돌아왔을 때 홈이 끝낸 운동의 이어서 하기 카드를 먼저 보이지 않는다(#101).
 *
 * 홈은 뒤로 가기 스택에 남아 있지만 운동 중 화면과 운동 요약이 위에 있는 동안은 그려지지 않아 화면 상태를 구독하는 곳이 없다.
 * 돌아오면 화면은 화면 상태의 지금 값(`uiState.value`)으로 첫 프레임을 그린다(`collectAsStateWithLifecycle`). 그러므로 홈이
 * 화면에 없는 동안 운동을 끝내도 지금 값이 끝낸 뒤의 상태를 따라가야 한다. 기기에서는 운동이 몇 초 넘게 걸리므로 구독이 없어진
 * 뒤 그만큼 시간을 흘린다(테스트 디스패처의 가상 시간).
 */
@RunWith(RobolectricTestRunner::class)
class HomeStaleResumeTest {
    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var db: TestDb
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
                HomeViewModel(db.workouts, db.routines, db.settings, "새 루틴", clock = { at(500) }, zone = ZoneOffset.UTC)
            }
        }
        return viewModelJobs.track(ViewModelProvider.create(store, factory)[HomeViewModel::class])
    }

    private fun <T> db(block: suspend TestDb.() -> T): T = runBlocking { db.block() }

    /**
     * 구독하지 않고 지금 값만 보며 [predicate]를 만족할 때까지 기다린다. 끝내 만족하지 않으면 마지막 값을 [message]와 함께 알린다.
     * 기다리는 것은 데이터베이스 스레드에서 오는 갱신이다.
     */
    private fun HomeViewModel.awaitValue(message: String, predicate: (HomeUiState) -> Boolean): HomeUiState = runBlocking {
        withContext(Dispatchers.Default) {
            try {
                withTimeout(TIMEOUT) {
                    while (!predicate(uiState.value)) delay(POLL_MS)
                    uiState.value
                }
            } catch (e: TimeoutCancellationException) {
                fail("$message. 지금 값: 진행 중=${uiState.value.inProgress}, 맨 위=${uiState.value.top?.routine?.name}")
                error("unreachable")
            }
        }
    }

    @Test
    fun homeDoesNotKeepTheFinishedWorkoutWhileItIsHidden() {
        val a = db { routines.addRoutine("가슴").also { routines.addRoutineExercise(it, BENCH, 2) } }
        val b = db { routines.addRoutine("등").also { routines.addRoutineExercise(it, PULL_UP, 2) } }
        val workoutId = db { workouts.startFromRoutine(a, at(0)) }
        val vm = viewModel()

        // 홈이 화면에 있다: 화면처럼 Main에서 구독하고 이어서 하기 카드가 보일 때까지 기다린다.
        val screen = CoroutineScope(main.dispatcher).launch { vm.uiState.collect {} }
        vm.awaitValue("이어서 하기 카드가 보인다") { it.inProgress?.workoutId == workoutId }

        // 운동 중 화면으로 간다: 홈이 그려지지 않아 구독이 없어지고, 운동하는 동안 시간이 흐른다.
        screen.cancel()
        main.dispatcher.scheduler.advanceTimeBy(HIDDEN_MS)
        main.dispatcher.scheduler.runCurrent()

        // 세트 하나를 완료하고 운동을 끝낸다.
        db {
            val set = workouts.observeWorkout(workoutId).first()!!.exercises.first().sets.first()
            workouts.completeSet(set.id, at(1))
            workouts.finishWorkout(workoutId, at(30))
        }

        // 요약의 "확인"으로 홈에 돌아오면 이 값으로 첫 프레임을 그린다.
        val shown = vm.awaitValue("홈이 화면에 없는 동안 끝낸 운동을 따라가지 않아 돌아오면 끝낸 운동의 카드를 먼저 보인다") {
            !it.loading && it.inProgress == null
        }
        assertNull("끝낸 운동의 이어서 하기 카드가 없다", shown.inProgress)
        assertEquals("맨 위는 다음 차례다(HOME-02)", b, shown.top?.routine?.id)
    }

    private companion object {
        const val TIMEOUT = 5_000L
        const val POLL_MS = 10L

        /** 홈이 운동 중 화면과 운동 요약 뒤에 있는 시간. 실제 운동보다 짧지만 구독을 멈추는 기다림(5초)보다 길다. */
        const val HIDDEN_MS = 60_000L
    }
}
