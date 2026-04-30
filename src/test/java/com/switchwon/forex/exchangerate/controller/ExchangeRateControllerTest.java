package com.switchwon.forex.exchangerate.controller;

import com.switchwon.forex.common.exception.BusinessException;
import com.switchwon.forex.common.exception.ErrorCode;
import com.switchwon.forex.exchangerate.dto.ExchangeRateDto;
import com.switchwon.forex.exchangerate.dto.ExchangeRateListResponse;
import com.switchwon.forex.exchangerate.service.ExchangeRateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExchangeRateController.class)
@DisplayName("ExchangeRateController API 검증")
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService exchangeRateService;

    private final ExchangeRateDto usdDto = new ExchangeRateDto(
            "USD",
            new BigDecimal("1551.32"),
            new BigDecimal("1477.45"),
            new BigDecimal("1403.58"),
            LocalDateTime.of(2026, 4, 22, 10, 1)
    );

    @Test
    @DisplayName("GET /exchange-rate/latest - 4개 통화 환율 목록 반환")
    void getLatestAll() throws Exception {
        given(exchangeRateService.getLatestAll())
                .willReturn(new ExchangeRateListResponse(List.of(usdDto)));

        mockMvc.perform(get("/exchange-rate/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.returnObject.exchangeRateList[0].currency").value("USD"))
                .andExpect(jsonPath("$.returnObject.exchangeRateList[0].buyRate").value(1551.32))
                .andExpect(jsonPath("$.returnObject.exchangeRateList[0].tradeStanRate").value(1477.45))
                .andExpect(jsonPath("$.returnObject.exchangeRateList[0].sellRate").value(1403.58));
    }

    @Test
    @DisplayName("GET /exchange-rate/latest/USD - 특정 통화 환율 반환")
    void getLatestByCurrency() throws Exception {
        given(exchangeRateService.getLatestByCurrency("USD")).willReturn(usdDto);

        mockMvc.perform(get("/exchange-rate/latest/USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.returnObject.currency").value("USD"))
                .andExpect(jsonPath("$.returnObject.buyRate").value(1551.32));
    }

    @Test
    @DisplayName("GET /exchange-rate/latest/GBP - 지원하지 않는 통화는 400 반환")
    void getLatestWithInvalidCurrency() throws Exception {
        given(exchangeRateService.getLatestByCurrency("GBP"))
                .willThrow(new BusinessException(ErrorCode.INVALID_CURRENCY));

        mockMvc.perform(get("/exchange-rate/latest/GBP"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENCY"));
    }

    @Test
    @DisplayName("GET /exchange-rate/latest/USD - 환율 데이터 없으면 404 반환")
    void getLatestWithNoData() throws Exception {
        given(exchangeRateService.getLatestByCurrency("USD"))
                .willThrow(new BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUND));

        mockMvc.perform(get("/exchange-rate/latest/USD"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXCHANGE_RATE_NOT_FOUND"));
    }
}
