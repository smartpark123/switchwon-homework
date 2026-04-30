package com.switchwon.forex.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String fromCurrency;

    @Column(nullable = false, length = 10)
    private String toCurrency;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal forexAmount;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal fromAmount;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal toAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tradeRate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Order() {}

    public Order(String fromCurrency, String toCurrency, BigDecimal forexAmount,
                 BigDecimal fromAmount, BigDecimal toAmount,
                 BigDecimal tradeRate, LocalDateTime createdAt) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.forexAmount = forexAmount;
        this.fromAmount = fromAmount;
        this.toAmount = toAmount;
        this.tradeRate = tradeRate;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getFromCurrency() { return fromCurrency; }
    public String getToCurrency() { return toCurrency; }
    public BigDecimal getForexAmount() { return forexAmount; }
    public BigDecimal getFromAmount() { return fromAmount; }
    public BigDecimal getToAmount() { return toAmount; }
    public BigDecimal getTradeRate() { return tradeRate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
