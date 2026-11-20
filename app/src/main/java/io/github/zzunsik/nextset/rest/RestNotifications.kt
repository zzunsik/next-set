package io.github.zzunsik.nextset.rest

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.zzunsik.nextset.MainActivity
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.rest.RestTimerRules
import io.github.zzunsik.nextset.domain.rest.RestTimerState

/**
 * 휴식 알림(REST-07, REST-08). 휴식 중 알림 하나와 휴식 끝 알림 하나다(docs/rest-timer.md "Live Updates").
 *
 * 휴식 중 알림은 남은 시간을 시스템이 세는 카운트다운으로 보여 주고 승격(Live Updates)을 요청한다. 앱은 알림을 1초마다 고치지 않는다.
 * 승격 여부는 기기가 정하고, 승격되지 않으면 같은 알림이 일반 알림으로 보인다. 알림 권한이 없으면 아무것도 보이지 않는다.
 */
class RestNotifications(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    /** 채널을 만든다. 이미 있으면 그대로다. */
    fun ensureChannels() {
        manager.createNotificationChannel(
            // 신호는 앱이 직접 내므로 휴식 중 채널은 조용해야 한다.
            NotificationChannelCompat.Builder(CHANNEL_ONGOING, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.rest_channel_ongoing))
                .setShowBadge(false)
                .build(),
        )
        manager.createNotificationChannel(
            // 끝 알림은 다른 앱을 쓰는 중에도 보이도록 화면 위에 잠깐 띄운다. 소리와 진동은 앱이 낸 끝 신호로 갈음한다.
            NotificationChannelCompat.Builder(CHANNEL_DONE, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.rest_channel_done))
                .setSound(null, null)
                .setVibrationEnabled(false)
                .setShowBadge(false)
                .build(),
        )
    }

    /** 알림을 보일 수 있는가(권한과 앱 알림 설정). */
    fun enabled(): Boolean = manager.areNotificationsEnabled()

    /**
     * 휴식 중 알림. [nowElapsed]와 [nowWall]은 같은 순간의 단조 시계와 벽시계다. 카운트다운은 벽시계의 끝나는 시각까지 센다.
     */
    fun ongoing(state: RestTimerState?, nowElapsed: Long, nowWall: Long): Notification {
        ensureChannels()
        val builder = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_rest_timer)
            .setContentTitle(context.getString(R.string.rest_notification_title))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppIntent())
            .setDeleteIntent(RestTimerActionReceiver.pendingIntent(context, RestTimerActionReceiver.ACTION_DISMISSED))
            .setRequestPromotedOngoing(true)
        if (state != null) {
            state.nextSet?.let { builder.setContentText(context.getString(R.string.rest_next_set, it)) }
            builder
                .setWhen(nowWall + state.remainingMillis(nowElapsed))
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(0, context.getString(R.string.rest_decrease, RestTimerRules.STEP_SECONDS), action(RestTimerActionReceiver.ACTION_DECREASE))
                .addAction(0, context.getString(R.string.rest_increase, RestTimerRules.STEP_SECONDS), action(RestTimerActionReceiver.ACTION_INCREASE))
                .addAction(0, context.getString(R.string.rest_skip), action(RestTimerActionReceiver.ACTION_SKIP))
        }
        return builder.build()
    }

    /** 휴식 중 알림을 고친다. 서비스가 이미 같은 ID로 띄워 둔 알림이다. */
    @SuppressLint("MissingPermission")
    fun updateOngoing(notification: Notification) {
        if (enabled()) manager.notify(ONGOING_ID, notification)
    }

    /** 휴식 끝 알림. 승격을 요청하지 않는 일반 알림이고 [DONE_TIMEOUT_MILLIS] 뒤 저절로 지워진다. */
    fun done(state: RestTimerState): Notification {
        ensureChannels()
        return NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_rest_timer)
            .setContentTitle(context.getString(R.string.rest_done_title))
            .apply { state.nextSet?.let { setContentText(context.getString(R.string.rest_next_set, it)) } }
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setAutoCancel(true)
            .setTimeoutAfter(DONE_TIMEOUT_MILLIS)
            .setContentIntent(openAppIntent())
            .build()
    }

    @SuppressLint("MissingPermission")
    fun postDone(state: RestTimerState) {
        if (enabled()) manager.notify(DONE_ID, done(state))
    }

    fun cancelDone() = manager.cancel(DONE_ID)

    private fun action(name: String) = RestTimerActionReceiver.pendingIntent(context, name)

    /** 알림을 누르면 앱을 연다. 앱이 이미 떠 있으면 그 화면으로 돌아간다. */
    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        const val CHANNEL_ONGOING = "rest_timer"
        const val CHANNEL_DONE = "rest_done"
        const val ONGOING_ID = 1
        const val DONE_ID = 2

        /** 끝 알림이 저절로 지워지기까지. 다음 세트를 하러 돌아올 만한 시간이다(이 PR의 판단). */
        const val DONE_TIMEOUT_MILLIS = 5 * 60 * 1000L
    }
}
