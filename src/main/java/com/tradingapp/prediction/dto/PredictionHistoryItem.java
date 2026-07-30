package com.tradingapp.prediction.dto;

import com.tradingapp.prediction.PredictionLog;
import java.time.Instant;
import java.util.List;

/** A row of the caller's saved prediction history (GET /api/predictions/history). */
public record PredictionHistoryItem(Long id, String ticker, String trend, double confidence,
                                    int horizonDays, List<String> detectedPatterns, Instant createdAt) {

    public static PredictionHistoryItem from(PredictionLog log) {
        return new PredictionHistoryItem(
                log.getId(),
                log.getTicker(),
                log.getTrendClassification(),
                log.getConfidenceScore(),
                log.getHorizonDays(),
                log.getDetectedPatternsAsList(),
                log.getCreatedAt());
    }
}
