package io.github.zzunsik.nextset.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/** 하단 내비게이션의 탭(docs/DESIGN.md "이동 구조"). 홈, 기록, 설정 셋이다. */
enum class TopLevelTab(val key: NavKey, val label: Int, val icon: Int) {
    Home(HomeKey, R.string.nav_home, R.drawable.ic_home),
    History(HistoryKey, R.string.nav_history, R.drawable.ic_history),
    Settings(SettingsKey, R.string.nav_settings, R.drawable.ic_settings),
}

/** [key]가 탭 화면이면 그 탭. 운동 중과 하위 화면이면 null이고 하단 내비게이션을 숨긴다. */
fun topLevelTabOf(key: NavKey?): TopLevelTab? = TopLevelTab.entries.firstOrNull { it.key == key }

/**
 * [tab]으로 간다. 홈은 늘 스택의 맨 아래에 있고, 다른 탭은 홈 바로 위에 하나만 둔다.
 * 그래서 기록과 설정에서 뒤로 가면 홈으로 오고, 홈에서 뒤로 가면 앱을 나간다.
 */
fun MutableList<NavKey>.selectTab(tab: TopLevelTab) {
    while (size > 1) removeAt(lastIndex)
    if (firstOrNull() != HomeKey) {
        clear()
        add(HomeKey)
    }
    if (tab != TopLevelTab.Home) add(tab.key)
}

/**
 * 탭 화면 아래에 하단 내비게이션을 붙인다. [current]가 탭 화면이 아니면 숨긴다.
 * [content]에 넘기는 Modifier는 바의 높이만큼 아래를 비우고, 바가 시스템 내비게이션 바 자리를 이미 맡았다고 알린다
 * (화면 안의 Scaffold가 같은 여백을 두 번 두지 않는다).
 */
@Composable
fun TopLevelScaffold(
    current: NavKey?,
    onSelect: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val selected = topLevelTabOf(current)
    Scaffold(
        modifier = modifier,
        containerColor = NextSetTheme.colors.bg,
        contentWindowInsets = WindowInsets(0),
        bottomBar = { if (selected != null) NextSetBottomBar(selected = selected, onSelect = onSelect) },
    ) { padding: PaddingValues ->
        content(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        )
    }
}

/**
 * 하단 내비게이션. `bg` 바탕, 고른 탭은 `text`와 위쪽 `go` 표시, 고르지 않은 탭은 `text3`다.
 * 강조색은 누르는 요소에만 쓰므로 현재 위치 표시에 쓰지 않는다(docs/DESIGN.md "쓰임새 규칙").
 */
@Composable
fun NextSetBottomBar(selected: TopLevelTab, onSelect: (TopLevelTab) -> Unit, modifier: Modifier = Modifier) {
    val colors = NextSetTheme.colors
    Column(modifier) {
        HorizontalDivider(color = colors.line)
        NavigationBar(containerColor = colors.bg) {
            TopLevelTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = tab == selected,
                    onClick = { if (tab != selected) onSelect(tab) },
                    icon = {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Box(Modifier.width(36.dp).height(3.dp).background(if (tab == selected) colors.go else Color.Transparent))
                            Icon(painter = painterResource(tab.icon), contentDescription = null)
                        }
                    },
                    label = { Text(text = stringResource(tab.label), style = NextSetTheme.typography.caption) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.text,
                        selectedTextColor = colors.text,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = colors.text3,
                        unselectedTextColor = colors.text3,
                    ),
                )
            }
        }
    }
}
