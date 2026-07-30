package com.stock_predictor.predictions.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stock_predictor.predictions.repository.PredictionRepository;
import com.stock_predictor.stocks.repository.StockPriceRepository;
import com.stock_predictor.stocks.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs against a real local Postgres instance (see application-postgres-test.yml,
 * connection details read from backend-repo/.env). Validates that the
 * POST /api/predictions/batch endpoint honors the Section 3a data contract and
 * writes correctly to the predictions table — not a cross-machine test against the
 * ML teammate's actual batch job.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgres-test")
class PredictionBatchIntegrationTest {

	// Locked 50-ticker list, decisions.md Section 4.
	private static final List<String> LOCKED_TICKERS = List.of(
			"AAPL", "MSFT", "GOOGL", "AMZN", "META", "NVDA", "TSLA", "BRK.B", "JPM", "V",
			"JNJ", "WMT", "PG", "MA", "UNH", "HD", "DIS", "BAC", "XOM", "CVX",
			"KO", "PEP", "ABBV", "COST", "MRK", "ADBE", "CRM", "NFLX", "INTC", "AMD",
			"CSCO", "PFE", "TMO", "ABT", "NKE", "MCD", "ORCL", "IBM", "QCOM", "TXN",
			"HON", "UPS", "CAT", "GS", "MS", "BA", "GE", "LMT", "SBUX", "LOW");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private PredictionRepository predictionRepository;

	@Autowired
	private StockPriceRepository stockPriceRepository;

	@Autowired
	private StockRepository stockRepository;

	@BeforeEach
	void cleanSlate() {
		predictionRepository.deleteAll();
		stockPriceRepository.deleteAll();
		stockRepository.deleteAll();
	}

	@AfterEach
	void cleanUp() {
		predictionRepository.deleteAll();
		stockPriceRepository.deleteAll();
		stockRepository.deleteAll();
	}

	@Test
	void validBatchOfFiftyPredictionsIsWrittenToPredictionsTable() throws Exception {
		LocalDate predictionDate = LocalDate.of(2026, 7, 20);
		LocalDate targetDate = LocalDate.of(2026, 7, 21);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("predictions", LOCKED_TICKERS.stream()
				.map(ticker -> validPredictionItem(ticker, predictionDate, targetDate))
				.toList());
		String body = jsonMapper.writeValueAsString(payload);

		mockMvc.perform(post("/api/predictions/batch")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.saved").value(50));

		assertPredictionsPersisted(predictionDate, targetDate);
	}

	private void assertPredictionsPersisted(LocalDate predictionDate, LocalDate targetDate) {
		var stored = predictionRepository.findAll();
		org.junit.jupiter.api.Assertions.assertEquals(50, stored.size());

		var aapl = stored.stream()
				.filter(p -> p.getTicker().equals("AAPL"))
				.findFirst()
				.orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals("up", aapl.getPredictedTrend());
		org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("0.63").compareTo(aapl.getConfidence()));
		org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("0.72").compareTo(aapl.getModelAccuracy()));
		org.junit.jupiter.api.Assertions.assertEquals(
				0, new BigDecimal("171.85").compareTo(aapl.getLastClosePrice()));
		org.junit.jupiter.api.Assertions.assertEquals(predictionDate, aapl.getPredictionDate());
		org.junit.jupiter.api.Assertions.assertEquals(targetDate, aapl.getPredictedForDate());
		org.junit.jupiter.api.Assertions.assertTrue(aapl.getIndicatorsJson().contains("moving_avg_short"));
	}

	@Test
	void missingRequiredFieldIsRejectedWithClearError() throws Exception {
		String body = """
				{
				  "predictions": [
				    {
				      "prediction_date": "2026-07-20",
				      "target_date": "2026-07-21",
				      "predicted_direction": "up",
				      "confidence": 0.63,
				      "model_accuracy": 0.72,
				      "features": {"moving_avg_short": 172.3},
				      "last_close_price": 171.85
				    }
				  ]
				}
				""";

		mockMvc.perform(post("/api/predictions/batch")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").exists())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("ticker")));

		org.junit.jupiter.api.Assertions.assertEquals(0, predictionRepository.count());
	}

	@Test
	void wrongTypeFieldIsRejectedWithClearError() throws Exception {
		String body = """
				{
				  "predictions": [
				    {
				      "ticker": "AAPL",
				      "prediction_date": "2026-07-20",
				      "target_date": "2026-07-21",
				      "predicted_direction": "up",
				      "confidence": "very confident",
				      "model_accuracy": 0.72,
				      "features": {"moving_avg_short": 172.3},
				      "last_close_price": 171.85
				    }
				  ]
				}
				""";

		mockMvc.perform(post("/api/predictions/batch")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").exists());

		org.junit.jupiter.api.Assertions.assertEquals(0, predictionRepository.count());
	}

	@Test
	void invalidDirectionIsRejectedWithClearError() throws Exception {
		Map<String, Object> item = validPredictionItem(
				"AAPL", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 21));
		item.put("predicted_direction", "sideways");
		String body = jsonMapper.writeValueAsString(Map.of("predictions", List.of(item)));

		mockMvc.perform(post("/api/predictions/batch")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").exists());

		org.junit.jupiter.api.Assertions.assertEquals(0, predictionRepository.count());
	}

	@Test
	void emptyPredictionsArrayIsRejected() throws Exception {
		String body = """
				{ "predictions": [] }
				""";

		mockMvc.perform(post("/api/predictions/batch")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());
	}

	private Map<String, Object> validPredictionItem(String ticker, LocalDate predictionDate, LocalDate targetDate) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("ticker", ticker);
		item.put("prediction_date", predictionDate.toString());
		item.put("target_date", targetDate.toString());
		item.put("predicted_direction", "up");
		item.put("confidence", 0.63);
		item.put("model_accuracy", 0.72);
		item.put("features", Map.of(
				"moving_avg_short", 172.3,
				"moving_avg_long", 168.9,
				"volatility_20d", 0.021,
				"momentum_5d", 0.015));
		item.put("last_close_price", 171.85);
		return item;
	}
}
