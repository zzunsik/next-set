package io.github.zzunsik.nextset.rest

import io.github.zzunsik.nextset.domain.rest.RestCue
import io.github.zzunsik.nextset.domain.rest.RestTimer
import io.github.zzunsik.nextset.domain.rest.RestTimerEngine
import io.github.zzunsik.nextset.domain.rest.RestTimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 컨트롤러가 바깥에 시키는 일. 앱에서는 서비스, 알림, 소리 장치이고 테스트는 기록만 한다. */
interface RestTimerEffects {
    /** 휴식이 시작됐다. 포그라운드 서비스를 띄운다. 띄우지 못해도(백그라운드 시작 제한) 앱 안의 타이머는 돈다. */
    fun startService()

    fun signal(cue: RestCue)

    /** 휴식이 끝나 신호를 낸 뒤. 끝 알림을 띄운다. */
    fun postDone(state: RestTimerState)

    fun cancelDone()

    /** 휴식이 없어졌다(끝, 건너뛰기, 취소). 소리 장치를 놓는다. 서비스는 상태를 보고 스스로 멈춘다. */
    fun released()

    fun save(state: RestTimerState?)

    fun load(): RestTimerState?
}

/**
 * 앱에 하나 있는 휴식 타이머(docs/rest-timer.md "구현 방식"). 상태는 [RestTimerEngine]이 갖고, 여기서는 바깥 일을 잇는다.
 *
 * - 시작: 화면에서 세트를 완료할 때(앱이 보일 때) 서비스를 띄운다. 서비스가 알림과 wake lock을 쥐고 이 객체의 상태를 따라간다.
 * - 신호: [scope]에서 엔진의 신호 시각마다 [RestTimerEffects.signal]을 부른다. 앱이 화면에 있든 없든 같은 코드다.
 * - 저장: 상태가 바뀔 때마다 끝나는 시각을 남긴다. 프로세스가 죽은 뒤 앱을 열면 [onAppVisible]이 서비스를 다시 띄운다.
 */
class RestTimerController(
    private val engine: RestTimerEngine,
    private val scope: CoroutineScope,
    private val effects: RestTimerEffects,
) : RestTimer {
    override val state: StateFlow<RestTimerState?> = engine.state

    private val refreshes = MutableStateFlow(0)

    /** 휴식 중 알림을 다시 보내야 할 때마다 바뀐다. 서비스가 본다. */
    val notificationRefreshes: StateFlow<Int> = refreshes.asStateFlow()

    /** 사용자가 이번 휴식의 알림을 지웠다. 그 휴식이 끝날 때까지 다시 띄우지 않는다(docs/rest-timer.md). */
    @Volatile
    private var appVisible = false

    @Volatile
    var ongoingDismissed: Boolean = false
        private set

    init {
        effects.load()?.let(engine::restore)
        scope.launch {
            var previous: RestTimerState? = null
            engine.state.collect { current ->
                effects.save(current)
                if (previous != null && current == null) {
                    ongoingDismissed = false
                    effects.released()
                }
                previous = current
            }
        }
        scope.launch {
            engine.runCues(
                signal = { effects.signal(it) },
                // 앱이 화면에 있으면 바에 "0:00"이 보이므로 끝 알림을 띄우지 않는다.
                onFinished = { if (!appVisible) effects.postDone(it) },
            )
        }
    }

    override fun now(): Long = engine.now()

    override fun start(workoutId: Long, setId: Long, seconds: Int) {
        effects.cancelDone()
        ongoingDismissed = false
        engine.start(workoutId, setId, seconds)
        effects.startService()
    }

    override fun adjust(deltaSeconds: Int) = engine.adjust(deltaSeconds)

    override fun skip() = engine.skip()

    override fun stopForSet(setId: Long) = engine.stopForSet(setId)

    override fun cancel() {
        engine.skip()
        effects.cancelDone()
    }

    override fun setNextSet(text: String?) = engine.setNextSet(text)

    override fun onNotificationsAllowed() {
        ongoingDismissed = false
        refreshes.update { it + 1 }
    }

    fun onOngoingDismissed() {
        ongoingDismissed = true
    }

    /**
     * 앱이 화면에 나왔다. 끝 알림을 지우고(docs/rest-timer.md "휴식이 끝났을 때" 4번), 휴식 중인데 서비스가 없으면
     * (프로세스가 죽었다 살아남) 서비스를 다시 띄운다.
     */
    fun onAppVisible(serviceRunning: Boolean) {
        appVisible = true
        effects.cancelDone()
        val current = engine.state.value
        if (current != null && !current.finished && !serviceRunning) effects.startService()
    }

    fun onAppHidden() {
        appVisible = false
    }
}
