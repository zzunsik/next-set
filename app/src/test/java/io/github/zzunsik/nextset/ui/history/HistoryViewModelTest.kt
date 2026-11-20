package io.github.zzunsik.nextset.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zzunsik.nextset.data.repository.TestDb
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PLANK
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.at
import io.github.zzunsik.nextset.data.repository.finishedWorkout
import io.github.zzunsik.nextset.data.repository.reps
import io.github.zzunsik.nextset.data.repository.seconds
import io.github.zzunsik.nextset.data.repository.wr
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.ui.component.NumberFieldKind
import io.github.zzunsik.nextset.ui.component.NumberPadKey
import io.github.zzunsik.nextset.ui.exercise.MainDispatcherRule
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 기록 목록(HIST-01, HIST-06)과 기록 상세·수정(HIST-02, HIST-03, HIST-05)의 상태.
 * 메모리 데이터베이스와 진짜 저장소 구현으로 돌린다. 시간대는 UTC로 고정한다.
 */
@RunWith(RobolectricTestRunner::class)
class HistoryViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var db: TestDb
    private val store = ViewModelStore()
    private var created = 0

    /** 지금 시각. 2026-01-01T00:00Z에서 [nowMinutes]분 뒤. */
    private var nowMinutes = 100_000L

    @Before
    fun setUp() = runBlocking { db = TestDb().seed(); Unit }

    /** 화면이 보는 것처럼 화면 상태를 구독해 둔다. 화면 상태는 구독하는 동안에만 데이터베이스를 읽는다. */
    private val screens = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @After
    fun tearDown() {
        screens.cancel()
        // 화면 상태가 차례로 하던 쓰기(예: 더한 세트의 값 맞추기)가 끝나기를 잠깐 기다린다. 그 사이에 데이터베이스를 닫으면
        // 남은 쓰기가 닫힌 데이터베이스를 읽어 뒤에 도는 다른 테스트 클래스가 UncaughtExceptionsBeforeTest로 실패한다.
        runBlocking { withContext(Dispatchers.Default) { delay(WRITE_DRAIN_MILLIS) } }
        store.clear()
        db.close()
    }

    private inline fun <reified T : ViewModel> create(crossinline make: () -> T): T {
        val factory = viewModelFactory { initializer { make() } }
        return ViewModelProvider.create(store, factory)["vm-${created++}", T::class]
    }

    private fun list() = create { HistoryListViewModel(db.workouts, db.routines, clock = { at(nowMinutes) }, zone = ZoneOffset.UTC) }.also { vm -> screens.launch { vm.uiState.collect {} } }

    private fun report(id: Long, editing: Boolean = false, isNew: Boolean = false) =
        create { WorkoutReportViewModel(db.workouts, id, startEditing = editing, isNew = isNew, zone = ZoneOffset.UTC) }
            .also { vm -> screens.launch { vm.uiState.collect {} } }

    private fun <T> await(flow: Flow<T>, predicate: (T) -> Boolean): T = runBlocking {
        withContext(Dispatchers.Default) { withTimeout(TIMEOUT) { flow.first(predicate) } }
    }

    private fun WorkoutReportViewModel.await(predicate: (WorkoutReportUi) -> Boolean): WorkoutReportUi =
        await(uiState) { !it.loading && it.report != null && predicate(it.report) }.report!!

    private fun workout(id: Long) = runBlocking { db.workouts.observeWorkout(id).first() }

    // ---- 기록 목록 ----

    @Test
    fun listIsNewestFirstGroupedByMonthWithRowValues() {
        val january = runBlocking { db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10)))) }
        // 2월 1일. 무게와 횟수, 횟수만, 시간 종목이 섞이고 완료하지 않은 세트가 하나 있다.
        val february = runBlocking {
            db.finishedWorkout(
                at(31 * 24 * 60),
                mapOf(BENCH to listOf(wr(62_500, 8), wr(62_500, 6)), PULL_UP to listOf(reps(10)), PLANK to listOf(seconds(60))),
                incomplete = setOf(BENCH),
            )
        }
        val state = await(list().uiState) { !it.loading && it.months.isNotEmpty() }

        assertEquals(listOf(YearMonth.of(2026, 2), YearMonth.of(2026, 1)), state.months.map { it.month })
        val row = state.months[0].rows.single()
        assertEquals(february, row.id)
        assertEquals(LocalDate.of(2026, 2, 1), row.date)
        assertEquals(60L, row.stats.minutes)
        assertEquals("완료하지 않은 세트는 세지 않는다", 4, row.stats.setCount)
        assertEquals("횟수만, 시간 종목은 볼륨에 넣지 않는다", 62_500L * 14, row.stats.volumeGrams)
        assertTrue("62.5kg이 60kg을 넘었다", row.hasRecord)
        assertEquals(january, state.months[1].rows.single().id)
        assertFalse("처음 한 종목은 최고 기록이 아니다", state.months[1].rows.single().hasRecord)
    }

    @Test
    fun pastWorkoutIsCreatedOnThePickedDateAndOpensForEditing() {
        runBlocking {
            db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10))))
            db.finishedWorkout(at(20 * 24 * 60), mapOf(BENCH to listOf(wr(70_000, 5))))
        }
        val routineId = runBlocking {
            db.routines.addRoutine("테스트 루틴").also { db.routines.addRoutineExercise(it, BENCH, 2, Target(weight = Weight.kg(50), repsMin = 8)) }
        }
        val vm = list()
        await(vm.uiState) { !it.loading }
        assertEquals(LocalDate.of(2026, 3, 11), at(nowMinutes).atZone(ZoneOffset.UTC).toLocalDate())

        vm.onDatePicked(LocalDate.of(2026, 3, 12))
        assertNull("오늘보다 뒤의 날짜는 받지 않는다", vm.uiState.value.addPast)

        vm.onDatePicked(LocalDate.of(2026, 1, 10))
        val dialog = await(vm.uiState) { it.addPast != null }.addPast!!
        assertEquals(listOf("테스트 루틴"), dialog.routines.map { it.name })
        vm.onAddPast(routineId)
        val id = await(vm.openCreated) { true }

        val created = workout(id)!!
        assertEquals(LocalDate.of(2026, 1, 10), created.startedAt.atZone(ZoneOffset.UTC).toLocalDate())
        assertEquals(Duration.ofHours(1), Duration.between(created.startedAt, created.finishedAt))
        // 직전 기록(1월 1일)과 목표로 채운 세트는 완료로 둔다.
        assertEquals(listOf(wr(60_000, 10), wr(50_000, 8)), created.exercises.single().sets.map { it.value })
        assertTrue(created.exercises.single().sets.all { it.isCompleted })
        // 목록의 제자리(두 기록 사이)에 들어간다.
        val rows = await(vm.uiState) { s -> s.months.sumOf { it.rows.size } == 3 }.months.flatMap { it.rows }
        assertEquals(id, rows[1].id)
    }

    // ---- 기록 상세·수정 ----

    @Test
    fun editingAValueRecomputesRecordsAndStats() {
        val first = runBlocking { db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10)))) }
        val second = runBlocking { db.finishedWorkout(at(1_000), mapOf(BENCH to listOf(wr(62_500, 8)))) }
        val secondView = report(second)
        assertEquals(listOf(true), secondView.await { true }.exercises.single().sets.map { it.isRecord })

        // 앞 기록의 무게를 70kg으로 고치면 뒤 기록의 표시가 사라진다.
        val vm = report(first, editing = true)
        val setId = vm.await { true }.exercises.single().sets.single().id
        vm.onFieldClick(setId, NumberFieldKind.WEIGHT)
        listOf(NumberPadKey.Digit(7), NumberPadKey.Digit(0)).forEach(vm::onNumberPadKey)
        vm.await { it.stats.volumeGrams == 700_000L }
        assertEquals(listOf(false), secondView.await { r -> r.records.isEmpty() }.exercises.single().sets.map { it.isRecord })
    }

    @Test
    fun addAndDeleteSetsAndPruneEmptyOnesWhenDone() {
        val id = runBlocking { db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10)), PULL_UP to listOf(reps(8)))) }
        val vm = report(id, editing = true)
        val bench = vm.await { true }.exercises[0]

        // 더한 세트는 바로 위 행의 값으로 채워지고 완료된다.
        vm.onAddSet(bench.id)
        val added = vm.await { it.exercises[0].sets.size == 2 && it.exercises[0].sets[1].completed }.exercises[0].sets[1]
        assertEquals(wr(60_000, 10), added.value)
        assertTrue(added.completed)
        vm.await { it.stats.setCount == 3 }

        // 칸을 비우면 완료가 풀리고 숫자에서 빠진다.
        vm.onFieldClick(added.id, NumberFieldKind.REPS)
        vm.onNumberPadKey(NumberPadKey.Backspace)
        vm.await { it.stats.setCount == 2 }

        // 지운 세트는 되돌린다.
        val pullUpSet = vm.uiState.value.report!!.exercises[1].sets.single().id
        vm.onDeleteSet(pullUpSet)
        vm.await { it.exercises[1].sets.isEmpty() }
        vm.onUndoDelete()
        vm.await { it.exercises[1].sets.size == 1 }

        // "완료"를 누르면 빈 세트를 지운다.
        vm.onEditToggle()
        await(vm.uiState) { !it.editing }
        val pruned = await(db.workouts.observeWorkout(id)) { w -> w!!.exercises[0].sets.size == 1 }!!
        assertEquals(listOf(1, 1), pruned.exercises.map { it.sets.size })
    }

    @Test
    fun removingAnExerciseAndAddingOne() {
        val id = runBlocking { db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10)), PULL_UP to listOf(reps(8)))) }
        val vm = report(id, editing = true)
        val pullUp = vm.await { true }.exercises[1]
        vm.onRemoveExerciseRequest(pullUp.id)
        assertEquals(ReportDialog.RemoveExercise(pullUp.id, "테스트 풀업"), await(vm.uiState) { it.dialog != null }.dialog)
        vm.onRemoveExerciseConfirm(pullUp.id)
        vm.await { it.exercises.size == 1 }

        vm.onExercisesPicked(listOf(PULL_UP))
        // 직전 기록이 없고 위 행도 없어 값이 빈 세트 하나로 들어온다. 완료하지 않은 상태다.
        val added = vm.await { it.exercises.size == 2 && it.exercises[1].sets.isNotEmpty() }.exercises[1]
        assertEquals(RecordType.REPS, added.recordType)
        assertFalse(added.sets.single().completed)
    }

    @Test
    fun dateAndStartTimeMoveTheWorkoutAndMinutesChangeTheLength() {
        val id = runBlocking { db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10)))) }
        val vm = report(id, editing = true)
        vm.await { true }

        vm.onDateChange(LocalDate.of(2025, 12, 30))
        vm.await { it.startedAt == at(-2 * 24 * 60) }
        vm.onStartTimeChange(LocalTime.of(18, 30))
        val moved = vm.await { it.startedAt.atZone(ZoneOffset.UTC).toLocalTime() == LocalTime.of(18, 30) }
        assertEquals(LocalDate.of(2025, 12, 30), moved.startedAt.atZone(ZoneOffset.UTC).toLocalDate())
        assertEquals("운동 시간은 그대로다", 60L, moved.stats.minutes)

        vm.onMinutesClick()
        listOf(NumberPadKey.Digit(4), NumberPadKey.Digit(5)).forEach(vm::onNumberPadKey)
        assertEquals(45L, vm.await { it.stats.minutes == 45L }.stats.minutes)
    }

    @Test
    fun deletingTheRecordAsksFirst() {
        val id = runBlocking { db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10)))) }
        val vm = report(id)
        vm.await { true }
        vm.onDeleteRequest()
        assertEquals(ReportDialog.Delete, await(vm.uiState) { it.dialog != null }.dialog)
        vm.dismissDialog()
        assertNull(await(vm.uiState) { it.dialog == null }.dialog)
        assertTrue(workout(id) != null)

        vm.onDeleteRequest()
        vm.onDeleteConfirm()
        assertEquals(ReportEvent.Deleted, await(vm.events) { true })
        assertNull(workout(id))
    }

    @Test
    fun newPastWorkoutLeftWithoutSetsIsDiscarded() {
        val id = runBlocking { db.workouts.addPastWorkout(at(0), at(60)) }
        val vm = report(id, editing = true, isNew = true)
        vm.await { true }
        var left = false
        vm.leave { left = true }
        await(db.workouts.observeWorkout(id)) { it == null }
        await(vm.uiState) { left }
        assertTrue(left)
    }

    @Test
    fun noteIsSavedAsTyped() {
        val id = runBlocking { db.finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 10)))) }
        val vm = report(id)
        vm.await { true }
        vm.onNoteChange("어깨가 뻐근했다")
        assertEquals("어깨가 뻐근했다", vm.noteText)
        assertEquals("어깨가 뻐근했다", vm.await { it.note != null }.note)
    }

    private companion object {
        const val TIMEOUT = 5_000L
        const val WRITE_DRAIN_MILLIS = 500L
    }
}
