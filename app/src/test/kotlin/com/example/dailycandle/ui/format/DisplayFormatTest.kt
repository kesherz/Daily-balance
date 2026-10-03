package com.example.dailycandle.ui.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class DisplayFormatTest {
    @Test fun `very large values and derived percentages are not truncated`() {
        val large = "9".repeat(320)
        assertEquals(large, DisplayFormat.value(large.toBigDecimal(), Locale.US).replace(",", ""))
        val percentage = "1" + "0".repeat(660)
        assertEquals("$percentage%", DisplayFormat.percent(percentage.toBigDecimal(), Locale.US).replace(",", ""))
    }
    @Test fun `localized display keeps decimal precision and deliberate signs`() {
        val value = "-0.000000000000000000000000000000000000000001".toBigDecimal()
        assertEquals(value.toPlainString(), DisplayFormat.value(value, Locale.US))
        assertEquals("+1.25", DisplayFormat.signed("1.25".toBigDecimal(), Locale.US))
        assertTrue(DisplayFormat.value("1.25".toBigDecimal(), Locale.FRANCE).contains(','))
        assertTrue(DisplayFormat.value("1.25".toBigDecimal(), Locale.forLanguageTag("fa")).contains('٫'))
    }

    @Test fun `date formatting is independent of a device timezone`() {
        val date = LocalDate.of(2026, 10, 3)
        assertEquals("Oct 3, 2026", DisplayFormat.date(date, Locale.US))
        assertTrue(DisplayFormat.date(date, Locale.forLanguageTag("fa")).isNotBlank())
    }

    @Test fun `axis precision follows tick spacing and handles extremes`() {
        assertEquals("85", DisplayFormat.axis("84.9325".toBigDecimal(), Locale.US, "33.5575".toBigDecimal()))
        assertEquals("1.23456", DisplayFormat.axis("1.23456".toBigDecimal(), Locale.US, "0.00004".toBigDecimal()))
        assertEquals("1E100", DisplayFormat.axis("1e100".toBigDecimal(), Locale.US))
        assertEquals("1E-100", DisplayFormat.axis("1e-100".toBigDecimal(), Locale.US))
    }
}
