package com.switchwon.forex.exchangerate.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Random;

/**
 * 외부 API 장애 또는 주말 대응용 Mock 구현체.
 * forex.client.use-mock=true 또는 --spring.profiles.active=mock 시 활성화됩니다.
 * 현실적인 시세 범위 내에서 ±0.5% 노이즈를 추가하여 스케줄러 동작을 확인합니다.
 */
@Component
@ConditionalOnProperty(name = "forex.client.use-mock", havingValue = "true")
public class MockExchangeRateClient implements ExchangeRateClient {

    private static final Map<String, BigDecimal> BASE_RATES = Map.of(
            "USD", new BigDecimal("1377.00"),
            "JPY", new BigDecimal("9.10"),    // 1엔 기준 (100엔 환산은 Scheduler에서)
            "CNY", new BigDecimal("189.50"),
            "EUR", new BigDecimal("1520.00")
    );

    private final Random random = new Random();

    @Override
    public Map<String, BigDecimal> fetchBaseRates() {
        Map<String, BigDecimal> rates = new java.util.HashMap<>();
        BASE_RATES.forEach((currency, base) ->
                rates.put(currency, applyNoise(base))
        );
        return rates;
    }

    /** 기준 시세에 ±0.5% 랜덤 노이즈 적용 */
    private BigDecimal applyNoise(BigDecimal base) {
        double noise = 1.0 + (random.nextDouble() - 0.5) * 0.01;
        return base.multiply(BigDecimal.valueOf(noise))
                   .setScale(10, RoundingMode.HALF_UP);
    }
}
