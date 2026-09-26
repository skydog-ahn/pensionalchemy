package com.pension.alchemy

import com.pension.alchemy.data.model.Asset
import com.pension.alchemy.data.model.AssetType
import com.pension.alchemy.data.model.RepaymentMethod
import com.pension.alchemy.data.model.UserProfile
import com.pension.alchemy.domain.engine.SimulationEngine
import com.pension.alchemy.util.LoanCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanCalculatorTest {

    @Test
    fun testEqualPrincipalAndInterestMonthlyPayment() {
        // 1억원, 연 5%, 10년 (120개월) 원리금균등
        // PMT = 100,000,000 * (0.05/12 * (1+0.05/12)^120) / ((1+0.05/12)^120 - 1)
        // 약 1,060,655원
        val payment = LoanCalculator.calculateMonthlyPayment(
            principal = 100_000_000L,
            annualRatePercent = 5.0,
            maturityYears = 10,
            repaymentMethod = RepaymentMethod.EQUAL_PRINCIPAL_AND_INTEREST
        )
        assertTrue("월 원리금 상환액은 약 1,060,655원이어야 함 (실제: $payment)", payment in 1_050_000L..1_070_000L)
    }

    @Test
    fun testBulletMonthlyPayment() {
        // 1억원, 연 6%, 만기일시/이자만 -> 월 이자 = 100,000,000 * 0.06 / 12 = 500,000원
        val payment = LoanCalculator.calculateMonthlyPayment(
            principal = 100_000_000L,
            annualRatePercent = 6.0,
            maturityYears = 5,
            repaymentMethod = RepaymentMethod.BULLET
        )
        assertEquals(500_000L, payment)
    }

    @Test
    fun testAnnualRepaymentProgressionEqualPrincipal() {
        // 1.2억원, 10년, 원금균등 -> 매년 원금 1,200만원 상환
        var balance = 120_000_000L
        for (year in 0 until 10) {
            val result = LoanCalculator.calculateAnnualRepayment(
                currentBalance = balance,
                originalPrincipal = 120_000_000L,
                annualRatePercent = 4.0,
                maturityYears = 10,
                repaymentMethod = RepaymentMethod.EQUAL_PRINCIPAL,
                yearIndex = year
            )
            assertEquals(12_000_000L, result.principalPayment)
            balance = result.remainingBalance
        }
        assertEquals(0L, balance)
    }

    @Test
    fun testSimulationEngineDebtReductionOverYears() {
        // 40세 사용자, 60세 은퇴, 10년 만기 1억원 대출
        val profile = UserProfile(
            birthYear = 1986, // 40세
            retirementAge = 60,
            targetEndAge = 90,
            monthlyExpenses = 3_000_000L
        )
        val debtAsset = Asset(
            name = "테스트 대출",
            type = AssetType.DEBT,
            currentValue = 100_000_000L,
            expectedGrowthRate = 4.0,
            repaymentMethod = RepaymentMethod.EQUAL_PRINCIPAL_AND_INTEREST,
            maturityYears = 10
        )
        val assets = listOf(
            Asset(name = "예금", type = AssetType.DEPOSIT, currentValue = 200_000_000L, expectedGrowthRate = 3.0),
            debtAsset
        )

        val summary = SimulationEngine.runComprehensiveSimulation(profile, emptyList(), assets, emptyList())
        // 현재 총 부채 스냅샷 확인
        assertEquals(100_000_000L, summary.currentTotalDebt)

        // 40세 말 (1년차 상환 후) 잔여 대출
        val firstYearDebt = summary.yearlyResults.first { it.age == 40 }.totalDebt
        assertTrue("1년 상환 후 잔여 부채는 1억원보다 줄어야 함 (현재: $firstYearDebt)", firstYearDebt < 100_000_000L && firstYearDebt > 80_000_000L)

        // 5년 후 (45세)에는 대출 잔액이 더욱 감소해 있어야 함
        val midDebt = summary.yearlyResults.first { it.age == 45 }.totalDebt
        assertTrue("5년 후 부채는 1년차보다 줄어들어야 함 (현재: $midDebt)", midDebt < firstYearDebt && midDebt > 0L)

        // 10년 후 (50세)에는 대출이 전액 상환되어 0원이어야 함
        val endDebt = summary.yearlyResults.first { it.age == 50 }.totalDebt
        assertEquals(0L, endDebt)
    }
}
