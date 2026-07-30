package com.tradingapp.prediction.dto;

import java.util.List;

/** Mirrors the predictor's POST /predict body exactly (snake_case). */
@SuppressWarnings("checkstyle:ParameterName")
public record PredictRequest(String request_id, String ticker, List<OhlcvBar> ohlcv) {
}
