package com.stock_predictor.predictions.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record TopPredictionResponse(
		String ticker,
		String companyName,
		String predictedTrend,
		BigDecimal confidence,
		LocalDate predictedForDate) {
}
