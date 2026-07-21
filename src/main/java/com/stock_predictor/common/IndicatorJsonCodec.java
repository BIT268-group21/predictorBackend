package com.stock_predictor.common;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
public class IndicatorJsonCodec {

	private static final TypeReference<Map<String, BigDecimal>> INDICATOR_TYPE =
			new TypeReference<>() {};

	private final JsonMapper jsonMapper;

	public IndicatorJsonCodec(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public String toJson(Object value) {
		try {
			return jsonMapper.writeValueAsString(value);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to serialize JSON", ex);
		}
	}

	public Map<String, BigDecimal> indicatorsFromJson(String json) {
		if (json == null || json.isBlank()) {
			return Collections.emptyMap();
		}
		try {
			Map<String, BigDecimal> raw = jsonMapper.readValue(json, INDICATOR_TYPE);
			Map<String, BigDecimal> normalized = new LinkedHashMap<>();
			for (Map.Entry<String, BigDecimal> entry : raw.entrySet()) {
				normalized.put(normalizeIndicatorKey(entry.getKey()), entry.getValue());
			}
			return normalized;
		} catch (Exception ex) {
			return Collections.emptyMap();
		}
	}

	public Map<String, BigDecimal> normalizeIndicators(Map<String, BigDecimal> indicators) {
		if (indicators == null || indicators.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, BigDecimal> normalized = new LinkedHashMap<>();
		for (Map.Entry<String, BigDecimal> entry : indicators.entrySet()) {
			normalized.put(normalizeIndicatorKey(entry.getKey()), entry.getValue());
		}
		return normalized;
	}

	private String normalizeIndicatorKey(String key) {
		return switch (key) {
			case "sma_5" -> "sma5";
			case "sma_20" -> "sma20";
			case "rsi_14" -> "rsi14";
			default -> key;
		};
	}
}
