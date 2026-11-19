package io.github.zzunsik.nextset.data.repository

import androidx.room3.useReaderConnection
import io.github.zzunsik.nextset.data.db.NextSetDatabase
import io.github.zzunsik.nextset.data.repository.TestExercises.BENCH
import io.github.zzunsik.nextset.data.repository.TestExercises.PLANK
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** 앱이 꺼졌다 켜져도 하던 운동을 이어서 한다(LOG-13, NF-09). 파일 데이터베이스를 닫았다 다시 연다. */
@RunWith(RobolectricTestRunner::class)
class InProgressWorkoutPersistenceTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun inProgressWorkoutSurvivesClosingAndReopeningTheDatabase() = runTest {
        val first = TestDb(NextSetDatabase.build(context)).seed()
        val workoutId: Long
        val written = try {
            workoutId = first.workouts.startEmpty(at(0))
            val bench = first.workouts.addExercise(workoutId, BENCH, setCount = 2)
            first.workouts.addExercise(workoutId, PLANK, setCount = 1)
            val sets = first.workouts.observeWorkout(workoutId).first()!!.exercises[0].sets
            // 입력 한 번이 쓰기 한 번이다. 저장 버튼 없이 바로 남는다.
            first.workouts.updateSet(sets[0].id, wr(60_000, 10))
            first.workouts.completeSet(sets[0].id, at(1))
            first.workouts.updateSet(sets[1].id, wr(62_500, 8))
            first.workouts.updateExerciseNote(bench, "지어낸 메모")
            first.workouts.observeWorkout(workoutId).first()!!
        } finally {
            first.close()
        }

        val reopened = TestDb(NextSetDatabase.build(context))
        try {
            val restored = reopened.workouts.observeInProgressWorkout().first()
            assertNotNull(restored)
            assertEquals(written, restored)
            assertNull(restored!!.finishedAt)
            assertEquals(wr(62_500, 8), restored.exercises[0].sets[1].value)
            assertNull(restored.exercises[0].sets[1].completedAt)
        } finally {
            reopened.close()
        }
    }

    @Test
    fun fileDatabaseUsesWalWithNormalSync() = runTest {
        // Room 기본값을 그대로 쓴다. WAL에 synchronous NORMAL이면 커밋한 쓰기는 앱이 죽어도 남는다.
        // 전원이 갑자기 끊기면 마지막 커밋 몇 개가 되돌아갈 수 있지만 파일은 깨지지 않는다
        // (docs/architecture.md "진행 중인 운동의 저장"). 기본값이 바뀌면 이 테스트가 알려 준다.
        val database = NextSetDatabase.build(context)
        try {
            val (journal, synchronous) = database.useReaderConnection { connection ->
                val journal = connection.usePrepared("PRAGMA journal_mode") { it.step(); it.getText(0) }
                val synchronous = connection.usePrepared("PRAGMA synchronous") { it.step(); it.getLong(0) }
                journal to synchronous
            }
            assertEquals("wal", journal.lowercase())
            assertEquals("1 = NORMAL", 1L, synchronous)
        } finally {
            database.close()
        }
    }
}
