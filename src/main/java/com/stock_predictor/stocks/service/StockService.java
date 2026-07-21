package com.stock_predictor.stocks.service;

import com.stock_predictor.common.ResourceNotFoundException;
import com.stock_predictor.stocks.dto.PricePointResponse;
import com.stock_predictor.stocks.dto.StockProfileResponse;
import com.stock_predictor.stocks.entity.Stock;
import com.stock_predictor.stocks.entity.StockPrice;
import com.stock_predictor.stocks.repository.StockPriceRepository;
import com.stock_predictor.stocks.repository.StockRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StockService {

	private final StockRepository stockRepository;
	private final StockPriceRepository stockPriceRepository;

	public StockService(StockRepository stockRepository, StockPriceRepository stockPriceRepository) {
		this.stockRepository = stockRepository;
		this.stockPriceRepository = stockPriceRepository;
	}

	public StockProfileResponse getProfile(String ticker) {
		Stock stock = stockRepository.findById(normalizeTicker(ticker))
				.orElseThrow(() -> new ResourceNotFoundException("Stock not found: " + ticker));
		return new StockProfileResponse(stock.getTicker(), stock.getCompanyName(), stock.getSector());
	}

	public List<PricePointResponse> getRecentPrices(String ticker, int days) {
		requireStockExists(ticker);
		int lookbackDays = Math.max(1, Math.min(days, 365));
		List<StockPrice> prices = stockPriceRepository
				.findByTickerOrderByPriceDateDesc(normalizeTicker(ticker), PageRequest.of(0, lookbackDays));
		return prices.stream()
				.sorted(Comparator.comparing(StockPrice::getPriceDate))
				.map(price -> new PricePointResponse(
						price.getPriceDate(),
						price.getOpen(),
						price.getHigh(),
						price.getLow(),
						price.getClose(),
						price.getVolume()))
				.toList();
	}

	public List<StockPrice> getPricesForPrediction(String ticker, int lookbackDays) {
		requireStockExists(ticker);
		return stockPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
				normalizeTicker(ticker),
				java.time.LocalDate.now().minusDays(lookbackDays));
	}

	private void requireStockExists(String ticker) {
		if (!stockRepository.existsById(normalizeTicker(ticker))) {
			throw new ResourceNotFoundException("Stock not found: " + ticker);
		}
	}

	private String normalizeTicker(String ticker) {
		return ticker.trim().toUpperCase();
	}
}
