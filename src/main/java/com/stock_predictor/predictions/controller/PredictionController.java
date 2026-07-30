package com.stock_predictor.predictions.controller;

import com.stock_predictor.predictions.dto.AccuracyResponse;
import com.stock_predictor.predictions.dto.BatchPredictionRequest;
import com.stock_predictor.predictions.dto.BatchPredictionResponse;
import com.stock_predictor.predictions.dto.TopPredictionResponse;
import com.stock_predictor.predictions.service.PredictionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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

	@PostMapping("/batch")
	@ResponseStatus(HttpStatus.CREATED)
	public BatchPredictionResponse createBatch(@Valid @RequestBody BatchPredictionRequest request) {
		return predictionService.saveBatch(request);
	}
}
