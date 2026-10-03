package com.example.dailycandle.domain

import java.math.BigDecimal

object AmountInput {
    // Deliberate resource bounds, far beyond everyday balances. Arithmetic stays exact.
    // Also encompass every finite legacy Double, including subnormal scientific notation.
    const val MAX_INTEGER_DIGITS = 320
    const val MAX_DECIMAL_PLACES = 340
    const val MAX_TEXT_LENGTH = MAX_INTEGER_DIGITS + MAX_DECIMAL_PLACES + 3

    /** Accept localized digits and a decimal comma, but never guess grouping separators. */
    fun parse(text: String): BigDecimal? {
        if (text.length > MAX_TEXT_LENGTH) return null
        val normalized = buildString {
            text.trim().forEach { character ->
                val digit = Character.digit(character, 10)
                append(when {
                    digit >= 0 -> ('0'.code + digit).toChar()
                    character == '\u066B' || character == ',' -> '.'
                    character == '\u2212' -> '-'
                    else -> character
                })
            }
        }
        if (!normalized.matches(Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)"))) return null
        return parseCanonical(normalized)
    }

    /** Storage/CSV use ASCII and a period. Legacy Double strings may contain exponents. */
    fun parseCanonical(text: String): BigDecimal? {
        if (text.length > MAX_TEXT_LENGTH) return null
        if (!text.matches(Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]{1,3})?"))) {
            return null
        }
        val value = text.toBigDecimalOrNull()?.stripTrailingZeros() ?: return null
        if (value.precision() - value.scale() > MAX_INTEGER_DIGITS || value.scale() > MAX_DECIMAL_PLACES) {
            return null
        }
        return value
    }

    fun canonical(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()
}
