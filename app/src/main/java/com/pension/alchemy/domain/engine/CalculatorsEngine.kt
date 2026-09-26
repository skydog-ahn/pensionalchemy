package com.pension.alchemy.domain.engine

import com.pension.alchemy.data.model.*
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToLong

object CalculatorsEngine {

    /**
     * 모드 1: 적립 및 예탁 미래가치 계산 (Accumulation & Lump-sum Future Value)
     */
    fun calculateAccumulation(
        monthlyDeposit: Long,
        periodYears: Int,
        annualInterestRate: Double,
        initialDeposit: Long = 0L
    ): AccumulationResult {
        val r = annualInterestRate / 100.0 / 12.0
        val totalMonths = periodYears * 12
        var currentBalance = initialDeposit.toDouble()
        var totalPrincipal = initialDeposit
        val yearlyList = mutableListOf<AccumulationYearResult>()

        for (m in 1..totalMonths) {
            currentBalance = (currentBalance + monthlyDeposit.toDouble()) * (1.0 + r)
            totalPrincipal += monthlyDeposit

            if (m % 12 == 0) {
                val year = m / 12
                val balanceLong = currentBalance.roundToLong()
                yearlyList.add(
                    AccumulationYearResult(
                        year = year,
                        totalDeposit = totalPrincipal,
                        totalInterest = (balanceLong - totalPrincipal).coerceAtLeast(0L),
                        balance = balanceLong
                    )
                )
            }
        }

        val futureValue = currentBalance.roundToLong()
        val totalMonthly = monthlyDeposit * totalMonths
        return AccumulationResult(
            futureValue = futureValue,
            initialDeposit = initialDeposit,
            totalMonthlyDeposits = totalMonthly,
            totalPrincipal = totalPrincipal,
            totalInterest = (futureValue - totalPrincipal).coerceAtLeast(0L),
            yearlyBreakdown = yearlyList
        )
    }

    /**
     * 모드 2: 자산 인출 시뮬레이션 (Withdrawal)
     */
    fun calculateWithdrawal(
        startingWealth: Long,
        annualGrowthRate: Double,
        isFixedAmount: Boolean,
        monthlyWithdrawal: Long,
        annualWithdrawalRate: Double,
        startAge: Int = 60,
        yearsToSimulate: Int = 35
    ): WithdrawalResult {
        var balance = startingWealth.toDouble()
        val yearlyList = mutableListOf<WithdrawalYearResult>()
        var isDepleted = false
        var depletionAge = 0

        val r = annualGrowthRate / 100.0

        for (y in 1..yearsToSimulate) {
            val age = startAge + y - 1
            val startBal = balance

            if (balance <= 0.0) {
                if (!isDepleted) {
                    isDepleted = true
                    depletionAge = age
                }
                yearlyList.add(
                    WithdrawalYearResult(
                        age = age,
                        year = y,
                        startingBalance = 0L,
                        growthAmount = 0L,
                        withdrawalAmount = 0L,
                        endingBalance = 0L
                    )
                )
                continue
            }

            val withdrawalForYear = if (isFixedAmount) {
                monthlyWithdrawal.toDouble() * 12.0
            } else {
                startBal * (annualWithdrawalRate / 100.0)
            }

            val growth = startBal * r
            balance = startBal + growth - withdrawalForYear

            if (balance <= 0.0) {
                balance = 0.0
                if (!isDepleted) {
                    isDepleted = true
                    depletionAge = age
                }
            }

            yearlyList.add(
                WithdrawalYearResult(
                    age = age,
                    year = y,
                    startingBalance = startBal.roundToLong(),
                    growthAmount = growth.roundToLong(),
                    withdrawalAmount = withdrawalForYear.roundToLong(),
                    endingBalance = balance.roundToLong()
                )
            )
        }

        return WithdrawalResult(
            isDepleted = isDepleted,
            depletionAge = depletionAge,
            finalBalance = balance.roundToLong(),
            yearlyBreakdown = yearlyList
        )
    }

    /**
     * 모드 3: 자산 고갈기간 계산 (Depletion)
     */
    fun calculateDepletion(
        currentWealth: Long,
        monthlyWithdrawal: Long,
        annualGrowthRate: Double,
        inflationRate: Double,
        currentAge: Int = 60
    ): DepletionResult {
        val annualWithdrawal = monthlyWithdrawal * 12.0
        val netRate = (annualGrowthRate - inflationRate) / 100.0

        // 원금 보존 케이스 (수익이 인출액 이상)
        if (netRate > 0 && (currentWealth * netRate) >= annualWithdrawal) {
            return DepletionResult(
                depletionYears = 999.0,
                depletionAge = 999.0,
                isForeverSafe = true,
                recommendedSafeMonthlyWithdrawal = ((currentWealth * netRate) / 12.0).roundToLong()
            )
        }

        // 고갈 연수 시뮬레이션
        var bal = currentWealth.toDouble()
        var months = 0
        val monthlyRate = netRate / 12.0

        while (bal > 0 && months < 1200) { // 최대 100년
            bal = (bal * (1.0 + monthlyRate)) - monthlyWithdrawal.toDouble()
            months++
        }

        val years = months / 12.0
        val safeMonthly = if (netRate > 0) ((currentWealth * netRate) / 12.0).roundToLong() else (currentWealth / (30 * 12))

        return DepletionResult(
            depletionYears = years,
            depletionAge = currentAge + years,
            isForeverSafe = false,
            recommendedSafeMonthlyWithdrawal = safeMonthly
        )
    }

    /**
     * 모드 4: 목표 필요자산 역산 (Required Wealth)
     */
    fun calculateRequiredWealth(
        desiredMonthlyExpense: Long,
        retirementYears: Int = 30,
        expectedReturnRate: Double = 4.0,
        currentAge: Int = 45,
        targetRetirementAge: Int = 60
    ): RequiredWealthResult {
        val annualExpense = desiredMonthlyExpense.toDouble() * 12.0
        val r = (expectedReturnRate / 100.0).coerceAtLeast(0.001)

        // 연금 현가 공식: PV = PMT * [1 - (1+r)^(-n)] / r
        val targetTotal = annualExpense * (1.0 - (1.0 + r).pow(-retirementYears.toDouble())) / r
        val targetWealthLong = targetTotal.roundToLong()

        // 축적 기간 동안 필요한 월 저축액 계산
        val yearsToSave = (targetRetirementAge - currentAge).coerceAtLeast(1)
        val monthsToSave = yearsToSave * 12
        val saveMonthlyR = r / 12.0

        // FV = PMT * [(1+r)^n - 1] / r  ==> PMT = FV * r / [(1+r)^n - 1]
        val monthlySavingsNeeded = if (saveMonthlyR > 0) {
            targetTotal * saveMonthlyR / ((1.0 + saveMonthlyR).pow(monthsToSave.toDouble()) - 1.0)
        } else {
            targetTotal / monthsToSave
        }

        return RequiredWealthResult(
            targetTotalWealth = targetWealthLong,
            monthlySavingsNeeded = monthlySavingsNeeded.roundToLong(),
            expectedAnnualRetirementIncome = (targetWealthLong * 0.04).roundToLong() // 4% 룰 기준 연 인출액
        )
    }

    /**
     * 모드 5: 국민연금 조기 vs 정상 vs 연기 손익분기점 (Break-even)
     */
    fun calculateNationalPensionBreakEven(
        normalMonthlyAmount: Long = 1_500_000L,
        statutoryAge: Int = 65,
        policy: PolicySettings = PolicySettings()
    ): NationalPensionBreakEvenResult {
        // 조기수령 (5년 조기, 설정된 연간 감액률 반영)
        val earlyAge = statutoryAge - 5
        val earlyFactor = 1.0 - (5.0 * (policy.nationalEarlyReductionRatePerYear / 100.0))
        val earlyMonthly = (normalMonthlyAmount * earlyFactor).roundToLong()

        // 정상수령 (100%)
        val normalAge = statutoryAge
        val normalMonthly = normalMonthlyAmount

        // 연기수령 (5년 연기, 설정된 연간 증액률 반영)
        val delayedAge = statutoryAge + 5
        val delayedFactor = 1.0 + (5.0 * (policy.nationalDelayIncreaseRatePerYear / 100.0))
        val delayedMonthly = (normalMonthlyAmount * delayedFactor).roundToLong()

        val comparisonList = mutableListOf<BreakEvenAgeResult>()
        var earlyCumulative = 0L
        var normalCumulative = 0L
        var delayedCumulative = 0L

        var earlyVsNormalBreakEven = 0
        var normalVsDelayedBreakEven = 0

        for (age in earlyAge..95) {
            if (age >= earlyAge) {
                earlyCumulative += earlyMonthly * 12L
            }
            if (age >= normalAge) {
                normalCumulative += normalMonthly * 12L
            }
            if (age >= delayedAge) {
                delayedCumulative += delayedMonthly * 12L
            }

            if (earlyVsNormalBreakEven == 0 && normalCumulative >= earlyCumulative && age >= normalAge) {
                earlyVsNormalBreakEven = age
            }
            if (normalVsDelayedBreakEven == 0 && delayedCumulative >= normalCumulative && age >= delayedAge) {
                normalVsDelayedBreakEven = age
            }

            comparisonList.add(
                BreakEvenAgeResult(
                    age = age,
                    earlyCumulative = earlyCumulative,
                    normalCumulative = normalCumulative,
                    delayedCumulative = delayedCumulative
                )
            )
        }

        return NationalPensionBreakEvenResult(
            earlyMonthlyAmount = earlyMonthly,
            normalMonthlyAmount = normalMonthly,
            delayedMonthlyAmount = delayedMonthly,
            earlyVsNormalBreakEvenAge = if (earlyVsNormalBreakEven > 0) earlyVsNormalBreakEven else 76,
            normalVsDelayedBreakEvenAge = if (normalVsDelayedBreakEven > 0) normalVsDelayedBreakEven else 82,
            comparisonList = comparisonList
        )
    }

    /**
     * 모드 6: 연금저축/IRP 세액공제 & 복리 효과 (Tax Benefit)
     */
    fun calculateTaxBenefit(
        annualContribution: Long = 9_000_000L,
        isLowIncomeTier: Boolean = true,
        policy: PolicySettings = PolicySettings()
    ): TaxBenefitResult {
        val cappedContribution = annualContribution.coerceIn(0L, policy.maxTaxCreditContribution)
        val taxRate = if (isLowIncomeTier) policy.taxCreditRateLowIncome / 100.0 else policy.taxCreditRateHighIncome / 100.0
        val annualRefund = (cappedContribution * taxRate).roundToLong()

        // 30년간 환급금 단순 합계
        val cumulative30Years = annualRefund * 30L

        // 연 6% 복리 재투자 시 30년 후 환급금 미래가치
        val r = 0.06
        var reinvestedFV = 0.0
        for (y in 1..30) {
            reinvestedFV = (reinvestedFV + annualRefund.toDouble()) * (1.0 + r)
        }

        // 일반 과세 계좌(매년 이자소득세 차감)로 동일 금액 투자 시
        val afterTaxR = 0.06 * (1.0 - (policy.generalInterestTaxRate / 100.0))
        var generalAccFV = 0.0
        for (y in 1..30) {
            generalAccFV = (generalAccFV + cappedContribution.toDouble()) * (1.0 + afterTaxR)
        }

        // 연금저축 계좌로 비과세 복리 운용 시
        var pensionAccFV = 0.0
        for (y in 1..30) {
            pensionAccFV = (pensionAccFV + cappedContribution.toDouble()) * (1.0 + r)
        }

        val bonus = (pensionAccFV - generalAccFV) + reinvestedFV

        return TaxBenefitResult(
            annualContribution = cappedContribution,
            taxRefundAmount = annualRefund,
            cumulativeTaxRefund30Years = cumulative30Years,
            reinvestedFutureValue30Years = reinvestedFV.roundToLong(),
            generalTaxAccountFutureValue = generalAccFV.roundToLong(),
            taxSavingsAlchemyBonus = bonus.roundToLong()
        )
    }
}