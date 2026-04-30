package com.switchwon.forex.exchangerate.dto;

import com.switchwon.forex.exchangerate.entity.ExchangeRateHistory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExchangeRateDto(
        String currency,
        BigDecimal buyRate,
        BigDecimal tradeStanRate,
        BigDecimal sellRate,
        LocalDateTime dateTime
) {
    public static ExchangeRateDto from(ExchangeRateHistory entity) {
        return new ExchangeRateDto(
                entity.getCurrency(),
                entity.getBuyRate(),
                entity.getTradeStanRate(),
                entity.getSellRate(),
                entity.getCollectedAt()
        );
    }
}
