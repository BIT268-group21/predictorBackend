package com.stock_predictor.ingestion.fmp;

import com.stock_predictor.config.AppProperties;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FmpClient {

	private static final Logger log = LoggerFactory.getLogger(FmpClient.class);

	private final RestClient restClient;
	private final AppProperties appProperties;

	public FmpClient(RestClient restClient, AppProperties appProperties) {
		this.restClient = restClient;
		this.appProperties = appProperties;
	}

	public List<FmpPriceRecord> fetchHistoricalPrices(String ticker) {
		String apiKey = appProperties.fmp().apiKey();
		if (apiKey == null || apiKey.isBlank()) {
			log.warn("FMP API key is not configured; skipping fetch for {}", ticker);
			return List.of();
		}

		FmpPriceRecord[] response = restClient.get()
				.uri(appProperties.fmp().baseUrl() + "/stable/historical-price-eod/full?symbol={symbol}&apikey={apikey}",
						ticker, apiKey)
				.retrieve()
				.body(FmpPriceRecord[].class);

		if (response == null || response.length == 0) {
			return List.of();
		}
		return Arrays.asList(response);
	}
}
