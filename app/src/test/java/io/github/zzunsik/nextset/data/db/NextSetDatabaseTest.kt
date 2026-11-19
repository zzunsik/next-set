package io.github.zzunsik.nextset.data.db

import androidx.room3.useReaderConnection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NextSetDatabaseTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun inMemoryDatabaseOpensWithBundledSqlite() = runTest {
        val database = NextSetDatabase.inMemory(context)
        try {
            val version = database.useReaderConnection { connection ->
                connection.usePrepared("SELECT sqlite_version()") { statement ->
                    statement.step()
                    statement.getText(0)
                }
            }
            // BundledSQLiteDriver에 든 SQLite는 윈도 함수(3.25)와 UPSERT(3.24)를 지원한다.
            val (major, minor) = version.split('.').map(String::toInt)
            assertTrue("SQLite $version", major > 3 || (major == 3 && minor >= 25))
        } finally {
            database.close()
        }
    }

    @Test
    fun fileDatabaseOpensAndCreatesFile() = runTest {
        val database = NextSetDatabase.build(context)
        try {
            // 연결을 한 번 열어야 파일이 생긴다.
            val tables = database.useReaderConnection { connection ->
                connection.usePrepared("SELECT name FROM sqlite_master WHERE type = 'table'") { statement ->
                    buildList { while (statement.step()) add(statement.getText(0)) }
                }
            }
            assertTrue("테이블: $tables", "schema_placeholder" in tables)
        } finally {
            database.close()
        }
        assertTrue(context.getDatabasePath(NextSetDatabase.FILE_NAME).exists())
    }
}
