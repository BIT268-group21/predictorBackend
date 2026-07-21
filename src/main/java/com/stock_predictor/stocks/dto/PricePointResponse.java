package com.stock_predictor.stocks.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PricePointResponse(
		LocalDate date,
		BigDecimal open,
		BigDecimal high,
		BigDecimal low,
		BigDecimal close,
		Long volume) {
}
