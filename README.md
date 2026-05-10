# LINE Interface RESTful Service

A JAX-RS (Jersey 1.x) RESTful web service that integrates with a LINE customer data platform. The service exposes four POST endpoints for customer lookup, LINE profile registration, government ID update, and customer verification.

---

## Technology stack

| Layer | Technology |
| --- | --- |
| Language | Java 11 |
| REST framework | JAX-RS via Jersey 1.19 |
| JSON | Gson 2.10, org.json |
| Database | PostgreSQL (JDBC direct) |
| Logging | SLF4J + Log4j |
| Build | Maven 3 |
| Runtime | Servlet 3.0 container (Tomcat 8+) |

---

## Project structure

```text
java-restful-service-line-interface/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/ws/integration/
    │   │   ├── config/
    │   │   │   ├── AppConfig.java          # singleton config (env vars)
    │   │   │   └── EnvType.java            # environment enum
    │   │   ├── dao/
    │   │   │   └── LineInterfaceDao.java   # JDBC data access (all 4 operations)
    │   │   ├── endpoint/
    │   │   │   └── LineInterfaceService.java # JAX-RS resource class
    │   │   ├── model/
    │   │   │   ├── ErrorResponse.java
    │   │   │   ├── HeaderData.java
    │   │   │   ├── RequestLine.java
    │   │   │   ├── RequestRecord.java
    │   │   │   ├── ResponseLine.java
    │   │   │   ├── ResponseRecord.java
    │   │   │   └── ResponseStatus.java
    │   │   └── util/
    │   │       ├── CallWSUtils.java        # HTTP client for CUS-20 service
    │   │       ├── ConnectionDB.java       # JDBC connection factory
    │   │       └── DatabaseUtils.java      # JDBC resource cleanup helpers
    │   ├── resources/
    │   │   └── log4j.properties
    │   └── webapp/
    │       ├── META-INF/MANIFEST.MF
    │       └── WEB-INF/web.xml
    └── test/
        └── java/                           # unit/integration tests go here
```

---

## Configuration

All runtime values are read from **environment variables**. Sensible localhost defaults are used when a variable is not set.

| Variable | Description | Default |
| --- | --- | --- |
| `DB_URL` | Full JDBC connection URL | `jdbc:postgresql://localhost:5432/customer` |
| `DB_USER` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | _(empty)_ |
| `CUS20_DOMAIN` | Base URL of the external CUS-20 service | `http://localhost:3001` |

Set variables before starting the container:

```bash
export DB_URL=jdbc:postgresql://db-host:5432/customer
export DB_USER=appuser
export DB_PASSWORD=secret
export CUS20_DOMAIN=http://cus20-service:3001
```

---

## Building

Requires **Java 11+** and **Maven 3.6+**.

```bash
mvn clean package
```

The WAR is produced at `target/line-interface.war`.

---

## Deployment

Deploy `line-interface.war` to a Servlet 3.0 container such as **Apache Tomcat 8+**.

The application will be available at:

```text
http://<host>:<port>/line-interface/rest/LineInterface/<endpoint>
```

---

## API endpoints

All endpoints accept and return `application/json`.

Base path: `/rest/LineInterface`

### POST `/checkCustomerName` — Line-01

Check whether a customer exists by first name, last name, and date of birth.

#### Request body

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

#### Success response

```json
{
  "headerData": { "messageId": "MSG-001", "sentDateTime": "..." },
  "responseRecord": { "partyId": 12345, "govtId": "1234567890123" },
  "responseStatus": { "statusCode": "S", "errorCode": "200", "errorMessage": "ดำเนินการเรียบร้อย" }
}
```

---

### POST `/insertCustomerDetail` — Line-02

Register a customer's LINE profile (LINE ID, mobile, email) and log the entry.

**Required fields:** `govtId`, `mobilePhone`, `email`, `lineId`

---

### POST `/updateCustomerGovId` — Line-03

Record a government ID update request into the `line_update` table.

**Required fields:** `govtId`

---

### POST `/checkCustomerGovId` — Line-05

Verify whether a citizen ID exists, first by calling the CUS-20 external service, then falling back to the local `line_update` table.

**Required fields:** `citizenId`

#### Response record

| `statusCode` | Meaning |
| --- | --- |
| `Y` | Customer found |
| `N` | Customer not found |

---

## Response envelope

Every response shares a common structure:

```json
{
  "headerData":     { "messageId": "...", "sentDateTime": "...", "responseDateTime": "..." },
  "responseRecord": { ... },
  "responseStatus": {
    "statusCode":   "S or E",
    "errorCode":    "200 or 500",
    "errorMessage": "..."
  }
}
```

| `statusCode` | `errorCode` | Meaning |
| --- | --- | --- |
| `S` | `200` | Success |
| `E` | `500` | Error |

---

## Logging

Logs are written to both stdout and a rolling file:

```text
${catalina.home}/logs/line-interface/line-interface.log
```

Maximum file size: 5 MB, 10 backup files retained.

---

## Author

Thitipong Roongprasert
