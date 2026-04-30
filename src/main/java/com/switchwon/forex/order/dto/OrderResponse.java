package com.switchwon.forex.order.dto;

import com.switchwon.forex.order.entity.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        BigDecimal fromAmount,
        String fromCurrency,
        BigDecimal toAmount,
        String toCurrency,
        BigDecimal tradeRate,
        LocalDateTime dateTime
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                normalizeAmount(order.getFromAmount()),
                order.getFromCurrency(),
                normalizeAmount(order.getToAmount()),
                order.getToCurrency(),
                order.getTradeRate(),
                order.getCreatedAt()
        );
    }

    private static BigDecimal normalizeAmount(BigDecimal amount) {
        // API 응답 가독성을 위해 trailing zero를 1자리 소수까지만 노출
        return amount.setScale(1, RoundingMode.DOWN);
    }
}
