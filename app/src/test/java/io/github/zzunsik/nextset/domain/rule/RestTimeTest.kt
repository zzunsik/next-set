package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** REST-03: 종목의 값이 먼저, 없으면 설정의 기본값. SET-01: 고를 수 있는 기본값. */
class RestTimeTest {
    private fun exercise(restSeconds: Int?) = Exercise(
        id = "test-curl",
        nameKo = "테스트 컬",
        nameEn = null,
        aliases = emptyList(),
        bodyPart = BodyPart.ARMS,
        subPart = null,
        equipment = Equipment.DUMBBELL,
        recordType = RecordType.WEIGHT_REPS,
        isBuiltin = false,
        isArchived = false,
        restSeconds = restSeconds,
    )

    @Test
    fun exerciseWithoutOwnValueFollowsDefault() {
        assertEquals(120, restSecondsFor(exercise(null), Settings()))
        assertEquals(75, restSecondsFor(exercise(null), Settings(defaultRestSeconds = 75)))
    }

    @Test
    fun exerciseOwnValueWinsOverDefault() {
        assertEquals(180, restSecondsFor(exercise(180), Settings(defaultRestSeconds = 75)))
    }

    @Test
    fun choicesAreThirtySecondsToFiveMinutesInFifteenSecondSteps() {
        assertEquals(30, defaultRestChoices.first())
        assertEquals(300, defaultRestChoices.last())
        assertEquals(19, defaultRestChoices.size)
        assertTrue(Settings.DEFAULT_REST_SECONDS in defaultRestChoices)
        assertTrue(defaultRestChoices.all(Settings::isValidDefaultRest))
        assertEquals(defaultRestChoices.sorted(), defaultRestChoices)
    }
}
