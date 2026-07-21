package com.stock_predictor.stocks.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
		name = "stock_prices",
		uniqueConstraints = @UniqueConstraint(columnNames = {"ticker", "price_date"})
)
public class StockPrice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 10, nullable = false)
	private String ticker;

	@Column(name = "price_date", nullable = false)
	private LocalDate priceDate;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal open;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal high;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal low;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal close;

	@Column(nullable = false)
	private Long volume;

	protected StockPrice() {
	}

	public StockPrice(
			String ticker,
			LocalDate priceDate,
			BigDecimal open,
			BigDecimal high,
			BigDecimal low,
			BigDecimal close,
			Long volume) {
		this.ticker = ticker;
		this.priceDate = priceDate;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.volume = volume;
	}

	public Long getId() {
		return id;
	}

	public String getTicker() {
		return ticker;
	}

	public LocalDate getPriceDate() {
		return priceDate;
	}

	public BigDecimal getOpen() {
		return open;
	}

	public BigDecimal getHigh() {
		return high;
	}

	public BigDecimal getLow() {
		return low;
	}

	public BigDecimal getClose() {
		return close;
	}

	public Long getVolume() {
		return volume;
	}
}
