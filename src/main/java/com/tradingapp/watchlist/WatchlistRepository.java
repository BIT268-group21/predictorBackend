package com.tradingapp.watchlist;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {

    List<WatchlistItem> findByUserIdOrderByCreatedAtAscIdAsc(Long userId);

    Optional<WatchlistItem> findByUserIdAndTicker(Long userId, String ticker);

    boolean existsByUserIdAndTicker(Long userId, String ticker);
}
