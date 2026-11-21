package io.github.zzunsik.nextset.ui.screenshot

/**
 * 스크린샷 테스트가 쓰는 기기 조건. 폭 360dp의 흔한 휴대폰 크기, xhdpi.
 * 메서드마다 `+night`(어두운 화면)과 `fontScale = LARGE_FONT_SCALE`(글자 크게)를 더해 네 조합을 찍는다.
 */
const val SCREENSHOT_DEVICE = "w360dp-h640dp-xhdpi"

/** 글자 크기 "크게"(NF-10). Android 14부터 고를 수 있는 가장 큰 값인 200%다. */
const val LARGE_FONT_SCALE = 2.0f
