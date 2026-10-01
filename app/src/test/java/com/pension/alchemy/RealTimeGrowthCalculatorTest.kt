package com.pension.alchemy

import com.pension.alchemy.data.model.Asset
import com.pension.alchemy.data.model.AssetType
import com.pension.alchemy.data.model.Income
import com.pension.alchemy.data.model.IncomeType
import com.pension.alchemy.data.model.Pension
import com.pension.alchemy.data.model.PensionType
import com.pension.alchemy.domain.engine.RealTimeGrowthCalculator
import com.pension.alchemy.util.CurrencyFormatter
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RealTimeGrowthCalculatorTest {

    @Test
    fun testElapsedSecondsCalculation() {
        // 기준일 2026-09-01 00:00:00 -> 2026-09-11 12:00:00 (10.5일 경과 = 10 * 86400 + 43200 = 907200초)
        val baseDateStr = "2026-09-01"
        val currentDateTime = LocalDateTime.of(2026, 9, 11, 12, 0, 0)
        val elapsed = RealTimeGrowthCalculator.calculateElapsedSeconds(baseDateStr, currentDateTime)
        assertEquals(907200L, elapsed)

        // 미래 날짜일 경우 0초로 음수 방지 처리
        val futureDateStr = "2026-09-20"
        val futureElapsed = RealTimeGrowthCalculator.calculateElapsedSeconds(futureDateStr, currentDateTime)
        assertEquals(0L, futureElapsed)

        // 잘못된 날짜 문자열인 경우 오늘(또는 유효 날짜)로 fallback 처리되어 크래시 없이 동작
        val invalidElapsed = RealTimeGrowthCalculator.calculateElapsedSeconds("invalid-date", currentDateTime)
        assertTrue(invalidElapsed >= 0L)
    }

    @Test
    fun testAssetGrowthCalculation() {
        val asset = Asset(
            id = "asset-1",
            name = "S&P 500 ETF",
            type = AssetType.ETF,
            currentValue = 100_000_000L, // 1억원
            expectedGrowthRate = 10.0,    // 연 10%
            baseDate = "2026-09-01"
        )

        // 2026-09-01 00:00:00 에서 100일 후
        val currentDateTime = LocalDateTime.of(2026, 9, 1, 0, 0, 0).plusDays(100)
        val detail = RealTimeGrowthCalculator.calculateAssetGrowth(asset, currentDateTime)

        // 연간 초수 = 365.25 * 86400 = 31,557,600초
        // 연간 수익금 = 10,000,000원 -> 초당 수익 = 약 0.31688원
        assertTrue(detail.wonPerSecond in 0.31..0.32)
        assertEquals(100L * 86400L, detail.elapsedSeconds)
        assertTrue("100일간 수익은 약 270만원 가량이어야 함 (실제: ${detail.accumulatedGrowth})", detail.accumulatedGrowth in 2_700_000L..2_800_000L)
        assertEquals(100_000_000L + detail.accumulatedGrowth, detail.realTimeValue)
    }

    @Test
    fun testLiabilityGrowthCalculation() {
        val debt = Asset(
            id = "debt-1",
            name = "주택담보대출",
            type = AssetType.DEBT,
            currentValue = 200_000_000L,
            expectedGrowthRate = 4.0, // 연 4% 이자
            baseDate = "2026-09-01"
        )

        val currentDateTime = LocalDateTime.of(2026, 9, 1, 0, 0, 0).plusDays(30)
        val detail = RealTimeGrowthCalculator.calculateAssetGrowth(debt, currentDateTime)

        // 부채는 원금 자체가 늘어나지 않음
        assertEquals(200_000_000L, detail.realTimeValue)
        assertTrue(detail.wonPerSecond > 0.0) // 초당 발생 이자율
    }

    @Test
    fun testPensionGrowthCalculation() {
        val personalPension = Pension(
            id = "pension-1",
            name = "개인연금저축펀드",
            type = PensionType.PERSONAL,
            currentBalance = 50_000_000L,
            expectedGrowthRate = 6.0,
            baseDate = "2026-09-01"
        )

        val currentDateTime = LocalDateTime.of(2026, 9, 1, 0, 0, 0).plusDays(50)
        val detail = RealTimeGrowthCalculator.calculatePensionGrowth(personalPension, currentDateTime)

        assertTrue(detail.wonPerSecond > 0.0)
        assertTrue(detail.accumulatedGrowth > 0L)
        assertEquals(50_000_000L + detail.accumulatedGrowth, detail.realTimeBalance)

        // 공적연금(국민연금)은 적립금이 없으므로 운용수익 초당 0원
        val nationalPension = Pension(
            id = "pension-2",
            name = "국민연금",
            type = PensionType.NATIONAL,
            currentBalance = 0L,
            expectedMonthlyAmount = 1_500_000L,
            expectedGrowthRate = 2.0
        )
        val nationalDetail = RealTimeGrowthCalculator.calculatePensionGrowth(nationalPension, currentDateTime)
        assertEquals(0.0, nationalDetail.wonPerSecond, 0.0001)
        assertEquals(0L, nationalDetail.accumulatedGrowth)
    }

    @Test
    fun testIncomeGrowthCalculation() {
        val salary = Income(
            id = "income-1",
            name = "근로소득",
            type = IncomeType.SALARY,
            monthlyAmount = 4_000_000L, // 연 48,000,000원
            baseDate = "2026-09-01"
        )

        val currentDateTime = LocalDateTime.of(2026, 9, 1, 0, 0, 0).plusDays(10)
        val detail = RealTimeGrowthCalculator.calculateIncomeGrowth(salary, currentDateTime)

        // 연 4800만원 / 31,557,600 = 초당 약 1.521원
        assertTrue(detail.wonPerSecond in 1.51..1.53)
        // 10일간 = 864,000초 * 1.521 = 약 131.4만원
        assertTrue(detail.accumulatedIncome in 1_300_000L..1_330_000L)
    }

    @Test
    fun testTotalRealTimeGrowthSummary() {
        val assets = listOf(
            Asset(id = "1", name = "주식", type = AssetType.STOCK, currentValue = 100_000_000L, expectedGrowthRate = 6.0, baseDate = "2026-09-01"),
            Asset(id = "2", name = "대출", type = AssetType.DEBT, currentValue = 50_000_000L, expectedGrowthRate = 4.0, baseDate = "2026-09-01")
        )
        val pensions = listOf(
            Pension(id = "p1", name = "IRP", type = PensionType.RETIREMENT, currentBalance = 30_000_000L, expectedGrowthRate = 5.0, baseDate = "2026-09-01")
        )

        val currentDateTime = LocalDateTime.of(2026, 9, 1, 0, 0, 0).plusDays(30)
        val summary = RealTimeGrowthCalculator.calculateTotalRealTimeGrowth(assets, pensions, emptyList(), currentDateTime)

        // 총자산 = 주식(1억) + IRP(3천만) = 1억 3천만원
        assertEquals(130_000_000L, summary.baseTotalGrossAssets)
        // 총부채 = 대출 5천만원
        assertEquals(50_000_000L, summary.baseTotalDebt)
        // 순자산 = 8천만원
        assertEquals(80_000_000L, summary.baseNetWorth)

        // 30일 경과 후 실시간 자산 및 순자산은 증가
        assertTrue(summary.realTimeTotalGrossAssets > summary.baseTotalGrossAssets)
        assertTrue(summary.realTimeNetWorth > summary.baseNetWorth)
        assertTrue(summary.netWonPerSecond > 0.0)
    }

    @Test
    fun testComprehensiveTotalRealTimeGrowthWithIncomeAndExpenses() {
        val assets = listOf(
            Asset(id = "1", name = "예금", type = AssetType.DEPOSIT, currentValue = 100_000_000L, expectedGrowthRate = 3.0, baseDate = "2026-09-01")
        )
        val incomes = listOf(
            Income(id = "i1", name = "급여", monthlyAmount = 3_000_000L, endAge = 60, baseDate = "2026-09-01")
        )
        val monthlyExpenses = 2_000_000L // 잉여 = 월 100만원 저축
        val currentDateTime = LocalDateTime.of(2026, 9, 1, 0, 0, 0).plusDays(10)

        val summary = RealTimeGrowthCalculator.calculateTotalRealTimeGrowth(
            assets = assets,
            pensions = emptyList(),
            incomes = incomes,
            monthlyExpenses = monthlyExpenses,
            currentAge = 40,
            currentDateTime = currentDateTime
        )

        // 자산 수익: 1억 * 3% / 365.25 / 86400 = 약 0.095원/초
        // 소득 유입: 300만 * 12 / 365.25 / 86400 = 약 1.141원/초
        // 생활비 유출: 200만 * 12 / 365.25 / 86400 = 약 0.760원/초
        // 종합 순증가 속도 = 약 0.095 + 1.141 - 0.760 = 약 0.476원/초
        assertTrue(summary.netWonPerSecond > 0.45 && summary.netWonPerSecond < 0.50)
        assertTrue(summary.totalNetGain > 0L)
        assertTrue(summary.incomeAccumulatedGain > 0L)
        assertTrue(summary.livingExpenseAccumulated > 0L)
    }

    @Test
    fun testCurrencyFormatterSplitManAndWon() {
        // 5억 2000만 3456원 -> manPart: "5억 2,000만", wonPart: "3,456원"
        val split1 = CurrencyFormatter.splitManAndWon(520_003_456L, alwaysIncludeWon = false)
        assertEquals("5억 2,000만", split1.manPart)
        assertEquals("3,456원", split1.wonPart)

        // 85,000,000원 -> manPart: "8,500만원", wonPart: "" (alwaysIncludeWon=false)
        val split2 = CurrencyFormatter.splitManAndWon(85_000_000L, alwaysIncludeWon = false)
        assertEquals("8,500만원", split2.manPart)
        assertEquals("", split2.wonPart)

        // 85,000,000원 -> manPart: "8,500만", wonPart: "0원" (alwaysIncludeWon=true)
        val split3 = CurrencyFormatter.splitManAndWon(85_000_000L, alwaysIncludeWon = true)
        assertEquals("8,500만", split3.manPart)
        assertEquals("0원", split3.wonPart)

        // 7,890원 (1만원 미만) -> manPart: "0만", wonPart: "7,890원"
        val split4 = CurrencyFormatter.splitManAndWon(7_890L, alwaysIncludeWon = true)
        assertEquals("0만", split4.manPart)
        assertEquals("7,890원", split4.wonPart)

        // 0원 -> manPart: "0만", wonPart: "0원" (alwaysIncludeWon=true)
        val split5 = CurrencyFormatter.splitManAndWon(0L, alwaysIncludeWon = true)
        assertEquals("0만", split5.manPart)
        assertEquals("0원", split5.wonPart)
    }

    @Test
    fun testFormatKoreanWonFullUnits() {
        // 35억 6527만 6711원 (스크린샷 피드백 기준 금액)
        assertEquals("35억 6,527만 6,711원", CurrencyFormatter.formatKoreanWon(3_565_276_711L))

        // 100억 단위 (128억 4500만 1234원)
        assertEquals("128억 4,500만 1,234원", CurrencyFormatter.formatKoreanWon(12_845_001_234L))

        // 100억원 정액
        assertEquals("100억원", CurrencyFormatter.formatKoreanWon(10_000_000_000L))

        // 35억 6527만원 (원 단위 0)
        assertEquals("35억 6,527만원", CurrencyFormatter.formatKoreanWon(3_565_270_000L))

        // 27만 6711원 (자산 증가 표현 금액)
        assertEquals("27만 6,711원", CurrencyFormatter.formatKoreanWon(276_711L))

        // 27만원
        assertEquals("27만원", CurrencyFormatter.formatKoreanWon(270_000L))

        // 6711원
        assertEquals("6,711원", CurrencyFormatter.formatKoreanWon(6_711L))

        // 0원
        assertEquals("0원", CurrencyFormatter.formatKoreanWon(0L))
    }
}
