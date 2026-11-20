package io.github.zzunsik.nextset

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import io.github.zzunsik.nextset.data.backup.BackupApp
import io.github.zzunsik.nextset.data.backup.RoomBackupRepository
import io.github.zzunsik.nextset.data.db.NextSetDatabase
import io.github.zzunsik.nextset.data.repository.AssetLicenseRepository
import io.github.zzunsik.nextset.data.repository.RoomExerciseRepository
import io.github.zzunsik.nextset.data.repository.RoomRoutineRepository
import io.github.zzunsik.nextset.data.repository.RoomSettingsRepository
import io.github.zzunsik.nextset.data.repository.RoomWorkoutRepository
import io.github.zzunsik.nextset.data.seed.BuiltinExerciseSeeder
import io.github.zzunsik.nextset.domain.repository.BackupRepository
import io.github.zzunsik.nextset.domain.repository.ExerciseRepository
import io.github.zzunsik.nextset.domain.repository.LicenseRepository
import io.github.zzunsik.nextset.domain.repository.RoutineRepository
import io.github.zzunsik.nextset.domain.repository.SettingsRepository
import io.github.zzunsik.nextset.domain.repository.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 앱이 쓰는 객체를 만드는 곳. 의존성 주입 라이브러리 없이 여기서 직접 만들어 넘긴다
 * (docs/architecture.md "의존성 주입"). 화면 상태는 저장소 인터페이스만 받는다.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /**
     * 처음 쓸 때 연다. 첫 화면이 화면 모드를 읽고, API 31 이상에서는 `SystemNightModeSync`도 앱 시작 때 읽는다(#54).
     */
    private val database: NextSetDatabase by lazy { NextSetDatabase.build(appContext) }

    /** 내장 종목은 종목을 처음 읽을 때 반영한다. */
    private val seeder: BuiltinExerciseSeeder by lazy {
        BuiltinExerciseSeeder(database) {
            withContext(Dispatchers.IO) {
                appContext.assets.open(BuiltinExerciseSeeder.ASSET_NAME).bufferedReader().use { it.readText() }
            }
        }
    }

    val exerciseRepository: ExerciseRepository by lazy { RoomExerciseRepository(database, seeder) }
    val routineRepository: RoutineRepository by lazy { RoomRoutineRepository(database) }
    val workoutRepository: WorkoutRepository by lazy { RoomWorkoutRepository(database) }
    val settingsRepository: SettingsRepository by lazy { RoomSettingsRepository(database) }

    /** 백업 파일 내보내기와 가져오기(DATA-03, DATA-04). */
    val backupRepository: BackupRepository by lazy {
        val info = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        RoomBackupRepository(
            database = database,
            seeder = seeder,
            appVersion = BackupApp(
                versionName = info.versionName.orEmpty(),
                versionCode = PackageInfoCompat.getLongVersionCode(info),
            ),
        )
    }

    /** 오픈소스 라이선스(SET-03). `assets/licenses/`의 파일만 읽는다. */
    val licenseRepository: LicenseRepository by lazy {
        AssetLicenseRepository { fileName ->
            withContext(Dispatchers.IO) {
                appContext.assets.open("licenses/$fileName").bufferedReader().use { it.readText() }
            }
        }
    }
}
