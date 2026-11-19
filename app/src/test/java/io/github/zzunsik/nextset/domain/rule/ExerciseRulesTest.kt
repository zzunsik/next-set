package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.ExerciseDraft
import io.github.zzunsik.nextset.domain.model.ExerciseFilter
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SubPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseRulesTest {
    private fun exercise(
        id: String,
        nameKo: String,
        bodyPart: BodyPart,
        subPart: SubPart? = null,
        nameEn: String? = null,
        aliases: List<String> = emptyList(),
        archived: Boolean = false,
    ) = Exercise(id, nameKo, nameEn, aliases, bodyPart, subPart, Equipment.BARBELL, RecordType.WEIGHT_REPS, true, archived, null)

    private val bench = exercise("bench", "바벨 벤치 프레스", BodyPart.CHEST, nameEn = "Barbell Bench Press", aliases = listOf("벤치"))
    private val squat = exercise("squat", "백 스쿼트", BodyPart.LEGS, SubPart.QUADS, aliases = listOf("스쿼트"))
    private val curl = exercise("curl", "레그 컬", BodyPart.LEGS, SubPart.HAMSTRINGS)
    private val customLegs = exercise("custom-1", "내 하체 운동", BodyPart.LEGS)
    private val hidden = exercise("hidden", "숨긴 벤치", BodyPart.CHEST, archived = true)
    private val all = listOf(bench, squat, curl, customLegs, hidden)

    @Test
    fun searchIgnoresSpacesAndCase() {
        assertTrue(bench.matchesQuery("벤치프레스"))
        assertTrue(bench.matchesQuery(" 벤치 프레스 "))
        assertTrue(bench.matchesQuery("benchpress"))
        assertTrue(bench.matchesQuery("BARBELL bench"))
        assertTrue(squat.matchesQuery("스쿼"))
        assertTrue(bench.matchesQuery(""))
        assertFalse(bench.matchesQuery("스쿼트"))
    }

    @Test
    fun filterHidesArchivedAndAppliesPartFilters() {
        assertEquals(listOf(bench, squat, curl, customLegs), all.filterBy(ExerciseFilter()))
        assertEquals(listOf(bench), all.filterBy(ExerciseFilter(query = "벤치")))
        // 세부 부위를 고르지 않으면 세부 부위가 빈 종목까지 그 부위 전체가 보인다.
        assertEquals(listOf(squat, curl, customLegs), all.filterBy(ExerciseFilter(bodyPart = BodyPart.LEGS)))
        assertEquals(listOf(curl), all.filterBy(ExerciseFilter(bodyPart = BodyPart.LEGS, subPart = SubPart.HAMSTRINGS)))
        assertEquals(listOf(squat), all.filterBy(ExerciseFilter(query = "스쿼트", bodyPart = BodyPart.LEGS)))
    }

    @Test
    fun subPartMustBelongToBodyPart() {
        assertTrue(isValidSubPart(BodyPart.LEGS, SubPart.GLUTES))
        assertTrue(isValidSubPart(BodyPart.CHEST, null))
        assertFalse(isValidSubPart(BodyPart.ARMS, SubPart.QUADS))
        assertFalse(isValidSubPart(BodyPart.CHEST, SubPart.BICEPS))
    }

    @Test
    fun draftNormalizationClearsMismatchedSubPartAndValidates() {
        val draft = ExerciseDraft(
            nameKo = "  원암 푸시다운 ",
            nameEn = " ",
            aliases = listOf(" 푸시다운", "", "푸시다운"),
            bodyPart = BodyPart.CHEST,
            subPart = SubPart.TRICEPS,
            equipment = Equipment.CABLE,
            recordType = RecordType.WEIGHT_REPS,
        ).normalized()
        assertEquals("원암 푸시다운", draft.nameKo)
        assertNull(draft.nameEn)
        assertEquals(listOf("푸시다운"), draft.aliases)
        assertNull(draft.subPart)
        draft.validate()

        assertThrows(IllegalArgumentException::class.java) { draft.copy(nameKo = " ").validate() }
        assertThrows(IllegalArgumentException::class.java) { draft.copy(subPart = SubPart.BICEPS).validate() }
    }

    @Test
    fun nameConflictIgnoresSpacesCaseHiddenAndSelf() {
        assertTrue(all.hasNameConflict("바벨 벤치 프레스"))
        assertTrue("띄어쓰기만 다르면 겹친다", all.hasNameConflict("바벨벤치프레스"))
        assertTrue("영어 이름도 견준다", all.hasNameConflict(" barbell bench press "))
        assertFalse("별칭은 견주지 않는다", all.hasNameConflict("벤치"))
        assertFalse("숨긴 종목은 견주지 않는다", all.hasNameConflict("숨긴 벤치"))
        assertFalse("고치는 종목 자신은 빼고 본다", all.hasNameConflict("내 하체 운동", exceptId = "custom-1"))
        assertTrue(all.hasNameConflict("내 하체 운동"))
        assertFalse("빈 이름은 겹침이 아니라 빈 이름으로 막는다", all.hasNameConflict("  "))
    }
}
