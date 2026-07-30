package com.stock_predictor.ingestion.service;

import com.stock_predictor.config.AppProperties;
import com.stock_predictor.ingestion.fmp.FmpClient;
import com.stock_predictor.ingestion.fmp.FmpPriceRecord;
import com.stock_predictor.ingestion.ml.MlPredictRequest;
import com.stock_predictor.ingestion.ml.MlPredictResponse;
import com.stock_predictor.ingestion.ml.MlServiceClient;
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

	private final StockRepository stockRepository;
	private final StockPriceRepository stockPriceRepository;
	private final PredictionRepository predictionRepository;
	private final FmpClient fmpClient;
	private final MlServiceClient mlServiceClient;
	private final AppProperties appProperties;

	public IngestionService(
			StockRepository stockRepository,
			StockPriceRepository stockPriceRepository,
			PredictionRepository predictionRepository,
			FmpClient fmpClient,
			MlServiceClient mlServiceClient,
			AppProperties appProperties) {
		this.stockRepository = stockRepository;
		this.stockPriceRepository = stockPriceRepository;
		this.predictionRepository = predictionRepository;
		this.fmpClient = fmpClient;
		this.mlServiceClient = mlServiceClient;
		this.appProperties = appProperties;
	}

	@Transactional
	public void ingestPricesForAllStocks() {
		for (Stock stock : stockRepository.findAll()) {
			ingestPricesForTicker(stock.getTicker());
		}
	}

	@Transactional
	public int ingestPricesForTicker(String ticker) {
		List<FmpPriceRecord> records = fmpClient.fetchHistoricalPrices(ticker);
		int inserted = 0;

		for (FmpPriceRecord record : records) {
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
	public void generatePredictionsForAllStocks() {
		int lookbackDays = appProperties.ingestion().priceLookbackDays();
		for (Stock stock : stockRepository.findAll()) {
			generatePredictionForTicker(stock.getTicker(), lookbackDays);
		}
	}

	@Transactional
	public void generatePredictionForTicker(String ticker, int lookbackDays) {
		LocalDate fromDate = LocalDate.now().minusDays(lookbackDays);
		List<StockPrice> prices = stockPriceRepository
				.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(ticker, fromDate);

		if (prices.size() < 5) {
			log.warn("Not enough price history for {} to generate prediction", ticker);
			return;
		}

		LocalDate latestPriceDate = prices.get(prices.size() - 1).getPriceDate();
		LocalDate predictedForDate = latestPriceDate.plusDays(1);

		boolean alreadyExists = predictionRepository
				.findTopByTickerOrderByPredictedForDateDescCreatedAtDesc(ticker)
				.map(existing -> existing.getPredictedForDate().equals(predictedForDate))
				.orElse(false);
		if (alreadyExists) {
			log.debug("Prediction already exists for {} on {}", ticker, predictedForDate);
			return;
		}

		MlPredictRequest request = new MlPredictRequest(
				ticker,
				prices.stream()
						.map(price -> new MlPredictRequest.MlPricePoint(
								price.getPriceDate(),
								price.getOpen(),
								price.getHigh(),
								price.getLow(),
								price.getClose(),
								price.getVolume()))
						.toList());

		MlPredictResponse response = mlServiceClient.predict(request);
		// response.indicators() isn't persisted: this live path's indicator set
		// (e.g. sma5/sma20/rsi14) doesn't match the fixed feature columns the
		// batch contract uses (moving_avg_short/long, volatility_20d, momentum_5d)
		// -- see the architecture note in docs/API_FLOW.md.

		predictionRepository.save(new Prediction(
				ticker,
				response.trend(),
				response.confidence(),
				response.reasoning(),
				predictedForDate));

		log.info("Stored prediction for {} targeting {}", ticker, predictedForDate);
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
		var priorPriceOpt = stockPriceRepository.findByTickerAndPriceDate(
				prediction.getTicker(), targetDate.minusDays(1));

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
			log.warn("Unable to parse FMP date: {}", value);
			return null;
		}
	}
}
