package io.github.zzunsik.nextset.rest

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
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
 */
class RestTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private lateinit var wakeLock: PowerManager.WakeLock

    private val controller get() = (application as NextSetApplication).container.restTimer
    private val notifications by lazy { RestNotifications(this) }

    override fun onCreate() {
        super.onCreate()
        running = true
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            .apply { setReferenceCounted(false) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
                combine(controller.state, controller.notificationRefreshes) { current, _ -> current }.collect { current ->
                    if (current == null || current.finished) {
                        stop()
                        return@collect
                    }
                    // 놓지 못하는 경우에 대비해 제한 시간을 둔다. 남은 시간은 10분을 넘지 않는다.
                    wakeLock.acquire(RestTimerRules.MAX_REMAINING_MILLIS + WAKE_LOCK_MARGIN_MILLIS)
                    if (!controller.ongoingDismissed) {
                        notifications.updateOngoing(
                            notifications.ongoing(current, SystemClock.elapsedRealtime(), System.currentTimeMillis()),
                        )
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun stop() {
        if (wakeLock.isHeld) wakeLock.release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        if (wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val WAKE_LOCK_TAG = "NextSet:RestTimer"
        private const val WAKE_LOCK_MARGIN_MILLIS = 60_000L

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
