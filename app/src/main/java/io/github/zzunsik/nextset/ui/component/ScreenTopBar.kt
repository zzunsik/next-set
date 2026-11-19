package io.github.zzunsik.nextset.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/**
 * 하위 화면의 상단 바. 뒤로 가기, 제목(`title`), 오른쪽 글자 버튼 자리.
 * M3의 `TopAppBar`는 실험 API라 쓰지 않는다(docs/DESIGN.md "Material 3 요소를 고르는 규칙").
 * 높이는 고정하지 않는다. 글자가 커지면 늘어난다.
 */
@Composable
fun ScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = NextSetTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = NextSetTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.xs),
    ) {
        // IconButton의 누르는 요소는 40dp라 48dp 상자로 키운다(NF-10).
        IconButton(onClick = onBack, modifier = Modifier.size(NextSetTheme.sizes.minTouchTarget)) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.navigate_back),
                tint = colors.text,
            )
        }
        Text(
            text = title,
            style = NextSetTheme.typography.title,
            color = colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        actions()
    }
}
