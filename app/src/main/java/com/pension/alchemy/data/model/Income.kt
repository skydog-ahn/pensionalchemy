package com.pension.alchemy.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class IncomeType(val displayName: String) {
    SALARY("근로소득"),
    BONUS("상여/성과급"),
    BUSINESS("사업소득"),
    RENTAL("임대소득"),
    OTHER("기타소득")
}

@Serializable
data class Income(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: IncomeType = IncomeType.SALARY,
    val monthlyAmount: Long = 0L, // 월 소득 (원)
    val endAge: Int = 60,         // 소득 종료 나이 (은퇴 시기)
    val expectedGrowthRate: Double = 2.0, // 임금/소득 상승률 (%)
    // 소득 입력/기준 일자 (YYYY-MM-DD)
    val baseDate: String = ""
) {
    val effectiveBaseDate: String
        get() = if (baseDate.isNotBlank()) baseDate else java.time.LocalDate.now().toString()
}