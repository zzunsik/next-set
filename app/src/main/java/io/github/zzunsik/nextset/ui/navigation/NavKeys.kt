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

/**
 * 루틴 목록. 홈(#23)이 생기기 전까지 쓰는 임시 화면이다. 홈의 "순서 편집" 상태와 같은 일(RTN-05)을 하고
 * "새 루틴"과 행을 눌러 루틴 편집을 연다. 홈이 이 일을 맡으면 지운다.
 */
@Serializable
data object RoutineListKey : NavKey

/**
 * 루틴 편집(docs/DESIGN.md "루틴 편집"). [routineId]의 루틴은 이미 저장돼 있다.
 * [isNew]면 "새 루틴"으로 막 만든 루틴이다. 제목이 "새 루틴"이고, 이름도 종목도 없이 나가면 버린다.
 */
@Serializable
data class RoutineEditKey(val routineId: Long, val isNew: Boolean = false) : NavKey

/** 루틴 종목 하나의 세트 수, 목표, 휴식 시간을 고치는 종목 설정(docs/DESIGN.md "루틴 편집"의 종목 설정). */
@Serializable
data class RoutineExerciseKey(val routineId: Long, val routineExerciseId: Long) : NavKey
