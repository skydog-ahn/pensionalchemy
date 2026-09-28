/**
 * CalculatorsEngine.js
 * 6대 전문 재무 계산기 엔진
 * 1. 적립·예탁 미래가치
 * 2. 자산 인출 시뮬레이션
 * 3. 자산 고갈 타이머
 * 4. 목표 필요자산 역산
 * 5. 국민연금 조기/연기 손익분기
 * 6. 연금저축/IRP 절세 연금술사
 */

const CalculatorsEngine = {
    /**
     * 1. 적립 및 예탁 미래가치 계산
     */
    calculateAccumulation(monthlyDeposit, periodYears, annualInterestRate, initialDeposit = 0) {
        const r = (annualInterestRate / 100.0) / 12.0;
        const totalMonths = periodYears * 12;
        let currentBalance = Number(initialDeposit);
        let totalPrincipal = Number(initialDeposit);
        const yearlyList = [];

        for (let m = 1; m <= totalMonths; m++) {
            currentBalance = (currentBalance + Number(monthlyDeposit)) * (1.0 + r);
            totalPrincipal += Number(monthlyDeposit);

            if (m % 12 === 0) {
                const year = m / 12;
                const bal = Math.round(currentBalance);
                yearlyList.push({
                    year,
                    totalDeposit: totalPrincipal,
                    totalInterest: Math.max(0, bal - totalPrincipal),
                    balance: bal
                });
            }
        }

        const futureValue = Math.round(currentBalance);
        return {
            futureValue,
            initialDeposit,
            totalMonthlyDeposits: monthlyDeposit * totalMonths,
            totalPrincipal,
            totalInterest: Math.max(0, futureValue - totalPrincipal),
            yearlyBreakdown: yearlyList
        };
    },

    /**
     * 2. 자산 인출 시뮬레이션
     */
    calculateWithdrawal(startingWealth, annualGrowthRate, isFixedAmount, monthlyWithdrawal, annualWithdrawalRate, startAge = 60, yearsToSimulate = 35) {
        let balance = Number(startingWealth);
        const yearlyList = [];
        let isDepleted = false;
        let depletionAge = 0;
        const r = annualGrowthRate / 100.0;

        for (let y = 1; y <= yearsToSimulate; y++) {
            const age = startAge + y - 1;
            const startBal = balance;

            if (balance <= 0) {
                if (!isDepleted) {
                    isDepleted = true;
                    depletionAge = age;
                }
                yearlyList.push({
                    age,
                    year: y,
                    startingBalance: 0,
                    growthAmount: 0,
                    withdrawalAmount: 0,
                    endingBalance: 0
                });
                continue;
            }

            const withdrawalForYear = isFixedAmount 
                ? Number(monthlyWithdrawal) * 12 
                : startBal * (annualWithdrawalRate / 100.0);

            const growth = startBal * r;
            balance = startBal + growth - withdrawalForYear;

            if (balance < 0) {
                balance = 0;
                if (!isDepleted) {
                    isDepleted = true;
                    depletionAge = age;
                }
            }

            yearlyList.push({
                age,
                year: y,
                startingBalance: Math.round(startBal),
                growthAmount: Math.round(growth),
                withdrawalAmount: Math.round(withdrawalForYear),
                endingBalance: Math.round(balance)
            });
        }

        return {
            isDepleted,
            depletionAge,
            finalBalance: Math.round(balance),
            yearlyBreakdown: yearlyList
        };
    },

    /**
     * 3. 자산 고갈 타이머
     */
    calculateDepletion(currentWealth, monthlyWithdrawal, annualGrowthRate, inflationRate, currentAge = 60) {
        const annualWithdrawal = monthlyWithdrawal * 12.0;
        const netRate = (annualGrowthRate - inflationRate) / 100.0;

        // 영구 안전 보존 케이스
        if (netRate > 0 && (currentWealth * netRate) >= annualWithdrawal) {
            return {
                depletionYears: 999.0,
                depletionAge: 999.0,
                isForeverSafe: true,
                recommendedSafeMonthlyWithdrawal: Math.round((currentWealth * netRate) / 12.0)
            };
        }

        let bal = Number(currentWealth);
        let months = 0;
        const monthlyRate = netRate / 12.0;

        while (bal > 0 && months < 1200) {
            bal = (bal * (1.0 + monthlyRate)) - Number(monthlyWithdrawal);
            months++;
        }

        const years = months / 12.0;
        const safeMonthly = (netRate > 0) 
            ? Math.round((currentWealth * netRate) / 12.0) 
            : Math.round(currentWealth / (30 * 12));

        return {
            depletionYears: years,
            depletionAge: currentAge + years,
            isForeverSafe: false,
            recommendedSafeMonthlyWithdrawal: safeMonthly
        };
    },

    /**
     * 4. 목표 필요자산 역산
     */
    calculateRequiredWealth(desiredMonthlyExpense, retirementYears = 30, expectedReturnRate = 4.0, currentAge = 45, targetRetirementAge = 60) {
        const annualExpense = Number(desiredMonthlyExpense) * 12.0;
        const r = Math.max(0.001, expectedReturnRate / 100.0);

        // 연금 현가 공식: PV = PMT * [1 - (1+r)^(-n)] / r
        const targetTotal = annualExpense * (1.0 - Math.pow(1.0 + r, -retirementYears)) / r;
        const targetWealthLong = Math.round(targetTotal);

        // 축적 기간 동안 필요한 월 저축액
        const yearsToSave = Math.max(1, targetRetirementAge - currentAge);
        const monthsToSave = yearsToSave * 12;
        const saveMonthlyR = r / 12.0;

        let monthlySavingsNeeded = 0;
        if (saveMonthlyR > 0) {
            monthlySavingsNeeded = targetTotal * saveMonthlyR / (Math.pow(1.0 + saveMonthlyR, monthsToSave) - 1.0);
        } else {
            monthlySavingsNeeded = targetTotal / monthsToSave;
        }

        return {
            targetTotalWealth: targetWealthLong,
            monthlySavingsNeeded: Math.round(monthlySavingsNeeded),
            expectedAnnualRetirementIncome: Math.round(targetWealthLong * 0.04) // 4% 룰
        };
    },

    /**
     * 5. 국민연금 조기 vs 정상 vs 연기 손익분기점
     */
    calculateNationalPensionBreakEven(normalMonthlyAmount = 1500000, statutoryAge = 65, policy = new PolicySettings()) {
        const earlyAge = statutoryAge - 5;
        const earlyFactor = 1.0 - (5.0 * (policy.nationalEarlyReductionRatePerYear / 100.0));
        const earlyMonthly = Math.round(normalMonthlyAmount * earlyFactor);

        const normalAge = statutoryAge;
        const normalMonthly = normalMonthlyAmount;

        const delayedAge = statutoryAge + 5;
        const delayedFactor = 1.0 + (5.0 * (policy.nationalDelayIncreaseRatePerYear / 100.0));
        const delayedMonthly = Math.round(normalMonthlyAmount * delayedFactor);

        const comparisonList = [];
        let earlyCumulative = 0;
        let normalCumulative = 0;
        let delayedCumulative = 0;

        let earlyVsNormalBreakEven = 0;
        let normalVsDelayedBreakEven = 0;

        for (let age = earlyAge; age <= 95; age++) {
            if (age >= earlyAge) earlyCumulative += earlyMonthly * 12;
            if (age >= normalAge) normalCumulative += normalMonthly * 12;
            if (age >= delayedAge) delayedCumulative += delayedMonthly * 12;

            if (earlyVsNormalBreakEven === 0 && normalCumulative >= earlyCumulative && age >= normalAge) {
                earlyVsNormalBreakEven = age;
            }
            if (normalVsDelayedBreakEven === 0 && delayedCumulative >= normalCumulative && age >= delayedAge) {
                normalVsDelayedBreakEven = age;
            }

            comparisonList.push({
                age,
                earlyCumulative,
                normalCumulative,
                delayedCumulative
            });
        }

        return {
            earlyMonthlyAmount: earlyMonthly,
            normalMonthlyAmount: normalMonthly,
            delayedMonthlyAmount: delayedMonthly,
            earlyVsNormalBreakEvenAge: earlyVsNormalBreakEven > 0 ? earlyVsNormalBreakEven : 76,
            normalVsDelayedBreakEvenAge: normalVsDelayedBreakEven > 0 ? normalVsDelayedBreakEven : 82,
            comparisonList
        };
    },

    /**
     * 6. 연금저축/IRP 세액공제 & 절세 복리 마법
     */
    calculateTaxBenefit(annualContribution = 9000000, isLowIncomeTier = true, policy = new PolicySettings()) {
        const cappedContribution = Math.min(policy.maxTaxCreditContribution, Math.max(0, annualContribution));
        const taxRate = isLowIncomeTier 
            ? (policy.taxCreditRateLowIncome / 100.0) 
            : (policy.taxCreditRateHighIncome / 100.0);
        const annualRefund = Math.round(cappedContribution * taxRate);

        const cumulative30Years = annualRefund * 30;

        // 연 6% 복리 재투자 시 30년 후 환급금 미래가치
        const r = 0.06;
        let reinvestedFV = 0.0;
        for (let y = 1; y <= 30; y++) {
            reinvestedFV = (reinvestedFV + annualRefund) * (1.0 + r);
        }

        // 일반 과세 계좌 (이자소득세 매년 차감)
        const afterTaxR = 0.06 * (1.0 - (policy.generalInterestTaxRate / 100.0));
        let generalAccFV = 0.0;
        for (let y = 1; y <= 30; y++) {
            generalAccFV = (generalAccFV + cappedContribution) * (1.0 + afterTaxR);
        }

        // 연금저축 비과세 복리 운용
        let pensionAccFV = 0.0;
        for (let y = 1; y <= 30; y++) {
            pensionAccFV = (pensionAccFV + cappedContribution) * (1.0 + r);
        }

        const bonus = (pensionAccFV - generalAccFV) + reinvestedFV;

        return {
            annualContribution: cappedContribution,
            taxRefundAmount: annualRefund,
            cumulativeTaxRefund30Years: cumulative30Years,
            reinvestedFutureValue30Years: Math.round(reinvestedFV),
            generalTaxAccountFutureValue: Math.round(generalAccFV),
            taxSavingsAlchemyBonus: Math.round(bonus)
        };
    }
};

/**
 * 개인연금, 퇴직연금/IRP, 개인연금보험 전용 상호연동 연산 엔진
 */
const PensionPlanCalculator = {
    /**
     * 1. 수급 개시 시점 예상 적립금 (FV) 계산
     */
    calculateAccumulatedAtStartAge(currentAge, startAge, currentBalance, monthlyContribution, contributionEndAge, annualGrowthRate) {
        if (startAge <= currentAge) {
            return Math.max(0, Number(currentBalance) || 0);
        }

        const monthlyRate = ((Number(annualGrowthRate) || 0) / 100.0) / 12.0;
        const totalMonths = (startAge - currentAge) * 12;
        const payEnd = Math.max(currentAge, Math.min(Number(contributionEndAge) || startAge, startAge));
        const payMonths = (payEnd - currentAge) * 12;

        let balance = Number(currentBalance) || 0;
        const depositAmount = Number(monthlyContribution) || 0;

        for (let m = 1; m <= totalMonths; m++) {
            const deposit = (m <= payMonths) ? depositAmount : 0;
            balance = (balance + deposit) * (1.0 + monthlyRate);
        }

        return Math.max(0, Math.round(balance));
    },

    /**
     * 2. 수급 기간(년수)을 바탕으로 월 예상 수령액 계산 (PMT 공식)
     */
    calculateMonthlyPayoutFromPeriod(accumulatedFund, periodYears, annualGrowthRate) {
        const fund = Number(accumulatedFund) || 0;
        const years = Number(periodYears) || 0;
        if (fund <= 0 || years <= 0) return 0;

        const totalMonths = years * 12;
        const monthlyRate = ((Number(annualGrowthRate) || 0) / 100.0) / 12.0;

        if (monthlyRate <= 0) {
            return Math.max(0, Math.round(fund / totalMonths));
        }

        const discountFactor = 1.0 - Math.pow(1.0 + monthlyRate, -totalMonths);
        if (discountFactor <= 0) return 0;

        const monthlyPayout = fund * monthlyRate / discountFactor;
        return Math.max(0, Math.round(monthlyPayout));
    },

    /**
     * 3. 희망 월 수령액을 바탕으로 수급 가능 기간(년수) 및 종료 나이 계산 (NPER 공식)
     */
    calculatePeriodFromMonthlyPayout(accumulatedFund, desiredMonthlyPayout, startAge, annualGrowthRate, maxAge = 100) {
        const fund = Number(accumulatedFund) || 0;
        const payout = Number(desiredMonthlyPayout) || 0;
        const start = Number(startAge) || 60;
        const maxA = Number(maxAge) || 100;

        if (fund <= 0 || payout <= 0) {
            return { endAge: start, periodYears: 0, isForeverSafe: false };
        }

        const monthlyRate = ((Number(annualGrowthRate) || 0) / 100.0) / 12.0;

        // 이자만으로 월 인출액 충당 가능 (영구 수급 / 원금 보존)
        if (monthlyRate > 0 && (fund * monthlyRate) >= payout) {
            const maxYears = Math.max(1, maxA - start);
            return {
                endAge: maxA,
                periodYears: maxYears,
                isForeverSafe: true
            };
        }

        if (monthlyRate <= 0) {
            const months = fund / payout;
            const years = Math.max(1, Math.round(months / 12));
            const endAge = Math.min(maxA, start + years);
            return {
                endAge,
                periodYears: endAge - start,
                isForeverSafe: false
            };
        }

        const numerator = 1.0 - (fund * monthlyRate / payout);
        if (numerator <= 0) {
            const maxYears = Math.max(1, maxA - start);
            return { endAge: maxA, periodYears: maxYears, isForeverSafe: true };
        }

        const months = -Math.log(numerator) / Math.log(1.0 + monthlyRate);
        const years = Math.max(1, Math.round(months / 12));
        const calculatedEndAge = start + years;
        const finalEndAge = Math.min(maxA, Math.max(start + 1, calculatedEndAge));

        return {
            endAge: finalEndAge,
            periodYears: finalEndAge - start,
            isForeverSafe: false
        };
    },

    /**
     * 4. 개인연금보험 월 수령액 계산
     */
    calculateAnnuityInsurancePayout(accumulatedFund, startAge, isWholeLife, fixedPeriodYears = 20, annualGrowthRate = 3.5) {
        const periodYears = isWholeLife ? Math.max(5, 100 - startAge) : Math.max(1, fixedPeriodYears);
        const endAge = startAge + periodYears;
        const monthlyPayout = this.calculateMonthlyPayoutFromPeriod(accumulatedFund, periodYears, annualGrowthRate);
        return { endAge, monthlyPayout };
    }
};

