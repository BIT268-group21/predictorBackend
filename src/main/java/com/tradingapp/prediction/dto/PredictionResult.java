package com.tradingapp.prediction.dto;

import java.time.Instant;
import java.util.List;

/**
 * Frontend-facing prediction payload. {@code confidence} is the model's
 * uncalibrated probability for the predicted class and is passed through as-is.
 */
public record PredictionResult(String ticker, String trend, double confidence,
                               int horizonDays, List<String> detectedPatterns, Instant timestamp) {
}
