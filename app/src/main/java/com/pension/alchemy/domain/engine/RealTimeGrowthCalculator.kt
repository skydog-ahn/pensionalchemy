package com.pension.alchemy.domain.engine

import com.pension.alchemy.data.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

/**
 * 자산 및 연금/소득의 기준일(입력일자) 기반 실시간 초당 증가 연산 엔진
 */
object RealTimeGrowthCalculator {

    const val SECONDS_PER_YEAR = 365.25 * 86400.0

    /**
     * 문자열 날짜(YYYY-MM-DD)를 안전하게 LocalDate로 파싱
     */
    fun parseDateSafely(dateStr: String): LocalDate {
        return try {
            if (dateStr.isNotBlank()) LocalDate.parse(dateStr.trim()) else LocalDate.now()
        } catch (_: Exception) {
            LocalDate.now()
        }
    }

    /**
     * 기준일 00:00:00부터 기준 시각(기본값: 현재)까지 경과된 초(Seconds) 산출
     */
    fun calculateElapsedSeconds(
        baseDateStr: String,
        currentDateTime: LocalDateTime = LocalDateTime.now()
    ): Long {
        val baseDate = parseDateSafely(baseDateStr)
        val baseDateTime = baseDate.atStartOfDay()
        return ChronoUnit.SECONDS.between(baseDateTime, currentDateTime).coerceAtLeast(0L)
    }

    /**
     * 개별 자산 실시간 증가 연산 결과
     */
    data class AssetGrowthDetail(
        val assetId: String,
        val baseValue: Long,
        val expectedGrowthRate: Double,
        val isLiability: Boolean,
        val baseDate: String,
        val wonPerSecond: Double,
        val elapsedSeconds: Long,
        val accumulatedGrowth: Long,
        val realTimeValue: Long
    )

    /**
     * 개별 자산의 초당 증가율 및 기준일 이후 누적 증가액 산출
     */
    fun calculateAssetGrowth(
        asset: Asset,
        currentDateTime: LocalDateTime = LocalDateTime.now()
    ): AssetGrowthDetail {
        val elapsedSeconds = calculateElapsedSeconds(asset.effectiveBaseDate, currentDateTime)
        val wonPerSecond = (asset.currentValue * (asset.expectedGrowthRate / 100.0)) / SECONDS_PER_YEAR
        val accumulatedGrowth = (elapsedSeconds * wonPerSecond).roundToLong()
        
        // 자산이면 원금 + 누적수익, 부채이면 원금 그대로 (이자는 비용으로 별도 반영)
        val realTimeValue = if (!asset.isLiability) {
            asset.currentValue + accumulatedGrowth
        } else {
            asset.currentValue
        }

        return AssetGrowthDetail(
            assetId = asset.id,
            baseValue = asset.currentValue,
            expectedGrowthRate = asset.expectedGrowthRate,
            isLiability = asset.isLiability,
            baseDate = asset.effectiveBaseDate,
            wonPerSecond = wonPerSecond,
            elapsedSeconds = elapsedSeconds,
            accumulatedGrowth = accumulatedGrowth,
            realTimeValue = realTimeValue
        )
    }

    /**
     * 개별 연금 적립금 실시간 운용수익 연산 결과
     */
    data class PensionGrowthDetail(
        val pensionId: String,
        val baseBalance: Long,
        val expectedGrowthRate: Double,
        val baseDate: String,
        val wonPerSecond: Double,
        val elapsedSeconds: Long,
        val accumulatedGrowth: Long,
        val realTimeBalance: Long
    )

    /**
     * 개별 연금 적립금의 초당 운용수익 및 기준일 이후 누적 증가액 산출
     */
    fun calculatePensionGrowth(
        pension: Pension,
        currentDateTime: LocalDateTime = LocalDateTime.now()
    ): PensionGrowthDetail {
        val isFunded = pension.type != PensionType.NATIONAL && pension.type != PensionType.HOUSING
        val elapsedSeconds = calculateElapsedSeconds(pension.effectiveBaseDate, currentDateTime)
        val wonPerSecond = if (isFunded) {
            (pension.currentBalance * (pension.expectedGrowthRate / 100.0)) / SECONDS_PER_YEAR
        } else {
            0.0
        }
        val accumulatedGrowth = (elapsedSeconds * wonPerSecond).roundToLong()
        val realTimeBalance = pension.currentBalance + accumulatedGrowth

        return PensionGrowthDetail(
            pensionId = pension.id,
            baseBalance = pension.currentBalance,
            expectedGrowthRate = pension.expectedGrowthRate,
            baseDate = pension.effectiveBaseDate,
            wonPerSecond = wonPerSecond,
            elapsedSeconds = elapsedSeconds,
            accumulatedGrowth = accumulatedGrowth,
            realTimeBalance = realTimeBalance
        )
    }

    /**
     * 개별 정기 소득 실시간 누적 수입 연산 결과
     */
    data class IncomeGrowthDetail(
        val incomeId: String,
        val monthlyAmount: Long,
        val baseDate: String,
        val wonPerSecond: Double,
        val elapsedSeconds: Long,
        val accumulatedIncome: Long
    )

    /**
     * 정기 소득의 기준일 이후 실시간 누적 유입액 산출
     */
    fun calculateIncomeGrowth(
        income: Income,
        currentDateTime: LocalDateTime = LocalDateTime.now()
    ): IncomeGrowthDetail {
        val elapsedSeconds = calculateElapsedSeconds(income.effectiveBaseDate, currentDateTime)
        val wonPerSecond = (income.monthlyAmount * 12.0) / SECONDS_PER_YEAR
        val accumulatedIncome = (elapsedSeconds * wonPerSecond).roundToLong()

        return IncomeGrowthDetail(
            incomeId = income.id,
            monthlyAmount = income.monthlyAmount,
            baseDate = income.effectiveBaseDate,
            wonPerSecond = wonPerSecond,
            elapsedSeconds = elapsedSeconds,
            accumulatedIncome = accumulatedIncome
        )
    }

    /**
     * 종합 가계 자산 및 순자산의 실시간 초당 증가 종합 지표
     */
    data class TotalRealTimeGrowthSummary(
        // 총자산
        val baseTotalGrossAssets: Long,
        val realTimeTotalGrossAssets: Long,
        val totalGrossAssetGain: Long,
        val grossAssetWonPerSecond: Double,

        // 총부채
        val baseTotalDebt: Long,
        val realTimeTotalDebt: Long,
        val debtInterestWonPerSecond: Double,

        // 순자산
        val baseNetWorth: Long,
        val realTimeNetWorth: Long,
        val totalNetGain: Long,
        val netWonPerSecond: Double,

        // 연금 적립금 합산
        val basePensionAssets: Long,
        val realTimePensionAssets: Long,
        val pensionGain: Long,
        val pensionWonPerSecond: Double,

        // 정기 소득 및 생활소비 세부
        val incomeAccumulatedGain: Long = 0L,
        val incomeWonPerSecond: Double = 0.0,
        val livingExpenseAccumulated: Long = 0L,
        val livingExpenseWonPerSecond: Double = 0.0
    )

    /**
     * 기존 호출 호환용 오버로드 (소비지출 미반영 단순 자산 집계)
     */
    fun calculateTotalRealTimeGrowth(
        assets: List<Asset>,
        pensions: List<Pension>,
        incomes: List<Income> = emptyList(),
        currentDateTime: LocalDateTime = LocalDateTime.now()
    ): TotalRealTimeGrowthSummary {
        return calculateTotalRealTimeGrowth(
            assets = assets,
            pensions = pensions,
            incomes = incomes,
            monthlyExpenses = 0L,
            currentAge = 0,
            currentDateTime = currentDateTime
        )
    }

    /**
     * 전체 자산, 연금, 소득, 소비, 부채의 개별 기준일 기준 실시간 종합 순자산 초당 증가 산출
     */
    fun calculateTotalRealTimeGrowth(
        assets: List<Asset>,
        pensions: List<Pension>,
        incomes: List<Income> = emptyList(),
        monthlyExpenses: Long = 0L,
        currentAge: Int = 0,
        currentDateTime: LocalDateTime = LocalDateTime.now()
    ): TotalRealTimeGrowthSummary {
        val assetDetails = assets.map { calculateAssetGrowth(it, currentDateTime) }
        val fundedPensions = pensions.filter { it.type != PensionType.NATIONAL && it.type != PensionType.HOUSING }
        val pensionDetails = fundedPensions.map { calculatePensionGrowth(it, currentDateTime) }
        val activeIncomes = if (currentAge > 0) incomes.filter { currentAge <= it.endAge } else incomes
        val incomeDetails = activeIncomes.map { calculateIncomeGrowth(it, currentDateTime) }

        val nonLiabilityDetails = assetDetails.filter { !it.isLiability }
        val debtDetails = assetDetails.filter { it.isLiability }

        // 1. 일반 자산 (금융 + 부동산)
        val baseAssetsValue = nonLiabilityDetails.sumOf { it.baseValue }
        val assetsAccumulatedGain = nonLiabilityDetails.sumOf { it.accumulatedGrowth }
        val assetsWonPerSecond = nonLiabilityDetails.sumOf { it.wonPerSecond }

        // 2. 연금 적립금 자산
        val basePensionValue = pensionDetails.sumOf { it.baseBalance }
        val pensionAccumulatedGain = pensionDetails.sumOf { it.accumulatedGrowth }
        val pensionWonPerSecond = pensionDetails.sumOf { it.wonPerSecond }

        // 3. 정기 소득 유입 (월급/사업소득)
        val incomeAccumulatedGain = incomeDetails.sumOf { it.accumulatedIncome }
        val incomeWonPerSecond = incomeDetails.sumOf { it.wonPerSecond }

        // 4. 총부채 및 이자비용
        val baseTotalDebt = debtDetails.sumOf { it.baseValue }
        val debtInterestWonPerSecond = debtDetails.sumOf { it.wonPerSecond }
        val debtAccumulatedInterest = debtDetails.sumOf { (it.elapsedSeconds * it.wonPerSecond).roundToLong() }
        val realTimeTotalDebt = baseTotalDebt

        // 5. 생활비 소비 지출 (월간 생활비 기준일 경과 반영)
        val livingExpenseWonPerSecond = if (monthlyExpenses > 0L) (monthlyExpenses * 12.0) / SECONDS_PER_YEAR else 0.0
        val expenseBaseDate = if (monthlyExpenses > 0L) {
            activeIncomes.map { parseDateSafely(it.effectiveBaseDate) }.minOrNull()
                ?: nonLiabilityDetails.map { parseDateSafely(it.baseDate) }.minOrNull()
                ?: LocalDate.now()
        } else {
            LocalDate.now()
        }
        val expenseElapsedSeconds = ChronoUnit.SECONDS.between(expenseBaseDate.atStartOfDay(), currentDateTime).coerceAtLeast(0L)
        val livingExpenseAccumulated = if (monthlyExpenses > 0L) (expenseElapsedSeconds * livingExpenseWonPerSecond).roundToLong() else 0L

        // 6. 사적연금 급여 차감 직접 납입 지출
        val deductedPensions = if (currentAge > 0) {
            fundedPensions.filter { currentAge <= it.contributionEndAge && it.isDeductedFromIncome }
        } else {
            fundedPensions.filter { it.isDeductedFromIncome }
        }
        val pensionContribWonPerSecond = deductedPensions.sumOf { (it.monthlyContribution * 12.0) / SECONDS_PER_YEAR }
        val pensionContribAccumulated = deductedPensions.sumOf {
            (calculateElapsedSeconds(it.effectiveBaseDate, currentDateTime) * ((it.monthlyContribution * 12.0) / SECONDS_PER_YEAR)).roundToLong()
        }

        // 7. 가계 순 잉여 현금흐름 (소득 - 생활비 - 연금납입)
        val isComprehensiveHousehold = monthlyExpenses > 0L || currentAge > 0
        val netCashFlowWonPerSecond = if (isComprehensiveHousehold) {
            incomeWonPerSecond - livingExpenseWonPerSecond - pensionContribWonPerSecond
        } else {
            0.0
        }
        val netCashFlowAccumulated = if (isComprehensiveHousehold) {
            incomeAccumulatedGain - livingExpenseAccumulated - pensionContribAccumulated
        } else {
            0L
        }

        // 8. 총자산 = 일반 자산 + 연금 적립금 + 가계 실시간 잉여저축 (회계적 항등식: 자산 = 부채 + 순자산)
        val baseTotalGrossAssets = baseAssetsValue + basePensionValue
        val totalGrossAssetGain = assetsAccumulatedGain + pensionAccumulatedGain + netCashFlowAccumulated
        val realTimeTotalGrossAssets = (baseTotalGrossAssets + totalGrossAssetGain).coerceAtLeast(0L)
        val grossAssetWonPerSecond = assetsWonPerSecond + pensionWonPerSecond + netCashFlowWonPerSecond

        // 9. 순자산 (Net Worth = 총자산 - 총부채, 부채 0원 시 총자산과 100% 일치)
        val totalNetGain = totalGrossAssetGain - debtAccumulatedInterest
        val netWonPerSecond = grossAssetWonPerSecond - debtInterestWonPerSecond

        val baseNetWorth = (baseTotalGrossAssets - baseTotalDebt).coerceAtLeast(0L)
        val realTimeNetWorth = (realTimeTotalGrossAssets - realTimeTotalDebt).coerceAtLeast(0L)

        return TotalRealTimeGrowthSummary(
            baseTotalGrossAssets = baseTotalGrossAssets,
            realTimeTotalGrossAssets = realTimeTotalGrossAssets,
            totalGrossAssetGain = totalGrossAssetGain,
            grossAssetWonPerSecond = grossAssetWonPerSecond,
            baseTotalDebt = baseTotalDebt,
            realTimeTotalDebt = realTimeTotalDebt,
            debtInterestWonPerSecond = debtInterestWonPerSecond,
            baseNetWorth = baseNetWorth,
            realTimeNetWorth = realTimeNetWorth,
            totalNetGain = totalNetGain,
            netWonPerSecond = netWonPerSecond,
            basePensionAssets = basePensionValue,
            realTimePensionAssets = basePensionValue + pensionAccumulatedGain,
            pensionGain = pensionAccumulatedGain,
            pensionWonPerSecond = pensionWonPerSecond,
            incomeAccumulatedGain = incomeAccumulatedGain,
            incomeWonPerSecond = incomeWonPerSecond,
            livingExpenseAccumulated = livingExpenseAccumulated,
            livingExpenseWonPerSecond = livingExpenseWonPerSecond
        )
    }
}
