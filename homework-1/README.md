# Banking Transactions API

**Student**: Vadym Drozdov
**AI Tools Used**: Claude Code (claude-sonnet-4-6)

---

## Project Overview

A simple in-memory REST API for banking transactions built with Java 21 + Spring Boot 3.2.5.
Implements all core tasks and all four optional features (A–D) from the homework specification.

---

## Implemented Features

**Core (Tasks 1–3)**
- Create transactions (deposit, withdrawal, transfer)
- List transactions with combined filtering (accountId, type, date range)
- Get transaction by ID
- Account balance grouped by currency

**Additional Features**
- Option A: Account summary (totals per currency, count, most recent date)
- Option B: Simple interest calculation per currency
- Option C: CSV export of all transactions
- Option D: In-memory rate limiting — 100 req/min per IP (returns 429)

---

## Architecture Decisions

| Decision | Rationale |
|---|---|
| `ConcurrentHashMap` as data store | Thread-safe, O(1) reads — no database needed for homework |
| `BigDecimal` for amounts | Prevents floating-point errors; `@Digits(fraction=2)` enforces 2 decimal max |
| `TransactionType` / `TransactionStatus` enums | Compile-time safety; `@JsonValue` serializes as lowercase strings |
| `Transaction` immutable (final class, no setters) | Transactions are created once and never mutated |
| `ErrorResponse` record | Type-safe, consistent JSON error shape across all handlers |
| `ValidationException(field, message)` | Carries field name from service layer to error handler |
| `CopyOnWriteArrayList` for rate limiter | Safe concurrent access at low-volume homework scale |
| Records for response DTOs | Concise, immutable — ideal for read-only response objects in Java 21 |
| Servlet `Filter` for rate limiting | Runs before Spring dispatcher, catches all requests |
| Multi-currency balance as `Map<String, BigDecimal>` | Naturally handles the "group by currency" requirement |

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/transactions` | Create a transaction |
| GET | `/transactions` | List transactions (filterable) |
| GET | `/transactions/{id}` | Get transaction by ID |
| GET | `/transactions/export?format=csv` | Export all transactions as CSV |
| GET | `/accounts/{accountId}/balance` | Account balance by currency |
| GET | `/accounts/{accountId}/summary` | Deposit/withdrawal totals and count |
| GET | `/accounts/{accountId}/interest?rate=0.05&days=30` | Simple interest calculation |

---

## Transaction Model

```json
{
  "id": "uuid",
  "fromAccount": "ACC-XXXXX or null",
  "toAccount": "ACC-XXXXX or null",
  "amount": 100.00,
  "currency": "USD",
  "type": "deposit | withdrawal | transfer",
  "timestamp": "2024-01-15T10:30:00Z",
  "status": "completed"
}
```

---

## Validation Rules

- `amount`: required, positive, maximum 2 decimal places
- `fromAccount` / `toAccount`: format `ACC-XXXXX` (exactly 5 alphanumeric chars after dash)
- `currency`: one of `USD`, `EUR`, `GBP`, `JPY`, `CHF`, `CAD`, `AUD` — a curated subset of ISO 4217 (full standard not implemented)
- `type`: one of `deposit`, `withdrawal`, `transfer`
- `rate` (interest endpoint): must be positive
- `days` (interest endpoint): must be at least 1
- Business rules:
  - deposit → `toAccount` required
  - withdrawal → `fromAccount` required
  - transfer → both required, must be different accounts

---

## Error Response Format

All errors share a consistent shape:

```json
{
  "error": "Validation failed",
  "details": [
    { "field": "amount", "message": "Amount must be a positive number" }
  ]
}
```

| Scenario | HTTP | `error` value |
|---|---|---|
| Bean validation failure | 400 | `"Validation failed"` |
| Business rule violation | 400 | `"Validation failed"` |
| Missing required param | 400 | `"Validation failed"` |
| Wrong param type | 400 | `"Validation failed"` |
| Transaction not found | 404 | `"Not Found"` |
| Rate limit exceeded | 429 | `"Rate limit exceeded"` |

---

## Docker

**Build and run:**
```bash
docker build -t banking-api .
docker run --rm -p 8080:8080 banking-api
```

API available at **http://localhost:8080**

**Or with Docker Compose:**
```bash
docker compose up --build
```

---

## AI-Assisted Development Note

This project was implemented using **Claude Code** (claude-sonnet-4-6) as an AI coding assistant.
The AI helped with: project scaffolding, implementation of all classes, business logic, code review,
and iterative improvements toward a senior-level submission standard.
All generated code was reviewed for correctness against the homework specification.
