package io.github.zzunsik.nextset.rest

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.zzunsik.nextset.domain.rest.RestTimerState
import kotlin.math.abs

/**
 * 휴식 중인 타이머의 끝나는 시각을 기기에 남긴다. 프로세스가 죽었다가 앱을 다시 열면 이 값으로 휴식 바를 되살린다
 * (REST-06, docs/rest-timer.md "타이머의 운명").
 *
 * 운동 기록이 아니라 기기 상태라 데이터베이스와 백업에 넣지 않는다(docs/architecture.md "백업에 넣지 않는 것": 휴식 타이머 상태).
 * 단조 시계 값과 함께 벽시계 값도 적는다. 단조 시계는 재부팅하면 0부터 다시 세므로 둘이 어긋나면 벽시계를 믿는다.
 */
class RestTimerStore(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE))

    fun save(state: RestTimerState?, nowElapsed: Long, nowWall: Long) {
        prefs.edit {
            if (state == null || state.finished) {
                clear()
            } else {
                putLong(KEY_WORKOUT, state.workoutId)
                putLong(KEY_SET, state.setId)
                putLong(KEY_ENDS_AT_ELAPSED, state.endsAt)
                putLong(KEY_ENDS_AT_WALL, nowWall + (state.endsAt - nowElapsed))
                putLong(KEY_TOTAL, state.totalMillis)
                putString(KEY_NEXT_SET, state.nextSet)
            }
        }
    }

    /** 남긴 휴식. 없으면 null이다. 끝났는지는 부르는 쪽이 본다. */
    fun load(nowElapsed: Long, nowWall: Long): RestTimerState? {
        if (!prefs.contains(KEY_ENDS_AT_ELAPSED)) return null
        val endsAtElapsed = prefs.getLong(KEY_ENDS_AT_ELAPSED, 0)
        val wallRemaining = prefs.getLong(KEY_ENDS_AT_WALL, 0) - nowWall
        val elapsedRemaining = endsAtElapsed - nowElapsed
        val endsAt = if (abs(wallRemaining - elapsedRemaining) > CLOCK_TOLERANCE_MILLIS) nowElapsed + wallRemaining else endsAtElapsed
        return RestTimerState(
            workoutId = prefs.getLong(KEY_WORKOUT, 0),
            setId = prefs.getLong(KEY_SET, 0),
            endsAt = endsAt,
            totalMillis = prefs.getLong(KEY_TOTAL, 0),
            nextSet = prefs.getString(KEY_NEXT_SET, null),
        )
    }

    private companion object {
        const val FILE_NAME = "rest_timer"
        const val KEY_WORKOUT = "workout_id"
        const val KEY_SET = "set_id"
        const val KEY_ENDS_AT_ELAPSED = "ends_at_elapsed"
        const val KEY_ENDS_AT_WALL = "ends_at_wall"
        const val KEY_TOTAL = "total_millis"
        const val KEY_NEXT_SET = "next_set"
        const val CLOCK_TOLERANCE_MILLIS = 5_000L
    }
}
