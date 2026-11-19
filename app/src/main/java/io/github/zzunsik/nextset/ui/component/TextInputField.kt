package io.github.zzunsik.nextset.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/**
 * 시스템 키보드로 적는 한 줄 입력 칸(docs/DESIGN.md `SearchField`). 검색 칸과 종목 이름 칸이 쓴다.
 *
 * - `surface` 바탕, 높이 48dp 이상, 모서리 `radius.md`. 입력 중에는 `accent` 2dp 테두리.
 * - [leadingIcon]이 있으면 앞에 `text2` 색으로 그린다(검색 칸의 돋보기).
 * - 글자가 있으면 끝에 지우기 버튼이 생긴다. [clearLabel]이 그 버튼의 접근성 이름이다.
 * - 비어 있을 때 [placeholder]를 `text2` 색으로 보인다.
 */
@Composable
fun TextInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    clearLabel: String? = null,
    imeAction: ImeAction = ImeAction.Done,
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    val shape = NextSetTheme.radius.md
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NextSetTheme.sizes.minTouchTarget),
        textStyle = NextSetTheme.typography.body.copy(color = colors.text),
        singleLine = true,
        cursorBrush = SolidColor(colors.accent),
        interactionSource = interaction,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .background(colors.surface, shape)
                    .then(if (focused) Modifier.border(2.dp, colors.accent, shape) else Modifier)
                    .heightIn(min = NextSetTheme.sizes.minTouchTarget)
                    .padding(start = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Icon(
                        painter = painterResource(leadingIcon),
                        contentDescription = null,
                        tint = colors.text2,
                        modifier = Modifier.size(NextSetTheme.sizes.icon),
                    )
                    Box(Modifier.width(spacing.sm))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = spacing.sm),
                ) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = NextSetTheme.typography.body, color = colors.text2)
                    }
                    innerTextField()
                }
                if (value.isNotEmpty() && clearLabel != null) {
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(NextSetTheme.sizes.minTouchTarget),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = clearLabel,
                            tint = colors.text2,
                        )
                    }
                } else {
                    Box(Modifier.width(spacing.md))
                }
            }
        },
    )
}

/** 종목 고르기의 검색 칸. 글자를 칠 때마다 [onQueryChange]가 불린다(EX-02). */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextInputField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(R.string.exercise_search_hint),
        modifier = modifier,
        leadingIcon = R.drawable.ic_search,
        clearLabel = stringResource(R.string.exercise_search_clear),
        imeAction = ImeAction.Search,
    )
}
