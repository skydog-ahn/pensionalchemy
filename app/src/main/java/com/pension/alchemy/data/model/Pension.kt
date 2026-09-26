package com.pension.alchemy.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class PensionType(val displayName: String, val tierDescription: String) {
    NATIONAL("국민/공적연금", "1층 기본보장"),
    RETIREMENT("퇴직연금/IRP", "2층 표준보장"),
    PERSONAL("개인연금저축", "3층 세제적격연금"),
    ANNUITY_INSURANCE("개인연금보험", "3층 비과세연금"),
    HOUSING("주택연금", "역모기지 보장"),
    OTHER("기타연금", "기타 사적연금")
}

@Serializable
data class Pension(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: PensionType,
    val startAge: Int = 65,
    val endAge: Int = 100,
    val expectedMonthlyAmount: Long = 0L, // 원 단위
    val expectedGrowthRate: Double = 0.0, // 연간 운용수익률 / 물가연동률 (%)
    val currentBalance: Long = 0L,        // 현재 적립 잔액 (원)
    val monthlyContribution: Long = 0L,   // 월 추가 납입액 (원)
    val contributionEndAge: Int = 60,     // 납입 종료 나이
    val claimOffsetYears: Int = 0,        // 국민연금 조기/연기 수령 (-5년 ~ +5년)
    val isTaxDeductionEligible: Boolean = true // 세액공제 대상 여부 (연금저축, IRP)
)