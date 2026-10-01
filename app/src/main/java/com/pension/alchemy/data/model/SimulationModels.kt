package com.pension.alchemy.data.model

import kotlinx.serialization.Serializable

/**
 * 나이별 시뮬레이션 단일 연도 상세 결과
 */
@Serializable
data class YearlySimulationResult(
    val age: Int,
    val year: Int,
    val stage: String, // 축적기, 소득 크레바스, 은퇴기
    val isCrevasse: Boolean,
    
    // 자산 현황 (원 단위)
    val totalGrossAssets: Long,   // 총 자산 (금융 + 연금적립금 + 부동산)
    val totalDebt: Long,          // 총 부채
    val netAssetValue: Long,      // 순자산 (총 자산 - 총 부채)
    val financialAssets: Long,    // 유동/투자 금융자산
    val pensionAssets: Long,      // 연금 적립금 잔액
    val realEstateAssets: Long,   // 부동산 자산
    
    // 월 현금흐름 (원 단위)
    val monthlyTotalIncome: Long,        // 총 월 소득 (세후 근로 + 세후 실지급 연금)
    val monthlyWorkIncome: Long,         // 근로/사업 소득
    val monthlyPensionIncome: Long,      // 총 연금 실수령액 (세후 실지급)
    val grossMonthlyPensionIncome: Long = monthlyPensionIncome, // 총 연금 세전 수령액
    val monthlyPensionTax: Long = 0L,    // 월 예상 연금 소득세
    val monthlyNationalPension: Long,    // 1층 국민연금 (세후)
    val monthlyRetirementPension: Long,  // 2층 퇴직연금 (세후)
    val monthlyPersonalPension: Long,    // 3층 개인연금저축 (세후)
    val monthlyAnnuityInsurancePension: Long = 0L, // 3층 비과세 개인연금보험 (비과세 100%)
    val monthlyHousingPension: Long,     // 주택연금 (비과세 100%)
    val monthlyOtherPension: Long,       // 기타연금 (세후)
    val monthlyExpenses: Long,           // 월 생활비+의료비 지출
    val monthlyPensionContribution: Long = 0L, // 사적연금 월 정기 납입 지출액
    val monthlyDebtService: Long = 0L,         // 부채 원리금 월 상환 지출액
    val monthlyNetCashFlow: Long,        // 월 순 현금흐름 (소득 - [생활비+연금납입+대출상환])
    val isHealthInsuranceDisqualified: Boolean = false, // 공적연금 연 2,000만원 초과 (건보 피부양자 탈락 위험)
    val isPrivatePensionLimitExceeded: Boolean = false  // 사적연금 연 1,500만원 초과 (16.5% 분리과세 구간 진입)
)

/**
 * 실시간 초당 자산 수익 및 가계 순현금흐름 지표
 * 정기 수입 + 자산 기대수익 - 생활비 소비 - 대출이자 - 정기납입을 종합 산출
 */
@Serializable
data class RealTimeYieldMetrics(
    // 1. 유입 (Inflow)
    val annualRegularIncome: Long = 0L,      // 정기 수입 (근로/사업소득 연간 총액)
    val annualFinancialGain: Long = 0L,      // 금융자산 연간 기대수익 (개별 자산 기대수익률 가중합산)
    val annualRealEstateGain: Long = 0L,     // 부동산 연간 기대상승분 (개별 부동산 기대수익률 반영, 0%면 0원)
    val annualPensionGain: Long = 0L,        // 사적연금 적립금 연간 운용수익
    val annualTotalInflow: Long = 0L,        // 연간 총 유입액 (정기소득 + 자산 총수익)

    // 2. 유출 (Outflow)
    val annualLivingExpenses: Long = 0L,     // 현재 연간 생활비 소비 지출
    val annualDebtInterestCost: Long = 0L,   // 대출 부채 연간 이자비용
    val annualPensionContributionDeducted: Long = 0L, // 수입에서 직접 지출되는 사적연금 연간 납입액
    val annualTotalOutflow: Long = 0L,       // 연간 총 유출액 (생활비 + 대출이자 + 연금납입)

    // 3. 순자산 순증가 종합 (Net Growth)
    val annualNetWealthGrowth: Long = 0L,    // 연간 실질 순자산 순증가액 (총유입 - 총유출)
    val annualNetCapitalGain: Long = 0L,     // 순수 자본소득 (자산수익 - 대출이자)
    val wonPerSecond: Double = 0.0,          // 초당 실질 순자산 순증가 속도 (원/초)
    val wonPerHour: Double = 0.0,            // 시간당 순증가 (원/시간)
    val wonPerDay: Long = 0L,                // 일당 순증가 (원/일)
    val wonPerMonth: Long = 0L,              // 월당 순증가 (원/월)
    val assetWonPerSecond: Double = 0.0      // 순수 자산 운용수익 초당 증가율 (원/초, 소득/지출 제외 순수 자산 증가)
)

/**
 * 종합 시뮬레이션 요약 리포트
 */
@Serializable
data class SimulationSummary(
    val currentAge: Int,
    val endAge: Int,
    val retirementAge: Int,
    val nationalPensionStartAge: Int,
    
    // 자산 통계
    val currentNetWorth: Long,
    val currentTotalAssets: Long,
    val currentTotalDebt: Long,
    val currentFinancialAssets: Long,
    val currentPensionAssets: Long,
    val currentRealEstateAssets: Long,
    
    // 고갈 및 안전성
    val depletionAge: Int, // 0이면 endAge까지 고갈 없음
    val isSafeRetirement: Boolean,
    val peakAssetAge: Int,
    val peakAssetValue: Long,
    val finalAssetValue: Long,
    
    // 소득 대체율 및 소득
    val preRetirementMonthlyIncome: Long,
    val postRetirementMonthlyPension: Long,
    val incomeReplacementRate: Double, // % 단위 (예: 62.5%)
    
    // 소득 크레바스 (소득 공백기)
    val crevasseInfo: IncomeCrevasseInfo,
    
    // 은퇴 준비 건강도 점수
    val healthScore: RetirementHealthScore,
    
    // 실시간 초당 자산 수익 지표
    val realTimeYield: RealTimeYieldMetrics = RealTimeYieldMetrics(),
    
    // 연도별 시뮬레이션 결과 리스트
    val yearlyResults: List<YearlySimulationResult> = emptyList()
)

/**
 * 소득 크레바스(은퇴 ~ 국민연금 수령 개시 사이 공백기) 분석
 */
@Serializable
data class IncomeCrevasseInfo(
    val hasCrevasse: Boolean,
    val startAge: Int,
    val endAge: Int,
    val durationYears: Int,
    val monthlyShortfall: Long, // 월 평균 부족액
    val totalRequiredBridgeFund: Long // 필요 총 비상 브릿지 자금
)

/**
 * 은퇴 준비 건강도 점수 (0 ~ 100점)
 */
@Serializable
data class RetirementHealthScore(
    val score: Int, // 0 ~ 100
    val grade: String, // 최우수(S), 우수(A), 양호(B), 주의(C), 위험(D)
    val gradeColorHex: String,
    val feedbackMessage: String,
    val replacementRateScore: Int, // 30점 만점
    val longevitySafetyScore: Int, // 40점 만점
    val crevasseDefenseScore: Int, // 20점 만점
    val assetDiversificationScore: Int // 10점 만점
)

// --- 6대 계산기 모델 ---

@Serializable
data class AccumulationYearResult(
    val year: Int,
    val totalDeposit: Long, // 원금 누적
    val totalInterest: Long, // 복리 이자 누적
    val balance: Long        // 총 평가액
)

@Serializable
data class AccumulationResult(
    val futureValue: Long,
    val initialDeposit: Long = 0L,
    val totalMonthlyDeposits: Long = 0L,
    val totalPrincipal: Long,
    val totalInterest: Long,
    val yearlyBreakdown: List<AccumulationYearResult>
)

@Serializable
data class WithdrawalYearResult(
    val age: Int,
    val year: Int,
    val startingBalance: Long,
    val growthAmount: Long,
    val withdrawalAmount: Long,
    val endingBalance: Long
)

@Serializable
data class WithdrawalResult(
    val isDepleted: Boolean,
    val depletionAge: Int,
    val finalBalance: Long,
    val yearlyBreakdown: List<WithdrawalYearResult>
)

@Serializable
data class DepletionResult(
    val depletionYears: Double,
    val depletionAge: Double,
    val isForeverSafe: Boolean, // 원금 보존(수익률 >= 인출률)
    val recommendedSafeMonthlyWithdrawal: Long
)

@Serializable
data class RequiredWealthResult(
    val targetTotalWealth: Long,
    val monthlySavingsNeeded: Long,
    val expectedAnnualRetirementIncome: Long
)

@Serializable
data class BreakEvenAgeResult(
    val age: Int,
    val earlyCumulative: Long,   // 60세 조기수령 누적
    val normalCumulative: Long,  // 65세 정상수령 누적
    val delayedCumulative: Long  // 70세 연기수령 누적
)

@Serializable
data class NationalPensionBreakEvenResult(
    val earlyMonthlyAmount: Long,
    val normalMonthlyAmount: Long,
    val delayedMonthlyAmount: Long,
    val earlyVsNormalBreakEvenAge: Int, // 조기 vs 정상 교차 나이 (약 76세)
    val normalVsDelayedBreakEvenAge: Int, // 정상 vs 연기 교차 나이 (약 82세)
    val comparisonList: List<BreakEvenAgeResult>
)

@Serializable
data class TaxBenefitResult(
    val annualContribution: Long,
    val taxRefundAmount: Long,          // 당해 연도 세액공제 환급액 (13.2% or 16.5%)
    val cumulativeTaxRefund30Years: Long, // 30년 환급 총액
    val reinvestedFutureValue30Years: Long, // 환급금 연 6% 재투자 시 30년 후 복리 자산
    val generalTaxAccountFutureValue: Long, // 일반 과세 계좌(이자소득세 15.4% 매년 차감) 대비 차액
    val taxSavingsAlchemyBonus: Long    // 절세+과세이연 마법으로 추가 창출된 자산
)