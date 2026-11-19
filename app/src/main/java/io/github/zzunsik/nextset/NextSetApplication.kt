package io.github.zzunsik.nextset

import android.app.Application

/** 앱이 살아 있는 동안 [AppContainer] 하나를 쥔다. */
class NextSetApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
