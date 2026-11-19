package io.github.zzunsik.nextset.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val defaultRestSeconds: Int = DEFAULT_REST_SECONDS,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** 직접 고른 맨 위 루틴(HOME-03). */
    val pinnedRoutineId: Long? = null,
    val notificationPromptDeclined: Boolean = false,
) {
    companion object {
        const val DEFAULT_REST_SECONDS = 120
        const val MIN_REST_SECONDS = 30
        const val MAX_REST_SECONDS = 300
        const val REST_STEP_SECONDS = 15

        /** SET-01: 30초부터 5분까지 15초 단위. */
        fun isValidDefaultRest(seconds: Int): Boolean =
            seconds in MIN_REST_SECONDS..MAX_REST_SECONDS && seconds % REST_STEP_SECONDS == 0
    }
}
