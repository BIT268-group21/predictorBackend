package com.stock_predictor.ingestion.fmp;

import java.math.BigDecimal;

public record FmpPriceRecord(
		String date,
		BigDecimal open,
		BigDecimal high,
		BigDecimal low,
		BigDecimal close,
		Long volume) {
}
