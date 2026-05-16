package com.aesthetic.tracker.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class JsonMeasurementImporterTest {
    @Test
    fun `parses supported measurement fields`() {
        val result = JsonMeasurementImporter.parse(
            """{"weightKg":70.1,"bodyFatPercent":17.8,"skeletalMuscleKg":29.4,"pulse":86,"waterPercent":55.2}""",
            LocalDate.of(2026, 5, 16),
        )

        val measurement = (result as ImportParseResult.Success).measurement
        assertEquals(70.1, measurement.weightKg, 0.01)
        assertEquals(17.8, measurement.bodyFatPercent, 0.01)
        assertEquals(86, measurement.pulse)
        assertEquals(LocalDate.of(2026, 5, 16), measurement.date)
    }

    @Test
    fun `empty import returns user facing error`() {
        val result = JsonMeasurementImporter.parse("")

        assertTrue(result is ImportParseResult.Error)
        assertEquals("Вставьте JSON с данными замера перед импортом.", (result as ImportParseResult.Error).message)
    }

    @Test
    fun `unsupported json fields return user facing error`() {
        val result = JsonMeasurementImporter.parse("""{"foo":1}""")

        assertTrue(result is ImportParseResult.Error)
        assertEquals("В JSON не найдены поддерживаемые поля замера.", (result as ImportParseResult.Error).message)
    }

    @Test
    fun `invalid json returns sanitized parse error`() {
        val result = JsonMeasurementImporter.parse("""{"weightKg":}""")

        assertTrue(result is ImportParseResult.Error)
        assertEquals("Не удалось прочитать JSON. Проверьте запятые, кавычки и числовые значения.", (result as ImportParseResult.Error).message)
    }
}
