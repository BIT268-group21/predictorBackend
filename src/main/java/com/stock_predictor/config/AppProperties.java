package com.stock_predictor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
		FmpProperties fmp,
		MlProperties ml,
		IngestionProperties ingestion,
		PredictionJobProperties prediction,
		AccuracyJobProperties accuracy,
		CorsProperties cors) {

	public record FmpProperties(String apiKey, String baseUrl) {
	}

	public record MlProperties(String baseUrl) {
	}

	public record IngestionProperties(int priceLookbackDays, String cron) {
	}

	public record PredictionJobProperties(String cron) {
	}

	public record AccuracyJobProperties(String cron) {
	}

	public record CorsProperties(String allowedOrigins) {
	}
}
