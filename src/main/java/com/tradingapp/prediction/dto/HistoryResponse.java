package com.tradingapp.prediction.dto;

import java.util.List;

/** Mirrors the predictor's GET /history/{ticker} response exactly. */
public record HistoryResponse(String ticker, int rows, List<HistoryBar> ohlcv) {
}
