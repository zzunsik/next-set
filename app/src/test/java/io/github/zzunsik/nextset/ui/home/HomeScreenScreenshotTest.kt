package io.github.zzunsik.nextset.ui.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.zzunsik.nextset.ui.a11y.assertAccessible
import io.github.zzunsik.nextset.ui.navigation.HomeKey
import io.github.zzunsik.nextset.ui.navigation.TopLevelScaffold
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
 * 홈의 스크린샷 비교와 접근성 검사. 앱처럼 하단 내비게이션을 붙여 찍는다.
 * 화면 모드는 시스템 설정(`+night`)을 따르는 기본값으로 둔다. 이름과 값은 지어낸 것이다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class HomeScreenScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun capture(state: HomeUiState = HomeSamples.many, reordering: Boolean = false) {
        compose.setContent {
            NextSetTheme {
                TopLevelScaffold(current = HomeKey, onSelect = {}) { modifier ->
                    HomeScreen(
                        state = state,
                        now = HomeSamples.now,
                        today = HomeSamples.today,
                        actions = HomeActions(),
                        modifier = modifier,
                        initialReordering = reordering,
                    )
                }
            }
        }
        // 접근성 검사를 먼저 돌려 접근성 문제를 스크린샷 차이보다 먼저 알린다.
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
    }

    /**
     * "진행 중인 운동이 있어요." 대화상자는 창이 따로 떠서 `assertAccessible()`(화면 하나를 본다)이 닿지 않는다.
     * 대화상자의 글과 버튼을 대화상자 바탕(`surface`) 위에 그대로 그려 먼저 검사하고, 화면 전체를 찍는다(백업 화면과 같은 방법).
     */
    private fun capturePrompt() {
        // 한 테스트에서 setContent는 한 번만 부를 수 있어 상태로 바꿔 그린다.
        var showDialog by mutableStateOf(false)
        compose.setContent {
            NextSetTheme {
                if (showDialog) {
                    TopLevelScaffold(current = HomeKey, onSelect = {}) { modifier ->
                        HomeScreen(
                            state = HomeSamples.inProgressPrompt,
                            now = HomeSamples.now,
                            today = HomeSamples.today,
                            actions = HomeActions(),
                            modifier = modifier,
                        )
                    }
                } else {
                    Surface(color = NextSetTheme.colors.surface) {
                        Column {
                            InProgressPromptTitle()
                            InProgressPromptText(HomeSamples.inProgressWorkout)
                            InProgressPromptButtons(HomeActions())
                        }
                    }
                }
            }
        }
        compose.assertAccessible()
        showDialog = true
        compose.waitForIdle()
        captureScreenRoboImage()
    }

    // 루틴 여러 개

    @Test
    fun light() = capture()

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun lightLargeFont() = capture()

    @Test
    @Config(qualifiers = "+night")
    fun dark() = capture()

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun darkLargeFont() = capture()

    // 루틴 없음

    @Test
    fun empty() = capture(HomeSamples.empty)

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun emptyDarkLargeFont() = capture(HomeSamples.empty)

    // 루틴 하나

    @Test
    fun oneRoutine() = capture(HomeSamples.one)

    @Test
    @Config(qualifiers = "+night")
    fun oneRoutineDark() = capture(HomeSamples.one)

    // 직접 고른 맨 위 루틴

    @Test
    fun pinned() = capture(HomeSamples.pinned)

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun pinnedDarkLargeFont() = capture(HomeSamples.pinned)

    // 이어서 하기

    @Test
    fun resume() = capture(HomeSamples.resume)

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun resumeLargeFont() = capture(HomeSamples.resume)

    @Test
    @Config(qualifiers = "+night")
    fun resumeDark() = capture(HomeSamples.resume)

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun resumeDarkLargeFont() = capture(HomeSamples.resume)

    // 진행 중인 운동이 있을 때 다른 루틴을 시작하려 하면

    @Test
    fun inProgressPrompt() = capturePrompt()

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun inProgressPromptDarkLargeFont() = capturePrompt()

    // 순서 편집

    @Test
    fun reordering() = capture(reordering = true)

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun reorderingDarkLargeFont() = capture(reordering = true)
}
