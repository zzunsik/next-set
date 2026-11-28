package io.github.zzunsik.nextset.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/** 종목 목록 행의 상태(docs/DESIGN.md `ExerciseRow`). */
sealed interface ExerciseRowMode {
    /** 고르기. 끝에 체크 상자가 있고 행을 누르면 선택이 켜지고 꺼진다. */
    data class Pick(val selected: Boolean, val onToggle: () -> Unit) : ExerciseRowMode

    /** 관리. 끝 요소가 없다. [onEdit]이 있으면(직접 추가한 종목) 행을 눌러 종목 수정을 연다. */
    data class Manage(val onEdit: (() -> Unit)?) : ExerciseRowMode
}

/**
 * 종목 목록 행. 이름(`body`)과 보조 줄("가슴 · 바벨", `caption`, `text2`). 종목 이미지는 넣지 않는다.
 * 두 줄 행의 최소 높이는 56dp다. 높이는 고정하지 않아 글자가 커지면 늘어난다.
 */
@Composable
fun ExerciseRow(
    exercise: Exercise,
    mode: ExerciseRowMode,
    modifier: Modifier = Modifier,
) {
    val colors = NextSetTheme.colors
    val spacing = NextSetTheme.spacing
    val interaction = when (mode) {
        is ExerciseRowMode.Pick -> Modifier.toggleable(
            value = mode.selected,
            role = Role.Checkbox,
            onValueChange = { mode.onToggle() },
        )
        is ExerciseRowMode.Manage -> mode.onEdit?.let { onEdit ->
            Modifier.clickable(onClickLabel = stringResource(R.string.exercise_edit_action), onClick = onEdit)
        } ?: Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NextSetTheme.sizes.listRowMinHeight)
            .then(interaction)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(text = exercise.nameKo, style = NextSetTheme.typography.bodyStrong, color = colors.text)
            Text(text = exercise.detailText(), style = NextSetTheme.typography.caption, color = colors.text2)
        }
        if (mode is ExerciseRowMode.Pick) {
            // 누르는 것은 행 전체다. 체크 상자는 모양만 그린다.
            Box(
                modifier = Modifier.size(32.dp).then(
                    if (mode.selected) Modifier.background(colors.go, CircleShape)
                    else Modifier.border(2.dp, colors.text3, CircleShape),
                ),
                contentAlignment = Alignment.Center,
            ) {
                if (mode.selected) Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.goMark, modifier = Modifier.size(24.dp))
            }
        }
    }
}
