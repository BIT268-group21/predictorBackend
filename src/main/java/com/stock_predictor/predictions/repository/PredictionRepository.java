package com.stock_predictor.predictions.repository;

import com.stock_predictor.predictions.entity.Prediction;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

	Optional<Prediction> findTopByTickerOrderByPredictedForDateDescCreatedAtDesc(String ticker);

	List<Prediction> findByTickerAndActualTrendIsNotNullOrderByPredictedForDateDesc(String ticker);

	@Query("""
			SELECT p FROM Prediction p
			WHERE p.actualTrend IS NULL
			  AND p.predictedForDate < :asOfDate
			ORDER BY p.predictedForDate ASC
			""")
	List<Prediction> findPendingAccuracyChecks(@Param("asOfDate") LocalDate asOfDate);

	@Query("""
			SELECT p FROM Prediction p
			WHERE p.predictedForDate = (
			    SELECT MAX(p2.predictedForDate) FROM Prediction p2 WHERE p2.ticker = p.ticker
			)
			ORDER BY p.confidence DESC, p.createdAt DESC
			""")
	List<Prediction> findLatestPerTicker();
}
