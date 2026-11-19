package io.github.zzunsik.nextset.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.ExerciseFilter
import io.github.zzunsik.nextset.domain.model.SubPart
import io.github.zzunsik.nextset.domain.repository.ExerciseRepository
import io.github.zzunsik.nextset.domain.rule.filterBy
import io.github.zzunsik.nextset.ui.navigation.ExercisePickerMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** 종목 고르기 화면이 그리는 상태. */
data class ExercisePickerUiState(
    val mode: ExercisePickerMode = ExercisePickerMode.Pick,
    val query: String = "",
    val bodyPart: BodyPart? = null,
    val subPart: SubPart? = null,
    /** 검색어와 부위 필터를 함께 적용한 목록. 숨긴 종목은 없다. */
    val exercises: List<Exercise> = emptyList(),
    /** 고른 종목의 식별자. 고른 순서다. 필터에 가려 목록에 안 보이는 종목도 남는다. */
    val selectedIds: List<String> = emptyList(),
    /** 저장소에서 아직 목록을 받지 못했다. 이때는 빈 상태를 보이지 않는다. */
    val loading: Boolean = true,
) {
    val showsNoResults: Boolean get() = !loading && exercises.isEmpty()
}

/**
 * 종목 고르기(EX-02, EX-03). 검색과 거르기는 domain의 규칙([filterBy])이 한다.
 * 저장소에서 숨기지 않은 종목 전체를 한 번 받아 두고, 검색어나 필터가 바뀔 때마다 다시 거른다.
 */
class ExercisePickerViewModel(
    repository: ExerciseRepository,
    val mode: ExercisePickerMode,
) : ViewModel() {
    private val filter = MutableStateFlow(ExerciseFilter())
    private val selected = MutableStateFlow<List<String>>(emptyList())

    val uiState: StateFlow<ExercisePickerUiState> = combine(
        repository.observeExercises(),
        filter,
        selected,
    ) { all, filter, selected ->
        ExercisePickerUiState(
            mode = mode,
            query = filter.query,
            bodyPart = filter.bodyPart,
            subPart = filter.subPart,
            exercises = all.filterBy(filter),
            selectedIds = selected,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ExercisePickerUiState(mode = mode))

    fun onQueryChange(query: String) {
        filter.update { it.copy(query = query) }
    }

    /** 부위 칩. null은 "전체"다. 부위를 바꾸면 고른 세부 부위는 풀린다(docs/DESIGN.md `BodyPartFilter`). */
    fun onBodyPartSelect(bodyPart: BodyPart?) {
        filter.update { if (it.bodyPart == bodyPart) it else it.copy(bodyPart = bodyPart, subPart = null) }
    }

    /** 세부 부위 칩. 고른 칩을 다시 누르면 풀린다. 부위와 맞지 않는 값은 무시한다. */
    fun onSubPartToggle(subPart: SubPart) {
        filter.update {
            when {
                it.bodyPart != subPart.bodyPart -> it
                it.subPart == subPart -> it.copy(subPart = null)
                else -> it.copy(subPart = subPart)
            }
        }
    }

    /** 행을 누르면 선택이 켜지고 꺼진다. 관리로 열었으면 고르지 않는다. */
    fun onToggle(exerciseId: String) {
        if (mode != ExercisePickerMode.Pick) return
        selected.update { if (exerciseId in it) it - exerciseId else it + exerciseId }
    }

    /** 이 화면에서 새로 만든 종목은 고른 상태로 넣는다(docs/DESIGN.md "종목 추가·수정"). */
    fun onExerciseCreated(exerciseId: String) {
        if (mode != ExercisePickerMode.Pick) return
        selected.update { if (exerciseId in it) it else it + exerciseId }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
