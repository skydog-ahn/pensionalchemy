/**
 * LoanCalculator.js
 * 부채 상환 방식(원리금균등, 원금균등, 만기일시, 거치식) 계산기
 */

const LoanCalculator = {
    calculateMonthlyPayment(principal, annualRatePercent, maturityYears, repaymentMethodKey) {
        if (principal <= 0 || maturityYears <= 0) return 0;
        const monthlyRate = (annualRatePercent / 100.0) / 12.0;
        const totalMonths = maturityYears * 12;

        switch (repaymentMethodKey) {
            case 'EQUAL_PRINCIPAL_AND_INTEREST': {
                if (monthlyRate <= 0.0) {
                    return Math.round(principal / totalMonths);
                }
                const factor = Math.pow(1.0 + monthlyRate, totalMonths);
                return Math.round(principal * (monthlyRate * factor) / (factor - 1.0));
            }
            case 'EQUAL_PRINCIPAL': {
                const monthlyPrincipal = principal / totalMonths;
                const firstMonthInterest = principal * monthlyRate;
                return Math.round(monthlyPrincipal + firstMonthInterest);
            }
            case 'BULLET':
            case 'INTEREST_ONLY':
            default:
                return Math.round(principal * monthlyRate);
        }
    },

    calculateAnnualRepayment(currentBalance, originalPrincipal, annualRatePercent, maturityYears, repaymentMethodKey, yearIndex) {
        if (currentBalance <= 0) {
            return { principalPayment: 0, interestPayment: 0, remainingBalance: 0 };
        }

        const rate = Math.max(0.0, annualRatePercent / 100.0);
        const annualInterest = Math.round(currentBalance * rate);

        switch (repaymentMethodKey) {
            case 'EQUAL_PRINCIPAL_AND_INTEREST': {
                if (yearIndex >= maturityYears) {
                    return { principalPayment: currentBalance, interestPayment: annualInterest, remainingBalance: 0 };
                }
                const monthlyPayment = this.calculateMonthlyPayment(originalPrincipal, annualRatePercent, maturityYears, repaymentMethodKey);
                const annualTotalPayment = monthlyPayment * 12;
                const calculatedPrincipalPayment = Math.max(0, annualTotalPayment - annualInterest);
                const actualPrincipalPayment = Math.min(currentBalance, calculatedPrincipalPayment);
                const finalPrincipalPayment = (yearIndex === maturityYears - 1) ? currentBalance : actualPrincipalPayment;
                const remaining = Math.max(0, currentBalance - finalPrincipalPayment);
                return { principalPayment: finalPrincipalPayment, interestPayment: annualInterest, remainingBalance: remaining };
            }
            case 'EQUAL_PRINCIPAL': {
                if (yearIndex >= maturityYears - 1) {
                    return { principalPayment: currentBalance, interestPayment: annualInterest, remainingBalance: 0 };
                }
                const annualPrincipal = Math.round(originalPrincipal / Math.max(1, maturityYears));
                const actualPrincipal = Math.min(currentBalance, annualPrincipal);
                const remaining = Math.max(0, currentBalance - actualPrincipal);
                return { principalPayment: actualPrincipal, interestPayment: annualInterest, remainingBalance: remaining };
            }
            case 'BULLET': {
                if (yearIndex >= maturityYears - 1) {
                    return { principalPayment: currentBalance, interestPayment: annualInterest, remainingBalance: 0 };
                }
                return { principalPayment: 0, interestPayment: annualInterest, remainingBalance: currentBalance };
            }
            case 'INTEREST_ONLY':
            default:
                return { principalPayment: 0, interestPayment: annualInterest, remainingBalance: currentBalance };
        }
    }
};
