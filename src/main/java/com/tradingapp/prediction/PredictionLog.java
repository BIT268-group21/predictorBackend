package com.tradingapp.prediction;

import com.tradingapp.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "prediction_logs")
public class PredictionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String ticker;

    @Column(name = "trend_classification")
    private String trendClassification;

    @Column(name = "confidence_score")
    private double confidenceScore;

    @Column(name = "horizon_days")
    private int horizonDays;

    /** Comma-joined pattern names; empty string when the model found none. */
    @Column(name = "detected_patterns", length = 1000)
    private String detectedPatterns;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PredictionLog() {
    }

    public PredictionLog(User user, String ticker, String trendClassification, double confidenceScore,
                         int horizonDays, List<String> detectedPatterns) {
        this.user = user;
        this.ticker = ticker;
        this.trendClassification = trendClassification;
        this.confidenceScore = confidenceScore;
        this.horizonDays = horizonDays;
        this.detectedPatterns = detectedPatterns == null ? "" : String.join(",", detectedPatterns);
    }

    @PrePersist
    void onPersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTicker() {
        return ticker;
    }

    public String getTrendClassification() {
        return trendClassification;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public int getHorizonDays() {
        return horizonDays;
    }

    public String getDetectedPatterns() {
        return detectedPatterns;
    }

    public List<String> getDetectedPatternsAsList() {
        if (detectedPatterns == null || detectedPatterns.isBlank()) {
            return List.of();
        }
        return Arrays.stream(detectedPatterns.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .toList();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
