package com.example.dailycandle.domain

import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

data class DailyEntry(
    val date: LocalDate,
    val value: BigDecimal,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

/** Repository uniqueness is enforced by the date primary key. Last input wins here. */
fun orderedEntries(entries: List<DailyEntry>): List<DailyEntry> =
    entries.associateBy { it.date }.values.sortedBy { it.date }

enum class Direction { INCREASE, DECREASE, UNCHANGED }

data class Change(val absolute: BigDecimal, val percent: BigDecimal?) {
    val direction: Direction
        get() = when (absolute.signum()) {
            1 -> Direction.INCREASE
            -1 -> Direction.DECREASE
            else -> Direction.UNCHANGED
        }

    companion object {
        /** For a negative baseline, change is relative to its magnitude. Zero is undefined. */
        fun between(open: BigDecimal, close: BigDecimal): Change {
            val delta = close.subtract(open)
            return Change(
                delta,
                if (open.signum() == 0) null
                else delta.multiply(BigDecimal("100")).divide(open.abs(), MathContext.DECIMAL64),
            )
        }
    }
}

data class Candle(
    val date: LocalDate,
    val previousDate: LocalDate,
    val open: BigDecimal,
    val close: BigDecimal,
) {
    val change: Change = Change.between(open, close)
}

fun buildCandles(entries: List<DailyEntry>): List<Candle> =
    orderedEntries(entries).zipWithNext { previous, current ->
        Candle(current.date, previous.date, previous.value, current.value)
    }

data class Summary(
    val latest: DailyEntry,
    val latestChange: Change?,
    val totalChange: Change?,
    val sevenDayChange: Change?,
    val thirtyDayChange: Change?,
    val highest: DailyEntry,
    val lowest: DailyEntry,
)

fun summarize(entries: List<DailyEntry>): Summary? {
    val sorted = orderedEntries(entries)
    val latest = sorted.lastOrNull() ?: return null
    fun recent(days: Long): Change? {
        // Epoch-day arithmetic also handles a cutoff before LocalDate.MIN after CSV import.
        val cutoff = latest.date.toEpochDay() - days
        return sorted.lastOrNull { it.date.toEpochDay() <= cutoff }
            ?.let { Change.between(it.value, latest.value) }
    }
    return Summary(
        latest = latest,
        latestChange = sorted.getOrNull(sorted.lastIndex - 1)?.let {
            Change.between(it.value, latest.value)
        },
        totalChange = if (sorted.size < 2) null else Change.between(sorted.first().value, latest.value),
        sevenDayChange = recent(7),
        thirtyDayChange = recent(30),
        highest = sorted.maxBy { it.value },
        lowest = sorted.minBy { it.value },
    )
}
