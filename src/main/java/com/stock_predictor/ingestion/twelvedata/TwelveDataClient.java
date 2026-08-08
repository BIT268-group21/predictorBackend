package com.stock_predictor.ingestion.twelvedata;

import com.stock_predictor.config.AppProperties;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Twelve Data replacement for the old FMP client (decisions.md: one external
 * price data source for the whole system, reusing the ML side's
 * TWELVE_DATA_API_KEY). Rate limiting (8 requests/minute on the free tier) is
 * enforced by the caller between tickers, not here — see
 * IngestionService.ingestPricesForAllStocks().
 */
@Component
public class TwelveDataClient {

	private static final Logger log = LoggerFactory.getLogger(TwelveDataClient.class);

	// Only the last couple of trading days are needed to fill in stock_prices for
	// grading, not full history.
	private static final int OUTPUT_SIZE = 10;

	private final RestClient restClient;
	private final AppProperties appProperties;

	public TwelveDataClient(RestClient restClient, AppProperties appProperties) {
		this.restClient = restClient;
		this.appProperties = appProperties;
	}

	public List<TwelveDataPriceRecord> fetchHistoricalPrices(String ticker) {
		String apiKey = appProperties.twelvedata().apiKey();
		if (apiKey == null || apiKey.isBlank()) {
			log.warn("Twelve Data API key is not configured; skipping fetch for {}", ticker);
			return List.of();
		}

		TwelveDataPriceRecord.TimeSeriesResponse response = restClient.get()
				.uri(appProperties.twelvedata().baseUrl()
						+ "/time_series?symbol={symbol}&interval=1day&outputsize={outputsize}&apikey={apikey}",
						ticker, OUTPUT_SIZE, apiKey)
				.retrieve()
				.body(TwelveDataPriceRecord.TimeSeriesResponse.class);

		if (response == null) {
			log.warn("Twelve Data returned an empty response for {}", ticker);
			return List.of();
		}

		if ("error".equals(response.status())) {
			log.warn("Twelve Data returned an error for {}: {}", ticker, response.message());
			return List.of();
		}

		if (response.values() == null || response.values().isEmpty()) {
			return List.of();
		}

		List<TwelveDataPriceRecord> records = new ArrayList<>(response.values().size());
		for (TwelveDataPriceRecord.RawValue value : response.values()) {
			TwelveDataPriceRecord record = parseValue(ticker, value);
			if (record != null) {
				records.add(record);
			}
		}

		// Twelve Data returns values newest-first; the rest of the pipeline expects
		// ascending order, same as the ML pipeline's own pull_and_check.py.
		records.sort(Comparator.comparing(TwelveDataPriceRecord::date));
		return records;
	}

	private TwelveDataPriceRecord parseValue(String ticker, TwelveDataPriceRecord.RawValue value) {
		try {
			return new TwelveDataPriceRecord(
					value.datetime(),
					new BigDecimal(value.open()),
					new BigDecimal(value.high()),
					new BigDecimal(value.low()),
					new BigDecimal(value.close()),
					Long.valueOf(value.volume()));
		} catch (NumberFormatException | NullPointerException ex) {
			log.warn("Skipping unparsable Twelve Data value for {}: {}", ticker, value);
			return null;
		}
	}
}
