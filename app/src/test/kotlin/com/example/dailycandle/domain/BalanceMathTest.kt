package com.example.dailycandle.domain

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class BalanceMathTest {
    @Test fun `undefined pointer centroids cannot corrupt a viewport`() {
        val latest = ChartViewport.latest(100)
        assertEquals(latest, latest.transform(100, Double.NaN, Double.NaN, Double.NaN))
        assertEquals(latest, ChartViewport(Double.NaN, Double.NaN).bounded(100))
        assertTrue(ChartViewport(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).bounded(100).start.isFinite())
    }
    private fun entry(day: Int, value: String) = DailyEntry(LocalDate.of(2026, 1, 1).plusDays(day.toLong()), value.toBigDecimal())

    @Test fun `order by actual date and keep final duplicate`() {
        val ordered = orderedEntries(listOf(entry(5, "30"), entry(1, "10"), entry(5, "40")))
        assertEquals(listOf(entry(1, "10"), entry(5, "40")), ordered)
    }

    @Test fun `candles connect consecutive records across gaps with real dates`() {
        val candles = buildCandles(listOf(entry(9, "25"), entry(0, "10"), entry(5, "20")))
        assertEquals(2, candles.size)
        assertEquals(entry(5, "20").date, candles.first().date)
        assertEquals(entry(0, "10").date, candles.first().previousDate)
        assertEquals(BigDecimal("10"), candles.first().open)
        assertEquals(BigDecimal("20"), candles.first().close)
    }

    @Test fun `increase decrease and unchanged candles preserve exact differences`() {
        val changes = buildCandles(listOf(entry(0, "1.01"), entry(1, "1.02"), entry(2, "-2"), entry(3, "-2.00")))
        assertEquals(listOf(Direction.INCREASE, Direction.DECREASE, Direction.UNCHANGED), changes.map { it.change.direction })
        assertEquals(0, BigDecimal("0.01").compareTo(changes.first().change.absolute))
        assertEquals(0, changes.last().change.absolute.signum())
    }

    @Test fun `percentage handles zero and a negative baseline intentionally`() {
        assertNull(Change.between(BigDecimal.ZERO, BigDecimal.ONE).percent)
        assertNull(Change.between(BigDecimal.ZERO, BigDecimal.ZERO).percent)
        assertEquals(0, BigDecimal("50").compareTo(Change.between(BigDecimal("-100"), BigDecimal("-50")).percent))
        assertEquals(0, BigDecimal("-50").compareTo(Change.between(BigDecimal("100"), BigDecimal("50")).percent))
    }

    @Test fun `empty and first entry have no invented changes`() {
        assertNull(summarize(emptyList()))
        assertTrue(buildCandles(listOf(entry(0, "8"))).isEmpty())
        val summary = summarize(listOf(entry(0, "8")))!!
        assertNull(summary.latestChange)
        assertNull(summary.totalChange)
        assertEquals(summary.highest, summary.lowest)
    }

    @Test fun `summary uses chronological endpoints and date based cutoffs`() {
        val summary = summarize(listOf(entry(40, "30"), entry(0, "10"), entry(15, "50"), entry(32, "20")))!!
        assertEquals(BigDecimal("30"), summary.latest.value)
        assertEquals(BigDecimal("10"), summary.latestChange!!.absolute)
        assertEquals(BigDecimal("20"), summary.totalChange!!.absolute)
        assertEquals(BigDecimal("10"), summary.sevenDayChange!!.absolute)
        assertEquals(BigDecimal("20"), summary.thirtyDayChange!!.absolute)
        assertEquals(BigDecimal("50"), summary.highest.value)
        assertEquals(BigDecimal("10"), summary.lowest.value)
        assertNull(summarize(listOf(entry(0, "1"), entry(3, "2")))!!.sevenDayChange)
    }

    @Test fun `very large and tiny chart values stay finite after normalization`() {
        listOf(
            listOf(BigDecimal("1E+70"), BigDecimal("1E+70").add(BigDecimal.ONE)),
            listOf(BigDecimal("-1E-40"), BigDecimal("1E-40")),
            listOf(BigDecimal.ZERO, BigDecimal.ZERO),
            listOf(BigDecimal("-3")),
        ).forEach { values ->
            val range = ValueRange.padded(values)
            assertTrue(range.high > range.low)
            values.forEach { assertTrue(range.fraction(it).isFinite()); assertTrue(range.fraction(it) in 0f..1f) }
        }
    }

    @Test fun `valid imported calendar extremes cannot break recent summaries`() {
        val minimum = DailyEntry(LocalDate.MIN, BigDecimal.TEN)
        val first = summarize(listOf(minimum))!!
        assertNull(first.sevenDayChange)
        assertNull(first.thirtyDayChange)
        val next = summarize(listOf(minimum, minimum.copy(date = LocalDate.MIN.plusDays(7), value = BigDecimal("12"))))!!
        assertEquals(BigDecimal("2"), next.sevenDayChange!!.absolute)
        assertNull(next.thirtyDayChange)
        assertNull(summarize(listOf(minimum.copy(date = LocalDate.MAX)))!!.sevenDayChange)
    }

    @Test fun `viewport bounds zoom pan and reset with long histories`() {
        val initial = ChartViewport.latest(100_000)
        assertEquals(99_976.0, initial.start, 0.0)
        val zoomed = initial.transform(100_000, 4.0, 0.0, 0.5)
        assertEquals(6.0, zoomed.count, 0.0)
        assertTrue(initial.transform(100_000, 1.0, 1E10, 0.5).start >= 0)
        assertEquals(365.0, ChartViewport(0.0, 1E10).bounded(100_000).count, 0.0)
        assertEquals(1.0, ChartViewport.latest(1).bounded(1).count, 0.0)
    }
}
