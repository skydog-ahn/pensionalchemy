package com.pension.alchemy.data.model

import kotlinx.serialization.Serializable

/**
 * 계산기 화면(CalculatorScreen)의 사용자 입력 및 설정값 모델
 * 앱 종료 후 재실행 시에도 유지되며, 전체 데이터 백업 및 복구 대상에 포함됩니다.
 */
@Serializable
data class CalculatorSettings(
    val selectedMode: Int = 0,

    // ──────────────────────────────────────────────
    // 1. 적립 및 예탁 미래가치 (Accumulation)
    // ──────────────────────────────────────────────
    val accDepositType: Int = 0,               // 0: 혼합, 1: 적립식, 2: 예탁식
    val accInitialDeposit: Long = 20_000_000L, // 초기 예탁금 (원)
    val accMonthlyDeposit: Long = 1_000_000L,  // 월 적립액 (원)
    val accPeriodYears: Int = 15,              // 적립/거치 기간 (년)
    val accInterestRate: Double = 6.0,         // 연간 기대수익률 (%)

    // ──────────────────────────────────────────────
    // 2. 자산 인출 시뮬레이션 (Withdrawal)
    // ──────────────────────────────────────────────
    val wdWealth: Long = 500_000_000L,         // 시작 자산 (원)
    val wdMonthlyWithdrawal: Long = 2_500_000L,// 월 인출액 (원)
    val wdGrowthRate: Double = 5.0,            // 운용 수익률 (%)

    // ──────────────────────────────────────────────
    // 3. 자산 고갈 타이머 (Depletion)
    // ──────────────────────────────────────────────
    val depWealth: Long = 300_000_000L,        // 보유 자산 (원)
    val depWithdrawal: Long = 2_500_000L,      // 월 인출액 (원)
    val depGrowth: Double = 4.0,               // 운용 수익률 (%)
    val depInflation: Double = 2.0,            // 물가상승률 (%)

    // ──────────────────────────────────────────────
    // 4. 목표 필요자산 역산기 (Required Wealth)
    // ──────────────────────────────────────────────
    val reqExpense: Long = 3_000_000L,         // 목표 희망 월 생활비 (원)
    val reqYears: Int = 30,                    // 인출 유지 기간 (년)
    val reqReturnRate: Double = 4.0,           // 기대 운용 수익률 (%)

    // ──────────────────────────────────────────────
    // 5. 국민연금 손익분기점 (Break-even)
    // ──────────────────────────────────────────────
    val beNormalAmount: Long = 1_500_000L,     // 65세 정상수령 기준 월 예상액 (원)

    // ──────────────────────────────────────────────
    // 6. 절세 연금술사 (Tax Benefit)
    // ──────────────────────────────────────────────
    val taxContribution: Long = 9_000_000L,    // 연간 납입액 (원)
    val taxIsLowIncome: Boolean = true         // 총급여 5,500만원 이하 (우대세율 여부)
)
