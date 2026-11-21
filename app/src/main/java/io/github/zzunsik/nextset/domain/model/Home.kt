package io.github.zzunsik.nextset.domain.model

import java.time.Instant

/**
 * 홈 화면이 한 시점에 읽은 데이터(docs/DESIGN.md "홈", HOME-01~HOME-06). 저장소가 한 읽기 트랜잭션에서 모두 읽으므로
 * 서로 어긋나지 않는다. 예) 운동을 끝낸 직후 맨 위 루틴은 다음 차례로 바뀌었는데 진행 중인 운동은 아직 남아 있는 조합은
 * 나오지 않는다(#97).
 */
data class HomeSnapshot(
    /** 진행 중인 운동. 없으면 null이다. */
    val inProgress: Workout?,
    /** 모든 루틴. 목록 순서대로(RTN-05). */
    val routines: List<HomeRoutine>,
    /** 맨 위 카드의 루틴. 직접 고른 루틴이 있으면 그것, 없으면 다음 차례다. 루틴이 없으면 null이다. */
    val top: Routine?,
    /** 직접 고른 루틴(HOME-03). 없으면 null이다. */
    val pinnedRoutineId: Long?,
)

/** 홈의 루틴 하나. */
data class HomeRoutine(
    val routine: Routine,
    /** 루틴의 종목 이름. 루틴 순서대로. */
    val exerciseNames: List<String>,
    /** 이 루틴으로 운동해 끝낸 기록 가운데 가장 늦게 시작한 시각. 없으면 null이다. */
    val lastStartedAt: Instant?,
)
