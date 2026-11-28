package io.github.zzunsik.nextset.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/**
 * 하나를 고르는 칩(docs/DESIGN.md `BodyPartFilter`). 부위 필터와 종목 추가·수정의 선택 칸이 쓴다.
 *
 * - 기본: `surface` 바탕, 글자 `text2`(`bodyStrong`)를 가운데에. 보이는 높이 36dp, 누르는 영역 48dp.
 * - 선택됨: `accentSoft` 바탕, 글자 `accentText`, 앞에 체크 아이콘. 색만으로 알리지 않도록 체크를 둔다.
 * - 칩 폭은 고르기 전후로 같다. 늘 체크 아이콘이 들어갈 폭을 잡아 두고, 고르지 않았을 때는 글자를
 *   그 폭의 가운데에 놓는다. 고를 때 폭이 늘면 여러 줄로 놓인 칩이 다른 줄로 밀린다(#56).
 * - 모서리는 `radius.full`.
 *
 * M3 `FilterChip`은 누르는 요소 자체가 보이는 크기(32dp)라 48dp 검사(NF-10)를 통과하지 못한다.
 * 그래서 48dp 상자가 누름을 받고 안에 보이는 칩을 그린다. 높이와 아이콘 크기는 M3 칩의 값을 쓴다.
 * 화면 읽기 프로그램에는 한 줄에서 하나를 고르는 단추(RadioButton)로 알린다.
 */
@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    val shape = NextSetTheme.radius.full
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .heightIn(min = NextSetTheme.sizes.minTouchTarget)
            .widthIn(min = NextSetTheme.sizes.minTouchTarget)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Layout(
            content = {
                // 고르지 않았을 때도 폭을 재려고 늘 만들고, 고른 때만 놓아 그린다.
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
                Text(
                    text = text,
                    style = NextSetTheme.typography.bodyStrong,
                    color = if (selected) colors.accentText else colors.text2,
                    modifier = Modifier.padding(vertical = spacing.xs),
                )
            },
            modifier = Modifier
                .heightIn(min = 36.dp)
                .clip(shape)
                .then(
                    if (selected) {
                        Modifier.background(colors.accentSoft, shape)
                    } else {
                        Modifier.background(colors.surface, shape)
                    },
                )
                .indication(interaction, ripple()),
        ) { measurables, constraints ->
            val start = spacing.sm.roundToPx()
            val gap = spacing.sm.roundToPx()
            val end = spacing.md.roundToPx()
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val icon = measurables[0].measure(loose)
            val reserved = start + icon.width + gap + end
            val label = measurables[1].measure(loose.offset(horizontal = -reserved))
            val width = (reserved + label.width).coerceIn(constraints.minWidth, constraints.maxWidth)
            val height = maxOf(icon.height, label.height).coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(width, height) {
                if (selected) {
                    icon.place(start, (height - icon.height) / 2)
                    label.place(start + icon.width + gap, (height - label.height) / 2)
                } else {
                    label.place((width - label.width) / 2, (height - label.height) / 2)
                }
            }
        }
    }
}
