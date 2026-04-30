package com.switchwon.forex.exchangerate.controller;

import com.switchwon.forex.common.response.ApiResponse;
import com.switchwon.forex.exchangerate.dto.ExchangeRateDto;
import com.switchwon.forex.exchangerate.dto.ExchangeRateListResponse;
import com.switchwon.forex.exchangerate.service.ExchangeRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exchange-rate")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<ExchangeRateListResponse>> getLatestAll() {
        return ResponseEntity.ok(ApiResponse.ok(exchangeRateService.getLatestAll()));
    }

    @GetMapping("/latest/{currency}")
    public ResponseEntity<ApiResponse<ExchangeRateDto>> getLatestByCurrency(
            @PathVariable String currency) {
        return ResponseEntity.ok(ApiResponse.ok(exchangeRateService.getLatestByCurrency(currency)));
    }
}
