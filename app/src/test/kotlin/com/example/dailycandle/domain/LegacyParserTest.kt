package com.example.dailycandle.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class LegacyParserTest {
    @Test fun `read both historical date formats and scientific double strings`() {
        val result = LegacyParser.parse(LegacySnapshot("10.0,1.0E-8,-4.25", "2026-01-03,2026/01/01,2026/01/02"))
        assertEquals(listOf("0.00000001", "-4.25", "10"), result.entries.map { AmountInput.canonical(it.value) })
        assertEquals(0, result.rejected)
    }

    @Test fun `invalid values never shift later dates`() {
        val result = LegacyParser.parse(LegacySnapshot("1,bad,3", "2026/01/01,2026/01/02,2026/01/03"))
        assertEquals(listOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3)), result.entries.map { it.date })
        assertEquals("3", AmountInput.canonical(result.entries.last().value))
        assertEquals(1, result.rejected)
    }

    @Test fun `mismatches malformed dates and nonfinite values are rejected independently`() {
        val result = LegacyParser.parse(LegacySnapshot("1,NaN,3,Infinity,5", "2026/01/01,2026/01/02,2026/02/30,2026/01/04"))
        assertEquals(1, result.entries.size)
        assertEquals(4, result.rejected)
        assertEquals(2, LegacyParser.parse(LegacySnapshot("", "2026/01/01,2026/01/02")).rejected)
    }

    @Test fun `last valid duplicate wins while rejected slots do not replace it`() {
        val snapshot = LegacySnapshot("1,2,bad", "2026/01/01,2026/01/01,2026/01/01")
        val result = LegacyParser.parse(snapshot)
        assertEquals("2", AmountInput.canonical(result.entries.single().value))
        assertEquals(1, result.duplicateDates)
        assertEquals(1, result.rejected)
        assertEquals(result, LegacyParser.parse(snapshot))
        assertEquals("1,2,bad", snapshot.values)
    }
}
