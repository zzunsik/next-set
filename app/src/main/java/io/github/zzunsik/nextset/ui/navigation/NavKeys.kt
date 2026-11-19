package io.github.zzunsik.nextset.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 화면 키. 프로세스가 죽었다 살아나도 뒤로 가기 스택을 복원하도록 직렬화한다. */
@Serializable
data object HomeKey : NavKey
