package io.github.zzunsik.nextset.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// 화면 키. 프로세스가 죽었다 살아나도 뒤로 가기 스택을 복원하도록 직렬화한다.

/** 홈(docs/DESIGN.md "홈"). 뒤로 가기 스택의 맨 아래에 늘 있다. 하단 내비게이션의 "홈" 탭이다. */
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

/**
 * 기록 목록(docs/DESIGN.md "기록 목록", HIST-01). 하단 내비게이션의 "기록" 탭이다. 화면은 `historyEntries`가 잇는다.
 */
@Serializable
data object HistoryKey : NavKey

/** 설정(docs/DESIGN.md "설정"). 하단 내비게이션의 "설정" 탭이다. */
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

/** 운동을 끝낸 뒤의 운동 요약(docs/DESIGN.md "운동 요약", LOG-15). "확인"과 뒤로 가기는 홈으로 간다. */
@Serializable
data class WorkoutSummaryKey(val workoutId: Long) : NavKey

/**
 * 기록 상세·수정(docs/DESIGN.md "기록 상세·수정"). [editing]이면 수정 상태로 연다.
 * [isNew]면 "지난 운동 적기"로 막 만든 기록이다. 완료한 세트 없이 나가면 기록을 지운다.
 */
@Serializable
data class HistoryDetailKey(val workoutId: Long, val editing: Boolean = false, val isNew: Boolean = false) : NavKey

/**
 * 루틴 편집(docs/DESIGN.md "루틴 편집"). [routineId]의 루틴은 이미 저장돼 있다.
 * [isNew]면 "새 루틴"으로 막 만든 루틴이다. 제목이 "새 루틴"이고, 이름도 종목도 없이 나가면 버린다.
 */
@Serializable
data class RoutineEditKey(val routineId: Long, val isNew: Boolean = false) : NavKey

/** 루틴 종목 하나의 세트 수, 목표, 휴식 시간을 고치는 종목 설정(docs/DESIGN.md "루틴 편집"의 종목 설정). */
@Serializable
data class RoutineExerciseKey(val routineId: Long, val routineExerciseId: Long) : NavKey

/** 백업 파일 내보내기와 가져오기(DATA-03, DATA-04). */
@Serializable
data object BackupKey : NavKey
