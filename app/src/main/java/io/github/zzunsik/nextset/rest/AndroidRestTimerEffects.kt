package io.github.zzunsik.nextset.rest

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
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

    override fun released() {
        signaler.release()
        // 서비스가 이미 없으면 휴식 중 알림도 없어야 한다. 서비스가 아직 있으면 서비스가 멈추며 지운다(#69).
        if (!RestTimerService.running) notifications.clearLeftoverOngoing()
    }

    override fun save(state: RestTimerState?) = store.save(state, elapsed(), wall())

    /** 프로세스가 뜬 뒤 처음 부른다. 이어 갈 휴식이 없으면 지난 프로세스가 남긴 휴식 중 알림을 지운다(#69). */
    override fun load(): RestTimerState? {
        logLastExit()
        val saved = store.load(elapsed(), wall())
        if (!RestTimerService.running && (saved == null || saved.endsAt <= elapsed())) notifications.clearLeftoverOngoing()
        return saved
    }

    /**
     * 지난번 프로세스가 끝난 이유를 남긴다(API 30 이상). 휴식 중에 프로세스가 죽었는지 기기에서 확인하는 데 쓴다(#69).
     * 시각, 이유 코드, 시스템이 붙인 설명만 적는다.
     */
    private fun logLastExit() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        try {
            val exit = context.getSystemService(ActivityManager::class.java)
                ?.getHistoricalProcessExitReasons(context.packageName, 0, 1)
                ?.firstOrNull() ?: return
            Log.d(TAG, "지난 프로세스 종료 시각=${exit.timestamp} 이유=${exit.reason} 중요도=${exit.importance} 설명=${exit.description}")
        } catch (e: RuntimeException) {
            Log.w(TAG, "지난 프로세스 종료 이유를 읽지 못했다", e)
        }
    }

    private companion object {
        const val TAG = "RestTimer"
    }
}
