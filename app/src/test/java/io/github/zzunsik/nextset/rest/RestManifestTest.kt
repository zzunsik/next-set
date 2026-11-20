package io.github.zzunsik.nextset.rest

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * 휴식 타이머의 매니페스트 선언(docs/rest-timer.md "권한과 매니페스트 선언"). 쓰기로 한 권한만 있고 받지 않기로 한 권한은 없다.
 * 인터넷 권한은 `ManifestPermissionTest`가 본다(DATA-02).
 */
@RunWith(RobolectricTestRunner::class)
class RestManifestTest {
    private val app = RuntimeEnvironment.getApplication()

    private val requested: List<String> =
        app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toList()

    @Test
    fun declaresOnlyTheRestTimerPermissions() {
        val expected = listOf(
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.POST_PROMOTED_NOTIFICATIONS",
            "android.permission.WAKE_LOCK",
            "android.permission.VIBRATE",
        )
        expected.forEach { assertTrue("$it 가 없다: $requested", it in requested) }
        val notWanted = listOf(
            "android.permission.SCHEDULE_EXACT_ALARM",
            "android.permission.USE_EXACT_ALARM",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
            "android.permission.FOREGROUND_SERVICE_HEALTH",
            "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
            "android.permission.ACCESS_NOTIFICATION_POLICY",
        )
        notWanted.forEach { assertFalse("$it 는 받지 않기로 했다", it in requested) }
    }

    @Test
    fun serviceIsPrivateSpecialUse() {
        val info = app.packageManager.getServiceInfo(ComponentName(app, RestTimerService::class.java), PackageManager.GET_META_DATA)
        assertFalse(info.exported)
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE, info.foregroundServiceType)
    }

    @Test
    fun receiverIsPrivate() {
        val info = app.packageManager.getReceiverInfo(ComponentName(app, RestTimerActionReceiver::class.java), 0)
        assertFalse(info.exported)
    }
}
