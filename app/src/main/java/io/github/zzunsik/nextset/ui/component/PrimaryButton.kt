package io.github.zzunsik.nextset.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/**
 * 주요 버튼(docs/DESIGN.md "크기": 운동 시작, 이어서 하기).
 * 채운 강조색, 화면 폭을 채우고 높이는 56dp 이상, 모서리는 `radius.md`, 글자는 `label`이다.
 * 높이를 고정하지 않아 글자가 커지면 버튼이 늘어난다(NF-10).
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val spacing = NextSetTheme.spacing
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NextSetTheme.sizes.primaryButtonHeight),
        enabled = enabled,
        shape = NextSetTheme.radius.md,
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.sm),
    ) {
        Text(text = text, style = NextSetTheme.typography.label, textAlign = TextAlign.Center)
    }
}
