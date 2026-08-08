package com.stock_predictor.ingestion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stock_predictor.ingestion.twelvedata.TwelveDataClient;
import com.stock_predictor.predictions.repository.PredictionRepository;
import com.stock_predictor.stocks.entity.Stock;
import com.stock_predictor.stocks.repository.StockPriceRepository;
import com.stock_predictor.stocks.repository.StockRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class IngestionServiceTest {

	@Test
	void classifyTrendUp() {
		assertEquals("up", IngestionService.classifyTrend(new BigDecimal("100"), new BigDecimal("105")));
	}

	@Test
	void classifyTrendDown() {
		assertEquals("down", IngestionService.classifyTrend(new BigDecimal("100"), new BigDecimal("95")));
	}

	@Test
	void classifyTrendFlat() {
		assertEquals("flat", IngestionService.classifyTrend(new BigDecimal("100"), new BigDecimal("100.05")));
	}

	@Test
	void ingestPricesForAllStocksSleepsBetweenTickersButNotAfterTheLast() {
		StockRepository stockRepository = Mockito.mock(StockRepository.class);
		StockPriceRepository stockPriceRepository = Mockito.mock(StockPriceRepository.class);
		PredictionRepository predictionRepository = Mockito.mock(PredictionRepository.class);
		TwelveDataClient twelveDataClient = Mockito.mock(TwelveDataClient.class);

		when(stockRepository.findAll()).thenReturn(List.of(
				new Stock("AAPL", "Apple", "Tech"),
				new Stock("MSFT", "Microsoft", "Tech"),
				new Stock("GOOGL", "Alphabet", "Tech")));
		when(twelveDataClient.fetchHistoricalPrices(anyString())).thenReturn(List.of());

		IngestionService service = spy(new IngestionService(
				stockRepository, stockPriceRepository, predictionRepository, twelveDataClient));
		doNothing().when(service).sleepBetweenRequests();

		service.ingestPricesForAllStocks();

		verify(twelveDataClient, times(3)).fetchHistoricalPrices(anyString());
		// 3 tickers -> 2 gaps between them, no trailing sleep after the last one.
		verify(service, times(2)).sleepBetweenRequests();
	}

	@Test
	void ingestPricesForAllStocksDoesNotSleepForASingleTicker() {
		StockRepository stockRepository = Mockito.mock(StockRepository.class);
		StockPriceRepository stockPriceRepository = Mockito.mock(StockPriceRepository.class);
		PredictionRepository predictionRepository = Mockito.mock(PredictionRepository.class);
		TwelveDataClient twelveDataClient = Mockito.mock(TwelveDataClient.class);

		when(stockRepository.findAll()).thenReturn(List.of(new Stock("AAPL", "Apple", "Tech")));
		when(twelveDataClient.fetchHistoricalPrices(anyString())).thenReturn(List.of());

		IngestionService service = spy(new IngestionService(
				stockRepository, stockPriceRepository, predictionRepository, twelveDataClient));
		doNothing().when(service).sleepBetweenRequests();

		service.ingestPricesForAllStocks();

		verify(service, never()).sleepBetweenRequests();
	}
}
