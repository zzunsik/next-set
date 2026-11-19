package io.github.zzunsik.nextset.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * 앱의 데이터베이스. 테이블은 #20에서 더한다. 그때까지 [SchemaPlaceholder] 하나만 있다.
 *
 * 드라이버는 기기와 JVM 테스트가 같은 SQLite를 쓰도록 [BundledSQLiteDriver]로 고정한다
 * (docs/architecture.md "데이터베이스").
 */
@Database(entities = [SchemaPlaceholder::class], version = 1, exportSchema = true)
abstract class NextSetDatabase : RoomDatabase() {
    companion object {
        const val FILE_NAME = "next-set.db"

        fun build(context: Context): NextSetDatabase =
            Room.databaseBuilder<NextSetDatabase>(context, context.getDatabasePath(FILE_NAME).absolutePath)
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
    }
}
