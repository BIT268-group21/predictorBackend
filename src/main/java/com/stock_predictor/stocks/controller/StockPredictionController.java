package com.stock_predictor.stocks.controller;

import com.stock_predictor.predictions.dto.PredictionDetailResponse;
import com.stock_predictor.predictions.service.PredictionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class StockPredictionController {

	private final PredictionService predictionService;

	public StockPredictionController(PredictionService predictionService) {
		this.predictionService = predictionService;
	}

	@GetMapping("/{ticker}/prediction")
	public PredictionDetailResponse getLatestPrediction(@PathVariable String ticker) {
		return predictionService.getLatestPrediction(ticker);
	}
}
