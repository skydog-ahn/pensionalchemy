package com.pension.alchemy.domain.engine

import com.pension.alchemy.data.model.*
import com.pension.alchemy.util.LoanCalculator
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
     * 문자열 날짜(YYYY-MM-DD or YYYY-MM-DD HH:mm:ss or YYYY-MM-DDTHH:mm:ss)를 안전하게 LocalDate로 파싱
     */
    fun parseDateSafely(dateStr: String): LocalDate {
        return parseDateTimeSafely(dateStr).toLocalDate()
    }

    /**
     * 문자열 날짜/시간을 안전하게 LocalDateTime으로 파싱
     */
    fun parseDateTimeSafely(dateTimeStr: String): LocalDateTime {
        if (dateTimeStr.isBlank()) return LocalDate.now().atStartOfDay()
        val trimmed = dateTimeStr.trim()
        return try {
            when {
                trimmed.contains("T") -> LocalDateTime.parse(trimmed)
                trimmed.contains(" ") -> {
                    val parts = trimmed.split(" ")
                    val date = LocalDate.parse(parts[0])
                    val timeParts = parts[1].split(":")
                    val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
                    val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                    val second = timeParts.getOrNull(2)?.toIntOrNull() ?: 0
                    date.atTime(hour, minute, second)
                }
                else -> LocalDate.parse(trimmed).atStartOfDay()
            }
        } catch (_: Exception) {
            LocalDate.now().atStartOfDay()
        }
    }

    /**
     * 기준일시부터 기준 시각(기본값: 현재)까지 경과된 초(Seconds) 산출
     */
    fun calculateElapsedSeconds(
        baseDateStr: String,
        currentDateTime: LocalDateTime = LocalDateTime.now()
    ): Long {
        val baseDateTime = parseDateTimeSafely(baseDateStr)
        return ChronoUnit.SECONDS.between(baseDateTime, currentDateTime).coerceAtLeast(0L)
    }

    /**
     * 날짜/일시 문자열(yyyy-MM-dd 또는 yyyy-MM-dd HH:mm 등)을 "yyyy.MM.dd" 형태의 날짜 점(dot) 표기로 변환
     */
    fun formatAsDotDate(dateTimeStr: String?): String {
        if (dateTimeStr.isNullOrBlank()) return ""
        return try {
            val date = parseDateTimeSafely(dateTimeStr).toLocalDate()
            date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))
        } catch (_: Exception) {
            dateTimeStr.take(10).replace("-", ".")
        }
    }

    /**
     * 공통 기준일 표기를 "yyyy.MM.dd 기준" 형태로 변환
     */
    fun formatAsBaseDateLabel(dateTimeStr: String?): String {
        return if (!dateTimeStr.isNullOrBlank()) {
            "${formatAsDotDate(dateTimeStr)} 기준"
        } else {
            "기준일 대비"
        }
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
     * 부채 종류(상환방식)에 따른 연간 원금 상환액 산출
     */
    fun calculateAnnualPrincipalRepayment(debt: Asset): Double {
        if (!debt.isLiability || debt.currentValue <= 0L) return 0.0
        val maturityYears = debt.maturityYears.coerceAtLeast(1)
        return when (debt.repaymentMethod) {
            RepaymentMethod.EQUAL_PRINCIPAL -> {
                debt.currentValue.toDouble() / maturityYears.toDouble()
            }
            RepaymentMethod.EQUAL_PRINCIPAL_AND_INTEREST -> {
                val monthlyPayment = LoanCalculator.calculateMonthlyPayment(
                    debt.currentValue,
                    debt.expectedGrowthRate,
                    maturityYears,
                    debt.repaymentMethod
                )
                val annualPayment = monthlyPayment * 12.0
                val annualInterest = debt.currentValue * (debt.expectedGrowthRate / 100.0)
                maxOf(0.0, annualPayment - annualInterest)
            }
            RepaymentMethod.BULLET, RepaymentMethod.INTEREST_ONLY -> 0.0
        }
    }

    /**
     * 개별 자산의 초당 증가율 및 기준일 이후 누적 증가액 산출
     */
    fun calculateAssetGrowth(
        asset: Asset,
        currentDateTime: LocalDateTime = LocalDateTime.now()
    ): AssetGrowthDetail {
        val elapsedSeconds = calculateElapsedSeconds(asset.effectiveBaseDate, currentDateTime)

        if (!asset.isLiability) {
            val wonPerSecond = (asset.currentValue * (asset.expectedGrowthRate / 100.0)) / SECONDS_PER_YEAR
            val accumulatedGrowth = (elapsedSeconds * wonPerSecond).roundToLong()
            val realTimeValue = asset.currentValue + accumulatedGrowth
            return AssetGrowthDetail(
                assetId = asset.id,
                baseValue = asset.currentValue,
                expectedGrowthRate = asset.expectedGrowthRate,
                isLiability = false,
                baseDate = asset.effectiveBaseDate,
                wonPerSecond = wonPerSecond,
                elapsedSeconds = elapsedSeconds,
                accumulatedGrowth = accumulatedGrowth,
                realTimeValue = realTimeValue
            )
        } else {
            // 부채의 경우: 부채 종류(상환방식)에 따른 실시간 원금 상환 및 기준일 상환액 반영
            val interestWonPerSecond = (asset.currentValue * (asset.expectedGrowthRate / 100.0)) / SECONDS_PER_YEAR
            val annualPrincipal = calculateAnnualPrincipalRepayment(asset)
            val principalWonPerSecond = annualPrincipal / SECONDS_PER_YEAR
            val repaidPrincipal = (elapsedSeconds * principalWonPerSecond).roundToLong().coerceIn(0L, asset.currentValue)
            val realTimeValue = (asset.currentValue - repaidPrincipal).coerceAtLeast(0L)

            return AssetGrowthDetail(
                assetId = asset.id,
                baseValue = asset.currentValue,
                expectedGrowthRate = asset.expectedGrowthRate,
                isLiability = true,
                baseDate = asset.effectiveBaseDate,
                wonPerSecond = interestWonPerSecond,
                elapsedSeconds = elapsedSeconds,
                accumulatedGrowth = repaidPrincipal,
                realTimeValue = realTimeValue
            )
        }
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
        val debtPrincipalWonPerSecond: Double = 0.0,
        val debtPrincipalRepaidAccumulated: Long = 0L,
        val debtInterestAccumulated: Long = 0L,
        val debtRepaymentAccumulated: Long = 0L,

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
        val livingExpenseWonPerSecond: Double = 0.0,

        // 전체 공통 기준일시
        val globalBaseDateTime: String = ""
    )

    /**
     * 기존 호출 호환용 오버로드 (소비지출 미반영 단순 자산 집계)
     */
    fun calculateTotalRealTimeGrowth(
        assets: List<Asset>,
        pensions: List<Pension>,
        incomes: List<Income> = emptyList(),
        currentDateTime: LocalDateTime = LocalDateTime.now(),
        globalBaseDateTime: String = ""
    ): TotalRealTimeGrowthSummary {
        return calculateTotalRealTimeGrowth(
            assets = assets,
            pensions = pensions,
            incomes = incomes,
            monthlyExpenses = 0L,
            currentAge = 0,
            currentDateTime = currentDateTime,
            globalBaseDateTime = globalBaseDateTime
        )
    }

    /**
     * 전체 자산, 연금, 소득, 소비, 부채의 개별 기준일 및 전체 공통 기준일 기반 실시간 종합 순자산 초당 증가 산출
     *
     * [연산 구조 및 의미]
     * - 각 항목의 기준일이 전체 기준일보다 빠른 경우:
     *   항목 기준일부터 전체 기준일까지의 계산값을 기초 자산에 추가하고, 전체 기준일부터 현재 일시까지 맞춰 계산
     * - 각 항목의 기준일이 전체 기준일보다 늦는 경우:
     *   각 항목의 기준일부터 계산된 것이 총자산 및 실시간 시간 타이머로 계산
     */
    fun calculateTotalRealTimeGrowth(
        assets: List<Asset>,
        pensions: List<Pension>,
        incomes: List<Income> = emptyList(),
        monthlyExpenses: Long = 0L,
        currentAge: Int = 0,
        currentDateTime: LocalDateTime = LocalDateTime.now(),
        globalBaseDateTime: String = ""
    ): TotalRealTimeGrowthSummary {
        val hasGlobalBaseDate = globalBaseDateTime.isNotBlank()
        val globalTargetDateTime = if (hasGlobalBaseDate) {
            val raw = parseDateTimeSafely(globalBaseDateTime)
            if (raw.isAfter(currentDateTime)) currentDateTime else raw
        } else {
            currentDateTime
        }

        val fundedPensions = pensions.filter { it.type != PensionType.NATIONAL && it.type != PensionType.HOUSING }
        val activeIncomes = if (currentAge > 0) incomes.filter { currentAge <= it.endAge } else incomes

        val nonLiabilityAssets = assets.filter { !it.isLiability }
        val debtAssets = assets.filter { it.isLiability }

        // 1. 일반 자산 (금융 + 부동산)
        var baseAssetsValue = 0L
        var realTimeAssetsValue = 0L
        var assetsWonPerSecond = 0.0

        for (asset in nonLiabilityAssets) {
            val assetEffectiveBase = if (asset.baseDate.isNotBlank()) {
                asset.baseDate
            } else if (hasGlobalBaseDate) {
                globalBaseDateTime
            } else {
                asset.effectiveBaseDate
            }
            val itemDateTimeRaw = parseDateTimeSafely(assetEffectiveBase)
            val itemDateTime = if (itemDateTimeRaw.isAfter(currentDateTime)) currentDateTime else itemDateTimeRaw
            val wonPerSec = (asset.currentValue * (asset.expectedGrowthRate / 100.0)) / SECONDS_PER_YEAR
            assetsWonPerSecond += wonPerSec

            if (hasGlobalBaseDate) {
                if (!itemDateTime.isAfter(globalTargetDateTime)) {
                    // 항목 기준일 <= 전체 기준일: 전체 기준일까지의 성장분을 기초값에 추가하고, 전체 기준일부터 현재까지 계산
                    val preSeconds = ChronoUnit.SECONDS.between(itemDateTime, globalTargetDateTime).coerceAtLeast(0L)
                    val preGain = (preSeconds * wonPerSec).roundToLong()
                    val baseValAtGlobal = asset.currentValue + preGain

                    val postSeconds = ChronoUnit.SECONDS.between(globalTargetDateTime, currentDateTime).coerceAtLeast(0L)
                    val postGain = (postSeconds * wonPerSec).roundToLong()

                    baseAssetsValue += baseValAtGlobal
                    realTimeAssetsValue += (baseValAtGlobal + postGain)
                } else {
                    // 항목 기준일 > 전체 기준일: 항목 기준일부터 계산된 것이 총자산 타이머로 계산
                    val baseValAtGlobal = asset.currentValue
                    val itemSeconds = ChronoUnit.SECONDS.between(itemDateTime, currentDateTime).coerceAtLeast(0L)
                    val itemGain = (itemSeconds * wonPerSec).roundToLong()

                    baseAssetsValue += baseValAtGlobal
                    realTimeAssetsValue += (baseValAtGlobal + itemGain)
                }
            } else {
                val baseVal = asset.currentValue
                val elapsedSeconds = ChronoUnit.SECONDS.between(itemDateTime, currentDateTime).coerceAtLeast(0L)
                val gain = (elapsedSeconds * wonPerSec).roundToLong()

                baseAssetsValue += baseVal
                realTimeAssetsValue += (baseVal + gain)
            }
        }
        val assetsAccumulatedGain = realTimeAssetsValue - baseAssetsValue

        // 2. 연금 적립금 자산
        var basePensionValue = 0L
        var realTimePensionAssets = 0L
        var pensionWonPerSecond = 0.0

        for (pension in fundedPensions) {
            val pensionEffectiveBase = if (pension.baseDate.isNotBlank()) {
                pension.baseDate
            } else if (hasGlobalBaseDate) {
                globalBaseDateTime
            } else {
                pension.effectiveBaseDate
            }
            val itemDateTimeRaw = parseDateTimeSafely(pensionEffectiveBase)
            val itemDateTime = if (itemDateTimeRaw.isAfter(currentDateTime)) currentDateTime else itemDateTimeRaw
            val wonPerSec = (pension.currentBalance * (pension.expectedGrowthRate / 100.0)) / SECONDS_PER_YEAR
            pensionWonPerSecond += wonPerSec

            if (hasGlobalBaseDate) {
                if (!itemDateTime.isAfter(globalTargetDateTime)) {
                    val preSeconds = ChronoUnit.SECONDS.between(itemDateTime, globalTargetDateTime).coerceAtLeast(0L)
                    val preGain = (preSeconds * wonPerSec).roundToLong()
                    val baseBalAtGlobal = pension.currentBalance + preGain

                    val postSeconds = ChronoUnit.SECONDS.between(globalTargetDateTime, currentDateTime).coerceAtLeast(0L)
                    val postGain = (postSeconds * wonPerSec).roundToLong()

                    basePensionValue += baseBalAtGlobal
                    realTimePensionAssets += (baseBalAtGlobal + postGain)
                } else {
                    val baseBalAtGlobal = pension.currentBalance
                    val itemSeconds = ChronoUnit.SECONDS.between(itemDateTime, currentDateTime).coerceAtLeast(0L)
                    val itemGain = (itemSeconds * wonPerSec).roundToLong()

                    basePensionValue += baseBalAtGlobal
                    realTimePensionAssets += (baseBalAtGlobal + itemGain)
                }
            } else {
                val baseBal = pension.currentBalance
                val elapsedSeconds = ChronoUnit.SECONDS.between(itemDateTime, currentDateTime).coerceAtLeast(0L)
                val gain = (elapsedSeconds * wonPerSec).roundToLong()

                basePensionValue += baseBal
                realTimePensionAssets += (baseBal + gain)
            }
        }
        val pensionAccumulatedGain = realTimePensionAssets - basePensionValue

        // 3. 정기 소득 유입 (월급/사업소득)
        var baseIncomeValue = 0L
        var incomeAccumulatedGain = 0L
        var incomeWonPerSecond = 0.0

        val fallbackAssetBaseDate = nonLiabilityAssets.map { it.baseDate }.filter { it.isNotBlank() }.minOrNull()
            ?: pensions.map { it.baseDate }.filter { it.isNotBlank() }.minOrNull()

        for (income in activeIncomes) {
            val incomeEffectiveBase = if (income.baseDate.isNotBlank()) {
                income.baseDate
            } else if (hasGlobalBaseDate) {
                if (fallbackAssetBaseDate != null && parseDateTimeSafely(globalBaseDateTime).isAfter(parseDateTimeSafely(fallbackAssetBaseDate))) {
                    fallbackAssetBaseDate
                } else {
                    globalBaseDateTime
                }
            } else {
                income.effectiveBaseDate
            }
            val itemDateTimeRaw = parseDateTimeSafely(incomeEffectiveBase)
            val itemDateTime = if (itemDateTimeRaw.isAfter(currentDateTime)) currentDateTime else itemDateTimeRaw
            val wonPerSec = (income.monthlyAmount * 12.0) / SECONDS_PER_YEAR
            incomeWonPerSecond += wonPerSec

            if (hasGlobalBaseDate) {
                if (!itemDateTime.isAfter(globalTargetDateTime)) {
                    val preSeconds = ChronoUnit.SECONDS.between(itemDateTime, globalTargetDateTime).coerceAtLeast(0L)
                    baseIncomeValue += (preSeconds * wonPerSec).roundToLong()

                    val postSeconds = ChronoUnit.SECONDS.between(globalTargetDateTime, currentDateTime).coerceAtLeast(0L)
                    incomeAccumulatedGain += (postSeconds * wonPerSec).roundToLong()
                } else {
                    val itemSeconds = ChronoUnit.SECONDS.between(itemDateTime, currentDateTime).coerceAtLeast(0L)
                    incomeAccumulatedGain += (itemSeconds * wonPerSec).roundToLong()
                }
            } else {
                val itemSeconds = ChronoUnit.SECONDS.between(itemDateTime, currentDateTime).coerceAtLeast(0L)
                incomeAccumulatedGain += (itemSeconds * wonPerSec).roundToLong()
            }
        }

        // 4. 총부채 및 이자비용 (부채 종류에 따른 실시간 상환 및 기준일 상환액 반영)
        var baseTotalDebt = 0L
        var realTimeTotalDebt = 0L
        var debtInterestWonPerSecond = 0.0
        var debtAccumulatedInterest = 0L
        var debtPrincipalWonPerSecond = 0.0
        var baseDebtInterest = 0L

        for (debt in debtAssets) {
            val debtEffectiveBase = if (debt.baseDate.isNotBlank()) {
                debt.baseDate
            } else if (hasGlobalBaseDate) {
                globalBaseDateTime
            } else {
                debt.effectiveBaseDate
            }
            val itemDateTimeRaw = parseDateTimeSafely(debtEffectiveBase)
            val itemDateTime = if (itemDateTimeRaw.isAfter(currentDateTime)) currentDateTime else itemDateTimeRaw
            val interestWonPerSec = (debt.currentValue * (debt.expectedGrowthRate / 100.0)) / SECONDS_PER_YEAR
            debtInterestWonPerSecond += interestWonPerSec

            val annualPrincipal = calculateAnnualPrincipalRepayment(debt)
            val principalWonPerSec = annualPrincipal / SECONDS_PER_YEAR
            debtPrincipalWonPerSecond += principalWonPerSec

            if (hasGlobalBaseDate) {
                if (!itemDateTime.isAfter(globalTargetDateTime)) {
                    // 항목 등록/기준일 <= 전체 기준일:
                    // 기준일까지 상환된 원금을 반영하여 전체 기준일 시점의 기초 부채 산출
                    val preSeconds = ChronoUnit.SECONDS.between(itemDateTime, globalTargetDateTime).coerceAtLeast(0L)
                    val preRepaid = (preSeconds * principalWonPerSec).roundToLong().coerceIn(0L, debt.currentValue)
                    val baseDebtAtGlobal = debt.currentValue - preRepaid
                    baseDebtInterest += (preSeconds * interestWonPerSec).roundToLong()

                    // 전체 기준일부터 현재 일시까지 실시간 상환된 원금 반영
                    val postSeconds = ChronoUnit.SECONDS.between(globalTargetDateTime, currentDateTime).coerceAtLeast(0L)
                    val postRepaid = (postSeconds * principalWonPerSec).roundToLong().coerceIn(0L, baseDebtAtGlobal)
                    val realTimeDebt = baseDebtAtGlobal - postRepaid

                    baseTotalDebt += baseDebtAtGlobal
                    realTimeTotalDebt += realTimeDebt
                    debtAccumulatedInterest += (postSeconds * interestWonPerSec).roundToLong()
                } else {
                    // 항목 등록/기준일 > 전체 기준일:
                    val baseDebtAtGlobal = debt.currentValue
                    val itemSeconds = ChronoUnit.SECONDS.between(itemDateTime, currentDateTime).coerceAtLeast(0L)
                    val itemRepaid = (itemSeconds * principalWonPerSec).roundToLong().coerceIn(0L, baseDebtAtGlobal)
                    val realTimeDebt = baseDebtAtGlobal - itemRepaid

                    baseTotalDebt += baseDebtAtGlobal
                    realTimeTotalDebt += realTimeDebt
                    debtAccumulatedInterest += (itemSeconds * interestWonPerSec).roundToLong()
                }
            } else {
                val elapsedSeconds = ChronoUnit.SECONDS.between(itemDateTime, currentDateTime).coerceAtLeast(0L)
                val repaid = (elapsedSeconds * principalWonPerSec).roundToLong().coerceIn(0L, debt.currentValue)
                val realTimeDebt = debt.currentValue - repaid

                baseTotalDebt += debt.currentValue
                realTimeTotalDebt += realTimeDebt
                debtAccumulatedInterest += (elapsedSeconds * interestWonPerSec).roundToLong()
            }
        }

        // 실시간 상환된 부채 원금 총액 (기초 기준일 대비 실시간 상환액)
        val totalPrincipalRepaid = (baseTotalDebt - realTimeTotalDebt).coerceAtLeast(0L)
        val totalDebtPaymentAccumulated = totalPrincipalRepaid + debtAccumulatedInterest
        val debtPaymentWonPerSecond = debtPrincipalWonPerSecond + debtInterestWonPerSecond

        // 5. 생활비 소비 지출
        val livingExpenseWonPerSecond = if (monthlyExpenses > 0L) (monthlyExpenses * 12.0) / SECONDS_PER_YEAR else 0.0
        var baseLivingExpense = 0L
        var livingExpenseAccumulated = 0L

        if (monthlyExpenses > 0L) {
            val defaultIncomeBase = if (fallbackAssetBaseDate != null && hasGlobalBaseDate && parseDateTimeSafely(globalBaseDateTime).isAfter(parseDateTimeSafely(fallbackAssetBaseDate))) {
                fallbackAssetBaseDate
            } else {
                globalBaseDateTime
            }
            val expenseBaseDate = activeIncomes.map { parseDateTimeSafely(if (it.baseDate.isNotBlank()) it.baseDate else defaultIncomeBase) }.minOrNull()
                ?: nonLiabilityAssets.map { parseDateTimeSafely(it.effectiveBaseDate) }.minOrNull()
                ?: globalTargetDateTime

            if (hasGlobalBaseDate) {
                if (!expenseBaseDate.isAfter(globalTargetDateTime)) {
                    val preSeconds = ChronoUnit.SECONDS.between(expenseBaseDate, globalTargetDateTime).coerceAtLeast(0L)
                    baseLivingExpense = (preSeconds * livingExpenseWonPerSecond).roundToLong()
                    val postSeconds = ChronoUnit.SECONDS.between(globalTargetDateTime, currentDateTime).coerceAtLeast(0L)
                    livingExpenseAccumulated = (postSeconds * livingExpenseWonPerSecond).roundToLong()
                } else {
                    val itemSeconds = ChronoUnit.SECONDS.between(expenseBaseDate, currentDateTime).coerceAtLeast(0L)
                    livingExpenseAccumulated = (itemSeconds * livingExpenseWonPerSecond).roundToLong()
                }
            } else {
                val elapsedSeconds = ChronoUnit.SECONDS.between(expenseBaseDate, currentDateTime).coerceAtLeast(0L)
                livingExpenseAccumulated = (elapsedSeconds * livingExpenseWonPerSecond).roundToLong()
            }
        }

        // 6. 사적연금 급여 차감 직접 납입 지출
        val deductedPensions = if (currentAge > 0) {
            fundedPensions.filter { currentAge <= it.contributionEndAge && it.isDeductedFromIncome }
        } else {
            fundedPensions.filter { it.isDeductedFromIncome }
        }
        val pensionContribWonPerSecond = deductedPensions.sumOf { (it.monthlyContribution * 12.0) / SECONDS_PER_YEAR }
        var pensionContribAccumulated = 0L

        for (dp in deductedPensions) {
            val dpEffectiveBase = if (dp.baseDate.isNotBlank()) {
                dp.baseDate
            } else if (hasGlobalBaseDate) {
                globalBaseDateTime
            } else {
                dp.effectiveBaseDate
            }
            val itemDateTimeRaw = parseDateTimeSafely(dpEffectiveBase)
            val itemDateTime = if (itemDateTimeRaw.isAfter(currentDateTime)) currentDateTime else itemDateTimeRaw
            val wonPerSec = (dp.monthlyContribution * 12.0) / SECONDS_PER_YEAR

            if (hasGlobalBaseDate) {
                if (!itemDateTime.isAfter(globalTargetDateTime)) {
                    val postSeconds = ChronoUnit.SECONDS.between(globalTargetDateTime, currentDateTime).coerceAtLeast(0L)
                    pensionContribAccumulated += (postSeconds * wonPerSec).roundToLong()
                } else {
                    val itemSeconds = ChronoUnit.SECONDS.between(itemDateTime, currentDateTime).coerceAtLeast(0L)
                    pensionContribAccumulated += (itemSeconds * wonPerSec).roundToLong()
                }
            } else {
                val itemSeconds = ChronoUnit.SECONDS.between(itemDateTime, currentDateTime).coerceAtLeast(0L)
                pensionContribAccumulated += (itemSeconds * wonPerSec).roundToLong()
            }
        }

        // 7. 가계 순 잉여 현금흐름 (소득 - 생활비 - 사적연금직접납입 - 대출원리금상환)
        // 대출 상환액(원리금)이 가계 소득 및 현금흐름에서 차감되어 자산 증가액에 정확하게 반영됩니다.
        val netCashFlowWonPerSecond = incomeWonPerSecond - livingExpenseWonPerSecond - pensionContribWonPerSecond - debtPaymentWonPerSecond
        val netCashFlowAccumulated = incomeAccumulatedGain - livingExpenseAccumulated - pensionContribAccumulated - totalDebtPaymentAccumulated

        // 8. 연금 적립금 및 운용수익 (사적연금 납입금 자산 이전 보존 처리)
        val realTimePensionAssetsWithContrib = realTimePensionAssets + pensionContribAccumulated
        val pensionGainWithContrib = pensionAccumulatedGain + pensionContribAccumulated
        val pensionWonPerSecondWithContrib = pensionWonPerSecond + pensionContribWonPerSecond

        // 기초 기준일 시점의 가계 순현금 축적분 (전체 기준일 이전 항목들의 경과분)
        val basePreCashAccumulated = baseIncomeValue - baseLivingExpense - baseDebtInterest
        val baseTotalGrossAssets = (baseAssetsValue + basePensionValue + basePreCashAccumulated).coerceAtLeast(0L)

        // 실시간 총자산 = 기초 총자산 + 일반자산성장분 + 연금성장분(납입금 포함) + 가계 순잉여현금흐름(소득 - 생활비 - 대출원리금상환)
        // (회계 원칙: 부채 상환 시 지출된 현금이 정확히 차감되어 총자산과 순자산이 실물 흐름과 완벽히 일치합니다)
        val realTimeTotalGrossAssets = (baseTotalGrossAssets + assetsAccumulatedGain + pensionGainWithContrib + netCashFlowAccumulated).coerceAtLeast(0L)
        val totalGrossAssetGain = (realTimeTotalGrossAssets - baseTotalGrossAssets).coerceAtLeast(0L)

        // 9. 실시간 자산증가속도(v_w) 및 순자산 산출 (회계적 항등식: 총자산 = 순자산 + 총부채 완벽 보존)
        val grossAssetWonPerSecond = assetsWonPerSecond + pensionWonPerSecondWithContrib + netCashFlowWonPerSecond
        val netWonPerSecond = grossAssetWonPerSecond + debtPrincipalWonPerSecond

        // 기초 순자산 및 실시간 순자산
        val baseNetWorth = (baseTotalGrossAssets - baseTotalDebt).coerceAtLeast(0L)
        val realTimeNetWorth = (realTimeTotalGrossAssets - realTimeTotalDebt).coerceAtLeast(0L)
        val totalNetGain = (realTimeNetWorth - baseNetWorth).coerceAtLeast(0L)

        return TotalRealTimeGrowthSummary(
            baseTotalGrossAssets = baseTotalGrossAssets,
            realTimeTotalGrossAssets = realTimeTotalGrossAssets,
            totalGrossAssetGain = totalGrossAssetGain,
            grossAssetWonPerSecond = grossAssetWonPerSecond,
            baseTotalDebt = baseTotalDebt,
            realTimeTotalDebt = realTimeTotalDebt,
            debtInterestWonPerSecond = debtInterestWonPerSecond,
            debtPrincipalWonPerSecond = debtPrincipalWonPerSecond,
            debtPrincipalRepaidAccumulated = totalPrincipalRepaid,
            debtInterestAccumulated = debtAccumulatedInterest,
            debtRepaymentAccumulated = totalDebtPaymentAccumulated,
            baseNetWorth = baseNetWorth,
            realTimeNetWorth = realTimeNetWorth,
            totalNetGain = totalNetGain,
            netWonPerSecond = netWonPerSecond,
            basePensionAssets = basePensionValue,
            realTimePensionAssets = realTimePensionAssetsWithContrib,
            pensionGain = pensionGainWithContrib,
            pensionWonPerSecond = pensionWonPerSecondWithContrib,
            incomeAccumulatedGain = incomeAccumulatedGain,
            incomeWonPerSecond = incomeWonPerSecond,
            livingExpenseAccumulated = livingExpenseAccumulated,
            livingExpenseWonPerSecond = livingExpenseWonPerSecond,
            globalBaseDateTime = globalBaseDateTime
        )
    }
}
