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
                formatted = `${eokVal.toFixed(1)}억`;
            } else if (man > 0) {
                const manVal = absAmount / 10000.0;
                formatted = `${Math.round(manVal)}만`;
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

    formatPercent(rate) {
        return `${(rate || 0).toFixed(1)}%`;
    }
};
