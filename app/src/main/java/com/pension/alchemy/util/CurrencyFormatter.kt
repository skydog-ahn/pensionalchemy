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
                eok > 0 && man > 0 && won > 0 -> "${decimalFormat.format(eok)}억 ${decimalFormat.format(man)}만 ${decimalFormat.format(won)}원"
                eok > 0 && man > 0 -> "${decimalFormat.format(eok)}억 ${decimalFormat.format(man)}만원"
                eok > 0 && won > 0 -> "${decimalFormat.format(eok)}억 ${decimalFormat.format(won)}원"
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

    fun formatNumber(value: Long): String {
        return decimalFormat.format(value)
    }

    /**
     * 만원 단위를 기본으로 하고, 타이머 가산 단위인 원 단위를 분리하는 데이터 클래스
     */
    data class FormattedManAndWon(
        val isNegative: Boolean,
        val manPart: String,       // 만원 단위 텍스트 (예: "5억 2,000만", "8,500만")
        val wonPart: String,       // 타이머 가산 원 단위 텍스트 (예: "3,456원", "0원")
        val fullFormatted: String  // 결합 텍스트 (예: "5억 2,000만 3,456원")
    )

    /**
     * 금액을 만원 단위(큰 숫자용)와 원 단위(타이머 가산 작은 숫자용)로 분리
     */
    fun splitManAndWon(amount: Long, alwaysIncludeWon: Boolean = true): FormattedManAndWon {
        if (amount == 0L) {
            return FormattedManAndWon(
                isNegative = false,
                manPart = if (alwaysIncludeWon) "0만" else "0원",
                wonPart = if (alwaysIncludeWon) "0원" else "",
                fullFormatted = "0원"
            )
        }

        val isNegative = amount < 0
        val absAmount = abs(amount)

        val eok = absAmount / 100_000_000L      // 억 (10^8)
        val remainder = absAmount % 100_000_000L
        val man = remainder / 10_000L          // 만 (10^4)
        val won = remainder % 10_000L          // 원

        val signStr = if (isNegative) "-" else ""

        val rawManText = when {
            eok > 0 && man > 0 -> "$signStr${decimalFormat.format(eok)}억 ${decimalFormat.format(man)}만"
            eok > 0 -> "$signStr${decimalFormat.format(eok)}억"
            man > 0 -> "$signStr${decimalFormat.format(man)}만"
            alwaysIncludeWon -> "${signStr}0만"
            else -> ""
        }

        val wonText = when {
            won > 0 -> "${decimalFormat.format(won)}원"
            alwaysIncludeWon -> "0원"
            else -> ""
        }

        val (manPart, full) = when {
            rawManText.isNotEmpty() && wonText.isNotEmpty() -> {
                rawManText to "$rawManText $wonText"
            }
            rawManText.isNotEmpty() -> {
                "${rawManText}원" to "${rawManText}원"
            }
            wonText.isNotEmpty() -> {
                "" to "$signStr$wonText"
            }
            else -> "0원" to "0원"
        }

        return FormattedManAndWon(
            isNegative = isNegative,
            manPart = manPart,
            wonPart = wonText,
            fullFormatted = full
        )
    }
}