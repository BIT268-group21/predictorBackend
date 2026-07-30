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

	@Column(name = "was_correct")
	private Boolean wasCorrect;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(columnDefinition = "TEXT")
	private String indicatorsJson;

	@Column(name = "prediction_date")
	private LocalDate predictionDate;

	@Column(name = "model_accuracy", precision = 5, scale = 4)
	private BigDecimal modelAccuracy;

	@Column(name = "last_close_price", precision = 12, scale = 4)
	private BigDecimal lastClosePrice;

	protected Prediction() {
	}

	public Prediction(
			String ticker,
			String predictedTrend,
			BigDecimal confidence,
			String reasoning,
			LocalDate predictedForDate,
			String indicatorsJson) {
		this(ticker, predictedTrend, confidence, reasoning, predictedForDate, indicatorsJson, null, null, null);
	}

	public Prediction(
			String ticker,
			String predictedTrend,
			BigDecimal confidence,
			String reasoning,
			LocalDate predictedForDate,
			String indicatorsJson,
			LocalDate predictionDate,
			BigDecimal modelAccuracy,
			BigDecimal lastClosePrice) {
		this.ticker = ticker;
		this.predictedTrend = predictedTrend;
		this.confidence = confidence;
		this.reasoning = reasoning;
		this.predictedForDate = predictedForDate;
		this.indicatorsJson = indicatorsJson;
		this.predictionDate = predictionDate;
		this.modelAccuracy = modelAccuracy;
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

	public Boolean getWasCorrect() {
		return wasCorrect;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public String getIndicatorsJson() {
		return indicatorsJson;
	}

	public LocalDate getPredictionDate() {
		return predictionDate;
	}

	public BigDecimal getModelAccuracy() {
		return modelAccuracy;
	}

	public BigDecimal getLastClosePrice() {
		return lastClosePrice;
	}

	public void setActualTrend(String actualTrend) {
		this.actualTrend = actualTrend;
	}

	public void setWasCorrect(Boolean wasCorrect) {
		this.wasCorrect = wasCorrect;
	}
}
