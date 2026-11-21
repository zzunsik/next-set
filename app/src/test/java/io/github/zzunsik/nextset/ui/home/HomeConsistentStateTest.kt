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
import io.github.zzunsik.nextset.domain.model.Routine
import io.github.zzunsik.nextset.domain.model.Workout
import io.github.zzunsik.nextset.domain.repository.RoutineRepository
import io.github.zzunsik.nextset.domain.repository.WorkoutRepository
import io.github.zzunsik.nextset.ui.ViewModelJobs
import io.github.zzunsik.nextset.ui.exercise.MainDispatcherRule
import java.time.ZoneOffset
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 운동을 끝낸 직후 홈이 서로 어긋난 조합을 내보내지 않는다(#97).
 *
 * 저장소의 흐름은 같은 쓰기 뒤에도 흐름마다 따로 다시 읽어 내보내므로 어느 쪽이 먼저 올지 정해져 있지 않다(#94, PR #96).
 * 기기에서는 CPU가 바쁠 때 한 프레임쯤 어긋났다. 여기서는 그 순서를 문(gate)으로 고정한다. 진행 중인 운동이나 맨 위
 * 루틴의 흐름 하나만 다시 읽은 값을 내보내지 못하게 잡아 두고 운동을 끝낸다. 화면 상태가 한 시점의 읽기로 만들어지면
 * 흐름 하나가 늦어도 어긋난 조합이 생기지 않는다.
 *
 * 맞는 조합: 루틴 a로 운동하는 동안은 진행 중인 운동이 있고 맨 위가 a다. 끝낸 뒤에는 진행 중인 운동이 없고 맨 위가 b다.
 */
@RunWith(RobolectricTestRunner::class)
class HomeConsistentStateTest {
    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var db: TestDb
    private val store = ViewModelStore()
    private val viewModelJobs = ViewModelJobs()
    private val recorder = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 진행 중인 운동의 흐름을 막는 문. 닫혀 있으면 다시 읽은 값을 내보내지 않고 기다린다. */
    private val inProgressOpen = MutableStateFlow(true)

    /** 맨 위 루틴의 흐름을 막는 문. */
    private val topOpen = MutableStateFlow(true)

    @Before
    fun setUp() = runBlocking { db = TestDb().seed(); Unit }

    @After
    fun tearDown() {
        inProgressOpen.value = true
        topOpen.value = true
        recorder.cancel()
        viewModelJobs.clearAndAwait(store)
        db.close()
    }

    /** 진짜 저장소에 문만 끼운 것. 나머지는 그대로 넘긴다. */
    private class GatedWorkouts(private val delegate: WorkoutRepository, private val open: MutableStateFlow<Boolean>) :
        WorkoutRepository by delegate {
        override fun observeInProgressWorkout(): Flow<Workout?> =
            delegate.observeInProgressWorkout().onEach { open.first { it } }
    }

    private class GatedRoutines(private val delegate: RoutineRepository, private val open: MutableStateFlow<Boolean>) :
        RoutineRepository by delegate {
        override fun observeTopRoutine(): Flow<Routine?> = delegate.observeTopRoutine().onEach { open.first { it } }
    }

    private fun viewModel(): HomeViewModel {
        val factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    GatedWorkouts(db.workouts, inProgressOpen),
                    GatedRoutines(db.routines, topOpen),
                    db.settings,
                    "새 루틴",
                    clock = { at(500) },
                    zone = ZoneOffset.UTC,
                )
            }
        }
        return viewModelJobs.track(ViewModelProvider.create(store, factory)[HomeViewModel::class])
    }

    private fun <T> await(flow: Flow<T>, predicate: (T) -> Boolean): T = runBlocking {
        withContext(Dispatchers.Default) { withTimeout(TIMEOUT) { flow.first(predicate) } }
    }

    private fun HomeViewModel.awaitState(predicate: (HomeUiState) -> Boolean): HomeUiState =
        await(uiState) { !it.loading && predicate(it) }

    private fun <T> db(block: suspend TestDb.() -> T): T = runBlocking { db.block() }

    private fun routine(name: String, exerciseId: String): Long = db {
        routines.addRoutine(name).also { routines.addRoutineExercise(it, exerciseId, 2) }
    }

    /** 화면 상태가 내보내는 값을 모두 모은다. */
    private fun HomeViewModel.record(): List<HomeUiState> {
        val states = Collections.synchronizedList(mutableListOf<HomeUiState>())
        recorder.launch { uiState.collect { states += it } }
        return states
    }

    private class Scenario(val a: Long, val b: Long, val workoutId: Long, val vm: HomeViewModel)

    /** 루틴 a, b, c를 만들고 a로 운동을 시작해 세트 하나를 완료한 데까지. 화면 상태가 그것을 보일 때까지 기다린다. */
    private fun startWorkoutWithA(): Scenario {
        val a = routine("가슴", BENCH)
        val b = routine("등", PULL_UP)
        routine("하체", SQUAT)
        val vm = viewModel()
        vm.awaitState { it.top?.routine?.id == a }
        val workoutId = db {
            val id = workouts.startFromRoutine(a, at(0))
            val set = workouts.observeWorkout(id).first()!!.exercises.first().sets.first()
            workouts.completeSet(set.id, at(1))
            id
        }
        vm.awaitState { it.inProgress?.completedSets == 1 && it.top?.routine?.id == a }
        return Scenario(a, b, workoutId, vm)
    }

    private fun assertConsistent(states: List<HomeUiState>, a: Long, b: Long) {
        val snapshot = synchronized(states) { states.toList() }
        val wrong = snapshot.filter { state ->
            val topId = state.top?.routine?.id
            val running = state.inProgress != null
            (running && topId != a) || (!running && topId != b)
        }
        assertTrue(
            "어긋난 조합을 내보냈다(진행 중=${wrong.map { it.inProgress != null }}, 맨 위=${wrong.map { it.top?.routine?.name }}, " +
                "목록=${wrong.map { s -> s.others.map { it.routine.name } }})",
            wrong.isEmpty(),
        )
    }

    @Test
    fun finishingDoesNotShowTheNextRoutineWhileTheWorkoutStillLooksInProgress() {
        val s = startWorkoutWithA()
        val states = s.vm.record()
        s.vm.awaitState { true }

        inProgressOpen.value = false
        db { workouts.finishWorkout(s.workoutId, at(30)) }
        // 맨 위 루틴은 막지 않았으므로 b로 바뀐다. 그때 진행 중인 운동이 남아 있으면 b가 목록에도 보인다(HOME-04, HOME-06).
        val advanced = s.vm.awaitState { it.top?.routine?.id == s.b }
        inProgressOpen.value = true
        val settled = s.vm.awaitState { it.top?.routine?.id == s.b && it.inProgress == null }

        assertEquals("맨 위가 b로 바뀐 순간에는 운동이 끝나 있다", null, advanced.inProgress)
        assertEquals("맨 위 카드의 루틴은 목록에서 뺀다(HOME-04)", listOf("가슴", "하체"), settled.others.map { it.routine.name })
        assertConsistent(states, s.a, s.b)
    }

    @Test
    fun finishingDoesNotShowTheOldTopRoutineAfterTheWorkoutIsGone() {
        val s = startWorkoutWithA()
        val states = s.vm.record()
        s.vm.awaitState { true }

        topOpen.value = false
        db { workouts.finishWorkout(s.workoutId, at(30)) }
        // 진행 중인 운동은 막지 않았으므로 사라진다. 그때 맨 위가 아직 a면 방금 한 루틴이 다음 차례로 보인다.
        val finished = s.vm.awaitState { it.inProgress == null }
        topOpen.value = true
        s.vm.awaitState { it.top?.routine?.id == s.b && it.inProgress == null }

        assertEquals("운동이 사라진 순간에는 맨 위가 다음 차례다", s.b, finished.top?.routine?.id)
        assertConsistent(states, s.a, s.b)
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
