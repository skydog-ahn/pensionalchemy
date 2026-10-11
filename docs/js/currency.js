/**
 * PensionAlchemy Currency & Number Formatter
 */

const CurrencyFormatter = {
    decimalFormat(num) {
        return Math.round(num).toLocaleString('ko-KR');
    },

    format(amount, currency = 'KRW', isShort = false) {
        if (currency === 'KRW') {
            return this.formatKoreanWon(amount, isShort);
        }
        const prefix = currency === 'USD' ? '$' : currency === 'JPY' ? '¥' : currency === 'EUR' ? '€' : `${currency} `;
        const absVal = Math.abs(amount);
        if (isShort) {
            if (absVal >= 1e9) return `${prefix}${(amount / 1e9).toFixed(1)}B`;
            if (absVal >= 1e6) return `${prefix}${(amount / 1e6).toFixed(1)}M`;
            if (absVal >= 1e3) return `${prefix}${(amount / 1e3).toFixed(1)}K`;
            return `${prefix}${this.decimalFormat(amount)}`;
        }
        return `${prefix}${this.decimalFormat(amount)}`;
    },

    formatKoreanWon(amount, isShort = false) {
        const num = Math.round(amount || 0);
        if (num === 0) return "0원";

        const isNegative = num < 0;
        const absAmount = Math.abs(num);

        const eok = Math.floor(absAmount / 100000000);
        const remainder = absAmount % 100000000;
        const man = Math.floor(remainder / 10000);
        const won = remainder % 10000;

        let formatted = "";
        if (isShort) {
            if (eok > 0) {
                const eokVal = absAmount / 100000000.0;
                if (absAmount % 100000000 === 0) {
                    formatted = `${this.decimalFormat(eok)}억원`;
                } else {
                    formatted = `${eokVal.toFixed(1)}억원`;
                }
            } else if (man > 0) {
                const manVal = Math.floor(absAmount / 10000);
                formatted = `${this.decimalFormat(manVal)}만원`;
            } else {
                formatted = `${this.decimalFormat(absAmount)}원`;
            }
        } else {
            if (eok > 0 && man > 0) {
                formatted = `${this.decimalFormat(eok)}억 ${this.decimalFormat(man)}만원`;
            } else if (eok > 0) {
                formatted = `${this.decimalFormat(eok)}억원`;
            } else if (man > 0 && won > 0) {
                formatted = `${this.decimalFormat(man)}만 ${this.decimalFormat(won)}원`;
            } else if (man > 0) {
                formatted = `${this.decimalFormat(man)}만원`;
            } else {
                formatted = `${this.decimalFormat(won)}원`;
            }
        }

        return isNegative ? `-${formatted}` : formatted;
    },

    formatToManWon(amount) {
        const num = Math.round(amount || 0);
        if (num === 0) return "0원";

        const isNegative = num < 0;
        const absAmount = Math.abs(num);

        const eok = Math.floor(absAmount / 100000000);
        const remainder = absAmount % 100000000;
        const man = Math.floor(remainder / 10000);

        let formatted = "";
        if (eok > 0 && man > 0) {
            formatted = `${this.decimalFormat(eok)}억 ${this.decimalFormat(man)}만원`;
        } else if (eok > 0) {
            formatted = `${this.decimalFormat(eok)}억원`;
        } else if (man > 0) {
            formatted = `${this.decimalFormat(man)}만원`;
        } else {
            formatted = "0원";
        }

        return isNegative ? `-${formatted}` : formatted;
    },

    formatMonthly(amount, currency = 'KRW') {
        return `${this.format(amount, currency, false)}/월`;
    },

    /**
     * 주기별 환산 속도(만원/시, 만원/일, 만원/월) 포맷팅
     * 기본 만원 단위로 띄어쓰기 없이 붙여서 표기하며,
     * 시간(만원/시)은 1만원 이상이라도 소수점 둘째 자리까지 반올림하여 표기 (예: 44,280 -> 4.43만원/시)
     * 일/월(만원/일, 만원/월)은 만원 미만 시에만 소수점 둘째 자리까지 표기 (예: 1,060,000 -> 106만원/일)
     * 예: 5,420 -> "0.54만원/시", 44,280 -> "4.43만원/시", 1,060,000 -> "106만원/일"
     */
    formatRate(amount, unit) {
        const num = Number(amount || 0);
        const isNegative = num < 0;
        const absAmount = Math.abs(num);
        const sign = isNegative ? '-' : '';

        const normalizedUnit = unit.startsWith('원/') ? `만${unit}` : unit;
        const isHourly = normalizedUnit.includes('/시');

        let formattedNum = '0';
        if (absAmount === 0) {
            formattedNum = '0';
        } else if (isHourly) {
            const manDecimal = absAmount / 10000.0;
            formattedNum = manDecimal.toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        } else if (absAmount < 10000) {
            const manDecimal = absAmount / 10000.0;
            const str = manDecimal.toFixed(2);
            formattedNum = (str === '1.00') ? '1' : (str === '0.00') ? '0' : str;
        } else {
            const manVal = Math.round(absAmount / 10000.0);
            formattedNum = this.decimalFormat(manVal);
        }

        return `${sign}${formattedNum}${normalizedUnit}`;
    },

    formatPercent(rate) {
        return `${(rate || 0).toFixed(1)}%`;
    }
};
