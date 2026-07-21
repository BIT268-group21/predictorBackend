package com.stock_predictor.stocks.repository;

import com.stock_predictor.stocks.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, String> {
}
