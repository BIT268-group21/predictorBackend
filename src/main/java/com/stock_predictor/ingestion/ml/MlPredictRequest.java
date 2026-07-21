package com.stock_predictor.ingestion.ml;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MlPredictRequest(String ticker, List<MlPricePoint> prices) {

	public record MlPricePoint(
			LocalDate date,
			BigDecimal open,
			BigDecimal high,
			BigDecimal low,
			BigDecimal close,
			Long volume) {
	}
}
