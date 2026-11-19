package io.github.zzunsik.nextset.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 화면 키. 프로세스가 죽었다 살아나도 뒤로 가기 스택을 복원하도록 직렬화한다. */
@Serializable
data object HomeKey : NavKey

/** 설정(docs/DESIGN.md "설정"). */
@Serializable
data object SettingsKey : NavKey

/** 오픈소스 라이선스 목록(SET-03). */
@Serializable
data object LicensesKey : NavKey

/** 오픈소스 라이선스 한 건. [id]는 `assets/licenses/index.json`의 항목 id다. */
@Serializable
data class LicenseDetailKey(val id: String) : NavKey
