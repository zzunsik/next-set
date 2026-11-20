package io.github.zzunsik.nextset.rest

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.github.zzunsik.nextset.domain.rest.RestCue
import io.github.zzunsik.nextset.domain.rest.RingerMode
import io.github.zzunsik.nextset.domain.rest.SignalOutput
import io.github.zzunsik.nextset.domain.rest.signalOutputFor

/** 소리를 내는 곳. 테스트는 낸 소리를 적어 두는 가짜를 넘긴다. */
fun interface TonePlayer {
    fun play(cue: RestCue)

    /** 휴식이 끝나 더 낼 소리가 없다. */
    fun release() {}
}

/**
 * 휴식 신호를 소리나 진동으로 낸다(REST-04, REST-05). 신호마다 방해 금지 상태와 벨소리 모드를 새로 읽어
 * 휴식 중에 모드를 바꿔도 다음 신호부터 따른다(docs/rest-timer.md "소리와 진동").
 *
 * 끝나는 순간의 신호는 소리가 길고 두 번 울리며, 진동도 두 번이라 앞의 신호와 구분된다.
 */
class RestSignaler(
    context: Context,
    private val tones: TonePlayer = ToneGeneratorPlayer(),
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    /** 지금 신호를 낸다면 어떻게 낼지. */
    fun output(): SignalOutput = signalOutputFor(doNotDisturb = doNotDisturb(), ringerMode = ringerMode())

    /** 휴식이 끝났다. 소리 장치를 놓는다. */
    fun release() = tones.release()

    fun signal(cue: RestCue) {
        when (output()) {
            SignalOutput.SOUND -> tones.play(cue)
            SignalOutput.VIBRATION -> vibrate(cue)
            SignalOutput.NONE -> Unit
        }
    }

    /** `INTERRUPTION_FILTER_UNKNOWN`(읽지 못함)과 `ALL`은 꺼진 것으로 본다. */
    private fun doNotDisturb(): Boolean = when (notificationManager?.currentInterruptionFilter) {
        NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        NotificationManager.INTERRUPTION_FILTER_ALARMS,
        NotificationManager.INTERRUPTION_FILTER_NONE,
        -> true
        else -> false
    }

    private fun ringerMode(): RingerMode = when (audioManager?.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> RingerMode.SILENT
        AudioManager.RINGER_MODE_VIBRATE -> RingerMode.VIBRATE
        else -> RingerMode.NORMAL
    }

    private fun vibrate(cue: RestCue) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect = vibrationOf(cue)
        // 백그라운드 앱의 진동은 용도를 알림으로 밝혀야 울린다(docs/rest-timer.md "진동").
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_NOTIFICATION))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, NOTIFICATION_AUDIO)
        }
    }

    companion object {
        private val NOTIFICATION_AUDIO: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        /** 신호마다의 진동. 10초 전은 한 번, 카운트다운은 짧게 한 번, 끝은 길게 두 번. */
        fun vibrationOf(cue: RestCue): VibrationEffect = when (cue) {
            RestCue.WARNING -> VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            RestCue.COUNTDOWN -> VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
            RestCue.END -> VibrationEffect.createWaveform(END_VIBRATION, -1)
        }

        /** 끝의 진동: 쉼 0ms, 400ms 울림, 150ms 쉼, 400ms 울림. */
        val END_VIBRATION = longArrayOf(0, 400, 150, 400)
    }
}

/**
 * 내장 톤으로 소리를 낸다. 효과음 파일을 싣지 않는다(docs/rest-timer.md "소리"). 알림 음량을 따르도록 알림 스트림에 낸다.
 * 톤 장치를 만들지 못하면(오디오 자원이 모자람) 그 신호는 소리 없이 지나간다.
 */
class ToneGeneratorPlayer : TonePlayer {
    private var generator: ToneGenerator? = null

    override fun play(cue: RestCue) {
        val tone = generator ?: try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME).also { generator = it }
        } catch (_: RuntimeException) {
            return
        }
        when (cue) {
            RestCue.WARNING -> tone.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            RestCue.COUNTDOWN -> tone.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            RestCue.END -> tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
        }
    }

    override fun release() {
        generator?.release()
        generator = null
    }

    private companion object {
        const val VOLUME = 100
    }
}
