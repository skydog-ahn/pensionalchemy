package com.pension.alchemy.domain.engine

import com.pension.alchemy.data.model.RetirementHealthScore

object RetirementHealthAnalyzer {
    fun evaluate(
        incomeReplacementRate: Double,
        depletionAge: Int,
        targetEndAge: Int,
        hasCrevasseShortfall: Boolean,
        hasRealEstate: Boolean,
        hasFinancialAssets: Boolean,
        hasPensionAssets: Boolean
    ): RetirementHealthScore {
        // 1. 소득대체율 점수 (30점 만점)
        // 60% 이상: 30점, 50~59%: 25점, 40~49%: 20점, 30~39%: 14점, 미만: 8점
        val replacementScore = when {
            incomeReplacementRate >= 60.0 -> 30
            incomeReplacementRate >= 50.0 -> 25
            incomeReplacementRate >= 40.0 -> 20
            incomeReplacementRate >= 30.0 -> 14
            else -> 8
        }

        // 2. 장수 자산 안전성 점수 (40점 만점)
        // 고갈 없음(0): 40점, 95세 이상 고갈: 34점, 85~94세: 24점, 75~84세: 12점, 75세 미만: 5점
        val longevityScore = when {
            depletionAge == 0 -> 40
            depletionAge >= 95 -> 34
            depletionAge >= 85 -> 24
            depletionAge >= 75 -> 12
            else -> 5
        }

        // 3. 소득 크레바스 방어력 점수 (20점 만점)
        val crevasseScore = if (!hasCrevasseShortfall) 20 else 8

        // 4. 자산 다각화 점수 (10점 만점)
        var diversification = 0
        if (hasFinancialAssets) diversification += 4
        if (hasPensionAssets) diversification += 4
        if (hasRealEstate) diversification += 2
        val diversificationScore = diversification.coerceAtMost(10)

        val totalScore = (replacementScore + longevityScore + crevasseScore + diversificationScore).coerceIn(0, 100)

        val (grade, colorHex, message) = when {
            totalScore >= 90 -> Triple(
                "최우수 (S)",
                "#10B981", // Emerald
                "완벽에 가까운 은퇴 준비 상태입니다! 3층 연금과 자산이 안정적인 황금빛 노후 현금흐름을 창출하고 있습니다."
            )
            totalScore >= 80 -> Triple(
                "우수 (A)",
                "#3B82F6", // Blue
                "매우 훌륭한 노후 플랜을 보유하고 있습니다. 물가 상승 및 의료비 변수를 지속적으로 점검하세요."
            )
            totalScore >= 70 -> Triple(
                "양호 (B)",
                "#F59E0B", // Amber
                "기본적인 노후 안전망이 갖추어졌으나, 소득 공백기(크레바스)나 고령기 의료비 대비 추가 보강이 권장됩니다."
            )
            totalScore >= 50 -> Triple(
                "주의 (C)",
                "#F97316", // Orange
                "은퇴 후 생활비 부족 또는 조기 자산 고갈 위험이 있습니다. 개인연금 납입액 증액이나 은퇴 연기 검토가 필요합니다."
            )
            else -> Triple(
                "위험 (D)",
                "#EF4444", // Red
                "은퇴 후 심각한 재정 적자가 예상됩니다. 1~3층 연금 재설계와 부채 감축, 긴급 은퇴 플랜 수립이 시급합니다."
            )
        }

        return RetirementHealthScore(
            score = totalScore,
            grade = grade,
            gradeColorHex = colorHex,
            feedbackMessage = message,
            replacementRateScore = replacementScore,
            longevitySafetyScore = longevityScore,
            crevasseDefenseScore = crevasseScore,
            assetDiversificationScore = diversificationScore
        )
    }
}