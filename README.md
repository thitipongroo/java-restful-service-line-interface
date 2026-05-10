# LINE Interface RESTful Service

A JAX-RS (Jersey 1.x) RESTful web service that integrates with a LINE customer data platform. The service exposes four POST endpoints for customer lookup, LINE profile registration, government ID update, and customer verification.

---

## Architecture

```text
┌──────────────────────────────────────────────────────┐
│               LINE Platform / Client App              │
│   POST /rest/LineInterface/{endpoint}                 │
└──────────────────────┬───────────────────────────────┘
                       │ HTTP/JSON
                       ▼
┌──────────────────────────────────────────────────────┐
│            Tomcat 8+ Servlet Container                │
│                                                      │
│   Jersey JAX-RS dispatcher                           │
│   └── LineInterfaceService.java  (@Path, @POST)      │
│         │                                            │
│         ├── Gson deserialise RequestLine → POJOs     │
│         ├── Business logic + validation              │
│         └── Gson serialise ResponseLine → JSON       │
└──────────┬────────────────────────┬──────────────────┘
           │ JDBC (direct)          │ HTTP (CUS-20)
           ▼                        ▼
  ┌─────────────────┐    ┌─────────────────────────┐
  │   PostgreSQL DB  │    │  CUS-20 External Service │
  │                  │    │  (customer verification) │
  │  line_customer   │    │  GET /api/customer       │
  │  line_update     │    │  via CallWSUtils.java     │
  └─────────────────┘    └─────────────────────────┘
           │
           ▼
  SLF4J + Log4j (rolling file)
  ${catalina.home}/logs/line-interface/
```

---

## Tech Stack

| Layer | Technology | Why |
| ----- | ---------- | --- |
| Language | Java 11 | LTS release; widely deployed in enterprise servlet containers |
| REST framework | JAX-RS via Jersey 1.19 | Standard Java EE REST API; integrates with Tomcat as a servlet filter — no Spring IoC overhead |
| JSON | Gson 2.10 + org.json | Gson handles POJO ↔ JSON mapping; org.json used for ad-hoc JSON construction in the HTTP client |
| Database | PostgreSQL (JDBC direct) | Direct JDBC gives fine-grained control over query shape; no ORM abstraction layer |
| Logging | SLF4J + Log4j | Industry-standard Java logging with rolling file appender; decoupled logger API (SLF4J) from implementation (Log4j) |
| Build | Maven 3 | Standard Java build lifecycle; dependency management via POM |
| Runtime | Servlet 3.0 (Tomcat 8+) | WAR deployment model; compatible with most enterprise Java hosting environments |

---

## Project Structure

```text
java-restful-service-line-interface/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/ws/integration/
    │   │   ├── config/
    │   │   │   ├── AppConfig.java          # Singleton config — reads env vars with defaults
    │   │   │   └── EnvType.java            # Enum of config key names
    │   │   ├── dao/
    │   │   │   └── LineInterfaceDao.java   # JDBC data access — all 4 operations
    │   │   ├── endpoint/
    │   │   │   └── LineInterfaceService.java  # JAX-RS resource class (@Path, @POST)
    │   │   ├── model/
    │   │   │   ├── ErrorResponse.java
    │   │   │   ├── HeaderData.java
    │   │   │   ├── RequestLine.java
    │   │   │   ├── RequestRecord.java
    │   │   │   ├── ResponseLine.java
    │   │   │   ├── ResponseRecord.java
    │   │   │   └── ResponseStatus.java
    │   │   └── util/
    │   │       ├── CallWSUtils.java        # HTTP client for CUS-20 external service
    │   │       ├── ConnectionDB.java       # JDBC connection factory
    │   │       └── DatabaseUtils.java      # JDBC resource cleanup helpers
    │   ├── resources/
    │   │   └── log4j.properties
    │   └── webapp/
    │       ├── META-INF/MANIFEST.MF
    │       └── WEB-INF/web.xml             # Jersey servlet registration
    └── test/
        └── java/                           # Unit / integration tests
```

---

## System Flow

```text
Incoming POST
  │
  ▼ Jersey dispatcher → LineInterfaceService.java
  │
  ├─ Gson.fromJson(body, RequestLine.class)
  │    → HeaderData { messageId, sentDateTime }
  │    → RequestRecord { firstName, lastName, dob / govtId / citizenId }
  │
  ├─ Validate required fields → return ErrorResponse (statusCode=E) if missing
  │
  ├─ [Line-01] checkCustomerName
  │    └─ DAO.findByName(firstName, lastName, dob)
  │         → SELECT partyId, govtId FROM line_customer WHERE ...
  │         → ResponseRecord { partyId, govtId }
  │
  ├─ [Line-02] insertCustomerDetail
  │    └─ DAO.insertLineProfile(govtId, mobilePhone, email, lineId)
  │
  ├─ [Line-03] updateCustomerGovId
  │    └─ DAO.insertGovIdUpdate(govtId)
  │         → INSERT INTO line_update ...
  │
  └─ [Line-05] checkCustomerGovId
       ├─ CallWSUtils.callCUS20(citizenId)  → try external service first
       │    → HTTP GET CUS20_DOMAIN/api/customer?citizenId=...
       │    └─ parse statusCode Y/N
       └─ if CUS-20 fails → fallback: SELECT FROM line_update WHERE govtId=citizenId

  Response envelope:
  {
    "headerData":     { messageId, sentDateTime, responseDateTime },
    "responseRecord": { ... },
    "responseStatus": { statusCode: "S"|"E", errorCode: "200"|"500", errorMessage }
  }
```

---

## API Reference

Base path: `/rest/LineInterface`

All endpoints accept and return `application/json`.

### POST `/checkCustomerName` — Line-01

Check whether a customer exists by first name, last name, and date of birth.

**Request:**

```json
{
  "headerData": {
    "messageId": "MSG-001",
    "sentDateTime": "2024-01-01T10:00:00"
  },
  "requestRecord": {
    "firstName": "สมชาย",
    "lastName": "ใจดี",
    "dob": "01/01/2510"
  }
}
```

> `dob` must be in `dd/MM/yyyy` format using the **Buddhist Era** year.

**Success response:**

```json
{
  "headerData": { "messageId": "MSG-001", "sentDateTime": "...", "responseDateTime": "..." },
  "responseRecord": { "partyId": 12345, "govtId": "1234567890123" },
  "responseStatus": { "statusCode": "S", "errorCode": "200", "errorMessage": "ดำเนินการเรียบร้อย" }
}
```

**Error response:**

```json
{
  "responseStatus": { "statusCode": "E", "errorCode": "500", "errorMessage": "Customer not found" }
}
```

---

### POST `/insertCustomerDetail` — Line-02

Register a customer's LINE profile (LINE ID, mobile, email) and log the entry.

Required fields: `govtId`, `mobilePhone`, `email`, `lineId`

---

### POST `/updateCustomerGovId` — Line-03

Record a government ID update request into the `line_update` table.

Required fields: `govtId`

---

### POST `/checkCustomerGovId` — Line-05

Verify whether a citizen ID exists — first by calling CUS-20, then falling back to the local `line_update` table.

Required fields: `citizenId`

| `statusCode` in `responseRecord` | Meaning |
| --------------------------------- | ------- |
| `Y` | Customer found |
| `N` | Customer not found |

---

## Response Envelope

Every response shares a common structure:

```json
{
  "headerData":     { "messageId": "...", "sentDateTime": "...", "responseDateTime": "..." },
  "responseRecord": { "..." : "..." },
  "responseStatus": {
    "statusCode":   "S or E",
    "errorCode":    "200 or 500",
    "errorMessage": "..."
  }
}
```

| `statusCode` | `errorCode` | Meaning |
| ------------ | ----------- | ------- |
| `S` | `200` | Success |
| `E` | `500` | Error |

---

## Configuration

All runtime values are read from **environment variables**. Localhost defaults are used when a variable is not set.

| Variable | Description | Default |
| -------- | ----------- | ------- |
| `DB_URL` | Full JDBC connection URL | `jdbc:postgresql://localhost:5432/customer` |
| `DB_USER` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | _(empty)_ |
| `CUS20_DOMAIN` | Base URL of the CUS-20 external service | `http://localhost:3001` |

Set variables before starting the container:

```bash
export DB_URL=jdbc:postgresql://db-host:5432/customer
export DB_USER=appuser
export DB_PASSWORD=secret
export CUS20_DOMAIN=http://cus20-service:3001
```

---

## Building and Deployment

Requires **Java 11+** and **Maven 3.6+**.

```bash
mvn clean package
```

The WAR is produced at `target/line-interface.war`. Deploy to **Apache Tomcat 8+**:

```bash
cp target/line-interface.war $CATALINA_HOME/webapps/
```

The application will be available at:

```text
http://<host>:<port>/line-interface/rest/LineInterface/<endpoint>
```

---

## Logging

Logs are written to both stdout and a rolling file:

```text
${catalina.home}/logs/line-interface/line-interface.log
```

Maximum file size: 5 MB, 10 backup files retained.

---

## Tradeoffs

| Decision | Alternative | Reasoning |
| -------- | ----------- | --------- |
| JAX-RS / Jersey | Spring Boot | No IoC container or auto-configuration overhead; simpler deployment as a WAR to an existing Tomcat pool |
| Direct JDBC | JPA / Hibernate | Full control over SQL; no N+1 or lazy-loading surprises; acceptable for a small, stable schema |
| Synchronous CUS-20 HTTP call | Async HTTP (CompletableFuture) | Simpler error handling; CUS-20 latency is acceptable for the expected request volume |
| Gson for JSON | Jackson | Gson is lighter; no annotation-driven configuration needed for simple POJO mapping |
| Singleton `AppConfig` | CDI / Spring `@Value` | No DI framework available; a singleton is the minimal correct solution without introducing new dependencies |

---

## Scaling Considerations

| Concern | Approach |
| ------- | -------- |
| JDBC connection exhaustion under load | Replace `ConnectionDB` with HikariCP connection pool — it handles pool sizing, timeout, and validation |
| CUS-20 latency blocks Tomcat threads | Move to an async HTTP client (OkHttp + `CompletableFuture`); allows more concurrent requests without increasing thread count |
| Single Tomcat instance SPOF | Deploy multiple Tomcat instances behind an NGINX/Apache load balancer; sessions are stateless (no session required — each request is self-contained) |
| Log volume in production | Replace rolling file appender with a log aggregator (ELK stack or Loki) to enable full-text search across instances |
| Java 11 EOL in enterprise | Upgrade POM to Java 17 LTS; Jersey 1.x → Jersey 3.x (Jakarta EE 9+) for continued security patches |
