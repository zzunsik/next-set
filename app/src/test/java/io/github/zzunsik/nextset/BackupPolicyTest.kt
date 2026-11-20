package io.github.zzunsik.nextset

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.xmlpull.v1.XmlPullParser

/**
 * Android 자동 백업(클라우드 백업)과 기기 간 직접 전송에 앱 데이터를 넣지 않는다(DATA-05).
 * 매니페스트의 `allowBackup`이 꺼져 있고, 규칙 파일이 두 경로 모두에서 모든 영역을 뺀다.
 */
@RunWith(RobolectricTestRunner::class)
class BackupPolicyTest {
    private val app = RuntimeEnvironment.getApplication()

    @Test
    fun autoBackupIsOff() {
        assertEquals(0, app.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    @Test
    fun extractionRulesExcludeEveryDomainForCloudAndDeviceTransfer() {
        val excluded = mutableMapOf<String, MutableSet<String>>()
        var section: String? = null
        val parser = app.resources.getXml(R.xml.data_extraction_rules)
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "cloud-backup", "device-transfer" -> section = parser.name
                "include" -> throw AssertionError("넣는 규칙이 있다: $section")
                "exclude" -> excluded.getOrPut(checkNotNull(section)) { mutableSetOf() } +=
                    parser.getAttributeValue(null, "domain") ?: parser.getAttributeValue(ANDROID_NS, "domain")
            }
        }
        val all = setOf("root", "file", "database", "sharedpref", "external")
        for (name in listOf("cloud-backup", "device-transfer")) {
            assertTrue("$name: ${excluded[name]}", excluded[name].orEmpty().containsAll(all))
        }
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
