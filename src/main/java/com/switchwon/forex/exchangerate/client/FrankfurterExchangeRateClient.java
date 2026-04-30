package com.switchwon.forex.exchangerate.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Frankfurter API(https://api.frankfurter.app) 를 통해 환율을 조회합니다.
 * KRW 기준 외화 환율이므로 역수 변환 후 반환합니다.
 * forex.client.use-mock=false(기본값)일 때 활성화됩니다.
 */
@Component
@ConditionalOnProperty(name = "forex.client.use-mock", havingValue = "false", matchIfMissing = true)
public class FrankfurterExchangeRateClient implements ExchangeRateClient {

    private static final String API_URL =
            "https://api.frankfurter.dev/v1/latest?from=KRW&to=USD,JPY,CNY,EUR";

    private final RestClient restClient;

    public FrankfurterExchangeRateClient() {
        this.restClient = RestClient.create();
    }

    @Override
    public Map<String, BigDecimal> fetchBaseRates() {
        FrankfurterResponse response = restClient.get()
                .uri(API_URL)
                .retrieve()
                .body(FrankfurterResponse.class);

        if (response == null || response.rates() == null) {
            throw new IllegalStateException("Frankfurter API 응답이 비어 있습니다.");
        }

        // KRW/외화 → 외화/KRW 역수 변환
        Map<String, BigDecimal> baseRates = new java.util.HashMap<>();
        response.rates().forEach((currency, rate) ->
                baseRates.put(currency, BigDecimal.ONE.divide(rate, 10, RoundingMode.HALF_UP))
        );
        return baseRates;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FrankfurterResponse(String base, Map<String, BigDecimal> rates) {}
}
