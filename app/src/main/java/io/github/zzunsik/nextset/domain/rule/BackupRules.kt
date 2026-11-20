package io.github.zzunsik.nextset.domain.rule

import java.time.LocalDateTime
import java.util.Locale

/**
 * 백업 파일의 이름. 내보낸 날짜와 시각을 붙인다. 예: `next-set-backup-20261118-093000.json`.
 * 기기 언어와 관계없이 아라비아 숫자로 적는다.
 */
fun backupFileName(exportedAt: LocalDateTime): String = String.format(
    Locale.ROOT,
    "next-set-backup-%04d%02d%02d-%02d%02d%02d.json",
    exportedAt.year,
    exportedAt.monthValue,
    exportedAt.dayOfMonth,
    exportedAt.hour,
    exportedAt.minute,
    exportedAt.second,
)
