package com.stock_predictor.predictions.dto;

import java.util.List;

public record AccuracyResponse(
		String ticker,
		long totalPredictions,
		long correct,
		double accuracyPercent,
		List<AccuracyHistoryItem> history) {
}
