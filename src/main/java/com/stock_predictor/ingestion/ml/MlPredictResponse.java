package com.stock_predictor.ingestion.ml;

import java.math.BigDecimal;
import java.util.Map;

public record MlPredictResponse(
		String ticker,
		String trend,
		BigDecimal confidence,
		String reasoning,
		Map<String, BigDecimal> indicators) {
}
