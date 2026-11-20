package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.Settings

/**
 * 종목에 적용할 휴식 시간(REST-03). 종목에 따로 정한 값이 있으면 그 값, 없으면 설정의 기본값이다
 * (docs/architecture.md "종목별 휴식 시간"). 휴식 타이머(#26)는 이 함수로 시간을 정한다.
 */
fun restSecondsFor(exercise: Exercise, settings: Settings): Int =
    restSecondsFor(exercise.restSeconds, settings.defaultRestSeconds)

/** [restSecondsFor]와 같은 규칙. 종목에 정한 값 [exerciseRestSeconds]가 없으면 기본값 [defaultRestSeconds]다. */
fun restSecondsFor(exerciseRestSeconds: Int?, defaultRestSeconds: Int): Int = exerciseRestSeconds ?: defaultRestSeconds

/** 설정에서 고를 수 있는 기본 휴식 시간(SET-01). 30초부터 5분까지 15초 단위, 짧은 것부터. */
val defaultRestChoices: List<Int> =
    (Settings.MIN_REST_SECONDS..Settings.MAX_REST_SECONDS step Settings.REST_STEP_SECONDS).toList()
