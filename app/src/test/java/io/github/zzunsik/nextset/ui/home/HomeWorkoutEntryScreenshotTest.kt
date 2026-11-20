package io.github.zzunsik.nextset.ui.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.zzunsik.nextset.domain.model.Routine
import io.github.zzunsik.nextset.ui.a11y.assertAccessible
import io.github.zzunsik.nextset.ui.screenshot.LARGE_FONT_SCALE
import io.github.zzunsik.nextset.ui.screenshot.SCREENSHOT_DEVICE
import io.github.zzunsik.nextset.ui.theme.NextSetTheme
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 홈의 임시 운동 입구(#24). 진짜 홈은 #23이 만든다. 이름과 값은 지어낸 것이다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = SCREENSHOT_DEVICE)
class HomeWorkoutEntryScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val startedAt = Instant.parse("2026-03-14T09:00:00Z")

    private fun capture(state: HomeWorkoutUiState) {
        compose.setContent {
            NextSetTheme {
                HomeScreen(
                    onOpenSettings = {},
                    workoutEntry = {
                        HomeWorkoutEntry(
                            state = state,
                            now = startedAt.plusSeconds(32 * 60),
                            onResume = {},
                            onStartRoutine = {},
                            onStartEmpty = {},
                        )
                    },
                )
            }
        }
        compose.assertAccessible()
        compose.onRoot().captureRoboImage()
    }

    private val inProgress = HomeWorkoutUiState(
        loading = false,
        inProgress = InProgressWorkoutUi(workoutId = 1, routineName = "가슴 루틴", totalSets = 12, completedSets = 5, startedAt = startedAt),
    )

    private val routines = HomeWorkoutUiState(
        loading = false,
        routines = listOf(Routine(1, "가슴 루틴", 0), Routine(2, "하체 루틴", 1)),
    )

    @Test
    fun resume() = capture(inProgress)

    @Test
    @Config(fontScale = LARGE_FONT_SCALE)
    fun resumeLargeFont() = capture(inProgress)

    @Test
    @Config(qualifiers = "+night")
    fun resumeDark() = capture(inProgress)

    @Test
    @Config(qualifiers = "+night", fontScale = LARGE_FONT_SCALE)
    fun resumeDarkLargeFont() = capture(inProgress)

    @Test
    fun start() = capture(routines)

    @Test
    @Config(qualifiers = "+night")
    fun startDark() = capture(routines)
}
