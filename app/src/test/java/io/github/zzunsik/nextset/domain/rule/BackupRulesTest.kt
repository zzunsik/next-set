package io.github.zzunsik.nextset.domain.rule

import java.time.LocalDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupRulesTest {
    @Test
    fun fileNameHasDateAndTime() {
        assertEquals("next-set-backup-20261118-093000.json", backupFileName(LocalDateTime.of(2026, 11, 18, 9, 30, 0)))
        assertEquals("next-set-backup-20270102-230509.json", backupFileName(LocalDateTime.of(2027, 1, 2, 23, 5, 9)))
    }

    @Test
    fun fileNameUsesAsciiDigitsInAnyLocale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("next-set-backup-20261118-093000.json", backupFileName(LocalDateTime.of(2026, 11, 18, 9, 30, 0)))
        } finally {
            Locale.setDefault(saved)
        }
    }
}
