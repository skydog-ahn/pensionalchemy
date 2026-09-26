package com.pension.alchemy

import com.pension.alchemy.data.model.*
import com.pension.alchemy.ui.screens.CalculatorViewModel
import com.pension.alchemy.util.SampleDataGenerator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class BackupRestoreTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testBackupDataSerializationAndDeserialization() {
        val originalProfile = SampleDataGenerator.createDefaultProfile().copy(
            birthYear = 1980,
            retirementAge = 58,
            monthlyExpenses = 3_200_000L,
            policySettings = PolicySettings(
                privatePensionAnnualLimit = 18_000_000L,
                healthInsurancePensionLimit = 25_000_000L,
                financialAssetReturnRate = 5.2
            )
        )
        val originalPensions = SampleDataGenerator.createDefaultPensions()
        val originalAssets = SampleDataGenerator.createDefaultAssets()
        val originalIncomes = SampleDataGenerator.createDefaultIncomes()
        val originalCalcSettings = CalculatorSettings(
            selectedMode = 2,
            accDepositType = 1,
            accMonthlyDeposit = 1_500_000L,
            wdWealth = 700_000_000L,
            wdMonthlyWithdrawal = 3_000_000L,
            depWealth = 400_000_000L,
            depWithdrawal = 2_800_000L,
            depGrowth = 5.5,
            reqExpense = 4_000_000L,
            beNormalAmount = 1_800_000L,
            taxContribution = 12_000_000L
        )

        val backupData = BackupData(
            version = 1,
            appName = "PensionAlchemy",
            backupDate = "2026-09-25 19:40:00",
            userProfile = originalProfile,
            pensions = originalPensions,
            assets = originalAssets,
            incomes = originalIncomes,
            calculatorSettings = originalCalcSettings
        )

        // 1. JSON 직렬화 (백업)
        val jsonString = json.encodeToString(backupData)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("PensionAlchemy"))
        assertTrue(jsonString.contains("18000000")) // 변경된 사적연금 한도

        // 2. JSON 역직렬화 (복구)
        val restoredBackup = json.decodeFromString<BackupData>(jsonString)
        assertEquals(backupData.version, restoredBackup.version)
        assertEquals(backupData.backupDate, restoredBackup.backupDate)

        // 3. 프로필 및 정책 변수 검증
        assertEquals(1980, restoredBackup.userProfile.birthYear)
        assertEquals(58, restoredBackup.userProfile.retirementAge)
        assertEquals(3_200_000L, restoredBackup.userProfile.monthlyExpenses)
        assertEquals(18_000_000L, restoredBackup.userProfile.policySettings.privatePensionAnnualLimit)
        assertEquals(5.2, restoredBackup.userProfile.policySettings.financialAssetReturnRate, 0.001)

        // 4. 연금 목록 검증
        assertEquals(originalPensions.size, restoredBackup.pensions.size)
        assertEquals(originalPensions[0].name, restoredBackup.pensions[0].name)
        assertEquals(originalPensions[0].expectedMonthlyAmount, restoredBackup.pensions[0].expectedMonthlyAmount)

        // 5. 자산 목록 검증
        assertEquals(originalAssets.size, restoredBackup.assets.size)
        assertEquals(originalAssets[0].name, restoredBackup.assets[0].name)
        assertEquals(originalAssets[0].currentValue, restoredBackup.assets[0].currentValue)

        // 6. 소득 목록 검증
        assertEquals(originalIncomes.size, restoredBackup.incomes.size)

        // 7. 계산기 사용자 설정값 검증
        assertEquals(2, restoredBackup.calculatorSettings.selectedMode)
        assertEquals(1, restoredBackup.calculatorSettings.accDepositType)
        assertEquals(1_500_000L, restoredBackup.calculatorSettings.accMonthlyDeposit)
        assertEquals(700_000_000L, restoredBackup.calculatorSettings.wdWealth)
        assertEquals(3_000_000L, restoredBackup.calculatorSettings.wdMonthlyWithdrawal)
        assertEquals(400_000_000L, restoredBackup.calculatorSettings.depWealth)
        assertEquals(2_800_000L, restoredBackup.calculatorSettings.depWithdrawal)
        assertEquals(5.5, restoredBackup.calculatorSettings.depGrowth, 0.001)
        assertEquals(4_000_000L, restoredBackup.calculatorSettings.reqExpense)
        assertEquals(1_800_000L, restoredBackup.calculatorSettings.beNormalAmount)
        assertEquals(12_000_000L, restoredBackup.calculatorSettings.taxContribution)
    }

    @Test
    fun testCalculatorViewModelStateSync() {
        val vm = CalculatorViewModel()

        // 사용자 임의 설정값 변경 시뮬레이션
        vm.selectedMode = 3
        vm.accInitialDeposit = 50_000_000L
        vm.wdGrowthRate = 7.5
        vm.depWealth = 600_000_000L
        vm.taxContribution = 15_000_000L
        vm.taxIsLowIncome = false

        // 백업을 위해 toSettings() 추출
        val savedSettings = vm.toSettings()
        assertEquals(3, savedSettings.selectedMode)
        assertEquals(50_000_000L, savedSettings.accInitialDeposit)
        assertEquals(7.5, savedSettings.wdGrowthRate, 0.001)
        assertEquals(600_000_000L, savedSettings.depWealth)
        assertEquals(15_000_000L, savedSettings.taxContribution)
        assertFalse(savedSettings.taxIsLowIncome)

        // 다른 새로운 뷰모델에 복원 적용 시뮬레이션
        val newVm = CalculatorViewModel()
        newVm.applySettings(savedSettings)

        assertEquals(3, newVm.selectedMode)
        assertEquals(50_000_000L, newVm.accInitialDeposit)
        assertEquals(7.5, newVm.wdGrowthRate, 0.001)
        assertEquals(600_000_000L, newVm.depWealth)
        assertEquals(15_000_000L, newVm.taxContribution)
        assertFalse(newVm.taxIsLowIncome)
    }
}
