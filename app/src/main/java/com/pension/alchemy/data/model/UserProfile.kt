package com.pension.alchemy.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class UserProfile(
    val birthYear: Int = 1985,
    val retirementAge: Int = 60,
    val targetEndAge: Int = 100,
    val monthlyExpenses: Long = 2_500_000L, // 은퇴 후 월 희망 생활비 (현재 가치 기준, 시뮬레이션 시 물가상승률 복리 자동 환산)
    val medicalExpenseRatio: Double = 0.10, // 의료비 비중 (물가+3% 가중 적용)
    val inflationRate: Double = 2.0,        // 연간 물가상승률 (%)
    val currency: String = "KRW",
    val policySettings: PolicySettings = PolicySettings()
) {
    val currentAge: Int get() = LocalDate.now().year - birthYear
}