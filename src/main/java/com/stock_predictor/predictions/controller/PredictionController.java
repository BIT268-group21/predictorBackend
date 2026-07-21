package com.stock_predictor.predictions.controller;

import com.stock_predictor.predictions.dto.AccuracyResponse;
import com.stock_predictor.predictions.dto.TopPredictionResponse;
import com.stock_predictor.predictions.service.PredictionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

	private final PredictionService predictionService;

	public PredictionController(PredictionService predictionService) {
		this.predictionService = predictionService;
	}

	@GetMapping("/top")
	public List<TopPredictionResponse> getTopPredictions(
			@RequestParam(defaultValue = "10") int limit) {
		return predictionService.getTopPredictions(limit);
	}

	@GetMapping("/accuracy")
	public AccuracyResponse getAccuracy(@RequestParam String ticker) {
		return predictionService.getAccuracyHistory(ticker);
	}
}
