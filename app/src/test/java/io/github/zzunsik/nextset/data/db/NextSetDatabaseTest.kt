package io.github.zzunsik.nextset.data.db

import androidx.room3.useReaderConnection
import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NextSetDatabaseTest {
    private val context = RuntimeEnvironment.getApplication()

    private suspend fun NextSetDatabase.query(sql: String): List<String> = useReaderConnection { connection ->
        connection.usePrepared(sql) { statement ->
            buildList { while (statement.step()) add(statement.getText(0)) }
        }
    }

    private suspend fun NextSetDatabase.exec(vararg sql: String) = useWriterConnection { connection ->
        sql.forEach { connection.executeSQL(it) }
    }

    private suspend fun NextSetDatabase.count(table: String): Int = query("SELECT COUNT(*) FROM $table").single().toInt()

    @Test
    fun inMemoryDatabaseOpensWithBundledSqlite() = runTest {
        val database = NextSetDatabase.inMemory(context)
        try {
            val version = database.query("SELECT sqlite_version()").single()
            // BundledSQLiteDriver에 든 SQLite는 윈도 함수(3.25)와 UPSERT(3.24)를 지원한다.
            val (major, minor) = version.split('.').map(String::toInt)
            assertTrue("SQLite $version", major > 3 || (major == 3 && minor >= 25))
        } finally {
            database.close()
        }
    }

    @Test
    fun fileDatabaseHasTheSevenTables() = runTest {
        val database = NextSetDatabase.build(context)
        try {
            val tables = database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name NOT LIKE 'sqlite_%' AND name NOT IN ('room_master_table', 'android_metadata')",
            )
            assertEquals(
                setOf("exercise", "routine", "routine_exercise", "workout", "workout_exercise", "workout_set", "app_settings"),
                tables.toSet(),
            )
        } finally {
            database.close()
        }
        assertTrue(context.getDatabasePath(NextSetDatabase.FILE_NAME).exists())
    }

    @Test
    fun foreignKeysAreEnforced() = runTest {
        // Room 3은 외래 키를 선언한 엔티티가 있을 때만 PRAGMA foreign_keys = ON을 보낸다(#18에서 확인).
        val database = NextSetDatabase.inMemory(context)
        try {
            assertEquals(listOf("1"), database.query("PRAGMA foreign_keys"))
        } finally {
            database.close()
        }
    }

    @Test
    fun deletionsPropagateAsDesigned() = runTest {
        val database = NextSetDatabase.inMemory(context)
        try {
            database.exec(
                "INSERT INTO exercise (id, name_ko, name_en, aliases, body_part, sub_part, equipment, record_type, is_builtin) " +
                    "VALUES ('used', '쓴 종목', NULL, '', 'CHEST', NULL, 'BARBELL', 'WEIGHT_REPS', 0)",
                "INSERT INTO exercise (id, name_ko, name_en, aliases, body_part, sub_part, equipment, record_type, is_builtin) " +
                    "VALUES ('unused', '안 쓴 종목', NULL, '', 'BACK', NULL, 'CABLE', 'REPS', 0)",
                "INSERT INTO routine (id, name, position) VALUES (1, '루틴', 0)",
                "INSERT INTO routine_exercise (routine_id, exercise_id, position, set_count) VALUES (1, 'used', 0, 3)",
                "INSERT INTO routine_exercise (routine_id, exercise_id, position, set_count) VALUES (1, 'unused', 1, 3)",
                "INSERT INTO app_settings (id, default_rest_seconds, theme_mode, pinned_routine_id) VALUES (1, 120, 'SYSTEM', 1)",
                "INSERT INTO workout (id, routine_id, routine_name, started_at, finished_at) VALUES (1, 1, '루틴', 0, 1000)",
                "INSERT INTO workout_exercise (id, workout_id, exercise_id, position, record_type) VALUES (1, 1, 'used', 0, 'WEIGHT_REPS')",
                "INSERT INTO workout_set (workout_exercise_id, position, weight_g, reps, completed_at) VALUES (1, 0, 60000, 10, 500)",
            )

            // 기록에 쓰인 종목 행은 지우지 못한다(RESTRICT).
            assertThrows(SQLiteException::class.java) {
                kotlinx.coroutines.runBlocking { database.exec("DELETE FROM exercise WHERE id = 'used'") }
            }
            // 안 쓴 종목을 지우면 루틴 종목이 함께 지워진다.
            database.exec("DELETE FROM exercise WHERE id = 'unused'")
            assertEquals(1, database.count("routine_exercise"))

            // 루틴을 지우면 루틴 종목은 지워지고 운동 기록의 routine_id와 고른 루틴은 빈다.
            database.exec("DELETE FROM routine WHERE id = 1")
            assertEquals(0, database.count("routine_exercise"))
            assertEquals(listOf("1"), database.query("SELECT COUNT(*) FROM workout WHERE routine_id IS NULL AND routine_name = '루틴'"))
            assertEquals(listOf("1"), database.query("SELECT COUNT(*) FROM app_settings WHERE pinned_routine_id IS NULL"))

            // 운동 기록을 지우면 운동 기록 종목과 세트가 지워진다.
            database.exec("DELETE FROM workout WHERE id = 1")
            assertEquals(0, database.count("workout_exercise"))
            assertEquals(0, database.count("workout_set"))
            database.exec("DELETE FROM exercise WHERE id = 'used'")
            assertEquals(0, database.count("exercise"))
        } finally {
            database.close()
        }
    }

    @Test
    fun enumsAreTextAndBooleansAreIntegers() = runTest {
        val database = NextSetDatabase.inMemory(context)
        try {
            database.exec(
                "INSERT INTO exercise (id, name_ko, name_en, aliases, body_part, sub_part, equipment, record_type, is_builtin) " +
                    "VALUES ('x', '종목', NULL, '', 'ARMS', 'BICEPS', 'DUMBBELL', 'WEIGHT_REPS', 1)",
            )
            assertEquals(
                listOf("text|text|integer|integer"),
                database.query("SELECT typeof(body_part) || '|' || typeof(record_type) || '|' || typeof(is_builtin) || '|' || typeof(is_archived) FROM exercise"),
            )
            assertEquals(listOf("0"), database.query("SELECT is_archived FROM exercise"))
        } finally {
            database.close()
        }
    }
}
