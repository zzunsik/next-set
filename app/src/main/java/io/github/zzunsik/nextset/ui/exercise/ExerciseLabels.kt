package io.github.zzunsik.nextset.ui.exercise

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.BodyPart
import io.github.zzunsik.nextset.domain.model.Equipment
import io.github.zzunsik.nextset.domain.model.Exercise
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SubPart

/** 부위, 장비, 기록 방식의 화면 이름. 문구는 문자열 리소스에 있다(NF-03). */
@get:StringRes
val BodyPart.label: Int
    get() = when (this) {
        BodyPart.CHEST -> R.string.body_part_chest
        BodyPart.BACK -> R.string.body_part_back
        BodyPart.SHOULDERS -> R.string.body_part_shoulders
        BodyPart.ARMS -> R.string.body_part_arms
        BodyPart.LEGS -> R.string.body_part_legs
        BodyPart.CORE -> R.string.body_part_core
    }

@get:StringRes
val SubPart.label: Int
    get() = when (this) {
        SubPart.QUADS -> R.string.sub_part_quads
        SubPart.HAMSTRINGS -> R.string.sub_part_hamstrings
        SubPart.GLUTES -> R.string.sub_part_glutes
        SubPart.CALVES -> R.string.sub_part_calves
        SubPart.BICEPS -> R.string.sub_part_biceps
        SubPart.TRICEPS -> R.string.sub_part_triceps
        SubPart.FOREARMS -> R.string.sub_part_forearms
    }

@get:StringRes
val Equipment.label: Int
    get() = when (this) {
        Equipment.BARBELL -> R.string.equipment_barbell
        Equipment.DUMBBELL -> R.string.equipment_dumbbell
        Equipment.MACHINE -> R.string.equipment_machine
        Equipment.CABLE -> R.string.equipment_cable
        Equipment.BODYWEIGHT -> R.string.equipment_bodyweight
        Equipment.KETTLEBELL -> R.string.equipment_kettlebell
        Equipment.BAND -> R.string.equipment_band
        Equipment.OTHER -> R.string.equipment_other
    }

@get:StringRes
val RecordType.label: Int
    get() = when (this) {
        RecordType.WEIGHT_REPS -> R.string.record_type_weight_reps
        RecordType.REPS -> R.string.record_type_reps
        RecordType.DURATION -> R.string.record_type_duration
    }

/** 이 부위의 세부 부위. 하체와 팔에만 있다. */
val BodyPart.subParts: List<SubPart>
    get() = SubPart.entries.filter { it.bodyPart == this }

/**
 * 종목 목록 행의 보조 줄(docs/DESIGN.md `ExerciseRow`). "가슴 · 바벨", 세부 부위가 있으면 부위 자리에 세부 부위를 적는다.
 * 직접 추가한 종목은 끝에 "직접 추가"를 붙인다.
 */
@Composable
fun Exercise.detailText(): String {
    val part = stringResource(subPart?.label ?: bodyPart.label)
    val equipment = stringResource(equipment.label)
    return if (isBuiltin) {
        stringResource(R.string.exercise_row_detail, part, equipment)
    } else {
        stringResource(R.string.exercise_row_detail_custom, part, equipment)
    }
}
