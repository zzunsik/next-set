package io.github.zzunsik.nextset.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/**
 * 하나를 고르는 칩(docs/DESIGN.md `BodyPartFilter`). 부위 필터와 종목 추가·수정의 선택 칸이 쓴다.
 *
 * - 기본: `text2` 1dp 테두리, 글자 `text`(`label`). 보이는 높이 32dp, 누르는 영역 48dp.
 * - 선택됨: `accentSoft` 바탕, 글자 `accent`, 앞에 체크 아이콘.
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
        Row(
            modifier = Modifier
                .heightIn(min = FilterChipDefaults.Height)
                .clip(shape)
                .then(
                    if (selected) {
                        Modifier.background(colors.accentSoft, shape)
                    } else {
                        Modifier.border(1.dp, colors.text2, shape)
                    },
                )
                .indication(interaction, ripple())
                .padding(start = if (selected) spacing.sm else spacing.md, end = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
            Text(
                text = text,
                style = NextSetTheme.typography.label,
                color = if (selected) colors.accent else colors.text,
                modifier = Modifier.padding(vertical = spacing.xs),
            )
        }
    }
}
