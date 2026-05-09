# How to Run the Application

## Prerequisites

- Java 21 (verify: `java -version`)
- Maven 3.6+ (verify: `mvn -version`)
- Docker (optional, for containerised run — verify: `docker --version`)

---

## Local Run (port 8080)

**Build:**
```bash
cd homework-1
mvn clean package
```

Produces `target/banking-api-1.0.0.jar`.

**Run — Option A (Maven plugin):**
```bash
mvn spring-boot:run
```

**Run — Option B (JAR directly):**
```bash
java -jar target/banking-api-1.0.0.jar
```

**Run — Option C (demo script):**
```bash
./demo/run.sh
```

Server starts on **http://localhost:8080**.

---

## Docker Run (port 8080)

**Build image and run container:**
```bash
docker build -t banking-api .
docker run --rm -p 8080:8080 banking-api
```

**Or with Docker Compose:**
```bash
docker compose up --build
```

**Or using the demo script:**
```bash
./demo/run-docker.sh
```

Server starts on **http://localhost:8080**.

To stop Docker Compose: `Ctrl+C`, then `docker compose down`.

> The Docker image uses a multi-stage build — no need to run `mvn package` first.

---

## Test with curl

> All URLs below work for both local and Docker runs — both use port **8080**.

**Create a deposit:**
```bash
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"type":"deposit","toAccount":"ACC-12345","amount":1000.00,"currency":"USD"}' | jq .
```

**Create a transfer:**
```bash
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"type":"transfer","fromAccount":"ACC-12345","toAccount":"ACC-67890","amount":300.00,"currency":"USD"}' | jq .
```

**List all transactions:**
```bash
curl -s http://localhost:8080/transactions | jq .
```

**Filter by account and type:**
```bash
curl -s "http://localhost:8080/transactions?accountId=ACC-12345&type=transfer" | jq .
```

**Filter by date range:**
```bash
curl -s "http://localhost:8080/transactions?from=2024-01-01&to=2024-12-31" | jq .
```

**Account balance:**
```bash
curl -s http://localhost:8080/accounts/ACC-12345/balance | jq .
```

**Account summary (Option A):**
```bash
curl -s http://localhost:8080/accounts/ACC-12345/summary | jq .
```

**Interest calculation (Option B):**
```bash
curl -s "http://localhost:8080/accounts/ACC-12345/interest?rate=0.05&days=30" | jq .
```

**Export CSV (Option C):**
```bash
curl -s "http://localhost:8080/transactions/export?format=csv"
```

**Test rate limiting (Option D) — 101 requests, last returns 429:**
```bash
for i in $(seq 1 101); do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/transactions
done
```

**Validation error examples:**
```bash
# Negative amount + bad currency
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"type":"deposit","toAccount":"ACC-12345","amount":-5,"currency":"XYZ"}' | jq .

# Missing required account for type
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"type":"deposit","amount":100,"currency":"USD"}' | jq .

# Missing required query param
curl -s "http://localhost:8080/transactions/export" | jq .
```

---

## Test with IDE HTTP Client

Open `demo/sample-requests.http` in VS Code (with REST Client extension) or IntelliJ IDEA.
Each `###` block is a runnable request — click "Send Request" above each block.

> The file uses `@baseUrl = http://localhost:8080` — works for both local and Docker runs.
