package com.switchwon.forex.exchangerate.scheduler;

import com.switchwon.forex.exchangerate.client.ExchangeRateClient;
import com.switchwon.forex.exchangerate.entity.ExchangeRateHistory;
import com.switchwon.forex.exchangerate.service.ExchangeRateService;
import com.switchwon.forex.util.CurrencyCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ExchangeRateScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateScheduler.class);

    private final ExchangeRateClient exchangeRateClient;
    private final ExchangeRateService exchangeRateService;

    public ExchangeRateScheduler(ExchangeRateClient exchangeRateClient,
                                 ExchangeRateService exchangeRateService) {
        this.exchangeRateClient = exchangeRateClient;
        this.exchangeRateService = exchangeRateService;
    }

    /**
     * 1분 주기로 환율을 수집합니다.
     * fixedDelay: 이전 작업 완료 후 1분 대기 (중복 실행 방지)
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 1_000)
    public void collect() {
        log.info("[Scheduler] 환율 수집 시작");
        try {
            Map<String, BigDecimal> baseRates = exchangeRateClient.fetchBaseRates();
            LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

            List<ExchangeRateHistory> histories = new ArrayList<>();
            baseRates.forEach((currency, rawBase) -> {
                BigDecimal baseRate = "JPY".equals(currency)
                        ? CurrencyCalculator.applyJpyUnit(rawBase)
                        : CurrencyCalculator.roundBaseRate(rawBase);

                histories.add(new ExchangeRateHistory(
                        currency,
                        baseRate,
                        CurrencyCalculator.calcBuyRate(baseRate),
                        CurrencyCalculator.calcSellRate(baseRate),
                        now
                ));
            });

            exchangeRateService.saveAll(histories);
            log.info("[Scheduler] 환율 수집 완료 — {} 건 저장", histories.size());
        } catch (Exception e) {
            log.error("[Scheduler] 환율 수집 실패: {}", e.getMessage(), e);
        }
    }
}
