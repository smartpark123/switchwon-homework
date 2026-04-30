package com.switchwon.forex.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 환율 계산 규칙을 담당하는 순수 유틸 클래스.
 * 상태를 갖지 않으며 모든 메서드는 static으로 단위 테스트가 용이합니다.
 */
public final class CurrencyCalculator {

    private static final BigDecimal BUY_SPREAD  = new BigDecimal("1.05");
    private static final BigDecimal SELL_SPREAD = new BigDecimal("0.95");
    private static final BigDecimal JPY_UNIT    = new BigDecimal("100");

    private CurrencyCalculator() {}

    /** 매매기준율에 5% 가산하여 전신환 매입율 반환 (소수점 2자리 반올림) */
    public static BigDecimal calcBuyRate(BigDecimal baseRate) {
        return baseRate.multiply(BUY_SPREAD).setScale(2, RoundingMode.HALF_UP);
    }

    /** 매매기준율에 5% 차감하여 전신환 매도율 반환 (소수점 2자리 반올림) */
    public static BigDecimal calcSellRate(BigDecimal baseRate) {
        return baseRate.multiply(SELL_SPREAD).setScale(2, RoundingMode.HALF_UP);
    }

    /** 1엔 단위 시세를 100엔 단위로 환산 (소수점 2자리 반올림) */
    public static BigDecimal applyJpyUnit(BigDecimal jpyRate) {
        return jpyRate.multiply(JPY_UNIT).setScale(2, RoundingMode.HALF_UP);
    }

    /** 매매기준율을 2자리 반올림으로 정규화 */
    public static BigDecimal roundBaseRate(BigDecimal rate) {
        return rate.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * KRW → 외화 매수: 고객이 지불할 KRW 금액 계산
     * forexAmount(외화) × buyRate, 소수점 이하 버림
     */
    public static BigDecimal calcKrwToBuy(BigDecimal forexAmount, BigDecimal buyRate) {
        return forexAmount.multiply(buyRate).setScale(0, RoundingMode.FLOOR);
    }

    /**
     * 외화 → KRW 매도: 고객이 수령할 KRW 금액 계산
     * forexAmount(외화) × sellRate, 소수점 이하 버림
     */
    public static BigDecimal calcKrwFromSell(BigDecimal forexAmount, BigDecimal sellRate) {
        return forexAmount.multiply(sellRate).setScale(0, RoundingMode.FLOOR);
    }
}
