package io.github.zzunsik.nextset.data.backup

import io.github.zzunsik.nextset.domain.model.BackupProblem
import io.github.zzunsik.nextset.domain.model.BackupSummary
import io.github.zzunsik.nextset.domain.model.InvalidBackupException
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** 백업 파일의 형식 변환과 검사(DATA-03, DATA-04). 순수 JVM에서 돈다. 값은 지어냈다. */
class BackupCodecTest {
    private val sample = BackupSamples.read(1)

    @Test
    fun everyFormatVersionHasASampleThatDecodes() {
        // 형식 버전마다 표본을 두고 지우지 않는다(docs/architecture.md "옛 백업 호환").
        for (version in 1..BackupCodec.CURRENT_VERSION) {
            val file = BackupCodec.decode(BackupSamples.read(version))
            assertEquals(BackupCodec.CURRENT_VERSION, file.formatVersion)
        }
    }

    @Test
    fun sampleDecodesWithItsValues() {
        val file = BackupCodec.decode(sample)

        assertEquals(ThemeMode.DARK, file.settings.themeMode)
        assertEquals(90, file.settings.defaultRestSeconds)
        assertEquals(7L, file.settings.pinnedRoutineId)
        assertEquals(4, file.exercises.size)
        val custom = file.exercises.single { !it.builtin }
        assertEquals(RecordType.DURATION, custom.recordType)
        assertTrue(custom.archived)
        // 종목의 지금 방식(시간)과 적을 당시의 방식(무게와 횟수)이 다른 기록이 그대로 읽힌다.
        val customRecord = file.workouts[0].exercises.single { it.exerciseId == custom.id }
        assertEquals(RecordType.WEIGHT_REPS, customRecord.recordType)
        assertEquals(20000, customRecord.sets.single().weightG)
        assertEquals(
            BackupSummary(workoutCount = 2, routineCount = 2, hasWorkoutInProgress = true),
            BackupCodec.summarize(file),
        )
    }

    @Test
    fun encodeThenDecodeGivesTheSameFile() {
        val file = BackupCodec.decode(sample)
        assertEquals(file, BackupCodec.decode(BackupCodec.encode(file)))
    }

    @Test
    fun unknownFieldsAndByteOrderMarkAreAccepted() {
        // 새 앱이 기본값 있는 필드를 더한 파일도 옛 앱이 읽는다.
        val withExtra = sample.replaceFirst("\"exportedAt\"", "\"futureOptional\": {\"a\": 1},\n  \"exportedAt\"")
        assertEquals(BackupCodec.decode(sample), BackupCodec.decode(Char(0xFEFF) + withExtra))
    }

    @Test
    fun filesOfOtherFormatsAreNotBackups() {
        assertProblem(BackupProblem.NOT_BACKUP, "")
        assertProblem(BackupProblem.NOT_BACKUP, "운동 기록")
        assertProblem(BackupProblem.NOT_BACKUP, "[1, 2, 3]")
        assertProblem(BackupProblem.NOT_BACKUP, "{\"version\": 1, \"exercises\": []}")
        assertProblem(BackupProblem.NOT_BACKUP, sample.replace("\"next-set-backup\"", "\"other-app-backup\""))
        assertProblem(BackupProblem.NOT_BACKUP, sample.replace("\"format\": \"next-set-backup\"", "\"format\": 1"))
    }

    @Test
    fun newerFormatVersionIsRejected() {
        assertProblem(
            BackupProblem.NEWER_VERSION,
            sample.replace("\"formatVersion\": 1", "\"formatVersion\": ${BackupCodec.CURRENT_VERSION + 1}"),
        )
        // 새 버전 파일은 모르는 필드와 열거 값이 있어도 버전으로 먼저 거른다.
        assertProblem(
            BackupProblem.NEWER_VERSION,
            sample.replace("\"formatVersion\": 1", "\"formatVersion\": 99").replace("\"CHEST\"", "\"NECK\""),
        )
    }

    @Test
    fun brokenFilesAreRejected() {
        val cases = mapOf(
            "잘린 파일" to sample.substring(0, sample.length / 2),
            "버전이 문자열" to sample.replace("\"formatVersion\": 1", "\"formatVersion\": \"1\""),
            "버전 0" to sample.replace("\"formatVersion\": 1", "\"formatVersion\": 0"),
            "필드가 빠짐" to sample.replace("\"settings\": {", "\"settingsRenamed\": {"),
            "모르는 열거 값" to sample.replace("\"CHEST\"", "\"NECK\""),
            "숫자 자리에 글" to sample.replace("\"setCount\": 4", "\"setCount\": \"넷\""),
            "없는 종목을 가리킴" to sample.replace("\"exerciseId\": \"test-retired-curl\"", "\"exerciseId\": \"nowhere\""),
            "없는 루틴을 가리킴" to sample.replace("\"routineId\": 3,", "\"routineId\": 4,"),
            "없는 직접 고른 루틴" to sample.replace("\"pinnedRoutineId\": 7", "\"pinnedRoutineId\": 8"),
            "겹치는 종목" to sample.replace("\"id\": \"test-retired-curl\"", "\"id\": \"test-pull-up\""),
            "겹치는 루틴" to sample.replace("\"id\": 3,", "\"id\": 7,"),
            "세부 부위가 부위와 맞지 않음" to sample.replace("\"subPart\": \"BICEPS\"", "\"subPart\": \"QUADS\""),
            "고를 수 없는 기본 휴식 시간" to sample.replace("\"defaultRestSeconds\": 90", "\"defaultRestSeconds\": 91"),
            "세트 수 0" to sample.replace("\"setCount\": 4", "\"setCount\": 0"),
            "음수 무게" to sample.replace("\"weightG\": 62500", "\"weightG\": -1"),
            "횟수만 방식에 무게" to sample.replace(
                "{ \"weightG\": null, \"reps\": 9,",
                "{ \"weightG\": 1000, \"reps\": 9,",
            ),
            "시간 방식 목표에 횟수" to sample.replace("\"targetRepsMin\": null,\n          \"targetRepsMax\": null,\n          \"targetDurationS\": 30", "\"targetRepsMin\": 5,\n          \"targetRepsMax\": null,\n          \"targetDurationS\": 30"),
            "끝난 시각이 시작보다 이름" to sample.replace("\"finishedAt\": 1794913200000", "\"finishedAt\": 1"),
            "진행 중인 운동이 둘" to sample.replace("\"finishedAt\": 1794913200000", "\"finishedAt\": null"),
        )
        cases.forEach { (name, text) ->
            assertTrue("표본을 바꾸지 못했다: $name", text != sample)
            assertProblem(BackupProblem.BROKEN, text, name)
        }
    }

    private fun assertProblem(expected: BackupProblem, text: String, name: String = text.take(40)) {
        try {
            BackupCodec.decode(text)
            fail("거부하지 않았다: $name")
        } catch (e: InvalidBackupException) {
            assertEquals("$name: ${e.message}", expected, e.problem)
            assertNotNull(e.message)
        }
    }
}

/** `app/src/test/resources/backup/`의 형식 버전별 표본. */
internal object BackupSamples {
    fun read(version: Int): String {
        val stream = checkNotNull(javaClass.getResourceAsStream("/backup/v$version.json")) { "표본 v$version.json 이 없다" }
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }.replace("\r\n", "\n")
    }
}
