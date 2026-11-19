package io.github.zzunsik.nextset.ui.a11y

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import com.google.android.apps.common.testing.accessibility.framework.checks.TextContrastCheck
import com.google.android.apps.common.testing.accessibility.framework.uielement.ViewHierarchyElement
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.fail

/**
 * 화면 테스트의 접근성 검사. 두 가지를 함께 본다.
 *
 * 1. Accessibility Test Framework(ATF) 검사. Roborazzi의 `checkRoboAccessibility`로 돌린다.
 *    빠진 라벨, 글자 색 대비 등을 잡는다. 경고(WARNING)도 실패로 본다.
 *    Compose UI 테스트의 `enableAccessibilityChecks()`는 Robolectric에서 모든 검사가 NOT_RUN이 되어
 *    아무것도 잡지 못한다(#19에서 확인). 그래서 Robolectric을 지원하는 Roborazzi 쪽을 쓴다.
 * 2. 누르는 요소의 배치 크기가 48dp 이상인지(NF-10). [assertTouchTargets]를 본다.
 *
 * 예외는 하나다. 비활성 요소의 글자 대비(TextContrastCheck)만 빼고 본다([disabledTextContrast]).
 * docs/DESIGN.md("비활성 요소는 … 대비 기준의 대상이 아니다")와 WCAG 1.4.3(비활성 UI 요소는 예외)를 따른다.
 * 승인됨(소유자, 2026-11-20, #21). 비활성 요소라도 다른 검사(빠진 라벨 등)와 활성 요소의 대비는 그대로 실패로 본다.
 *
 * 그래픽 모드가 NATIVE여야 대비 검사가 화면을 읽을 수 있다(`@GraphicsMode(NATIVE)`).
 *
 * Roborazzi의 접근성 검사 API는 실험 표시가 붙어 있어 옵트인한다. 테스트 코드에만 쓰고 앱 코드에는 들어가지 않는다.
 */
@OptIn(ExperimentalRoborazziApi::class)
fun ComposeContentTestRule.assertAccessible() {
    onRoot().checkRoboAccessibility(
        roborazziATFAccessibilityCheckOptions = RoborazziATFAccessibilityCheckOptions(
            checker = RoborazziATFAccessibilityChecker(
                preset = AccessibilityCheckPreset.LATEST,
                suppressions = disabledTextContrast,
            ),
            failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Warning,
        ),
    )
    assertTouchTargets()
}

/**
 * 비활성 요소의 글자 대비 결과만 고른다. 검사 종류가 TextContrastCheck이고, 결과가 붙은 요소나 그 조상 가운데
 * 비활성(Compose의 `Disabled` 의미가 옮겨진 `isEnabled == false`)인 것이 있을 때만 뺀다.
 * 비활성 버튼의 글자는 ATF에 버튼(비활성)의 자식 글자 요소(활성)로 나오므로 조상까지 본다.
 */
private val disabledTextContrast = object : TypeSafeMatcher<AccessibilityViewCheckResult>() {
    override fun matchesSafely(item: AccessibilityViewCheckResult): Boolean =
        item.accessibilityHierarchyCheck == TextContrastCheck::class.java &&
            item.element.isInsideDisabled()

    override fun describeTo(description: Description) {
        description.appendText("비활성 요소의 TextContrastCheck 결과")
    }
}

private fun ViewHierarchyElement?.isInsideDisabled(): Boolean =
    generateSequence(this) { it.parentView }.any { !it.isEnabled }

/** 누르는 요소의 최소 크기(docs/DESIGN.md "크기"). */
val MinTouchTarget = 48.dp

private val isActionable = SemanticsMatcher("누를 수 있는 요소") { node ->
    node.config.getOrNull(SemanticsActions.OnClick) != null ||
        node.config.getOrNull(SemanticsActions.OnLongClick) != null
}

/**
 * 누르는 요소마다 배치된 크기([SemanticsNode.size])가 가로세로 48dp 이상인지 본다.
 *
 * 화면에 보이는 영역(boundsInRoot)이 아니라 요소의 배치 크기를 잰다. 보이는 영역은 스크롤 목록의 끝에 걸쳐
 * 잘린 요소나 화면 밖의 요소에서 작게(0까지) 나와, 48dp로 배치한 요소도 실패로 잡는다(#21).
 *
 * ATF의 TouchTargetSizeCheck만으로는 못 잡는다. Compose는 48dp보다 작은 누르는 요소의
 * 접근성 영역(touchBoundsInRoot)을 48dp로 늘려서 알리기 때문이다. 이 늘린 영역은
 * 옆 요소와 겹치면 줄어들 수 있으므로 기대지 않고, 요소 자체를 48dp 이상으로 배치하게 한다
 * (예: `size(48.dp)` 상자 안에 보이는 모양을 둔다). M3의 `minimumInteractiveComponentSize`는 바깥 배치만 키우고
 * 누르는 요소 자체는 보이는 크기로 남겨 이 검사를 통과하지 못한다(#21에서 IconButton, 글자 버튼, 칩으로 확인).
 */
fun ComposeContentTestRule.assertTouchTargets() {
    val minPx = with(density) { MinTouchTarget.toPx() }
    val small = onAllNodes(isActionable, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .filter { it.size.width < minPx - 0.5f || it.size.height < minPx - 0.5f }
    if (small.isNotEmpty()) {
        fail(
            "누르는 영역이 ${MinTouchTarget.value.toInt()}dp보다 작은 요소가 있다(NF-10):\n" +
                small.joinToString("\n") { "  - ${it.describe(density.density)}" },
        )
    }
}

private fun SemanticsNode.describe(density: Float): String {
    val w = size.width / density
    val h = size.height / density
    val role = config.getOrNull(SemanticsProperties.Role)
    val label = config.getOrNull(SemanticsProperties.ContentDescription)
        ?: config.getOrNull(SemanticsProperties.Text)?.map { it.text }
    return "크기=${"%.1f".format(w)}×${"%.1f".format(h)}dp 역할=$role 이름=$label"
}
