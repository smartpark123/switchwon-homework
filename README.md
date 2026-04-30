
---

# [백엔드 개발자 과제] 실시간 환율 기반 외환 주문 시스템 구축
안녕하세요! 저희 채용 과정에 참여해주셔서 진심으로 감사합니다.
본 프로젝트는 외부 환율 API를 연동하여 실시간 환율 정보를 수집하고, 이를 기반으로 사용자가 외화를 주문(매수/매도)할 수 있는 서버 시스템을 구축하는 과제입니다. 과제를 통해 지원자님의 기술적 역량과 문제 해결 능력을 확인하고자 합니다.

## ✉️ 과제 제출 방법
- 제출 마감: 과제는 일주일 내에 완료하여 tech@switchwon.com 으로 제출해야 합니다.
- 제출 형식: 소스 코드를 GitHub 리포지토리에 업로드하고, 해당 GitHub 링크를 이메일로 제출해주세요.

## 📝  1. 과제 개요
주요 4개 통화(USD,JPY,CNY,EUR) 에 대해 실시간으로 변동하는 환율 정보를 데이터베이스에 이력으로 관리하고, 사용자가 요청한 외화 금액을 현재 환율에 맞춰 원화로 환산하여 주문을 처리하는 백엔드 시스템을 구현합니다.

- 환율 시세의 경우 무료로 사용가능한 외부 API 를 활용 하는것을 권장 드립니다. 과제 수행을 위해 별도의 비용을 지출하지 마시기 바랍니다. 
- 외부 API 연동이 어려울 경우, 서비스 구축 시점의 현실적인 시세 범위(Range) 내에서 환율 데이터가 생성(Mock)되도록 구현합니다.

---

## ✅ 2. 기술 제약 사항

### 🛠 Tech Stack
- **Language & Framework**: Java 17+ / Spring Boot 3.x
- **Database**: 지원자에게 별도 DB가 제공되지 않으므로 **In-Memory DB(H2)** 또는 **Local DB(SQLite)** 등을 활용 해주세요. 
- **Scheduling**: 환율 정보는 **1분 단위**로 최신화되어야 합니다.

### 📊 데이터 정밀도 및 환산 규칙
1. **환율 정밀도**: 모든 환율 소수점은 **둘째 자리까지 반올림**하여 처리합니다.
2. **단위 환산 (JPY)**: 1엔 단위로 시세를 제공하더라도, 국내 표준인 **100엔(JPY 100) 단위**로 환산하여 적용합니다.
3. **원화 절사**: 원화(KRW) 환산 금액의 소수점 이하는 **버림(Floor)** 처리합니다.
4. **환율 스프레드**: 외부 환율 API가 매매기준율/실시간 시세만 제공할 경우 buy/sell rate 에 대해서 아래 산식을 적용합니다.
   - **전신환 매입율(buyRate)**: 매매기준율 × 1.05 (5% 가산)
   - **전신환 매도율(sellRate)**: 매매기준율 × 0.95 (5% 차감)
5. 주말이나 API 제공처의 사정에 따라 환율 변동이 없는 경우도 있으므로, 스케줄러의 동작 확인을 위해 수집 시점에 임의의 랜덤값을 추가하여 시세를 변동시키셔도 무방합니다.
   
---

## ✅ 3. 데이터 저장 요구사항 (Persistence)

시스템은 다음 두 가지 핵심 도메인을 DB에 설계하고 저장해야 합니다.

1.  **환율 수신 이력 (Exchange Rate History)**
    - 스케줄러를 통해 수집된 통화별 매매기준율, 매입율, 매도율, 수집 일시를 저장합니다.
2.  **외화 주문 내역 (Order)**
    - 주문 시점의 **적용 환율**과 **환산 원화 금액**을 포함하여 주문 성공 결과를 저장합니다.

---

## 4. API 명세서

### [과제 1] 환율 정보 제공 API
**대상 통화:** `USD`, `JPY`, `CNY`, `EUR` (총 4개)

#### 1) 전체 통화 최신 환율 조회
- **Endpoint**: `GET /exchange-rate/latest`
- **Response**:
```json
{
  "code": "OK",
  "message": "SUCCESS",
  "returnObject": {
    "exchangeRateList": [
      {
        "currency": "USD",
        "buyRate": 1480.43,
        "tradeStanRate": 1477.45,
        "sellRate": 1474.47,
        "dateTime": "2026-04-22T10:01:00"
      },
      {
        "currency": "JPY",
        "buyRate": 915.20,
        "tradeStanRate": 910.50,
        "sellRate": 905.80,
        "dateTime": "2026-04-22T10:01:00"
      }
    ]
  }
}
```

#### 2) 특정 통화 최신 환율 상세 조회
- **Endpoint**: `GET /exchange-rate/latest/{currency}`
```json
{
  "code": "OK",
  "message": "SUCCESS",
  "returnObject": {
    "currency": "USD",
    "buyRate": 1480.43,
    "tradeStanRate": 1477.45,
    "sellRate": 1474.47,
    "dateTime": "2026-04-22T10:01:00"
  }
}
```

---

### [과제 2] 외화 주문 API
- 이중 통화 주문(예: USD/JPY)은 고려하지 않으며, 모든 주문은 `KRW`를 기준으로 진행합니다.
- 주문의 금액은 **외화** 기준으로 해주세요.

#### Case A: KRW → 외화 매수 (고객이 외화를 사는 경우)
- **Endpoint**: `POST /order`
- **Request Body**:
```json
{
  "forexAmount": 200,
  "fromCurrency": "KRW",
  "toCurrency": "USD"
}
```
- **Response**: (적용 환율: `buyRate`)
```json
{
  "code": "OK",
  "message": "SUCCESS",
  "returnObject": {
    "fromAmount": 296086,
    "fromCurrency": "KRW",
    "toAmount": 200.0,
    "toCurrency": "USD",
    "tradeRate": 1480.43,
    "dateTime" : "2026-04-22T10:01:00"
  }
}
```

#### Case B: 외화 → KRW 매도 (고객이 외화를 파는 경우)
- **Endpoint**: `POST /order`
- **Request Body**:
```json
{
  "forexAmount": 133,
  "fromCurrency": "USD",
  "toCurrency": "KRW"
}
```
- **Response**: (적용 환율: `sellRate`)
```json
{
  "code": "OK",
  "message": "SUCCESS",
  "returnObject": {
    "fromAmount": 133,
    "fromCurrency": "USD",
    "toAmount": 196104,
    "toCurrency": "KRW",
    "tradeRate": 1474.47,
    "dateTime" : "2026-04-22T10:01:00"
  }
}
```

---

### [과제 3] 주문 내역 조회 API
- **Endpoint**: `GET /order/list`
- **Response**: `orderList` 형태의 배열 반환
```json
{
  "code": "OK",
  "message": "SUCCESS",
  "returnObject": {
    "orderList": [
      {
        "id": 1,    // pkey
        "fromAmount": 296086,
        "fromCurrency": "KRW",
        "toAmount": 200.0,
        "toCurrency": "USD",
        "tradeRate": 1480.43,
        "dateTime" : "2026-04-22T10:01:00"
      },
      {
        "id": 2,
        "fromAmount": 133,
        "fromCurrency": "USD",
        "toAmount": 196104,
        "toCurrency": "KRW",
        "tradeRate": 1474.47,
        "dateTime" : "2026-04-22T10:01:00"
      }
    ]
  }
}
```

---

## ⚠️ 주의사항

- 공통응답 에러 처리에 대해서는 지원자님의 역량에 따라 구현 부탁 드리겠습니다. 기본 성공 응답 값은 아래의 내용을 참고 부탁 드립니다.
```json
 {
    "code": "OK",
    "message": "정상적으로 처리되었습니다.",
    ...data
}
```
- 과제 진행 시 **별도의 인증/세션은 고려하지 않고** 순수 과제 기능 본연에 집중해 주세요.
- 프로젝트 제출 시, 코드 실행이 되지 않아 평가가 불가능한 사례가 종종 발생하고 있습니다. 제출 전 최종 실행 여부를 반드시 확인해 주시기 바랍니다.
---

---

## 🚀 구현 내용 및 실행 가이드

### 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 |
| Database | H2 File Mode (재시작 후 데이터 유지) |
| ORM | Spring Data JPA |
| Build | Gradle (Kotlin DSL) |
| 외부 환율 API | [Frankfurter](https://api.frankfurter.dev) (무료, 인증 불필요) |

---

### 실행 방법

**1. 프로젝트 클론 후 실행**

```bash
git clone {repository_url}
cd switchwon-homework
./gradlew bootRun
```

별도의 DB 설치나 설정 없이 바로 실행됩니다.  
앱 시작 약 1분 후 스케줄러가 환율 데이터를 자동 수집합니다.

**2. 테스트 실행**

```bash
./gradlew test
```

**3. H2 콘솔 (DB 확인)**

브라우저에서 `http://localhost:8080/h2-console` 접속
- JDBC URL: `jdbc:h2:file:./data/forexdb`
- User Name: `sa`
- Password: (빈칸)

---

### 주요 구현 사항

**환율 수집**
- `@Scheduled(fixedDelay = 60_000)`으로 1분 주기 수집
- Frankfurter API 미응답 시 Mock 데이터로 자동 전환 (`forex.client.use-mock: true`)
- JPY: 1엔 기준 시세 → 100엔 단위 자동 환산

**환율 계산 규칙** (`CurrencyCalculator`)
- 매매기준율 소수점 2자리 반올림 (`HALF_UP`)
- 매입율(buyRate) = 매매기준율 × 1.05
- 매도율(sellRate) = 매매기준율 × 0.95
- KRW 환산 금액 소수점 이하 버림 (`FLOOR`)
- 모든 계산 `BigDecimal` 사용 (부동소수점 오차 방지)

**주문 처리**
- `fromCurrency=KRW` → 매수, `buyRate` 적용
- `toCurrency=KRW` → 매도, `sellRate` 적용
- KRW ↔ KRW, USD ↔ JPY 등 잘못된 조합 → 400 에러

**에러 처리**
- `@RestControllerAdvice`로 통합 예외 처리
- 에러 코드: `EXCHANGE_RATE_NOT_FOUND`, `INVALID_CURRENCY`, `INVALID_ORDER_REQUEST`

---

### API 테스트 예시 (curl)

```bash
# 전체 환율 조회
curl http://localhost:8080/exchange-rate/latest

# 특정 통화 조회
curl http://localhost:8080/exchange-rate/latest/USD

# 매수 주문 (KRW → USD 200달러)
curl -X POST http://localhost:8080/order \
  -H "Content-Type: application/json" \
  -d '{"forexAmount": 200, "fromCurrency": "KRW", "toCurrency": "USD"}'

# 매도 주문 (USD 133달러 → KRW)
curl -X POST http://localhost:8080/order \
  -H "Content-Type: application/json" \
  -d '{"forexAmount": 133, "fromCurrency": "USD", "toCurrency": "KRW"}'

# 주문 내역 조회
curl http://localhost:8080/order/list
```