package com.pension.alchemy.domain.engine

import com.pension.alchemy.data.model.*
import com.pension.alchemy.util.LoanCalculator
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

object SimulationEngine {

    /**
     * 출생연도별 법정 국민연금 수령 개시 연령
     */
    fun getStatutoryNationalPensionAge(birthYear: Int): Int {
        return when {
            birthYear <= 1952 -> 60
            birthYear in 1953..1956 -> 61
            birthYear in 1957..1960 -> 62
            birthYear in 1961..1964 -> 63
            birthYear in 1965..1968 -> 64
            else -> 65 // 1969년생 이후는 만 65세
        }
    }

    /**
     * 종합 생애 자산 및 은퇴 시뮬레이션 실행
     */
    fun runComprehensiveSimulation(
        profile: UserProfile,
        pensions: List<Pension>,
        assets: List<Asset>,
        incomes: List<Income>
    ): SimulationSummary {
        val currentYear = LocalDate.now().year
        val currentAge = profile.currentAge.coerceAtLeast(20)
        val endAge = profile.targetEndAge.coerceIn(currentAge + 1, 110)
        val retirementAge = profile.retirementAge.coerceIn(currentAge, endAge)

        val statutoryNationalAge = getStatutoryNationalPensionAge(profile.birthYear)
        val nationalPension = pensions.firstOrNull { it.type == PensionType.NATIONAL }
        val nationalPensionStartAge = if (nationalPension != null) {
            val baseAge = if (nationalPension.startAge != 65) nationalPension.startAge else statutoryNationalAge
            (baseAge + nationalPension.claimOffsetYears).coerceIn(55, 75)
        } else {
            statutoryNationalAge
        }

        val policy = profile.policySettings

        // 초기 자산 분류 및 개별 자산 기대수익률 가중평균 산출
        val financialAssetsList = assets.filter { !it.isLiability && it.type != AssetType.REAL_ESTATE }
        var liquidFinancialAssets = financialAssetsList.sumOf { it.currentValue }
        val customFinancialYield = if (liquidFinancialAssets > 0L) {
            financialAssetsList.sumOf { it.currentValue * (it.expectedGrowthRate / 100.0) } / liquidFinancialAssets.toDouble()
        } else {
            policy.financialAssetReturnRate / 100.0
        }

        val realEstateAssetsList = assets.filter { !it.isLiability && it.type == AssetType.REAL_ESTATE }
        var realEstateAssets = realEstateAssetsList.sumOf { it.currentValue }
        val customRealEstateYield = if (realEstateAssets > 0L) {
            realEstateAssetsList.sumOf { it.currentValue * (it.expectedGrowthRate / 100.0) } / realEstateAssets.toDouble()
        } else {
            policy.realEstateGrowthRate / 100.0
        }

        // 개별 부채 상환 추적 맵 (assetId -> currentBalance)
        val debtAssets = assets.filter { it.isLiability }
        val debtBalances = debtAssets.associate { it.id to it.currentValue }.toMutableMap()
        var debtBalance = debtBalances.values.sum()

        // 연금 적립금 추적 맵 (공적연금인 국민연금과 역모기지인 주택연금은 적립식 계좌자산이 아니므로 제외)
        val pensionBalances = pensions
            .filter { it.type != PensionType.NATIONAL && it.type != PensionType.HOUSING }
            .associate { it.id to it.currentBalance.toDouble() }
            .toMutableMap()

        // 생활비 초기값 (은퇴 전: 현재 월 생활소비 / 은퇴 후: 현재가치 기준 희망생활비의 은퇴시점 미래가치 환산)
        val generalInflationRate = profile.inflationRate / 100.0
        val medicalInflationRate = generalInflationRate + (policy.medicalInflationSurcharge / 100.0)

        // 1) 은퇴 전: 현재 월 생활비 (현재가치 기준에서 매년 물가상승률 복리 적용)
        var preRetireLivingExpense = profile.currentMonthlyExpenses.toDouble()

        // 2) 은퇴 후: 은퇴 시점까지 물가상승 복리 반영한 미래가치 기준 (기본생활비 + 의료비 가중치)
        val yearsToRetirement = (retirementAge - currentAge).coerceAtLeast(0)
        val postRetireBaseLiving = profile.monthlyExpenses * (1.0 - profile.medicalExpenseRatio)
        val postRetireBaseMed = profile.monthlyExpenses * profile.medicalExpenseRatio
        var postRetireLivingExpense = postRetireBaseLiving * (1.0 + generalInflationRate).pow(yearsToRetirement.toDouble())
        var postRetireMedExpense = postRetireBaseMed * (1.0 + medicalInflationRate).pow(yearsToRetirement.toDouble())

        val yearlyResults = mutableListOf<YearlySimulationResult>()
        var peakAsset = Long.MIN_VALUE
        var peakAge = currentAge
        var depletionAge = 0

        var preRetirementWorkIncome = 0L
        var postRetirementPension = 0L

        var crevasseShortfallTotal = 0L
        var crevasseYearCount = 0

        val isCrevassePossible = retirementAge < nationalPensionStartAge

        for (age in currentAge..endAge) {
            val yearsPassed = age - currentAge
            val year = currentYear + yearsPassed

            // 1. 월 지출: 은퇴 전에는 현재 생활 소비액, 은퇴 시점부터는 은퇴 후 필요 생활비(미래가치) 적용
            val monthlyExpenses: Long = if (age < retirementAge) {
                preRetireLivingExpense.roundToLong()
            } else {
                (postRetireLivingExpense + postRetireMedExpense).roundToLong()
            }
            val annualExpenses = monthlyExpenses * 12L

            // 2. 근로 및 기타 정기 소득
            var monthlyWorkIncome = 0L
            for (inc in incomes) {
                if (age <= inc.endAge) {
                    val growthFactor = (1.0 + inc.expectedGrowthRate / 100.0).pow(yearsPassed.toDouble())
                    monthlyWorkIncome += (inc.monthlyAmount * growthFactor).roundToLong()
                }
            }

            if (age == retirementAge - 1 && monthlyWorkIncome > 0L) {
                preRetirementWorkIncome = monthlyWorkIncome
            }

            // 3. 연금 적립 및 수령액 산출
            var grossMonthlyNational = 0L
            var grossMonthlyRetirement = 0L
            var grossMonthlyPersonal = 0L
            var grossMonthlyAnnuityInsurance = 0L
            var grossMonthlyHousing = 0L
            var grossMonthlyOther = 0L
            var totalPensionAssets = 0L
            var annualPensionContributionsTotal = 0L
            var annualPensionContributionsDeducted = 0L

            for (p in pensions) {
                val effectiveStartAge = if (p.type == PensionType.NATIONAL) {
                    val baseAge = if (p.startAge != 65) p.startAge else statutoryNationalAge
                    (baseAge + p.claimOffsetYears).coerceIn(55, 75)
                } else {
                    p.startAge
                }

                val isFundedPension = p.type != PensionType.NATIONAL && p.type != PensionType.HOUSING
                val currentBal = if (isFundedPension) (pensionBalances[p.id] ?: 0.0) else 0.0

                // A) 적립기 (수령 개시 전 및 납입기)
                if (isFundedPension) {
                    if (age <= p.contributionEndAge && p.monthlyContribution > 0L) {
                        val annualContrib = p.monthlyContribution * 12.0
                        annualPensionContributionsTotal += p.monthlyContribution * 12L
                        if (p.isDeductedFromIncome) {
                            annualPensionContributionsDeducted += p.monthlyContribution * 12L
                        }
                        val rate = p.expectedGrowthRate / 100.0
                        val updatedBal = (currentBal + annualContrib) * (1.0 + rate)
                        pensionBalances[p.id] = updatedBal
                    } else if (age < effectiveStartAge && currentBal > 0.0) {
                        // 납입 완료 후 수령 전까지 운용수익 복리 증식
                        val rate = p.expectedGrowthRate / 100.0
                        pensionBalances[p.id] = currentBal * (1.0 + rate)
                    }
                }

                // B) 수령기 지급액 계산
                var monthlyPayout = 0.0
                if (age >= effectiveStartAge && age <= p.endAge) {
                    monthlyPayout = p.expectedMonthlyAmount.toDouble()

                    if (p.type == PensionType.NATIONAL) {
                        // 조기/연기 수령 보정률 (설정된 연간 감액/증액률 반영)
                        if (p.claimOffsetYears != 0) {
                            val adjustmentFactor = if (p.claimOffsetYears < 0) {
                                1.0 + (p.claimOffsetYears * (policy.nationalEarlyReductionRatePerYear / 100.0))
                            } else {
                                1.0 + (p.claimOffsetYears * (policy.nationalDelayIncreaseRatePerYear / 100.0))
                            }
                            monthlyPayout *= adjustmentFactor
                        }
                        // 국민연금은 법률에 따라 매년 물가상승률(CPI) 복리 연동 인상
                        val cpiFactor = (1.0 + generalInflationRate).pow(yearsPassed.toDouble())
                        monthlyPayout *= cpiFactor
                    } else if (p.type == PensionType.HOUSING || p.type == PensionType.OTHER) {
                        // 주택연금 및 기타 확정연금은 명목 고정 지급
                    } else {
                        // 사적연금(개인연금, 퇴직연금, 연금보험)은 이미 잔여적립금 복리운용을 포함한 PMT 연금화 금액이므로
                        // 추적 잔액이 있는 경우 잔액 한도 내에서 안전하게 인출 지급
                        val hasTrackedBalance = (p.currentBalance > 0L || p.monthlyContribution > 0L)
                        if (hasTrackedBalance) {
                            val currentAccBal = pensionBalances[p.id] ?: 0.0
                            val rate = p.expectedGrowthRate / 100.0
                            val availableThisYear = currentAccBal * (1.0 + rate)
                            val requestedAnnual = monthlyPayout * 12.0
                            if (availableThisYear < requestedAnnual && availableThisYear > 0.0) {
                                monthlyPayout = availableThisYear / 12.0
                            } else if (availableThisYear <= 0.0 && age > effectiveStartAge) {
                                monthlyPayout = 0.0
                            }
                        }
                    }

                    val payoutLong = monthlyPayout.roundToLong()
                    when (p.type) {
                        PensionType.NATIONAL -> grossMonthlyNational += payoutLong
                        PensionType.RETIREMENT -> grossMonthlyRetirement += payoutLong
                        PensionType.PERSONAL -> grossMonthlyPersonal += payoutLong
                        PensionType.ANNUITY_INSURANCE -> grossMonthlyAnnuityInsurance += payoutLong
                        PensionType.HOUSING -> grossMonthlyHousing += payoutLong
                        PensionType.OTHER -> grossMonthlyOther += payoutLong
                    }
                }

                // C) 수령기 적립금 인출 및 잔여 자산 갱신
                if (isFundedPension) {
                    val hasTrackedBalance = (p.currentBalance > 0L || p.monthlyContribution > 0L)
                    if (hasTrackedBalance) {
                        if (age >= effectiveStartAge && age <= p.endAge) {
                            val rate = p.expectedGrowthRate / 100.0
                            val balWithYield = (pensionBalances[p.id] ?: currentBal) * (1.0 + rate)
                            val annualWithdrawal = monthlyPayout * 12.0
                            val remainingBal = (balWithYield - annualWithdrawal).coerceAtLeast(0.0)
                            pensionBalances[p.id] = remainingBal
                        } else if (age > p.endAge) {
                            // 수령 기간 만료 후 계좌 잔액 소진
                            pensionBalances[p.id] = 0.0
                        }
                        totalPensionAssets += (pensionBalances[p.id] ?: 0.0).roundToLong()
                    }
                }
            }

            // 3-1. 세법에 따른 연금소득세 및 실지급액(세후 실수령액) 정밀 계산 (설정된 PolicySettings 적용)
            val nationalAnnualGross = grossMonthlyNational * 12L
            val nationalAnnualTax = PensionTaxCalculator.calculateAnnualNationalPensionTax(nationalAnnualGross, policy)
            val nationalMonthlyTax = nationalAnnualTax / 12L
            val netMonthlyNational = grossMonthlyNational - nationalMonthlyTax
            val isHealthInsuranceDisqualified = nationalAnnualGross > policy.healthInsurancePensionLimit

            // 퇴직연금 (설정된 1~10년차 vs 11년차 이상 차등 감면율 적용)
            var retirementMonthlyTax = 0L
            for (p in pensions) {
                if (p.type == PensionType.RETIREMENT && age in p.startAge..p.endAge) {
                    val yearsSinceStart = (age - p.startAge + 1).coerceAtLeast(1)
                    val pPayout = p.expectedMonthlyAmount
                    retirementMonthlyTax += PensionTaxCalculator.calculateMonthlyRetirementPensionTax(pPayout, yearsSinceStart, policy)
                }
            }
            val netMonthlyRetirement = (grossMonthlyRetirement - retirementMonthlyTax).coerceAtLeast(0L)

            // 개인연금저축 (설정된 사적연금 한도 및 연령별 저율/초과 분리과세율 적용)
            val personalAnnualGross = grossMonthlyPersonal * 12L
            val personalAnnualTax = PensionTaxCalculator.calculateAnnualPersonalPensionTax(personalAnnualGross, age, policy)
            val personalMonthlyTax = personalAnnualTax / 12L
            val netMonthlyPersonal = (grossMonthlyPersonal - personalMonthlyTax).coerceAtLeast(0L)
            val isPrivatePensionLimitExceeded = personalAnnualGross > policy.privatePensionAnnualLimit

            // 개인연금보험 (10년 이상 유지 시 비과세 0%)
            val netMonthlyAnnuityInsurance = grossMonthlyAnnuityInsurance

            // 주택연금 (역모기지 대출금 비과세 0%)
            val netMonthlyHousing = grossMonthlyHousing

            // 기타연금 (5.5% 분리과세)
            val otherMonthlyTax = PensionTaxCalculator.calculateMonthlyOtherPensionTax(grossMonthlyOther)
            val netMonthlyOther = (grossMonthlyOther - otherMonthlyTax).coerceAtLeast(0L)

            // 총 연금 수령액 (세전 vs 세후 실지급)
            val totalGrossMonthlyPension = grossMonthlyNational + grossMonthlyRetirement + grossMonthlyPersonal + grossMonthlyAnnuityInsurance + grossMonthlyHousing + grossMonthlyOther
            val totalMonthlyPensionTax = nationalMonthlyTax + retirementMonthlyTax + personalMonthlyTax + otherMonthlyTax
            val monthlyPensionIncome = (totalGrossMonthlyPension - totalMonthlyPensionTax).coerceAtLeast(0L) // 세후 실수령액

            val totalMonthlyIncome = monthlyWorkIncome + monthlyPensionIncome
            val annualIncome = totalMonthlyIncome * 12L

            if (age == nationalPensionStartAge && postRetirementPension == 0L) {
                postRetirementPension = monthlyPensionIncome
            }

            // 4. 생애 주기 단계 및 소득 크레바스 판정
            val stage: String
            val isCrevasse: Boolean
            if (age < retirementAge) {
                stage = "축적기"
                isCrevasse = false
            } else if (age < nationalPensionStartAge) {
                stage = "소득 크레바스 (공백기)"
                isCrevasse = true
                val shortfall = monthlyExpenses - totalMonthlyIncome
                if (shortfall > 0) {
                    crevasseShortfallTotal += shortfall
                    crevasseYearCount++
                }
            } else {
                stage = "은퇴/연금수령기"
                isCrevasse = false
            }

            // 4-1. 부채 원리금 상환 계산 (기한 및 상환방식 반영)
            var annualDebtPrincipal = 0L
            var annualDebtInterest = 0L
            for (debt in debtAssets) {
                val currentDebtBal = debtBalances[debt.id] ?: 0L
                if (currentDebtBal > 0L) {
                    val result = LoanCalculator.calculateAnnualRepayment(
                        currentBalance = currentDebtBal,
                        originalPrincipal = debt.currentValue,
                        annualRatePercent = debt.expectedGrowthRate,
                        maturityYears = debt.maturityYears.coerceAtLeast(1),
                        repaymentMethod = debt.repaymentMethod,
                        yearIndex = yearsPassed
                    )
                    debtBalances[debt.id] = result.remainingBalance
                    annualDebtPrincipal += result.principalPayment
                    annualDebtInterest += result.interestPayment
                }
            }
            debtBalance = debtBalances.values.sum()
            val annualDebtService = annualDebtPrincipal + annualDebtInterest

            // 5. 자산 성장 및 현금흐름 밸런싱 (개별 자산 가중 기대수익률 적용)
            // 총 지출 = 생활비/의료비 + 부채 원리금 상환액 + 가계 수입에서 직접 납입하는 사적연금 납입액
            val annualTotalExpenses = annualExpenses + annualDebtService + annualPensionContributionsDeducted
            val annualCashFlow = annualIncome - annualTotalExpenses
            val financialYield = if (liquidFinancialAssets > 0L) customFinancialYield else 0.0
            val realEstateYield = customRealEstateYield

            if (annualCashFlow >= 0L) {
                // 흑자: 잉여금이 금융자산에 축적 (만약 마이너스 통장/결손이 있었다면 우선 메꿈)
                val base = if (liquidFinancialAssets > 0L) {
                    (liquidFinancialAssets * (1.0 + financialYield)).roundToLong()
                } else {
                    liquidFinancialAssets
                }
                liquidFinancialAssets = base + annualCashFlow
            } else {
                // 적자: 지출 부족분 발생
                var deficit = abs(annualCashFlow)

                // 1) 금융자산이 플러스인 경우 먼저 인출
                if (liquidFinancialAssets > 0L) {
                    val grownAssets = (liquidFinancialAssets * (1.0 + financialYield)).roundToLong()
                    if (grownAssets >= deficit) {
                        liquidFinancialAssets = grownAssets - deficit
                        deficit = 0L
                    } else {
                        deficit -= grownAssets
                        liquidFinancialAssets = 0L
                    }
                }

                // 2) 금융자산 소진 후 부동산 자산에서 차감 (주택연금화, 축소, 유동화 가정)
                if (deficit > 0L && realEstateAssets > 0L) {
                    if (realEstateAssets >= deficit) {
                        realEstateAssets -= deficit
                        deficit = 0L
                    } else {
                        deficit -= realEstateAssets
                        realEstateAssets = 0L
                    }
                }

                // 3) 모든 유동자산과 부동산이 소진된 후에도 남은 적자: 순자산 결손(신규 부채/마이너스)으로 누적 반영!
                if (deficit > 0L) {
                    liquidFinancialAssets -= deficit
                }
            }

            // 부동산 자산 자연 성장 (부동산이 남아있을 때만)
            if (realEstateAssets > 0L) {
                realEstateAssets = (realEstateAssets * (1.0 + realEstateYield)).roundToLong()
            }

            // 총 자산 및 순자산 산출
            val positiveFinancialAssets = liquidFinancialAssets.coerceAtLeast(0L)
            val totalGrossAssets = positiveFinancialAssets + realEstateAssets + totalPensionAssets
            // 순자산: 총자산 - 대출부채 - 누적적자(금융자산 결손분)
            val netAssetValue = if (liquidFinancialAssets < 0L) {
                totalGrossAssets - debtBalance + liquidFinancialAssets // liquidFinancialAssets is negative
            } else {
                totalGrossAssets - debtBalance
            }

            // 고갈 나이 추적 (처음으로 순자산이 0 이하가 된 시점)
            if (netAssetValue <= 0L && depletionAge == 0 && age > currentAge) {
                depletionAge = age
            }

            // 최대 자산 추적
            if (netAssetValue > peakAsset) {
                peakAsset = netAssetValue
                peakAge = age
            }

            yearlyResults.add(
                YearlySimulationResult(
                    age = age,
                    year = year,
                    stage = stage,
                    isCrevasse = isCrevasse,
                    totalGrossAssets = totalGrossAssets,
                    totalDebt = debtBalance,
                    netAssetValue = netAssetValue,
                    financialAssets = liquidFinancialAssets,
                    pensionAssets = totalPensionAssets,
                    realEstateAssets = realEstateAssets,
                    monthlyTotalIncome = totalMonthlyIncome,
                    monthlyWorkIncome = monthlyWorkIncome,
                    monthlyPensionIncome = monthlyPensionIncome,
                    grossMonthlyPensionIncome = totalGrossMonthlyPension,
                    monthlyPensionTax = totalMonthlyPensionTax,
                    monthlyNationalPension = netMonthlyNational,
                    monthlyRetirementPension = netMonthlyRetirement,
                    monthlyPersonalPension = netMonthlyPersonal,
                    monthlyAnnuityInsurancePension = netMonthlyAnnuityInsurance,
                    monthlyHousingPension = netMonthlyHousing,
                    monthlyOtherPension = netMonthlyOther,
                    monthlyExpenses = monthlyExpenses,
                    monthlyPensionContribution = annualPensionContributionsDeducted / 12L,
                    monthlyDebtService = annualDebtService / 12L,
                    monthlyNetCashFlow = annualCashFlow / 12L,
                    isHealthInsuranceDisqualified = isHealthInsuranceDisqualified,
                    isPrivatePensionLimitExceeded = isPrivatePensionLimitExceeded
                )
            )

            // 익년 물가상승 반영
            if (age < retirementAge) {
                preRetireLivingExpense *= (1.0 + generalInflationRate)
            } else {
                postRetireLivingExpense *= (1.0 + generalInflationRate)
                postRetireMedExpense *= (1.0 + medicalInflationRate)
            }
        }

        // 소득대체율 계산
        val incomeReplacementRate = if (preRetirementWorkIncome > 0) {
            val safePension = if (postRetirementPension > 0) postRetirementPension else (yearlyResults.firstOrNull { it.age >= retirementAge }?.monthlyPensionIncome ?: 0L)
            (safePension.toDouble() / preRetirementWorkIncome.toDouble() * 100.0).coerceIn(0.0, 200.0)
        } else {
            50.0 // 기본 준거치
        }

        // 소득 크레바스 정보
        val crevasseDuration = if (isCrevassePossible) nationalPensionStartAge - retirementAge else 0
        val avgCrevasseShortfall = if (crevasseYearCount > 0) crevasseShortfallTotal / crevasseYearCount else 0L
        val crevasseInfo = IncomeCrevasseInfo(
            hasCrevasse = isCrevassePossible && crevasseDuration > 0,
            startAge = retirementAge,
            endAge = nationalPensionStartAge,
            durationYears = crevasseDuration,
            monthlyShortfall = avgCrevasseShortfall,
            totalRequiredBridgeFund = avgCrevasseShortfall * 12L * crevasseDuration.toLong()
        )

        // 은퇴 건강도 점수 분석
        val healthScore = RetirementHealthAnalyzer.evaluate(
            incomeReplacementRate = incomeReplacementRate,
            depletionAge = depletionAge,
            targetEndAge = endAge,
            hasCrevasseShortfall = crevasseInfo.hasCrevasse && crevasseInfo.monthlyShortfall > 0,
            hasRealEstate = assets.any { !it.isLiability && it.type == AssetType.REAL_ESTATE },
            hasFinancialAssets = assets.any { !it.isLiability && it.type != AssetType.REAL_ESTATE },
            hasPensionAssets = pensions.isNotEmpty()
        )

        val initialTotalDebt = assets.filter { it.isLiability }.sumOf { it.currentValue }
        val initialFinancialAssets = assets.filter { !it.isLiability && it.type != AssetType.REAL_ESTATE }.sumOf { it.currentValue }
        val initialRealEstateAssets = assets.filter { !it.isLiability && it.type == AssetType.REAL_ESTATE }.sumOf { it.currentValue }
        val initialPensionAssets = pensions.filter { it.type != PensionType.NATIONAL && it.type != PensionType.HOUSING }.sumOf { it.currentBalance }
        val initialGrossAssets = initialFinancialAssets + initialRealEstateAssets + initialPensionAssets
        val initialNetWorth = initialGrossAssets - initialTotalDebt

        // 실시간 초당 자산 수익 및 가계 순현금흐름 지표 (현재 기준 나이)
        // 1. 유입 (Inflow)
        val initialAnnualRegularIncome = incomes.filter { currentAge <= it.endAge }.sumOf { it.monthlyAmount * 12L }
        val annualFinancialGain = financialAssetsList.sumOf { (it.currentValue * (it.expectedGrowthRate / 100.0)).roundToLong() }
        val annualRealEstateGain = realEstateAssetsList.sumOf { (it.currentValue * (it.expectedGrowthRate / 100.0)).roundToLong() }
        val annualPensionGain = pensions
            .filter { it.type != PensionType.NATIONAL && it.type != PensionType.HOUSING }
            .sumOf { (it.currentBalance * (it.expectedGrowthRate / 100.0)).roundToLong() }
        val annualTotalInflow = initialAnnualRegularIncome + annualFinancialGain + annualRealEstateGain + annualPensionGain

        // 2. 유출 (Outflow)
        val annualLivingExpenses = profile.currentMonthlyExpenses * 12L
        val annualDebtInterestCost = debtAssets.sumOf { (it.currentValue * (it.expectedGrowthRate / 100.0)).roundToLong() }
        val annualPensionContributionDeducted = pensions
            .filter { it.type != PensionType.NATIONAL && it.type != PensionType.HOUSING && currentAge <= it.contributionEndAge && it.isDeductedFromIncome }
            .sumOf { it.monthlyContribution * 12L }
        val annualTotalOutflow = annualLivingExpenses + annualDebtInterestCost + annualPensionContributionDeducted

        // 3. 종합 실질 순자산 순증가액 (Net Wealth Growth)
        val annualNetWealthGrowth = annualTotalInflow - annualTotalOutflow
        val annualNetCapitalGain = annualFinancialGain + annualRealEstateGain + annualPensionGain - annualDebtInterestCost

        val wonPerSecond = annualNetWealthGrowth / (365.25 * 86400.0)
        val wonPerHour = wonPerSecond * 3600.0
        val wonPerDay = (annualNetWealthGrowth / 365.25).roundToLong()
        val wonPerMonth = (annualNetWealthGrowth / 12.0).roundToLong()

        val realTimeYield = RealTimeYieldMetrics(
            annualRegularIncome = initialAnnualRegularIncome,
            annualFinancialGain = annualFinancialGain,
            annualRealEstateGain = annualRealEstateGain,
            annualPensionGain = annualPensionGain,
            annualTotalInflow = annualTotalInflow,
            annualLivingExpenses = annualLivingExpenses,
            annualDebtInterestCost = annualDebtInterestCost,
            annualPensionContributionDeducted = annualPensionContributionDeducted,
            annualTotalOutflow = annualTotalOutflow,
            annualNetWealthGrowth = annualNetWealthGrowth,
            annualNetCapitalGain = annualNetCapitalGain,
            wonPerSecond = wonPerSecond,
            wonPerHour = wonPerHour,
            wonPerDay = wonPerDay,
            wonPerMonth = wonPerMonth
        )

        val firstYear = yearlyResults.firstOrNull()
        return SimulationSummary(
            currentAge = currentAge,
            endAge = endAge,
            retirementAge = retirementAge,
            nationalPensionStartAge = nationalPensionStartAge,
            currentNetWorth = initialNetWorth,
            currentTotalAssets = initialGrossAssets,
            currentTotalDebt = initialTotalDebt,
            currentFinancialAssets = initialFinancialAssets,
            currentPensionAssets = initialPensionAssets,
            currentRealEstateAssets = initialRealEstateAssets,
            depletionAge = depletionAge,
            isSafeRetirement = depletionAge == 0,
            peakAssetAge = peakAge,
            peakAssetValue = if (peakAsset == Long.MIN_VALUE) initialNetWorth else peakAsset,
            finalAssetValue = yearlyResults.lastOrNull()?.netAssetValue ?: 0L,
            preRetirementMonthlyIncome = preRetirementWorkIncome,
            postRetirementMonthlyPension = postRetirementPension,
            incomeReplacementRate = incomeReplacementRate,
            crevasseInfo = crevasseInfo,
            healthScore = healthScore,
            realTimeYield = realTimeYield,
            yearlyResults = yearlyResults
        )
    }
}