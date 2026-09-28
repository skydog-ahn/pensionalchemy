package com.pension.alchemy

import com.pension.alchemy.domain.engine.PensionPlanCalculator
import org.junit.Assert.*
import org.junit.Test

class PensionPlanCalculatorTest {

    @Test
    fun testAccumulationCalculation() {
        // 40세 현재, 60세 수령개시, 현재적립금 1000만원, 월납입 50만원, 60세까지 납입, 연수익률 5%
        val accumulated = PensionPlanCalculator.calculateAccumulatedAtStartAge(
            currentAge = 40,
            startAge = 60,
            currentBalance = 10_000_000L,
            monthlyContribution = 500_000L,
            contributionEndAge = 60,
            annualGrowthRate = 5.0
        )
        // 20년 동안 원금 = 1000만 + 50만*240 = 1억 3000만원
        // 5% 복리 적용 시 2억 이상으로 증식되어야 함
        assertTrue("적립금 총액은 원금 1억 3천만원보다 커야 함 (실제: $accumulated)", accumulated > 130_000_000L)
        assertTrue("예상 적립금 범위 체크 (2억~2.5억 사이)", accumulated in 200_000_000L..250_000_000L)
    }

    @Test
    fun testPeriodToMonthlyPayout() {
        // 2억원 적립금, 20년 수급(60세~80세), 연수익률 4%
        val monthlyPayout = PensionPlanCalculator.calculateMonthlyPayoutFromPeriod(
            accumulatedFund = 200_000_000L,
            periodYears = 20,
            annualGrowthRate = 4.0
        )
        // PMT = 2억 * (0.04/12) / [1 - (1 + 0.04/12)^(-240)] = 약 121만원
        assertTrue("월 수령액은 약 110만~130만원 사이여야 함 (실제: $monthlyPayout)", monthlyPayout in 1_100_000L..1_300_000L)
    }

    @Test
    fun testMonthlyPayoutToEndAgeLinkage() {
        // 2억원 적립금, 연수익률 4%, 60세 개시, 월 120만원 희망
        val result = PensionPlanCalculator.calculatePeriodFromMonthlyPayout(
            accumulatedFund = 200_000_000L,
            desiredMonthlyPayout = 1_211_960L,
            startAge = 60,
            annualGrowthRate = 4.0
        )
        // 약 20년 수급 -> endAge = 80세 부근이어야 함
        assertEquals(80, result.endAge)
        assertEquals(20, result.periodYears)
        assertFalse(result.isForeverSafe)
    }

    @Test
    fun testForeverSafeCase() {
        // 10억원 적립금, 5% 수익률(월 약 416만원 이자), 희망수령액 300만원
        val result = PensionPlanCalculator.calculatePeriodFromMonthlyPayout(
            accumulatedFund = 1_000_000_000L,
            desiredMonthlyPayout = 3_000_000L,
            startAge = 60,
            annualGrowthRate = 5.0,
            maxAge = 100
        )
        assertTrue(result.isForeverSafe)
        assertEquals(100, result.endAge)
    }

    @Test
    fun testAnnuityInsuranceWholeLife() {
        val (endAge, monthlyPayout) = PensionPlanCalculator.calculateAnnuityInsurancePayout(
            accumulatedFund = 100_000_000L,
            startAge = 60,
            isWholeLife = true,
            annualGrowthRate = 3.5
        )
        assertEquals(100, endAge)
        assertTrue(monthlyPayout > 0L)
    }
}
