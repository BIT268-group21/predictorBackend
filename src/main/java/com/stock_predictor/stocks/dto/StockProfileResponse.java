package com.stock_predictor.stocks.dto;

public record StockProfileResponse(
		String ticker,
		String companyName,
		String sector) {
}
