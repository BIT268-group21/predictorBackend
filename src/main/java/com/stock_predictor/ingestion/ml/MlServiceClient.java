package com.stock_predictor.ingestion.ml;

import com.stock_predictor.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MlServiceClient {

	private static final Logger log = LoggerFactory.getLogger(MlServiceClient.class);

	private final RestClient restClient;
	private final AppProperties appProperties;

	public MlServiceClient(RestClient restClient, AppProperties appProperties) {
		this.restClient = restClient;
		this.appProperties = appProperties;
	}

	public MlPredictResponse predict(MlPredictRequest request) {
		try {
			MlPredictResponse response = restClient.post()
					.uri(appProperties.ml().baseUrl() + "/predict")
					.body(request)
					.retrieve()
					.body(MlPredictResponse.class);
			if (response == null) {
				throw new IllegalStateException("Empty response from ML service for " + request.ticker());
			}
			return response;
		} catch (Exception ex) {
			log.error("ML prediction failed for {}: {}", request.ticker(), ex.getMessage());
			throw new IllegalStateException("ML prediction failed for " + request.ticker(), ex);
		}
	}
}
