package io.github.zzunsik.nextset.ui.history

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * 날짜 고르기 창. [max]보다 뒤의 날짜는 고를 수 없다.
 *
 * Material 3 1.4.0의 `DatePicker`는 실험 API라(NF-07) 플랫폼의 날짜 고르기 창을 쓴다. 창의 모양은 앱 테마
 * (`Theme.NextSet`, 밝은 화면과 어두운 화면)를 따른다.
 */
internal fun showDatePicker(context: Context, initial: LocalDate, max: LocalDate, zone: ZoneId, onPicked: (LocalDate) -> Unit) {
    val dialog = DatePickerDialog(
        context,
        { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth,
    )
    dialog.datePicker.maxDate = max.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    dialog.show()
}

/** 시각 고르기 창. `TimePicker`도 실험 API라 플랫폼의 창을 쓴다. 오전·오후로 고른다. */
internal fun showTimePicker(context: Context, initial: LocalTime, onPicked: (LocalTime) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(LocalTime.of(hour, minute)) },
        initial.hour,
        initial.minute,
        false,
    ).show()
}
