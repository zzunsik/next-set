package io.github.zzunsik.nextset.domain.rest

import kotlinx.coroutines.flow.StateFlow

/**
 * 휴식 타이머(REST-01~REST-09). 화면과 알림 버튼이 같은 타이머 하나를 쓴다.
 * 시각은 기기가 켜진 뒤 흐른 밀리초(단조 시계)다. 남은 시간은 끝나는 시각에서 지금을 빼서 구한다(docs/rest-timer.md "끝나는 시각 맞추기").
 */
interface RestTimer {
    /** 휴식 중이 아니면 null이다. */
    val state: StateFlow<RestTimerState?>

    /** 지금 시각. [RestTimerState.endsAt]과 같은 시계다. */
    fun now(): Long

    /** 휴식을 [seconds]초로 새로 시작한다. 휴식 중이면 다시 시작한다(docs/DESIGN.md `RestTimerBar`). */
    fun start(workoutId: Long, setId: Long, seconds: Int)

    /** 남은 시간을 [deltaSeconds]만큼 바꾼다. 줄여서 0 이하가 되면 건너뛰기와 같다. */
    fun adjust(deltaSeconds: Int)

    /** 신호와 종료 알림 없이 휴식을 끝낸다. */
    fun skip()

    /** [setId]로 시작한 휴식이면 멈춘다. 세트의 완료를 풀었을 때 부른다. */
    fun stopForSet(setId: Long)

    /** 휴식과 알림을 모두 정리한다. 운동을 끝내거나 버렸을 때 부른다. */
    fun cancel()

    /** 알림에 적을 다음 세트. 화면이 다음 세트를 바꿀 때마다 알린다. */
    fun setNextSet(text: String?)

    /** 알림 권한을 받았다. 휴식 중이면 알림을 다시 보낸다(docs/rest-timer.md "휴식 도중에 허용한 경우"). */
    fun onNotificationsAllowed()
}

/**
 * 휴식 하나. [endsAt]은 끝나는 시각이고 [totalMillis]는 진행 막대의 전체 길이다.
 * [finished]는 끝나는 순간을 지나 "0:00"을 잠시 보이는 상태다(docs/DESIGN.md `RestTimerBar` "종료").
 */
data class RestTimerState(
    val workoutId: Long,
    val setId: Long,
    val endsAt: Long,
    val totalMillis: Long,
    val nextSet: String? = null,
    val finished: Boolean = false,
) {
    fun remainingMillis(now: Long): Long = if (finished) 0 else (endsAt - now).coerceAtLeast(0)

    /** 남은 비율. 0~1. */
    fun progress(now: Long): Float =
        if (totalMillis <= 0) 0f else (remainingMillis(now).toFloat() / totalMillis).coerceIn(0f, 1f)

    /** "+15초"는 남은 시간이 10분이 될 때까지 누를 수 있다(docs/DESIGN.md `RestTimerBar`). */
    fun canIncrease(now: Long): Boolean = !finished && remainingMillis(now) < RestTimerRules.MAX_REMAINING_MILLIS
}

/** 신호(REST-04). 끝나기 10초 전 한 번, 5초 전부터 1초마다, 끝나는 순간. */
enum class RestCue { WARNING, COUNTDOWN, END }

object RestTimerRules {
    /** 늘리고 줄이는 폭(REST-02). */
    const val STEP_SECONDS = 15

    /** "+15초"로 늘릴 수 있는 남은 시간의 끝(docs/DESIGN.md `RestTimerBar`). */
    const val MAX_REMAINING_MILLIS = 10 * 60 * 1000L

    /** 끝난 뒤 "0:00"을 보이는 시간. */
    const val FINISHED_DISPLAY_MILLIS = 2_000L

    /** 신호를 내는 때. 끝나기 몇 초 전인지, 큰 것부터. */
    val cueSeconds: List<Int> = listOf(10, 5, 4, 3, 2, 1, 0)

    fun cueAt(secondsBeforeEnd: Int): RestCue = when (secondsBeforeEnd) {
        0 -> RestCue.END
        10 -> RestCue.WARNING
        else -> RestCue.COUNTDOWN
    }

    /**
     * 남은 시간이 [remainingMillis]일 때 아직 내지 않은 신호들(끝나기 몇 초 전). 이미 지난 신호는 빠진다(제안).
     * 남은 시간이 정확히 신호 시점이면 그 신호를 포함한다.
     */
    fun pendingCues(remainingMillis: Long): List<Int> = cueSeconds.filter { it * 1000L <= remainingMillis }
}
