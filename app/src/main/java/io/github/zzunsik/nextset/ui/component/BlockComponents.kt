package io.github.zzunsik.nextset.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/** 목록 행들을 하나의 무채색 면에 담는다. 행 사이 구분선은 호출자가 넣는다. */
@Composable
fun GroupedCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().clip(NextSetTheme.radius.card).background(NextSetTheme.colors.surface),
        content = content,
    )
}

/** LazyColumn에서도 행별 재사용을 유지하면서 하나의 묶음 카드로 그린다. */
@Composable
fun groupedRowShape(first: Boolean, last: Boolean) = RoundedCornerShape(
    topStart = if (first) NextSetTheme.radius.cardSize else 0.dp,
    topEnd = if (first) NextSetTheme.radius.cardSize else 0.dp,
    bottomStart = if (last) NextSetTheme.radius.cardSize else 0.dp,
    bottomEnd = if (last) NextSetTheme.radius.cardSize else 0.dp,
)

enum class StatTileKind { Progress, Record, Neutral }
enum class StatTileValueSize { Hero, Display }

/** 값과 단위의 밑선을 맞추며 큰 글자에서도 높이를 늘리는 숫자 타일. */
@Composable
fun StatTile(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    kind: StatTileKind = StatTileKind.Neutral,
    valueSize: StatTileValueSize = StatTileValueSize.Hero,
    minHeight: Dp = 150.dp,
    onClick: (() -> Unit)? = null,
    supporting: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = NextSetTheme.colors
    val numberStyle = when (valueSize) {
        StatTileValueSize.Hero -> NextSetTheme.typography.numeralHero
        StatTileValueSize.Display -> NextSetTheme.typography.display
    }
    val foreground = when (kind) {
        StatTileKind.Progress -> colors.onGo
        StatTileKind.Record -> colors.ink
        StatTileKind.Neutral -> colors.text
    }
    val background = when (kind) {
        StatTileKind.Progress -> colors.go
        StatTileKind.Record -> colors.record
        StatTileKind.Neutral -> colors.surface
    }
    Column(
        modifier = modifier.fillMaxWidth().clip(NextSetTheme.radius.card).background(background)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = minHeight).padding(NextSetTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (kind == StatTileKind.Record) {
                Icon(painterResource(R.drawable.ic_trophy), contentDescription = null, tint = foreground, modifier = Modifier.size(16.dp))
            }
            Text(label, style = NextSetTheme.typography.label, color = if (kind == StatTileKind.Neutral) colors.text2 else foreground)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                value, style = numberStyle, color = foreground,
                maxLines = 1, modifier = Modifier.weight(1f, fill = false).alignByBaseline(),
                autoSize = TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = numberStyle.fontSize),
            )
            Text(unit, style = NextSetTheme.typography.heading, color = foreground, modifier = Modifier.alignByBaseline())
        }
        CompositionLocalProvider(LocalContentColor provides foreground) { supporting() }
    }
}

/** 종목별 볼륨. 가장 큰 종목을 기준으로 채우며 값은 접근성 설명에 함께 제공한다. */
@Composable
fun HorizontalVolumeBar(
    label: String,
    value: Double,
    maximum: Double,
    valueText: String,
    modifier: Modifier = Modifier,
) {
    val colors = NextSetTheme.colors
    val fraction = if (value.isFinite() && maximum.isFinite() && maximum > 0.0) (value / maximum).coerceIn(0.0, 1.0).toFloat() else 0f
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = NextSetTheme.typography.caption, color = colors.text2)
        Box(Modifier.fillMaxWidth().height(10.dp).clip(NextSetTheme.radius.full).background(colors.surface2)) {
            if (fraction > 0f) Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(colors.go))
        }
        Text(valueText, style = NextSetTheme.typography.label, color = colors.text)
    }
}

/** 막대 전체를 하나의 설명으로 읽는다. 음수·비정상 값은 빈 막대로 표시한다. */
@Composable
fun VolumeBars(
    values: List<Double>,
    description: String,
    modifier: Modifier = Modifier,
    highlightedIndex: Int? = null,
) {
    val colors = NextSetTheme.colors
    val safe = values.map { if (it.isFinite()) it.coerceAtLeast(0.0) else 0.0 }
    val maximum = safe.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
    Row(
        modifier = modifier.height(64.dp).semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom,
    ) {
        safe.forEachIndexed { index, value ->
            Box(
                Modifier.weight(1f).height(if (value == 0.0) 6.dp else (64 * value / maximum).dp.coerceAtLeast(6.dp))
                    .background(
                        when {
                            value == 0.0 -> colors.surface2
                            highlightedIndex == null || highlightedIndex == index -> colors.go
                            else -> colors.text3
                        },
                        RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp),
                    ),
            )
        }
    }
}
