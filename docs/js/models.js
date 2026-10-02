/**
 * PensionAlchemy Data Models & Presets
 */

const AssetType = {
    DEPOSIT: { key: 'DEPOSIT', displayName: '예금', isLiability: false, icon: 'account_balance' },
    SAVINGS: { key: 'SAVINGS', displayName: '적금', isLiability: false, icon: 'savings' },
    STOCK: { key: 'STOCK', displayName: '주식', isLiability: false, icon: 'show_chart' },
    BOND: { key: 'BOND', displayName: '채권', isLiability: false, icon: 'description' },
    ETF: { key: 'ETF', displayName: 'ETF/펀드', isLiability: false, icon: 'pie_chart' },
    COMMODITY: { key: 'COMMODITY', displayName: '원자재(금 등)', isLiability: false, icon: 'monetization_on' },
    REAL_ESTATE: { key: 'REAL_ESTATE', displayName: '부동산', isLiability: false, icon: 'apartment' },
    CRYPTO: { key: 'CRYPTO', displayName: '가상자산', isLiability: false, icon: 'currency_bitcoin' },
    DEBT: { key: 'DEBT', displayName: '대출/부채', isLiability: true, icon: 'credit_card' },
    OTHER: { key: 'OTHER', displayName: '기타 자산', isLiability: false, icon: 'category' }
};

const RepaymentMethod = {
    EQUAL_PRINCIPAL_AND_INTEREST: { key: 'EQUAL_PRINCIPAL_AND_INTEREST', displayName: '원리금균등', shortDescription: '매월 원금+이자 균등 분할 상환' },
    EQUAL_PRINCIPAL: { key: 'EQUAL_PRINCIPAL', displayName: '원금균등', shortDescription: '매월 동일한 원금 상환 (이자 점차 감소)' },
    BULLET: { key: 'BULLET', displayName: '만기일시', shortDescription: '기간 중 이자만 납부 후 만기에 일시 상환' },
    INTEREST_ONLY: { key: 'INTEREST_ONLY', displayName: '거치(이자만)', shortDescription: '원금 상환 없이 매월 이자만 지출' }
};

const IncomeType = {
    SALARY: { key: 'SALARY', displayName: '근로소득', icon: 'badge' },
    BONUS: { key: 'BONUS', displayName: '상여/성과급', icon: 'card_giftcard' },
    BUSINESS: { key: 'BUSINESS', displayName: '사업소득', icon: 'storefront' },
    RENTAL: { key: 'RENTAL', displayName: '임대소득', icon: 'home_work' },
    OTHER: { key: 'OTHER', displayName: '기타소득', icon: 'payments' }
};

const PensionType = {
    NATIONAL: { key: 'NATIONAL', displayName: '국민/공적연금', tierDescription: '1층 기본보장', color: '#3B82F6' },
    RETIREMENT: { key: 'RETIREMENT', displayName: '퇴직연금/IRP', tierDescription: '2층 표준보장', color: '#8B5CF6' },
    PERSONAL: { key: 'PERSONAL', displayName: '개인연금저축', tierDescription: '3층 세제적격연금', color: '#10B981' },
    ANNUITY_INSURANCE: { key: 'ANNUITY_INSURANCE', displayName: '개인연금보험', tierDescription: '3층 비과세연금', color: '#06B6D4' },
    HOUSING: { key: 'HOUSING', displayName: '주택연금', tierDescription: '역모기지 보장', color: '#F59E0B' },
    OTHER: { key: 'OTHER', displayName: '기타연금', tierDescription: '기타 사적연금', color: '#64748B' }
};

class PolicySettings {
    constructor(init = {}) {
        // 1. 공적/사적연금 세법 한도 (원 단위)
        this.privatePensionAnnualLimit = init.privatePensionAnnualLimit ?? 15000000;
        this.healthInsurancePensionLimit = init.healthInsurancePensionLimit ?? 20000000;
        this.maxPensionIncomeDeduction = init.maxPensionIncomeDeduction ?? 9000000;
        this.basicPersonalDeduction = init.basicPersonalDeduction ?? 1500000;

        // 2. 사적연금 인출 연령별 세율 & 초과 세율 (%)
        this.privatePensionRateUnder70 = init.privatePensionRateUnder70 ?? 5.5;
        this.privatePensionRateUnder80 = init.privatePensionRateUnder80 ?? 4.4;
        this.privatePensionRate80OrOver = init.privatePensionRate80OrOver ?? 3.3;
        this.privatePensionExcessRate = init.privatePensionExcessRate ?? 16.5;

        // 3. 퇴직연금(IRP) 분할인출 감면율 & 기준 퇴직소득세율 (%)
        this.retirementTaxDiscountRateEarly = init.retirementTaxDiscountRateEarly ?? 30.0;
        this.retirementTaxDiscountRateLate = init.retirementTaxDiscountRateLate ?? 40.0;
        this.baseRetirementTaxRate = init.baseRetirementTaxRate ?? 5.0;

        // 4. 연금저축/IRP 세액공제 납입한도 & 공제율
        this.maxTaxCreditContribution = init.maxTaxCreditContribution ?? 9000000;
        this.lowIncomeSalaryThreshold = init.lowIncomeSalaryThreshold ?? 55000000;
        this.taxCreditRateLowIncome = init.taxCreditRateLowIncome ?? 16.5;
        this.taxCreditRateHighIncome = init.taxCreditRateHighIncome ?? 13.2;
        this.generalInterestTaxRate = init.generalInterestTaxRate ?? 15.4;

        // 5. 국민연금 조기/연기 수령 연간 조정률 (%/년)
        this.nationalEarlyReductionRatePerYear = init.nationalEarlyReductionRatePerYear ?? 6.0;
        this.nationalDelayIncreaseRatePerYear = init.nationalDelayIncreaseRatePerYear ?? 7.2;

        // 6. 시뮬레이션 기본 자산수익률 및 의료비 가중률 (%/년)
        this.financialAssetReturnRate = init.financialAssetReturnRate ?? 4.5;
        this.realEstateGrowthRate = init.realEstateGrowthRate ?? 2.0;
        this.medicalInflationSurcharge = init.medicalInflationSurcharge ?? 3.0;

        // 7. 대한민국 순자산 분포 모수 (로그정규분포, 단위: 억원)
        this.wealthDistributionMean = init.wealthDistributionMean ?? 1.00984;
        this.wealthDistributionStdDev = init.wealthDistributionStdDev ?? 1.1937;
    }
}

class UserProfile {
    constructor(init = {}) {
        this.birthYear = init.birthYear ?? 1985;
        this.retirementAge = init.retirementAge ?? 60;
        this.targetEndAge = init.targetEndAge ?? 100;
        this.currentMonthlyExpenses = init.currentMonthlyExpenses ?? 3000000; // 은퇴 전 현재 생활 소비액
        this.monthlyExpenses = init.monthlyExpenses ?? 2500000;               // 은퇴 후 필요 생활비 (현재가치 기준)
        this.medicalExpenseRatio = init.medicalExpenseRatio ?? 0.10;
        this.inflationRate = init.inflationRate ?? 2.0;
        this.currency = init.currency ?? 'KRW';
        this.globalBaseDateTime = init.globalBaseDateTime ?? '2026-01-01';
        this.policySettings = new PolicySettings(init.policySettings || {});
    }

    get currentAge() {
        return new Date().getFullYear() - this.birthYear;
    }
}

class Asset {
    constructor(init = {}) {
        this.id = init.id || 'a_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4);
        this.name = init.name || '';
        this.type = init.type || 'DEPOSIT';
        this.currentValue = Number(init.currentValue) || 0;
        this.expectedGrowthRate = Number(init.expectedGrowthRate) || 0;
        this.repaymentMethod = init.repaymentMethod || 'EQUAL_PRINCIPAL_AND_INTEREST';
        this.maturityYears = Number(init.maturityYears) || 10;
        this.isLiability = init.isLiability !== undefined ? Boolean(init.isLiability) : (this.type === 'DEBT');
        this.baseDate = init.baseDate || '2026-01-01';
    }
}

class Pension {
    constructor(init = {}) {
        this.id = init.id || 'p_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4);
        this.name = init.name || '';
        this.type = init.type || 'PERSONAL';
        this.startAge = Number(init.startAge) || 60;
        this.endAge = Number(init.endAge) || 85;
        this.expectedMonthlyAmount = Number(init.expectedMonthlyAmount) || 0;
        this.expectedGrowthRate = Number(init.expectedGrowthRate) || 0;
        this.currentBalance = Number(init.currentBalance) || 0;
        this.monthlyContribution = Number(init.monthlyContribution) || 0;
        this.contributionEndAge = Number(init.contributionEndAge) || 60;
        this.isTaxDeductionEligible = Boolean(init.isTaxDeductionEligible);
        this.claimOffsetYears = Number(init.claimOffsetYears) || 0;
        this.isDeductedFromIncome = init.isDeductedFromIncome !== undefined ? Boolean(init.isDeductedFromIncome) : true;
        this.baseDate = init.baseDate || '2026-01-01';
    }
}

class Income {
    constructor(init = {}) {
        this.id = init.id || 'i_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4);
        this.name = init.name || '';
        this.type = init.type || 'SALARY';
        this.monthlyAmount = Number(init.monthlyAmount) || 0;
        this.endAge = Number(init.endAge) || 60;
        this.expectedGrowthRate = Number(init.expectedGrowthRate) || 0;
        this.baseDate = init.baseDate || '2026-01-01';
    }
}

// 생애주기 프리셋 정의
const Presets = {
    // 20대 사회초년생 (26세)
    preset20s: () => ({
        profile: new UserProfile({
            birthYear: 2000,
            retirementAge: 60,
            targetEndAge: 100,
            currentMonthlyExpenses: 1800000,
            monthlyExpenses: 2000000,
            medicalExpenseRatio: 0.08,
            inflationRate: 2.0,
            globalBaseDateTime: '2026-01-01'
        }),
        pensions: [
            new Pension({ id: 'p_20_1', name: '국민연금', type: 'NATIONAL', startAge: 65, endAge: 100, expectedMonthlyAmount: 1200000, expectedGrowthRate: 2.0, claimOffsetYears: 0, baseDate: '2026-01-01' }),
            new Pension({ id: 'p_20_2', name: '개인연금저축펀드', type: 'PERSONAL', startAge: 60, endAge: 85, expectedMonthlyAmount: 600000, expectedGrowthRate: 6.0, currentBalance: 3000000, monthlyContribution: 200000, contributionEndAge: 60, isTaxDeductionEligible: true, baseDate: '2026-01-01' }),
            new Pension({ id: 'p_20_3', name: '퇴직연금 (DC)', type: 'RETIREMENT', startAge: 60, endAge: 80, expectedMonthlyAmount: 500000, expectedGrowthRate: 5.0, currentBalance: 4000000, monthlyContribution: 250000, contributionEndAge: 60, isTaxDeductionEligible: true, baseDate: '2026-01-01' })
        ],
        assets: [
            new Asset({ id: 'a_20_1', name: '청년도약 및 청약저축', type: 'SAVINGS', currentValue: 15000000, expectedGrowthRate: 4.5, isLiability: false, baseDate: '2026-01-01' }),
            new Asset({ id: 'a_20_2', name: '비상금 파킹통장', type: 'DEPOSIT', currentValue: 5000000, expectedGrowthRate: 2.5, isLiability: false, baseDate: '2026-01-01' }),
            new Asset({ id: 'a_20_3', name: '학자금 대출(부채)', type: 'DEBT', currentValue: 8000000, expectedGrowthRate: 1.7, repaymentMethod: 'EQUAL_PRINCIPAL_AND_INTEREST', maturityYears: 5, isLiability: true, baseDate: '2026-01-01' })
        ],
        incomes: [
            new Income({ id: 'i_20_1', name: '첫 직장 급여', type: 'SALARY', monthlyAmount: 2800000, endAge: 60, expectedGrowthRate: 3.5, baseDate: '2026-01-01' })
        ]
    }),

    // 30대 자산형성기 (35세)
    preset30s: () => ({
        profile: new UserProfile({
            birthYear: 1991,
            retirementAge: 60,
            targetEndAge: 100,
            currentMonthlyExpenses: 2800000,
            monthlyExpenses: 2600000,
            medicalExpenseRatio: 0.09,
            inflationRate: 2.0,
            globalBaseDateTime: '2026-01-01'
        }),
        pensions: [
            new Pension({ id: 'p_30_1', name: '국민연금', type: 'NATIONAL', startAge: 65, endAge: 100, expectedMonthlyAmount: 1500000, expectedGrowthRate: 2.0, claimOffsetYears: 0, baseDate: '2026-01-01' }),
            new Pension({ id: 'p_30_2', name: '퇴직연금 (IRP)', type: 'RETIREMENT', startAge: 60, endAge: 80, expectedMonthlyAmount: 900000, expectedGrowthRate: 5.0, currentBalance: 35000000, monthlyContribution: 300000, contributionEndAge: 60, isTaxDeductionEligible: true, baseDate: '2026-01-01' }),
            new Pension({ id: 'p_30_3', name: '개인연금저축', type: 'PERSONAL', startAge: 60, endAge: 85, expectedMonthlyAmount: 850000, expectedGrowthRate: 5.5, currentBalance: 25000000, monthlyContribution: 400000, contributionEndAge: 60, isTaxDeductionEligible: true, baseDate: '2026-01-01' })
        ],
        assets: [
            new Asset({ id: 'a_30_1', name: '전세보증금(부동산)', type: 'REAL_ESTATE', currentValue: 300000000, expectedGrowthRate: 2.0, isLiability: false, baseDate: '2026-01-01' }),
            new Asset({ id: 'a_30_2', name: '미국 지수 ETF', type: 'ETF', currentValue: 45000000, expectedGrowthRate: 7.0, isLiability: false, baseDate: '2026-01-01' }),
            new Asset({ id: 'a_30_3', name: '비상금 CMA', type: 'DEPOSIT', currentValue: 15000000, expectedGrowthRate: 3.0, isLiability: false, baseDate: '2026-01-01' }),
            new Asset({ id: 'a_30_4', name: '전세대출(부채)', type: 'DEBT', currentValue: 140000000, expectedGrowthRate: 3.8, repaymentMethod: 'BULLET', maturityYears: 4, isLiability: true, baseDate: '2026-01-01' })
        ],
        incomes: [
            new Income({ id: 'i_30_1', name: '주 직장 급여', type: 'SALARY', monthlyAmount: 4200000, endAge: 60, expectedGrowthRate: 3.0, baseDate: '2026-01-01' })
        ]
    }),

    // 40대 확장기 (45세) - 기본값
    preset40s: () => ({
        profile: new UserProfile({
            birthYear: 1981,
            retirementAge: 60,
            targetEndAge: 100,
            currentMonthlyExpenses: 3800000,
            monthlyExpenses: 3300000,
            medicalExpenseRatio: 0.10,
            inflationRate: 2.0,
            globalBaseDateTime: '2026-01-01'
        }),
        pensions: [
            new Pension({ id: 'p_40_1', name: '국민연금', type: 'NATIONAL', startAge: 65, endAge: 100, expectedMonthlyAmount: 1800000, expectedGrowthRate: 2.0, claimOffsetYears: 0, baseDate: '2026-01-01' }),
            new Pension({ id: 'p_40_2', name: '퇴직연금 (DC/IRP)', type: 'RETIREMENT', startAge: 60, endAge: 80, expectedMonthlyAmount: 1250000, expectedGrowthRate: 4.5, currentBalance: 85000000, monthlyContribution: 600000, contributionEndAge: 60, isTaxDeductionEligible: true, baseDate: '2026-01-01' }),
            new Pension({ id: 'p_40_3', name: '개인연금저축', type: 'PERSONAL', startAge: 60, endAge: 85, expectedMonthlyAmount: 950000, expectedGrowthRate: 5.5, currentBalance: 55000000, monthlyContribution: 750000, contributionEndAge: 60, isTaxDeductionEligible: true, baseDate: '2026-01-01' }),
            new Pension({ id: 'p_40_4', name: '개인연금보험 (비과세)', type: 'ANNUITY_INSURANCE', startAge: 60, endAge: 90, expectedMonthlyAmount: 400000, expectedGrowthRate: 3.5, currentBalance: 25000000, monthlyContribution: 300000, contributionEndAge: 60, isTaxDeductionEligible: false, baseDate: '2026-01-01' })
        ],
        assets: [
            new Asset({ id: 'a_40_1', name: '거주 아파트', type: 'REAL_ESTATE', currentValue: 700000000, expectedGrowthRate: 2.5, isLiability: false, baseDate: '2026-01-01' }),
            new Asset({ id: 'a_40_2', name: '주식 및 글로벌 ETF', type: 'ETF', currentValue: 120000000, expectedGrowthRate: 6.0, isLiability: false, baseDate: '2026-01-01' }),
            new Asset({ id: 'a_40_3', name: '비상금 예적금', type: 'DEPOSIT', currentValue: 50000000, expectedGrowthRate: 3.0, isLiability: false, baseDate: '2026-01-01' }),
            new Asset({ id: 'a_40_4', name: '주택담보대출(부채)', type: 'DEBT', currentValue: 220000000, expectedGrowthRate: 3.8, repaymentMethod: 'EQUAL_PRINCIPAL_AND_INTEREST', maturityYears: 15, isLiability: true, baseDate: '2026-01-01' })
        ],
        incomes: [
            new Income({ id: 'i_40_1', name: '본인 근로소득', type: 'SALARY', monthlyAmount: 5800000, endAge: 60, expectedGrowthRate: 2.5, baseDate: '2026-01-01' }),
            new Income({ id: 'i_40_2', name: '배우자 부업/파트타임', type: 'OTHER', monthlyAmount: 1500000, endAge: 58, expectedGrowthRate: 1.5, baseDate: '2026-01-01' })
        ]
    }),

    // 50대 은퇴가속기 (55세)
    preset50s: () => ({
        profile: new UserProfile({
            birthYear: 1971,
            retirementAge: 60,
            targetEndAge: 100,
            currentMonthlyExpenses: 4200000,
            monthlyExpenses: 3500000,
            medicalExpenseRatio: 0.12,
            inflationRate: 2.0,
            globalBaseDateTime: '2026-01-01'
        }),
        pensions: [
            new Pension({ id: 'p_50_1', name: '국민연금', type: 'NATIONAL', startAge: 65, endAge: 100, expectedMonthlyAmount: 1950000, expectedGrowthRate: 2.0, claimOffsetYears: 0, baseDate: '2026-01-01' }),
            new Pension({ id: 'p_50_2', name: '퇴직연금 DB/DC', type: 'RETIREMENT', startAge: 60, endAge: 80, expectedMonthlyAmount: 1650000, expectedGrowthRate: 4.0, currentBalance: 190000000, monthlyContribution: 800000, contributionEndAge: 60, isTaxDeductionEligible: true, baseDate: '2026-01-01' }),
            new Pension({ id: 'p_50_3', name: '개인연금저축', type: 'PERSONAL', startAge: 60, endAge: 85, expectedMonthlyAmount: 1100000, expectedGrowthRate: 4.5, currentBalance: 130000000, monthlyContribution: 750000, contributionEndAge: 60, isTaxDeductionEligible: true, baseDate: '2026-01-01' }),
            new Pension({ id: 'p_50_4', name: '개인연금보험 (비과세)', type: 'ANNUITY_INSURANCE', startAge: 60, endAge: 90, expectedMonthlyAmount: 500000, expectedGrowthRate: 3.5, currentBalance: 40000000, monthlyContribution: 300000, contributionEndAge: 60, isTaxDeductionEligible: false, baseDate: '2026-01-01' }),
            new Pension({ id: 'p_50_5', name: '주택연금 (역모기지)', type: 'HOUSING', startAge: 70, endAge: 100, expectedMonthlyAmount: 1350000, expectedGrowthRate: 0.0, baseDate: '2026-01-01' })
        ],
        assets: [
            new Asset({ id: 'a_50_1', name: '보유 주택', type: 'REAL_ESTATE', currentValue: 850000000, expectedGrowthRate: 2.0, isLiability: false, baseDate: '2026-01-01' }),
            new Asset({ id: 'a_50_2', name: '안전 배당주 및 채권', type: 'BOND', currentValue: 180000000, expectedGrowthRate: 4.8, isLiability: false, baseDate: '2026-01-01' }),
            new Asset({ id: 'a_50_3', name: '정기예금', type: 'DEPOSIT', currentValue: 100000000, expectedGrowthRate: 3.2, isLiability: false, baseDate: '2026-01-01' }),
            new Asset({ id: 'a_50_4', name: '잔여 대출(부채)', type: 'DEBT', currentValue: 30000000, expectedGrowthRate: 3.9, repaymentMethod: 'EQUAL_PRINCIPAL_AND_INTEREST', maturityYears: 5, isLiability: true, baseDate: '2026-01-01' })
        ],
        incomes: [
            new Income({ id: 'i_50_1', name: '본인 급여', type: 'SALARY', monthlyAmount: 6500000, endAge: 60, expectedGrowthRate: 1.0, baseDate: '2026-01-01' })
        ]
    }),

    // 60대 은퇴생활기 (63세)
    preset60s: () => ({
        profile: new UserProfile({
            birthYear: 1963,
            retirementAge: 60,
            targetEndAge: 100,
            currentMonthlyExpenses: 2800000,
            monthlyExpenses: 2800000,
            medicalExpenseRatio: 0.15,
            inflationRate: 2.0,
            globalBaseDateTime: '2026-01-01'
        }),
        pensions: [
            new Pension({ id: 'p_60_1', name: '국민연금', type: 'NATIONAL', startAge: 63, endAge: 100, expectedMonthlyAmount: 1650000, expectedGrowthRate: 2.0, claimOffsetYears: 0, baseDate: '2026-01-01' }),
            new Pension({ id: 'p_60_2', name: '퇴직연금 (IRP 분할수령)', type: 'RETIREMENT', startAge: 60, endAge: 80, expectedMonthlyAmount: 1100000, expectedGrowthRate: 3.8, currentBalance: 150000000, monthlyContribution: 0, contributionEndAge: 60, isTaxDeductionEligible: true, baseDate: '2026-01-01' }),
            new Pension({ id: 'p_60_3', name: '개인연금저축', type: 'PERSONAL', startAge: 60, endAge: 85, expectedMonthlyAmount: 650000, expectedGrowthRate: 4.0, currentBalance: 90000000, monthlyContribution: 0, contributionEndAge: 60, isTaxDeductionEligible: true, baseDate: '2026-01-01' }),
            new Pension({ id: 'p_60_4', name: '주택연금 (역모기지)', type: 'HOUSING', startAge: 70, endAge: 100, expectedMonthlyAmount: 1200000, expectedGrowthRate: 0.0, baseDate: '2026-01-01' })
        ],
        assets: [
            new Asset({ id: 'a_60_1', name: '거주 주택', type: 'REAL_ESTATE', currentValue: 650000000, expectedGrowthRate: 1.8, isLiability: false, baseDate: '2026-01-01' }),
            new Asset({ id: 'a_60_2', name: '배당주 및 안정형 펀드', type: 'STOCK', currentValue: 120000000, expectedGrowthRate: 4.5, isLiability: false, baseDate: '2026-01-01' }),
            new Asset({ id: 'a_60_3', name: '비상금 및 MMF', type: 'DEPOSIT', currentValue: 40000000, expectedGrowthRate: 2.8, isLiability: false, baseDate: '2026-01-01' })
        ],
        incomes: [
            new Income({ id: 'i_60_1', name: '시니어 자문/소일거리', type: 'OTHER', monthlyAmount: 1000000, endAge: 68, expectedGrowthRate: 0.0, baseDate: '2026-01-01' })
        ]
    })
};
