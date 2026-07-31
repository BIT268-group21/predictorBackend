package com.stock_predictor.predictions.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stock_predictor.predictions.entity.Prediction;
import com.stock_predictor.predictions.entity.PredictionFeature;
import com.stock_predictor.predictions.repository.PredictionFeatureRepository;
import com.stock_predictor.predictions.repository.PredictionRepository;
import com.stock_predictor.stocks.entity.Stock;
import com.stock_predictor.stocks.entity.StockPrice;
import com.stock_predictor.stocks.repository.StockPriceRepository;
import com.stock_predictor.stocks.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PredictionControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StockRepository stockRepository;

	@Autowired
	private StockPriceRepository stockPriceRepository;

	@Autowired
	private PredictionRepository predictionRepository;

	@Autowired
	private PredictionFeatureRepository predictionFeatureRepository;

	@BeforeEach
	void setUp() {
		predictionFeatureRepository.deleteAll();
		predictionRepository.deleteAll();
		stockPriceRepository.deleteAll();
		stockRepository.deleteAll();

		stockRepository.save(new Stock("AAPL", "Apple Inc.", "Technology"));

		LocalDate today = LocalDate.now();
		for (int i = 30; i >= 0; i--) {
			LocalDate date = today.minusDays(i);
			stockPriceRepository.save(new StockPrice(
					"AAPL",
					date,
					new BigDecimal("100"),
					new BigDecimal("101"),
					new BigDecimal("99"),
					new BigDecimal("100.50"),
					1_000_000L));
		}

		Prediction prediction = predictionRepository.save(new Prediction(
				"AAPL",
				"up",
				new BigDecimal("0.720"),
				"5-day average crossed above 20-day average",
				today.plusDays(1),
				today,
				new BigDecimal("0.75"),
				new BigDecimal("330.0")));
		predictionFeatureRepository.save(
				new PredictionFeature(prediction, "moving_avg_short", new BigDecimal("331.4")));
		predictionFeatureRepository.save(
				new PredictionFeature(prediction, "moving_avg_long", new BigDecimal("328.1")));
	}

	@Test
	void topPredictionsReturnsSeededPrediction() throws Exception {
		mockMvc.perform(get("/api/predictions/top").param("limit", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].ticker").value("AAPL"))
				.andExpect(jsonPath("$[0].companyName").value("Apple Inc."))
				.andExpect(jsonPath("$[0].predictedTrend").value("up"))
				.andExpect(jsonPath("$[0].confidence").value(0.72));
	}

	@Test
	void stockPredictionReturnsLatestStoredPrediction() throws Exception {
		mockMvc.perform(get("/api/stocks/AAPL/prediction"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ticker").value("AAPL"))
				.andExpect(jsonPath("$.trend").value("up"))
				.andExpect(jsonPath("$.indicators.moving_avg_short").value(331.4));
	}

	@Test
	void stockProfileReturnsCompanyData() throws Exception {
		mockMvc.perform(get("/api/stocks/AAPL"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ticker").value("AAPL"))
				.andExpect(jsonPath("$.companyName").value("Apple Inc."));
	}
}
