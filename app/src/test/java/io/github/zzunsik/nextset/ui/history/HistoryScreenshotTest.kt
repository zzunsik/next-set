package io.github.zzunsik.nextset.ui.history

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.zzunsik.nextset.ui.a11y.assertAccessible
import io.github.zzunsik.nextset.ui.screenshot.LARGE_FONT_SCALE
import io.github.zzunsik.nextset.ui.screenshot.SCREENSHOT_DEVICE
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 기록 목록, 운동 요약, 기록 상세·수정의 스크린샷 비교와 접근성 검사. 값은 지어낸 것이다([HistorySamples]).
 * 요약은 최고 기록이 있는 운동이다(벤치 프레스 두 세트, 풀업 한 세트).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class HistoryScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun captureList(state: HistoryUiState = HistorySamples.list()) {
        compose.setContent { NextSetTheme { HistoryListScreen(state = state, actions = HistoryListActions(onBack = {})) } }
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
    }

    private fun captureSummary(scrollTo: String? = null) {
        compose.setContent {
            NextSetTheme { WorkoutSummaryScreen(state = HistorySamples.report(), noteText = "", onNoteChange = {}, onConfirm = {}) }
        }
        scrollTo?.let { compose.onNodeWithTag(REPORT_LIST_TAG).performScrollToNode(hasText(it)) }
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
    }

    private fun captureDetail(state: ReportUiState) {
        compose.setContent {
            NextSetTheme {
                HistoryDetailScreen(
                    state = state,
                    noteText = state.report?.note.orEmpty(),
                    today = HistorySamples.today,
                    zone = HistorySamples.zone,
                    actions = HistoryDetailActions(),
                )
            }
        }
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
    }

    // ---- 기록 목록 ----

    @Test
    fun list() = captureList()

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun listLargeFont() = captureList()

    @Test
    @Config(qualifiers = "+night")
    fun listDark() = captureList()

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun listDarkLargeFont() = captureList()

    /** 기록 없음. */
    @Test
    fun listEmpty() = captureList(HistoryUiState(loading = false, today = HistorySamples.today))

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun listEmptyDarkLargeFont() = captureList(HistoryUiState(loading = false, today = HistorySamples.today))

    // ---- 운동 요약 ----

    @Test
    fun summary() = captureSummary()

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun summaryLargeFont() = captureSummary()

    @Test
    @Config(qualifiers = "+night")
    fun summaryDark() = captureSummary()

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun summaryDarkLargeFont() = captureSummary()

    /** 아래로 내려 종목별 세트 기록과 세트 행의 최고 기록 배지, 메모 칸을 본다. */
    @Test
    fun summaryScrolled() = captureSummary(scrollTo = "운동 메모")

    // ---- 기록 상세·수정 ----

    @Test
    fun detail() = captureDetail(HistorySamples.report())

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun detailDarkLargeFont() = captureDetail(HistorySamples.report())

    /** 수정 상태. 날짜, 시작 시각, 운동 시간 칸이 생기고 세트 칸을 숫자판으로 고친다. */
    @Test
    fun detailEditing() = captureDetail(HistorySamples.report(editing = true))

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun detailEditingLargeFont() = captureDetail(HistorySamples.report(editing = true))

    @Test
    @Config(qualifiers = "+night")
    fun detailEditingDark() = captureDetail(HistorySamples.report(editing = true))

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun detailEditingDarkLargeFont() = captureDetail(HistorySamples.report(editing = true))
}
