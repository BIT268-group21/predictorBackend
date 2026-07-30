package com.tradingapp.prediction.dto;

import java.util.List;

/** Mirrors the predictor's POST /predict response exactly (snake_case). */
@SuppressWarnings("checkstyle:ParameterName")
public record PredictResponse(String request_id, String ticker, String status, int horizon_days,
                              String trend_classification, double confidence_score,
                              List<String> detected_patterns, String timestamp, String detail) {
}
