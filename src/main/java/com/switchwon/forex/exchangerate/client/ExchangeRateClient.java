package com.switchwon.forex.exchangerate.client;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 외부 환율 API 추상화 인터페이스.
 * 구현체는 FrankfurterExchangeRateClient(실제) 또는 MockExchangeRateClient(fallback).
 * 반환값은 통화 코드 → KRW 기준 매매기준율 (1단위당).
 * JPY의 경우 1엔 기준 반환 — 100엔 환산은 Scheduler/Service에서 처리.
 */
public interface ExchangeRateClient {

    Map<String, BigDecimal> fetchBaseRates();
}
