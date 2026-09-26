/**
 * PensionTaxCalculator.js
 * 대한민국 연금소득 과세 체계 및 세후 실수령액 정밀 계산
 */

const PensionTaxCalculator = {
    /**
     * 국민연금/공적연금 연간 세액 계산 (연금소득공제 및 종합소득세율)
     */
    calculateAnnualNationalPensionTax(annualGrossAmount, policy = new PolicySettings()) {
        if (annualGrossAmount <= 0) return 0;

        // 법정 연금소득공제
        let pensionDeduction = 0;
        if (annualGrossAmount <= 3500000) {
            pensionDeduction = annualGrossAmount;
        } else if (annualGrossAmount <= 7000000) {
            pensionDeduction = Math.round(3500000 + (annualGrossAmount - 3500000) * 0.4);
        } else if (annualGrossAmount <= 14000000) {
            pensionDeduction = Math.round(4900000 + (annualGrossAmount - 7000000) * 0.2);
        } else {
            pensionDeduction = Math.min(policy.maxPensionIncomeDeduction, Math.round(6300000 + (annualGrossAmount - 14000000) * 0.1));
        }

        // 기본 인적공제
        const taxableBase = Math.max(0, annualGrossAmount - pensionDeduction - policy.basicPersonalDeduction);

        // 종합소득세율 (지방소득세 10% 가산)
        if (taxableBase <= 0) return 0;
        if (taxableBase <= 14000000) return Math.round(taxableBase * 0.066);
        if (taxableBase <= 50000000) return Math.round(924000 + (taxableBase - 14000000) * 0.165);
        if (taxableBase <= 88000000) return Math.round(6864000 + (taxableBase - 50000000) * 0.264);
        return Math.round(16896000 + (taxableBase - 88000000) * 0.385);
    },

    /**
     * 퇴직연금 월 세액 계산 (1~10년차 vs 11년차 이상 차등 감면율)
     */
    calculateMonthlyRetirementPensionTax(monthlyGrossAmount, yearsSinceStart, policy = new PolicySettings()) {
        if (monthlyGrossAmount <= 0) return 0;
        const discountRate = (yearsSinceStart <= 10) 
            ? (policy.retirementTaxDiscountRateEarly / 100.0) 
            : (policy.retirementTaxDiscountRateLate / 100.0);
        const effectiveRate = (policy.baseRetirementTaxRate / 100.0) * (1.0 - discountRate);
        return Math.round(monthlyGrossAmount * effectiveRate);
    },

    /**
     * 개인연금저축(세제적격) 연간 세액 계산
     */
    calculateAnnualPersonalPensionTax(annualGrossAmount, age, policy = new PolicySettings()) {
        if (annualGrossAmount <= 0) return 0;

        let baseRate = 0;
        if (age < 70) baseRate = policy.privatePensionRateUnder70 / 100.0;
        else if (age < 80) baseRate = policy.privatePensionRateUnder80 / 100.0;
        else baseRate = policy.privatePensionRate80OrOver / 100.0;

        if (annualGrossAmount <= policy.privatePensionAnnualLimit) {
            return Math.round(annualGrossAmount * baseRate);
        } else {
            const baseTax = Math.round(policy.privatePensionAnnualLimit * baseRate);
            const excessRate = policy.privatePensionExcessRate / 100.0;
            const excessTax = Math.round((annualGrossAmount - policy.privatePensionAnnualLimit) * excessRate);
            return baseTax + excessTax;
        }
    },

    /**
     * 개인연금보험: 10년 이상 유지 시 비과세 0%
     */
    calculateMonthlyAnnuityInsuranceTax() {
        return 0;
    },

    /**
     * 주택연금: 역모기지 대출금 성격으로 비과세 0%
     */
    calculateMonthlyHousingPensionTax() {
        return 0;
    },

    /**
     * 기타 사적연금: 기본 5.5% 분리과세
     */
    calculateMonthlyOtherPensionTax(monthlyGrossAmount) {
        if (monthlyGrossAmount <= 0) return 0;
        return Math.round(monthlyGrossAmount * 0.055);
    }
};
