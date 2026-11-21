package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.rule.KoreanParticle.AND
import io.github.zzunsik.nextset.domain.rule.KoreanParticle.DIRECTION
import io.github.zzunsik.nextset.domain.rule.KoreanParticle.OBJECT
import io.github.zzunsik.nextset.domain.rule.KoreanParticle.SUBJECT
import io.github.zzunsik.nextset.domain.rule.KoreanParticle.TOPIC
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 이름 끝소리에 맞는 조사 고르기(#93). 이름은 지어낸 것이다. */
class KoreanParticleTest {
    @Test
    fun hangulFollowsTheFinalConsonant() {
        assertEquals(FinalSound.OTHER, finalSoundOf("가슴 루틴"))
        assertEquals(FinalSound.NONE, finalSoundOf("하체 데이"))
        assertEquals(FinalSound.RIEUL, finalSoundOf("상체 풀"))
        assertEquals("가슴 루틴을", withParticle("가슴 루틴", OBJECT))
        assertEquals("하체 데이를", withParticle("하체 데이", OBJECT))
        assertEquals("가슴 루틴이", withParticle("가슴 루틴", SUBJECT))
        assertEquals("하체 데이가", withParticle("하체 데이", SUBJECT))
        assertEquals("가슴 루틴은", withParticle("가슴 루틴", TOPIC))
        assertEquals("하체 데이는", withParticle("하체 데이", TOPIC))
        assertEquals("가슴 루틴과", withParticle("가슴 루틴", AND))
        assertEquals("하체 데이와", withParticle("하체 데이", AND))
    }

    @Test
    fun directionParticleTreatsRieulLikeAVowel() {
        assertEquals("가슴 루틴으로", withParticle("가슴 루틴", DIRECTION))
        assertEquals("하체 데이로", withParticle("하체 데이", DIRECTION))
        assertEquals("상체 풀로", withParticle("상체 풀", DIRECTION))
        // ㄹ로 시작하는 겹받침은 ㄹ 받침이 아니다.
        assertEquals("닭으로", withParticle("닭", DIRECTION))
        // 다른 조사는 ㄹ도 받침으로 본다.
        assertEquals("상체 풀을", withParticle("상체 풀", OBJECT))
    }

    @Test
    fun digitsFollowTheirKoreanReading() {
        val expected = mapOf(
            '0' to "을", '1' to "을", '2' to "를", '3' to "을", '4' to "를",
            '5' to "를", '6' to "을", '7' to "을", '8' to "을", '9' to "를",
        )
        expected.forEach { (digit, particle) ->
            assertEquals("루틴 $digit$particle", withParticle("루틴 $digit", OBJECT))
        }
        assertEquals("루틴 1로", withParticle("루틴 1", DIRECTION))
        assertEquals("루틴 7로", withParticle("루틴 7", DIRECTION))
        assertEquals("루틴 8로", withParticle("루틴 8", DIRECTION))
        assertEquals("루틴 3으로", withParticle("루틴 3", DIRECTION))
        assertEquals("루틴 2로", withParticle("루틴 2", DIRECTION))
        // 십, 백, 천, 만 모두 ㄹ이 아닌 받침이다.
        assertEquals("루틴 10을", withParticle("루틴 10", OBJECT))
        assertEquals("루틴 100으로", withParticle("루틴 100", DIRECTION))
        assertEquals("Upper 3을", withParticle("Upper 3", OBJECT))
    }

    @Test
    fun standaloneLatinLettersReadTheLetterName() {
        val withRieul = "LR"
        val withOtherFinal = "MN"
        ('A'..'Z').forEach { letter ->
            val expected = when (letter) {
                in withRieul -> FinalSound.RIEUL
                in withOtherFinal -> FinalSound.OTHER
                else -> FinalSound.NONE
            }
            assertEquals("Day $letter", expected, finalSoundOf("Day $letter"))
            assertEquals("Day ${letter.lowercaseChar()}", expected, finalSoundOf("Day ${letter.lowercaseChar()}"))
        }
        assertEquals("Push Day A를", withParticle("Push Day A", OBJECT))
        assertEquals("Leg Day L을", withParticle("Leg Day L", OBJECT))
        assertEquals("Leg Day L로", withParticle("Leg Day L", DIRECTION))
        assertEquals("Plan M으로", withParticle("Plan M", DIRECTION))
        assertEquals("Plan X를", withParticle("Plan X", OBJECT))
        assertEquals("A를", withParticle("A", OBJECT))
        assertEquals("하체B를", withParticle("하체B", OBJECT))
        assertEquals("루틴 3A를", withParticle("루틴 3A", OBJECT))
    }

    @Test
    fun latinWordsAreNotGuessed() {
        // "Push"(푸시)와 "Back"(백)은 철자로 받침을 가릴 수 없다.
        assertNull(finalSoundOf("Push"))
        assertNull(finalSoundOf("Back"))
        assertNull(finalSoundOf("Push Day"))
        assertNull(finalSoundOf("PPL"))
        assertNull(withParticle("Push Day", OBJECT))
    }

    @Test
    fun symbolsEmojiAndJamoAreNotGuessed() {
        assertNull(finalSoundOf("등 운동 ★"))
        assertNull(finalSoundOf("하체 💪"))
        assertNull(finalSoundOf("하체!"))
        assertNull(finalSoundOf("ㅋ"))
        assertNull(finalSoundOf("하체 Ａ"))
        assertNull(finalSoundOf(""))
        assertNull(finalSoundOf("   "))
        assertNull(finalSoundOf("()"))
        assertNull(withParticle("등 운동 ★", OBJECT))
    }

    @Test
    fun trailingSpacesAndClosingMarksAreSkipped() {
        assertEquals("가슴 루틴을", withParticle("가슴 루틴  ", OBJECT))
        assertEquals("가슴 (B)를", withParticle("가슴 (B)", OBJECT))
        assertEquals("가슴 (상체)를", withParticle("가슴 (상체)", OBJECT))
        assertEquals("가슴 [3]을", withParticle("가슴 [3]", OBJECT))
        assertEquals("\"풀\"로", withParticle("\"풀\"", DIRECTION))
        assertEquals("가슴 「팔」로", withParticle("가슴 「팔」", DIRECTION))
        assertNull(finalSoundOf("가슴 (Push)"))
    }
}
