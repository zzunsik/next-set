package io.github.zzunsik.nextset.data.repository

import io.github.zzunsik.nextset.data.db.NextSetDatabase
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.SQUAT
import io.github.zzunsik.nextset.domain.model.ThemeMode
import io.github.zzunsik.nextset.domain.rule.restSecondsFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** 설정 화면에서 바꾸는 값(SET-01, SET-02, REST-03)이 저장되고 적용되는지. */
@RunWith(RobolectricTestRunner::class)
class SettingsPersistenceTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun changingDefaultRestAppliesToExercisesWithoutOwnValue() = runTest {
        TestDb().seed().use { db ->
            db.exercises.setRestSeconds(SQUAT, 180)

            db.settings.setDefaultRestSeconds(90)

            val settings = db.settings.observeSettings().first()
            // 종목별 값이 없는 종목은 바꾼 기본값을 따르고, 따로 정한 종목은 그 값을 지킨다.
            assertEquals(90, restSecondsFor(db.exercises.getExercise(BENCH)!!, settings))
            assertEquals(180, restSecondsFor(db.exercises.getExercise(SQUAT)!!, settings))

            db.settings.setDefaultRestSeconds(45)
            val changed = db.settings.observeSettings().first()
            assertEquals(45, restSecondsFor(db.exercises.getExercise(BENCH)!!, changed))
            assertEquals(180, restSecondsFor(db.exercises.getExercise(SQUAT)!!, changed))
        }
    }

    @Test
    fun eachThemeModeIsKeptAfterReopeningTheDatabase() = runTest {
        // 처음 값은 시스템 설정 따름이다(SET-02).
        TestDb(NextSetDatabase.build(context)).use { db ->
            assertEquals(ThemeMode.SYSTEM, db.settings.observeSettings().first().themeMode)
        }
        for (mode in listOf(ThemeMode.DARK, ThemeMode.LIGHT, ThemeMode.SYSTEM)) {
            TestDb(NextSetDatabase.build(context)).use { db -> db.settings.setThemeMode(mode) }
            TestDb(NextSetDatabase.build(context)).use { db ->
                assertEquals(mode, db.settings.observeSettings().first().themeMode)
            }
        }
    }

    @Test
    fun defaultRestIsKeptAfterReopeningTheDatabase() = runTest {
        TestDb(NextSetDatabase.build(context)).use { db -> db.settings.setDefaultRestSeconds(225) }
        TestDb(NextSetDatabase.build(context)).use { db ->
            assertEquals(225, db.settings.observeSettings().first().defaultRestSeconds)
        }
    }
}
