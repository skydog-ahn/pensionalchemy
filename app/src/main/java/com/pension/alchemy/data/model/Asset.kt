package com.pension.alchemy.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class AssetType(val displayName: String, val isLiability: Boolean) {
    DEPOSIT("예금", false),
    SAVINGS("적금", false),
    STOCK("주식", false),
    BOND("채권", false),
    ETF("ETF/펀드", false),
    COMMODITY("원자재(금 등)", false),
    REAL_ESTATE("부동산", false),
    CRYPTO("가상자산", false),
    DEBT("대출/부채", true),
    OTHER("기타 자산", false)
}

/**
 * 부채 상환 방식
 */
@Serializable
enum class RepaymentMethod(val displayName: String, val shortDescription: String) {
    EQUAL_PRINCIPAL_AND_INTEREST("원리금균등", "매월 원금+이자 균등 분할 상환"),
    EQUAL_PRINCIPAL("원금균등", "매월 동일한 원금 상환 (이자 점차 감소)"),
    BULLET("만기일시", "기간 중 이자만 납부 후 만기에 일시 상환"),
    INTEREST_ONLY("거치(이자만)", "원금 상환 없이 매월 이자만 지출")
}

@Serializable
data class Asset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: AssetType,
    val currentValue: Long = 0L, // 원 단위 (부채인 경우 대출 잔액)
    val expectedGrowthRate: Double = 3.0, // 연간 기대수익률 or 대출금리 (%)
    val currency: String = "KRW",
    // 부채(대출) 상환 관련 필드
    val repaymentMethod: RepaymentMethod = RepaymentMethod.EQUAL_PRINCIPAL_AND_INTEREST,
    val maturityYears: Int = 10 // 잔여 상환 기간 (년)
) {
    val isLiability: Boolean get() = type.isLiability
}