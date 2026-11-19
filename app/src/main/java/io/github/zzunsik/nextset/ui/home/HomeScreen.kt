package io.github.zzunsik.nextset.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import io.github.zzunsik.nextset.ui.theme.ThemeMode

/** 기능이 들어오기 전까지 앱 이름과 안내 문구만 보여 준다. */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val spacing = NextSetTheme.spacing
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
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
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NextSetTheme(themeMode = ThemeMode.Light) {
        HomeScreen()
    }
}
