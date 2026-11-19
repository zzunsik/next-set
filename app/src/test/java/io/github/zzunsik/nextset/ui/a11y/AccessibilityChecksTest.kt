package io.github.zzunsik.nextset.ui.a11y

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * 접근성 검사 자체가 동작하는지 본다. 잘못 만든 예시마다 [assertAccessible]이 실패해야 한다.
 * 검사가 조용히 아무것도 안 하게 되면(예: Robolectric에서 ATF 검사가 NOT_RUN) 이 테스트가 잡는다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccessibilityChecksTest {
    @get:Rule
    val compose = createComposeRule()

    private fun assertFailsWith(expected: String, content: @Composable () -> Unit) {
        compose.setContent {
            Box(Modifier.size(200.dp).background(Color.White), contentAlignment = Alignment.Center) {
                content()
            }
        }
        val error = runCatching { compose.assertAccessible() }.exceptionOrNull()
        assertTrue("접근성 검사가 실패해야 한다", error != null)
        assertTrue("실패 내용에 \"$expected\"가 있어야 한다: ${error?.message}", error!!.message!!.contains(expected))
    }

    @Test
    fun passesWellFormedTarget() {
        compose.setContent {
            Box(Modifier.size(200.dp).background(Color.White), contentAlignment = Alignment.Center) {
                Box(Modifier.size(48.dp).semantics { contentDescription = "닫기" }.clickable {})
            }
        }
        compose.assertAccessible()
    }

    @Test
    fun failsOnTouchTargetSmallerThan48dp() = assertFailsWith("48dp보다 작은") {
        Box(Modifier.size(40.dp).semantics { contentDescription = "닫기" }.clickable {})
    }

    /** 스크롤 목록에서 보이는 영역이 잘린 요소도 배치 크기로 잰다. 작게 배치한 요소는 화면 밖에 있어도 잡는다. */
    @Test
    fun measuresLaidOutSizeInsideScrolledContainers() {
        compose.setContent {
            Box(Modifier.size(200.dp).background(Color.White)) {
                Column(Modifier.size(200.dp, 100.dp).verticalScroll(rememberScrollState())) {
                    Box(Modifier.size(200.dp, 80.dp))
                    // 위 20dp만 보이고 나머지는 잘린다.
                    Box(Modifier.size(48.dp).semantics { contentDescription = "걸친 요소" }.clickable {})
                    Box(Modifier.size(48.dp).semantics { contentDescription = "화면 밖 요소" }.clickable {})
                }
            }
        }
        compose.assertTouchTargets()
    }

    @Test
    fun failsOnSmallTargetOutsideTheVisibleArea() = assertFailsWith("48dp보다 작은") {
        Column(Modifier.size(200.dp, 100.dp).verticalScroll(rememberScrollState())) {
            Box(Modifier.size(200.dp, 150.dp))
            Box(Modifier.size(40.dp).semantics { contentDescription = "닫기" }.clickable {})
        }
    }

    @Test
    fun failsOnMissingLabel() = assertFailsWith("SpeakableTextPresentCheck") {
        Box(Modifier.size(48.dp).clickable {})
    }

    @Test
    fun failsOnLowTextContrast() = assertFailsWith("TextContrastCheck") {
        Text("흐린 글자", color = Color(0xFFBBBBBB), modifier = Modifier.padding(8.dp))
    }
}
