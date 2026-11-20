package io.github.zzunsik.nextset.ui.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.zzunsik.nextset.domain.model.OpenSourceNotice
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 라이선스 전문 화면(SET-03, #57). 값은 지어낸 것이다. */
@RunWith(RobolectricTestRunner::class)
class LicenseDetailScreenTest {
    @get:Rule
    val compose = createComposeRule()

    /** 원문이 70~80자에서 바꾼 줄은 화면에서 문단 하나로 이어지고, 빈 줄과 목록 줄은 남는다. */
    @Test
    fun joinsLineBreaksWithinAParagraph() {
        val text = "PREAMBLE\nThe goals of this made-up license are to\nkeep the text readable.\n\n1) First made-up\ncondition.\n2) Second condition."
        compose.setContent {
            NextSetTheme {
                LicenseDetailScreen(
                    state = LicenseDetailUiState(
                        notice = OpenSourceNotice(id = "lib", name = "Test Library", author = "Test Org", licenseName = "Test License"),
                        text = text,
                    ),
                    onBack = {},
                )
            }
        }

        val shown = compose.onNodeWithText("PREAMBLE", substring = true).fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString("") { it.text }
        assertEquals(
            "PREAMBLE\nThe goals of this made-up license are to keep the text readable.\n\n1) First made-up condition.\n2) Second condition.",
            shown,
        )
    }
}
