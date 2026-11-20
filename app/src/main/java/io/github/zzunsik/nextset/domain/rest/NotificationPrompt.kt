package io.github.zzunsik.nextset.domain.rest

/**
 * 알림 켜기 안내의 상태(REST-09, docs/rest-timer.md "묻는 방식").
 *
 * @param permissionDialogAvailable 시스템이 앱에 알림 허용 창을 띄워 주는 버전인가(Android 13 이상).
 * @param notificationsEnabled 앱의 알림이 켜져 있는가.
 * @param declined 휴식 바의 '알림 켜기'로 뜬 창에서 거절한 적이 있는가.
 */
data class NotificationPromptInput(
    val permissionDialogAvailable: Boolean,
    val notificationsEnabled: Boolean,
    val declined: Boolean,
)

/** 휴식 바에 '알림 켜기'를 보이는가. 알림이 꺼져 있고 아직 거절한 적이 없을 때만이다. Android 12L 이하에는 묻는 창이 없어 보이지 않는다. */
fun NotificationPromptInput.showsRestBarButton(): Boolean =
    permissionDialogAvailable && !notificationsEnabled && !declined

/** 설정 화면에 '알림 꺼짐' 행을 보이는가. 알림이 꺼져 있으면 어느 버전에서나 보인다. */
fun NotificationPromptInput.showsSettingsRow(): Boolean = !notificationsEnabled

/** 허용 창의 결과. */
enum class PermissionResult {
    GRANTED,

    /** "허용 안함"을 눌렀다. */
    DENIED,

    /** 고르지 않고 창을 닫았거나, 시스템이 창을 띄우지 않고 바로 실패를 돌려줬다. 둘은 앱이 가르지 못한다. */
    DISMISSED,
}

/**
 * 허용 창 결과를 판정한다. 거절이 아니라는 결과는 [shouldShowRationale]로 가른다. true면 "허용 안함"을 누른 것이다(추론, docs/rest-timer.md).
 */
fun permissionResultOf(granted: Boolean, shouldShowRationale: Boolean): PermissionResult = when {
    granted -> PermissionResult.GRANTED
    shouldShowRationale -> PermissionResult.DENIED
    else -> PermissionResult.DISMISSED
}

/**
 * 휴식 바의 '알림 켜기' 결과로 거절을 기록할지. "허용 안함"이면 기록한다. 닫은 것으로 판정한 결과가 연달아 두 번이면
 * 시스템이 창을 띄우지 않는 상태일 수 있어 거절과 같게 다룬다(docs/rest-timer.md "구현할 때 정할 것과 확인할 것").
 *
 * @param previousDismissals 지금 결과 앞에 연달아 나온 '닫음'의 수.
 */
fun recordsDecline(result: PermissionResult, previousDismissals: Int): Boolean = when (result) {
    PermissionResult.GRANTED -> false
    PermissionResult.DENIED -> true
    PermissionResult.DISMISSED -> previousDismissals >= 1
}
