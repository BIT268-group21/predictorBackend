package com.tradingapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * Client for the Python prediction microservice. The read timeout is generous
     * on purpose: the predictor may be on a free tier that cold-starts in 30-60s.
     */
    @Bean
    RestClient predictorRestClient(@Value("${predictor.base-url}") String baseUrl,
                                   @Value("${predictor.api-key}") String apiKey) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(60_000);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-Key", apiKey)
                .requestFactory(factory)
                .build();
    }
}
