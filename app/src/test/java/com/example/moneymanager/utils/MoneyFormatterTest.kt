package com.example.moneymanager.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MoneyFormatterTest {

    private val locale = Locale.US

    @Test
    fun formatMoneyCompact_formatsBelowThresholdWithSeparators() {
        assertEquals("9,999", formatMoneyCompact(9999.0, locale))
        assertEquals("0", formatMoneyCompact(0.0, locale))
    }

    @Test
    fun formatMoneyCompact_formatsThousandsWithK() {
        assertEquals("10k", formatMoneyCompact(10_000.0, locale))
        assertEquals("10.5k", formatMoneyCompact(10_500.0, locale))
        assertEquals("999.9k", formatMoneyCompact(999_999.0, locale))
    }

    @Test
    fun formatMoneyCompact_formatsMillionsAndBillions() {
        assertEquals("1m", formatMoneyCompact(1_000_000.0, locale))
        assertEquals("1.25m", formatMoneyCompact(1_250_000.0, locale))
        assertEquals("100m", formatMoneyCompact(100_000_000.0, locale))
        assertEquals("1b", formatMoneyCompact(1_000_000_000.0, locale))
    }

    @Test
    fun formatMoneyCompact_preservesNegativeSign() {
        assertEquals("-12.5k", formatMoneyCompact(-12_500.0, locale))
    }
}
