package io.github.zzunsik.nextset.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.room3.withReadTransaction
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * 앱의 데이터베이스. 테이블은 docs/architecture.md "저장 스키마"의 일곱 개다.
 *
 * 드라이버는 기기와 JVM 테스트가 같은 SQLite를 쓰도록 [BundledSQLiteDriver]로 고정한다
 * (docs/architecture.md "데이터베이스").
 *
 * 첫 릴리스 전이라 버전 1을 고쳐 쓴다. 첫 릴리스부터는 엔티티를 바꾸면 버전을 올리고
 * [MIGRATIONS]나 `autoMigrations`에 더하고 마이그레이션 테스트를 넣는다(docs/architecture.md "스키마 버전 관리").
 */
@Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        AppSettingsEntity::class,
    ],
    version = NextSetDatabase.VERSION,
    exportSchema = true,
)
abstract class NextSetDatabase : RoomDatabase() {
    internal abstract fun exerciseDao(): ExerciseDao

    internal abstract fun routineDao(): RoutineDao

    internal abstract fun workoutDao(): WorkoutDao

    internal abstract fun settingsDao(): SettingsDao

    /**
     * [tables]가 바뀔 때마다 [read]를 읽기 트랜잭션 하나에서 돌려 내보낸다.
     * 여러 테이블을 한 번에 읽어 서로 어긋난 중간 상태를 내보내지 않는다.
     */
    internal fun <T> observe(vararg tables: String, read: suspend () -> T): Flow<T> =
        invalidationTracker.createFlow(*tables, emitInitialState = true)
            .map { withReadTransaction { read() } }
            .distinctUntilChanged()

    companion object {
        const val FILE_NAME = "next-set.db"

        /** 스키마 버전. 바꾸면 `app/schemas/`에 새 JSON이 생긴다. */
        const val VERSION = 1

        /** 손으로 쓴 마이그레이션. 버전 1뿐이라 아직 없다. */
        val MIGRATIONS: Array<Migration> = emptyArray()

        fun build(context: Context): NextSetDatabase = build(context, context.getDatabasePath(FILE_NAME).absolutePath)

        /** [path]의 파일을 연다. 마이그레이션 테스트가 도우미로 만든 파일을 열 때도 쓴다. */
        fun build(context: Context, path: String): NextSetDatabase =
            Room.databaseBuilder<NextSetDatabase>(context, path)
                .configure()
                .build()

        /** 테스트용. 파일을 남기지 않는다. */
        fun inMemory(context: Context): NextSetDatabase =
            Room.inMemoryDatabaseBuilder<NextSetDatabase>(context)
                .configure()
                .build()

        private fun RoomDatabase.Builder<NextSetDatabase>.configure() =
            setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .addMigrations(*MIGRATIONS)
    }
}

internal object Tables {
    const val EXERCISE = "exercise"
    const val ROUTINE = "routine"
    const val ROUTINE_EXERCISE = "routine_exercise"
    const val WORKOUT = "workout"
    const val WORKOUT_EXERCISE = "workout_exercise"
    const val WORKOUT_SET = "workout_set"
    const val APP_SETTINGS = "app_settings"
}
