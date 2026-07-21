package com.stock_predictor.predictions.dto;

import java.time.LocalDate;
import java.util.List;

public record AccuracyHistoryItem(
		LocalDate date,
		String predicted,
		String actual,
		boolean wasCorrect) {
}
