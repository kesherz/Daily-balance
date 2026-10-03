package com.example.dailycandle.ui.format

import com.example.dailycandle.domain.AmountInput
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object DisplayFormat {
    fun value(value: BigDecimal, locale: Locale): String = NumberFormat.getNumberInstance(locale).apply {
        maximumIntegerDigits = 1_000
        maximumFractionDigits = AmountInput.MAX_DECIMAL_PLACES
        minimumFractionDigits = 0
    }.format(value)

    fun signed(value: BigDecimal, locale: Locale): String =
        (if (value.signum() > 0) "+" else "") + value(value, locale)

    fun percent(value: BigDecimal, locale: Locale): String = NumberFormat.getPercentInstance(locale).apply {
        maximumIntegerDigits = 1_000
        maximumFractionDigits = 2
    }.format(value.movePointLeft(2))

    fun date(date: LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    fun shortDate(date: LocalDate, locale: Locale, wide: Boolean): String =
        date.format(DateTimeFormatter.ofPattern(if (wide) "MMM yyyy" else "d MMM", locale))

    fun axis(value: BigDecimal, locale: Locale, step: BigDecimal? = null): String {
        val magnitude = value.abs()
        return if (magnitude >= BigDecimal("1000000") ||
            (magnitude.signum() != 0 && magnitude < BigDecimal("0.001"))) {
            DecimalFormat("0.##E0", java.text.DecimalFormatSymbols.getInstance(locale)).format(value)
        } else {
            val decimals = step?.stripTrailingZeros()?.let { (2 - it.precision() + it.scale()).coerceIn(0, 6) } ?: 4
            value(value.setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros(), locale)
        }
    }
}
