package io.github.zzunsik.nextset.ui.workout

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.zzunsik.nextset.data.repository.TestDb
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PLANK
import io.github.zzunsik.nextset.data.repository.TestExercises.PULL_UP
import io.github.zzunsik.nextset.data.repository.TestExercises.SQUAT
import io.github.zzunsik.nextset.data.repository.at
import io.github.zzunsik.nextset.data.repository.finishedWorkout
import io.github.zzunsik.nextset.data.repository.reps
import io.github.zzunsik.nextset.data.repository.seconds
import io.github.zzunsik.nextset.data.repository.wr
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Target
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.ui.component.NumberFieldKind
import io.github.zzunsik.nextset.ui.component.NumberPadKey
import io.github.zzunsik.nextset.ui.ViewModelJobs
import io.github.zzunsik.nextset.ui.exercise.MainDispatcherRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 운동 중 화면의 상태(LOG-01~LOG-14). 메모리 데이터베이스와 진짜 저장소 구현으로 돌린다.
 * 저장은 데이터베이스 스레드에서 끝나므로 상태가 바뀔 때까지 기다린 뒤 견준다([awaitState]).
 */
@RunWith(RobolectricTestRunner::class)
class WorkoutViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var db: TestDb
    private var clockMinutes = 60L
    /** 화면 상태를 담는 곳. 데이터베이스를 닫기 전에 비워 화면 상태의 구독을 끝낸다. */
    private val store = ViewModelStore()

    @Before
    fun setUp() = runBlocking { db = TestDb().seed(); Unit }

    /** 데이터베이스를 닫기 전에 화면 상태의 코루틴이 모두 끝나기를 기다린다(#75). */
    private val viewModelJobs = ViewModelJobs()

    @After
    fun tearDown() {
        viewModelJobs.clearAndAwait(store)
        db.close()
    }

    private var created = 0

    private fun viewModel(workoutId: Long): WorkoutViewModel {
        val factory = viewModelFactory {
            initializer { WorkoutViewModel(db.workouts, db.routines, workoutId, clock = { at(clockMinutes++) }) }
        }
        return viewModelJobs.track(ViewModelProvider.create(store, factory)["vm-${created++}", WorkoutViewModel::class])
    }

    private fun WorkoutViewModel.awaitState(predicate: (WorkoutUiState) -> Boolean): WorkoutUiState = runBlocking {
        withContext(Dispatchers.Default) { withTimeout(TIMEOUT) { uiState.first { !it.loading && predicate(it) } } }
    }

    private fun <T> await(flow: Flow<T>, predicate: (T) -> Boolean): T = runBlocking {
        withContext(Dispatchers.Default) { withTimeout(TIMEOUT) { flow.first(predicate) } }
    }

    private fun WorkoutViewModel.nextEvent(): WorkoutEvent = runBlocking {
        withContext(Dispatchers.Default) { withTimeout(TIMEOUT) { events.first() } }
    }

    private fun WorkoutViewModel.events(count: Int): List<WorkoutEvent> = runBlocking {
        withContext(Dispatchers.Default) { withTimeout(TIMEOUT) { events.take(count).toList() } }
    }

    private fun <T> db(block: suspend TestDb.() -> T): T = runBlocking { db.block() }

    private fun stored(workoutId: Long) = db { workouts.observeWorkout(workoutId).first()!! }

    /** 벤치 3세트(목표 60kg 8~12회), 풀업 2세트(목표 없음)의 루틴. */
    private fun routine(): Long = db {
        val id = routines.addRoutine("테스트 루틴")
        routines.addRoutineExercise(id, BENCH, 3, Target(weight = Weight.kg(60), repsMin = 8, repsMax = 12))
        routines.addRoutineExercise(id, PULL_UP, 2)
        id
    }

    private fun start(routineId: Long): Long = db { workouts.startFromRoutine(routineId, at(60)) }

    // ---- 미리 채우기(LOG-03) ----

    @Test
    fun prefillsFromThePreviousRecord() {
        db { finishedWorkout(at(0), mapOf(BENCH to listOf(wr(70_000, 6), wr(67_500, 7)))) }
        val vm = viewModel(start(routine()))
        val bench = vm.awaitState { it.exercises.isNotEmpty() }.exercises[0]
        assertEquals(wr(70_000, 6), bench.sets[0].value)
        assertEquals(wr(67_500, 7), bench.sets[1].value)
        assertEquals("직전 기록 열", wr(70_000, 6), bench.sets[0].previous)
        assertEquals("직전 기록에 없는 순번은 목표", wr(60_000, 8), bench.sets[2].value)
        assertNull(bench.sets[2].previous)
        assertEquals("미리 채운 값으로 보인다", setOf(NumberFieldKind.WEIGHT, NumberFieldKind.REPS), bench.sets[0].suggested)
    }

    @Test
    fun prefillsFromTheTargetWhenThereIsNoRecord() {
        val vm = viewModel(start(routine()))
        val bench = vm.awaitState { it.exercises.isNotEmpty() }.exercises[0]
        assertEquals(List(3) { wr(60_000, 8) }, bench.sets.map { it.value })
        assertTrue(bench.sets.all { it.previous == null })
        assertEquals(setOf(NumberFieldKind.WEIGHT, NumberFieldKind.REPS), bench.sets[1].suggested)
    }

    @Test
    fun leavesFieldsEmptyWithoutRecordOrTarget() {
        val vm = viewModel(start(routine()))
        val pullUp = vm.awaitState { it.exercises.size == 2 }.exercises[1]
        assertEquals(List(2) { SetValue.EMPTY }, pullUp.sets.map { it.value })
        assertTrue(pullUp.sets.all { it.suggested.isEmpty() })
    }

    @Test
    fun editedValueIsNoLongerShownAsPrefilled() {
        val workoutId = start(routine())
        val vm = viewModel(workoutId)
        val set = vm.awaitState { it.exercises.isNotEmpty() }.exercises[0].sets[0]
        vm.onFieldClick(set.id, NumberFieldKind.REPS)
        vm.onNumberPadKey(NumberPadKey.Digit(1))
        vm.onNumberPadKey(NumberPadKey.Digit(0))
        vm.onNumberPadKey(NumberPadKey.Close)

        val row = vm.awaitState { it.exercises[0].sets[0].value.reps == 10 && it.numberPad == null }.exercises[0].sets[0]
        assertEquals(setOf(NumberFieldKind.WEIGHT), row.suggested)
        // 입력한 값은 바로 저장된다(NF-09).
        await(db.workouts.observeWorkout(workoutId)) { it!!.exercises[0].sets[0].value == wr(60_000, 10) }
    }

    // ---- 체크만 눌러 완료(LOG-04) ----

    @Test
    fun checkOnlySavesThePrefilledValue() {
        db { finishedWorkout(at(0), mapOf(BENCH to listOf(wr(72_500, 5)))) }
        val workoutId = start(routine())
        val vm = viewModel(workoutId)
        val set = vm.awaitState { it.exercises.isNotEmpty() }.exercises[0].sets[0]

        vm.onCheck(set.id)

        val saved = await(db.workouts.observeWorkout(workoutId)) { it!!.exercises[0].sets[0].isCompleted }!!
        assertEquals(wr(72_500, 5), saved.exercises[0].sets[0].value)
        assertEquals(WorkoutEvent.SetCompleted(set.id, saved.exercises[0].id, null), vm.nextEvent())
    }

    @Test
    fun checkingAnEmptySetOpensTheNumberPadInstead() {
        val vm = viewModel(start(routine()))
        val set = vm.awaitState { it.exercises.size == 2 }.exercises[1].sets[0]

        vm.onCheck(set.id)

        val state = vm.awaitState { it.numberPad != null }
        assertEquals(SetFieldId(set.id, NumberFieldKind.REPS), state.numberPad!!.focused.id)
        assertFalse(state.exercises[1].sets[0].completed)
    }

    @Test
    fun checkingAgainUncompletes() {
        val workoutId = start(routine())
        val vm = viewModel(workoutId)
        val bench = vm.awaitState { it.exercises.isNotEmpty() }.exercises[0]
        val set = bench.sets[0]
        vm.onCheck(set.id)
        vm.awaitState { it.exercises[0].sets[0].completed }

        vm.onCheck(set.id)

        vm.awaitState { !it.exercises[0].sets[0].completed }
        assertNull(stored(workoutId).exercises[0].sets[0].completedAt)
        assertEquals(
            listOf(WorkoutEvent.SetCompleted(set.id, bench.id, null), WorkoutEvent.SetUncompleted(set.id)),
            vm.events(2),
        )
    }

    @Test
    fun numberPadDoneCompletesTheSet() {
        val workoutId = start(routine())
        val vm = viewModel(workoutId)
        val set = vm.awaitState { it.exercises.size == 2 }.exercises[1].sets[0]
        vm.onFieldClick(set.id, NumberFieldKind.REPS)
        vm.onNumberPadKey(NumberPadKey.Digit(7))
        vm.onNumberPadKey(NumberPadKey.Next) // 마지막 칸이라 "완료"다.

        val state = vm.awaitState { it.exercises[1].sets[0].completed }
        assertNull("숫자판이 닫힌다", state.numberPad)
        assertEquals(reps(7), stored(workoutId).exercises[1].sets[0].value)
    }

    // ---- 다음 세트(LOG-06) ----

    @Test
    fun nextSetMovesOnAndCrossesExercises() {
        val vm = viewModel(start(routine()))
        val start = vm.awaitState { it.exercises.size == 2 }
        val bench = start.exercises[0]
        assertEquals(bench.sets[0].id, start.nextSet!!.setId)
        assertEquals(1, start.nextSet!!.setNumber)
        assertEquals(3, start.nextSet!!.setCount)
        assertEquals("값은 미리 채운 값이다", wr(60_000, 8), start.nextSet!!.value)
        assertTrue(start.exercises[0].sets[0].isNext)

        vm.onCheck(bench.sets[0].id)
        assertEquals(2, vm.awaitState { it.nextSet?.setId == bench.sets[1].id }.nextSet!!.setNumber)

        vm.onCheck(bench.sets[1].id)
        vm.onCheck(bench.sets[2].id)
        val crossed = vm.awaitState { it.nextSet?.workoutExerciseId == start.exercises[1].id }
        assertEquals(start.exercises[1].sets[0].id, crossed.nextSet!!.setId)
        assertEquals("테스트 풀업", crossed.nextSet!!.exerciseName)
        assertTrue(crossed.exercises[0].allDone)
    }

    @Test
    fun allDoneWhenEverySetIsCompleted() {
        val workoutId = db { workouts.startEmpty(at(60)) }
        val vm = viewModel(workoutId)
        assertFalse("빈 운동은 모두 완료가 아니다", vm.awaitState { true }.allDone)
        vm.onExercisesPicked(listOf(PLANK))
        val set = vm.awaitState { it.exercises.size == 1 }.exercises[0].sets[0]
        vm.onFieldClick(set.id, NumberFieldKind.DURATION)
        listOf(1, 3, 0).forEach { vm.onNumberPadKey(NumberPadKey.Digit(it)) }
        vm.onNumberPadKey(NumberPadKey.Next)

        val done = vm.awaitState { it.allDone }
        assertNull(done.nextSet)
        assertEquals(seconds(90), done.exercises[0].sets[0].value)
    }

    @Test
    fun editingShowsInTheNextSetDisplayRightAway() {
        val vm = viewModel(start(routine()))
        val set = vm.awaitState { it.exercises.isNotEmpty() }.exercises[0].sets[0]
        vm.onFieldClick(set.id, NumberFieldKind.WEIGHT)
        vm.onNumberPadKey(NumberPadKey.Step(Weight.STEP_2_5KG.grams))
        assertEquals(wr(62_500, 8), vm.awaitState { it.nextSet?.value?.weight == Weight(62_500) }.nextSet!!.value)
        assertEquals("줄어든 표시와 같은 행의 칸에도 보인다", wr(62_500, 8), vm.awaitState { it.exercises[0].sets[0].value == wr(62_500, 8) }.exercises[0].sets[0].value)
    }

    // ---- 세트와 종목(LOG-09, LOG-10) ----

    @Test
    fun addedSetCopiesTheRowAboveWhenNothingElseFills() {
        val workoutId = start(routine())
        val vm = viewModel(workoutId)
        val pullUp = vm.awaitState { it.exercises.size == 2 }.exercises[1]
        vm.onFieldClick(pullUp.sets[1].id, NumberFieldKind.REPS)
        vm.onNumberPadKey(NumberPadKey.Digit(9))
        vm.onNumberPadKey(NumberPadKey.Close)
        vm.awaitState { it.exercises[1].sets[1].value == reps(9) }

        vm.onAddSet(pullUp.id)

        val added = vm.awaitState { it.exercises[1].sets.size == 3 && it.exercises[1].sets[2].value == reps(9) }
        assertEquals(setOf(NumberFieldKind.REPS), added.exercises[1].sets[2].suggested)
    }

    @Test
    fun addedSetPrefersTheTarget() {
        val vm = viewModel(start(routine()))
        val bench = vm.awaitState { it.exercises.isNotEmpty() }.exercises[0]
        vm.onAddSet(bench.id)
        assertEquals(wr(60_000, 8), vm.awaitState { it.exercises[0].sets.size == 4 }.exercises[0].sets[3].value)
    }

    @Test
    fun deletedSetCanBeRestored() {
        val workoutId = start(routine())
        val vm = viewModel(workoutId)
        val bench = vm.awaitState { it.exercises.isNotEmpty() }.exercises[0]
        vm.onCheck(bench.sets[0].id)
        vm.awaitState { it.exercises[0].sets[0].completed }

        vm.onDeleteSet(bench.sets[0].id)
        vm.awaitState { it.exercises[0].sets.size == 2 }
        vm.onUndoDelete()

        val restored = vm.awaitState { it.exercises[0].sets.size == 3 }.exercises[0].sets[0]
        assertTrue(restored.completed)
        assertEquals(wr(60_000, 8), restored.value)
    }

    @Test
    fun deleteLastSet() {
        val vm = viewModel(start(routine()))
        val bench = vm.awaitState { it.exercises.isNotEmpty() }.exercises[0]
        vm.onDeleteLastSet(bench.id)
        assertEquals(bench.sets.take(2).map { it.id }, vm.awaitState { it.exercises[0].sets.size == 2 }.exercises[0].sets.map { it.id })
    }

    @Test
    fun exercisesAreAddedRemovedAndReordered() {
        val workoutId = start(routine())
        val vm = viewModel(workoutId)
        val first = vm.awaitState { it.exercises.size == 2 }

        vm.onExercisesPicked(listOf(SQUAT, PLANK))
        val added = vm.awaitState { it.exercises.size == 4 }
        assertEquals(listOf(BENCH, PULL_UP, SQUAT, PLANK), added.exercises.map { it.exerciseId })
        assertEquals("더한 종목에는 세트가 하나 있다", 1, added.exercises[2].sets.size)

        vm.onMoveExercise(added.exercises[3].id, -1)
        vm.awaitState { it.exercises.map { e -> e.exerciseId } == listOf(BENCH, PULL_UP, PLANK, SQUAT) }
        vm.onMoveExercise(added.exercises[0].id, -1) // 맨 위는 더 올라가지 않는다.

        vm.onRemoveExerciseRequest(first.exercises[1].id)
        assertEquals(WorkoutDialog.RemoveExercise(first.exercises[1].id, "테스트 풀업"), vm.awaitState { it.dialog == WorkoutDialog.RemoveExercise(first.exercises[1].id, "테스트 풀업") }.dialog)
        vm.onRemoveExerciseConfirm(first.exercises[1].id)
        val removed = vm.awaitState { it.exercises.size == 3 }
        assertEquals(listOf(BENCH, PLANK, SQUAT), removed.exercises.map { it.exerciseId })
        assertNull(removed.dialog)
    }

    @Test
    fun notesAreSavedAsTyped() {
        val workoutId = start(routine())
        val vm = viewModel(workoutId)
        val bench = vm.awaitState { it.exercises.isNotEmpty() }.exercises[0]
        vm.onExerciseNoteRequest(bench.id)
        vm.onExerciseNoteChange(bench.id, "그립 넓게")
        vm.onWorkoutNoteRequest()
        vm.onWorkoutNoteChange("컨디션 좋음")

        val saved = await(db.workouts.observeWorkout(workoutId)) { it!!.note == "컨디션 좋음" }!!
        assertEquals("그립 넓게", saved.exercises[0].note)
        assertEquals("그립 넓게", vm.awaitState { it.exercises[0].note == "그립 넓게" }.exercises[0].note)
    }

    // ---- 끝내기(LOG-10, LOG-15)와 버리기 ----

    @Test
    fun finishingWithoutCompletedSetsOffersToDiscard() {
        val workoutId = start(routine())
        val vm = viewModel(workoutId)
        vm.awaitState { it.exercises.isNotEmpty() }
        vm.onFinishRequest()
        assertEquals(WorkoutDialog.NoCompletedSets, vm.awaitState { it.dialog == WorkoutDialog.NoCompletedSets }.dialog)

        vm.onDiscardConfirm()

        assertEquals(WorkoutEvent.Exit(WorkoutExit.Home), vm.nextEvent())
        assertNull(db { workouts.observeWorkout(workoutId).first() })
    }

    @Test
    fun unchangedRoutineWorkoutFinishesWithoutAsking() {
        val workoutId = start(routine())
        val vm = viewModel(workoutId)
        val state = vm.awaitState { it.exercises.size == 2 }
        state.exercises.flatMap { it.sets }.forEach { set ->
            if (set.value.reps == null) {
                vm.onFieldClick(set.id, NumberFieldKind.REPS)
                vm.onNumberPadKey(NumberPadKey.Digit(5))
                vm.onNumberPadKey(NumberPadKey.Next)
            } else {
                vm.onCheck(set.id)
            }
        }
        vm.awaitState { it.allDone }
        vm.events(5) // 세트 완료 신호 다섯 개

        vm.onFinishRequest()

        assertEquals(WorkoutEvent.Exit(WorkoutExit.Finished(workoutId)), vm.nextEvent())
        assertNull("묻지 않았다", vm.awaitState { it.dialog == null }.dialog)
        assertNotNull(stored(workoutId).finishedAt)
    }

    @Test
    fun emptyWorkoutDoesNotAskAboutTheRoutine() {
        val workoutId = db { workouts.startEmpty(at(60)) }
        val vm = viewModel(workoutId)
        vm.onExercisesPicked(listOf(BENCH))
        val set = vm.awaitState { it.exercises.size == 1 }.exercises[0].sets[0]
        vm.onFieldClick(set.id, NumberFieldKind.WEIGHT)
        listOf(4, 0).forEach { vm.onNumberPadKey(NumberPadKey.Digit(it)) }
        vm.onNumberPadKey(NumberPadKey.Next)
        vm.onNumberPadKey(NumberPadKey.Digit(5))
        vm.onNumberPadKey(NumberPadKey.Next)
        vm.awaitState { it.allDone }
        vm.nextEvent()

        vm.onFinishRequest()

        assertEquals(WorkoutEvent.Exit(WorkoutExit.Finished(workoutId)), vm.nextEvent())
        assertEquals(wr(40_000, 5), stored(workoutId).exercises.single().sets.single().value)
    }

    @Test
    fun incompleteSetsAreAnnouncedThenRoutineChangeIsAsked() {
        val routineId = routine()
        val workoutId = start(routineId)
        val vm = viewModel(workoutId)
        val state = vm.awaitState { it.exercises.size == 2 }
        vm.onCheck(state.exercises[0].sets[0].id)
        vm.awaitState { it.exercises[0].sets[0].completed }
        vm.onRemoveExerciseConfirm(state.exercises[1].id) // 풀업을 뺐다.
        vm.awaitState { it.exercises.size == 1 }

        vm.onFinishRequest()
        assertEquals(WorkoutDialog.IncompleteSets(2), vm.awaitState { it.dialog == WorkoutDialog.IncompleteSets(2) }.dialog)
        vm.onIncompleteConfirm()
        vm.awaitState { it.dialog == WorkoutDialog.RoutineChange }

        vm.onRoutineChangeAnswer(apply = true)

        assertEquals(WorkoutEvent.SetCompleted(state.exercises[0].sets[0].id, state.exercises[0].id, null), vm.nextEvent())
        assertEquals(WorkoutEvent.Exit(WorkoutExit.Finished(workoutId)), vm.nextEvent())
        val routine = db { routines.observeRoutine(routineId).first()!! }
        assertEquals("완료하지 않은 세트도 루틴의 세트 수로 센다", listOf(BENCH to 3), routine.exercises.map { it.exercise.id to it.setCount })
        val finished = stored(workoutId)
        assertEquals("완료하지 않은 세트는 기록에 남지 않는다", 1, finished.exercises.single().sets.size)
    }

    @Test
    fun answeringThisTimeLeavesTheRoutineAlone() {
        val routineId = routine()
        val workoutId = start(routineId)
        val vm = viewModel(workoutId)
        val state = vm.awaitState { it.exercises.size == 2 }
        vm.onMoveExercise(state.exercises[1].id, -1)
        vm.awaitState { it.exercises[0].exerciseId == PULL_UP }
        vm.onCheck(state.exercises[0].sets[0].id)
        vm.awaitState { it.exercises[1].sets[0].completed }

        vm.onFinishRequest()
        vm.onIncompleteConfirm()
        vm.awaitState { it.dialog == WorkoutDialog.RoutineChange }
        vm.onRoutineChangeAnswer(apply = false)

        vm.nextEvent()
        assertEquals(WorkoutEvent.Exit(WorkoutExit.Finished(workoutId)), vm.nextEvent())
        val routine = db { routines.observeRoutine(routineId).first()!! }
        assertEquals(listOf(BENCH to 3, PULL_UP to 2), routine.exercises.map { it.exercise.id to it.setCount })
    }

    @Test
    fun discardAsksFirstAndLeavesNoRecord() {
        val workoutId = start(routine())
        val vm = viewModel(workoutId)
        val set = vm.awaitState { it.exercises.isNotEmpty() }.exercises[0].sets[0]
        vm.onCheck(set.id)
        vm.awaitState { it.exercises[0].sets[0].completed }
        vm.onDiscardRequest()
        assertEquals(WorkoutDialog.Discard, vm.awaitState { it.dialog == WorkoutDialog.Discard }.dialog)
        vm.dismissDialog()
        assertNull(vm.awaitState { it.dialog == null }.dialog)

        vm.onDiscardRequest()
        vm.onDiscardConfirm()

        vm.nextEvent()
        assertEquals(WorkoutEvent.Exit(WorkoutExit.Home), vm.nextEvent())
        assertNull(db { workouts.observeWorkout(workoutId).first() })
        assertTrue(db { workouts.observeHistory().first() }.isEmpty())
    }

    // ---- 최고 기록(LOG-14) ----

    @Test
    fun beatingTheRecordIsAnnouncedAndMarked() {
        db { finishedWorkout(at(0), mapOf(BENCH to listOf(wr(60_000, 8)))) }
        val vm = viewModel(start(routine()))
        val bench = vm.awaitState { it.exercises.isNotEmpty() }.exercises[0]
        // 첫 세트는 직전 기록과 같은 60kg 8회라 최고 기록이 아니다.
        vm.onCheck(bench.sets[0].id)
        assertEquals(WorkoutEvent.SetCompleted(bench.sets[0].id, bench.id, null), vm.nextEvent())

        vm.onFieldClick(bench.sets[1].id, NumberFieldKind.REPS)
        vm.onNumberPadKey(NumberPadKey.Step(1))
        vm.onNumberPadKey(NumberPadKey.Next)

        assertEquals(WorkoutEvent.NewRecord("테스트 벤치"), vm.nextEvent())
        val state = vm.awaitState { it.exercises[0].sets[1].isRecord }
        assertFalse(state.exercises[0].sets[0].isRecord)

        vm.onCheck(bench.sets[1].id)
        assertFalse("완료를 풀면 표시도 사라진다", vm.awaitState { !it.exercises[0].sets[1].completed }.exercises[0].sets[1].isRecord)
    }

    // ---- 다시 만들기(LOG-13) ----

    @Test
    fun recreatedViewModelKeepsValuesAndProgress() {
        val workoutId = start(routine())
        val first = viewModel(workoutId)
        val bench = first.awaitState { it.exercises.isNotEmpty() }.exercises[0]
        first.onCheck(bench.sets[0].id)
        first.onFieldClick(bench.sets[1].id, NumberFieldKind.WEIGHT)
        listOf(6, 5).forEach { first.onNumberPadKey(NumberPadKey.Digit(it)) }
        // 숫자판을 닫지 않은 채 화면이 없어진다.
        await(db.workouts.observeWorkout(workoutId)) { it!!.exercises[0].sets[1].value.weight == Weight.kg(65) }

        val second = viewModel(workoutId)
        val restored = second.awaitState { it.exercises.isNotEmpty() }
        assertTrue(restored.exercises[0].sets[0].completed)
        assertEquals(wr(65_000, 8), restored.exercises[0].sets[1].value)
        assertEquals(bench.sets[1].id, restored.nextSet!!.setId)
    }

    @Test
    fun goneWorkoutExitsToHome() {
        val workoutId = start(routine())
        val vm = viewModel(workoutId)
        vm.awaitState { it.exercises.isNotEmpty() }
        db { workouts.discardWorkout(workoutId) }
        assertEquals(WorkoutEvent.Exit(WorkoutExit.Home), vm.nextEvent())
        assertNull(withTimeoutOrNullBlocking { vm.events.firstOrNull() })
    }

    private fun <T> withTimeoutOrNullBlocking(block: suspend () -> T): T? = runBlocking {
        withContext(Dispatchers.Default) { withTimeoutOrNull(300) { block() } }
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
