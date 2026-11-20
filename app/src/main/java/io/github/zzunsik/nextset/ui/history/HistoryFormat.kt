package io.github.zzunsik.nextset.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.zzunsik.nextset.R
import io.github.zzunsik.nextset.domain.model.RecordType
import io.github.zzunsik.nextset.domain.model.SetValue
import io.github.zzunsik.nextset.ui.workout.durationText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 볼륨의 숫자. 천 단위 쉼표를 찍고 kg 아래 자리는 있을 때만 적는다(docs/DESIGN.md "숫자와 단위": 4,250kg).
 * `4250000`은 "4,250", `187500`은 "187.5"다.
 */
fun volumeNumberText(grams: Long): String {
    val abs = Math.abs(grams)
    val whole = String.format(Locale.ROOT, "%,d", abs / GRAMS_PER_KG)
    val fraction = (abs % GRAMS_PER_KG).toString().padStart(3, '0').trimEnd('0')
    val sign = if (grams < 0) "-" else ""
    return if (fraction.isEmpty()) "$sign$whole" else "$sign$whole.$fraction"
}

@Composable
fun volumeText(grams: Long): String = stringResource(R.string.report_volume_value, volumeNumberText(grams))

/** 같은 루틴의 지난 기록과 견준 볼륨. "지난번보다 +120kg", "지난번보다 −50kg", 같으면 "지난번과 볼륨이 같아요." */
@Composable
fun volumeDeltaText(deltaGrams: Long): String = when {
    deltaGrams == 0L -> stringResource(R.string.report_volume_same)
    deltaGrams > 0 -> stringResource(R.string.report_volume_delta, "+" + volumeText(deltaGrams))
    else -> stringResource(R.string.report_volume_delta, MINUS + volumeText(-deltaGrams))
}

/** 단위까지 적은 세트 값(docs/DESIGN.md "숫자와 단위"). "60kg × 10회", "12회", "1:00". */
@Composable
fun fullSetText(type: RecordType, value: SetValue): String = when (type) {
    RecordType.WEIGHT_REPS -> stringResource(
        R.string.set_value_weight_reps_full,
        value.weight?.toKgText().orEmpty(),
        value.reps?.toString().orEmpty(),
    )
    RecordType.REPS -> stringResource(R.string.set_value_reps, value.reps?.toString().orEmpty())
    RecordType.DURATION -> value.durationSeconds?.let(::durationText).orEmpty()
}

/** 시각 "오후 6:30". */
fun clockText(instant: Instant, zone: ZoneId): String = CLOCK_FORMAT.format(instant.atZone(zone))

/**
 * 이름 뒤에 목적격 조사를 붙인다. 마지막 글자에 받침이 있으면 "을", 없으면 "를"이다("가슴 루틴을", "하체 데이를").
 * 숫자는 읽는 소리로 가린다. 한글과 숫자가 아닌 글자로 끝나면 "을(를)"을 붙인다.
 */
fun withObjectParticle(name: String): String {
    val last = name.trimEnd().lastOrNull() ?: return name
    val hasFinal = when (last) {
        in '가'..'힣' -> (last - '가') % FINAL_CONSONANTS != 0
        in '0'..'9' -> last in DIGITS_WITH_FINAL
        else -> null
    }
    return name.trimEnd() + when (hasFinal) {
        true -> "을"
        false -> "를"
        null -> "을(를)"
    }
}

private const val GRAMS_PER_KG = 1000L

/** 숫자판의 증감 키와 같은 빼기 표(U+2212). */
private const val MINUS = "−"

/** 한글 음절 하나에 들어가는 받침의 가짓수(받침 없음 포함). */
private const val FINAL_CONSONANTS = 28

/** 영, 일, 삼, 육, 칠, 팔은 받침이 있다. */
private const val DIGITS_WITH_FINAL = "013678"

private val CLOCK_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
