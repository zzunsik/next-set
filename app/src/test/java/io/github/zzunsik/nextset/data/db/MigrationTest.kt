package io.github.zzunsik.nextset.data.db

import androidx.room3.testing.MigrationTestHelper
import androidx.room3.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * 마이그레이션 테스트의 틀(docs/architecture.md "마이그레이션 테스트").
 *
 * 도우미는 스키마 JSON을 앱의 assets에서 읽는다. 단위 테스트는 디버그 빌드의 병합된 assets를 읽으므로
 * `app/build.gradle.kts`가 디버그 소스 세트에만 `app/schemas`를 assets로 더한다.
 *
 * 버전을 올릴 때마다 할 일:
 * 1. `NextSetDatabase.VERSION`을 올리고 [NextSetDatabase.MIGRATIONS]나 `autoMigrations`에 더한다.
 * 2. 아래 [SAMPLE_ROWS]처럼 그 버전의 표본 행을 넣는 함수를 더하고 `n`에서 `n+1`로 가는 테스트를 쓴다.
 * 3. [migratesFromEveryVersionToLatest]는 그대로 둔다. 버전 1부터 최신까지 올린 뒤 실제 Room으로 연다.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val context = RuntimeEnvironment.getApplication()
    private val file: File = context.getDatabasePath("migration-test.db")

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        file,
        BundledSQLiteDriver(),
        NextSetDatabase::class,
    )

    @Test
    fun everyVersionHasAnExportedSchema() {
        val folder = File("schemas/${NextSetDatabase::class.qualifiedName}")
        val versions = folder.listFiles { f -> f.extension == "json" }.orEmpty().map { it.nameWithoutExtension.toInt() }.sorted()
        assertEquals((1..NextSetDatabase.VERSION).toList(), versions)
        // 버전 n에서 n+1로 가는 마이그레이션이 하나씩 있다. 자동 마이그레이션은 Room이 따로 검사한다.
        val manual = NextSetDatabase.MIGRATIONS.map { it.startVersion to it.endVersion }
        assertTrue(manual.all { (from, to) -> to == from + 1 })
    }

    @Test
    fun migratesFromEveryVersionToLatest() = runTest {
        // 버전 1의 스키마로 만들고 SQL로 표본 행을 넣는다.
        helper.createDatabase(1).apply {
            SAMPLE_ROWS.forEach(::execSQL)
            close()
        }
        // 최신 버전까지 올리고 도우미가 결과 스키마를 최신 JSON과 견준다.
        helper.runMigrationsAndValidate(NextSetDatabase.VERSION, NextSetDatabase.MIGRATIONS.toList()).close()

        // 실제 Room 데이터베이스로 열어 표본 행이 남았는지 질의로 본다.
        val database = NextSetDatabase.build(context, file.absolutePath)
        try {
            val rows = database.useReaderConnection { connection ->
                connection.usePrepared(
                    "SELECT e.name_ko, s.weight_g, s.reps FROM workout_set s " +
                        "JOIN workout_exercise we ON we.id = s.workout_exercise_id JOIN exercise e ON e.id = we.exercise_id",
                ) { statement ->
                    buildList { while (statement.step()) add("${statement.getText(0)}|${statement.getLong(1)}|${statement.getLong(2)}") }
                }
            }
            assertEquals(listOf("표본 종목|60000|10"), rows)
            assertEquals(60_000, wrInDao(database))
        } finally {
            database.close()
        }
    }

    private suspend fun wrInDao(database: NextSetDatabase): Int? =
        database.workoutDao().bestWeightReps("sample", WorkoutDao.NO_SET)?.weightG

    private companion object {
        /** 버전 1 스키마의 표본 행. 값은 지어냈다. */
        val SAMPLE_ROWS = listOf(
            "INSERT INTO exercise (id, name_ko, name_en, aliases, body_part, sub_part, equipment, record_type, is_builtin, is_archived) " +
                "VALUES ('sample', '표본 종목', NULL, '', 'CHEST', NULL, 'BARBELL', 'WEIGHT_REPS', 0, 0)",
            "INSERT INTO routine (id, name, position) VALUES (1, '표본 루틴', 0)",
            "INSERT INTO routine_exercise (routine_id, exercise_id, position, set_count) VALUES (1, 'sample', 0, 3)",
            "INSERT INTO workout (id, routine_id, routine_name, started_at, finished_at) VALUES (1, 1, '표본 루틴', 0, 1000)",
            "INSERT INTO workout_exercise (id, workout_id, exercise_id, position, record_type) VALUES (1, 1, 'sample', 0, 'WEIGHT_REPS')",
            "INSERT INTO workout_set (workout_exercise_id, position, weight_g, reps, completed_at) VALUES (1, 0, 60000, 10, 500)",
            "INSERT INTO app_settings (id, default_rest_seconds, theme_mode, pinned_routine_id, builtin_seed_version, notification_prompt_declined) " +
                "VALUES (1, 120, 'SYSTEM', 1, 1, 0)",
        )
    }
}
