# Skill: Banking Transactions API (Homework 1)

## Homework Goal

Build a Simple Banking Transactions REST API using Java + Spring Boot.
All data is stored in-memory (no database). Implements all core tasks and all additional features (A–D).

---

## Project Architecture

```
homework-1/
├── pom.xml                          Spring Boot 3.2.5, Java 21
├── Dockerfile                       Multi-stage build; exposes port 8080
├── docker-compose.yml               Maps host:8080 → container:8080
└── src/main/java/com/homework/banking/
    ├── BankingApplication.java      Entry point
    ├── config/
    │   └── RateLimitingFilter.java  Servlet filter — 100 req/min/IP (Option D)
    ├── controller/
    │   ├── TransactionController.java   /transactions endpoints
    │   └── AccountController.java       /accounts endpoints
    ├── model/
    │   ├── Transaction.java         Immutable domain model (final class, no setters)
    │   ├── TransactionType.java     Enum: DEPOSIT, WITHDRAWAL, TRANSFER (@JsonValue → lowercase)
    │   └── TransactionStatus.java   Enum: PENDING, COMPLETED, FAILED (@JsonValue → lowercase)
    ├── dto/
    │   ├── CreateTransactionRequest.java  Input DTO with Bean Validation annotations
    │   ├── BalanceResponse.java           Record: accountId + Map<currency, balance>
    │   ├── SummaryResponse.java           Record: totals, count, mostRecentTransaction
    │   └── InterestResponse.java          Record: balances, interest, projectedBalance
    ├── exception/
    │   ├── ErrorResponse.java             Record: {error, details[{field, message}]}
    │   ├── ValidationException.java       RuntimeException carrying field + message
    │   ├── TransactionNotFoundException.java  RuntimeException for 404
    │   └── GlobalExceptionHandler.java    @RestControllerAdvice — all error formats
    ├── service/
    │   ├── TransactionService.java    CRUD + filtering + CSV export + format validation
    │   └── AccountService.java        Balance, summary, interest + param validation
    └── store/
        └── TransactionStore.java      ConcurrentHashMap — thread-safe in-memory store
```

---

## API Endpoints

| Method | Path | Status Codes | Description |
|---|---|---|---|
| POST | `/transactions` | 201, 400 | Create transaction |
| GET | `/transactions` | 200 | List/filter transactions |
| GET | `/transactions/{id}` | 200, 404 | Get by ID |
| GET | `/transactions/export?format=csv` | 200, 400 | Export CSV |
| GET | `/accounts/{id}/balance` | 200 | Balance by currency |
| GET | `/accounts/{id}/summary` | 200 | Totals + count + recent date |
| GET | `/accounts/{id}/interest?rate=&days=` | 200, 400 | Simple interest calc |

Rate limiting returns **429** when > 100 requests/minute/IP.

---

## Validation Rules

| Field | Rule | Layer |
|---|---|---|
| `amount` | Required, positive, max 2 decimal places | Bean Validation (`@Digits`) |
| `fromAccount` / `toAccount` | Format: `ACC-[A-Za-z0-9]{5}` | Bean Validation (`@Pattern`) |
| `currency` | Curated subset of ISO 4217: `USD EUR GBP JPY CHF CAD AUD` | Bean Validation (`@Pattern`) |
| `type` | One of: `deposit withdrawal transfer` | Bean Validation (`@Pattern`) |
| deposit → `toAccount` required | Business rule | Service (`ValidationException`) |
| withdrawal → `fromAccount` required | Business rule | Service (`ValidationException`) |
| transfer → both required + different | Business rule | Service (`ValidationException`) |
| `rate` | Must be positive | Service (`ValidationException`) |
| `days` | Must be ≥ 1 | Service (`ValidationException`) |
| `format` (export) | Must be `csv` | Service (`ValidationException`) |

---

## Error Response

All errors share the `ErrorResponse` record shape:

```json
{
  "error": "Validation failed",
  "details": [{ "field": "toAccount", "message": "toAccount is required for deposit transactions" }]
}
```

`GlobalExceptionHandler` handles:
- `MethodArgumentNotValidException` → 400 (bean validation)
- `ValidationException` → 400 (business/service rules, with real field name)
- `TransactionNotFoundException` → 404
- `HttpMessageNotReadableException` → 400 (malformed JSON)
- `MissingServletRequestParameterException` → 400 (missing required `?param=`)
- `MethodArgumentTypeMismatchException` → 400 (e.g., bad date format)
- `IllegalArgumentException` → 400 (safety fallback)

---

## Business Rules

- `deposit`: `toAccount` required, `fromAccount` optional
- `withdrawal`: `fromAccount` required, `toAccount` optional
- `transfer`: both required, must be different
- All created transactions default to `status: completed`
- Balance calculation uses only `completed` transactions
- Multiple currencies → grouped by currency in balance, summary, and interest responses

---

## How to Run

**Local (port 8080):**
```bash
cd homework-1
mvn spring-boot:run
```

**Docker (port 8080):**
```bash
docker build -t banking-api .
docker run --rm -p 8080:8080 banking-api
```

**Docker Compose:**
```bash
docker compose up --build
```

---

## How to Test

```bash
# Create deposit
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"type":"deposit","toAccount":"ACC-12345","amount":1000.00,"currency":"USD"}'

# Check balance
curl -s http://localhost:8080/accounts/ACC-12345/balance

# Export CSV
curl -s "http://localhost:8080/transactions/export?format=csv"
```

Or open `demo/sample-requests.http` in VS Code REST Client / IntelliJ HTTP Client.

---

## Extension Points (Future Improvements)

- **Persistence**: Replace `TransactionStore` with a JPA repository + H2/Postgres
- **Authentication**: Add Spring Security with JWT for per-user account isolation
- **Pagination**: Add `Pageable` to `GET /transactions` for large datasets
- **Distributed rate limiting**: Replace in-memory filter with Redis + Bucket4j
- **Multi-currency transfer**: Add FX rate conversion on cross-currency transfers
- **Tests**: Add `@SpringBootTest` integration tests with `MockMvc` for all endpoints
