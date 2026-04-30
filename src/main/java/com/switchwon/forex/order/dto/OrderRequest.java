package com.switchwon.forex.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderRequest(
        @NotNull(message = "forexAmount는 필수입니다.")
        @DecimalMin(value = "0.01", message = "forexAmount는 0보다 커야 합니다.")
        BigDecimal forexAmount,

        @NotBlank(message = "fromCurrency는 필수입니다.")
        String fromCurrency,

        @NotBlank(message = "toCurrency는 필수입니다.")
        String toCurrency
) {}
