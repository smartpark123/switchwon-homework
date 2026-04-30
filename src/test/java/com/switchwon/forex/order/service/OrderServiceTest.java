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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 비즈니스 로직 검증")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ExchangeRateHistoryRepository exchangeRateHistoryRepository;

    @InjectMocks
    private OrderService orderService;

    private ExchangeRateHistory usdRate;

    @BeforeEach
    void setUp() {
        usdRate = new ExchangeRateHistory(
                "USD",
                new BigDecimal("1477.45"),
                new BigDecimal("1551.32"),  // buyRate  = 1477.45 × 1.05
                new BigDecimal("1403.58"),  // sellRate = 1477.45 × 0.95
                LocalDateTime.of(2026, 4, 22, 10, 1)
        );
    }

    @Nested
    @DisplayName("매수 주문 (KRW → 외화)")
    class BuyOrder {

        @Test
        @DisplayName("KRW → USD 200달러 매수 시 buyRate 적용, KRW 금액 floor 처리")
        void buyUsd() {
            OrderRequest request = new OrderRequest(new BigDecimal("200"), "KRW", "USD");

            given(exchangeRateHistoryRepository.findTopByCurrencyOrderByCollectedAtDesc("USD"))
                    .willReturn(Optional.of(usdRate));
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            OrderResponse response = orderService.createOrder(request);

            // 200 × 1551.32 = 310264.00 → floor = 310264
            assertThat(response.fromCurrency()).isEqualTo("KRW");
            assertThat(response.toCurrency()).isEqualTo("USD");
            assertThat(response.toAmount()).isEqualByComparingTo(new BigDecimal("200"));
            assertThat(response.fromAmount()).isEqualByComparingTo(new BigDecimal("310264"));
            assertThat(response.tradeRate()).isEqualByComparingTo(new BigDecimal("1551.32"));
        }

        @Test
        @DisplayName("소문자 통화 코드도 대문자로 정규화하여 처리")
        void buyWithLowerCaseCurrency() {
            OrderRequest request = new OrderRequest(new BigDecimal("100"), "krw", "usd");

            given(exchangeRateHistoryRepository.findTopByCurrencyOrderByCollectedAtDesc("USD"))
                    .willReturn(Optional.of(usdRate));
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            OrderResponse response = orderService.createOrder(request);

            assertThat(response.fromCurrency()).isEqualTo("KRW");
            assertThat(response.toCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("환율 정보 없으면 EXCHANGE_RATE_NOT_FOUND 예외 발생")
        void buyWithNoExchangeRate() {
            OrderRequest request = new OrderRequest(new BigDecimal("200"), "KRW", "USD");

            given(exchangeRateHistoryRepository.findTopByCurrencyOrderByCollectedAtDesc("USD"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.EXCHANGE_RATE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("매도 주문 (외화 → KRW)")
    class SellOrder {

        @Test
        @DisplayName("USD 133달러 → KRW 매도 시 sellRate 적용, KRW 금액 floor 처리")
        void sellUsd() {
            OrderRequest request = new OrderRequest(new BigDecimal("133"), "USD", "KRW");

            given(exchangeRateHistoryRepository.findTopByCurrencyOrderByCollectedAtDesc("USD"))
                    .willReturn(Optional.of(usdRate));
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            OrderResponse response = orderService.createOrder(request);

            // 133 × 1403.58 = 186676.14 → floor = 186676
            assertThat(response.fromCurrency()).isEqualTo("USD");
            assertThat(response.toCurrency()).isEqualTo("KRW");
            assertThat(response.fromAmount()).isEqualByComparingTo(new BigDecimal("133"));
            assertThat(response.toAmount()).isEqualByComparingTo(new BigDecimal("186676"));
            assertThat(response.tradeRate()).isEqualByComparingTo(new BigDecimal("1403.58"));
        }
    }

    @Nested
    @DisplayName("잘못된 주문 요청")
    class InvalidOrder {

        @Test
        @DisplayName("KRW → KRW 주문은 INVALID_ORDER_REQUEST 예외 발생")
        void krwToKrw() {
            OrderRequest request = new OrderRequest(new BigDecimal("100000"), "KRW", "KRW");

            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_ORDER_REQUEST));
        }

        @Test
        @DisplayName("USD → JPY 이중 통화 주문은 INVALID_ORDER_REQUEST 예외 발생")
        void crossCurrencyOrder() {
            OrderRequest request = new OrderRequest(new BigDecimal("100"), "USD", "JPY");

            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_ORDER_REQUEST));
        }
    }

    @Nested
    @DisplayName("주문 내역 조회")
    class OrderList {

        @Test
        @DisplayName("전체 주문 내역을 OrderListResponse로 반환")
        void getOrderList() {
            Order order1 = new Order("KRW", "USD", new BigDecimal("200"),
                    new BigDecimal("310264"), new BigDecimal("200"),
                    new BigDecimal("1551.32"), LocalDateTime.now());
            Order order2 = new Order("USD", "KRW", new BigDecimal("133"),
                    new BigDecimal("133"), new BigDecimal("186676"),
                    new BigDecimal("1403.58"), LocalDateTime.now());

            given(orderRepository.findAll()).willReturn(List.of(order1, order2));

            OrderListResponse response = orderService.getOrderList();

            assertThat(response.orderList()).hasSize(2);
            assertThat(response.orderList().get(0).fromCurrency()).isEqualTo("KRW");
            assertThat(response.orderList().get(1).fromCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("주문이 없으면 빈 리스트 반환")
        void getOrderListEmpty() {
            given(orderRepository.findAll()).willReturn(List.of());

            OrderListResponse response = orderService.getOrderList();

            assertThat(response.orderList()).isEmpty();
        }
    }
}
