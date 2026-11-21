package io.github.zzunsik.nextset.ui.routine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.zzunsik.nextset.domain.model.RoutineDetail
import io.github.zzunsik.nextset.domain.model.RoutineExercise
import io.github.zzunsik.nextset.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 루틴 편집 화면이 그리는 상태. */
data class RoutineEditUiState(
    val isNew: Boolean = false,
    /** 이름 칸의 글자. 비어 있으면 저장소에는 "새 루틴"으로 저장돼 있다. */
    val name: String = "",
    /** 루틴 안의 순서대로. */
    val exercises: List<RoutineExercise> = emptyList(),
    /** 저장소에서 아직 루틴을 받지 못했다. */
    val loading: Boolean = true,
    /** 루틴이 없다(다른 곳에서 지웠다). */
    val missing: Boolean = false,
)

/**
 * 루틴 편집(RTN-01, RTN-02). 저장 버튼 없이 고칠 때마다 저장한다(NF-09). 쓰기는 부른 순서대로 한다([SerialWriter]).
 *
 * @param isNew "새 루틴"으로 막 만든 루틴이다. 이름도 종목도 없이 나가면 버린다([leave]).
 * @param newRoutineName 이름을 비웠을 때 저장할 이름("새 루틴"). 문자열 리소스에서 받는다(NF-03).
 */
class RoutineEditViewModel(
    private val repository: RoutineRepository,
    private val routineId: Long,
    val isNew: Boolean,
    private val newRoutineName: String,
) : ViewModel() {
    private val writes = SerialWriter(viewModelScope)
    private val detail: Flow<RoutineDetail?> = repository.observeRoutine(routineId)

    /**
     * 이름 칸의 글자. 저장소에서 처음 읽기 전에는 null이다. 처음 한 번만 저장소에서 채우고 그 뒤로는 친 글자를 따른다.
     * 입력 칸은 이 값을 [uiState]를 거치지 않고 바로 읽는다([nameText]). StateFlow를 한 번 거쳐 늦게 돌아온 값을 칸에 다시 넣으면
     * 빠르게 친 글자가 사라진다(에뮬레이터에서 확인).
     */
    private val name = MutableStateFlow<String?>(null)

    /** 이름 칸에 보일 글자. Compose 상태라 친 즉시 칸에 반영된다. */
    var nameText: String by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            val stored = detail.first()?.routine?.name.orEmpty()
            // 새 루틴은 "새 루틴"으로 저장돼 있어도 칸을 비워 둔다. 안내 글 "루틴 이름"이 보인다.
            if (name.value == null) {
                val initial = if (isNew && stored == newRoutineName) "" else stored
                nameText = initial
                name.value = initial
            }
        }
    }

    val uiState: StateFlow<RoutineEditUiState> = combine(detail, name) { detail, name ->
        RoutineEditUiState(
            isNew = isNew,
            name = name.orEmpty(),
            exercises = detail?.exercises.orEmpty(),
            loading = name == null,
            missing = name != null && detail == null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RoutineEditUiState(isNew = isNew))

    /** 이름을 바꾼다(RTN-01). 비우면 "새 루틴"으로 저장한다. 루틴 이름은 비어 있을 수 없다. */
    fun onNameChange(value: String) {
        nameText = value
        name.value = value
        writes.enqueue { repository.renameRoutine(routineId, value.trim().ifEmpty { newRoutineName }) }
    }

    /** 종목 고르기에서 고른 종목을 고른 순서대로 끝에 더한다(RTN-02). 세트 수는 [DEFAULT_SET_COUNT], 목표는 비운다. */
    fun addExercises(exerciseIds: List<String>) {
        if (exerciseIds.isEmpty()) return
        writes.enqueue {
            exerciseIds.forEach { repository.addRoutineExercise(routineId, it, DEFAULT_SET_COUNT) }
        }
    }

    /** 종목 순서를 [orderedIds]로 바꾼다(RTN-02). */
    fun reorder(orderedIds: List<Long>) {
        if (orderedIds == uiState.value.exercises.map { it.id }) return
        writes.enqueue { repository.reorderRoutineExercises(routineId, orderedIds) }
    }

    /** 종목 설정에서 뺀 종목을 같은 자리에 다시 넣는다("되돌리기"). */
    fun restore(removed: RemovedRoutineExercise) {
        writes.enqueue {
            val newId = repository.addRoutineExercise(routineId, removed.exerciseId, removed.setCount, removed.target)
            val ids = detail.first()?.exercises.orEmpty().map { it.id }.filter { it != newId }
            val ordered = ids.toMutableList().apply { add(removed.position.coerceIn(0, size), newId) }
            repository.reorderRoutineExercises(routineId, ordered)
        }
    }

    /** 루틴을 지운다(RTN-01). 지난 운동 기록은 남는다. 끝나면 [onDeleted]를 부른다. */
    fun delete(onDeleted: () -> Unit) {
        writes.enqueue {
            repository.deleteRoutine(routineId)
            onDeleted()
        }
    }

    /**
     * 화면을 나간다. 앞서 부른 쓰기를 마친 뒤 [onLeft]를 부른다.
     * 새 루틴인데 이름도 종목도 없으면 버린다(docs/DESIGN.md "루틴 편집").
     *
     * 이 화면이 처음 읽은 상태가 아니라 저장소에 있는 지금 상태로 판단한다([RoutineRepository.discardIfBlank]). 같은 루틴의 편집 화면이
     * 둘 열렸을 때(#103) 아래에 남은 예전 화면은 이름 칸이 빈 채로 남아 있어, 위 화면에서 이름을 붙인 루틴을 지울 수 있었다.
     */
    fun leave(onLeft: () -> Unit) {
        writes.enqueue {
            if (isNew && name.value.isNullOrBlank()) repository.discardIfBlank(routineId, newRoutineName)
            onLeft()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
