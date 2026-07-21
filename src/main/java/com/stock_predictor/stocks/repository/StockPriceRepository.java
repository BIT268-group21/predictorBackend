package com.stock_predictor.stocks.repository;

import com.stock_predictor.stocks.entity.StockPrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {

	Optional<StockPrice> findTopByTickerOrderByPriceDateDesc(String ticker);

	Optional<StockPrice> findByTickerAndPriceDate(String ticker, LocalDate priceDate);

	List<StockPrice> findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
			String ticker, LocalDate fromDate);

	List<StockPrice> findByTickerOrderByPriceDateDesc(String ticker, Pageable pageable);

	boolean existsByTickerAndPriceDate(String ticker, LocalDate priceDate);
}
