package com.pension.alchemy.domain.engine

import com.pension.alchemy.data.model.PolicySettings
import kotlin.math.roundToLong

/**
 * 대한민국 연금소득 과세 체계 (사용자 정의 PolicySettings 연동)
 *
 * 1. 공적연금 (국민연금 등):
 *    - 연간 총 수령액에서 연금소득공제(최대 한도 policy.maxPensionIncomeDeduction) 및 인적공제(policy.basicPersonalDeduction) 차감 후 종합소득세율 적용
 *    - 건강보험 피부양자 자격: 연간 공적연금 소득이 policy.healthInsurancePensionLimit 초과 시 피부양자 박탈
 *
 * 2. 퇴직연금 (IRP 등 연금 수령):
 *    - 퇴직금을 일시금 대신 연금(10년 이상)으로 분할 수령 시 이연퇴직소득세 감면
 *    - 1~10년차: policy.retirementTaxDiscountRateEarly% 감면
 *    - 11년차 이상: policy.retirementTaxDiscountRateLate% 감면
 *
 * 3. 개인연금저축 (세제적격 연금계좌 - 연금저축/IRP 세액공제분 및 운용수익):
 *    - 연간 사적연금 수령액 policy.privatePensionAnnualLimit 이하 시 연령별 저율 분리과세
 *      • 만 55세 ~ 69세: policy.privatePensionRateUnder70%
 *      • 만 70세 ~ 79세: policy.privatePensionRateUnder80%
 *      • 만 80세 이상: policy.privatePensionRate80OrOver%
 *    - 한도 초과 시: 한도까지는 저율 분리과세, 초과분은 policy.privatePensionExcessRate% 분리과세
 *
 * 4. 개인연금보험 (세제비적격 비과세):
 *    - 10년 이상 유지 시 비과세 요건 충족으로 0% (완전 비과세)
 *
 * 5. 주택연금 (역모기지):
 *    - 역모기지 대출금 성격으로 0% (완전 비과세)
 *
 * 6. 기타연금:
 *    - 기본 5.5% 분리과세
 */
object PensionTaxCalculator {

    /**
     * 국민연금/공적연금 연간 세액 계산 (연금소득공제 및 종합소득세율 적용)
     */
    fun calculateAnnualNationalPensionTax(
        annualGrossAmount: Long,
        policy: PolicySettings = PolicySettings()
    ): Long {
        if (annualGrossAmount <= 0L) return 0L

        // 연금소득공제 (법정 구간 및 설정된 최대 한도)
        val pensionDeduction = when {
            annualGrossAmount <= 3_500_000L -> annualGrossAmount
            annualGrossAmount <= 7_000_000L -> (3_500_000L + (annualGrossAmount - 3_500_000L) * 0.4).roundToLong()
            annualGrossAmount <= 14_000_000L -> (4_900_000L + (annualGrossAmount - 7_000_000L) * 0.2).roundToLong()
            else -> (6_300_000L + (annualGrossAmount - 14_000_000L) * 0.1).roundToLong()
                .coerceAtMost(policy.maxPensionIncomeDeduction)
        }

        // 1인 기본 인적공제
        val taxableBase = (annualGrossAmount - pensionDeduction - policy.basicPersonalDeduction).coerceAtLeast(0L)

        // 종합소득세율 (지방소득세 10% 포함 기본세율)
        return when {
            taxableBase <= 0L -> 0L
            taxableBase <= 14_000_000L -> (taxableBase * 0.066).roundToLong()
            taxableBase <= 50_000_000L -> (924_000L + (taxableBase - 14_000_000L) * 0.165).roundToLong()
            taxableBase <= 88_000_000L -> (6_864_000L + (taxableBase - 50_000_000L) * 0.264).roundToLong()
            else -> (16_896_000L + (taxableBase - 88_000_000L) * 0.385).roundToLong()
        }
    }

    /**
     * 퇴직연금 월 세액 계산 (10년 이내 vs 11년차 이상 차등 감면율)
     */
    fun calculateMonthlyRetirementPensionTax(
        monthlyGrossAmount: Long,
        yearsSinceStart: Int,
        policy: PolicySettings = PolicySettings()
    ): Long {
        if (monthlyGrossAmount <= 0L) return 0L
        val discountRate = if (yearsSinceStart <= 10) {
            policy.retirementTaxDiscountRateEarly / 100.0
        } else {
            policy.retirementTaxDiscountRateLate / 100.0
        }
        val effectiveRate = (policy.baseRetirementTaxRate / 100.0) * (1.0 - discountRate)
        return (monthlyGrossAmount * effectiveRate).roundToLong()
    }

    /**
     * 개인연금저축(세제적격) 연간 세액 계산
     * - 설정된 한도 이하: 연령별 저율 분리과세
     * - 설정된 한도 초과분: 초과분 분리과세율 적용
     */
    fun calculateAnnualPersonalPensionTax(
        annualGrossAmount: Long,
        age: Int,
        policy: PolicySettings = PolicySettings()
    ): Long {
        if (annualGrossAmount <= 0L) return 0L

        val baseRate = when {
            age < 70 -> policy.privatePensionRateUnder70 / 100.0
            age < 80 -> policy.privatePensionRateUnder80 / 100.0
            else -> policy.privatePensionRate80OrOver / 100.0
        }

        return if (annualGrossAmount <= policy.privatePensionAnnualLimit) {
            (annualGrossAmount * baseRate).roundToLong()
        } else {
            val baseTax = (policy.privatePensionAnnualLimit * baseRate).roundToLong()
            val excessRate = policy.privatePensionExcessRate / 100.0
            val excessTax = ((annualGrossAmount - policy.privatePensionAnnualLimit) * excessRate).roundToLong()
            baseTax + excessTax
        }
    }

    /**
     * 개인연금보험(세제비적격 비과세): 10년 이상 유지 시 전액 비과세 (세율 0%)
     */
    fun calculateMonthlyAnnuityInsuranceTax(): Long = 0L

    /**
     * 주택연금(역모기지 대출금): 전액 비과세 (세율 0%)
     */
    fun calculateMonthlyHousingPensionTax(): Long = 0L

    /**
     * 기타 사적연금 월 세액: 기본 5.5% 분리과세
     */
    fun calculateMonthlyOtherPensionTax(monthlyGrossAmount: Long): Long {
        if (monthlyGrossAmount <= 0L) return 0L
        return (monthlyGrossAmount * 0.055).roundToLong()
    }
}
