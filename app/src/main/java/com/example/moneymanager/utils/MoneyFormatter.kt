package com.example.moneymanager.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private const val WORD_JOINER = "\u2060"

// Rounding policy:
// - Compact values use truncation toward zero (RoundingMode.DOWN), not HALF_UP.
// - This keeps 999,999 as 999.9k instead of 1m.
fun formatMoneyCompact(amount: Double, locale: Locale = Locale.getDefault()): String {
    val absAmount = kotlin.math.abs(amount)
    val sign = if (amount < 0) "-" else ""
    val symbols = DecimalFormatSymbols.getInstance(locale)
    val normalFormatter = DecimalFormat("#,##0.##", symbols).apply {
        roundingMode = RoundingMode.DOWN
    }

    if (absAmount < 10_000) {
        return sign + normalFormatter.format(absAmount)
    }

    val compact = when {
        absAmount >= 1_000_000_000 -> {
            val value = BigDecimal.valueOf(absAmount)
                .divide(BigDecimal.valueOf(1_000_000_000), 2, RoundingMode.DOWN)
            DecimalFormat("#,##0.##", symbols).format(value) + WORD_JOINER + "b"
        }
        absAmount >= 1_000_000 -> {
            val value = BigDecimal.valueOf(absAmount)
                .divide(BigDecimal.valueOf(1_000_000), 2, RoundingMode.DOWN)
            DecimalFormat("#,##0.##", symbols).format(value) + WORD_JOINER + "m"
        }
        else -> {
            val value = BigDecimal.valueOf(absAmount)
                .divide(BigDecimal.valueOf(1_000), 1, RoundingMode.DOWN)
            DecimalFormat("#,##0.#", symbols).format(value) + WORD_JOINER + "k"
        }
    }

    return sign + compact
}
