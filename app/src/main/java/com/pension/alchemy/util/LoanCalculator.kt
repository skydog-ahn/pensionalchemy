package com.pension.alchemy.util

import com.pension.alchemy.data.model.RepaymentMethod
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 대출 및 부채 상환 계산 유틸리티
 */
object LoanCalculator {

    /**
     * 월 원리금 또는 이자 상환액 계산 (원 단위)
     */
    fun calculateMonthlyPayment(
        principal: Long,
        annualRatePercent: Double,
        maturityYears: Int,
        repaymentMethod: RepaymentMethod
    ): Long {
        if (principal <= 0L || maturityYears <= 0) return 0L
        val monthlyRate = annualRatePercent / 100.0 / 12.0
        val totalMonths = maturityYears * 12

        return when (repaymentMethod) {
            RepaymentMethod.EQUAL_PRINCIPAL_AND_INTEREST -> {
                if (monthlyRate <= 0.0) {
                    (principal.toDouble() / totalMonths).roundToLong()
                } else {
                    val factor = (1.0 + monthlyRate).pow(totalMonths.toDouble())
                    val monthlyPayment = principal.toDouble() * (monthlyRate * factor) / (factor - 1.0)
                    monthlyPayment.roundToLong()
                }
            }
            RepaymentMethod.EQUAL_PRINCIPAL -> {
                // 첫 달 기준 (원금 균등 + 첫 달 이자)
                val monthlyPrincipal = principal.toDouble() / totalMonths
                val firstMonthInterest = principal.toDouble() * monthlyRate
                (monthlyPrincipal + firstMonthInterest).roundToLong()
            }
            RepaymentMethod.BULLET, RepaymentMethod.INTEREST_ONLY -> {
                // 매월 이자만 납입
                (principal.toDouble() * monthlyRate).roundToLong()
            }
        }
    }

    /**
     * 1년간의 부채 상환액 (원금 상환액, 이자 지출액, 기말 잔액) 계산
     *
     * @param currentBalance 연초 잔여 부채 원금
     * @param originalPrincipal 최초 대출 원금
     * @param annualRatePercent 연간 대출 금리 (%)
     * @param maturityYears 총 대출 기간 (년)
     * @param repaymentMethod 상환 방식
     * @param yearIndex 대출 시작 후 경과 년수 (0부터 시작)
     */
    fun calculateAnnualRepayment(
        currentBalance: Long,
        originalPrincipal: Long,
        annualRatePercent: Double,
        maturityYears: Int,
        repaymentMethod: RepaymentMethod,
        yearIndex: Int
    ): AnnualRepaymentResult {
        if (currentBalance <= 0L) {
            return AnnualRepaymentResult(0L, 0L, 0L)
        }

        val rate = max(0.0, annualRatePercent / 100.0)
        val annualInterest = (currentBalance.toDouble() * rate).roundToLong()

        return when (repaymentMethod) {
            RepaymentMethod.EQUAL_PRINCIPAL_AND_INTEREST -> {
                if (yearIndex >= maturityYears) {
                    // 기간 만료 시 남은 잔액 모두 상환
                    AnnualRepaymentResult(
                        principalPayment = currentBalance,
                        interestPayment = annualInterest,
                        remainingBalance = 0L
                    )
                } else {
                    val monthlyPayment = calculateMonthlyPayment(
                        originalPrincipal,
                        annualRatePercent,
                        maturityYears,
                        repaymentMethod
                    )
                    val annualTotalPayment = monthlyPayment * 12L
                    // 원금 상환분 = 연간 총 상환액 - 이자
                    val calculatedPrincipalPayment = max(0L, annualTotalPayment - annualInterest)
                    val actualPrincipalPayment = min(currentBalance, calculatedPrincipalPayment)
                    // 만약 마지막 해라면 잔액 전액 상환
                    val finalPrincipalPayment = if (yearIndex == maturityYears - 1) {
                        currentBalance
                    } else {
                        actualPrincipalPayment
                    }
                    val remaining = max(0L, currentBalance - finalPrincipalPayment)
                    AnnualRepaymentResult(
                        principalPayment = finalPrincipalPayment,
                        interestPayment = annualInterest,
                        remainingBalance = remaining
                    )
                }
            }

            RepaymentMethod.EQUAL_PRINCIPAL -> {
                if (yearIndex >= maturityYears - 1) {
                    AnnualRepaymentResult(
                        principalPayment = currentBalance,
                        interestPayment = annualInterest,
                        remainingBalance = 0L
                    )
                } else {
                    val annualPrincipal = (originalPrincipal.toDouble() / maturityYears.toDouble()).roundToLong()
                    val actualPrincipal = min(currentBalance, annualPrincipal)
                    val remaining = max(0L, currentBalance - actualPrincipal)
                    AnnualRepaymentResult(
                        principalPayment = actualPrincipal,
                        interestPayment = annualInterest,
                        remainingBalance = remaining
                    )
                }
            }

            RepaymentMethod.BULLET -> {
                // 만기 도래 해(maturityYears - 1 또는 그 이후)에 원금 일시 상환
                if (yearIndex >= maturityYears - 1) {
                    AnnualRepaymentResult(
                        principalPayment = currentBalance,
                        interestPayment = annualInterest,
                        remainingBalance = 0L
                    )
                } else {
                    // 기간 중에는 이자만 납입
                    AnnualRepaymentResult(
                        principalPayment = 0L,
                        interestPayment = annualInterest,
                        remainingBalance = currentBalance
                    )
                }
            }

            RepaymentMethod.INTEREST_ONLY -> {
                // 원금 상환 없음, 매년 이자만 납입
                AnnualRepaymentResult(
                    principalPayment = 0L,
                    interestPayment = annualInterest,
                    remainingBalance = currentBalance
                )
            }
        }
    }
}

/**
 * 연간 대출 상환 결과 모델
 */
data class AnnualRepaymentResult(
    val principalPayment: Long,  // 당해 연도 상환된 원금
    val interestPayment: Long,   // 당해 연도 납부한 이자
    val remainingBalance: Long   // 연말 잔여 대출 원금
) {
    val totalCashOutflow: Long get() = principalPayment + interestPayment
}
