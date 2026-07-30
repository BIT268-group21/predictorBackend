package com.tradingapp.prediction.dto;

/** One bar as the predictor expects it — snake_case is irrelevant here, all fields are single words. */
public record OhlcvBar(double open, double high, double low, double close, double volume) {
}
