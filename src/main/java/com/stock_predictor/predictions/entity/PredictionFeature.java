package com.stock_predictor.predictions.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * One named feature value belonging to one Prediction -- e.g. ("lag_return_1", 0.0123).
 *
 * Modeled as a child table rather than fixed columns on Prediction (or a JSON
 * blob) because the ML pipeline does per-stock correlation-based feature
 * selection: the set of feature names differs per stock and isn't fixed or
 * known in advance (decisions.md Section 3a/14). A row per (prediction,
 * feature name) is the normalized way to represent a variable-arity attribute
 * set: every column here is atomic (1NF), feature_value depends on the whole
 * (prediction_id, feature_name) pair rather than either part alone (2NF), and
 * there's nothing else non-key to be transitively dependent on anything (3NF).
 */
@Entity
@Table(
		name = "prediction_features",
		uniqueConstraints = @UniqueConstraint(columnNames = {"prediction_id", "feature_name"}))
public class PredictionFeature {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "prediction_id", nullable = false)
	private Prediction prediction;

	@Column(name = "feature_name", length = 60, nullable = false)
	private String featureName;

	@Column(name = "feature_value", precision = 14, scale = 6, nullable = false)
	private BigDecimal featureValue;

	protected PredictionFeature() {
	}

	public PredictionFeature(Prediction prediction, String featureName, BigDecimal featureValue) {
		this.prediction = prediction;
		this.featureName = featureName;
		this.featureValue = featureValue;
	}

	public Long getId() {
		return id;
	}

	public Prediction getPrediction() {
		return prediction;
	}

	public String getFeatureName() {
		return featureName;
	}

	public BigDecimal getFeatureValue() {
		return featureValue;
	}
}
