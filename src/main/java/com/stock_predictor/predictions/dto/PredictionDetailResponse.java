package com.stock_predictor.predictions.dto;

import java.math.BigDecimal;
import java.util.Map;

public record PredictionDetailResponse(
		String ticker,
		String trend,
		BigDecimal confidence,
		String reasoning,
		Map<String, BigDecimal> indicators) {
}
