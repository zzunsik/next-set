package io.github.zzunsik.nextset.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import io.github.zzunsik.nextset.ui.theme.ThemeMode

/**
 * 기능이 들어오기 전까지 앱 이름과 안내 문구만 보여 준다.
 * 설정으로 가는 글자 버튼은 홈(#23)이 하단 내비게이션을 만들 때까지 두는 임시 입구다.
 * [workoutEntry]는 운동을 시작하고 이어서 하는 임시 입구(#24, [HomeWorkoutEntry]) 자리다. 홈(#23)이 카드와 행으로 바꾼다.
 */
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    workoutEntry: @Composable () -> Unit = {},
) {
    val spacing = NextSetTheme.spacing
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg, vertical = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = NextSetTheme.typography.headline,
                color = NextSetTheme.colors.text,
            )
            Text(
                text = stringResource(R.string.home_placeholder),
                style = NextSetTheme.typography.body,
                color = NextSetTheme.colors.text2,
            )
            workoutEntry()
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.heightIn(min = NextSetTheme.sizes.minTouchTarget),
            ) {
                Text(text = stringResource(R.string.home_open_settings), style = NextSetTheme.typography.label)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NextSetTheme(themeMode = ThemeMode.Light) {
        HomeScreen(onOpenSettings = {})
    }
}
