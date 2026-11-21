package io.github.zzunsik.nextset.ui.exercise

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.zzunsik.nextset.domain.repository.ExerciseRepository
import io.github.zzunsik.nextset.ui.navigation.ExerciseFormKey
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerKey
import io.github.zzunsik.nextset.ui.navigation.NavResults
import io.github.zzunsik.nextset.ui.navigation.ResultEffect
import io.github.zzunsik.nextset.ui.navigation.ResultKey
import io.github.zzunsik.nextset.ui.navigation.close
import io.github.zzunsik.nextset.ui.navigation.navigate
import io.github.zzunsik.nextset.ui.navigation.pickedExercisesKey

/** 종목 추가·수정이 새로 만든 종목의 식별자를 종목 고르기에 돌려주는 키. 종목 고르기는 한 번에 하나만 열린다. */
internal val CreatedExerciseKey = ResultKey<String>("exercise-form/created")

/**
 * 종목 고르기와 종목 추가·수정 화면을 뒤로 가기 스택에 잇는다.
 *
 * - 종목 고르기의 "n개 추가"는 고른 종목의 식별자 목록을 [NavResults]에 [ExercisePickerKey.resultName]으로 놓고 닫힌다.
 *   부른 화면은 `results.ResultEffect(pickedExercisesKey(이름)) { ids -> ... }`로 받는다.
 * - 뒤로 가기는 고른 것을 버린다. 값을 놓지 않는다.
 * - 종목 추가·수정에서 새로 만든 종목은 종목 고르기에 돌아가 고른 상태로 들어간다.
 */
fun EntryProviderScope<NavKey>.exerciseEntries(
    backStack: MutableList<NavKey>,
    results: NavResults,
    exerciseRepository: ExerciseRepository,
) {
    entry<ExercisePickerKey> { key ->
        val viewModel = viewModel { ExercisePickerViewModel(exerciseRepository, key.mode) }
        results.ResultEffect(CreatedExerciseKey) { id -> viewModel.onExerciseCreated(id) }
        ExercisePickerRoute(
            viewModel = viewModel,
            onBack = { backStack.close(key) },
            onConfirm = { ids ->
                // 맨 위일 때만 값을 놓고 닫는다. "n개 추가"를 두 번 눌러도 부른 화면(루틴 편집 등)까지 닫히지 않는다(#103).
                if (backStack.lastOrNull() == key) {
                    results.put(pickedExercisesKey(key.resultName), ids)
                    backStack.close(key)
                }
            },
            onNewExercise = { name -> backStack.navigate(key, ExerciseFormKey(initialName = name)) },
            onEditExercise = { id -> backStack.navigate(key, ExerciseFormKey(exerciseId = id)) },
        )
    }
    entry<ExerciseFormKey> { key ->
        val viewModel = viewModel { ExerciseFormViewModel(exerciseRepository, key.exerciseId, key.initialName) }
        ExerciseFormRoute(
            viewModel = viewModel,
            onBack = { backStack.close(key) },
            onFinished = { outcome ->
                // 두 번 불려도 이 화면만 닫고 값도 한 번만 놓는다.
                if (backStack.lastOrNull() == key) {
                    if (outcome is ExerciseFormOutcome.Created) results.put(CreatedExerciseKey, outcome.exerciseId)
                    backStack.close(key)
                }
            },
        )
    }
}
