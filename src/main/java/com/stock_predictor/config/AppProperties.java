package com.stock_predictor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
		FmpProperties fmp,
		IngestionProperties ingestion,
		AccuracyJobProperties accuracy,
		CorsProperties cors) {

	public record FmpProperties(String apiKey, String baseUrl) {
	}

	public record IngestionProperties(int priceLookbackDays, String cron) {
	}

	public record AccuracyJobProperties(String cron) {
	}

	public record CorsProperties(String allowedOrigins) {
	}
}
