package io.github.zzunsik.nextset.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.IntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.Weight
import io.github.zzunsik.nextset.ui.theme.NextSetMotion
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/**
 * 자체 숫자판(docs/DESIGN.md `NumberPad`, LOG-07). 시스템 키보드 대신 쓴다.
 *
 * 네 열 배치다. 맨 위는 증감 줄이고 "다음" 키가 2~4줄의 4열을 세로로 차지한다.
 * 칸 종류에 없는 키(횟수·시간 칸의 소수점)는 자리만 남기고 그리지 않는다.
 * 키를 누르면 [onKey]로 알린다. 값 계산은 [NumberPadSession]이 한다.
 */
@Composable
fun NumberPad(
    kind: NumberFieldKind,
    isLastField: Boolean,
    onKey: (NumberPadKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    Column(modifier = modifier.fillMaxWidth().background(colors.surface)) {
        HorizontalDivider(color = colors.line)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
        ) {
            // 네 열의 폭. 증감 줄과 숫자 줄의 열이 맞도록 폭을 직접 나눈다.
            val gap = spacing.sm
            val width = maxWidth
            val column = (width - gap * 3) / 4
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                KeyRow(gap) {
                    val stepWidth = (width - gap * (kind.steps.size - 1)) / kind.steps.size
                    kind.steps.forEach { StepKey(kind, it, onKey, Modifier.width(stepWidth).fillMaxHeight()) }
                }
                KeyRow(gap) {
                    (1..3).forEach { DigitKey(it, onKey, column) }
                    BackspaceKey(onKey, Modifier.width(column).fillMaxHeight())
                }
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    Column(
                        modifier = Modifier.width(column * 3 + gap * 2),
                        verticalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        KeyRow(gap) { (4..6).forEach { DigitKey(it, onKey, column) } }
                        KeyRow(gap) { (7..9).forEach { DigitKey(it, onKey, column) } }
                        KeyRow(gap) {
                            if (kind.hasDecimalPoint) {
                                DecimalPointKey(onKey, Modifier.width(column).fillMaxHeight())
                            } else {
                                Spacer(Modifier.width(column))
                            }
                            DigitKey(0, onKey, column)
                            CloseKey(onKey, Modifier.width(column).fillMaxHeight())
                        }
                    }
                    NextKey(isLastField, onKey, Modifier.width(column).fillMaxHeight())
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun KeyRow(gap: Dp, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) { content() }
}

/**
 * 키 하나. 높이는 52dp 이상이고 글자가 커지면 늘어난다(NF-10). 모서리는 `radius.md`다.
 * [description]이 있으면 화면 읽기 프로그램에 안의 글자 대신 이 이름을 알린다.
 */
@Composable
private fun PadKey(
    onClick: () -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
    description: String? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val described = if (description == null) {
        Modifier
    } else {
        Modifier.clearAndSetSemantics {
            contentDescription = description
            role = Role.Button
        }
    }
    Box(
        modifier = modifier
            .heightIn(min = NextSetTheme.sizes.numberPadKeyHeight)
            .clip(NextSetTheme.radius.md)
            .background(background)
            .combinedClickable(
                role = Role.Button,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel,
                onClick = onClick,
            )
            .then(described),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun DigitKey(digit: Int, onKey: (NumberPadKey) -> Unit, width: Dp) {
    PadKey(
        onClick = { onKey(NumberPadKey.Digit(digit)) },
        background = NextSetTheme.colors.bg,
        modifier = Modifier.width(width).fillMaxHeight(),
    ) {
        Text(text = "$digit", style = NextSetTheme.typography.title, color = NextSetTheme.colors.text)
    }
}

@Composable
private fun DecimalPointKey(onKey: (NumberPadKey) -> Unit, modifier: Modifier) {
    PadKey(
        onClick = { onKey(NumberPadKey.DecimalPoint) },
        background = NextSetTheme.colors.bg,
        modifier = modifier,
        description = stringResource(R.string.number_pad_decimal_point_description),
    ) {
        Text(text = ".", style = NextSetTheme.typography.title, color = NextSetTheme.colors.text)
    }
}

/** 증감 키. 값을 글자로 적고("+2.5") 화면 읽기 프로그램에는 "2.5 더하기"로 알린다. */
@Composable
private fun StepKey(kind: NumberFieldKind, amount: Int, onKey: (NumberPadKey) -> Unit, modifier: Modifier) {
    val size = Math.abs(amount)
    val plus = amount > 0
    val (text, description) = if (kind == NumberFieldKind.DURATION) {
        stringResource(if (plus) R.string.number_pad_step_plus_seconds else R.string.number_pad_step_minus_seconds, size) to
            stringResource(
                if (plus) R.string.number_pad_step_add_seconds_description else R.string.number_pad_step_subtract_seconds_description,
                size,
            )
    } else {
        val value = if (kind == NumberFieldKind.WEIGHT) Weight(size).toKgText() else "$size"
        stringResource(if (plus) R.string.number_pad_step_plus else R.string.number_pad_step_minus, value) to
            stringResource(
                if (plus) R.string.number_pad_step_add_description else R.string.number_pad_step_subtract_description,
                value,
            )
    }
    PadKey(
        onClick = { onKey(NumberPadKey.Step(amount)) },
        background = NextSetTheme.colors.bg,
        modifier = modifier,
        description = description,
    ) {
        Text(
            text = text,
            style = NextSetTheme.typography.label,
            color = NextSetTheme.colors.accent,
            textAlign = TextAlign.Center,
        )
    }
}

/** 지우기. 한 번 누르면 한 글자, 길게 누르면 전부 지운다. */
@Composable
private fun BackspaceKey(onKey: (NumberPadKey) -> Unit, modifier: Modifier) {
    PadKey(
        onClick = { onKey(NumberPadKey.Backspace) },
        background = NextSetTheme.colors.bg,
        modifier = modifier,
        description = stringResource(R.string.number_pad_backspace),
        onLongClick = { onKey(NumberPadKey.ClearAll) },
        onLongClickLabel = stringResource(R.string.number_pad_clear_all),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_backspace),
            contentDescription = null,
            tint = NextSetTheme.colors.text,
            modifier = Modifier.size(NextSetTheme.sizes.icon),
        )
    }
}

@Composable
private fun CloseKey(onKey: (NumberPadKey) -> Unit, modifier: Modifier) {
    PadKey(
        onClick = { onKey(NumberPadKey.Close) },
        background = NextSetTheme.colors.bg,
        modifier = modifier,
        description = stringResource(R.string.number_pad_close),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_keyboard_hide),
            contentDescription = null,
            tint = NextSetTheme.colors.text,
            modifier = Modifier.size(NextSetTheme.sizes.icon),
        )
    }
}

/** "다음". 채운 강조색 키다. 행의 마지막 칸에서는 "완료"로 바뀐다. */
@Composable
private fun NextKey(isLastField: Boolean, onKey: (NumberPadKey) -> Unit, modifier: Modifier) {
    PadKey(
        onClick = { onKey(NumberPadKey.Next) },
        background = NextSetTheme.colors.accent,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(if (isLastField) R.string.number_pad_done else R.string.number_pad_next),
            style = NextSetTheme.typography.label,
            color = NextSetTheme.colors.onAccent,
            textAlign = TextAlign.Center,
        )
    }
}

/** 열린 숫자판이 보일 키 구성. */
data class NumberPadSpec(val kind: NumberFieldKind, val isLastField: Boolean)

/** [NumberPadSession]에서 숫자판의 키 구성을 꺼낸다. 닫혀 있으면 null. */
val NumberPadSession<*>?.padSpec: NumberPadSpec?
    get() = this?.let { NumberPadSpec(it.focused.kind, it.isLastField) }

/**
 * 본문 아래를 덮은 하단 바와 숫자판의 높이(px). [NumberInputField]가 선택된 동안 이 값이 바뀌면
 * 자기 아래로 이만큼의 자리를 두고 보이도록 스크롤을 옮긴다.
 */
internal val LocalNumberPadOverlap = staticCompositionLocalOf<IntState?> { null }

/**
 * 숫자판을 쓰는 화면의 틀. 본문([content]) 위 아래쪽에 하단 바([bottomBar], 휴식 타이머 바 자리)와
 * 숫자판을 겹쳐 놓는다. 하단 바는 숫자판 바로 위에 붙는다(휴식 타이머 바, #26).
 *
 * - 숫자판은 아래에서 올라온다(docs/DESIGN.md "모션": standard spatial 기본).
 * - [content]는 하단 바와 숫자판이 덮은 높이를 아래 여백으로 받는다. 목록의 `contentPadding`으로 넘기면
 *   마지막 행도 덮이지 않는 자리까지 올릴 수 있다(docs/DESIGN.md `RestTimerBar`: 목록의 아래 여백을 늘린다).
 * - 본문의 [NumberInputField]는 선택된 동안 덮인 높이가 바뀌면 자기가 덮이지 않도록 목록을 밀어 올린다.
 *   그래서 숫자판이 올라와도 입력하는 칸이 가려지지 않는다.
 *
 * @param numberPad 열린 숫자판의 키 구성. null이면 닫힌다. 보통 `session.padSpec`을 넘긴다.
 */
@Composable
fun NumberPadScaffold(
    numberPad: NumberPadSpec?,
    onNumberPadKey: (NumberPadKey) -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    // 닫히는 동안에도 마지막 키 구성을 그린다. 그리는 데만 쓰는 값이라 스냅숏 상태로 두지 않는다.
    val lastSpec = remember { arrayOfNulls<NumberPadSpec>(1) }
    if (numberPad != null) lastSpec[0] = numberPad
    val overlap = remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val motion = NextSetMotion.standardSpatialDefault
    Box(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalNumberPadOverlap provides overlap) {
            content(PaddingValues(bottom = with(density) { overlap.intValue.toDp() }))
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { overlap.intValue = it.height },
        ) {
            bottomBar()
            AnimatedVisibility(
                visible = numberPad != null,
                enter = expandVertically(motion.spec(IntSize.VisibilityThreshold), expandFrom = Alignment.Top),
                exit = shrinkVertically(motion.spec(IntSize.VisibilityThreshold), shrinkTowards = Alignment.Top),
            ) {
                val spec = numberPad ?: lastSpec[0]
                if (spec != null) NumberPad(spec.kind, spec.isLastField, onNumberPadKey)
            }
        }
    }
}
