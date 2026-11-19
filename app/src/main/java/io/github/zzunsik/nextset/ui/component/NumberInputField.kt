package io.github.zzunsik.nextset.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * 숫자판으로 채우는 입력 칸(docs/DESIGN.md `SetRow`의 무게·횟수 칸). 누르면 [onClick]으로 알리고
 * 화면이 [NumberPadSession]을 연다. 시스템 키보드를 띄우지 않는다(LOG-07).
 *
 * - 보이는 높이 40dp, 누르는 영역 48dp 이상(NF-10). 폭은 부르는 쪽이 정한다(무게 72dp, 횟수 64dp 등).
 * - 선택: `accentSoft` 바탕에 `accent` 2dp 테두리. 값은 `text` 색.
 * - 값 없음: `text2` 1dp 테두리.
 * - [dimmed]: 아직 안 한 세트의 값처럼 `text2` 색으로 쓴다.
 * - 선택된 동안 [NumberPadScaffold]의 하단 바와 숫자판이 덮은 높이만큼 자기 아래에 자리를 두도록 스크롤을 옮긴다.
 *   그래서 숫자판이 올라와도 칸이 가려지지 않는다.
 *
 * @param value 칸에 보일 글자. [NumberEntry.displayText]나 [NumberEntry.displayTextOf]로 만든다.
 * @param label 화면 읽기 프로그램이 값 앞에 읽는 칸 이름("무게").
 */
@Composable
fun NumberInputField(
    value: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    val colors = NextSetTheme.colors
    val shape = NextSetTheme.radius.md
    val description = stringResource(
        R.string.number_field_description,
        label,
        value.ifEmpty { stringResource(R.string.number_field_empty) },
    )
    val requester = remember { BringIntoViewRequester() }
    val size = remember { mutableStateOf(IntSize.Zero) }
    val overlap = LocalNumberPadOverlap.current
    LaunchedEffect(selected, overlap) {
        if (!selected) return@LaunchedEffect
        // 숫자판이 올라오는 동안 덮인 높이가 바뀔 때마다 칸 아래로 그만큼의 자리를 두고 보이게 한다.
        snapshotFlow { (overlap?.intValue ?: 0) to size.value }.collectLatest { (covered, field) ->
            requester.bringIntoView(Rect(0f, 0f, field.width.toFloat(), (field.height + covered).toFloat()))
        }
    }
    val border = when {
        selected -> Modifier.border(2.dp, colors.accent, shape)
        value.isEmpty() -> Modifier.border(1.dp, colors.text2, shape)
        else -> Modifier
    }
    Box(
        modifier = modifier
            .bringIntoViewRequester(requester)
            .onSizeChanged { size.value = it }
            .heightIn(min = NextSetTheme.sizes.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                this.selected = selected
                onClick { onClick(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = NextSetTheme.sizes.inputVisibleHeight)
                .clip(shape)
                .background(if (selected) colors.accentSoft else colors.surface)
                .then(border),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value,
                style = NextSetTheme.typography.numeralRow,
                color = if (dimmed && !selected) colors.text2 else colors.text,
                textAlign = TextAlign.Center,
            )
        }
    }
}
