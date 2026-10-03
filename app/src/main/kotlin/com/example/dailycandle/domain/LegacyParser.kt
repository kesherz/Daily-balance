package com.example.dailycandle.domain

import java.time.LocalDate

data class LegacySnapshot(val values: String, val dates: String)
data class LegacyResult(val entries: List<DailyEntry>, val rejected: Int, val duplicateDates: Int)

object LegacyParser {
    /** Pair by ORIGINAL index; a bad number must never shift subsequent dates. */
    fun parse(snapshot: LegacySnapshot): LegacyResult {
        fun slots(text: String): List<String> = if (text.isEmpty()) emptyList() else text.split(',')
        val values = slots(snapshot.values)
        val dates = slots(snapshot.dates)
        val accepted = linkedMapOf<LocalDate, DailyEntry>()
        var rejected = 0
        var duplicates = 0
        repeat(maxOf(values.size, dates.size)) { index ->
            val value = values.getOrNull(index)?.trim()?.let(AmountInput::parseCanonical)
            val dateText = dates.getOrNull(index)?.trim()?.replace('/', '-')
            val date = dateText?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            if (value == null || date == null) {
                rejected++
            } else {
                if (accepted.containsKey(date)) duplicates++
                accepted[date] = DailyEntry(date, value)
            }
        }
        return LegacyResult(accepted.values.sortedBy { it.date }, rejected, duplicates)
    }
}
