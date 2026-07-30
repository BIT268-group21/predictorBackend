package com.tradingapp.prediction;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictionLogRepository extends JpaRepository<PredictionLog, Long> {

    List<PredictionLog> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
