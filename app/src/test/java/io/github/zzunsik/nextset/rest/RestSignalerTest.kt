package io.github.zzunsik.nextset.rest

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Vibrator
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
import io.github.zzunsik.nextset.domain.rest.RestCue
import io.github.zzunsik.nextset.domain.rest.SignalOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * 벨소리 모드별 신호(REST-05). 기기의 모드를 Robolectric으로 바꾸고 소리(가짜 톤)와 진동이 실제로 나는지 본다.
 */
@RunWith(RobolectricTestRunner::class)
class RestSignalerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val audio = context.getSystemService(AudioManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val vibrator: Vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
    private val tones = mutableListOf<RestCue>()
    private val signaler = RestSignaler(context) { tones += it }

    @Before
    fun setUp() {
        shadowOf(vibrator).setHasVibrator(true)
        notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    @Test
    fun normalModePlaysSoundOnly() {
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        assertEquals(SignalOutput.SOUND, signaler.output())
        signaler.signal(RestCue.END)
        assertEquals(listOf(RestCue.END), tones)
        assertFalse(shadowOf(vibrator).isVibrating)
    }

    @Test
    fun vibrateModeVibratesOnly() {
        audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        assertEquals(SignalOutput.VIBRATION, signaler.output())
        signaler.signal(RestCue.COUNTDOWN)
        assertTrue(shadowOf(vibrator).isVibrating)
        assertEquals(emptyList<RestCue>(), tones)
    }

    @Test
    fun silentModeGivesNothing() {
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        assertEquals(SignalOutput.NONE, signaler.output())
        signaler.signal(RestCue.WARNING)
        assertEquals(emptyList<RestCue>(), tones)
        assertFalse(shadowOf(vibrator).isVibrating)
    }

    @Test
    fun doNotDisturbGivesNothingEvenInNormalMode() {
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        listOf(
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            NotificationManager.INTERRUPTION_FILTER_ALARMS,
            NotificationManager.INTERRUPTION_FILTER_NONE,
        ).forEach { filter ->
            notifications.setInterruptionFilter(filter)
            assertEquals("필터 $filter", SignalOutput.NONE, signaler.output())
        }
        signaler.signal(RestCue.END)
        assertEquals(emptyList<RestCue>(), tones)
    }

    @Test
    fun modeIsReadAgainForEverySignal() {
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        signaler.signal(RestCue.COUNTDOWN)
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        signaler.signal(RestCue.COUNTDOWN)
        assertEquals(listOf(RestCue.COUNTDOWN), tones)
    }

    @Test
    fun endVibrationDiffersFromEarlierCues() {
        assertEquals(listOf(0L, 400L, 150L, 400L), RestSignaler.END_VIBRATION.toList())
        val end = RestSignaler.vibrationOf(RestCue.END)
        assertTrue(end != RestSignaler.vibrationOf(RestCue.COUNTDOWN))
        assertTrue(end != RestSignaler.vibrationOf(RestCue.WARNING))
        assertTrue(RestSignaler.vibrationOf(RestCue.WARNING) != RestSignaler.vibrationOf(RestCue.COUNTDOWN))
    }
}
