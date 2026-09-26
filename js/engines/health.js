/**
 * RetirementHealthAnalyzer.js
 * 은퇴 준비 건강도 점수 (0 ~ 100점) 및 S/A/B/C/D 등급 분석
 */

const RetirementHealthAnalyzer = {
    evaluate({
        incomeReplacementRate,
        depletionAge,
        targetEndAge,
        hasCrevasseShortfall,
        hasRealEstate,
        hasFinancialAssets,
        hasPensionAssets
    }) {
        // 1. 소득대체율 점수 (30점 만점)
        let replacementScore = 8;
        if (incomeReplacementRate >= 60.0) replacementScore = 30;
        else if (incomeReplacementRate >= 50.0) replacementScore = 25;
        else if (incomeReplacementRate >= 40.0) replacementScore = 20;
        else if (incomeReplacementRate >= 30.0) replacementScore = 14;

        // 2. 장수 자산 안전성 점수 (40점 만점)
        let longevityScore = 5;
        if (depletionAge === 0) longevityScore = 40;
        else if (depletionAge >= 95) longevityScore = 34;
        else if (depletionAge >= 85) longevityScore = 24;
        else if (depletionAge >= 75) longevityScore = 12;

        // 3. 소득 크레바스 방어력 점수 (20점 만점)
        const crevasseScore = !hasCrevasseShortfall ? 20 : 8;

        // 4. 자산 다각화 점수 (10점 만점)
        let diversification = 0;
        if (hasFinancialAssets) diversification += 4;
        if (hasPensionAssets) diversification += 4;
        if (hasRealEstate) diversification += 2;
        const diversificationScore = Math.min(10, diversification);

        const totalScore = Math.min(100, Math.max(0, replacementScore + longevityScore + crevasseScore + diversificationScore));

        let grade = "위험 (D)";
        let gradeColorHex = "#EF4444";
        let feedbackMessage = "은퇴 후 심각한 재정 적자가 예상됩니다. 1~3층 연금 재설계와 부채 감축, 긴급 은퇴 플랜 수립이 시급합니다.";

        if (totalScore >= 90) {
            grade = "최우수 (S)";
            gradeColorHex = "#10B981"; // Emerald
            feedbackMessage = "완벽에 가까운 은퇴 준비 상태입니다! 3층 연금과 자산이 안정적인 황금빛 노후 현금흐름을 창출하고 있습니다.";
        } else if (totalScore >= 80) {
            grade = "우수 (A)";
            gradeColorHex = "#3B82F6"; // Blue
            feedbackMessage = "매우 훌륭한 노후 플랜을 보유하고 있습니다. 물가 상승 및 의료비 변수를 지속적으로 점검하세요.";
        } else if (totalScore >= 70) {
            grade = "양호 (B)";
            gradeColorHex = "#F59E0B"; // Amber
            feedbackMessage = "기본적인 노후 안전망이 갖추어졌으나, 소득 공백기(크레바스)나 고령기 의료비 대비 추가 보강이 권장됩니다.";
        } else if (totalScore >= 50) {
            grade = "주의 (C)";
            gradeColorHex = "#F97316"; // Orange
            feedbackMessage = "은퇴 후 생활비 부족 또는 조기 자산 고갈 위험이 있습니다. 개인연금 납입액 증액이나 은퇴 연기 검토가 필요합니다.";
        }

        return {
            score: totalScore,
            grade,
            gradeColorHex,
            feedbackMessage,
            replacementRateScore: replacementScore,
            longevitySafetyScore: longevityScore,
            crevasseDefenseScore: crevasseScore,
            assetDiversificationScore: diversificationScore
        };
    }
};
