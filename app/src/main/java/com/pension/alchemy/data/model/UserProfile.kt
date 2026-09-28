package com.pension.alchemy.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class UserProfile(
    val birthYear: Int = 1985,
    val retirementAge: Int = 60,
    val targetEndAge: Int = 100,
    val currentMonthlyExpenses: Long = 3_000_000L, // 현재 월 생활 소비 지출액 (현재가치 기준, 은퇴 전까지 물가상승 반영)
    val monthlyExpenses: Long = 2_500_000L,        // 은퇴 후 월 희망 생활비 (현재가치 기준, 은퇴 시점 물가상승 복리 자동 환산)
    val medicalExpenseRatio: Double = 0.10,        // 의료비 비중 (물가+3% 가중 적용)
    val inflationRate: Double = 2.0,               // 연간 물가상승률 (%)
    val currency: String = "KRW",
    val policySettings: PolicySettings = PolicySettings()
) {
    val currentAge: Int get() = LocalDate.now().year - birthYear

    /**
     * 은퇴 시점(retirementAge)의 물가상승률 복리 환산 예상 월 필요 생활비(미래가치) 계산
     */
    fun calculateFutureRetirementExpense(): Long {
        val years = (retirementAge - currentAge).coerceAtLeast(0)
        val rate = inflationRate / 100.0
        return (monthlyExpenses * Math.pow(1.0 + rate, years.toDouble())).toLong()
    }
}