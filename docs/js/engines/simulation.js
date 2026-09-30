/**
 * SimulationEngine.js
 * 종합 생애 자산 및 은퇴 시뮬레이션 엔진
 */

const SimulationEngine = {
    /**
     * 출생연도별 법정 국민연금 수령 개시 연령
     */
    getStatutoryNationalPensionAge(birthYear) {
        if (birthYear <= 1952) return 60;
        if (birthYear >= 1953 && birthYear <= 1956) return 61;
        if (birthYear >= 1957 && birthYear <= 1960) return 62;
        if (birthYear >= 1961 && birthYear <= 1964) return 63;
        if (birthYear >= 1965 && birthYear <= 1968) return 64;
        return 65; // 1969년생 이후
    },

    /**
     * 종합 생애 자산 시뮬레이션 실행
     */
    runComprehensiveSimulation(profile, pensions, assets, incomes) {
        const currentYear = new Date().getFullYear();
        const currentAge = Math.max(20, profile.currentAge);
        const endAge = Math.min(110, Math.max(currentAge + 1, profile.targetEndAge));
        const retirementAge = Math.min(endAge, Math.max(currentAge, profile.retirementAge));

        const statutoryNationalAge = this.getStatutoryNationalPensionAge(profile.birthYear);
        const nationalPension = pensions.find(p => p.type === 'NATIONAL');
        let nationalPensionStartAge = statutoryNationalAge;
        if (nationalPension) {
            const baseAge = (nationalPension.startAge && nationalPension.startAge !== 65) 
                ? nationalPension.startAge 
                : statutoryNationalAge;
            nationalPensionStartAge = Math.min(75, Math.max(55, baseAge + (nationalPension.claimOffsetYears || 0)));
        }

        const policy = profile.policySettings || new PolicySettings();

        // 자산/부채 판별 헬퍼 (isLiability 프로퍼티 또는 type === 'DEBT')
        const isDebt = (a) => Boolean(a.isLiability || a.type === 'DEBT');

        // 초기 자산 분류 및 개별 자산 가중 기대수익률 산출
        const financialAssetsList = assets.filter(a => !isDebt(a) && a.type !== 'REAL_ESTATE');
        let liquidFinancialAssets = financialAssetsList.reduce((sum, a) => sum + (Number(a.currentValue) || 0), 0);
        const customFinancialYield = (liquidFinancialAssets > 0)
            ? financialAssetsList.reduce((sum, a) => sum + (Number(a.currentValue) || 0) * ((Number(a.expectedGrowthRate) || 0) / 100.0), 0) / liquidFinancialAssets
            : ((policy.financialAssetReturnRate || 4.0) / 100.0);

        const realEstateAssetsList = assets.filter(a => !isDebt(a) && a.type === 'REAL_ESTATE');
        let realEstateAssets = realEstateAssetsList.reduce((sum, a) => sum + (Number(a.currentValue) || 0), 0);
        const customRealEstateYield = (realEstateAssets > 0)
            ? realEstateAssetsList.reduce((sum, a) => sum + (Number(a.currentValue) || 0) * ((Number(a.expectedGrowthRate) || 0) / 100.0), 0) / realEstateAssets
            : ((policy.realEstateGrowthRate || 2.0) / 100.0);

        // 개별 부채 상환 추적 맵
        const debtAssets = assets.filter(a => isDebt(a));
        const debtBalances = {};
        debtAssets.forEach(d => {
            debtBalances[d.id] = Number(d.currentValue) || 0;
        });
        let debtBalance = Object.values(debtBalances).reduce((a, b) => a + b, 0);

        // 연금 적립금 추적 맵 (국민연금, 주택연금은 적립식 계좌자산이 아니므로 제외)
        const pensionBalances = {};
        pensions.filter(p => p.type !== 'NATIONAL' && p.type !== 'HOUSING').forEach(p => {
            pensionBalances[p.id] = Number(p.currentBalance) || 0;
        });

        // 생활비 초기값 (은퇴 전: 현재 월 생활소비 / 은퇴 후: 현재가치 기준 희망생활비의 은퇴시점 미래가치 환산)
        const generalInflationRate = (profile.inflationRate || 2.0) / 100.0;
        const medicalInflationRate = generalInflationRate + ((policy.medicalInflationSurcharge || 3.0) / 100.0);

        // 1) 은퇴 전: 현재 월 생활비 (현재가치 기준에서 매년 물가상승률 복리 적용)
        let preRetireLivingExpense = Number(profile.currentMonthlyExpenses || 3000000);

        // 2) 은퇴 후: 은퇴 시점까지 물가상승 복리 반영한 미래가치 기준 (기본생활비 + 의료비 가중치)
        const yearsToRetirement = Math.max(0, retirementAge - currentAge);
        const postRetireBaseLiving = profile.monthlyExpenses * (1.0 - (profile.medicalExpenseRatio || 0.10));
        const postRetireBaseMed = profile.monthlyExpenses * (profile.medicalExpenseRatio || 0.10);
        let postRetireLivingExpense = postRetireBaseLiving * Math.pow(1.0 + generalInflationRate, yearsToRetirement);
        let postRetireMedExpense = postRetireBaseMed * Math.pow(1.0 + medicalInflationRate, yearsToRetirement);

        const yearlyResults = [];
        let peakAsset = -Infinity;
        let peakAge = currentAge;
        let depletionAge = 0;

        let preRetirementWorkIncome = 0;
        let postRetirementPension = 0;

        let crevasseShortfallTotal = 0;
        let crevasseYearCount = 0;

        const isCrevassePossible = retirementAge < nationalPensionStartAge;

        for (let age = currentAge; age <= endAge; age++) {
            const yearsPassed = age - currentAge;
            const year = currentYear + yearsPassed;

            // 1. 월 지출: 은퇴 전에는 현재 생활 소비액, 은퇴 시점부터는 은퇴 후 필요 생활비(미래가치) 적용
            const monthlyExpenses = age < retirementAge
                ? Math.round(preRetireLivingExpense)
                : Math.round(postRetireLivingExpense + postRetireMedExpense);
            const annualExpenses = monthlyExpenses * 12;

            // 2. 근로 및 기타 정기 소득
            let monthlyWorkIncome = 0;
            for (const inc of incomes) {
                if (age <= inc.endAge) {
                    const growthFactor = Math.pow(1.0 + (inc.expectedGrowthRate || 0) / 100.0, yearsPassed);
                    monthlyWorkIncome += Math.round(Number(inc.monthlyAmount || 0) * growthFactor);
                }
            }

            if (age === retirementAge - 1 && monthlyWorkIncome > 0) {
                preRetirementWorkIncome = monthlyWorkIncome;
            }

            // 3. 연금 적립 및 수령액 산출
            let grossMonthlyNational = 0;
            let grossMonthlyRetirement = 0;
            let grossMonthlyPersonal = 0;
            let grossMonthlyAnnuityInsurance = 0;
            let grossMonthlyHousing = 0;
            let grossMonthlyOther = 0;
            let totalPensionAssets = 0;
            let annualPensionContributionsTotal = 0;
            let annualPensionContributionsDeducted = 0;

            for (const p of pensions) {
                const effectiveStartAge = (p.type === 'NATIONAL')
                    ? nationalPensionStartAge
                    : p.startAge;

                const isFunded = (p.type !== 'NATIONAL' && p.type !== 'HOUSING');
                let currentBal = isFunded ? (pensionBalances[p.id] || 0) : 0;

                // A) 적립기
                if (isFunded) {
                    if (age <= p.contributionEndAge && (p.monthlyContribution || 0) > 0) {
                        const annualContribution = p.monthlyContribution * 12;
                        annualPensionContributionsTotal += annualContribution;
                        if (p.isDeductedFromIncome !== false) {
                            annualPensionContributionsDeducted += annualContribution;
                        }
                        const rate = (p.expectedGrowthRate || 0) / 100.0;
                        currentBal = (currentBal + annualContribution) * (1.0 + rate);
                        pensionBalances[p.id] = currentBal;
                    } else if (age < effectiveStartAge && currentBal > 0) {
                        const rate = (p.expectedGrowthRate || 0) / 100.0;
                        currentBal = currentBal * (1.0 + rate);
                        pensionBalances[p.id] = currentBal;
                    }
                }

                // B) 수령기
                let monthlyPayout = 0;
                if (age >= effectiveStartAge && age <= p.endAge) {
                    monthlyPayout = Number(p.expectedMonthlyAmount || 0);

                    if (p.type === 'NATIONAL') {
                        // 국민연금 조기/연기 보정
                        if (p.claimOffsetYears) {
                            const adj = (p.claimOffsetYears < 0)
                                ? 1.0 + (p.claimOffsetYears * (policy.nationalEarlyReductionRatePerYear / 100.0))
                                : 1.0 + (p.claimOffsetYears * (policy.nationalDelayIncreaseRatePerYear / 100.0));
                            monthlyPayout *= adj;
                        }
                        // 물가상승률 복리 연동
                        const cpiFactor = Math.pow(1.0 + generalInflationRate, yearsPassed);
                        monthlyPayout *= cpiFactor;
                    } else if (p.type === 'HOUSING' || p.type === 'OTHER') {
                        // 명목 고정 지급
                    } else {
                        // 사적연금(개인연금, 퇴직연금, 연금보험)은 이미 잔여적립금 복리운용을 포함한 PMT 연금화 금액
                        const hasTrackedBalance = (p.currentBalance > 0 || p.monthlyContribution > 0);
                        if (hasTrackedBalance) {
                            const currentAccBal = pensionBalances[p.id] || 0;
                            const rate = (p.expectedGrowthRate || 0) / 100.0;
                            const availableThisYear = currentAccBal * (1.0 + rate);
                            const requestedAnnual = monthlyPayout * 12;
                            if (availableThisYear < requestedAnnual && availableThisYear > 0) {
                                monthlyPayout = availableThisYear / 12;
                            } else if (availableThisYear <= 0 && age > effectiveStartAge) {
                                monthlyPayout = 0;
                            }
                        }
                    }

                    const payoutLong = Math.round(monthlyPayout);
                    switch (p.type) {
                        case 'NATIONAL': grossMonthlyNational += payoutLong; break;
                        case 'RETIREMENT': grossMonthlyRetirement += payoutLong; break;
                        case 'PERSONAL': grossMonthlyPersonal += payoutLong; break;
                        case 'ANNUITY_INSURANCE': grossMonthlyAnnuityInsurance += payoutLong; break;
                        case 'HOUSING': grossMonthlyHousing += payoutLong; break;
                        case 'OTHER': default: grossMonthlyOther += payoutLong; break;
                    }
                }

                // C) 수령기 적립금 인출 및 잔여 자산 갱신
                if (isFunded) {
                    const hasTrackedBalance = (p.currentBalance > 0 || p.monthlyContribution > 0);
                    if (hasTrackedBalance) {
                        if (age >= effectiveStartAge && age <= p.endAge) {
                            const rate = (p.expectedGrowthRate || 0) / 100.0;
                            const balWithYield = (pensionBalances[p.id] || currentBal) * (1.0 + rate);
                            const annualWithdrawal = monthlyPayout * 12;
                            pensionBalances[p.id] = Math.max(0, balWithYield - annualWithdrawal);
                        } else if (age > p.endAge) {
                            pensionBalances[p.id] = 0;
                        }
                        totalPensionAssets += Math.round(pensionBalances[p.id] || 0);
                    }
                }
            }

            // 3-1. 세금 및 실지급액 정밀 계산
            const nationalAnnualGross = grossMonthlyNational * 12;
            const nationalAnnualTax = PensionTaxCalculator.calculateAnnualNationalPensionTax(nationalAnnualGross, policy);
            const nationalMonthlyTax = Math.round(nationalAnnualTax / 12);
            const netMonthlyNational = grossMonthlyNational - nationalMonthlyTax;
            const isHealthInsuranceDisqualified = nationalAnnualGross > policy.healthInsurancePensionLimit;

            // 퇴직연금 세액
            let retirementMonthlyTax = 0;
            for (const p of pensions) {
                if (p.type === 'RETIREMENT' && age >= p.startAge && age <= p.endAge) {
                    const yearsSinceStart = Math.max(1, age - p.startAge + 1);
                    const pPayout = p.expectedMonthlyAmount;
                    retirementMonthlyTax += PensionTaxCalculator.calculateMonthlyRetirementPensionTax(pPayout, yearsSinceStart, policy);
                }
            }
            const netMonthlyRetirement = Math.max(0, grossMonthlyRetirement - retirementMonthlyTax);

            // 개인연금저축 세액
            const personalAnnualGross = grossMonthlyPersonal * 12;
            const personalAnnualTax = PensionTaxCalculator.calculateAnnualPersonalPensionTax(personalAnnualGross, age, policy);
            const personalMonthlyTax = Math.round(personalAnnualTax / 12);
            const netMonthlyPersonal = Math.max(0, grossMonthlyPersonal - personalMonthlyTax);
            const isPrivatePensionLimitExceeded = personalAnnualGross > policy.privatePensionAnnualLimit;

            // 비과세 연금
            const netMonthlyAnnuityInsurance = grossMonthlyAnnuityInsurance;
            const netMonthlyHousing = grossMonthlyHousing;

            // 기타연금
            const otherMonthlyTax = PensionTaxCalculator.calculateMonthlyOtherPensionTax(grossMonthlyOther);
            const netMonthlyOther = Math.max(0, grossMonthlyOther - otherMonthlyTax);

            // 총 연금
            const totalGrossMonthlyPension = grossMonthlyNational + grossMonthlyRetirement + grossMonthlyPersonal + grossMonthlyAnnuityInsurance + grossMonthlyHousing + grossMonthlyOther;
            const totalMonthlyPensionTax = nationalMonthlyTax + retirementMonthlyTax + personalMonthlyTax + otherMonthlyTax;
            const monthlyPensionIncome = Math.max(0, totalGrossMonthlyPension - totalMonthlyPensionTax);

            const totalMonthlyIncome = monthlyWorkIncome + monthlyPensionIncome;
            const annualIncome = totalMonthlyIncome * 12;

            if (age === nationalPensionStartAge && postRetirementPension === 0) {
                postRetirementPension = monthlyPensionIncome;
            }

            // 4. 생애 주기 단계 판정
            let stage = "축적기";
            let isCrevasse = false;
            if (age < retirementAge) {
                stage = "축적기";
            } else if (age < nationalPensionStartAge) {
                stage = "소득 크레바스 (공백기)";
                isCrevasse = true;
                const shortfall = monthlyExpenses - totalMonthlyIncome;
                if (shortfall > 0) {
                    crevasseShortfallTotal += shortfall;
                    crevasseYearCount++;
                }
            } else {
                stage = "은퇴/연금수령기";
            }

            // 4-1. 부채 원리금 상환
            let annualDebtPrincipal = 0;
            let annualDebtInterest = 0;
            for (const debt of debtAssets) {
                const currentDebtBal = debtBalances[debt.id] || 0;
                if (currentDebtBal > 0) {
                    const result = LoanCalculator.calculateAnnualRepayment(
                        currentDebtBal,
                        debt.currentValue,
                        debt.expectedGrowthRate || 3.8,
                        Math.max(1, debt.maturityYears || 10),
                        debt.repaymentMethod || 'EQUAL_PRINCIPAL_AND_INTEREST',
                        yearsPassed
                    );
                    debtBalances[debt.id] = result.remainingBalance;
                    annualDebtPrincipal += result.principalPayment;
                    annualDebtInterest += result.interestPayment;
                }
            }
            debtBalance = Object.values(debtBalances).reduce((a, b) => a + b, 0);
            const annualDebtService = annualDebtPrincipal + annualDebtInterest;

            // 5. 현금흐름 밸런싱 (개별 자산 가중 기대수익률 적용)
            const annualTotalExpenses = annualExpenses + annualDebtService + annualPensionContributionsDeducted;
            const annualCashFlow = annualIncome - annualTotalExpenses;
            const financialYield = (liquidFinancialAssets > 0) ? customFinancialYield : 0.0;
            const realEstateYield = customRealEstateYield;

            if (annualCashFlow >= 0) {
                const base = (liquidFinancialAssets > 0)
                    ? Math.round(liquidFinancialAssets * (1.0 + financialYield))
                    : liquidFinancialAssets;
                liquidFinancialAssets = base + annualCashFlow;
            } else {
                let deficit = Math.abs(annualCashFlow);

                // 1) 금융자산이 플러스인 경우 먼저 인출
                if (liquidFinancialAssets > 0) {
                    const grownAssets = Math.round(liquidFinancialAssets * (1.0 + financialYield));
                    if (grownAssets >= deficit) {
                        liquidFinancialAssets = grownAssets - deficit;
                        deficit = 0;
                    } else {
                        deficit -= grownAssets;
                        liquidFinancialAssets = 0;
                    }
                }

                // 2) 금융자산 소진 후 부동산 자산에서 차감
                if (deficit > 0 && realEstateAssets > 0) {
                    if (realEstateAssets >= deficit) {
                        realEstateAssets -= deficit;
                        deficit = 0;
                    } else {
                        deficit -= realEstateAssets;
                        realEstateAssets = 0;
                    }
                }

                // 3) 모든 유동자산과 부동산이 소진된 후에도 남은 적자: 누적 결손으로 계속 차감!
                if (deficit > 0) {
                    liquidFinancialAssets -= deficit;
                }
            }

            if (realEstateAssets > 0) {
                realEstateAssets = Math.round(realEstateAssets * (1.0 + realEstateYield));
            }

            const positiveFinancialAssets = Math.max(0, liquidFinancialAssets);
            const totalGrossAssets = positiveFinancialAssets + realEstateAssets + totalPensionAssets;
            // 순자산: 음수 허용
            const netAssetValue = (liquidFinancialAssets < 0)
                ? (totalGrossAssets - debtBalance + liquidFinancialAssets)
                : (totalGrossAssets - debtBalance);

            if (netAssetValue <= 0 && depletionAge === 0 && age > currentAge) {
                depletionAge = age;
            }
            if (netAssetValue > peakAsset) {
                peakAsset = netAssetValue;
                peakAge = age;
            }

            yearlyResults.push({
                age,
                year,
                stage,
                isCrevasse,
                totalGrossAssets,
                totalDebt: debtBalance,
                netAssetValue,
                financialAssets: liquidFinancialAssets,
                pensionAssets: totalPensionAssets,
                realEstateAssets,
                monthlyTotalIncome: totalMonthlyIncome,
                monthlyWorkIncome,
                monthlyPensionIncome,
                grossMonthlyPensionIncome: totalGrossMonthlyPension,
                monthlyPensionTax: totalMonthlyPensionTax,
                monthlyNationalPension: netMonthlyNational,
                monthlyRetirementPension: netMonthlyRetirement,
                monthlyPersonalPension: netMonthlyPersonal,
                monthlyAnnuityInsurancePension: netMonthlyAnnuityInsurance,
                monthlyHousingPension: netMonthlyHousing,
                monthlyOtherPension: netMonthlyOther,
                monthlyExpenses,
                monthlyPensionContribution: Math.round(annualPensionContributionsDeducted / 12),
                monthlyDebtService: Math.round(annualDebtService / 12),
                monthlyNetCashFlow: Math.round(annualCashFlow / 12),
                isHealthInsuranceDisqualified,
                isPrivatePensionLimitExceeded
            });

            // 익년 물가상승 반영
            if (age < retirementAge) {
                preRetireLivingExpense *= (1.0 + generalInflationRate);
            } else {
                postRetireLivingExpense *= (1.0 + generalInflationRate);
                postRetireMedExpense *= (1.0 + medicalInflationRate);
            }
        }

        // 소득대체율
        let incomeReplacementRate = 50.0;
        if (preRetirementWorkIncome > 0) {
            const safePension = postRetirementPension > 0 
                ? postRetirementPension 
                : (yearlyResults.find(r => r.age >= retirementAge)?.monthlyPensionIncome || 0);
            incomeReplacementRate = Math.min(200.0, Math.max(0.0, (safePension / preRetirementWorkIncome) * 100.0));
        }

        // 소득 크레바스 정보
        const crevasseDuration = isCrevassePossible ? Math.max(0, nationalPensionStartAge - retirementAge) : 0;
        const avgCrevasseShortfall = crevasseYearCount > 0 ? Math.round(crevasseShortfallTotal / crevasseYearCount) : 0;
        const crevasseInfo = {
            hasCrevasse: isCrevassePossible && crevasseDuration > 0,
            startAge: retirementAge,
            endAge: nationalPensionStartAge,
            durationYears: crevasseDuration,
            monthlyShortfall: avgCrevasseShortfall,
            totalRequiredBridgeFund: avgCrevasseShortfall * 12 * crevasseDuration
        };

        // 건강도 점수 분석
        const healthScore = RetirementHealthAnalyzer.evaluate({
            incomeReplacementRate,
            depletionAge,
            targetEndAge: endAge,
            hasCrevasseShortfall: crevasseInfo.hasCrevasse && crevasseInfo.monthlyShortfall > 0,
            hasRealEstate: assets.some(a => !isDebt(a) && a.type === 'REAL_ESTATE'),
            hasFinancialAssets: assets.some(a => !isDebt(a) && a.type !== 'REAL_ESTATE'),
            hasPensionAssets: pensions.length > 0
        });

        const initialTotalDebt = assets.filter(a => isDebt(a)).reduce((s, a) => s + (Number(a.currentValue) || 0), 0);
        const initialFinancialAssets = assets.filter(a => !isDebt(a) && a.type !== 'REAL_ESTATE').reduce((s, a) => s + (Number(a.currentValue) || 0), 0);
        const initialRealEstate = assets.filter(a => !isDebt(a) && a.type === 'REAL_ESTATE').reduce((s, a) => s + (Number(a.currentValue) || 0), 0);
        const initialPensionAssets = pensions.filter(p => p.type !== 'NATIONAL' && p.type !== 'HOUSING').reduce((s, p) => s + (Number(p.currentBalance) || 0), 0);
        const initialTotalAssets = initialFinancialAssets + initialRealEstate + initialPensionAssets;
        const initialNetWorth = initialTotalAssets - initialTotalDebt;

        // 실시간 초당 자산 수익 및 가계 순현금흐름 지표
        const initialAnnualRegularIncome = incomes
            .filter(i => currentAge <= i.endAge)
            .reduce((sum, i) => sum + (Number(i.monthlyAmount) || 0) * 12, 0);
        const annualFinancialGain = financialAssetsList
            .reduce((sum, a) => sum + Math.round((Number(a.currentValue) || 0) * ((Number(a.expectedGrowthRate) || 0) / 100.0)), 0);
        const annualRealEstateGain = realEstateAssetsList
            .reduce((sum, a) => sum + Math.round((Number(a.currentValue) || 0) * ((Number(a.expectedGrowthRate) || 0) / 100.0)), 0);
        const annualPensionGain = pensions
            .filter(p => p.type !== 'NATIONAL' && p.type !== 'HOUSING')
            .reduce((sum, p) => sum + Math.round((Number(p.currentBalance) || 0) * ((Number(p.expectedGrowthRate) || 0) / 100.0)), 0);
        const annualTotalInflow = initialAnnualRegularIncome + annualFinancialGain + annualRealEstateGain + annualPensionGain;

        const annualLivingExpenses = Number(profile.currentMonthlyExpenses || 3000000) * 12;
        const annualDebtInterestCost = debtAssets
            .reduce((sum, d) => sum + Math.round((Number(d.currentValue) || 0) * ((Number(d.expectedGrowthRate) || 3.8) / 100.0)), 0);
        const annualPensionContributionDeducted = pensions
            .filter(p => p.type !== 'NATIONAL' && p.type !== 'HOUSING' && currentAge <= (p.contributionEndAge || 60) && p.isDeductedFromIncome !== false)
            .reduce((sum, p) => sum + (Number(p.monthlyContribution) || 0) * 12, 0);
        const annualTotalOutflow = annualLivingExpenses + annualDebtInterestCost + annualPensionContributionDeducted;

        const annualNetWealthGrowth = annualTotalInflow - annualTotalOutflow;
        const annualNetCapitalGain = annualFinancialGain + annualRealEstateGain + annualPensionGain - annualDebtInterestCost;

        const wonPerSecond = annualNetWealthGrowth / (365.25 * 86400.0);
        const wonPerHour = wonPerSecond * 3600.0;
        const wonPerDay = Math.round(annualNetWealthGrowth / 365.25);
        const wonPerMonth = Math.round(annualNetWealthGrowth / 12.0);

        const realTimeYield = {
            annualRegularIncome: initialAnnualRegularIncome,
            annualFinancialGain,
            annualRealEstateGain,
            annualPensionGain,
            annualTotalInflow,
            annualLivingExpenses,
            annualDebtInterestCost,
            annualPensionContributionDeducted,
            annualTotalOutflow,
            annualNetWealthGrowth,
            annualNetCapitalGain,
            wonPerSecond,
            wonPerHour,
            wonPerDay,
            wonPerMonth
        };

        return {
            currentAge,
            retirementAge,
            endAge,
            currentNetWorth: initialNetWorth,
            currentTotalAssets: initialTotalAssets,
            currentTotalDebt: initialTotalDebt,
            currentFinancialAssets: initialFinancialAssets,
            currentPensionAssets: initialPensionAssets,
            currentRealEstateAssets: initialRealEstate,
            incomeReplacementRate,
            isSafeRetirement: depletionAge === 0,
            depletionAge,
            postRetirementMonthlyPension: yearlyResults.find(r => r.age === 65)?.monthlyPensionIncome || postRetirementPension,
            peakAssetValue: (peakAsset === -Infinity) ? initialNetWorth : peakAsset,
            peakAssetAge: peakAge,
            realTimeYield,
            yearlyResults,
            crevasseInfo,
            healthScore
        };
    }
};
