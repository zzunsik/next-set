package io.github.zzunsik.nextset.ui.settings

import io.github.zzunsik.nextset.domain.model.OpenSourceNotice
import io.github.zzunsik.nextset.domain.model.Settings
import io.github.zzunsik.nextset.domain.model.ThemeMode
import io.github.zzunsik.nextset.domain.repository.LicenseRepository
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

/** 화면 상태 테스트용 설정 저장소. [emit]을 부르기 전까지 값을 내보내지 않게 할 수 있다. */
class FakeSettingsRepository(initial: Settings? = Settings()) : SettingsRepository {
    val settings = MutableStateFlow(initial)

    override fun observeSettings(): Flow<Settings> = settings.filterNotNull()

    override suspend fun setDefaultRestSeconds(seconds: Int) {
        require(Settings.isValidDefaultRest(seconds))
        update { it.copy(defaultRestSeconds = seconds) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    override suspend fun setPinnedRoutine(routineId: Long?) = update { it.copy(pinnedRoutineId = routineId) }

    override suspend fun setNotificationPromptDeclined(declined: Boolean) =
        update { it.copy(notificationPromptDeclined = declined) }

    private fun update(change: (Settings) -> Settings) {
        settings.value = change(settings.value ?: Settings())
    }
}

/** 지어낸 라이선스 두 건. */
class FakeLicenseRepository : LicenseRepository {
    val notices = listOf(
        OpenSourceNotice(id = "font", name = "Test Font", author = "Test Author", licenseName = "Test License 1.0"),
        OpenSourceNotice(id = "lib", name = "Test Library", author = "Test Org", licenseName = "Apache License 2.0"),
    )

    override suspend fun notices(): List<OpenSourceNotice> = notices

    override suspend fun licenseText(id: String): String {
        val notice = notices.firstOrNull { it.id == id } ?: throw NoSuchElementException(id)
        return "${notice.name} license text.\nLine two of the made-up license."
    }
}
