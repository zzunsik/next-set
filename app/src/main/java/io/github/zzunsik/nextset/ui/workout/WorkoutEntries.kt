package io.github.zzunsik.nextset.ui.workout

import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.zzunsik.nextset.domain.repository.ExerciseRepository
import io.github.zzunsik.nextset.domain.repository.RoutineRepository
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import io.github.zzunsik.nextset.domain.repository.WorkoutRepository
import io.github.zzunsik.nextset.domain.rest.RestTimer
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerKey
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerMode
import io.github.zzunsik.nextset.ui.navigation.NavResults
import io.github.zzunsik.nextset.ui.navigation.ResultEffect
import io.github.zzunsik.nextset.ui.navigation.WorkoutSummaryKey
import io.github.zzunsik.nextset.ui.navigation.WorkoutKey
import io.github.zzunsik.nextset.ui.navigation.pickedExercisesKey

/** 운동 중 화면이 종목 고르기에서 고른 종목을 돌려받는 이름. */
internal const val ADD_EXERCISE_RESULT = "workout/add-exercise"

/**
 * 운동 중 화면을 뒤로 가기 스택에 잇는다. 운동 요약은 기록 화면들과 함께 `historyEntries`가 잇는다.
 *
 * - 뒤로 가기: 앞 화면(홈)으로 간다. 운동은 저장돼 있어 계속된다(docs/DESIGN.md "운동 중").
 *   손대지 않은 운동은 버리고 간다(docs/DESIGN.md "한 번 눌러 시작", [WorkoutViewModel.onLeave]).
 * - 끝내기: 운동 중 화면을 운동 요약(#27)으로 바꾼다. 요약의 "확인"과 뒤로 가기는 홈으로 간다.
 * - 버리기: 운동 중 화면을 닫는다.
 * - 종목 추가: 종목 고르기(#21)를 [ADD_EXERCISE_RESULT]로 열고 고른 종목을 끝에 더한다.
 */
fun EntryProviderScope<NavKey>.workoutEntries(
    backStack: MutableList<NavKey>,
    results: NavResults,
    workoutRepository: WorkoutRepository,
    routineRepository: RoutineRepository,
    settingsRepository: SettingsRepository? = null,
    exerciseRepository: ExerciseRepository? = null,
    restTimer: RestTimer? = null,
) {
    entry<WorkoutKey> { key ->
        val viewModel = viewModel {
            WorkoutViewModel(
                workoutRepository = workoutRepository,
                routineRepository = routineRepository,
                workoutId = key.workoutId,
                settingsRepository = settingsRepository,
                exerciseRepository = exerciseRepository,
            )
        }
        results.ResultEffect(pickedExercisesKey(ADD_EXERCISE_RESULT)) { ids -> viewModel.onExercisesPicked(ids) }
        val leave = { viewModel.onLeave { if (backStack.lastOrNull() == key) backStack.removeAt(backStack.lastIndex) } }
        BackHandler(onBack = leave)
        WorkoutRoute(
            viewModel = viewModel,
            onBack = leave,
            onAddExercise = {
                backStack.add(ExercisePickerKey(mode = ExercisePickerMode.Pick, resultName = ADD_EXERCISE_RESULT))
            },
            onSetCompleted = { event ->
                if (restTimer != null) {
                    // 휴식 중에 다른 세트를 완료하면 그 세트의 휴식 시간으로 다시 시작한다. 마지막 세트면 시작하지 않는다(제안).
                    val seconds = restSecondsToStart(event, viewModel.uiState.value)
                    if (seconds == null) restTimer.cancel() else restTimer.start(key.workoutId, event.setId, seconds)
                }
            },
            onSetUncompleted = { event -> restTimer?.stopForSet(event.setId) },
            restTimerBar = {
                if (restTimer != null) WorkoutRestBarHost(restTimer, viewModel, key.workoutId, settingsRepository)
            },
            onExit = { destination ->
                // 운동을 끝냈거나 버렸다. 휴식과 알림을 정리한다.
                restTimer?.cancel()
                // 두 번 불려도 이 화면만 바꾼다.
                if (backStack.lastOrNull() == key) {
                    backStack.removeAt(backStack.lastIndex)
                    if (destination is WorkoutExit.Finished) backStack.add(WorkoutSummaryKey(destination.workoutId))
                }
            },
        )
    }
}
