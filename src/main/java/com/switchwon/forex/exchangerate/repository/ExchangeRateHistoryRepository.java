package com.switchwon.forex.exchangerate.repository;

import com.switchwon.forex.exchangerate.entity.ExchangeRateHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExchangeRateHistoryRepository extends JpaRepository<ExchangeRateHistory, Long> {

    Optional<ExchangeRateHistory> findTopByCurrencyOrderByCollectedAtDesc(String currency);
}
