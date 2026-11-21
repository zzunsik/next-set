package io.github.zzunsik.nextset

import android.Manifest
import android.content.pm.PackageManager
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * 앱은 인터넷 권한을 요청하지 않는다(DATA-02).
 * Robolectric은 병합된 매니페스트를 읽으므로 의존성이 권한을 끌어오면 이 테스트가 실패한다.
 * 릴리스 빌드의 매니페스트는 CI가 따로 검사한다.
 */
@RunWith(RobolectricTestRunner::class)
class ManifestPermissionTest {
    @Test
    fun doesNotRequestInternetPermission() {
        val app = RuntimeEnvironment.getApplication()
        val info = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions.orEmpty().toList()

        assertFalse("요청 권한: $requested", Manifest.permission.INTERNET in requested)
    }
}
