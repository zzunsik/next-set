package io.github.zzunsik.nextset.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 화면 키. 프로세스가 죽었다 살아나도 뒤로 가기 스택을 복원하도록 직렬화한다. */
@Serializable
data object HomeKey : NavKey

/** 종목 고르기의 두 가지 쓰임(docs/DESIGN.md "종목 고르기"). */
@Serializable
enum class ExercisePickerMode {
    /** 루틴 편집, 운동 중, 기록 수정의 "종목 추가". 고른 종목을 부른 화면에 돌려준다. */
    Pick,

    /** 설정의 "종목 관리". 고르지 않고 직접 추가한 종목을 고치거나 지운다. */
    Manage,
}

/**
 * 종목 고르기. [Pick][ExercisePickerMode.Pick]으로 열면 "n개 추가"를 누를 때 고른 종목의 식별자 목록(고른 순서)을
 * [NavResults]에 [resultName]으로 놓고 닫힌다. 부르는 화면은 같은 이름의 [ResultKey]로 받는다([pickedExercisesKey]).
 */
@Serializable
data class ExercisePickerKey(
    val mode: ExercisePickerMode = ExercisePickerMode.Pick,
    val resultName: String = "",
) : NavKey

/** 고른 종목을 돌려받는 키. 부르는 화면이 [ExercisePickerKey.resultName]에 넣은 이름을 쓴다. */
fun pickedExercisesKey(resultName: String): ResultKey<List<String>> = ResultKey(resultName)

/** 종목 추가·수정. [exerciseId]가 없으면 새 종목이고 [initialName]을 이름 칸에 채운다. */
@Serializable
data class ExerciseFormKey(
    val exerciseId: String? = null,
    val initialName: String = "",
) : NavKey

/** 설정(docs/DESIGN.md "설정"). */
@Serializable
data object SettingsKey : NavKey

/** 오픈소스 라이선스 목록(SET-03). */
@Serializable
data object LicensesKey : NavKey

/** 오픈소스 라이선스 한 건. [id]는 `assets/licenses/index.json`의 항목 id다. */
@Serializable
data class LicenseDetailKey(val id: String) : NavKey

/** 운동 중(docs/DESIGN.md "운동 중"). 운동은 저장돼 있어 프로세스가 죽었다 살아나도 같은 화면으로 돌아온다(LOG-13). */
@Serializable
data class WorkoutKey(val workoutId: Long) : NavKey

/** 운동을 끝낸 뒤의 완료 화면. 운동 요약(#27)이 생기면 그 화면으로 바뀐다. */
@Serializable
data class WorkoutDoneKey(val workoutId: Long) : NavKey
