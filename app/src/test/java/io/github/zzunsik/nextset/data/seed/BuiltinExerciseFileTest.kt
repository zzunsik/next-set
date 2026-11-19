package io.github.zzunsik.nextset.data.seed

import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SubPart
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 내장 종목 파일(`app/src/main/assets/exercises.json`)을 검사한다(EX-01).
 * 원본은 docs/exercise-list.md의 표다. 둘이 어긋나면 [fileMatchesDocumentTable]이 실패한다.
 * 표를 고쳤으면 데이터 파일의 같은 줄을 고치고 `version`을 올린다.
 */
class BuiltinExerciseFileTest {
    private val file = BuiltinExerciseFile.parse(File("src/main/assets/exercises.json").readText())

    @Test
    fun countMatchesExerciseListAndIdsAreUnique() {
        // docs/exercise-list.md "종목 수": 전체 130개.
        assertEquals(130, file.exercises.size)
        assertEquals(file.exercises.size, file.exercises.map { it.id }.toSet().size)
        assertTrue(file.version >= 1)
    }

    @Test
    fun countsPerCategoryMatchExerciseList() {
        val active = file.exercises.filterNot { it.retired }
        assertEquals(
            mapOf(
                BodyPart.LEGS to 30, BodyPart.BACK to 24, BodyPart.CHEST to 21,
                BodyPart.SHOULDERS to 20, BodyPart.ARMS to 19, BodyPart.CORE to 16,
            ),
            active.groupingBy { it.bodyPart }.eachCount(),
        )
        assertEquals(
            mapOf(RecordType.WEIGHT_REPS to 99, RecordType.REPS to 27, RecordType.DURATION to 4),
            active.groupingBy { it.recordType }.eachCount(),
        )
    }

    @Test
    fun aliasesDoNotEqualAnotherExercisesName() {
        val names = file.exercises.associate { it.nameKo to it.id }
        val clashes = file.exercises.flatMap { exercise ->
            exercise.aliases.mapNotNull { alias ->
                names[alias]?.takeIf { it != exercise.id }?.let { "${exercise.id}: $alias" }
            }
        }
        assertEquals(emptyList<String>(), clashes)
    }

    @Test
    fun validationRejectsBrokenFiles() {
        val good = """{"id":"a-b","nameKo":"가","nameEn":"A","bodyPart":"CHEST","equipment":"BARBELL","recordType":"REPS"}"""
        BuiltinExerciseFile.parse("""{"version":1,"exercises":[$good]}""")

        fun broken(exercises: String) = assertThrows(IllegalArgumentException::class.java) {
            BuiltinExerciseFile.parse("""{"version":1,"exercises":[$exercises]}""")
        }
        broken("$good,$good") // 식별자 중복
        broken(good.replace("a-b", "A_B")) // 슬러그가 아니다
        broken(good.replace("\"CHEST\"", "\"LEGS\"")) // 하체인데 세부 부위가 없다
        broken(good.replace("\"bodyPart\":\"CHEST\"", "\"bodyPart\":\"CHEST\",\"subPart\":\"BICEPS\"")) // 부위와 안 맞는다
        broken(good.replace("\"REPS\"", "\"WEIGHT\"")) // 없는 열거 값
    }

    @Test
    fun fileMatchesDocumentTable() {
        val documented = parseDocumentTable(File("../docs/exercise-list.md").readText())
        val inFile = file.exercises.filterNot { it.retired }.map {
            DocumentRow(it.id, it.nameKo, it.nameEn, it.aliases, it.bodyPart, it.subPart, it.equipment, it.recordType)
        }
        // 한 줄씩 견줘 어긋난 줄을 바로 보여 준다.
        val docById = documented.associateBy { it.id }
        val fileById = inFile.associateBy { it.id }
        val mismatches = (docById.keys + fileById.keys).sorted().mapNotNull { id ->
            val doc = docById[id]
            val data = fileById[id]
            if (doc == data) null else "$id\n  문서: $doc\n  파일: $data"
        }
        assertEquals("docs/exercise-list.md와 exercises.json이 다르다", "", mismatches.joinToString("\n"))
        assertEquals("순서도 문서와 같게 둔다", documented.map { it.id }, inFile.map { it.id })
    }

    private data class DocumentRow(
        val id: String,
        val nameKo: String,
        val nameEn: String,
        val aliases: List<String>,
        val bodyPart: BodyPart,
        val subPart: SubPart?,
        val equipment: Equipment,
        val recordType: RecordType,
    )

    /** 종목 표의 줄(`| \`식별자\` | ...`)을 읽는다. 열은 표 머리글 순서다. 보조 부위는 앱 데이터에 넣지 않는다. */
    private fun parseDocumentTable(markdown: String): List<DocumentRow> =
        markdown.lines().filter { it.startsWith("| `") }.map { line ->
            val cells = line.split('|').drop(1).dropLast(1).map(String::trim)
            assertEquals("열 수: $line", 9, cells.size)
            DocumentRow(
                id = cells[0].removeSurrounding("`"),
                nameKo = cells[1],
                nameEn = cells[2],
                aliases = cells[3].split(',').map(String::trim).filter(String::isNotEmpty),
                bodyPart = BODY_PARTS.getValue(cells[4]),
                subPart = cells[5].takeIf(String::isNotEmpty)?.let(SUB_PARTS::getValue),
                equipment = EQUIPMENT.getValue(cells[7]),
                recordType = RECORD_TYPES.getValue(cells[8]),
            )
        }

    private companion object {
        // docs/exercise-list.md "표기 기준"의 값과 데이터 값.
        val BODY_PARTS = mapOf(
            "가슴" to BodyPart.CHEST, "등" to BodyPart.BACK, "어깨" to BodyPart.SHOULDERS,
            "팔" to BodyPart.ARMS, "하체" to BodyPart.LEGS, "코어" to BodyPart.CORE,
        )
        val SUB_PARTS = mapOf(
            "허벅지 앞" to SubPart.QUADS, "허벅지 뒤" to SubPart.HAMSTRINGS, "엉덩이" to SubPart.GLUTES,
            "종아리" to SubPart.CALVES, "이두" to SubPart.BICEPS, "삼두" to SubPart.TRICEPS, "전완" to SubPart.FOREARMS,
        )
        val EQUIPMENT = mapOf(
            "바벨" to Equipment.BARBELL, "덤벨" to Equipment.DUMBBELL, "머신" to Equipment.MACHINE,
            "케이블" to Equipment.CABLE, "맨몸" to Equipment.BODYWEIGHT, "케틀벨" to Equipment.KETTLEBELL,
            "밴드" to Equipment.BAND, "기타" to Equipment.OTHER,
        )
        val RECORD_TYPES = mapOf(
            "무게와 횟수" to RecordType.WEIGHT_REPS, "횟수만" to RecordType.REPS, "시간" to RecordType.DURATION,
        )
    }
}
