# 아키텍처 문서 — 실시간 환율 기반 외환 주문 시스템

## 목차

1. [기술 스택](#1-기술-스택)
2. [아키텍처 개요](#2-아키텍처-개요)
3. [패키지 구조](#3-패키지-구조)
4. [레이어 책임 정의](#4-레이어-책임-정의)
5. [도메인 설계](#5-도메인-설계)
6. [DB 스키마](#6-db-스키마)
7. [API 명세](#7-api-명세)
8. [환율 수집 흐름](#8-환율-수집-흐름)
9. [주문 처리 흐름](#9-주문-처리-흐름)
10. [계산 규칙](#10-계산-규칙)
11. [공통 응답 구조](#11-공통-응답-구조)
12. [예외 처리 전략](#12-예외-처리-전략)
13. [외부 API 연동 전략](#13-외부-api-연동-전략)
14. [설정 관리](#14-설정-관리)

---

## 1. 기술 스택

| 항목 | 선택 | 비고 |
|------|------|------|
| Language | Java 17 | Record, sealed class 활용 가능 |
| Framework | Spring Boot 3.x | |
| Build Tool | Gradle (Kotlin DSL) | |
| Database | H2 In-Memory | 실행 편의성, 재시작 시 초기화 |
| ORM | Spring Data JPA + Hibernate | |
| Scheduler | Spring `@Scheduled` | |
| HTTP Client | Spring `RestClient` (3.2+) | 외부 환율 API 호출 |
| Validation | spring-boot-starter-validation | |
| Test | JUnit 5 + Mockito | |

---

## 2. 아키텍처 개요

```
┌──────────────────────────────────────────────────────┐
│                     Client (HTTP)                    │
└─────────────────────┬────────────────────────────────┘
                      │ REST
┌─────────────────────▼────────────────────────────────┐
│                  Controller Layer                    │
│         ExchangeRateController / OrderController     │
└─────────────────────┬────────────────────────────────┘
                      │
┌─────────────────────▼────────────────────────────────┐
│                   Service Layer                      │
│       ExchangeRateService / OrderService             │
│          CurrencyCalculator (util)                   │
└──────────┬──────────────────────┬────────────────────┘
           │                      │
┌──────────▼──────────┐  ┌────────▼────────────────────┐
│  Repository Layer   │  │  ExchangeRateClient          │
│  (Spring Data JPA)  │  │  (interface)                │
└──────────┬──────────┘  └────────┬────────────────────┘
           │                      │
┌──────────▼──────────┐  ┌────────▼────────────────────┐
│   H2 In-Memory DB   │  │  Frankfurter API (외부)      │
│                     │  │  또는 MockExchangeRateClient  │
└─────────────────────┘  └─────────────────────────────┘

           ↑ 별도 스레드
┌──────────┴──────────────────────────────────────────┐
│            ExchangeRateScheduler                    │
│            @Scheduled(fixedDelay = 60_000ms)        │
└─────────────────────────────────────────────────────┘
```

---

## 3. 패키지 구조

```
src/main/java/com/switchwon/forex/
│
├── ForexApplication.java                     # Spring Boot 진입점
│
├── common/                                   # 전체 공통 모듈
│   ├── response/
│   │   ├── ApiResponse.java                  # 공통 응답 래퍼 ApiResponse<T>
│   │   └── ErrorCode.java                    # 에러 코드 enum
│   └── exception/
│       ├── BusinessException.java            # 비즈니스 예외 기반 클래스
│       └── GlobalExceptionHandler.java       # @RestControllerAdvice
│
├── exchangerate/                             # 환율 도메인
│   ├── controller/
│   │   └── ExchangeRateController.java
│   ├── service/
│   │   └── ExchangeRateService.java
│   ├── scheduler/
│   │   └── ExchangeRateScheduler.java        # 1분 주기 환율 수집
│   ├── client/
│   │   ├── ExchangeRateClient.java           # 인터페이스
│   │   ├── FrankfurterExchangeRateClient.java# 실제 외부 API 구현체
│   │   └── MockExchangeRateClient.java       # Mock 구현체 (fallback)
│   ├── repository/
│   │   └── ExchangeRateHistoryRepository.java
│   ├── entity/
│   │   └── ExchangeRateHistory.java          # JPA Entity
│   └── dto/
│       ├── ExchangeRateDto.java              # 단건 환율 DTO
│       └── ExchangeRateListResponse.java     # 목록 응답 DTO
│
├── order/                                    # 주문 도메인
│   ├── controller/
│   │   └── OrderController.java
│   ├── service/
│   │   └── OrderService.java
│   ├── repository/
│   │   └── OrderRepository.java
│   ├── entity/
│   │   └── Order.java                        # JPA Entity
│   └── dto/
│       ├── OrderRequest.java                 # POST /order 요청 DTO
│       └── OrderResponse.java               # 주문 결과 응답 DTO
│
└── util/
    └── CurrencyCalculator.java               # 환율 계산 유틸 (순수 static)

src/main/resources/
├── application.yml                           # 기본 설정
└── application-mock.yml                      # Mock 프로파일 설정

src/test/java/com/switchwon/forex/
├── exchangerate/
│   ├── service/ExchangeRateServiceTest.java
│   └── client/FrankfurterExchangeRateClientTest.java
├── order/
│   └── service/OrderServiceTest.java
└── util/
    └── CurrencyCalculatorTest.java           # 계산 규칙 단위 테스트
```

---

## 4. 레이어 책임 정의

### Controller
- HTTP 요청 수신 및 입력값 `@Valid` 검증
- Service 호출 후 `ApiResponse<T>`로 래핑하여 반환
- 비즈니스 로직 **없음**

### Service
- 비즈니스 규칙 처리 (환율 조회, 주문 방향 판단, 금액 계산)
- `@Transactional` 경계 관리
- Repository 및 ExchangeRateClient 오케스트레이션

### Repository
- Spring Data JPA 인터페이스
- 커스텀 쿼리는 JPQL 또는 메서드 네이밍 활용

### ExchangeRateClient (인터페이스)
```java
public interface ExchangeRateClient {
    // 통화 코드 → 매매기준율(KRW 기준) 맵 반환
    Map<String, BigDecimal> fetchBaseRates();
}
```
- 구현체 교체를 `@Profile` 또는 `@ConditionalOnProperty`로 제어
- Service는 인터페이스에만 의존 → 외부 API 변경 시 Service 무변경

### Scheduler
- `ExchangeRateClient`를 통해 원시 환율 수집
- `CurrencyCalculator`로 buyRate/sellRate 계산
- `ExchangeRateService`(또는 직접 Repository)에 저장 위임

### CurrencyCalculator
- 순수 `static` 유틸 메서드 모음, 상태 없음
- 단위 테스트 독립적으로 작성 가능

---

## 5. 도메인 설계

### ExchangeRateHistory (환율 수신 이력)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK, auto-increment |
| `currency` | String | 통화 코드 (USD/JPY/CNY/EUR) |
| `tradeStanRate` | BigDecimal | 매매기준율 |
| `buyRate` | BigDecimal | 전신환 매입율 |
| `sellRate` | BigDecimal | 전신환 매도율 |
| `collectedAt` | LocalDateTime | 수집 일시 |

### Order (외화 주문)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK, auto-increment |
| `fromCurrency` | String | 출발 통화 |
| `toCurrency` | String | 도착 통화 |
| `forexAmount` | BigDecimal | 요청 외화 금액 |
| `fromAmount` | BigDecimal | 실제 지불 금액 |
| `toAmount` | BigDecimal | 실제 수령 금액 |
| `tradeRate` | BigDecimal | 적용 환율 |
| `createdAt` | LocalDateTime | 주문 일시 |

---

## 6. DB 스키마

```sql
CREATE TABLE exchange_rate_history (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    currency       VARCHAR(10)    NOT NULL,
    trade_stan_rate DECIMAL(10,2) NOT NULL,
    buy_rate       DECIMAL(10,2)  NOT NULL,
    sell_rate      DECIMAL(10,2)  NOT NULL,
    collected_at   TIMESTAMP      NOT NULL
);

CREATE TABLE orders (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_currency VARCHAR(10)    NOT NULL,
    to_currency   VARCHAR(10)    NOT NULL,
    forex_amount  DECIMAL(18,4)  NOT NULL,
    from_amount   DECIMAL(18,4)  NOT NULL,
    to_amount     DECIMAL(18,4)  NOT NULL,
    trade_rate    DECIMAL(10,2)  NOT NULL,
    created_at    TIMESTAMP      NOT NULL
);

-- 최신 환율 조회 성능을 위한 인덱스
CREATE INDEX idx_exchange_rate_currency_collected
    ON exchange_rate_history (currency, collected_at DESC);
```

> H2는 `spring.jpa.hibernate.ddl-auto=create-drop` 설정으로 DDL 자동 생성.  
> `orders` 테이블명을 사용하는 이유: `order`는 SQL 예약어이므로 충돌 방지.

---

## 7. API 명세

### 공통 응답 형식

**성공**
```json
{
  "code": "OK",
  "message": "정상적으로 처리되었습니다.",
  "returnObject": { ... }
}
```

**실패**
```json
{
  "code": "CURRENCY_NOT_FOUND",
  "message": "지원하지 않는 통화입니다.",
  "returnObject": null
}
```

---

### GET /exchange-rate/latest

전체 통화(USD, JPY, CNY, EUR) 최신 환율 조회

**Response 200**
```json
{
  "code": "OK",
  "message": "정상적으로 처리되었습니다.",
  "returnObject": {
    "exchangeRateList": [
      {
        "currency": "USD",
        "buyRate": 1480.43,
        "tradeStanRate": 1477.45,
        "sellRate": 1474.47,
        "dateTime": "2026-04-22T10:01:00"
      }
    ]
  }
}
```

---

### GET /exchange-rate/latest/{currency}

특정 통화 최신 환율 상세 조회

**Path Variable**: `currency` — USD | JPY | CNY | EUR

**Response 200**
```json
{
  "code": "OK",
  "message": "정상적으로 처리되었습니다.",
  "returnObject": {
    "currency": "USD",
    "buyRate": 1480.43,
    "tradeStanRate": 1477.45,
    "sellRate": 1474.47,
    "dateTime": "2026-04-22T10:01:00"
  }
}
```

**Response 404** — 해당 통화 데이터 없음
```json
{
  "code": "EXCHANGE_RATE_NOT_FOUND",
  "message": "환율 정보를 찾을 수 없습니다.",
  "returnObject": null
}
```

---

### POST /order

외화 매수(KRW→외화) 또는 매도(외화→KRW) 주문

**Request Body**
```json
{
  "forexAmount": 200,
  "fromCurrency": "KRW",
  "toCurrency": "USD"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `forexAmount` | number | Y | 외화 기준 금액 (양수) |
| `fromCurrency` | string | Y | 출발 통화 |
| `toCurrency` | string | Y | 도착 통화 |

**주문 방향 판단**

| fromCurrency | toCurrency | 적용 환율 | 계산 |
|---|---|---|---|
| KRW | USD/JPY/CNY/EUR | `buyRate` | fromAmount = floor(forexAmount × buyRate) |
| USD/JPY/CNY/EUR | KRW | `sellRate` | toAmount = floor(forexAmount × sellRate) |
| 그 외 조합 | — | — | 400 Bad Request |

**Response 200 (매수)**
```json
{
  "code": "OK",
  "message": "정상적으로 처리되었습니다.",
  "returnObject": {
    "fromAmount": 296086,
    "fromCurrency": "KRW",
    "toAmount": 200.0,
    "toCurrency": "USD",
    "tradeRate": 1480.43,
    "dateTime": "2026-04-22T10:01:00"
  }
}
```

**Response 400** — 잘못된 통화 조합 또는 금액
```json
{
  "code": "INVALID_ORDER_REQUEST",
  "message": "주문 통화 조합이 올바르지 않습니다.",
  "returnObject": null
}
```

---

### GET /order/list

전체 주문 내역 조회

**Response 200**
```json
{
  "code": "OK",
  "message": "정상적으로 처리되었습니다.",
  "returnObject": {
    "orderList": [
      {
        "id": 1,
        "fromAmount": 296086,
        "fromCurrency": "KRW",
        "toAmount": 200.0,
        "toCurrency": "USD",
        "tradeRate": 1480.43,
        "dateTime": "2026-04-22T10:01:00"
      }
    ]
  }
}
```

---

## 8. 환율 수집 흐름

```
ExchangeRateScheduler.collect()  [1분마다 실행 — fixedDelay]
    │
    ├─ ExchangeRateClient.fetchBaseRates()
    │       └─ 반환: Map<String, BigDecimal>  { "USD": 1477.45, "JPY": 9.105, ... }
    │
    ├─ CurrencyCalculator.applyJpyUnit(jpyRate)
    │       └─ JPY: 9.105 → 910.50  (× 100, 소수점 2자리 반올림)
    │
    ├─ CurrencyCalculator.calcBuyRate(baseRate)
    │       └─ baseRate × 1.05, 소수점 2자리 반올림
    │
    ├─ CurrencyCalculator.calcSellRate(baseRate)
    │       └─ baseRate × 0.95, 소수점 2자리 반올림
    │
    └─ ExchangeRateHistoryRepository.saveAll(histories)
            └─ 4개 통화 이력 한 번에 저장
```

---

## 9. 주문 처리 흐름

```
POST /order  →  OrderController.createOrder(@Valid OrderRequest)
    │
    └─ OrderService.createOrder(request)
            │
            ├─ [1] 통화 방향 검증
            │       fromCurrency=KRW → 매수 / toCurrency=KRW → 매도
            │       그 외 → BusinessException(INVALID_ORDER_REQUEST)
            │
            ├─ [2] 외화 통화 코드 추출 (매수: toCurrency, 매도: fromCurrency)
            │
            ├─ [3] ExchangeRateHistoryRepository.findTopByCurrencyOrderByCollectedAtDesc(currency)
            │       없으면 → BusinessException(EXCHANGE_RATE_NOT_FOUND)
            │
            ├─ [4] CurrencyCalculator.calcKrwAmount(forexAmount, rate)
            │       결과에 floor 적용
            │
            ├─ [5] Order 엔티티 생성 → OrderRepository.save(order)
            │
            └─ [6] OrderResponse 반환
```

---

## 10. 계산 규칙

> 모든 계산은 `BigDecimal`로 처리. `double`/`float` 사용 금지.

### 환율 정밀도

```java
// 소수점 2자리 반올림
BigDecimal.setScale(2, RoundingMode.HALF_UP)
```

### JPY 100엔 환산

```java
// 1엔 기준 시세를 100엔 단위로 변환
BigDecimal jpyBaseRate = rawJpyRate
    .multiply(new BigDecimal("100"))
    .setScale(2, RoundingMode.HALF_UP);
```

### 매입율(buyRate) / 매도율(sellRate)

```java
BigDecimal buyRate  = baseRate.multiply(new BigDecimal("1.05"))
                              .setScale(2, RoundingMode.HALF_UP);
BigDecimal sellRate = baseRate.multiply(new BigDecimal("0.95"))
                              .setScale(2, RoundingMode.HALF_UP);
```

### KRW 환산 (매수 — KRW → 외화)

```java
// 고객이 외화 forexAmount를 사려면 KRW를 얼마 내야 하는가
// buyRate 적용, 소수점 이하 버림
BigDecimal krwAmount = forexAmount
    .multiply(buyRate)
    .setScale(0, RoundingMode.FLOOR);
```

### KRW 환산 (매도 — 외화 → KRW)

```java
// 고객이 외화 forexAmount를 팔면 KRW를 얼마 받는가
// sellRate 적용, 소수점 이하 버림
BigDecimal krwAmount = forexAmount
    .multiply(sellRate)
    .setScale(0, RoundingMode.FLOOR);
```

---

## 11. 공통 응답 구조

```java
// ApiResponse.java
public record ApiResponse<T>(
    String code,
    String message,
    T returnObject
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "정상적으로 처리되었습니다.", data);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }
}
```

---

## 12. 예외 처리 전략

```java
// ErrorCode.java
public enum ErrorCode {
    EXCHANGE_RATE_NOT_FOUND("EXCHANGE_RATE_NOT_FOUND", "환율 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_CURRENCY("INVALID_CURRENCY", "지원하지 않는 통화입니다.", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_REQUEST("INVALID_ORDER_REQUEST", "주문 요청이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 내부 오류입니다.", HttpStatus.INTERNAL_SERVER_ERROR);
}
```

```
예외 발생 경로:
Service → throw BusinessException(ErrorCode.XXX)
    → GlobalExceptionHandler.handleBusinessException()
    → ApiResponse.error(errorCode) 반환
```

`@Valid` 검증 실패 시 `MethodArgumentNotValidException`도 `GlobalExceptionHandler`에서 통합 처리.

---

## 13. 외부 API 연동 전략

### 기본: Frankfurter API

```
GET https://api.frankfurter.app/latest?from=KRW&to=USD,JPY,CNY,EUR
```

- 완전 무료, 인증 불필요
- 응답 예시:
  ```json
  {
    "base": "KRW",
    "rates": { "USD": 0.0006767, "JPY": 0.10985, "CNY": 0.004927, "EUR": 0.0005941 }
  }
  ```
- KRW 기준 환율이므로 역수 변환 필요: `1 / rate`

### Fallback: Mock 구현

```yaml
# application-mock.yml
forex:
  client:
    use-mock: true
```

```java
@ConditionalOnProperty(name = "forex.client.use-mock", havingValue = "true")
@Component
public class MockExchangeRateClient implements ExchangeRateClient {
    // 현실적인 시세 범위 내 랜덤 노이즈 ±0.5% 추가
}
```

기본 프로파일에서는 `FrankfurterExchangeRateClient`가 활성화되며,  
`--spring.profiles.active=mock` 또는 `forex.client.use-mock=true`로 Mock 전환.

---

## 14. 설정 관리

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:h2:mem:forexdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  h2:
    console:
      enabled: true        # http://localhost:8080/h2-console 접근 가능

forex:
  client:
    use-mock: false        # true 시 MockExchangeRateClient 활성화
  scheduler:
    currencies: USD,JPY,CNY,EUR
```
