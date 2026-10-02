package com.pension.alchemy.util

import com.pension.alchemy.data.model.*

object SampleDataGenerator {

    /**
     * 현행 2026년 기준 프리셋 공통 기준일 (YYYY-MM-DD)
     */
    const val DEFAULT_PRESET_BASE_DATE = "2026-01-01"

    // ──────────────────────────────────────────────
    // 20대 사회초년생 / 청년기 프리셋 (26세 기준)
    // ──────────────────────────────────────────────
    fun create20sPreset(): Triple<UserProfile, List<Pension>, Pair<List<Asset>, List<Income>>> {
        val profile = UserProfile(
            birthYear = 2000, // 26세
            retirementAge = 60,
            targetEndAge = 100,
            currentMonthlyExpenses = 1_800_000L,
            monthlyExpenses = 2_000_000L,
            medicalExpenseRatio = 0.08,
            inflationRate = 2.0,
            currency = "KRW",
            globalBaseDateTime = DEFAULT_PRESET_BASE_DATE
        )
        val pensions = listOf(
            Pension(
                name = "국민연금",
                type = PensionType.NATIONAL,
                startAge = 65,
                endAge = 100,
                expectedMonthlyAmount = 1_200_000L,
                expectedGrowthRate = 2.0,
                claimOffsetYears = 0,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Pension(
                name = "개인연금저축펀드",
                type = PensionType.PERSONAL,
                startAge = 60,
                endAge = 85,
                expectedMonthlyAmount = 600_000L,
                expectedGrowthRate = 6.0,
                currentBalance = 3_000_000L,
                monthlyContribution = 200_000L,
                contributionEndAge = 60,
                isTaxDeductionEligible = true,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Pension(
                name = "퇴직연금 (DC)",
                type = PensionType.RETIREMENT,
                startAge = 60,
                endAge = 80,
                expectedMonthlyAmount = 500_000L,
                expectedGrowthRate = 5.0,
                currentBalance = 4_000_000L,
                monthlyContribution = 250_000L,
                contributionEndAge = 60,
                isTaxDeductionEligible = true,
                baseDate = DEFAULT_PRESET_BASE_DATE
            )
        )
        val assets = listOf(
            Asset(
                name = "청년도약 및 청약저축",
                type = AssetType.SAVINGS,
                currentValue = 15_000_000L,
                expectedGrowthRate = 4.5,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Asset(
                name = "비상금 파킹통장",
                type = AssetType.DEPOSIT,
                currentValue = 5_000_000L,
                expectedGrowthRate = 2.5,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Asset(
                name = "학자금 대출(부채)",
                type = AssetType.DEBT,
                currentValue = 8_000_000L,
                expectedGrowthRate = 1.7,
                repaymentMethod = RepaymentMethod.EQUAL_PRINCIPAL_AND_INTEREST,
                maturityYears = 5,
                baseDate = DEFAULT_PRESET_BASE_DATE
            )
        )
        val incomes = listOf(
            Income(
                name = "첫 직장 급여",
                type = IncomeType.SALARY,
                monthlyAmount = 2_800_000L,
                endAge = 60,
                expectedGrowthRate = 3.5,
                baseDate = DEFAULT_PRESET_BASE_DATE
            )
        )
        return Triple(profile, pensions, Pair(assets, incomes))
    }

    // ──────────────────────────────────────────────
    // 30대 자산형성기 프리셋 (35세 기준)
    // ──────────────────────────────────────────────
    fun create30sPreset(): Triple<UserProfile, List<Pension>, Pair<List<Asset>, List<Income>>> {
        val profile = UserProfile(
            birthYear = 1991, // 35세
            retirementAge = 60,
            targetEndAge = 100,
            currentMonthlyExpenses = 2_800_000L,
            monthlyExpenses = 2_600_000L,
            medicalExpenseRatio = 0.09,
            inflationRate = 2.0,
            currency = "KRW",
            globalBaseDateTime = DEFAULT_PRESET_BASE_DATE
        )
        val pensions = listOf(
            Pension(
                name = "국민연금",
                type = PensionType.NATIONAL,
                startAge = 65,
                endAge = 100,
                expectedMonthlyAmount = 1_500_000L,
                expectedGrowthRate = 2.0,
                claimOffsetYears = 0,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Pension(
                name = "퇴직연금 (IRP)",
                type = PensionType.RETIREMENT,
                startAge = 60,
                endAge = 80,
                expectedMonthlyAmount = 850_000L,
                expectedGrowthRate = 5.5,
                currentBalance = 25_000_000L,
                monthlyContribution = 400_000L,
                contributionEndAge = 60,
                isTaxDeductionEligible = true,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Pension(
                name = "연금저축펀드",
                type = PensionType.PERSONAL,
                startAge = 60,
                endAge = 85,
                expectedMonthlyAmount = 800_000L,
                expectedGrowthRate = 6.0,
                currentBalance = 20_000_000L,
                monthlyContribution = 500_000L,
                contributionEndAge = 60,
                isTaxDeductionEligible = true,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Pension(
                name = "개인연금보험 (비과세)",
                type = PensionType.ANNUITY_INSURANCE,
                startAge = 60,
                endAge = 90,
                expectedMonthlyAmount = 300_000L,
                expectedGrowthRate = 3.5,
                currentBalance = 10_000_000L,
                monthlyContribution = 200_000L,
                contributionEndAge = 60,
                isTaxDeductionEligible = false,
                baseDate = DEFAULT_PRESET_BASE_DATE
            )
        )
        val assets = listOf(
            Asset(
                name = "청약 및 적금",
                type = AssetType.SAVINGS,
                currentValue = 40_000_000L,
                expectedGrowthRate = 3.2,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Asset(
                name = "미국 배당/성장 ETF",
                type = AssetType.ETF,
                currentValue = 35_000_000L,
                expectedGrowthRate = 7.0,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Asset(
                name = "전세보증금",
                type = AssetType.DEPOSIT,
                currentValue = 250_000_000L,
                expectedGrowthRate = 1.5,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Asset(
                name = "전세자금대출(부채)",
                type = AssetType.DEBT,
                currentValue = 120_000_000L,
                expectedGrowthRate = 3.8,
                repaymentMethod = RepaymentMethod.BULLET,
                maturityYears = 6,
                baseDate = DEFAULT_PRESET_BASE_DATE
            )
        )
        val incomes = listOf(
            Income(
                name = "본인 급여",
                type = IncomeType.SALARY,
                monthlyAmount = 4_000_000L,
                endAge = 60,
                expectedGrowthRate = 3.0,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Income(
                name = "맞벌이/부업 소득",
                type = IncomeType.OTHER,
                monthlyAmount = 1_500_000L,
                endAge = 55,
                expectedGrowthRate = 2.0,
                baseDate = DEFAULT_PRESET_BASE_DATE
            )
        )
        return Triple(profile, pensions, Pair(assets, incomes))
    }

    // ──────────────────────────────────────────────
    // 40대 대한민국 표준 가장 프리셋 (45세 기준)
    // ──────────────────────────────────────────────
    fun create40sPreset(): Triple<UserProfile, List<Pension>, Pair<List<Asset>, List<Income>>> {
        val profile = createDefaultProfile()
        val pensions = createDefaultPensions()
        val assets = createDefaultAssets()
        val incomes = createDefaultIncomes()
        return Triple(profile, pensions, Pair(assets, incomes))
    }

    fun createDefaultProfile(): UserProfile = UserProfile(
        birthYear = 1981, // 45세 기준
        retirementAge = 60,
        targetEndAge = 100,
        currentMonthlyExpenses = 3_800_000L,
        monthlyExpenses = 3_300_000L,
        medicalExpenseRatio = 0.10,
        inflationRate = 2.0,
        currency = "KRW",
        globalBaseDateTime = DEFAULT_PRESET_BASE_DATE
    )

    fun createDefaultPensions(): List<Pension> = listOf(
        Pension(
            name = "국민연금",
            type = PensionType.NATIONAL,
            startAge = 65,
            endAge = 100,
            expectedMonthlyAmount = 1_800_000L,
            expectedGrowthRate = 2.0,
            claimOffsetYears = 0,
            baseDate = DEFAULT_PRESET_BASE_DATE
        ),
        Pension(
            name = "퇴직연금 (DC형)",
            type = PensionType.RETIREMENT,
            startAge = 60,
            endAge = 80,
            expectedMonthlyAmount = 1_250_000L,
            expectedGrowthRate = 4.5,
            currentBalance = 85_000_000L,
            monthlyContribution = 600_000L,
            contributionEndAge = 60,
            isTaxDeductionEligible = true,
            baseDate = DEFAULT_PRESET_BASE_DATE
        ),
        Pension(
            name = "개인연금저축펀드",
            type = PensionType.PERSONAL,
            startAge = 60,
            endAge = 85,
            expectedMonthlyAmount = 950_000L,
            expectedGrowthRate = 5.5,
            currentBalance = 55_000_000L,
            monthlyContribution = 750_000L,
            contributionEndAge = 60,
            isTaxDeductionEligible = true,
            baseDate = DEFAULT_PRESET_BASE_DATE
        ),
        Pension(
            name = "개인연금보험 (비과세)",
            type = PensionType.ANNUITY_INSURANCE,
            startAge = 60,
            endAge = 90,
            expectedMonthlyAmount = 400_000L,
            expectedGrowthRate = 3.5,
            currentBalance = 25_000_000L,
            monthlyContribution = 300_000L,
            contributionEndAge = 60,
            isTaxDeductionEligible = false,
            baseDate = DEFAULT_PRESET_BASE_DATE
        )
    )

    fun createDefaultAssets(): List<Asset> = listOf(
        Asset(
            name = "거주 아파트",
            type = AssetType.REAL_ESTATE,
            currentValue = 700_000_000L,
            expectedGrowthRate = 2.5,
            baseDate = DEFAULT_PRESET_BASE_DATE
        ),
        Asset(
            name = "주식 및 글로벌 ETF",
            type = AssetType.ETF,
            currentValue = 120_000_000L,
            expectedGrowthRate = 6.0,
            baseDate = DEFAULT_PRESET_BASE_DATE
        ),
        Asset(
            name = "비상금 예적금",
            type = AssetType.DEPOSIT,
            currentValue = 50_000_000L,
            expectedGrowthRate = 3.0,
            baseDate = DEFAULT_PRESET_BASE_DATE
        ),
        Asset(
            name = "주택담보대출(부채)",
            type = AssetType.DEBT,
            currentValue = 220_000_000L,
            expectedGrowthRate = 3.8,
            repaymentMethod = RepaymentMethod.EQUAL_PRINCIPAL_AND_INTEREST,
            maturityYears = 15,
            baseDate = DEFAULT_PRESET_BASE_DATE
        )
    )

    fun createDefaultIncomes(): List<Income> = listOf(
        Income(
            name = "본인 근로소득",
            type = IncomeType.SALARY,
            monthlyAmount = 5_800_000L,
            endAge = 60,
            expectedGrowthRate = 2.5,
            baseDate = DEFAULT_PRESET_BASE_DATE
        ),
        Income(
            name = "배우자 부업/파트타임",
            type = IncomeType.OTHER,
            monthlyAmount = 1_500_000L,
            endAge = 58,
            expectedGrowthRate = 1.5,
            baseDate = DEFAULT_PRESET_BASE_DATE
        )
    )

    // ──────────────────────────────────────────────
    // 50대 은퇴 가속 준비기 프리셋 (55세 기준)
    // ──────────────────────────────────────────────
    fun create50sPreset(): Triple<UserProfile, List<Pension>, Pair<List<Asset>, List<Income>>> {
        val profile = UserProfile(
            birthYear = 1971, // 55세
            retirementAge = 60,
            targetEndAge = 100,
            currentMonthlyExpenses = 4_200_000L,
            monthlyExpenses = 3_500_000L,
            medicalExpenseRatio = 0.12,
            inflationRate = 2.0,
            currency = "KRW",
            globalBaseDateTime = DEFAULT_PRESET_BASE_DATE
        )
        val pensions = listOf(
            Pension(
                name = "국민연금",
                type = PensionType.NATIONAL,
                startAge = 65,
                endAge = 100,
                expectedMonthlyAmount = 1_950_000L,
                expectedGrowthRate = 2.0,
                claimOffsetYears = 0,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Pension(
                name = "퇴직연금 DB/DC",
                type = PensionType.RETIREMENT,
                startAge = 60,
                endAge = 80,
                expectedMonthlyAmount = 1_650_000L,
                expectedGrowthRate = 4.0,
                currentBalance = 190_000_000L,
                monthlyContribution = 800_000L,
                contributionEndAge = 60,
                isTaxDeductionEligible = true,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Pension(
                name = "개인연금저축",
                type = PensionType.PERSONAL,
                startAge = 60,
                endAge = 85,
                expectedMonthlyAmount = 1_100_000L,
                expectedGrowthRate = 4.5,
                currentBalance = 130_000_000L,
                monthlyContribution = 750_000L,
                contributionEndAge = 60,
                isTaxDeductionEligible = true,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Pension(
                name = "개인연금보험 (비과세)",
                type = PensionType.ANNUITY_INSURANCE,
                startAge = 60,
                endAge = 90,
                expectedMonthlyAmount = 500_000L,
                expectedGrowthRate = 3.5,
                currentBalance = 40_000_000L,
                monthlyContribution = 300_000L,
                contributionEndAge = 60,
                isTaxDeductionEligible = false,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Pension(
                name = "주택연금 (역모기지)",
                type = PensionType.HOUSING,
                startAge = 70,
                endAge = 100,
                expectedMonthlyAmount = 1_350_000L,
                expectedGrowthRate = 0.0,
                baseDate = DEFAULT_PRESET_BASE_DATE
            )
        )
        val assets = listOf(
            Asset(
                name = "보유 주택",
                type = AssetType.REAL_ESTATE,
                currentValue = 850_000_000L,
                expectedGrowthRate = 2.0,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Asset(
                name = "안전 배당주 및 채권",
                type = AssetType.BOND,
                currentValue = 180_000_000L,
                expectedGrowthRate = 4.8,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Asset(
                name = "정기예금",
                type = AssetType.DEPOSIT,
                currentValue = 100_000_000L,
                expectedGrowthRate = 3.2,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Asset(
                name = "잔여 대출(부채)",
                type = AssetType.DEBT,
                currentValue = 30_000_000L,
                expectedGrowthRate = 3.9,
                repaymentMethod = RepaymentMethod.EQUAL_PRINCIPAL_AND_INTEREST,
                maturityYears = 5,
                baseDate = DEFAULT_PRESET_BASE_DATE
            )
        )
        val incomes = listOf(
            Income(
                name = "본인 급여",
                type = IncomeType.SALARY,
                monthlyAmount = 6_500_000L,
                endAge = 60,
                expectedGrowthRate = 1.0,
                baseDate = DEFAULT_PRESET_BASE_DATE
            )
        )
        return Triple(profile, pensions, Pair(assets, incomes))
    }

    // ──────────────────────────────────────────────
    // 60대 은퇴 생활기 / 연금 인출기 프리셋 (63세 기준)
    // ──────────────────────────────────────────────
    fun create60sPreset(): Triple<UserProfile, List<Pension>, Pair<List<Asset>, List<Income>>> {
        val profile = UserProfile(
            birthYear = 1963, // 63세 (은퇴 완료)
            retirementAge = 60,
            targetEndAge = 100,
            currentMonthlyExpenses = 2_800_000L,
            monthlyExpenses = 2_800_000L,
            medicalExpenseRatio = 0.15,
            inflationRate = 2.0,
            currency = "KRW",
            globalBaseDateTime = DEFAULT_PRESET_BASE_DATE
        )
        val pensions = listOf(
            Pension(
                name = "국민연금",
                type = PensionType.NATIONAL,
                startAge = 63,
                endAge = 100,
                expectedMonthlyAmount = 1_650_000L,
                expectedGrowthRate = 2.0,
                claimOffsetYears = 0,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Pension(
                name = "퇴직연금 (IRP 분할수령)",
                type = PensionType.RETIREMENT,
                startAge = 60,
                endAge = 80,
                expectedMonthlyAmount = 1_100_000L,
                expectedGrowthRate = 3.8,
                currentBalance = 150_000_000L,
                monthlyContribution = 0L,
                contributionEndAge = 60,
                isTaxDeductionEligible = true,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Pension(
                name = "개인연금저축",
                type = PensionType.PERSONAL,
                startAge = 60,
                endAge = 85,
                expectedMonthlyAmount = 650_000L,
                expectedGrowthRate = 4.0,
                currentBalance = 90_000_000L,
                monthlyContribution = 0L,
                contributionEndAge = 60,
                isTaxDeductionEligible = true,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Pension(
                name = "주택연금 (역모기지)",
                type = PensionType.HOUSING,
                startAge = 70,
                endAge = 100,
                expectedMonthlyAmount = 1_200_000L,
                expectedGrowthRate = 0.0,
                baseDate = DEFAULT_PRESET_BASE_DATE
            )
        )
        val assets = listOf(
            Asset(
                name = "거주 주택",
                type = AssetType.REAL_ESTATE,
                currentValue = 650_000_000L,
                expectedGrowthRate = 1.8,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Asset(
                name = "배당주 및 안정형 펀드",
                type = AssetType.STOCK,
                currentValue = 120_000_000L,
                expectedGrowthRate = 4.5,
                baseDate = DEFAULT_PRESET_BASE_DATE
            ),
            Asset(
                name = "비상금 및 MMF",
                type = AssetType.DEPOSIT,
                currentValue = 40_000_000L,
                expectedGrowthRate = 2.8,
                baseDate = DEFAULT_PRESET_BASE_DATE
            )
            // 부채 0원
        )
        val incomes = listOf(
            Income(
                name = "시니어 자문/소일거리",
                type = IncomeType.OTHER,
                monthlyAmount = 1_000_000L,
                endAge = 68,
                expectedGrowthRate = 0.0,
                baseDate = DEFAULT_PRESET_BASE_DATE
            )
        )
        return Triple(profile, pensions, Pair(assets, incomes))
    }
}