package io.github.zzunsik.nextset.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/** 배지의 높이와 아이콘만 있는 배지의 지름(docs/DESIGN.md `RecordBadge`). */
private val BADGE_SIZE = 24.dp

/** 트로피 아이콘의 크기. */
private val BADGE_ICON_SIZE = 16.dp

/**
 * 최고 기록 배지(docs/DESIGN.md `RecordBadge` 기본). `text` 바탕에 `bg` 색 트로피와 "최고 기록"(`caption` SemiBold).
 * 모서리 `radius.full`, 높이 24dp. 운동 요약과 기록 상세에서 쓴다. 누르는 요소가 아니다.
 * 세 번째 색을 쓰지 않고 반전된 모양과 아이콘으로 눈에 띈다.
 */
@Composable
fun RecordBadge(modifier: Modifier = Modifier) {
    val colors = NextSetTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = BADGE_SIZE)
            .background(colors.text, NextSetTheme.radius.full)
            .padding(horizontal = NextSetTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.xs),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_trophy),
            contentDescription = null,
            tint = colors.bg,
            modifier = Modifier.size(BADGE_ICON_SIZE),
        )
        Text(
            text = stringResource(R.string.record_badge),
            style = NextSetTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = colors.bg,
        )
    }
}

/**
 * 아이콘만 있는 최고 기록 배지(docs/DESIGN.md `RecordBadge` "아이콘만"). 지름 24dp 원, `text` 바탕에 `bg` 색 트로피.
 * 접근성 이름 "최고 기록"을 붙인다. 세트 행과 기록 목록 행에서 쓴다.
 */
@Composable
fun RecordBadgeIcon(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.record_badge)
    Box(
        modifier = modifier
            .size(BADGE_SIZE)
            .background(NextSetTheme.colors.text, CircleShape)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_trophy),
            contentDescription = null,
            tint = NextSetTheme.colors.bg,
            modifier = Modifier.size(BADGE_ICON_SIZE),
        )
    }
}
