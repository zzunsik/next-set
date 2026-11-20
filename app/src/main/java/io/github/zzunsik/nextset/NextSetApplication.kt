package io.github.zzunsik.nextset

import android.app.Application
import android.app.UiModeManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** 앱이 살아 있는 동안 [AppContainer] 하나를 쥔다. */
class NextSetApplication : Application() {
    lateinit var container: AppContainer
        private set

    /** 앱이 살아 있는 동안 도는 작업. 프로세스와 함께 끝난다. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // 시작 화면이 앱의 화면 모드를 따르게 한다(#54). API 26~30에는 이 API가 없어 시작 화면이 시스템 설정을 따른다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val uiModeManager = getSystemService(UiModeManager::class.java)
            SystemNightModeSync(container.settingsRepository, uiModeManager::setApplicationNightMode)
                .start(applicationScope)
        }
    }
}
