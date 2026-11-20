package io.github.zzunsik.nextset.rest

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.zzunsik.nextset.NextSetApplication
import io.github.zzunsik.nextset.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * 서비스가 멈춘 뒤 휴식 중 알림(id=1)이 남지 않는다(#69). 실기기에서 서비스가 끝난 뒤에도
 * `ONGOING|NO_CLEAR|FOREGROUND_SERVICE` 알림이 지워지지 않고 남았다.
 */
@RunWith(RobolectricTestRunner::class)
class RestTimerServiceTest {
    private val app = RuntimeEnvironment.getApplication() as NextSetApplication
    private val timer = app.container.restTimer
    private val manager = app.getSystemService(NotificationManager::class.java)

    private fun idleMain() = shadowOf(Looper.getMainLooper()).idle()

    private fun ongoing() = shadowOf(manager).getNotification(RestNotifications.ONGOING_ID)

    @Test
    fun stoppedServiceNeverPostsTheOngoingNotificationAgain() {
        timer.start(workoutId = 1, setId = 10, seconds = 60)
        val controller = Robolectric.buildService(RestTimerService::class.java).create().startCommand(0, 1)
        idleMain()
        assertNotNull("휴식 중에는 알림이 있다", ongoing())

        timer.skip()
        idleMain()
        assertTrue(shadowOf(controller.get()).isStoppedBySelf)
        assertNull("멈추면 알림이 지워진다", ongoing())

        // stopSelf() 뒤 onDestroy()가 오기 전에 상태가 바뀌어도(새 휴식, 알림 다시 보내기) 멈춘 서비스는 알림을 올리지 않는다.
        // 새 휴식의 알림은 새 onStartCommand()가 startForeground()로 올린다.
        timer.start(workoutId = 1, setId = 11, seconds = 60)
        timer.onNotificationsAllowed()
        idleMain()
        assertNull("멈춘 서비스가 알림을 다시 올렸다", ongoing())
        controller.destroy()
    }

    @Test
    @SuppressLint("MissingPermission")
    fun releasingTheRestClearsALeftoverOngoingNotification() {
        // 시스템이 지우지 못하고 남긴 휴식 중 알림을 흉내 낸다. 서비스는 떠 있지 않다.
        val leftover = NotificationCompat.Builder(app, RestNotifications.CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_rest_timer)
            .setOngoing(true)
            .build()
        RestNotifications(app).ensureChannels()
        NotificationManagerCompat.from(app).notify(RestNotifications.ONGOING_ID, leftover)
        assertNotNull(ongoing())

        AndroidRestTimerEffects(app).released()

        assertNull("휴식이 없어졌는데 휴식 중 알림이 남았다", ongoing())
    }
}
