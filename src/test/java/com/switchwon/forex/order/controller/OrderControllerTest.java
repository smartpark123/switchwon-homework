package com.switchwon.forex.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.switchwon.forex.common.exception.BusinessException;
import com.switchwon.forex.common.exception.ErrorCode;
import com.switchwon.forex.order.dto.OrderListResponse;
import com.switchwon.forex.order.dto.OrderRequest;
import com.switchwon.forex.order.dto.OrderResponse;
import com.switchwon.forex.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController API 검증")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("POST /order - KRW → USD 매수 주문 성공")
    void createBuyOrder() throws Exception {
        OrderRequest request = new OrderRequest(new BigDecimal("200"), "KRW", "USD");
        OrderResponse response = new OrderResponse(
                1L,
                new BigDecimal("310264"),
                "KRW",
                new BigDecimal("200"),
                "USD",
                new BigDecimal("1551.32"),
                LocalDateTime.of(2026, 4, 22, 10, 1)
        );

        given(orderService.createOrder(any(OrderRequest.class))).willReturn(response);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.returnObject.fromCurrency").value("KRW"))
                .andExpect(jsonPath("$.returnObject.toCurrency").value("USD"))
                .andExpect(jsonPath("$.returnObject.fromAmount").value(310264))
                .andExpect(jsonPath("$.returnObject.toAmount").value(200))
                .andExpect(jsonPath("$.returnObject.tradeRate").value(1551.32));
    }

    @Test
    @DisplayName("POST /order - USD → KRW 매도 주문 성공")
    void createSellOrder() throws Exception {
        OrderRequest request = new OrderRequest(new BigDecimal("133"), "USD", "KRW");
        OrderResponse response = new OrderResponse(
                2L,
                new BigDecimal("133"),
                "USD",
                new BigDecimal("186676"),
                "KRW",
                new BigDecimal("1403.58"),
                LocalDateTime.of(2026, 4, 22, 10, 1)
        );

        given(orderService.createOrder(any(OrderRequest.class))).willReturn(response);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.fromCurrency").value("USD"))
                .andExpect(jsonPath("$.returnObject.toCurrency").value("KRW"))
                .andExpect(jsonPath("$.returnObject.toAmount").value(186676));
    }

    @Test
    @DisplayName("POST /order - forexAmount 누락 시 400 반환")
    void createOrderWithMissingAmount() throws Exception {
        String invalidBody = """
                {
                    "fromCurrency": "KRW",
                    "toCurrency": "USD"
                }
                """;

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /order - 잘못된 통화 조합은 400 반환")
    void createOrderWithInvalidCurrencyPair() throws Exception {
        OrderRequest request = new OrderRequest(new BigDecimal("100"), "USD", "JPY");

        given(orderService.createOrder(any(OrderRequest.class)))
                .willThrow(new BusinessException(ErrorCode.INVALID_ORDER_REQUEST));

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_REQUEST"));
    }

    @Test
    @DisplayName("GET /order/list - 주문 목록 반환")
    void getOrderList() throws Exception {
        OrderResponse order1 = new OrderResponse(1L, new BigDecimal("310264"), "KRW",
                new BigDecimal("200"), "USD", new BigDecimal("1551.32"), LocalDateTime.now());
        OrderResponse order2 = new OrderResponse(2L, new BigDecimal("133"), "USD",
                new BigDecimal("186676"), "KRW", new BigDecimal("1403.58"), LocalDateTime.now());

        given(orderService.getOrderList()).willReturn(new OrderListResponse(List.of(order1, order2)));

        mockMvc.perform(get("/order/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.returnObject.orderList").isArray())
                .andExpect(jsonPath("$.returnObject.orderList.length()").value(2))
                .andExpect(jsonPath("$.returnObject.orderList[0].id").value(1))
                .andExpect(jsonPath("$.returnObject.orderList[1].id").value(2));
    }

    @Test
    @DisplayName("GET /order/list - 주문 없으면 빈 배열 반환")
    void getOrderListEmpty() throws Exception {
        given(orderService.getOrderList()).willReturn(new OrderListResponse(List.of()));

        mockMvc.perform(get("/order/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnObject.orderList").isEmpty());
    }
}
