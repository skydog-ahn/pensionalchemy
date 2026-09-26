package com.pension.alchemy.util

import kotlin.math.*

/**
 * 대한민국 순자산 분포를 위한 로그정규분포(Log-Normal Distribution) 계산 유틸리티
 *
 * 확률변수 X (단위: 억원)가 로그정규분포를 따를 때,
 * ln(X) ~ N(mu, sigma^2)
 *
 * 2025년 대한민국 가계금융복지조사 순자산 기준 추정 모수:
 * - 평균 모수(mu): 1.00984
 * - 표준편차 모수(sigma): 1.1937
 */
object LogNormalDistribution {

    private const val SQRT_2PI = 2.506628274631000502415765284811
    private const val SQRT_2 = 1.414213562373095048801688724209

    /**
     * 로그정규분포의 확률밀도함수(PDF) f(x)
     * @param x 순자산 (단위: 억원, x > 0)
     * @param mu 로그 스케일 평균 모수 (기본값: 1.00984)
     * @param sigma 로그 스케일 표준편차 모수 (기본값: 1.1937)
     */
    fun pdf(x: Double, mu: Double = 1.00984, sigma: Double = 1.1937): Double {
        if (x <= 0.0) return 0.0
        val safeSigma = max(sigma, 1e-4)
        val lnX = ln(x)
        val diff = lnX - mu
        val exponent = -(diff * diff) / (2.0 * safeSigma * safeSigma)
        val denominator = x * safeSigma * SQRT_2PI
        return exp(exponent) / denominator
    }

    /**
     * 표준정규분포의 누적분포함수 Phi(z)
     * Abramowitz and Stegun Formula 7.1.26 (최대 절대 오차 < 1.5e-7)
     */
    fun standardNormalCdf(z: Double): Double {
        if (z.isNaN()) return 0.5
        val absZ = abs(z)

        val p = 0.2316419
        val b1 = 0.319381530
        val b2 = -0.356563782
        val b3 = 1.781477937
        val b4 = -1.821255978
        val b5 = 1.330274429

        val t = 1.0 / (1.0 + p * absZ)
        val poly = t * (b1 + t * (b2 + t * (b3 + t * (b4 + t * b5))))
        val normalPdf = exp(-0.5 * absZ * absZ) / SQRT_2PI
        val cdf = 1.0 - normalPdf * poly

        return if (z >= 0.0) cdf else 1.0 - cdf
    }

    /**
     * 로그정규분포의 누적분포함수(CDF) F(x) = P(X <= x)
     * @param x 순자산 (단위: 억원)
     */
    fun cdf(x: Double, mu: Double = 1.00984, sigma: Double = 1.1937): Double {
        if (x <= 0.0) return 0.0
        val safeSigma = max(sigma, 1e-4)
        val z = (ln(x) - mu) / safeSigma
        return standardNormalCdf(z).coerceIn(0.0, 1.0)
    }

    /**
     * 상위 확률 P(X > x) = 1 - CDF(x)
     * @param x 순자산 (단위: 억원)
     * @return 0.0 ~ 1.0 사이의 확률 (예: 0.15 = 상위 15%)
     */
    fun probabilityExceeding(x: Double, mu: Double = 1.00984, sigma: Double = 1.1937): Double {
        if (x <= 0.0) return 1.0
        return (1.0 - cdf(x, mu, sigma)).coerceIn(0.0, 1.0)
    }

    /**
     * 상위 백분율 (0% ~ 100%)
     * @param x 순자산 (단위: 억원)
     * @return 상위 몇 %인지 반환 (예: 15.2% -> 15.2)
     */
    fun topPercent(x: Double, mu: Double = 1.00984, sigma: Double = 1.1937): Double {
        return probabilityExceeding(x, mu, sigma) * 100.0
    }

    /**
     * 표준정규분포의 분위수 함수 (Inverse CDF, Probit function)
     * Peter J. Acklam's algorithm (오차 < 1.15e-9)
     */
    fun standardNormalInverseCdf(p: Double): Double {
        val safeP = p.coerceIn(1e-12, 1.0 - 1e-12)

        val a1 = -3.969683028665376e+01
        val a2 = 2.209460984245205e+02
        val a3 = -2.759285104469687e+02
        val a4 = 1.383577518672690e+02
        val a5 = -3.066479806614716e+01
        val a6 = 2.506628277459239e+00

        val b1 = -5.447609879822406e+01
        val b2 = 1.615858368580409e+02
        val b3 = -1.556989798598866e+02
        val b4 = 6.680131188771972e+01
        val b5 = -1.328068155288572e+01

        val c1 = -7.784894002430293e-03
        val c2 = -3.223964580411365e-01
        val c3 = -2.400758277161838e+00
        val c4 = -2.549732539343734e+00
        val c5 = 4.374664141464968e+00
        val c6 = 2.938163982698783e+00

        val d1 = 7.784695709041462e-03
        val d2 = 3.224671290700398e-01
        val d3 = 2.445134137142996e+00
        val d4 = 3.754408661907416e+00

        val pLow = 0.02425
        val pHigh = 1.0 - pLow

        return when {
            safeP < pLow -> {
                val q = sqrt(-2.0 * ln(safeP))
                (((((c1 * q + c2) * q + c3) * q + c4) * q + c5) * q + c6) /
                    ((((d1 * q + d2) * q + d3) * q + d4) * q + 1.0)
            }
            safeP <= pHigh -> {
                val q = safeP - 0.5
                val r = q * q
                (((((a1 * r + a2) * r + a3) * r + a4) * r + a5) * r + a6) * q /
                    (((((b1 * r + b2) * r + b3) * r + b4) * r + b5) * r + 1.0)
            }
            else -> {
                val q = sqrt(-2.0 * ln(1.0 - safeP))
                -(((((c1 * q + c2) * q + c3) * q + c4) * q + c5) * q + c6) /
                    ((((d1 * q + d2) * q + d3) * q + d4) * q + 1.0)
            }
        }
    }

    /**
     * 누적확률 p에 해당하는 자산 값 x (단위: 억원)
     * P(X <= x) = p
     */
    fun quantile(p: Double, mu: Double = 1.00984, sigma: Double = 1.1937): Double {
        val safeSigma = max(sigma, 1e-4)
        val z = standardNormalInverseCdf(p)
        return exp(mu + safeSigma * z)
    }

    /**
     * 상위 topPercent%에 해당하는 자산 값 x (단위: 억원)
     * 예: 상위 10% -> topPercentileValue(10.0) -> 약 12.68억원
     *     상위 50%(중위값) -> topPercentileValue(50.0) -> 약 2.75억원
     */
    fun topPercentileValue(topPercent: Double, mu: Double = 1.00984, sigma: Double = 1.1937): Double {
        val p = (1.0 - topPercent / 100.0).coerceIn(0.0001, 0.9999)
        return quantile(p, mu, sigma)
    }

    /**
     * P(X > x) 형식 문자열 포맷팅
     * @param x 억원 단위 자산값
     * @param prob 상위 확률 (0.0 ~ 1.0)
     */
    fun formatPXExceeds(x: Double, prob: Double): String {
        val percent = prob * 100.0
        val percentStr = if (percent < 0.1) {
            String.format(java.util.Locale.US, "%.2f", percent)
        } else {
            String.format(java.util.Locale.US, "%.1f", percent)
        }
        val xStr = if (x % 1.0 == 0.0) {
            String.format(java.util.Locale.US, "%.0f", x)
        } else {
            String.format(java.util.Locale.US, "%.2f", x).trimEnd('0').trimEnd('.')
        }
        return "P(X > ${xStr}억) = $percentStr%"
    }
}
