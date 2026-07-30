package com.tradingapp.alert.dto;

import com.tradingapp.alert.AlertDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record AlertRequest(
        @NotBlank String ticker,
        @NotNull @Positive BigDecimal targetPrice,
        @NotNull AlertDirection direction) {
}
