package io.github.zzunsik.nextset.ui.home

import io.github.zzunsik.nextset.domain.model.Routine
import java.time.Instant
import java.time.LocalDate

/** 홈 화면 테스트의 지어낸 루틴과 운동. */
internal object HomeSamples {
    val today: LocalDate = LocalDate.of(2026, 11, 20)
    val startedAt: Instant = Instant.parse("2026-11-20T09:00:00Z")
    val now: Instant = startedAt.plusSeconds(32 * 60)

    val chest = HomeRoutineUi(
        routine = Routine(1, "가슴과 삼두", 0),
        exerciseNames = listOf("벤치 프레스", "인클라인 덤벨 프레스", "케이블 크로스오버", "딥스", "푸시다운"),
        lastDoneOn = LocalDate.of(2026, 11, 14),
    )
    val back = HomeRoutineUi(
        routine = Routine(2, "등과 이두", 1),
        exerciseNames = listOf("랫 풀다운", "바벨 로우", "바벨 컬", "해머 컬"),
        lastDoneOn = LocalDate.of(2026, 11, 16),
    )
    val legs = HomeRoutineUi(
        routine = Routine(3, "하체", 2),
        exerciseNames = listOf("백 스쿼트", "루마니안 데드리프트", "레그 프레스"),
        lastDoneOn = LocalDate.of(2025, 12, 30),
    )
    val shoulders = HomeRoutineUi(Routine(4, "어깨", 3), listOf("오버헤드 프레스", "사이드 레터럴 레이즈"), lastDoneOn = null)

    val all = listOf(chest, back, legs, shoulders)

    /** 루틴 여럿. 다음 차례는 "등과 이두"다. */
    val many = HomeUiState(loading = false, top = back, others = all - back, routines = all)

    val one = HomeUiState(loading = false, top = chest, others = emptyList(), routines = listOf(chest))

    val empty = HomeUiState(loading = false)

    val pinned = HomeUiState(loading = false, top = legs, topPinned = true, others = all - legs, routines = all)

    val inProgressWorkout = InProgressWorkoutUi(
        workoutId = 10,
        routineName = "가슴과 삼두",
        totalSets = 12,
        completedSets = 5,
        startedAt = startedAt,
    )

    val resume = many.copy(inProgress = inProgressWorkout, others = all)

    val inProgressPrompt = resume.copy(dialog = HomeDialog.InProgress(inProgressWorkout, PendingStart.FromRoutine(3)))
}
