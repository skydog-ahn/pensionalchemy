package com.pension.alchemy.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.pension.alchemy.domain.engine.CalculatorsEngine
import com.pension.alchemy.util.CurrencyFormatter

class CalculatorViewModel : ViewModel() {

    var selectedMode by mutableIntStateOf(0)

    // ──────────────────────────────────────────────
    // 1. 적립 및 예탁 미래가치 (Accumulation & Lump Sum)
    // ──────────────────────────────────────────────
    // 0: 혼합(목돈 예탁 + 매월 적립), 1: 적립식만(매월 납입), 2: 예탁식만(목돈 일시 거치)
    var accDepositType by mutableIntStateOf(0)

    var accInitialDeposit by mutableLongStateOf(20_000_000L) // 초기 예탁금
    var accInitialDepositMin by mutableLongStateOf(0L)
    var accInitialDepositMax by mutableLongStateOf(500_000_000L)

    var accMonthlyDeposit by mutableLongStateOf(1_000_000L) // 월 적립액
    var accMonthlyDepositMin by mutableLongStateOf(0L)
    var accMonthlyDepositMax by mutableLongStateOf(10_000_000L)

    var accPeriodYears by mutableIntStateOf(15) // 적립/거치 기간 (년)
    var accPeriodYearsMin by mutableIntStateOf(1)
    var accPeriodYearsMax by mutableIntStateOf(50)

    var accInterestRate by mutableDoubleStateOf(6.0) // 연간 기대수익률 (%)
    var accInterestRateMin by mutableDoubleStateOf(0.5)
    var accInterestRateMax by mutableDoubleStateOf(20.0)

    // ──────────────────────────────────────────────
    // 2. 자산 인출 시뮬레이션 (Withdrawal)
    // ──────────────────────────────────────────────
    var wdWealth by mutableLongStateOf(500_000_000L) // 시작 자산
    var wdWealthMin by mutableLongStateOf(50_000_000L)
    var wdWealthMax by mutableLongStateOf(3_000_000_000L)

    var wdMonthlyWithdrawal by mutableLongStateOf(2_500_000L) // 월 인출액
    var wdMonthlyWithdrawalMin by mutableLongStateOf(500_000L)
    var wdMonthlyWithdrawalMax by mutableLongStateOf(20_000_000L)

    var wdGrowthRate by mutableDoubleStateOf(5.0) // 운용 수익률 (%)
    var wdGrowthRateMin by mutableDoubleStateOf(0.5)
    var wdGrowthRateMax by mutableDoubleStateOf(15.0)

    // ──────────────────────────────────────────────
    // 3. 자산 고갈 타이머 (Depletion)
    // ──────────────────────────────────────────────
    var depWealth by mutableLongStateOf(300_000_000L) // 보유 자산
    var depWealthMin by mutableLongStateOf(30_000_000L)
    var depWealthMax by mutableLongStateOf(3_000_000_000L)

    var depWithdrawal by mutableLongStateOf(2_500_000L) // 월 인출액
    var depWithdrawalMin by mutableLongStateOf(500_000L)
    var depWithdrawalMax by mutableLongStateOf(20_000_000L)

    // 고갈 타이머 수익률 (요구사항 6)
    var depGrowth by mutableDoubleStateOf(4.0) // 운용 수익률 (%)
    var depGrowthMin by mutableDoubleStateOf(0.0)
    var depGrowthMax by mutableDoubleStateOf(15.0)

    var depInflation by mutableDoubleStateOf(2.0) // 물가상승률 (%)
    var depInflationMin by mutableDoubleStateOf(0.0)
    var depInflationMax by mutableDoubleStateOf(10.0)

    // ──────────────────────────────────────────────
    // 4. 목표 필요자산 역산기 (Required Wealth)
    // ──────────────────────────────────────────────
    var reqExpense by mutableLongStateOf(3_000_000L) // 목표 희망 월 생활비
    var reqExpenseMin by mutableLongStateOf(1_000_000L)
    var reqExpenseMax by mutableLongStateOf(20_000_000L)

    var reqYears by mutableIntStateOf(30) // 인출 유지 기간 (년)
    var reqYearsMin by mutableIntStateOf(5)
    var reqYearsMax by mutableIntStateOf(60)

    var reqReturnRate by mutableDoubleStateOf(4.0) // 기대 운용 수익률 (%)
    var reqReturnRateMin by mutableDoubleStateOf(1.0)
    var reqReturnRateMax by mutableDoubleStateOf(15.0)

    // ──────────────────────────────────────────────
    // 5. 국민연금 손익분기점 (Break-even)
    // ──────────────────────────────────────────────
    var beNormalAmount by mutableLongStateOf(1_500_000L) // 65세 정상수령 기준 월 예상액
    var beNormalAmountMin by mutableLongStateOf(300_000L)
    var beNormalAmountMax by mutableLongStateOf(5_000_000L)

    // ──────────────────────────────────────────────
    // 6. 절세 연금술사 (Tax Benefit)
    // ──────────────────────────────────────────────
    var taxContribution by mutableLongStateOf(9_000_000L) // 연간 납입액
    var taxContributionMin by mutableLongStateOf(500_000L)
    var taxContributionMax by mutableLongStateOf(18_000_000L)
    var taxIsLowIncome by mutableStateOf(true) // 총급여 5,500만원 이하 (16.5% vs 13.2%)

    // ──────────────────────────────────────────────
    // 설정값 변환 및 복원 (백업/복구 및 영속화 지원)
    // ──────────────────────────────────────────────
    fun toSettings(): com.pension.alchemy.data.model.CalculatorSettings {
        return com.pension.alchemy.data.model.CalculatorSettings(
            selectedMode = selectedMode,
            accDepositType = accDepositType,
            accInitialDeposit = accInitialDeposit,
            accMonthlyDeposit = accMonthlyDeposit,
            accPeriodYears = accPeriodYears,
            accInterestRate = accInterestRate,
            wdWealth = wdWealth,
            wdMonthlyWithdrawal = wdMonthlyWithdrawal,
            wdGrowthRate = wdGrowthRate,
            depWealth = depWealth,
            depWithdrawal = depWithdrawal,
            depGrowth = depGrowth,
            depInflation = depInflation,
            reqExpense = reqExpense,
            reqYears = reqYears,
            reqReturnRate = reqReturnRate,
            beNormalAmount = beNormalAmount,
            taxContribution = taxContribution,
            taxIsLowIncome = taxIsLowIncome
        )
    }

    fun applySettings(settings: com.pension.alchemy.data.model.CalculatorSettings) {
        selectedMode = settings.selectedMode
        accDepositType = settings.accDepositType
        accInitialDeposit = settings.accInitialDeposit
        accMonthlyDeposit = settings.accMonthlyDeposit
        accPeriodYears = settings.accPeriodYears
        accInterestRate = settings.accInterestRate
        wdWealth = settings.wdWealth
        wdMonthlyWithdrawal = settings.wdMonthlyWithdrawal
        wdGrowthRate = settings.wdGrowthRate
        depWealth = settings.depWealth
        depWithdrawal = settings.depWithdrawal
        depGrowth = settings.depGrowth
        depInflation = settings.depInflation
        reqExpense = settings.reqExpense
        reqYears = settings.reqYears
        reqReturnRate = settings.reqReturnRate
        beNormalAmount = settings.beNormalAmount
        taxContribution = settings.taxContribution
        taxIsLowIncome = settings.taxIsLowIncome
    }

    /**
     * 클립보드 복사 헬퍼
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "계산 결과가 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "클립보드 복사에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}

