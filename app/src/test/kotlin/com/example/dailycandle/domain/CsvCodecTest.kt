package com.example.dailycandle.domain

import org.junit.Assert.*
import org.junit.Test
import java.io.StringReader
import java.io.StringWriter
import java.time.LocalDate

class CsvCodecTest {
    private fun read(text: String) = CsvCodec.read(StringReader(text))

    @Test fun `read BOM CRLF quoted fields negative decimals and chronological dates`() {
        val result = read("\uFEFFdate,value\r\n\"2026-01-03\",\"-1.005\"\r\n2026-01-01,0.10\r\n")
        assertNull(result.failure)
        assertEquals(listOf("0.1", "-1.005"), result.entries.map { AmountInput.canonical(it.value) })
        assertTrue(result.issues.isEmpty())
    }

    @Test fun `report malformed records and keep first valid duplicate`() {
        val result = read("date,value\n2026-02-30,2\n2026-01-01,NaN\n2026-01-01,1\n2026-01-01,2\n2026-01-03,4,5\n\"broken,3\n2026-01-04,8\n")
        assertEquals(2, result.entries.size)
        assertEquals("1", AmountInput.canonical(result.entries.first().value))
        assertEquals(5, result.invalidRows)
        assertEquals(listOf(2, 3, 5, 6, 7), result.issues.map { it.line })
        assertEquals(listOf(CsvProblem.DATE, CsvProblem.VALUE, CsvProblem.DUPLICATE_DATE, CsvProblem.COLUMNS, CsvProblem.COLUMNS), result.issues.map { it.problem })
    }

    @Test fun `bad headers and excessive input cannot partially import`() {
        assertEquals(CsvFailure.HEADER, read("").failure)
        assertEquals(CsvFailure.HEADER, read("value,date\n1,2026-01-01").failure)
        val oversized = read("date,value\n2026-01-01,1\n" + "x".repeat(CsvCodec.MAX_LINE_LENGTH + 1))
        assertEquals(CsvFailure.TOO_LARGE, oversized.failure)
        assertTrue(oversized.entries.isEmpty())
    }

    @Test fun `do not silently remove control characters or accept localized CSV values`() {
        val result = read("date,value\n2026-01-01,1\r2\n2026-01-02,\"1,2\"\n2026-01-03,۱۲\n")
        assertEquals(3, result.invalidRows)
        assertTrue(result.entries.isEmpty())
    }

    @Test fun `export stable exact values and round trip`() {
        val entries = listOf(DailyEntry(LocalDate.of(2026, 1, 2), "12345678901234567890.12345".toBigDecimal()), DailyEntry(LocalDate.of(2026, 1, 1), "-0.20".toBigDecimal()))
        val out = StringWriter()
        CsvCodec.write(entries, out)
        assertEquals("date,value\n2026-01-01,-0.2\n2026-01-02,12345678901234567890.12345\n", out.toString())
        assertEquals(entries.sortedBy { it.date }.map { AmountInput.canonical(it.value) }, read(out.toString()).entries.map { AmountInput.canonical(it.value) })
    }

    @Test fun `legacy recovery includes every unmatched and malformed slot`() {
        val writer = StringWriter()
        CsvCodec.writeLegacy(LegacySnapshot("1,bad,3", "2026/01/01,wrong"), writer)
        assertEquals("index,original_date,original_value\n1,\"2026/01/01\",\"1\"\n2,\"wrong\",\"bad\"\n3,\"\",\"3\"\n", writer.toString())
    }
}
