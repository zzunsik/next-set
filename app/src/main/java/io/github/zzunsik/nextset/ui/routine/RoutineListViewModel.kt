package io.github.zzunsik.nextset.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zzunsik.nextset.domain.model.Routine
import io.github.zzunsik.nextset.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 루틴 목록 화면이 그리는 상태. */
data class RoutineListUiState(
    /** 루틴 목록 순서(RTN-05). */
    val routines: List<Routine> = emptyList(),
    /** 저장소에서 아직 목록을 받지 못했다. 이때는 빈 상태를 보이지 않는다. */
    val loading: Boolean = true,
)

/**
 * 루틴 목록(임시 화면, [io.github.zzunsik.nextset.ui.navigation.RoutineListKey]). 루틴을 만들고 목록 순서를 바꾼다.
 *
 * @param newRoutineName 새로 만든 루틴의 이름("새 루틴"). 문자열 리소스에서 받는다(NF-03).
 */
class RoutineListViewModel(
    private val repository: RoutineRepository,
    private val newRoutineName: String,
) : ViewModel() {
    private val writes = SerialWriter(viewModelScope)

    val uiState: StateFlow<RoutineListUiState> = repository.observeRoutines()
        .map { RoutineListUiState(routines = it, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RoutineListUiState())

    /**
     * "새 루틴". 목록 끝에 "새 루틴"이라는 이름으로 바로 저장하고(NF-09) 만든 루틴의 식별자를 [onCreated]로 넘긴다.
     * 이름도 종목도 없이 루틴 편집을 나가면 루틴 편집이 이 루틴을 버린다.
     */
    fun createRoutine(onCreated: (Long) -> Unit) {
        writes.enqueue { onCreated(repository.addRoutine(newRoutineName)) }
    }

    /** 목록 순서를 [orderedIds]로 바꾼다(RTN-05). 다음 차례 계산(HOME-02)이 이 순서를 쓴다. */
    fun reorder(orderedIds: List<Long>) {
        if (orderedIds == uiState.value.routines.map { it.id }) return
        writes.enqueue { repository.reorderRoutines(orderedIds) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
