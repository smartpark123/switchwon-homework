package com.switchwon.forex.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CurrencyCalculator 계산 규칙 검증")
class CurrencyCalculatorTest {

    @Test
    @DisplayName("매입율 = 매매기준율 × 1.05, 소수점 2자리 반올림")
    void calcBuyRate() {
        BigDecimal result = CurrencyCalculator.calcBuyRate(new BigDecimal("1477.45"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("1551.32"));
    }

    @Test
    @DisplayName("매도율 = 매매기준율 × 0.95, 소수점 2자리 반올림")
    void calcSellRate() {
        BigDecimal result = CurrencyCalculator.calcSellRate(new BigDecimal("1477.45"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("1403.58"));
    }

    @Test
    @DisplayName("JPY 1엔 기준 → 100엔 환산")
    void applyJpyUnit() {
        BigDecimal result = CurrencyCalculator.applyJpyUnit(new BigDecimal("9.105"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("910.50"));
    }

    @Test
    @DisplayName("KRW→외화 매수: KRW 금액 = forexAmount × buyRate, 소수점 버림")
    void calcKrwToBuy() {
        // 200 USD × 1480.43 = 296086.00 → floor = 296086
        BigDecimal result = CurrencyCalculator.calcKrwToBuy(
                new BigDecimal("200"), new BigDecimal("1480.43"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("296086"));
    }

    @Test
    @DisplayName("외화→KRW 매도: KRW 금액 = forexAmount × sellRate, 소수점 버림")
    void calcKrwFromSell() {
        // 133 USD × 1474.47 = 196104.51 → floor = 196104
        BigDecimal result = CurrencyCalculator.calcKrwFromSell(
                new BigDecimal("133"), new BigDecimal("1474.47"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("196104"));
    }
}
