package com.stock_predictor.stocks.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stocks")
public class Stock {

	@Id
	@Column(length = 10)
	private String ticker;

	@Column(name = "company_name", length = 160, nullable = false)
	private String companyName;

	@Column(length = 80)
	private String sector;

	protected Stock() {
	}

	public Stock(String ticker, String companyName, String sector) {
		this.ticker = ticker;
		this.companyName = companyName;
		this.sector = sector;
	}

	public String getTicker() {
		return ticker;
	}

	public String getCompanyName() {
		return companyName;
	}

	public String getSector() {
		return sector;
	}
}
