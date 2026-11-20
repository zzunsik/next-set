package io.github.zzunsik.nextset.rest

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import io.github.zzunsik.nextset.NextSetApplication
import io.github.zzunsik.nextset.domain.rest.RestTimerRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 휴식 동안만 도는 포그라운드 서비스(종류 `specialUse`, docs/rest-timer.md "구현 방식").
 *
 * 앱이 화면에 없어도 프로세스가 살아 있게 하고(REST-06), 화면이 꺼져도 CPU가 돌도록 부분 wake lock을 잡고,
 * 휴식 중 알림을 띄운다(REST-07). 신호는 [RestTimerController]가 낸다. 휴식이 끝나거나 없어지면 스스로 멈춘다.
 * 시스템이 다시 만들지 않는다(START_NOT_STICKY). 다시 만든 서비스가 소리를 낼 권한을 받는지 문서로 확인하지 못했기 때문이다.
 *
 * 멈추기로 한 뒤에는 휴식 중 알림을 다시 올리지 않는다. 상태를 보던 작업을 먼저 끊고, 포그라운드를 내린 뒤 알림을 직접 지운다(#69).
 */
class RestTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private lateinit var wakeLock: PowerManager.WakeLock

    /** 가장 최근에 받은 시작 요청. 멈출 때 이보다 새 요청이 와 있으면 서비스는 이어서 돈다. */
    private var lastStartId = 0

    /** [stop]을 불렀고 새 시작 요청이 오지 않았다. 이 동안에는 알림을 고치지 않는다. */
    private var stopped = false

    private val controller get() = (application as NextSetApplication).container.restTimer
    private val notifications by lazy { RestNotifications(this) }

    override fun onCreate() {
        super.onCreate()
        running = true
        Log.d(TAG, "서비스 시작")
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            .apply { setReferenceCounted(false) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        stopped = false
        val state = controller.state.value
        // startForegroundService() 뒤 5초 안에 불러야 한다. 휴식이 이미 끝났어도 부른 뒤 멈춘다.
        ServiceCompat.startForeground(
            this,
            RestNotifications.ONGOING_ID,
            notifications.ongoing(state, SystemClock.elapsedRealtime(), System.currentTimeMillis()),
            restServiceType(Build.VERSION.SDK_INT),
        )
        if (job == null) {
            job = scope.launch {
                var first = true
                combine(controller.state, controller.notificationRefreshes) { current, _ -> current }.collect { current ->
                    if (stopped) return@collect
                    if (current == null || current.finished) {
                        stop()
                        return@collect
                    }
                    // 놓지 못하는 경우에 대비해 제한 시간을 둔다. 남은 시간은 10분을 넘지 않는다.
                    if (!wakeLock.isHeld) Log.d(TAG, "wake lock 잡음")
                    wakeLock.acquire(RestTimerRules.MAX_REMAINING_MILLIS + WAKE_LOCK_MARGIN_MILLIS)
                    // 처음 값은 방금 startForeground()로 올린 알림과 같다. 같은 알림을 곧바로 한 번 더 보내지 않는다.
                    if (!first && !controller.ongoingDismissed) {
                        notifications.updateOngoing(
                            notifications.ongoing(current, SystemClock.elapsedRealtime(), System.currentTimeMillis()),
                        )
                    }
                    first = false
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun stop() {
        if (stopped) return
        stopped = true
        // 알림을 고치던 작업부터 끊는다. 포그라운드를 내린 뒤 도착한 갱신이 알림을 되살리지 못하게 한다.
        job?.cancel()
        job = null
        if (wakeLock.isHeld) wakeLock.release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        // 시스템이 지우는 것과 별도로 앱도 지운다. 실기기에서 서비스가 끝난 뒤 알림이 남았다(#69).
        notifications.cancelOngoing()
        val stopping = stopSelfResult(lastStartId)
        Log.d(TAG, "서비스 멈춤 요청(멈춤 ${if (stopping) "확정" else "취소: 새 시작 요청이 있음"})")
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        if (wakeLock.isHeld) wakeLock.release()
        notifications.cancelOngoing()
        // 시스템이 포그라운드 알림을 지우는 일은 비동기다. 잠시 뒤에도 휴식이 없는데 알림이 남아 있으면 지운다(#69).
        val app = application as NextSetApplication
        Handler(Looper.getMainLooper()).postDelayed({
            val rest = app.container.restTimer.state.value
            if (!running && (rest == null || rest.finished)) RestNotifications(app).clearLeftoverOngoing()
        }, LEFTOVER_CHECK_DELAY_MILLIS)
        Log.d(TAG, "서비스 끝")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "RestTimer"
        private const val WAKE_LOCK_TAG = "NextSet:RestTimer"
        private const val WAKE_LOCK_MARGIN_MILLIS = 60_000L
        private const val LEFTOVER_CHECK_DELAY_MILLIS = 1_000L

        /** 서비스가 떠 있는가. 프로세스가 죽었다 살아난 뒤 다시 띄울지 정한다. */
        @Volatile
        var running: Boolean = false
            private set
    }
}

/**
 * `startForeground()`에 넘길 서비스 종류. API 34 이상만 `specialUse`이고 그 아래는 0이다.
 * API 29~33에서 허용되는 종류 목록에 `specialUse`가 없다(docs/rest-timer.md "시작 조건").
 */
fun restServiceType(sdkInt: Int): Int =
    if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
