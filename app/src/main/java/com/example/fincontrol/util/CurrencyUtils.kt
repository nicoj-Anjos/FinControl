package com.example.fincontrol.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    fun formatFromCents(cents: Long): String = formatter.format(cents / 100.0)

    fun parseToCents(raw: String): Long? {
        val sanitized = raw.trim()
            .replace("R$", "")
            .replace(" ", "")
            .replace(".", "")
            .replace(',', '.')
        if (sanitized.isBlank()) return null
        return try {
            (sanitized.toBigDecimal() * java.math.BigDecimal(100)).longValueExact()
        } catch (_: Exception) {
            null
        }
    }
}
