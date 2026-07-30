package com.tradingapp.alert;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<Alert> findByIdAndUserId(Long id, Long userId);

    List<Alert> findByTriggeredFalse();
}
