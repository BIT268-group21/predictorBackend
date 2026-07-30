package com.stock_predictor.predictions.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * The fixed, named feature set from decisions.md Section 3a's "features" object.
 * Modeled as explicit fields (not a generic map) so each maps to one atomic
 * column on the Prediction entity.
 */
public record PredictionFeatures(
		@JsonProperty("moving_avg_short") @NotNull BigDecimal movingAvgShort,
		@JsonProperty("moving_avg_long") @NotNull BigDecimal movingAvgLong,
		@JsonProperty("volatility_20d") @NotNull BigDecimal volatility20d,
		@JsonProperty("momentum_5d") @NotNull BigDecimal momentum5d) {
}
