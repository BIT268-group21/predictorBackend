package com.stock_predictor.predictions.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mirrors the per-stock shape in decisions.md Section 3a (snake_case on the wire).
 */
public record PredictionBatchItem(
		@NotBlank String ticker,
		@JsonProperty("prediction_date") @NotNull LocalDate predictionDate,
		@JsonProperty("target_date") @NotNull LocalDate targetDate,
		@JsonProperty("predicted_direction") @NotBlank
				@Pattern(regexp = "up|down", message = "must be 'up' or 'down'") String predictedDirection,
		@NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal confidence,
		@JsonProperty("model_accuracy") @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal modelAccuracy,
		@NotNull Map<String, BigDecimal> features,
		@JsonProperty("last_close_price") @NotNull @DecimalMin("0.0") BigDecimal lastClosePrice) {
}
