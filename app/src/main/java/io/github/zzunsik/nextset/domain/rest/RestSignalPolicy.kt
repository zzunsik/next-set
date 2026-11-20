package io.github.zzunsik.nextset.domain.rest

/** 기기의 벨소리 모드. */
enum class RingerMode { NORMAL, VIBRATE, SILENT }

/** 신호를 내는 방법. */
enum class SignalOutput { SOUND, VIBRATION, NONE }

/**
 * 신호를 어떻게 낼지(REST-05). 방해 금지가 켜져 있으면 벨소리 모드와 상관없이 내지 않는다.
 * 그다음 소리 모드면 소리, 진동 모드면 진동, 무음이면 없다(docs/rest-timer.md "신호 판정").
 */
fun signalOutputFor(doNotDisturb: Boolean, ringerMode: RingerMode): SignalOutput = when {
    doNotDisturb -> SignalOutput.NONE
    ringerMode == RingerMode.NORMAL -> SignalOutput.SOUND
    ringerMode == RingerMode.VIBRATE -> SignalOutput.VIBRATION
    else -> SignalOutput.NONE
}
