package com.stock_predictor.predictions.repository;

import com.stock_predictor.predictions.entity.PredictionFeature;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictionFeatureRepository extends JpaRepository<PredictionFeature, Long> {

	List<PredictionFeature> findByPredictionId(Long predictionId);
}
