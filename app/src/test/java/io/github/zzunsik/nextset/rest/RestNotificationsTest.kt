package io.github.zzunsik.nextset.rest

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import io.github.zzunsik.nextset.domain.rest.RestTimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** 휴식 알림의 내용(REST-07, REST-08)과 Android 버전에 따른 분기. 종목과 값은 지어낸 것이다. */
@RunWith(RobolectricTestRunner::class)
class RestNotificationsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifications = RestNotifications(context)
    private val manager = context.getSystemService(NotificationManager::class.java)

    private val state = RestTimerState(
        workoutId = 1,
        setId = 10,
        endsAt = 100_000,
        totalMillis = 120_000,
        nextSet = "벤치 프레스 60kg × 10",
    )

    @Test
    fun ongoingShowsNextSetAndCountsDownToEndTime() {
        // 단조 시계 10초, 벽시계 1,000,000일 때 남은 시간은 90초다.
        val notification = notifications.ongoing(state, nowElapsed = 10_000, nowWall = 1_000_000)
        val extras = notification.extras

        assertEquals("휴식 중", extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("다음 벤치 프레스 60kg × 10", extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals("카운트다운은 벽시계의 끝나는 시각까지 센다", 1_090_000L, notification.`when`)
        assertTrue(extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertTrue(extras.getBoolean(Notification.EXTRA_SHOW_WHEN))
        assertEquals(listOf("−15초", "+15초", "건너뛰기"), notification.actions.map { it.title.toString() })
        assertNotNull(notification.deleteIntent)
        assertNotNull(notification.contentIntent)
    }

    @Test
    fun ongoingMeetsLiveUpdateConditions() {
        val notification = notifications.ongoing(state, nowElapsed = 10_000, nowWall = 1_000_000)
        // docs/rest-timer.md "승격 조건": ongoing, 제목 있음, 승격 요청, 색 채움 없음, 커스텀 뷰 없음, 칩 글자를 따로 넣지 않음.
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(NotificationCompat.isRequestPromotedOngoing(notification))
        assertFalse(notification.extras.getBoolean(Notification.EXTRA_COLORIZED))
        assertNull(notification.contentView)
        assertNull(NotificationCompat.getShortCriticalText(notification))
        assertEquals(NotificationCompat.VISIBILITY_PUBLIC, notification.visibility)
        assertEquals(RestNotifications.CHANNEL_ONGOING, notification.channelId)
    }

    @Test
    @Config(sdk = [37])
    fun ongoingHasPromotableCharacteristicsByPlatformCheck() {
        // #70: 조건 목록을 따로 검사하는 위 테스트와 달리 플랫폼 자체의 판정을 본다. 사용자 설정과 제조사 조건은 보지 않는다.
        // SDK 37로 돈다. Robolectric 4.18의 SDK 36 이미지는 Android 16 초기 판정(색 채움을 요구함)이라 문서와 다르다(docs/rest-timer.md #70).
        val notification = notifications.ongoing(state, nowElapsed = 10_000, nowWall = 1_000_000)
        assertTrue("휴식 중 알림은 승격 조건을 모두 갖춰야 한다", notification.hasPromotableCharacteristics())

        // 판정이 실제로 조건을 보는지 확인한다. 같은 알림에서 ongoing만 빼면 승격 대상이 아니다.
        val notOngoing = NotificationCompat.Builder(context, RestNotifications.CHANNEL_ONGOING)
            .setSmallIcon(io.github.zzunsik.nextset.R.drawable.ic_rest_timer)
            .setContentTitle("휴식 중")
            .setRequestPromotedOngoing(true)
            .build()
        assertFalse(notOngoing.hasPromotableCharacteristics())
    }

    @Test
    fun channelsAreQuietBecauseTheAppSignalsItself() {
        notifications.ensureChannels()
        val ongoing = manager.getNotificationChannel(RestNotifications.CHANNEL_ONGOING)
        assertEquals(NotificationManager.IMPORTANCE_LOW, ongoing.importance)
        val done = manager.getNotificationChannel(RestNotifications.CHANNEL_DONE)
        assertNull(done.sound)
        assertFalse(done.shouldVibrate())
        assertTrue("휴식 중 채널은 IMPORTANCE_MIN이 아니어야 승격된다", ongoing.importance > NotificationManager.IMPORTANCE_MIN)
    }

    @Test
    fun doneIsPlainNotificationThatExpires() {
        val done = notifications.done(state.copy(finished = true))
        assertEquals("휴식 끝", done.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("다음 벤치 프레스 60kg × 10", done.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertFalse(NotificationCompat.isRequestPromotedOngoing(done))
        assertTrue(done.flags and Notification.FLAG_ONGOING_EVENT == 0)
        assertTrue(done.flags and Notification.FLAG_AUTO_CANCEL != 0)
        assertEquals(RestNotifications.DONE_TIMEOUT_MILLIS, done.timeoutAfter)
    }

    @Test
    fun doneIsPostedOnlyWhenNotificationsAreAllowed() {
        shadowOf(manager).setNotificationsEnabled(false)
        notifications.postDone(state)
        assertEquals(0, shadowOf(manager).size())

        shadowOf(manager).setNotificationsEnabled(true)
        notifications.postDone(state)
        assertNotNull(shadowOf(manager).getNotification(RestNotifications.DONE_ID))
        notifications.cancelDone()
        assertNull(shadowOf(manager).getNotification(RestNotifications.DONE_ID))
    }

    @Test
    fun serviceTypeIsSpecialUseOnlyFromAndroid14() {
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE, restServiceType(Build.VERSION_CODES.UPSIDE_DOWN_CAKE))
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE, restServiceType(37))
        assertEquals(0, restServiceType(Build.VERSION_CODES.TIRAMISU))
        assertEquals(0, restServiceType(Build.VERSION_CODES.O))
    }
}
