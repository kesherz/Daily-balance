package com.example.dailycandle.domain

import org.junit.Assert.*
import org.junit.Test

class AmountInputTest {
    @Test fun `accept exact signed decimals and decimal commas`() {
        assertEquals("-1234.005", AmountInput.canonical(AmountInput.parse(" -1234,005 ")!!))
        assertEquals("0.25", AmountInput.canonical(AmountInput.parse(".25")!!))
        assertEquals("0", AmountInput.canonical(AmountInput.parse("-0.000")!!))
    }

    @Test fun `accept Persian and Arabic digits`() {
        assertEquals("-12.05", AmountInput.canonical(AmountInput.parse("−۱۲٫۰۵")!!))
        assertEquals("42.7", AmountInput.canonical(AmountInput.parse("٤٢,٧")!!))
    }

    @Test fun `reject nonfinite malformed and ambiguous values`() {
        listOf("", " ", "NaN", "Infinity", "-Infinity", "1.2.3", "1,234.50", "1 234", "۱٬۲۳۴", "--1", "1e3").forEach {
            assertNull(it, AmountInput.parse(it))
        }
    }

    @Test fun `bound input without corrupting accepted precision`() {
        val exact = "9".repeat(AmountInput.MAX_INTEGER_DIGITS) + "." + "1".repeat(AmountInput.MAX_DECIMAL_PLACES)
        assertEquals(exact, AmountInput.canonical(AmountInput.parse(exact)!!))
        assertNull(AmountInput.parse("9".repeat(AmountInput.MAX_INTEGER_DIGITS + 1)))
        assertNull(AmountInput.parse("0." + "0".repeat(AmountInput.MAX_DECIMAL_PLACES) + "1"))
        assertNull(AmountInput.parseCanonical("1e999"))
        assertEquals("0.00000001", AmountInput.canonical(AmountInput.parseCanonical("1.0E-8")!!))
    }

    @Test fun `preserve extremes of the legacy finite Double range`() {
        listOf(Double.MAX_VALUE, -Double.MAX_VALUE, Double.MIN_VALUE, -Double.MIN_VALUE).forEach { old ->
            val parsed = AmountInput.parseCanonical(old.toString())!!
            assertEquals(0, old.toString().toBigDecimal().compareTo(parsed))
            assertEquals(parsed, AmountInput.parse(AmountInput.canonical(parsed)))
        }
    }
}
