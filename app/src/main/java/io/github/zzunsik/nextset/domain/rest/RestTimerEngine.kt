package io.github.zzunsik.nextset.domain.rest

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * 휴식 타이머의 상태와 신호 시각(docs/rest-timer.md "구현 이슈(#26)가 따를 순서" 1번). 안드로이드와 무관하다.
 *
 * 상태는 끝나는 시각 하나로 정한다. 신호는 [runCues]가 끝나는 시각을 기준으로 매번 다시 계산해 기다리므로
 * 한 번 늦은 신호의 오차가 다음 신호로 쌓이지 않는다. 시간을 바꾸면 그때 남은 시간 기준으로 다시 계산하고,
 * 이미 지난 신호는 내지 않는다(제안).
 *
 * @param now 단조 시계(밀리초). 테스트는 가짜 시계를 넘긴다.
 */
class RestTimerEngine(private val now: () -> Long) {
    private val mutableState = MutableStateFlow<RestTimerState?>(null)
    val state: StateFlow<RestTimerState?> = mutableState.asStateFlow()

    fun now(): Long = now.invoke()

    fun start(workoutId: Long, setId: Long, seconds: Int, nextSet: String? = mutableState.value?.nextSet) {
        val total = seconds.coerceAtLeast(0) * 1000L
        mutableState.value = RestTimerState(
            workoutId = workoutId,
            setId = setId,
            endsAt = now() + total,
            totalMillis = total,
            nextSet = nextSet,
        )
    }

    /** 저장해 둔 휴식을 되살린다. 이미 끝났으면 되살리지 않고 false를 돌려준다(docs/rest-timer.md "되살리는 방법"). */
    fun restore(saved: RestTimerState): Boolean {
        if (saved.finished || saved.endsAt <= now()) return false
        mutableState.value = saved
        return true
    }

    /** 늘리면 남은 시간 10분까지만 받는다. 줄여서 0 이하가 되면 건너뛰기와 같다(docs/DESIGN.md `RestTimerBar`). */
    fun adjust(deltaSeconds: Int) {
        val current = mutableState.value?.takeUnless { it.finished } ?: return
        val time = now()
        val remaining = current.remainingMillis(time) + deltaSeconds * 1000L
        if (remaining <= 0) {
            skip()
            return
        }
        val capped = if (deltaSeconds > 0) {
            remaining.coerceAtMost(maxOf(RestTimerRules.MAX_REMAINING_MILLIS, current.remainingMillis(time)))
        } else {
            remaining
        }
        mutableState.value = current.copy(endsAt = time + capped, totalMillis = maxOf(current.totalMillis, capped))
    }

    fun skip() {
        mutableState.value = null
    }

    fun stopForSet(setId: Long) {
        if (mutableState.value?.setId == setId) mutableState.value = null
    }

    fun setNextSet(text: String?) {
        mutableState.update { it?.copy(nextSet = text) }
    }

    /**
     * 휴식 동안 신호를 낸다. 끝나는 순간의 신호를 낸 뒤 [onFinished]를 부르고 "0:00"을 잠시 보인 다음 휴식을 지운다.
     * 상태가 바뀌면(시간 조절, 다시 시작) 하던 기다림을 버리고 새 상태로 다시 계산한다. 취소될 때까지 돈다.
     */
    suspend fun runCues(signal: suspend (RestCue) -> Unit, onFinished: suspend (RestTimerState) -> Unit = {}) {
        // 다음 세트 글자만 바뀐 것은 신호 시각과 무관하다. 다시 계산하면 방금 낸 신호를 또 낼 수 있어 거른다.
        state.map { it?.let(::Timing) }.distinctUntilChanged().collectLatest { timing ->
            if (timing == null) return@collectLatest
            if (timing.finished) {
                delay(RestTimerRules.FINISHED_DISPLAY_MILLIS)
                mutableState.update { if (it != null && Timing(it) == timing) null else it }
                return@collectLatest
            }
            for (seconds in RestTimerRules.pendingCues(timing.endsAt - now())) {
                val wait = timing.endsAt - seconds * 1000L - now()
                if (wait > 0) delay(wait)
                val cue = RestTimerRules.cueAt(seconds)
                if (cue != RestCue.END) {
                    signal(cue)
                    continue
                }
                var finished: RestTimerState? = null
                mutableState.update { current ->
                    if (current != null && Timing(current) == timing) current.copy(finished = true).also { finished = it } else current
                }
                // 상태가 바뀌어 이 블록이 취소되므로 끝나는 신호와 알림은 취소와 상관없이 끝까지 낸다.
                finished?.let { done ->
                    withContext(NonCancellable) {
                        signal(cue)
                        onFinished(done)
                    }
                }
            }
        }
    }

    /** 신호 시각을 정하는 값만 모은 것. */
    private data class Timing(val workoutId: Long, val setId: Long, val endsAt: Long, val finished: Boolean) {
        constructor(state: RestTimerState) : this(state.workoutId, state.setId, state.endsAt, state.finished)
    }
}
