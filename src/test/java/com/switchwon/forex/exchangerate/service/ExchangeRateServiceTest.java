package com.switchwon.forex.exchangerate.service;

import com.switchwon.forex.common.exception.BusinessException;
import com.switchwon.forex.common.exception.ErrorCode;
import com.switchwon.forex.exchangerate.dto.ExchangeRateDto;
import com.switchwon.forex.exchangerate.dto.ExchangeRateListResponse;
import com.switchwon.forex.exchangerate.entity.ExchangeRateHistory;
import com.switchwon.forex.exchangerate.repository.ExchangeRateHistoryRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateService 비즈니스 로직 검증")
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateHistoryRepository repository;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private ExchangeRateHistory usdHistory;
    private ExchangeRateHistory jpyHistory;
    private ExchangeRateHistory cnyHistory;
    private ExchangeRateHistory eurHistory;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 22, 10, 1);
        usdHistory = new ExchangeRateHistory("USD", new BigDecimal("1477.45"),
                new BigDecimal("1551.32"), new BigDecimal("1403.58"), now);
        jpyHistory = new ExchangeRateHistory("JPY", new BigDecimal("910.50"),
                new BigDecimal("956.03"), new BigDecimal("864.98"), now);
        cnyHistory = new ExchangeRateHistory("CNY", new BigDecimal("215.52"),
                new BigDecimal("226.30"), new BigDecimal("204.74"), now);
        eurHistory = new ExchangeRateHistory("EUR", new BigDecimal("1724.14"),
                new BigDecimal("1810.35"), new BigDecimal("1637.93"), now);
    }

    @Nested
    @DisplayName("전체 통화 최신 환율 조회")
    class GetLatestAll {

        @Test
        @DisplayName("USD/JPY/CNY/EUR 4개 통화 환율을 모두 반환")
        void getLatestAll() {
            given(repository.findTopByCurrencyOrderByCollectedAtDesc("USD")).willReturn(Optional.of(usdHistory));
            given(repository.findTopByCurrencyOrderByCollectedAtDesc("JPY")).willReturn(Optional.of(jpyHistory));
            given(repository.findTopByCurrencyOrderByCollectedAtDesc("CNY")).willReturn(Optional.of(cnyHistory));
            given(repository.findTopByCurrencyOrderByCollectedAtDesc("EUR")).willReturn(Optional.of(eurHistory));

            ExchangeRateListResponse response = exchangeRateService.getLatestAll();

            assertThat(response.exchangeRateList()).hasSize(4);
            assertThat(response.exchangeRateList())
                    .extracting(ExchangeRateDto::currency)
                    .containsExactly("USD", "JPY", "CNY", "EUR");
        }

        @Test
        @DisplayName("한 통화라도 데이터 없으면 EXCHANGE_RATE_NOT_FOUND 예외 발생")
        void getLatestAllWithMissingCurrency() {
            given(repository.findTopByCurrencyOrderByCollectedAtDesc("USD")).willReturn(Optional.of(usdHistory));
            given(repository.findTopByCurrencyOrderByCollectedAtDesc("JPY")).willReturn(Optional.empty());

            assertThatThrownBy(() -> exchangeRateService.getLatestAll())
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.EXCHANGE_RATE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("특정 통화 최신 환율 조회")
    class GetLatestByCurrency {

        @Test
        @DisplayName("USD 환율 조회 시 올바른 buyRate/sellRate/tradeStanRate 반환")
        void getLatestUsd() {
            given(repository.findTopByCurrencyOrderByCollectedAtDesc("USD")).willReturn(Optional.of(usdHistory));

            ExchangeRateDto dto = exchangeRateService.getLatestByCurrency("USD");

            assertThat(dto.currency()).isEqualTo("USD");
            assertThat(dto.tradeStanRate()).isEqualByComparingTo(new BigDecimal("1477.45"));
            assertThat(dto.buyRate()).isEqualByComparingTo(new BigDecimal("1551.32"));
            assertThat(dto.sellRate()).isEqualByComparingTo(new BigDecimal("1403.58"));
        }

        @Test
        @DisplayName("소문자 통화 코드도 대문자로 정규화하여 조회")
        void getLatestWithLowerCase() {
            given(repository.findTopByCurrencyOrderByCollectedAtDesc("USD")).willReturn(Optional.of(usdHistory));

            ExchangeRateDto dto = exchangeRateService.getLatestByCurrency("usd");

            assertThat(dto.currency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("지원하지 않는 통화 코드는 INVALID_CURRENCY 예외 발생")
        void getLatestWithUnsupportedCurrency() {
            assertThatThrownBy(() -> exchangeRateService.getLatestByCurrency("GBP"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_CURRENCY));
        }

        @Test
        @DisplayName("지원 통화지만 데이터 없으면 EXCHANGE_RATE_NOT_FOUND 예외 발생")
        void getLatestWithNoData() {
            given(repository.findTopByCurrencyOrderByCollectedAtDesc("JPY")).willReturn(Optional.empty());

            assertThatThrownBy(() -> exchangeRateService.getLatestByCurrency("JPY"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.EXCHANGE_RATE_NOT_FOUND));
        }
    }
}
