package com.switchwon.forex.order.service;

import com.switchwon.forex.common.exception.BusinessException;
import com.switchwon.forex.common.exception.ErrorCode;
import com.switchwon.forex.exchangerate.entity.ExchangeRateHistory;
import com.switchwon.forex.exchangerate.repository.ExchangeRateHistoryRepository;
import com.switchwon.forex.order.dto.OrderListResponse;
import com.switchwon.forex.order.dto.OrderRequest;
import com.switchwon.forex.order.dto.OrderResponse;
import com.switchwon.forex.order.entity.Order;
import com.switchwon.forex.order.repository.OrderRepository;
import com.switchwon.forex.util.CurrencyCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private static final String KRW = "KRW";

    private final OrderRepository orderRepository;
    private final ExchangeRateHistoryRepository exchangeRateHistoryRepository;

    public OrderService(OrderRepository orderRepository,
                        ExchangeRateHistoryRepository exchangeRateHistoryRepository) {
        this.orderRepository = orderRepository;
        this.exchangeRateHistoryRepository = exchangeRateHistoryRepository;
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        String from = request.fromCurrency().toUpperCase();
        String to   = request.toCurrency().toUpperCase();

        boolean isBuy  = KRW.equals(from) && !KRW.equals(to);
        boolean isSell = !KRW.equals(from) && KRW.equals(to);

        if (!isBuy && !isSell) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_REQUEST);
        }

        String forexCurrency = isBuy ? to : from;
        ExchangeRateHistory rate = exchangeRateHistoryRepository
                .findTopByCurrencyOrderByCollectedAtDesc(forexCurrency)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUND));

        BigDecimal forexAmount = request.forexAmount();
        BigDecimal fromAmount;
        BigDecimal toAmount;
        BigDecimal tradeRate;

        if (isBuy) {
            tradeRate  = rate.getBuyRate();
            fromAmount = CurrencyCalculator.calcKrwToBuy(forexAmount, tradeRate);
            toAmount   = forexAmount;
        } else {
            tradeRate  = rate.getSellRate();
            fromAmount = forexAmount;
            toAmount   = CurrencyCalculator.calcKrwFromSell(forexAmount, tradeRate);
        }

        Order order = new Order(from, to, forexAmount, fromAmount, toAmount, tradeRate,
                                LocalDateTime.now().withSecond(0).withNano(0));
        Order saved = orderRepository.save(order);
        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderListResponse getOrderList() {
        List<OrderResponse> list = orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
        return new OrderListResponse(list);
    }
}
