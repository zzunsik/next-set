package io.github.zzunsik.nextset.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** docs/DESIGN.md "간격, 모서리, 누르는 영역"의 간격 토큰(`space.*`). */
@Immutable
data class NextSetSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

/**
 * 모서리 토큰(`radius.*`).
 * `radius.full`(높이의 절반)은 [RoundedCornerShape]`(percent = 50)`로 둔다.
 */
@Immutable
data class NextSetRadius(
    /** `radius.md`의 반지름. 카드, 버튼, 입력 칸, 체크 버튼, 숫자판 키, 검색 칸. */
    val mdSize: Dp = 8.dp,
) {
    val md: RoundedCornerShape = RoundedCornerShape(mdSize)

    /** `radius.full`. 칩, 최고 기록 배지. */
    val full: RoundedCornerShape = RoundedCornerShape(percent = 50)
}

/** 크기 표의 값. 높이는 고정하지 않고 최소 높이로 쓴다(글자가 커지면 늘어난다). */
@Immutable
data class NextSetSizes(
    /** 누르는 요소의 최소 영역(NF-10). 보이는 모양이 작아도 영역은 이 크기를 채운다. */
    val minTouchTarget: Dp = 48.dp,
    val setRowMinHeight: Dp = 48.dp,
    val listRowMinHeight: Dp = 56.dp,
    val inputVisibleHeight: Dp = 40.dp,
    val checkButtonVisible: Dp = 40.dp,
    val numberPadKeyHeight: Dp = 52.dp,
    val primaryButtonHeight: Dp = 56.dp,
    val buttonHeight: Dp = 48.dp,
    val icon: Dp = 24.dp,
)

/**
 * M3 컴포넌트의 모양. 카드, 입력 칸처럼 작은 요소가 쓰는 역할을 `radius.md`로 둔다.
 * 하단 시트와 대화상자가 쓰는 `large`, `extraLarge`는 M3 기본 모양을 그대로 둔다(DESIGN.md).
 * M3 버튼은 이 값이 아니라 완전히 둥근 모양을 쓰므로 버튼을 만들 때 `radius.md`를 직접 넘긴다.
 */
internal fun NextSetRadius.toMaterialShapes() = Shapes(
    extraSmall = md,
    small = md,
    medium = md,
)
