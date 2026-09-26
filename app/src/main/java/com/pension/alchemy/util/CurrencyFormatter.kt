package com.pension.alchemy.util

import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.roundToLong

object CurrencyFormatter {
    private val decimalFormat = DecimalFormat("#,###")

    fun format(amount: Long, currency: String = "KRW", isShort: Boolean = false): String {
        if (currency == "KRW") {
            return formatKoreanWon(amount, isShort)
        }
        val prefix = when (currency) {
            "USD" -> "$"
            "JPY" -> "¥"
            "EUR" -> "€"
            else -> "$currency "
        }
        return if (isShort) {
            when {
                abs(amount) >= 1_000_000_000L -> "$prefix${String.format("%.1f", amount / 1_000_000_000.0)}B"
                abs(amount) >= 1_000_000L -> "$prefix${String.format("%.1f", amount / 1_000_000.0)}M"
                abs(amount) >= 1_000L -> "$prefix${String.format("%.1f", amount / 1_000.0)}K"
                else -> "$prefix${decimalFormat.format(amount)}"
            }
        } else {
            "$prefix${decimalFormat.format(amount)}"
        }
    }

    fun formatKoreanWon(amount: Long, isShort: Boolean = false): String {
        if (amount == 0L) return "0원"

        val isNegative = amount < 0
        val absAmount = abs(amount)

        val eok = absAmount / 100_000_000L      // 억 (10^8)
        val remainder = absAmount % 100_000_000L
        val man = remainder / 10_000L          // 만 (10^4)
        val won = remainder % 10_000L          // 원

        val formatted = if (isShort) {
            when {
                eok > 0 -> {
                    val eokVal = absAmount.toDouble() / 100_000_000.0
                    "${String.format("%.1f", eokVal)}억"
                }
                man > 0 -> {
                    val manVal = absAmount.toDouble() / 10_000.0
                    "${String.format("%.0f", manVal)}만"
                }
                else -> "${decimalFormat.format(absAmount)}원"
            }
        } else {
            when {
                eok > 0 && man > 0 -> "${decimalFormat.format(eok)}억 ${decimalFormat.format(man)}만원"
                eok > 0 -> "${decimalFormat.format(eok)}억원"
                man > 0 && won > 0 -> "${decimalFormat.format(man)}만 ${decimalFormat.format(won)}원"
                man > 0 -> "${decimalFormat.format(man)}만원"
                else -> "${decimalFormat.format(won)}원"
            }
        }

        return if (isNegative) "-$formatted" else formatted
    }

    fun formatMonthly(amount: Long, currency: String = "KRW"): String {
        return "${format(amount, currency, isShort = false)}/월"
    }

    fun formatPercent(rate: Double): String {
        return "${String.format("%.1f", rate)}%"
    }
}