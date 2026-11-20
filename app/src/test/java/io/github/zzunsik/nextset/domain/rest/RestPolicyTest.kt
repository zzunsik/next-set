package io.github.zzunsik.nextset.domain.rest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 벨소리 모드별 신호(REST-05)와 알림 켜기 안내의 상태(REST-09). */
class RestPolicyTest {
    @Test
    fun signalFollowsRingerMode() {
        assertEquals(SignalOutput.SOUND, signalOutputFor(doNotDisturb = false, ringerMode = RingerMode.NORMAL))
        assertEquals(SignalOutput.VIBRATION, signalOutputFor(doNotDisturb = false, ringerMode = RingerMode.VIBRATE))
        assertEquals(SignalOutput.NONE, signalOutputFor(doNotDisturb = false, ringerMode = RingerMode.SILENT))
    }

    @Test
    fun doNotDisturbSilencesEveryMode() {
        RingerMode.entries.forEach { mode ->
            assertEquals(mode.name, SignalOutput.NONE, signalOutputFor(doNotDisturb = true, ringerMode = mode))
        }
    }

    @Test
    fun cueKinds() {
        assertEquals(listOf(10, 5, 4, 3, 2, 1, 0), RestTimerRules.cueSeconds)
        assertEquals(RestCue.WARNING, RestTimerRules.cueAt(10))
        (1..5).forEach { assertEquals(RestCue.COUNTDOWN, RestTimerRules.cueAt(it)) }
        assertEquals(RestCue.END, RestTimerRules.cueAt(0))
        assertEquals(listOf(4, 3, 2, 1, 0), RestTimerRules.pendingCues(4_500))
    }

    @Test
    fun restBarButtonOnlyWhenOffAndNeverDeclinedOnAndroid13Plus() {
        val off = NotificationPromptInput(permissionDialogAvailable = true, notificationsEnabled = false, declined = false)
        assertTrue(off.showsRestBarButton())
        assertTrue(off.showsSettingsRow())
        assertFalse("허용하면 사라진다", off.copy(notificationsEnabled = true).showsRestBarButton())
        assertFalse(off.copy(notificationsEnabled = true).showsSettingsRow())
        assertFalse("거절하면 버튼을 숨기고", off.copy(declined = true).showsRestBarButton())
        assertTrue("설정에서만 안내한다", off.copy(declined = true).showsSettingsRow())
    }

    @Test
    fun android12LAndBelowNeverShowsRestBarButton() {
        val off = NotificationPromptInput(permissionDialogAvailable = false, notificationsEnabled = false, declined = false)
        assertFalse(off.showsRestBarButton())
        assertTrue("시스템 설정으로 끈 알림은 설정 화면에서 안내한다", off.showsSettingsRow())
    }

    @Test
    fun permissionResults() {
        assertEquals(PermissionResult.GRANTED, permissionResultOf(granted = true, shouldShowRationale = false))
        assertEquals(PermissionResult.DENIED, permissionResultOf(granted = false, shouldShowRationale = true))
        assertEquals(PermissionResult.DISMISSED, permissionResultOf(granted = false, shouldShowRationale = false))
    }

    @Test
    fun declineIsRecordedForDenyOrTwoDismissalsInARow() {
        assertTrue(recordsDecline(PermissionResult.DENIED, previousDismissals = 0))
        assertFalse("창을 닫기만 한 것은 거절이 아니다", recordsDecline(PermissionResult.DISMISSED, previousDismissals = 0))
        assertTrue("닫음이 연달아 두 번이면 거절과 같다", recordsDecline(PermissionResult.DISMISSED, previousDismissals = 1))
        assertFalse(recordsDecline(PermissionResult.GRANTED, previousDismissals = 3))
    }
}
