package com.pension.alchemy

import com.pension.alchemy.data.model.*
import com.pension.alchemy.domain.engine.RealTimeGrowthCalculator
import com.pension.alchemy.domain.engine.SimulationEngine
import com.pension.alchemy.util.SampleDataGenerator
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class PresetCalculationVerificationTest {

    @Test
    fun testAllPresetsDateIntegrityAndCalculations() {
        val presets = listOf(
            20 to SampleDataGenerator.create20sPreset(),
            30 to SampleDataGenerator.create30sPreset(),
            40 to SampleDataGenerator.create40sPreset(),
            50 to SampleDataGenerator.create50sPreset(),
            60 to SampleDataGenerator.create60sPreset()
        )

        val testNow = LocalDateTime.of(2026, 10, 2, 23, 45, 0)

        for ((ageGroup, data) in presets) {
            val (profile, pensions, assetsAndIncomes) = data
            val (assets, incomes) = assetsAndIncomes

            // 1. 날짜 설정 무결성 검증 (현행 버전 스펙 YYYY-MM-DD)
            assertTrue("$ageGroup 대 프로필의 globalBaseDateTime 이 비어있지 않아야 함", profile.globalBaseDateTime.isNotBlank())
            assertEquals("$ageGroup 대 프로필의 globalBaseDateTime 은 2026-01-01 이어야 함", "2026-01-01", profile.globalBaseDateTime)

            for (asset in assets) {
                assertTrue("$ageGroup 대 자산(${asset.name})의 baseDate 가 비어있지 않아야 함", asset.baseDate.isNotBlank())
                assertEquals("$ageGroup 대 자산(${asset.name})의 baseDate 는 2026-01-01 이어야 함", "2026-01-01", asset.baseDate)
            }

            for (pension in pensions) {
                assertTrue("$ageGroup 대 연금(${pension.name})의 baseDate 가 비어있지 않아야 함", pension.baseDate.isNotBlank())
                assertEquals("$ageGroup 대 연금(${pension.name})의 baseDate 는 2026-01-01 이어야 함", "2026-01-01", pension.baseDate)
            }

            for (income in incomes) {
                assertTrue("$ageGroup 대 소득(${income.name})의 baseDate 가 비어있지 않아야 함", income.baseDate.isNotBlank())
                assertEquals("$ageGroup 대 소득(${income.name})의 baseDate 는 2026-01-01 이어야 함", "2026-01-01", income.baseDate)
            }

            // 2. 시뮬레이션 엔진 연산 검증
            val simSummary = SimulationEngine.runComprehensiveSimulation(profile, pensions, assets, incomes)
            assertNotNull("$ageGroup 대 시뮬레이션 결과가 null이 아니어야 함", simSummary)
            assertTrue("$ageGroup 대 기초 순자산이 양수여야 함 (실제: ${simSummary.currentNetWorth})", simSummary.currentNetWorth > 0L)
            assertTrue("$ageGroup 대 은퇴 건강점수가 0~100 사이여야 함", simSummary.healthScore.score in 0..100)
            assertTrue("$ageGroup 대 초당 순자산 속도가 양수여야 함", simSummary.realTimeYield.wonPerSecond > 0.0)

            for (yr in simSummary.yearlyResults) {
                assertFalse("$ageGroup 대 연간 결과에 NaN이나 비정상 값이 없어야 함", yr.netAssetValue.toDouble().isNaN())
                assertTrue("$ageGroup 대 나이 순서가 올바르게 증가해야 함", yr.age >= profile.currentAge)
            }

            // 3. 실시간 성장 계산기 연산 검증
            val rtSummary = RealTimeGrowthCalculator.calculateTotalRealTimeGrowth(
                assets = assets,
                pensions = pensions,
                incomes = incomes,
                monthlyExpenses = profile.currentMonthlyExpenses,
                currentAge = profile.currentAge,
                currentDateTime = testNow,
                globalBaseDateTime = profile.effectiveGlobalBaseDateTime
            )

            // 회계적 항등식 검증: 자산 = 부채 + 순자산
            assertEquals(
                "$ageGroup 대 기초 총자산에서 기초 총부채를 차감한 값이 기초 순자산과 일치해야 함",
                rtSummary.baseTotalGrossAssets - rtSummary.baseTotalDebt,
                rtSummary.baseNetWorth
            )
            assertEquals(
                "$ageGroup 대 실시간 총자산에서 실시간 총부채를 차감한 값이 실시간 순자산과 일치해야 함",
                rtSummary.realTimeTotalGrossAssets - rtSummary.realTimeTotalDebt,
                rtSummary.realTimeNetWorth
            )

            // 시뮬레이션 초기 순자산과 실시간 기초 순자산 일치 검증
            assertEquals(
                "$ageGroup 대 시뮬레이션 초기 순자산과 실시간 기초 순자산이 일치해야 함",
                simSummary.currentNetWorth,
                rtSummary.baseNetWorth
            )

            // 2026-01-01 기준 약 9개월 경과 후의 실시간 순자산 누적 증가액 검증
            assertTrue(
                "$ageGroup 대 누적 순자산 증가액(totalNetGain)은 양수여야 함 (실제: ${rtSummary.totalNetGain})",
                rtSummary.totalNetGain > 0L
            )
            assertTrue(
                "$ageGroup 대 실시간 순자산은 기초 순자산보다 커야 함 (실제 실시간: ${rtSummary.realTimeNetWorth}, 기초: ${rtSummary.baseNetWorth})",
                rtSummary.realTimeNetWorth > rtSummary.baseNetWorth
            )
            assertTrue(
                "$ageGroup 대 실시간 초당 순자산 증가속도는 양수여야 함 (실제: ${rtSummary.netWonPerSecond})",
                rtSummary.netWonPerSecond > 0.0
            )

            // 공통 기준일 일치 검증
            assertEquals("2026-01-01", rtSummary.globalBaseDateTime)
        }
    }
}
