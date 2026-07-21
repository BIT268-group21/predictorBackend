package com.stock_predictor.ingestion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class IngestionServiceTest {

	@Test
	void classifyTrendUp() {
		assertEquals("up", IngestionService.classifyTrend(new BigDecimal("100"), new BigDecimal("105")));
	}

	@Test
	void classifyTrendDown() {
		assertEquals("down", IngestionService.classifyTrend(new BigDecimal("100"), new BigDecimal("95")));
	}

	@Test
	void classifyTrendFlat() {
		assertEquals("flat", IngestionService.classifyTrend(new BigDecimal("100"), new BigDecimal("100.05")));
	}
}
