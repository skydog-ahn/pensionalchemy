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

    @Test
    fun testGlobalBaseDateTimeEarlierThanItemBaseDate() {
        // 전체 공통 기준일이 항목 기준일보다 빠른 경우:
        // 전체 기준일: 2026-05-01, 자산 등록일: 2026-07-01, 현재 시점: 2026-09-01
        // 항목 기준일(7월 1일)부터 현재까지 계산된 것이 총자산 타이머로 계산됨
        val globalBase = "2026-05-01 00:00:00"
        val asset = Asset(
            id = "a1",
            name = "성장주",
            type = AssetType.STOCK,
            currentValue = 100_000_000L,
            expectedGrowthRate = 10.0,
            baseDate = "2026-07-01"
        )
        val currentDateTime = LocalDateTime.of(2026, 9, 1, 0, 0, 0)

        val summary = RealTimeGrowthCalculator.calculateTotalRealTimeGrowth(
            assets = listOf(asset),
            pensions = emptyList(),
            currentDateTime = currentDateTime,
            globalBaseDateTime = globalBase
        )

        // 전체 기준일 시점의 기초자산은 원금 1억원
        assertEquals(100_000_000L, summary.baseTotalGrossAssets)

        // 7월 1일부터 9월 1일까지(62일간)의 초당 증가가 누적 증가액으로 계산
        val expectedWonPerSec = (100_000_000L * 0.10) / RealTimeGrowthCalculator.SECONDS_PER_YEAR
        val expectedGain = (62L * 86400L * expectedWonPerSec).toLong()
        assertTrue("누적 증가액은 약 170만원 가량이어야 함", summary.totalGrossAssetGain in (expectedGain - 50_000L)..(expectedGain + 50_000L))
        assertEquals(summary.baseTotalGrossAssets + summary.totalGrossAssetGain, summary.realTimeTotalGrossAssets)

        // 개별 자산의 실시간 가치와 총자산 실시간 가치가 정확히 일치
        val assetDetail = RealTimeGrowthCalculator.calculateAssetGrowth(asset, currentDateTime)
        assertEquals(assetDetail.realTimeValue, summary.realTimeTotalGrossAssets)
    }

    @Test
    fun testGlobalBaseDateTimeLaterThanItemBaseDate() {
        // 항목 기준일이 전체 공통 기준일보다 빠른 경우:
        // 자산 등록일: 2026-05-01, 전체 기준일: 2026-07-01, 현재 시점: 2026-09-01
        // 자산 기준일부터 전체 기준일까지의 계산값이 기초자산에 추가되고, 전체 기준일부터 현재 일시까지 맞춰 계산됨
        val globalBase = "2026-07-01 00:00:00"
        val asset = Asset(
            id = "a1",
            name = "배당주",
            type = AssetType.STOCK,
            currentValue = 100_000_000L,
            expectedGrowthRate = 10.0,
            baseDate = "2026-05-01"
        )
        val currentDateTime = LocalDateTime.of(2026, 9, 1, 0, 0, 0)

        val summary = RealTimeGrowthCalculator.calculateTotalRealTimeGrowth(
            assets = listOf(asset),
            pensions = emptyList(),
            currentDateTime = currentDateTime,
            globalBaseDateTime = globalBase
        )

        val wonPerSec = (100_000_000L * 0.10) / RealTimeGrowthCalculator.SECONDS_PER_YEAR
        val preGain = (61L * 86400L * wonPerSec).toLong() // 5월 1일 ~ 7월 1일 (61일)
        val postGain = (62L * 86400L * wonPerSec).toLong() // 7월 1일 ~ 9월 1일 (62일)

        // 전체 기준일(7월 1일) 시점의 기초자산에는 5월~7월 증가분이 추가됨
        assertTrue("기초자산에 기준일까지의 증가분이 가산되어야 함", summary.baseTotalGrossAssets > 100_000_000L)
        assertTrue(summary.baseTotalGrossAssets in (100_000_000L + preGain - 50_000L)..(100_000_000L + preGain + 50_000L))

        // 전체 기준일 대비 증가는 7월 1일 ~ 9월 1일(62일)분
        assertTrue(summary.totalGrossAssetGain in (postGain - 50_000L)..(postGain + 50_000L))

        // 현재 일시까지 맞춘 실시간 총자산 = 개별 자산의 5월~9월 총 실시간 가치와 완벽 일치!
        val assetDetail = RealTimeGrowthCalculator.calculateAssetGrowth(asset, currentDateTime)
        assertEquals(assetDetail.realTimeValue, summary.realTimeTotalGrossAssets)
        assertEquals(summary.baseTotalGrossAssets + summary.totalGrossAssetGain, summary.realTimeTotalGrossAssets)
    }

    @Test
    fun testMixedItemBaseDatesWithGlobalBaseDateTime() {
        // 빠른 항목(자산 A: 2026-03-01)과 늦은 항목(연금 B: 2026-08-01)이 혼합된 경우
        // 전체 기준일: 2026-06-01, 현재 시점: 2026-10-01
        val globalBase = "2026-06-01 00:00:00"
        val asset = Asset(
            id = "a1",
            name = "글로벌 ETF",
            type = AssetType.ETF,
            currentValue = 200_000_000L,
            expectedGrowthRate = 6.0,
            baseDate = "2026-03-01" // 전체 기준일보다 빠름
        )
        val pension = Pension(
            id = "p1",
            name = "개인연금",
            type = PensionType.PERSONAL,
            currentBalance = 50_000_000L,
            expectedGrowthRate = 5.0,
            baseDate = "2026-08-01" // 전체 기준일보다 늦음
        )
        val currentDateTime = LocalDateTime.of(2026, 10, 1, 0, 0, 0)

        val summary = RealTimeGrowthCalculator.calculateTotalRealTimeGrowth(
            assets = listOf(asset),
            pensions = listOf(pension),
            currentDateTime = currentDateTime,
            globalBaseDateTime = globalBase
        )

        // 회계적 항등식 검증: Base + Gain == RealTime
        assertEquals(summary.baseTotalGrossAssets + summary.totalGrossAssetGain, summary.realTimeTotalGrossAssets)

        // 개별 항목 실시간 가치의 합 == 총자산 실시간 가치
        val assetDetail = RealTimeGrowthCalculator.calculateAssetGrowth(asset, currentDateTime)
        val pensionDetail = RealTimeGrowthCalculator.calculatePensionGrowth(pension, currentDateTime)
        val expectedRealTimeSum = assetDetail.realTimeValue + pensionDetail.realTimeBalance
        assertEquals(expectedRealTimeSum, summary.realTimeTotalGrossAssets)
    }

    @Test
    fun testFormatAsBaseDateLabel() {
        // yyyy-MM-dd HH:mm 형식의 일시 문자열이 "yyyy.MM.dd 기준"으로만 변환되는지 검증
        assertEquals("2026.10.02 기준", RealTimeGrowthCalculator.formatAsBaseDateLabel("2026-10-02 22:40"))
        assertEquals("2026.10.02 기준", RealTimeGrowthCalculator.formatAsBaseDateLabel("2026-10-02 22:40:55"))
        assertEquals("2026.10.02 기준", RealTimeGrowthCalculator.formatAsBaseDateLabel("2026-10-02"))

        // null 또는 빈 문자열인 경우 fallback
        assertEquals("기준일 대비", RealTimeGrowthCalculator.formatAsBaseDateLabel(""))
        assertEquals("기준일 대비", RealTimeGrowthCalculator.formatAsBaseDateLabel(null))

        // formatAsDotDate 단독 검증
        assertEquals("2026.10.02", RealTimeGrowthCalculator.formatAsDotDate("2026-10-02 22:40"))
        assertEquals("2026.05.15", RealTimeGrowthCalculator.formatAsDotDate("2026-05-15"))
        assertEquals("", RealTimeGrowthCalculator.formatAsDotDate(null))
        assertEquals("", RealTimeGrowthCalculator.formatAsDotDate(""))
    }

    @Test
    fun testPensionContributionPreservedInNetWorth() {
        // 급여 650만원, 생활비 400만원, 사적연금 납입 150만원(급여차감)인 가계
        // 연금 납입은 소비가 아니라 계좌간 이체(저축)이므로, 순자산 관점에서는 (소득 - 생활비) = 250만원이 온전히 보존되어야 함.
        val pension = Pension(
            id = "p1",
            name = "개인연금",
            type = PensionType.PERSONAL,
            currentBalance = 10_000_000L,
            expectedGrowthRate = 0.0,
            monthlyContribution = 1_500_000L,
            contributionEndAge = 60,
            isDeductedFromIncome = true,
            baseDate = "2026-01-01"
        )
        val income = Income(
            id = "i1",
            name = "급여",
            type = com.pension.alchemy.data.model.IncomeType.SALARY,
            monthlyAmount = 6_500_000L,
            endAge = 60,
            baseDate = "2026-01-01"
        )
        val globalBase = "2026-01-01 00:00"
        val currentDateTime = LocalDateTime.of(2027, 1, 1, 0, 0, 0) // 정확히 1년 경과 (365.25일)

        val summary = RealTimeGrowthCalculator.calculateTotalRealTimeGrowth(
            assets = emptyList(),
            pensions = listOf(pension),
            incomes = listOf(income),
            monthlyExpenses = 4_000_000L,
            currentAge = 50,
            currentDateTime = currentDateTime,
            globalBaseDateTime = globalBase
        )

        // 1년간 연금 납입액: 1,500,000 * 12 = 18,000,000원
        // 연금 적립금 자산 잔고: 기존 1000만원 + 1년간 납입 1800만원 = 2800만원
        val expectedPensionBal = 28_000_000L
        assertTrue("연금 잔고는 납입금이 적립되어 2800만원이어야 함", summary.realTimePensionAssets in (expectedPensionBal - 50_000L)..(expectedPensionBal + 50_000L))

        // 회계적 항등식 100% 만족
        assertEquals(summary.baseTotalGrossAssets + summary.totalGrossAssetGain, summary.realTimeTotalGrossAssets)
        assertEquals(summary.baseNetWorth + summary.totalNetGain, summary.realTimeNetWorth)
    }

    @Test
    fun testCurrentNetWorthIsIdenticalRegardlessOfGlobalBaseDate() {
        // [사용자 핵심 질문 검증]:
        // 전체기준일을 2026년 1월 1일로 하였을 때 현재 순자산 값과, 전체기준일을 현재 시간으로 하였을 때 현재 순자산 값은
        // 1원 단위까지 100% 완벽하게 동일해야 함!
        val asset1 = Asset(
            id = "a1",
            name = "국내주식",
            type = AssetType.STOCK,
            currentValue = 340_000_000L,
            expectedGrowthRate = 10.0,
            baseDate = "2026-09-01"
        )
        val asset2 = Asset(
            id = "a2",
            name = "현금성",
            type = AssetType.OTHER,
            currentValue = 750_000_000L,
            expectedGrowthRate = 2.5,
            baseDate = "2026-01-01"
        )
        val pension1 = Pension(
            id = "p1",
            name = "퇴직연금",
            type = PensionType.RETIREMENT,
            currentBalance = 360_000_000L,
            expectedGrowthRate = 4.5,
            baseDate = "2025-09-30"
        )
        val income1 = Income(
            id = "i1",
            name = "급여",
            type = com.pension.alchemy.data.model.IncomeType.SALARY,
            monthlyAmount = 6_500_000L,
            baseDate = ""
        )

        val currentDateTime = LocalDateTime.of(2026, 10, 2, 23, 12, 0)

        // Case A: 전체기준일 = 2026-01-01
        val summaryA = RealTimeGrowthCalculator.calculateTotalRealTimeGrowth(
            assets = listOf(asset1, asset2),
            pensions = listOf(pension1),
            incomes = listOf(income1),
            monthlyExpenses = 4_000_000L,
            currentDateTime = currentDateTime,
            globalBaseDateTime = "2026-01-01 00:00"
        )

        // Case B: 전체기준일 = 현재 시간 (2026-10-02 23:12:00)
        val summaryB = RealTimeGrowthCalculator.calculateTotalRealTimeGrowth(
            assets = listOf(asset1, asset2),
            pensions = listOf(pension1),
            incomes = listOf(income1),
            monthlyExpenses = 4_000_000L,
            currentDateTime = currentDateTime,
            globalBaseDateTime = "2026-10-02 23:12"
        )

        // 검증 1: 두 경우의 현재 순자산(realTimeNetWorth)은 완벽히 동일해야 함!
        assertEquals("전체기준일이 1월 1일이든 현재시간이든 현재 순자산은 동일해야 함", summaryA.realTimeNetWorth, summaryB.realTimeNetWorth)
        assertEquals("현재 총자산도 완벽히 동일해야 함", summaryA.realTimeTotalGrossAssets, summaryB.realTimeTotalGrossAssets)

        // 검증 2: 전체기준일이 현재시간일 때 누적 증가액은 0원이어야 함
        assertEquals(0L, summaryB.totalNetGain)
        assertEquals(summaryB.realTimeNetWorth, summaryB.baseNetWorth)

        // 검증 3: 전체기준일이 1월 1일일 때 누적 증가액은 양수(성장분)이어야 함
        assertTrue(summaryA.totalNetGain > 0L)
        assertEquals(summaryA.baseNetWorth + summaryA.totalNetGain, summaryA.realTimeNetWorth)
    }
}
