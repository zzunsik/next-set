package io.github.zzunsik.nextset.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/**
 * 세트 행의 완료 체크 버튼(docs/DESIGN.md `SetRow`).
 *
 * - 보이는 상자는 40dp, 누르는 영역은 48dp다(NF-10).
 * - 완료는 색과 체크 모양을 함께 쓴다(NF-06). 안 했으면 `text2` 2dp 테두리 상자,
 *   했으면 `done` 바탕에 `onDone` 체크 모양이다.
 * - 화면 읽기 프로그램에는 체크 상자로 알리고 이름은 [label]이다. 기본값은 "세트 완료"이고, 세트 행이 여럿인 화면은
 *   "벤치 프레스 1세트 완료"처럼 행마다 다른 이름을 넘긴다(같은 이름의 누르는 요소가 여럿이면 ATF가 잡는다).
 */
@Composable
fun DoneCheckButton(
    done: Boolean,
    onDoneChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.set_done),
) {
    val colors = NextSetTheme.colors
    val sizes = NextSetTheme.sizes
    val shape = NextSetTheme.radius.md
    Box(
        modifier = modifier
            .size(sizes.minTouchTarget)
            .semantics { contentDescription = label }
            .toggleable(value = done, role = Role.Checkbox, onValueChange = onDoneChange),
        contentAlignment = Alignment.Center,
    ) {
        val box = Modifier
            .size(sizes.checkButtonVisible)
            .clip(shape)
        if (done) {
            Box(box.background(colors.done), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = colors.onDone,
                    modifier = Modifier.size(sizes.icon),
                )
            }
        } else {
            Box(box.border(2.dp, colors.text2, shape))
        }
    }
}
