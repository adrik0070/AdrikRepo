package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

    /**
     * Formats an integer rupee value using the Indian numbering system (e.g., ₹ 30,419 or ₹ -219).
     */
    fun formatInr(amount: Long, showCurrencySymbol: Boolean = true): String {
        val isNegative = amount < 0
        val digits = abs(amount).toString()
        val formattedDigits = if (digits.length <= 3) {
            digits
        } else {
            val lastThree = digits.takeLast(3)
            val remaining = digits.dropLast(3)
            val groupedRemaining = remaining.reversed()
                .chunked(2)
                .joinToString(",")
                .reversed()
            "$groupedRemaining,$lastThree"
        }
        val sign = if (isNegative) "-" else ""
        return if (showCurrencySymbol) "₹ $sign$formattedDigits" else "$sign$formattedDigits"
    }

    fun todayDateStr(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        return sdf.format(Date())
    }
}
