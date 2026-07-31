package com.stock_predictor.predictions.service;

import com.stock_predictor.common.ResourceNotFoundException;
import com.stock_predictor.predictions.dto.AccuracyHistoryItem;
import com.stock_predictor.predictions.dto.AccuracyResponse;
import com.stock_predictor.predictions.dto.BatchPredictionRequest;
import com.stock_predictor.predictions.dto.BatchPredictionResponse;
import com.stock_predictor.predictions.dto.PredictionBatchItem;
import com.stock_predictor.predictions.dto.PredictionDetailResponse;
import com.stock_predictor.predictions.dto.TopPredictionResponse;
import com.stock_predictor.predictions.entity.Prediction;
import com.stock_predictor.predictions.entity.PredictionFeature;
import com.stock_predictor.predictions.repository.PredictionFeatureRepository;
import com.stock_predictor.predictions.repository.PredictionRepository;
import com.stock_predictor.stocks.entity.Stock;
import com.stock_predictor.stocks.repository.StockRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PredictionService {

	private final PredictionRepository predictionRepository;
	private final PredictionFeatureRepository predictionFeatureRepository;
	private final StockRepository stockRepository;

	public PredictionService(
			PredictionRepository predictionRepository,
			PredictionFeatureRepository predictionFeatureRepository,
			StockRepository stockRepository) {
		this.predictionRepository = predictionRepository;
		this.predictionFeatureRepository = predictionFeatureRepository;
		this.stockRepository = stockRepository;
	}

	public List<TopPredictionResponse> getTopPredictions(int limit) {
		int safeLimit = Math.max(1, Math.min(limit, 50));
		Map<String, Stock> stocksByTicker = stockRepository.findAll().stream()
				.collect(Collectors.toMap(Stock::getTicker, stock -> stock));

		return predictionRepository.findLatestPerTicker().stream()
				.limit(safeLimit)
				.map(prediction -> {
					Stock stock = stocksByTicker.get(prediction.getTicker());
					String companyName = stock != null ? stock.getCompanyName() : prediction.getTicker();
					return new TopPredictionResponse(
							prediction.getTicker(),
							companyName,
							prediction.getPredictedTrend(),
							prediction.getConfidence(),
							prediction.getPredictedForDate());
				})
				.toList();
	}

	@Transactional
	public BatchPredictionResponse saveBatch(BatchPredictionRequest request) {
		List<PredictionBatchItem> items = request.predictions();
		List<Prediction> entities = items.stream().map(this::toEntity).toList();
		predictionRepository.saveAll(entities); // populates generated IDs, needed below

		List<PredictionFeature> featureRows = new ArrayList<>();
		for (int i = 0; i < items.size(); i++) {
			Prediction saved = entities.get(i);
			items.get(i).features().forEach(
					(name, value) -> featureRows.add(new PredictionFeature(saved, name, value)));
		}
		predictionFeatureRepository.saveAll(featureRows);

		return new BatchPredictionResponse(entities.size());
	}

	private Prediction toEntity(PredictionBatchItem item) {
		return new Prediction(
				normalizeTicker(item.ticker()),
				item.predictedDirection(),
				item.confidence(),
				null,
				item.targetDate(),
				item.predictionDate(),
				item.modelAccuracy(),
				item.lastClosePrice());
	}

	public PredictionDetailResponse getLatestPrediction(String ticker) {
		String normalizedTicker = normalizeTicker(ticker);
		if (!stockRepository.existsById(normalizedTicker)) {
			throw new ResourceNotFoundException("Stock not found: " + ticker);
		}

		Prediction prediction = predictionRepository
				.findTopByTickerOrderByPredictedForDateDescCreatedAtDesc(normalizedTicker)
				.orElseThrow(() -> new ResourceNotFoundException("No prediction found for: " + ticker));

		return toDetailResponse(prediction);
	}

	public AccuracyResponse getAccuracyHistory(String ticker) {
		String normalizedTicker = normalizeTicker(ticker);
		if (!stockRepository.existsById(normalizedTicker)) {
			throw new ResourceNotFoundException("Stock not found: " + ticker);
		}

		List<Prediction> scored = predictionRepository
				.findByTickerAndActualTrendIsNotNullOrderByPredictedForDateDesc(normalizedTicker);

		long correct = scored.stream().filter(p -> Boolean.TRUE.equals(p.getWasCorrect())).count();
		long total = scored.size();
		double accuracyPercent = total == 0 ? 0.0 : (correct * 100.0) / total;

		List<AccuracyHistoryItem> history = scored.stream()
				.map(prediction -> new AccuracyHistoryItem(
						prediction.getPredictedForDate(),
						prediction.getPredictedTrend(),
						prediction.getActualTrend(),
						Boolean.TRUE.equals(prediction.getWasCorrect())))
				.toList();

		return new AccuracyResponse(normalizedTicker, total, correct, accuracyPercent, history);
	}

	private PredictionDetailResponse toDetailResponse(Prediction prediction) {
		return new PredictionDetailResponse(
				prediction.getTicker(),
				prediction.getPredictedTrend(),
				prediction.getConfidence(),
				prediction.getReasoning(),
				toIndicatorsMap(prediction));
	}

	private Map<String, BigDecimal> toIndicatorsMap(Prediction prediction) {
		Map<String, BigDecimal> indicators = new LinkedHashMap<>();
		for (PredictionFeature feature : predictionFeatureRepository.findByPredictionId(prediction.getId())) {
			indicators.put(feature.getFeatureName(), feature.getFeatureValue());
		}
		return indicators;
	}

	private String normalizeTicker(String ticker) {
		return ticker.trim().toUpperCase();
	}
}
