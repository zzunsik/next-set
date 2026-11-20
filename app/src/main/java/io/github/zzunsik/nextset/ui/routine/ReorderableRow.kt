package io.github.zzunsik.nextset.ui.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import sh.calvin.reorderable.ReorderableCollectionItemScope

/**
 * 손잡이를 끌어 순서를 바꾸는 목록 행(docs/DESIGN.md `ExerciseRow`의 루틴 편집 상태, `RoutineRow`의 순서 편집 상태).
 * 앞에 순서 바꾸기 손잡이, 이름(`body`), 보조 줄(`caption`, `text2`). 최소 높이 56dp.
 *
 * - 끄는 동안 행 바탕이 `surface`가 된다.
 * - 손잡이를 끌면 [Reorderable](https://github.com/Calvin-LL/Reorderable)이 행을 옮긴다. 놓으면 [onDragStopped]가 불린다.
 * - 화면 낭독기에는 "위로 옮기기", "아래로 옮기기" 동작을 붙인다([onMove], 끌기가 어려운 사용자를 위한 길).
 * - 행을 누르면 [onClick]. [onClickLabel]은 화면 낭독기가 읽는 동작 이름이다.
 * - [trailing]은 행 끝의 버튼 자리다(홈 순서 편집의 지우기 버튼).
 */
@Composable
internal fun ReorderableCollectionItemScope.ReorderableRow(
    title: String,
    caption: String?,
    onClick: () -> Unit,
    onClickLabel: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (offset: Int) -> Unit,
    onDragStopped: () -> Unit,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    val moveUp = stringResource(R.string.move_up)
    val moveDown = stringResource(R.string.move_down)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isDragging) colors.surface else colors.bg)
            .heightIn(min = NextSetTheme.sizes.listRowMinHeight)
            .clickable(onClickLabel = onClickLabel, onClick = onClick)
            .semantics {
                customActions = buildList {
                    if (canMoveUp) add(CustomAccessibilityAction(moveUp) { onMove(-1); true })
                    if (canMoveDown) add(CustomAccessibilityAction(moveDown) { onMove(1); true })
                }
            }
            .padding(start = spacing.xs, end = if (trailing != null) spacing.xs else spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        // 끄는 영역은 48dp다(NF-10). 누르는 요소가 아니라 화면 낭독기에는 행의 동작으로 알린다.
        Box(
            modifier = Modifier
                .size(NextSetTheme.sizes.minTouchTarget)
                .draggableHandle(onDragStopped = onDragStopped),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_drag_indicator),
                contentDescription = null,
                tint = colors.text2,
                modifier = Modifier.size(NextSetTheme.sizes.icon),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(text = title, style = NextSetTheme.typography.body, color = colors.text)
            if (caption != null) {
                Text(text = caption, style = NextSetTheme.typography.caption, color = colors.text2)
            }
        }
        trailing?.invoke()
    }
}

/**
 * 끄는 동안 화면에 보일 순서. [items]가 바뀌면(저장소가 새 순서를 내보내면) 그 순서로 돌아간다.
 * 끄는 동안에는 이 목록만 바꾸고, 손을 놓으면 한 번 저장한다.
 */
@Composable
internal fun <T> rememberDragOrder(items: List<T>): MutableState<List<T>> = remember(items) { mutableStateOf(items) }

/** Reorderable의 onMove. [from]과 [to]의 키로 [order] 안의 자리를 찾아 옮긴다. 목록 밖의 항목(머리, 버튼)은 무시한다. */
internal fun <T> moveByKey(order: MutableState<List<T>>, key: (T) -> Any, from: LazyListItemInfo, to: LazyListItemInfo) {
    val list = order.value
    val fromIndex = list.indexOfFirst { key(it) == from.key }
    val toIndex = list.indexOfFirst { key(it) == to.key }
    if (fromIndex < 0 || toIndex < 0) return
    order.value = list.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}
