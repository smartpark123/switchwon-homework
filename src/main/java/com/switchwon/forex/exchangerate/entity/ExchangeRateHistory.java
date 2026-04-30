package com.switchwon.forex.exchangerate.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "exchange_rate_history",
    indexes = @Index(name = "idx_currency_collected_at", columnList = "currency, collected_at DESC")
)
public class ExchangeRateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tradeStanRate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal buyRate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sellRate;

    @Column(nullable = false)
    private LocalDateTime collectedAt;

    protected ExchangeRateHistory() {}

    public ExchangeRateHistory(String currency, BigDecimal tradeStanRate,
                               BigDecimal buyRate, BigDecimal sellRate,
                               LocalDateTime collectedAt) {
        this.currency = currency;
        this.tradeStanRate = tradeStanRate;
        this.buyRate = buyRate;
        this.sellRate = sellRate;
        this.collectedAt = collectedAt;
    }

    public Long getId() { return id; }
    public String getCurrency() { return currency; }
    public BigDecimal getTradeStanRate() { return tradeStanRate; }
    public BigDecimal getBuyRate() { return buyRate; }
    public BigDecimal getSellRate() { return sellRate; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
}
