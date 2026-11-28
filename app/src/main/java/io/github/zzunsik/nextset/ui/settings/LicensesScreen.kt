package io.github.zzunsik.nextset.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.ui.component.GroupedCard
import io.github.zzunsik.nextset.domain.model.OpenSourceNotice
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

/**
 * 오픈소스 라이선스 목록(SET-03, docs/DESIGN.md "설정"). 글꼴, 아이콘, 라이브러리를 한 줄씩 보이고
 * 줄을 누르면 [onOpen]으로 저작권 표시와 라이선스 전문을 연다. [notices]가 null이면 읽는 중이다.
 */
@Composable
fun LicensesScreen(
    notices: List<OpenSourceNotice>?,
    onOpen: (OpenSourceNotice) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                ScreenTitleBar(title = stringResource(R.string.licenses_title), onBack = onBack)
            }
            item {
              GroupedCard(Modifier.padding(horizontal = NextSetTheme.spacing.lg)) {
               notices.orEmpty().forEachIndexed { index, notice ->
                if (index > 0) RowDivider()
                SettingsRow(
                    title = notice.name,
                    caption = stringResource(R.string.license_row_caption, notice.author, notice.licenseName),
                    onClick = { onOpen(notice) },
                )
               }
              }
            }
        }
    }
}

/**
 * 라이선스 한 건. 제목은 이름, 그 아래 만든 곳과 라이선스 이름, 그다음 전문이다. [state]가 null이면 읽는 중이다.
 * 전문은 [reflowLicenseText]로 문단 안의 줄바꿈을 합쳐 그린다.
 */
@Composable
fun LicenseDetailScreen(
    state: LicenseDetailUiState?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = NextSetTheme.spacing
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenTitleBar(title = state?.notice?.name.orEmpty(), onBack = onBack)
            if (state != null) {
                Column(
                    modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, bottom = spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    Text(
                        text = stringResource(R.string.license_row_caption, state.notice.author, state.notice.licenseName),
                        style = NextSetTheme.typography.body,
                        color = NextSetTheme.colors.text2,
                    )
                    // 원문의 70~80자 줄바꿈을 그대로 그리면 화면 폭에서 짧은 줄이 생긴다. 문단 안의 줄만 합친다(#57).
                    val text = remember(state.text) { reflowLicenseText(state.text) }
                    Text(text = text, style = NextSetTheme.typography.caption, color = NextSetTheme.colors.text)
                }
            }
        }
    }
}
