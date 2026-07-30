package com.stock_predictor.predictions.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "predictions")
public class Prediction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 10, nullable = false)
	private String ticker;

	@Column(name = "predicted_trend", length = 10, nullable = false)
	private String predictedTrend;

	@Column(nullable = false, precision = 4, scale = 3)
	private BigDecimal confidence;

	@Column(columnDefinition = "TEXT")
	private String reasoning;

	@Column(name = "predicted_for_date", nullable = false)
	private LocalDate predictedForDate;

	@Column(name = "actual_trend", length = 10)
	private String actualTrend;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "prediction_date")
	private LocalDate predictionDate;

	@Column(name = "model_accuracy", precision = 5, scale = 4)
	private BigDecimal modelAccuracy;

	// Atomic feature columns (1NF) -- previously stored together as one JSON
	// blob in a single TEXT column, which isn't a single indivisible value.
	// Exactly these four, named, are what the Data Contract (decisions.md
	// Section 3a) actually sends per prediction, so they're modeled as real
	// columns rather than a generic/open-ended map.
	@Column(name = "moving_avg_short", precision = 12, scale = 4)
	private BigDecimal movingAvgShort;

	@Column(name = "moving_avg_long", precision = 12, scale = 4)
	private BigDecimal movingAvgLong;

	@Column(name = "volatility_20d", precision = 8, scale = 4)
	private BigDecimal volatility20d;

	@Column(name = "momentum_5d", precision = 8, scale = 4)
	private BigDecimal momentum5d;

	@Column(name = "last_close_price", precision = 12, scale = 4)
	private BigDecimal lastClosePrice;

	protected Prediction() {
	}

	/** Live-generation path (no batch-only fields: prediction_date, model_accuracy, features, last_close_price). */
	public Prediction(
			String ticker,
			String predictedTrend,
			BigDecimal confidence,
			String reasoning,
			LocalDate predictedForDate) {
		this(ticker, predictedTrend, confidence, reasoning, predictedForDate,
				null, null, null, null, null, null, null);
	}

	public Prediction(
			String ticker,
			String predictedTrend,
			BigDecimal confidence,
			String reasoning,
			LocalDate predictedForDate,
			LocalDate predictionDate,
			BigDecimal modelAccuracy,
			BigDecimal movingAvgShort,
			BigDecimal movingAvgLong,
			BigDecimal volatility20d,
			BigDecimal momentum5d,
			BigDecimal lastClosePrice) {
		this.ticker = ticker;
		this.predictedTrend = predictedTrend;
		this.confidence = confidence;
		this.reasoning = reasoning;
		this.predictedForDate = predictedForDate;
		this.predictionDate = predictionDate;
		this.modelAccuracy = modelAccuracy;
		this.movingAvgShort = movingAvgShort;
		this.movingAvgLong = movingAvgLong;
		this.volatility20d = volatility20d;
		this.momentum5d = momentum5d;
		this.lastClosePrice = lastClosePrice;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getTicker() {
		return ticker;
	}

	public String getPredictedTrend() {
		return predictedTrend;
	}

	public BigDecimal getConfidence() {
		return confidence;
	}

	public String getReasoning() {
		return reasoning;
	}

	public LocalDate getPredictedForDate() {
		return predictedForDate;
	}

	public String getActualTrend() {
		return actualTrend;
	}

	/**
	 * Not a stored column (3NF -- this is fully determined by predictedTrend and
	 * actualTrend, so persisting it separately would be a transitive dependency /
	 * redundant, derivable data). Null until actualTrend is graded.
	 */
	public Boolean getWasCorrect() {
		if (actualTrend == null) {
			return null;
		}
		return actualTrend.equals(predictedTrend);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public LocalDate getPredictionDate() {
		return predictionDate;
	}

	public BigDecimal getModelAccuracy() {
		return modelAccuracy;
	}

	public BigDecimal getMovingAvgShort() {
		return movingAvgShort;
	}

	public BigDecimal getMovingAvgLong() {
		return movingAvgLong;
	}

	public BigDecimal getVolatility20d() {
		return volatility20d;
	}

	public BigDecimal getMomentum5d() {
		return momentum5d;
	}

	public BigDecimal getLastClosePrice() {
		return lastClosePrice;
	}

	public void setActualTrend(String actualTrend) {
		this.actualTrend = actualTrend;
	}
}
