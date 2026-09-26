package com.pension.alchemy.data.model

import kotlinx.serialization.Serializable

/**
 * 대한민국 연금 및 세제 정책 변수 설정 모델
 * 정부 정책이나 세법, 건강보험 제도의 개정에 따라 사용자가 언제든 직접 조정할 수 있습니다.
 */
@Serializable
data class PolicySettings(
    // ──────────────────────────────────────────────
    // 1. 공적/사적연금 세법 한도 (원 단위)
    // ──────────────────────────────────────────────
    val privatePensionAnnualLimit: Long = 15_000_000L,        // 2024 개정 사적연금 저율 분리과세 한도 (기본 1,500만원)
    val healthInsurancePensionLimit: Long = 20_000_000L,      // 건강보험 피부양자 탈락 공적연금 소득 한도 (기본 2,000만원)
    val maxPensionIncomeDeduction: Long = 9_000_000L,         // 공적연금 연금소득공제 최대 한도 (기본 900만원)
    val basicPersonalDeduction: Long = 1_500_000L,            // 1인 기본 인적공제 (기본 150만원)

    // ──────────────────────────────────────────────
    // 2. 사적연금 인출 연령별 세율 & 초과 세율 (%)
    // ──────────────────────────────────────────────
    val privatePensionRateUnder70: Double = 5.5,              // 만 55~69세 사적연금 세율 (기본 5.5%)
    val privatePensionRateUnder80: Double = 4.4,              // 만 70~79세 사적연금 세율 (기본 4.4%)
    val privatePensionRate80OrOver: Double = 3.3,             // 만 80세 이상 사적연금 세율 (기본 3.3%)
    val privatePensionExcessRate: Double = 16.5,              // 사적연금 한도 초과분 분리과세율 (기본 16.5%)

    // ──────────────────────────────────────────────
    // 3. 퇴직연금(IRP) 분할인출 감면율 & 기준 퇴직소득세율 (%)
    // ──────────────────────────────────────────────
    val retirementTaxDiscountRateEarly: Double = 30.0,        // 수령 1~10년차 퇴직소득세 감면율 (기본 30%)
    val retirementTaxDiscountRateLate: Double = 40.0,         // 수령 11년차 이상 퇴직소득세 감면율 (기본 40%)
    val baseRetirementTaxRate: Double = 5.0,                  // 기준 평균 퇴직소득세율 (기본 5.0%)

    // ──────────────────────────────────────────────
    // 4. 연금저축/IRP 세액공제 납입한도 & 공제율
    // ──────────────────────────────────────────────
    val maxTaxCreditContribution: Long = 9_000_000L,          // 연간 세액공제 납입한도 (기본 900만원)
    val lowIncomeSalaryThreshold: Long = 55_000_000L,         // 우대세율 기준 총급여 한도 (기본 5,500만원)
    val taxCreditRateLowIncome: Double = 16.5,                // 총급여 5,500만 이하 세액공제율 (기본 16.5%)
    val taxCreditRateHighIncome: Double = 13.2,               // 총급여 5,500만 초과 세액공제율 (기본 13.2%)
    val generalInterestTaxRate: Double = 15.4,                // 일반계좌 이자/배당소득세율 (기본 15.4%)

    // ──────────────────────────────────────────────
    // 5. 국민연금 조기/연기 수령 연간 조정률 (%/년)
    // ──────────────────────────────────────────────
    val nationalEarlyReductionRatePerYear: Double = 6.0,      // 국민연금 조기수령 연 감액률 (기본 6.0%/년)
    val nationalDelayIncreaseRatePerYear: Double = 7.2,       // 국민연금 연기수령 연 증액률 (기본 7.2%/년)

    // ──────────────────────────────────────────────
    // 6. 시뮬레이션 기본 자산수익률 및 의료비 가중률 (%/년)
    // ──────────────────────────────────────────────
    val financialAssetReturnRate: Double = 4.5,               // 유동 금융자산 보수적 기대수익률 (기본 4.5%)
    val realEstateGrowthRate: Double = 2.0,                   // 부동산 보수적 자연성장률 (기본 2.0%)
    val medicalInflationSurcharge: Double = 3.0,              // 노후 의료비 물가 추가 가중률 (기본 3.0%)

    // ──────────────────────────────────────────────
    // 7. 대한민국 순자산 분포 모수 (로그정규분포, 단위: 억원)
    // ──────────────────────────────────────────────
    val wealthDistributionMean: Double = 1.00984,             // 2025년 기준 로그정규분포 평균 모수 (mu)
    val wealthDistributionStdDev: Double = 1.1937             // 2025년 기준 로그정규분포 표준편차 모수 (sigma)
)
