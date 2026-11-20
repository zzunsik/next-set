package io.github.zzunsik.nextset.ui.routine

import io.github.zzunsik.nextset.domain.model.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** 종목 고르기에서 더한 종목의 처음 세트 수. docs/DESIGN.md의 종목 설정 예("− 3 +")를 따른다. */
const val DEFAULT_SET_COUNT = 3

/** 세트 수의 범위. 1 아래로는 내려가지 않는다(RTN-04). 위끝은 실수로 계속 눌렀을 때를 막는 값이다. */
const val MIN_SET_COUNT = 1
const val MAX_SET_COUNT = 99

/** 루틴에서 뺀 종목. 루틴 편집이 "되돌리기"로 같은 자리에 다시 넣을 때 쓴다. */
data class RemovedRoutineExercise(
    val exerciseId: String,
    val exerciseName: String,
    val setCount: Int,
    val target: Target,
    /** 빼기 전 루틴 안에서의 순서. 0부터 센다. */
    val position: Int,
)

/** 목표 횟수의 하한이 상한보다 크면 저장하지 않는다(입력 검증). */
val Target.hasValidRepsRange: Boolean
    get() = repsMin == null || repsMax == null || repsMin <= repsMax

/** [ids]에서 [index]번째를 [offset]만큼 옮긴 목록. 범위를 벗어나면 null이다. 화면 낭독기의 "위로/아래로 옮기기"가 쓴다. */
fun <T> List<T>.moved(index: Int, offset: Int): List<T>? {
    val target = index + offset
    if (index !in indices || target !in indices) return null
    return toMutableList().apply { add(target, removeAt(index)) }
}

/**
 * 저장소 쓰기를 부른 순서대로 하나씩 한다. 이름을 칠 때마다 저장하고(NF-09) 그 사이에 종목을 더하거나 화면을 나가도
 * 쓰기 순서가 뒤바뀌지 않는다. [scope]가 끝나면(화면 상태가 사라지면) 남은 쓰기도 버린다.
 *
 * 쓰기 하나가 [IllegalArgumentException]으로 거부되면(다른 곳에서 먼저 지운 행 등) 그 쓰기만 버리고 다음으로 넘어간다.
 */
internal class SerialWriter(scope: CoroutineScope) {
    private val jobs = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (job in jobs) {
                try {
                    job()
                } catch (_: IllegalArgumentException) {
                    // 저장소가 거부한 쓰기. 화면 상태는 저장소의 Flow로 다시 맞춰진다.
                }
            }
        }
    }

    fun enqueue(job: suspend () -> Unit) {
        jobs.trySend(job)
    }
}
