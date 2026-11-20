package io.github.zzunsik.nextset.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/**
 * 화면 맨 위의 제목 줄. [onBack]이 있으면 앞에 뒤로 가기 버튼(48dp)을 둔다.
 * M3 `TopAppBar`는 이 저장소의 Material 3 판(1.4.0)에서 실험 API라 쓰지 않는다(docs/DESIGN.md "Material 3 요소를 고르는 규칙").
 */
@Composable
internal fun ScreenTitleBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val spacing = NextSetTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NextSetTheme.sizes.listRowMinHeight)
            .padding(start = if (onBack != null) spacing.xs else spacing.lg, end = spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            // M3 IconButton의 배치 크기는 40dp라 누르는 영역(48dp)을 직접 잡는다(NF-10).
            IconButton(onClick = onBack, modifier = Modifier.size(NextSetTheme.sizes.minTouchTarget)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.navigate_back),
                    tint = NextSetTheme.colors.text,
                )
            }
            Spacer(Modifier.width(spacing.xs))
        }
        Text(
            text = title,
            style = NextSetTheme.typography.title,
            color = NextSetTheme.colors.text,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/**
 * 설정과 라이선스 목록의 한 줄. 제목(`body`)과 보조 줄(`caption`, `text2`), 최소 높이 56dp.
 * [onClick]이 없으면 누르는 요소가 아니다(아직 만들지 않은 화면으로 가는 행).
 */
@Composable
internal fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val spacing = NextSetTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = NextSetTheme.sizes.listRowMinHeight)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.CenterVertically),
    ) {
        Text(text = title, style = NextSetTheme.typography.body, color = NextSetTheme.colors.text)
        if (caption != null) {
            Text(text = caption, style = NextSetTheme.typography.caption, color = NextSetTheme.colors.text2)
        }
    }
}

/** 행 사이의 선(`line`). 장식이다. */
@Composable
internal fun RowDivider() {
    HorizontalDivider(color = NextSetTheme.colors.line)
}

/** 대화상자 안에서 하나를 고르는 목록. 줄마다 라디오 버튼과 이름, 줄 전체를 누른다. */
@Composable
internal fun <T> OptionList(
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = NextSetTheme.spacing
    Column(modifier = modifier.selectableGroup()) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = NextSetTheme.sizes.minTouchTarget)
                    .selectable(selected = option == selected, role = Role.RadioButton, onClick = { onSelect(option) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                RadioButton(selected = option == selected, onClick = null)
                Text(text = label(option), style = NextSetTheme.typography.body, color = NextSetTheme.colors.text)
            }
        }
    }
}

/**
 * 하나를 고르는 대화상자. 고르면 바로 적용하고 닫는다. 버튼은 "닫기" 하나다.
 * [note]가 있으면 목록 위에 보조 문구를 둔다. 목록이 길면 안쪽이 스크롤된다.
 */
@Composable
internal fun <T> OptionDialog(
    title: String,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    note: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = NextSetTheme.sizes.minTouchTarget)) {
                Text(stringResource(R.string.action_close), style = NextSetTheme.typography.label)
            }
        },
        title = { Text(title, style = NextSetTheme.typography.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(NextSetTheme.spacing.sm),
            ) {
                if (note != null) {
                    Text(note, style = NextSetTheme.typography.body, color = NextSetTheme.colors.text2)
                }
                OptionList(
                    options = options,
                    selected = selected,
                    label = label,
                    onSelect = {
                        onSelect(it)
                        onDismiss()
                    },
                )
            }
        },
    )
}

/**
 * 설정의 "알림" 행(docs/DESIGN.md "설정" 4번, REST-09). 알림이 꺼져 있을 때만 보인다.
 * "알림 꺼짐" 표시와 보조 문구, 오른쪽 끝의 "알림 켜기" 글자 버튼이다.
 */
@Composable
internal fun NotificationsOffRow(onEnable: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = NextSetTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NextSetTheme.sizes.listRowMinHeight)
            .padding(start = spacing.lg, end = spacing.xs, top = spacing.sm, bottom = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = stringResource(R.string.settings_notifications_off),
                style = NextSetTheme.typography.body,
                color = NextSetTheme.colors.text,
            )
            Text(
                text = stringResource(R.string.settings_notifications_caption),
                style = NextSetTheme.typography.caption,
                color = NextSetTheme.colors.text2,
            )
        }
        TextButton(
            onClick = onEnable,
            modifier = Modifier.heightIn(min = NextSetTheme.sizes.minTouchTarget),
            shape = NextSetTheme.radius.md,
        ) {
            Text(
                text = stringResource(R.string.notifications_enable),
                style = NextSetTheme.typography.label,
                color = NextSetTheme.colors.accent,
            )
        }
    }
}
