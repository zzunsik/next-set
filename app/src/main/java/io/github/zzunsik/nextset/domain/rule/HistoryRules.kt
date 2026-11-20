package io.github.zzunsik.nextset.domain.rule

import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.domain.model.Workout
import java.time.Duration

/**
 * 운동 기록 한 건의 숫자(HIST-02). 완료한 세트만 센다(docs/architecture.md `workout_set`).
 *
 * @property minutes 운동 시간. 시작부터 끝까지의 분이다. 진행 중이면 0이다.
 * @property setCount 완료한 세트 수. 기록 방식과 상관없이 모두 센다.
 * @property volumeGrams 볼륨. 무게와 횟수로 적은 완료 세트의 무게 × 횟수를 더한 그램이다.
 *   횟수만 종목과 시간 종목은 넣지 않는다(#27 제안). 그램을 Long으로 더해 넘치지 않는다.
 */
data class WorkoutStats(val minutes: Long, val setCount: Int, val volumeGrams: Long)

fun Workout.stats(): WorkoutStats {
    val completed = exercises.flatMap { exercise -> exercise.sets.filter { it.isCompleted }.map { exercise.recordType to it.value } }
    val volume = completed.sumOf { (type, value) -> if (type == RecordType.WEIGHT_REPS) value.volumeGrams() else 0L }
    val minutes = finishedAt?.let { Duration.between(startedAt, it).toMinutes().coerceAtLeast(0) } ?: 0
    return WorkoutStats(minutes = minutes, setCount = completed.size, volumeGrams = volume)
}

private fun SetValue.volumeGrams(): Long {
    val grams = weight?.grams ?: return 0
    val count = reps ?: return 0
    return grams.toLong() * count
}

/**
 * 최고 기록을 세운 세트(HIST-03). [workouts]는 끝난 운동 기록 전부다. 돌려주는 것은 표시를 붙일 세트의 식별자다.
 *
 * docs/architecture.md "최고 기록"의 요약 표시 규칙을 기록 전체에 한 번에 적용한다.
 * - 종목과 기록 방식(운동 기록 종목의 방식 사본)이 같은 완료 세트끼리만 견준다(HIST-04).
 * - 운동마다, 그 운동보다 먼저 시작한 운동의 최고 기록을 출발점으로 그 운동의 세트를 종목 순서, 세트 순서대로 본다.
 *   그때까지의 최고를 넘은 세트([isBetter])에 표시한다. 값이 같으면 넘은 것이 아니다(먼저 세운 쪽이 남는다).
 * - 먼저 시작한 운동에 같은 방식의 완료 세트가 없으면 그 운동에서 그 종목은 표시하지 않는다(처음 한 종목).
 * - 시작 시각이 같은 운동끼리는 서로 "먼저"가 아니다(질의의 `w.started_at < :startedAt`과 같다).
 *
 * 기록을 고치거나 지우면 다시 부르기만 하면 된다. 따로 저장한 것이 없다.
 */
fun recordSetIds(workouts: List<Workout>): Set<Long> {
    val best = mutableMapOf<Pair<String, RecordType>, SetValue>()
    val marked = mutableSetOf<Long>()
    workouts.groupBy { it.startedAt }.toSortedMap().values.forEach { sameStart ->
        val before = best.toMap()
        sameStart.forEach { workout ->
            val running = mutableMapOf<Pair<String, RecordType>, SetValue>()
            workout.exercises.sortedBy { it.position }.forEach { exercise ->
                val key = exercise.exercise.id to exercise.recordType
                exercise.sets.sortedBy { it.position }.filter { it.isCompleted }.forEach { set ->
                    val current = running[key] ?: before[key] ?: return@forEach
                    if (isBetter(exercise.recordType, set.value, current)) {
                        marked += set.id
                        running[key] = set.value
                    }
                }
            }
        }
        // 이 시각에 시작한 운동을 다 본 뒤에 최고 기록을 넓힌다.
        sameStart.forEach { workout ->
            workout.exercises.forEach { exercise ->
                val key = exercise.exercise.id to exercise.recordType
                bestOf(exercise.recordType, exercise.sets.filter { it.isCompleted }.map { it.value })?.let { value ->
                    val current = best[key]
                    if (current == null || isBetter(exercise.recordType, value, current)) best[key] = value
                }
            }
        }
    }
    return marked
}

/**
 * 같은 루틴으로 한 바로 앞의 기록. 요약의 "지난번보다 +120kg"에 쓴다(docs/DESIGN.md "운동 요약").
 * 빈 운동(루틴 없음)이면 null이다. 루틴 이름이 아니라 루틴 식별자로 찾는다.
 */
fun Workout.previousOfSameRoutine(workouts: List<Workout>): Workout? {
    val routine = routineId ?: return null
    return workouts
        .filter { it.id != id && it.routineId == routine && it.finishedAt != null && it.startedAt < startedAt }
        .maxWithOrNull(compareBy<Workout> { it.startedAt }.thenBy { it.id })
}
