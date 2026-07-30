package com.tradingapp.prediction.dto;

/** One row of the predictor's GET /history/{ticker} payload. */
public record HistoryBar(String date, double open, double high, double low, double close, double volume) {
}
