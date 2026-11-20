package io.github.zzunsik.nextset.rest

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import io.github.zzunsik.nextset.domain.rest.RestCue
import io.github.zzunsik.nextset.domain.rest.RestTimerState

/** [RestTimerController]의 바깥 일을 안드로이드로 한다. */
class AndroidRestTimerEffects(
    private val context: Context,
    private val store: RestTimerStore = RestTimerStore(context),
    private val signaler: RestSignaler = RestSignaler(context),
    private val notifications: RestNotifications = RestNotifications(context),
    private val elapsed: () -> Long = SystemClock::elapsedRealtime,
    private val wall: () -> Long = System::currentTimeMillis,
) : RestTimerEffects {
    override fun startService() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, RestTimerService::class.java))
        } catch (e: IllegalStateException) {
            // API 31 이상에서 앱이 보이지 않을 때 시작하면 ForegroundServiceStartNotAllowedException(IllegalStateException)이다.
            // 앱 안의 타이머와 신호는 그대로 돈다. 앱이 다시 보이면 onAppVisible이 다시 띄운다.
            Log.w(TAG, "휴식 타이머 서비스를 띄우지 못했다", e)
        }
    }

    override fun signal(cue: RestCue) {
        // 기기에서 신호 시각과 방법을 확인할 수 있게 남긴다(adb logcat -s RestTimer).
        Log.d(TAG, "신호 $cue ${signaler.output()}")
        signaler.signal(cue)
    }

    override fun postDone(state: RestTimerState) = notifications.postDone(state)

    override fun cancelDone() = notifications.cancelDone()

    override fun released() = signaler.release()

    override fun save(state: RestTimerState?) = store.save(state, elapsed(), wall())

    override fun load(): RestTimerState? = store.load(elapsed(), wall())

    private companion object {
        const val TAG = "RestTimer"
    }
}
