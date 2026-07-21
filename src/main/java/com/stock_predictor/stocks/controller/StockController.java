package com.stock_predictor.stocks.controller;

import com.stock_predictor.stocks.dto.PricePointResponse;
import com.stock_predictor.stocks.dto.StockProfileResponse;
import com.stock_predictor.stocks.service.StockService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

	private final StockService stockService;

	public StockController(StockService stockService) {
		this.stockService = stockService;
	}

	@GetMapping("/{ticker}")
	public StockProfileResponse getStock(@PathVariable String ticker) {
		return stockService.getProfile(ticker);
	}

	@GetMapping("/{ticker}/prices")
	public List<PricePointResponse> getPrices(
			@PathVariable String ticker,
			@RequestParam(defaultValue = "30") int days) {
		return stockService.getRecentPrices(ticker, days);
	}
}
