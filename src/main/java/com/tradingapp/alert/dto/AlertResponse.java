package com.tradingapp.alert.dto;

import com.tradingapp.alert.Alert;
import java.math.BigDecimal;
import java.time.Instant;

public record AlertResponse(Long id, String ticker, BigDecimal targetPrice, String direction,
                            boolean triggered, Instant createdAt, Instant triggeredAt) {

    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getTicker(),
                alert.getTargetPrice(),
                alert.getDirection().name(),
                alert.isTriggered(),
                alert.getCreatedAt(),
                alert.getTriggeredAt());
    }
}
