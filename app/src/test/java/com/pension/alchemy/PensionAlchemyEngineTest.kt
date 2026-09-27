package com.pension.alchemy

import com.pension.alchemy.data.model.*
import com.pension.alchemy.domain.engine.CalculatorsEngine
import com.pension.alchemy.domain.engine.SimulationEngine
import com.pension.alchemy.util.SampleDataGenerator
import org.junit.Assert.*
import org.junit.Test

class PensionAlchemyEngineTest {

    @Test
    fun testSimulationEngineComprehensive() {
        val profile = SampleDataGenerator.createDefaultProfile()
        val pensions = SampleDataGenerator.createDefaultPensions()
        val assets = SampleDataGenerator.createDefaultAssets()
        val incomes = SampleDataGenerator.createDefaultIncomes()

        val summary = SimulationEngine.runComprehensiveSimulation(profile, pensions, assets, incomes)

        assertNotNull(summary)
        assertTrue(summary.yearlyResults.isNotEmpty())
        assertTrue(summary.currentTotalAssets > 0L)
        assertTrue(summary.currentNetWorth > 0L)
        assertTrue(summary.healthScore.score in 0..100)
        println("Simulation completed successfully! NetWorth: ${summary.currentNetWorth}, HealthScore: ${summary.healthScore.score}")
    }

    @Test
    fun testStatutoryNationalPensionAge() {
        assertEquals(60, SimulationEngine.getStatutoryNationalPensionAge(1950))
        assertEquals(61, SimulationEngine.getStatutoryNationalPensionAge(1955))
        assertEquals(62, SimulationEngine.getStatutoryNationalPensionAge(1958))
        assertEquals(63, SimulationEngine.getStatutoryNationalPensionAge(1962))
        assertEquals(64, SimulationEngine.getStatutoryNationalPensionAge(1966))
        assertEquals(65, SimulationEngine.getStatutoryNationalPensionAge(1975))
    }

    @Test
    fun testCalculatorsEngine() {
        // 1. Accumulation & Lump Sum (적립 및 예탁 미래가치)
        val acc = CalculatorsEngine.calculateAccumulation(1_000_000L, 10, 6.0)
        assertTrue(acc.futureValue > 120_000_000L) // 10년 100만 원 원금(1.2억) 초과
        assertTrue(acc.totalInterest > 0L)

        // 1-2. Lump Sum Only (순수 예탁 미래가치)
        val lumpSumAcc = CalculatorsEngine.calculateAccumulation(
            monthlyDeposit = 0L,
            periodYears = 10,
            annualInterestRate = 6.0,
            initialDeposit = 50_000_000L
        )
        assertTrue(lumpSumAcc.futureValue > 50_000_000L)
        assertEquals(50_000_000L, lumpSumAcc.totalPrincipal)
        assertTrue(lumpSumAcc.totalInterest > 0L)

        // 2. Withdrawal
        val wd = CalculatorsEngine.calculateWithdrawal(500_000_000L, 5.0, true, 2_000_000L, 4.0)
        assertNotNull(wd.yearlyBreakdown)

        // 3. Depletion
        val dep = CalculatorsEngine.calculateDepletion(300_000_000L, 2_500_000L, 4.0, 2.0)
        assertTrue(dep.depletionYears > 0.0)

        // 4. Required Wealth
        val req = CalculatorsEngine.calculateRequiredWealth(3_000_000L, 30, 4.0)
        assertTrue(req.targetTotalWealth > 500_000_000L)

        // 5. Break-even
        val be = CalculatorsEngine.calculateNationalPensionBreakEven(1_500_000L, 65)
        assertTrue(be.earlyVsNormalBreakEvenAge in 74..78)
        assertTrue(be.normalVsDelayedBreakEvenAge in 80..85)

        // 6. Tax Benefit
        val tb = CalculatorsEngine.calculateTaxBenefit(9_000_000L, true)
        assertEquals(1_485_000L, tb.taxRefundAmount) // 900만 * 16.5% = 148만 5천 원
        assertTrue(tb.taxSavingsAlchemyBonus > 0L)
    }

    @Test
    fun testPensionTaxCalculator() {
        // 1. National Pension
        // 0원인 경우 세금 0
        assertEquals(0L, com.pension.alchemy.domain.engine.PensionTaxCalculator.calculateAnnualNationalPensionTax(0L))
        // 연 350만원 이하: 전액 공제 + 인적공제 -> 과표 0 -> 세금 0
        assertEquals(0L, com.pension.alchemy.domain.engine.PensionTaxCalculator.calculateAnnualNationalPensionTax(3_500_000L))
        // 연 2,100만원 (월 175만원) 국민연금
        val nationalTax21M = com.pension.alchemy.domain.engine.PensionTaxCalculator.calculateAnnualNationalPensionTax(21_000_000L)
        assertTrue("연 2100만원 수령 시 세금 발생해야 함", nationalTax21M > 0L)
        // 실효세율은 약 3~5% 수준이어야 함
        val effectiveRate = nationalTax21M.toDouble() / 21_000_000.0 * 100.0
        assertTrue("국민연금 실효세율은 1~6% 범위여야 함: $effectiveRate%", effectiveRate in 1.0..6.0)

        // 2. Retirement Pension (30% vs 40% 감면)
        val retGross1M = 1_000_000L
        val retTaxYear5 = com.pension.alchemy.domain.engine.PensionTaxCalculator.calculateMonthlyRetirementPensionTax(retGross1M, 5)
        val retTaxYear12 = com.pension.alchemy.domain.engine.PensionTaxCalculator.calculateMonthlyRetirementPensionTax(retGross1M, 12)
        assertEquals(35_000L, retTaxYear5) // 3.5%
        assertEquals(30_000L, retTaxYear12) // 3.0%
        assertTrue("11년차 이상 퇴직연금 세금이 10년 이내보다 적어야 함", retTaxYear12 < retTaxYear5)

        // 3. Personal Pension Savings (연령별 저율 분리과세)
        val persAnnual12M = 12_000_000L // 1,500만원 이하
        val persTaxAge65 = com.pension.alchemy.domain.engine.PensionTaxCalculator.calculateAnnualPersonalPensionTax(persAnnual12M, 65)
        val persTaxAge75 = com.pension.alchemy.domain.engine.PensionTaxCalculator.calculateAnnualPersonalPensionTax(persAnnual12M, 75)
        val persTaxAge85 = com.pension.alchemy.domain.engine.PensionTaxCalculator.calculateAnnualPersonalPensionTax(persAnnual12M, 85)
        assertEquals(660_000L, persTaxAge65) // 1200만 * 5.5% = 66만원
        assertEquals(528_000L, persTaxAge75) // 1200만 * 4.4% = 52.8만원
        assertEquals(396_000L, persTaxAge85) // 1200만 * 3.3% = 39.6만원

        // 3-1. 1,500만원 초과 시 초과분 16.5% 분리과세
        val persAnnual20M = 20_000_000L
        val persTaxAge65Over = com.pension.alchemy.domain.engine.PensionTaxCalculator.calculateAnnualPersonalPensionTax(persAnnual20M, 65)
        // 1500만 * 5.5% = 82.5만, 500만 * 16.5% = 82.5만 -> 합계 165만원
        assertEquals(1_650_000L, persTaxAge65Over)

        // 4. Annuity Insurance & Housing Pension (100% 비과세)
        assertEquals(0L, com.pension.alchemy.domain.engine.PensionTaxCalculator.calculateMonthlyAnnuityInsuranceTax())
        assertEquals(0L, com.pension.alchemy.domain.engine.PensionTaxCalculator.calculateMonthlyHousingPensionTax())
    }

    @Test
    fun testSimulationWithAnnuityInsuranceAndTaxes() {
        val profile = SampleDataGenerator.createDefaultProfile()
        val pensions = listOf(
            Pension(
                name = "국민연금",
                type = PensionType.NATIONAL,
                startAge = 65,
                endAge = 100,
                expectedMonthlyAmount = 1_800_000L
            ),
            Pension(
                name = "개인연금보험(비과세)",
                type = PensionType.ANNUITY_INSURANCE,
                startAge = 60,
                endAge = 90,
                expectedMonthlyAmount = 1_000_000L,
                currentBalance = 100_000_000L,
                expectedGrowthRate = 3.5,
                isTaxDeductionEligible = false
            ),
            Pension(
                name = "개인연금저축",
                type = PensionType.PERSONAL,
                startAge = 60,
                endAge = 85,
                expectedMonthlyAmount = 800_000L,
                expectedGrowthRate = 4.0
            )
        )
        val assets = SampleDataGenerator.createDefaultAssets()
        val incomes = SampleDataGenerator.createDefaultIncomes()

        val summary = SimulationEngine.runComprehensiveSimulation(profile, pensions, assets, incomes)

        // 60세 시점 검증 (개인연금보험 + 개인연금저축 수령)
        val result60 = summary.yearlyResults.first { it.age == 60 }
        assertTrue("개인연금보험 수령액이 0보다 커야 함", result60.monthlyAnnuityInsurancePension > 0L)
        assertTrue("총 세전 연금액이 세후 연금액보다 크거나 같아야 함", result60.grossMonthlyPensionIncome >= result60.monthlyPensionIncome)
        assertEquals("세전 - 세후 = 세금이어야 함", result60.grossMonthlyPensionIncome - result60.monthlyPensionIncome, result60.monthlyPensionTax)
        assertEquals("총 소득은 근로소득 + 실지급 연금소득이어야 함", result60.monthlyWorkIncome + result60.monthlyPensionIncome, result60.monthlyTotalIncome)

        // 66세 시점 검증 (국민연금 포함, 2,000만원 초과 건보 피부양자 탈락 판정)
        val result66 = summary.yearlyResults.first { it.age == 66 }
        assertTrue("연 1800만원 * 물가상승 초과 국민연금으로 피부양자 탈락 경고가 발생할 수 있음", result66.grossMonthlyPensionIncome > 0L)
        assertTrue("국민연금 세후 수령액이 세전보다 적거나 같아야 함", result66.monthlyNationalPension <= 1_800_000L * 2)
    }

    @Test
    fun testAssetReordering() {
        val list = listOf(
            Asset(name = "1번 자산", type = AssetType.DEPOSIT, currentValue = 100L),
            Asset(name = "2번 자산", type = AssetType.DEPOSIT, currentValue = 200L),
            Asset(name = "3번 자산", type = AssetType.DEPOSIT, currentValue = 300L)
        )
        val mutable = list.toMutableList()
        // 0번과 1번 위치 변경
        val item = mutable.removeAt(0)
        mutable.add(1, item)

        assertEquals("2번 자산", mutable[0].name)
        assertEquals("1번 자산", mutable[1].name)
        assertEquals("3번 자산", mutable[2].name)
    }

    @Test
    fun testCustomPolicySettings() {
        // 세법 개정 가상 시나리오:
        // 1) 사적연금 한도를 1,500만원 -> 2,400만원으로 상향
        // 2) 퇴직연금 10년 이하 감면율을 30% -> 50%로 상향
        // 3) 국민연금 연기 증액률을 연 7.2% -> 8.0%로 상향
        val customPolicy = PolicySettings(
            privatePensionAnnualLimit = 24_000_000L,
            privatePensionExcessRate = 20.0,
            retirementTaxDiscountRateEarly = 50.0,
            nationalDelayIncreaseRatePerYear = 8.0
        )

        // 1. 사적연금 한도 상향에 따른 절세 반영 검증
        val defaultTax = com.pension.alchemy.domain.engine.PensionTaxCalculator.calculateAnnualPersonalPensionTax(20_000_000L, 65)
        val customTax = com.pension.alchemy.domain.engine.PensionTaxCalculator.calculateAnnualPersonalPensionTax(20_000_000L, 65, customPolicy)
        assertEquals(1_650_000L, defaultTax) // 기본: 1500만*5.5% + 500만*16.5% = 165만원
        assertEquals(1_100_000L, customTax)  // 변경 정책: 2000만*5.5% = 110만원 (55만원 절세)
        assertTrue("한도 상향 시 세금이 줄어야 함", customTax < defaultTax)

        // 2. 퇴직연금 50% 감면율 반영 검증
        val customRetTax = com.pension.alchemy.domain.engine.PensionTaxCalculator.calculateMonthlyRetirementPensionTax(1_000_000L, 5, customPolicy)
        assertEquals(25_000L, customRetTax) // 100만 * 5.0% * (1 - 0.50) = 2.5만원

        // 3. 커스텀 정책이 주입된 프로필로 종합 시뮬레이션 정상 연동 검증
        val profile = SampleDataGenerator.createDefaultProfile().copy(policySettings = customPolicy)
        val summary = SimulationEngine.runComprehensiveSimulation(
            profile = profile,
            pensions = SampleDataGenerator.createDefaultPensions(),
            assets = SampleDataGenerator.createDefaultAssets(),
            incomes = SampleDataGenerator.createDefaultIncomes()
        )
        assertNotNull(summary)
        assertTrue(summary.yearlyResults.isNotEmpty())
    }

    @Test
    fun testAllLifecyclePresetsSimulation() {
        val presets = listOf(
            20 to SampleDataGenerator.create20sPreset(),
            30 to SampleDataGenerator.create30sPreset(),
            40 to SampleDataGenerator.create40sPreset(),
            50 to SampleDataGenerator.create50sPreset(),
            60 to SampleDataGenerator.create60sPreset()
        )

        for ((ageGroup, presetData) in presets) {
            val (profile, pensions, assetsAndIncomes) = presetData
            val (assets, incomes) = assetsAndIncomes

            assertNotNull("$ageGroup 대 프로필이 null이 아니어야 함", profile)
            assertTrue("$ageGroup 대 연금 목록이 비어있지 않아야 함", pensions.isNotEmpty())
            assertTrue("$ageGroup 대 자산 목록이 비어있지 않아야 함", assets.isNotEmpty())

            // 시뮬레이션 엔진이 예외 없이 완벽하게 동작하는지 검증
            val summary = SimulationEngine.runComprehensiveSimulation(profile, pensions, assets, incomes)
            assertNotNull("$ageGroup 대 시뮬레이션 요약이 생성되어야 함", summary)
            assertTrue("$ageGroup 대 연간 결과 목록이 비어있지 않아야 함", summary.yearlyResults.isNotEmpty())
            assertTrue("$ageGroup 대 점수가 0..100 범위여야 함", summary.healthScore.score in 0..100)

            // 나이 순환 안정성 검증
            assertEquals(profile.targetEndAge, summary.endAge)
            assertTrue("시작 나이가 종료 나이보다 작아야 함", summary.currentAge < summary.endAge)
        }
    }

    @Test
    fun testCalculatorsWithPolicy() {
        val customPolicy = PolicySettings(
            nationalEarlyReductionRatePerYear = 7.0, // 연 7% 감액
            nationalDelayIncreaseRatePerYear = 8.0,  // 연 8% 증액
            maxTaxCreditContribution = 12_000_000L,  // 1,200만원 한도 상향
            taxCreditRateLowIncome = 18.0            // 18% 공제율
        )

        val beResult = CalculatorsEngine.calculateNationalPensionBreakEven(
            normalMonthlyAmount = 2_000_000L,
            statutoryAge = 65,
            policy = customPolicy
        )
        // 5년 조기: 200만 * (1 - 5 * 0.07) = 200만 * 0.65 = 130만원
        assertEquals(1_300_000L, beResult.earlyMonthlyAmount)
        // 5년 연기: 200만 * (1 + 5 * 0.08) = 200만 * 1.40 = 280만원
        assertEquals(2_800_000L, beResult.delayedMonthlyAmount)

        val taxResult = CalculatorsEngine.calculateTaxBenefit(
            annualContribution = 12_000_000L,
            isLowIncomeTier = true,
            policy = customPolicy
        )
        // 1200만 * 18% = 216만원
        assertEquals(2_160_000L, taxResult.taxRefundAmount)
    }

    @Test
    fun testPersistentDeficitSimulationTrajectory() {
        // 수입이 없고 지출만 있는 시나리오: 30세 시작, 초기 자산 5천만원, 월 지출 200만원 (연 2,400만원 지출)
        val profile = UserProfile(
            birthYear = 1996, // 30세
            retirementAge = 60,
            targetEndAge = 40,
            monthlyExpenses = 2_000_000L,
            inflationRate = 0.0, // 직관적 검증을 위해 물가상승률 0% 가정
            policySettings = PolicySettings(financialAssetReturnRate = 0.0)
        )
        val initialCash = 50_000_000L
        val assets = listOf(
            Asset(name = "초기 현금", type = AssetType.DEPOSIT, currentValue = initialCash, expectedGrowthRate = 0.0)
        )
        // 연금 없음, 수입 없음
        val summary = SimulationEngine.runComprehensiveSimulation(profile, emptyList(), assets, emptyList())

        // 30세부터 40세까지 11개 연도 결과
        val results = summary.yearlyResults
        assertEquals(11, results.size)

        // 초기 자산 확인
        assertEquals(initialCash, summary.currentNetWorth)

        // 30세 말: 약 5,000만 - 2,400만 = 2,600만
        val r30 = results.first { it.age == 30 }
        assertTrue("30세 말 순자산은 2600만원 수준이어야 함", r30.netAssetValue in 25_000_000L..27_000_000L)

        // 31세 말: 약 2,600만 - 2,400만 = 200만
        val r31 = results.first { it.age == 31 }
        assertTrue("31세 말 순자산은 200만원 수준이어야 함", r31.netAssetValue in 1_000_000L..3_000_000L)

        // 32세 말: 200만 - 2,400만 = 약 -2,200만원 (음수 순자산 진입!)
        val r32 = results.first { it.age == 32 }
        assertTrue("32세 말 순자산은 마이너스여야 함 (실제: ${r32.netAssetValue})", r32.netAssetValue < 0L)
        assertEquals("최초 고갈 나이는 32세여야 함", 32, summary.depletionAge)

        // 33세 말, 34세 말... 특정 시점에 멈추지 않고 계속해서 음수로 지속 하락해야 함!
        for (i in 0 until results.size - 1) {
            val curr = results[i]
            val next = results[i + 1]
            assertTrue(
                "적자 지속 시 순자산은 다음 해에 더 감소해야 함 (현재 ${curr.age}세: ${curr.netAssetValue}, 다음 ${next.age}세: ${next.netAssetValue})",
                curr.netAssetValue > next.netAssetValue
            )
            // 특정 고정된 값으로 멈춰있는 구간이 없어야 함
            assertNotEquals(
                "연속된 두 해의 순자산이 고정된 값으로 멈춰있으면 안 됨",
                curr.netAssetValue,
                next.netAssetValue
            )
        }

        // 최종 40세 순자산은 약 -2억원 수준이어야 함
        val r40 = results.first { it.age == 40 }
        assertTrue("40세 최종 순자산은 깊은 마이너스여야 함 (실제: ${r40.netAssetValue})", r40.netAssetValue < -150_000_000L)
    }

    @Test
    fun testPensionAssetDepletionDuringPayout() {
        // 개인연금 60세 시작 ~ 70세 종료 (10년 수령), 매월 100만원 수령, 60세 시점 잔액 1.2억원
        val profile = UserProfile(
            birthYear = 1966, // 60세
            retirementAge = 60,
            targetEndAge = 80,
            monthlyExpenses = 1_000_000L,
            inflationRate = 0.0
        )
        val personalPension = Pension(
            name = "10년 확정 개인연금",
            type = PensionType.PERSONAL,
            startAge = 60,
            endAge = 70,
            expectedMonthlyAmount = 1_000_000L,
            currentBalance = 120_000_000L,
            expectedGrowthRate = 0.0,
            contributionEndAge = 60
        )
        val summary = SimulationEngine.runComprehensiveSimulation(profile, listOf(personalPension), emptyList(), emptyList())
        val results = summary.yearlyResults

        // 60세 시점: 연금 수령(연 1,200만원) 후 잔여 연금 자산은 1.2억 - 1200만 = 1.08억원
        val r60 = results.first { it.age == 60 }
        assertEquals(108_000_000L, r60.pensionAssets)

        // 65세 시점: 연금 자산이 중간 정도로 감소해 있어야 함
        val r65 = results.first { it.age == 65 }
        assertTrue("65세 연금 자산은 60세보다 감소해야 함", r65.pensionAssets < r60.pensionAssets && r65.pensionAssets > 0L)

        // 70세 시점: 10년 수령 완료로 0원에 도달
        val r70 = results.first { it.age == 70 }
        assertEquals("70세 연금 수령 완료 시 잔액은 0원이어야 함", 0L, r70.pensionAssets)

        // 71세 ~ 80세: 수령 종료 후에도 연금 자산이 0원으로 유지되어야 함 (고정된 값으로 남아있지 않아야 함)
        for (age in 71..80) {
            val r = results.first { it.age == age }
            assertEquals("${age}세에 연금 자산은 0원이어야 함", 0L, r.pensionAssets)
        }
    }
}