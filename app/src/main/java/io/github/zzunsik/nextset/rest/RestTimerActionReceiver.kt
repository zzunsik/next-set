package io.github.zzunsik.nextset.rest

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.zzunsik.nextset.NextSetApplication
import io.github.zzunsik.nextset.domain.rest.RestTimerRules

/**
 * 휴식 중 알림의 버튼(시간 줄이기, 늘리기, 건너뛰기)과 사용자가 알림을 지운 것을 받는다.
 * 화면을 열지 않는다(docs/rest-timer.md "버튼(액션)": targetSdk 31 이상에서 리시버가 액티비티를 여는 것은 막힌다).
 */
class RestTimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timer = (context.applicationContext as NextSetApplication).container.restTimer
        when (intent.action) {
            ACTION_DECREASE -> timer.adjust(-RestTimerRules.STEP_SECONDS)
            ACTION_INCREASE -> timer.adjust(RestTimerRules.STEP_SECONDS)
            ACTION_SKIP -> timer.skip()
            ACTION_DISMISSED -> timer.onOngoingDismissed()
        }
    }

    companion object {
        const val ACTION_DECREASE = "io.github.zzunsik.nextset.rest.DECREASE"
        const val ACTION_INCREASE = "io.github.zzunsik.nextset.rest.INCREASE"
        const val ACTION_SKIP = "io.github.zzunsik.nextset.rest.SKIP"
        const val ACTION_DISMISSED = "io.github.zzunsik.nextset.rest.DISMISSED"

        private val REQUEST_CODES = mapOf(ACTION_DECREASE to 1, ACTION_INCREASE to 2, ACTION_SKIP to 3, ACTION_DISMISSED to 4)

        /** 이 앱 안에서만 받는 명시적 인텐트. 내용을 바꿀 수 없다(FLAG_IMMUTABLE, docs/rest-timer.md). */
        fun pendingIntent(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODES.getValue(action),
            Intent(context, RestTimerActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
