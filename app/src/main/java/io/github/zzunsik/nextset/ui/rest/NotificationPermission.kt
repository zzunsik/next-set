package io.github.zzunsik.nextset.ui.rest

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import io.github.zzunsik.nextset.domain.rest.NotificationPromptInput
import io.github.zzunsik.nextset.domain.rest.PermissionResult
import io.github.zzunsik.nextset.domain.rest.permissionResultOf
import io.github.zzunsik.nextset.domain.rest.recordsDecline
import io.github.zzunsik.nextset.domain.rest.showsRestBarButton
import io.github.zzunsik.nextset.domain.rest.showsSettingsRow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 알림 켜기 안내(REST-09, docs/rest-timer.md "알림 권한"). 휴식 바의 '알림 켜기'와 설정 화면의 '알림 꺼짐' 행이 쓴다.
 * 앱은 먼저 묻지 않는다. 사용자가 버튼을 눌렀을 때만 시스템의 허용 창을 띄운다.
 */
@Stable
class NotificationPermissionState internal constructor(
    private val input: () -> NotificationPromptInput,
    private val requestFromRestBar: () -> Unit,
    private val enableFromSettings: () -> Unit,
) {
    /** 휴식 바에 '알림 켜기'를 보이는가. */
    val showRestBarButton: Boolean get() = input().showsRestBarButton()

    /** 설정에 '알림 꺼짐' 행을 보이는가. */
    val showSettingsRow: Boolean get() = input().showsSettingsRow()

    /** 휴식 바의 '알림 켜기'. 허용 창을 띄운다. */
    fun requestFromRestBar() = requestFromRestBar.invoke()

    /** 설정의 '알림 켜기'. 허용 창을 띄우고, 창이 뜨지 않는 상태면 기기의 앱 알림 설정을 연다. */
    fun enableFromSettings() = enableFromSettings.invoke()
}

/**
 * @param onAllowed 알림이 켜졌다. 휴식 중이면 알림을 다시 보내게 한다.
 */
@Composable
fun rememberNotificationPermissionState(
    settingsRepository: SettingsRepository,
    onAllowed: () -> Unit = {},
): NotificationPermissionState {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val currentOnAllowed by rememberUpdatedState(onAllowed)
    var enabled by remember { mutableStateOf(notificationsEnabled(context)) }
    // 저장한 값을 읽기 전에는 거절한 것으로 보아 버튼이 잠깐 보였다 사라지지 않게 한다.
    val declined by remember(settingsRepository) {
        settingsRepository.observeSettings().map { it.notificationPromptDeclined }
    }.collectAsStateWithLifecycle(initialValue = true)
    var dismissals by rememberSaveable { mutableIntStateOf(0) }
    var fromSettings by rememberSaveable { mutableStateOf(false) }
    var requestedAt by remember { mutableLongStateOf(0L) }

    // 기기 설정에서 알림을 켜고 돌아오면 허용 상태가 된다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = notificationsEnabled(context)
        if (now && !enabled) currentOnAllowed()
        enabled = now
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val rationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS) } ?: false
        val result = permissionResultOf(granted, rationale)
        enabled = notificationsEnabled(context)
        if (result == PermissionResult.GRANTED) {
            dismissals = 0
            currentOnAllowed()
            return@rememberLauncherForActivityResult
        }
        if (fromSettings) {
            if (result == PermissionResult.DENIED) scope.launch { settingsRepository.setNotificationPromptDeclined(true) }
            // 창 없이 곧바로 돌아왔으면 시스템이 더는 묻지 않는 상태다. 기기의 앱 알림 설정으로 간다.
            if (result == PermissionResult.DISMISSED && SystemClock.elapsedRealtime() - requestedAt < NO_DIALOG_MILLIS) {
                openAppNotificationSettings(context)
            }
            return@rememberLauncherForActivityResult
        }
        if (recordsDecline(result, dismissals)) scope.launch { settingsRepository.setNotificationPromptDeclined(true) }
        dismissals = if (result == PermissionResult.DISMISSED) dismissals + 1 else 0
    }

    return remember(launcher) {
        NotificationPermissionState(
            input = {
                NotificationPromptInput(
                    permissionDialogAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                    notificationsEnabled = enabled,
                    declined = declined,
                )
            },
            requestFromRestBar = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    fromSettings = false
                    requestedAt = SystemClock.elapsedRealtime()
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            enableFromSettings = {
                val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (permissionGranted || activity == null) {
                    // Android 12L 이하이거나 권한은 있는데 기기 설정에서 알림을 끈 경우. 허용 창이 없으니 설정 화면으로 간다.
                    openAppNotificationSettings(context)
                } else {
                    fromSettings = true
                    requestedAt = SystemClock.elapsedRealtime()
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
    }
}

/** 시스템이 허용 창을 띄우지 않고 곧바로 결과를 돌려준 것으로 보는 시간. 사람이 창을 보고 고르기에는 짧다(이 PR의 판단). */
private const val NO_DIALOG_MILLIS = 500L

private fun notificationsEnabled(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

/** 기기의 앱 알림 설정 화면(API 26, `ACTION_APP_NOTIFICATION_SETTINGS`). 열 화면이 없으면 앱 정보 화면을 연다. */
private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
        .apply { if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .apply { if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        )
    }
}
