/**
 * LogNormalDistribution.js
 * 대한민국 순자산 분포를 위한 로그정규분포(Log-Normal Distribution) 계산 유틸리티
 * 2025년 대한민국 가계금융복지조사 순자산 기준 추정 모수:
 * - 평균 모수(mu): 1.00984
 * - 표준편차 모수(sigma): 1.1937
 */

const LogNormalDistribution = {
    SQRT_2PI: 2.5066282746310005,
    SQRT_2: 1.4142135623730951,

    pdf(x, mu = 1.00984, sigma = 1.1937) {
        if (x <= 0) return 0;
        const safeSigma = Math.max(sigma, 1e-4);
        const lnX = Math.log(x);
        const diff = lnX - mu;
        const exponent = -(diff * diff) / (2.0 * safeSigma * safeSigma);
        const denominator = x * safeSigma * this.SQRT_2PI;
        return Math.exp(exponent) / denominator;
    },

    standardNormalCdf(z) {
        if (isNaN(z)) return 0.5;
        const absZ = Math.abs(z);

        const p = 0.2316419;
        const b1 = 0.319381530;
        const b2 = -0.356563782;
        const b3 = 1.781477937;
        const b4 = -1.821255978;
        const b5 = 1.330274429;

        const t = 1.0 / (1.0 + p * absZ);
        const poly = t * (b1 + t * (b2 + t * (b3 + t * (b4 + t * b5))));
        const normalPdf = Math.exp(-0.5 * absZ * absZ) / this.SQRT_2PI;
        const cdf = 1.0 - normalPdf * poly;

        return z >= 0.0 ? cdf : 1.0 - cdf;
    },

    cdf(x, mu = 1.00984, sigma = 1.1937) {
        if (x <= 0) return 0;
        const safeSigma = Math.max(sigma, 1e-4);
        const z = (Math.log(x) - mu) / safeSigma;
        return Math.min(1.0, Math.max(0.0, this.standardNormalCdf(z)));
    },

    probabilityExceeding(x, mu = 1.00984, sigma = 1.1937) {
        if (x <= 0) return 1.0;
        return Math.min(1.0, Math.max(0.0, 1.0 - this.cdf(x, mu, sigma)));
    },

    topPercent(x, mu = 1.00984, sigma = 1.1937) {
        return this.probabilityExceeding(x, mu, sigma) * 100.0;
    },

    standardNormalInverseCdf(p) {
        const safeP = Math.min(1.0 - 1e-12, Math.max(1e-12, p));

        const a1 = -3.969683028665376e+01;
        const a2 = 2.209460984245205e+02;
        const a3 = -2.759285104469687e+02;
        const a4 = 1.383577518672690e+02;
        const a5 = -3.066479806614716e+01;
        const a6 = 2.506628277459239e+00;

        const b1 = -5.447609879822406e+01;
        const b2 = 1.615858368580409e+02;
        const b3 = -1.556989798598866e+02;
        const b4 = 6.680131188771972e+01;
        const b5 = -1.328068155288572e+01;

        const c1 = -7.784894002430293e-03;
        const c2 = -3.223964580411365e-01;
        const c3 = -2.400758277161838e+00;
        const c4 = -2.549732539343734e+00;
        const c5 = 4.374664141464968e+00;
        const c6 = 2.938163982698783e+00;

        const d1 = 7.784695709041462e-03;
        const d2 = 3.224671290700398e-01;
        const d3 = 2.445134137142996e+00;
        const d4 = 3.754408661907416e+00;

        const pLow = 0.02425;
        const pHigh = 1.0 - pLow;

        if (safeP < pLow) {
            const q = Math.sqrt(-2.0 * Math.log(safeP));
            return (((((c1 * q + c2) * q + c3) * q + c4) * q + c5) * q + c6) /
                   ((((d1 * q + d2) * q + d3) * q + d4) * q + 1.0);
        }
        if (safeP <= pHigh) {
            const q = safeP - 0.5;
            const r = q * q;
            return (((((a1 * r + a2) * r + a3) * r + a4) * r + a5) * r + a6) * q /
                   (((((b1 * r + b2) * r + b3) * r + b4) * r + b5) * r + 1.0);
        }
        const q = Math.sqrt(-2.0 * Math.log(1.0 - safeP));
        return -(((((c1 * q + c2) * q + c3) * q + c4) * q + c5) * q + c6) /
                ((((d1 * q + d2) * q + d3) * q + d4) * q + 1.0);
    },

    quantile(p, mu = 1.00984, sigma = 1.1937) {
        const safeSigma = Math.max(sigma, 1e-4);
        const z = this.standardNormalInverseCdf(p);
        return Math.exp(mu + safeSigma * z);
    },

    topPercentileValue(topPercent, mu = 1.00984, sigma = 1.1937) {
        const p = Math.min(0.9999, Math.max(0.0001, 1.0 - topPercent / 100.0));
        return this.quantile(p, mu, sigma);
    },

    formatPXExceeds(x, prob) {
        const percent = prob * 100.0;
        const percentStr = percent < 0.1 ? percent.toFixed(2) : percent.toFixed(1);
        const xStr = (x % 1 === 0) ? x.toFixed(0) : x.toFixed(2).replace(/\.?0+$/, '');
        return `P(X > ${xStr}억) = ${percentStr}%`;
    }
};
