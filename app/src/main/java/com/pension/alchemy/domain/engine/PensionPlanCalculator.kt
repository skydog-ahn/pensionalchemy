package com.pension.alchemy.domain.engine

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 개인연금, 퇴직연금/IRP, 개인연금보험 전용 상호연동 연산 엔진
 *
 * 1. 수급 개시 시점 예상 적립금 산출 (현재잔액 + 월납입금 + 복리운용)
 * 2. 수급 기간(년수/종료나이)에 따른 월 예상 수령액 자동 연동 계산 (PMT 공식)
 * 3. 희망 월 수령액에 따른 수급 가능 기간(년수/종료나이) 자동 연동 계산 (NPER 공식)
 * 4. 개인연금보험의 종신형(100세) 및 확정 기간형 수령액 계산
 */
object PensionPlanCalculator {

    /**
     * 1. 수급 개시 시점(startAge) 총 예상 적립금 (FV) 계산
     *
     * @param currentAge 현재 나이
     * @param startAge 수급 개시 나이
     * @param currentBalance 현재 적립금 (원)
     * @param monthlyContribution 월 추가 납입액 (원)
     * @param contributionEndAge 납입 종료 나이
     * @param annualGrowthRate 연간 운용 수익률 (%)
     */
    fun calculateAccumulatedAtStartAge(
        currentAge: Int,
        startAge: Int,
        currentBalance: Long,
        monthlyContribution: Long,
        contributionEndAge: Int,
        annualGrowthRate: Double
    ): Long {
        if (startAge <= currentAge) {
            return currentBalance.coerceAtLeast(0L)
        }

        val monthlyRate = (annualGrowthRate / 100.0) / 12.0
        val totalMonths = (startAge - currentAge) * 12
        val payEndAge = contributionEndAge.coerceIn(currentAge, startAge)
        val payMonths = (payEndAge - currentAge) * 12

        var balance = currentBalance.toDouble()

        for (m in 1..totalMonths) {
            val deposit = if (m <= payMonths) monthlyContribution.toDouble() else 0.0
            balance = (balance + deposit) * (1.0 + monthlyRate)
        }

        return balance.roundToLong().coerceAtLeast(0L)
    }

    /**
     * 2. 수급 기간(년수)을 바탕으로 월 예상 수령액 계산 (PMT 연금화 공식)
     *
     * PV = PMT * [1 - (1 + i)^(-n)] / i  ==>  PMT = PV * i / [1 - (1 + i)^(-n)]
     * (수령 기간 중에도 잔여 적립금이 운용수익률로 증식된다고 가정)
     *
     * @param accumulatedFund 수급 개시 시점 적립금 (원)
     * @param periodYears 수급 기간 (년)
     * @param annualGrowthRate 수령 기간 중 연간 운용 수익률 (%)
     */
    fun calculateMonthlyPayoutFromPeriod(
        accumulatedFund: Long,
        periodYears: Int,
        annualGrowthRate: Double
    ): Long {
        if (accumulatedFund <= 0L || periodYears <= 0) return 0L

        val totalMonths = periodYears * 12
        val monthlyRate = (annualGrowthRate / 100.0) / 12.0

        if (monthlyRate <= 0.0) {
            return (accumulatedFund / totalMonths).coerceAtLeast(0L)
        }

        val discountFactor = 1.0 - (1.0 + monthlyRate).pow(-totalMonths.toDouble())
        if (discountFactor <= 0.0) return 0L

        val monthlyPayout = accumulatedFund * monthlyRate / discountFactor
        return monthlyPayout.roundToLong().coerceAtLeast(0L)
    }

    /**
     * 3. 희망 월 수령액을 바탕으로 수급 가능 기간(년수) 및 종료 나이 계산 (NPER 공식)
     *
     * n = -ln(1 - PV * i / PMT) / ln(1 + i)
     *
     * @param accumulatedFund 수급 개시 시점 적립금 (원)
     * @param desiredMonthlyPayout 희망 월 수령액 (원)
     * @param startAge 수급 개시 나이
     * @param annualGrowthRate 수령 기간 중 연간 운용 수익률 (%)
     * @param maxAge 최대 제한 연령 (기본 100세)
     * @return Pair(종료 나이 endAge, 수급 기간 년수 periodYears, 원금보존영구수급여부 isForeverSafe)
     */
    data class PeriodResult(
        val endAge: Int,
        val periodYears: Int,
        val isForeverSafe: Boolean
    )

    fun calculatePeriodFromMonthlyPayout(
        accumulatedFund: Long,
        desiredMonthlyPayout: Long,
        startAge: Int,
        annualGrowthRate: Double,
        maxAge: Int = 100
    ): PeriodResult {
        if (accumulatedFund <= 0L || desiredMonthlyPayout <= 0L) {
            return PeriodResult(endAge = startAge, periodYears = 0, isForeverSafe = false)
        }

        val monthlyRate = (annualGrowthRate / 100.0) / 12.0

        // 1) 이자만으로 월 인출액 충당 가능한 경우 (원금 불변/영구 수급)
        if (monthlyRate > 0.0 && (accumulatedFund * monthlyRate) >= desiredMonthlyPayout) {
            val maxYears = (maxAge - startAge).coerceAtLeast(1)
            return PeriodResult(
                endAge = maxAge,
                periodYears = maxYears,
                isForeverSafe = true
            )
        }

        // 2) 일반 소진 계산
        if (monthlyRate <= 0.0) {
            val months = (accumulatedFund.toDouble() / desiredMonthlyPayout.toDouble())
            val years = (months / 12.0).roundToLong().toInt().coerceAtLeast(1)
            val endAge = (startAge + years).coerceAtMost(maxAge)
            return PeriodResult(
                endAge = endAge,
                periodYears = endAge - startAge,
                isForeverSafe = false
            )
        }

        val numerator = 1.0 - (accumulatedFund * monthlyRate / desiredMonthlyPayout.toDouble())
        if (numerator <= 0.0) {
            val maxYears = (maxAge - startAge).coerceAtLeast(1)
            return PeriodResult(endAge = maxAge, periodYears = maxYears, isForeverSafe = true)
        }

        val months = -ln(numerator) / ln(1.0 + monthlyRate)
        val years = (months / 12.0).roundToLong().toInt().coerceAtLeast(1)
        val calculatedEndAge = startAge + years
        val finalEndAge = calculatedEndAge.coerceIn(startAge + 1, maxAge)

        return PeriodResult(
            endAge = finalEndAge,
            periodYears = finalEndAge - startAge,
            isForeverSafe = false
        )
    }

    /**
     * 4. 개인연금보험 월 수령액 계산
     * - 종신형: 100세까지 수급 기간 설정
     * - 확정 기간형: 10년, 15년, 20년 등 선택한 기간 기준
     */
    fun calculateAnnuityInsurancePayout(
        accumulatedFund: Long,
        startAge: Int,
        isWholeLife: Boolean,
        fixedPeriodYears: Int = 20,
        annualGrowthRate: Double = 3.5
    ): Pair<Int, Long> {
        val periodYears = if (isWholeLife) {
            (100 - startAge).coerceAtLeast(5)
        } else {
            fixedPeriodYears.coerceAtLeast(1)
        }
        val endAge = startAge + periodYears
        val monthlyAmount = calculateMonthlyPayoutFromPeriod(
            accumulatedFund = accumulatedFund,
            periodYears = periodYears,
            annualGrowthRate = annualGrowthRate
        )
        return Pair(endAge, monthlyAmount)
    }
}
