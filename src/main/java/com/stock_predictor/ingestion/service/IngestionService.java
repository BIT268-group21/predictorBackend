package com.stock_predictor.ingestion.service;

import com.stock_predictor.ingestion.twelvedata.TwelveDataClient;
import com.stock_predictor.ingestion.twelvedata.TwelveDataPriceRecord;
import com.stock_predictor.predictions.entity.Prediction;
import com.stock_predictor.predictions.repository.PredictionRepository;
import com.stock_predictor.stocks.entity.Stock;
import com.stock_predictor.stocks.entity.StockPrice;
import com.stock_predictor.stocks.repository.StockPriceRepository;
import com.stock_predictor.stocks.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngestionService {

	private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
	private static final BigDecimal FLAT_THRESHOLD = new BigDecimal("0.001");

	// Twelve Data's free tier allows 8 requests/minute; the ML pipeline's own
	// pull_and_check.py uses this same 8-second interval successfully.
	private static final long REQUEST_DELAY_MS = 8_000;

	private final StockRepository stockRepository;
	private final StockPriceRepository stockPriceRepository;
	private final PredictionRepository predictionRepository;
	private final TwelveDataClient twelveDataClient;

	public IngestionService(
			StockRepository stockRepository,
			StockPriceRepository stockPriceRepository,
			PredictionRepository predictionRepository,
			TwelveDataClient twelveDataClient) {
		this.stockRepository = stockRepository;
		this.stockPriceRepository = stockPriceRepository;
		this.predictionRepository = predictionRepository;
		this.twelveDataClient = twelveDataClient;
	}

	@Transactional
	public void ingestPricesForAllStocks() {
		List<Stock> stocks = stockRepository.findAll();
		for (int i = 0; i < stocks.size(); i++) {
			ingestPricesForTicker(stocks.get(i).getTicker());
			if (i < stocks.size() - 1) {
				sleepBetweenRequests();
			}
		}
	}

	// Package-private (not private) so tests can spy on it instead of eating the
	// real 8-second delay per ticker.
	void sleepBetweenRequests() {
		try {
			Thread.sleep(REQUEST_DELAY_MS);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.warn("Interrupted while waiting between Twelve Data requests");
		}
	}

	@Transactional
	public int ingestPricesForTicker(String ticker) {
		List<TwelveDataPriceRecord> records = twelveDataClient.fetchHistoricalPrices(ticker);
		int inserted = 0;

		for (TwelveDataPriceRecord record : records) {
			LocalDate priceDate = parseDate(record.date());
			if (priceDate == null) {
				continue;
			}
			if (stockPriceRepository.existsByTickerAndPriceDate(ticker, priceDate)) {
				continue;
			}
			stockPriceRepository.save(new StockPrice(
					ticker,
					priceDate,
					record.open(),
					record.high(),
					record.low(),
					record.close(),
					record.volume()));
			inserted++;
		}

		log.info("Ingested {} new price rows for {}", inserted, ticker);
		return inserted;
	}

	@Transactional
	public void evaluatePendingPredictions() {
		List<Prediction> pending = predictionRepository.findPendingAccuracyChecks(LocalDate.now());
		for (Prediction prediction : pending) {
			evaluatePrediction(prediction);
		}
	}

	private void evaluatePrediction(Prediction prediction) {
		LocalDate targetDate = prediction.getPredictedForDate();
		var targetPriceOpt = stockPriceRepository.findByTickerAndPriceDate(prediction.getTicker(), targetDate);
		// Most recent trading day strictly before the target (the ML pipeline's naive
		// next-trading-day can land on a Monday, where minusDays(1) would be a Sunday).
		var priorPriceOpt = stockPriceRepository.findTopByTickerAndPriceDateLessThanOrderByPriceDateDesc(
				prediction.getTicker(), targetDate);

		if (targetPriceOpt.isEmpty() || priorPriceOpt.isEmpty()) {
			return;
		}

		String actualTrend = classifyTrend(
				priorPriceOpt.get().getClose(),
				targetPriceOpt.get().getClose());

		prediction.setActualTrend(actualTrend);
		predictionRepository.save(prediction);

		log.info(
				"Scored prediction for {} on {}: predicted={}, actual={}, correct={}",
				prediction.getTicker(),
				targetDate,
				prediction.getPredictedTrend(),
				actualTrend,
				prediction.getWasCorrect());
	}

	static String classifyTrend(BigDecimal priorClose, BigDecimal currentClose) {
		BigDecimal change = currentClose.subtract(priorClose)
				.divide(priorClose, 6, java.math.RoundingMode.HALF_UP);

		if (change.abs().compareTo(FLAT_THRESHOLD) <= 0) {
			return "flat";
		}
		return change.signum() > 0 ? "up" : "down";
	}

	private LocalDate parseDate(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(value);
		} catch (DateTimeParseException ex) {
			log.warn("Unable to parse Twelve Data date: {}", value);
			return null;
		}
	}
}
