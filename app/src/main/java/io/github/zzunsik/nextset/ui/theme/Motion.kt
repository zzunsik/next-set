package io.github.zzunsik.nextset.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * docs/DESIGN.md "모션"의 스프링 값. 안정판에서 `MotionScheme`을 쓸 수 있을 때까지 이 값을 한곳에 모아 쓴다.
 * 값을 바꾸면 문서의 표도 함께 고친다. `MotionTokenSyncTest`가 둘을 견준다.
 */
object NextSetMotion {
    /** 스프링 하나의 값. */
    data class Spring(val dampingRatio: Float, val stiffness: Float) {
        fun <T> spec(visibilityThreshold: T? = null): SpringSpec<T> =
            spring(dampingRatio = dampingRatio, stiffness = stiffness, visibilityThreshold = visibilityThreshold)
    }

    val standardSpatialFast = Spring(0.9f, 1400f)
    val standardSpatialDefault = Spring(0.9f, 700f)
    val standardSpatialSlow = Spring(0.9f, 300f)
    val expressiveSpatialFast = Spring(0.6f, 800f)
    val expressiveSpatialDefault = Spring(0.8f, 380f)
    val expressiveSpatialSlow = Spring(0.8f, 200f)
    val effectsFast = Spring(1.0f, 3800f)
    val effectsDefault = Spring(1.0f, 1600f)
    val effectsSlow = Spring(1.0f, 800f)
}
