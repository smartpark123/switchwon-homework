package com.switchwon.forex.exchangerate.service;

import com.switchwon.forex.common.exception.BusinessException;
import com.switchwon.forex.common.exception.ErrorCode;
import com.switchwon.forex.exchangerate.dto.ExchangeRateDto;
import com.switchwon.forex.exchangerate.dto.ExchangeRateListResponse;
import com.switchwon.forex.exchangerate.entity.ExchangeRateHistory;
import com.switchwon.forex.exchangerate.repository.ExchangeRateHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ExchangeRateService {

    private static final List<String> SUPPORTED_CURRENCIES = Arrays.asList("USD", "JPY", "CNY", "EUR");

    private final ExchangeRateHistoryRepository repository;

    public ExchangeRateService(ExchangeRateHistoryRepository repository) {
        this.repository = repository;
    }

    public ExchangeRateListResponse getLatestAll() {
        List<ExchangeRateDto> rates = SUPPORTED_CURRENCIES.stream()
                .map(currency -> repository.findTopByCurrencyOrderByCollectedAtDesc(currency)
                        .map(ExchangeRateDto::from)
                        .orElseThrow(() -> new BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUND)))
                .toList();
        return new ExchangeRateListResponse(rates);
    }

    public ExchangeRateDto getLatestByCurrency(String currency) {
        String upper = currency.toUpperCase();
        if (!SUPPORTED_CURRENCIES.contains(upper)) {
            throw new BusinessException(ErrorCode.INVALID_CURRENCY);
        }
        ExchangeRateHistory history = repository.findTopByCurrencyOrderByCollectedAtDesc(upper)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUND));
        return ExchangeRateDto.from(history);
    }

    @Transactional
    public void saveAll(List<ExchangeRateHistory> histories) {
        repository.saveAll(histories);
    }
}
