package com.example.dailycandle.domain

import java.math.BigDecimal
import java.math.MathContext

/** Index coordinates, independent of pixels; only visible candles are scanned/drawn. */
data class ChartViewport(val start: Double, val count: Double) {
    fun bounded(total: Int): ChartViewport {
        val maximum = total.coerceAtMost(365).coerceAtLeast(1).toDouble()
        val visible = (count.takeIf { it.isFinite() } ?: minOf(24.0, maximum)).coerceIn(minOf(4.0, maximum), maximum)
        val lastStart = (total - visible).coerceAtLeast(0.0)
        return ChartViewport((start.takeIf { it.isFinite() } ?: lastStart).coerceIn(0.0, lastStart), visible)
    }

    fun transform(total: Int, zoom: Double, pan: Double, anchor: Double): ChartViewport {
        val before = bounded(total)
        val safeZoom = zoom.takeIf { it.isFinite() && it > 0 } ?: 1.0
        val after = ChartViewport(before.start, before.count / safeZoom.coerceIn(0.25, 4.0)).bounded(total)
        return ChartViewport(
            before.start + (anchor.takeIf { it.isFinite() } ?: 0.5).coerceIn(0.0, 1.0) * (before.count - after.count) -
                (pan.takeIf { it.isFinite() } ?: 0.0) * after.count,
            after.count,
        ).bounded(total)
    }

    companion object {
        fun latest(total: Int) = ChartViewport((total - 24).coerceAtLeast(0).toDouble(), minOf(24, total).coerceAtLeast(1).toDouble())
    }
}

data class ValueRange(val low: BigDecimal, val high: BigDecimal) {
    private val span = high.subtract(low)
    fun fraction(value: BigDecimal): Float =
        value.subtract(low).divide(span, MathContext.DECIMAL64).toFloat()

    fun at(fraction: BigDecimal): BigDecimal = low.add(span.multiply(fraction))

    companion object {
        fun padded(values: List<BigDecimal>): ValueRange {
            val low = values.minOrNull() ?: BigDecimal.ZERO
            val high = values.maxOrNull() ?: BigDecimal.ONE
            val span = high.subtract(low)
            val padding = if (span.signum() != 0) span.multiply(BigDecimal("0.12"))
            else if (low.signum() != 0) low.abs().multiply(BigDecimal("0.01"))
            else BigDecimal.ONE
            return ValueRange(low.subtract(padding), high.add(padding))
        }
    }
}
