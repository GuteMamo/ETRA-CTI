# ETRA-CTI: Ethiopian Threat Recognition & Analysis Platform

ETRA-CTI is an enterprise-grade application security event telemetry, threat intelligence, and vulnerability lifecycle platform. It provides continuous visibility into application-layer security events, ingests findings from industry scanners (OWASP ZAP, Burp Suite), executes deterministic and explainable detection rules, manages cryptographic evidence integrity, and governs the remediation and retesting lifecycle across organization tenants.

---

## 1. System Architecture & Tech Stack

```
                     ┌────────────────────────────────────────┐
                     │          Protected Web Apps            │
                     │  (Node.js, Python, Java, Go, etc.)     │
                     └───────────────────┬────────────────────┘
                                         │ M2M Event Ingestion
                                         │ (X-ETRA-API-KEY / Bearer)
                                         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ETRA-CTI Platform                                 │
│                                                                             │
│  ┌───────────────────────┐   ┌──────────────────┐   ┌────────────────────┐  │
│  │   Auth & Multi-Tenant │   │   Event Engine   │   │ Detection Service  │  │
│  │   - JWT / M2M Keys    │   │   - Schema Valid │   │ - Auth Burst Rule  │  │
│  │   - Tenant Isolation  │──>│   - Redaction    │──>│ - SQLi Rule        │  │
│  │   - BOLA / IDOR Guard │   │   - Persistence  │   │ - API Probing Rule │  │
│  └───────────────────────┘   └──────────────────┘   └─────────┬──────────┘  │
│                                                               │             │
│  ┌───────────────────────┐   ┌──────────────────┐             ▼             │
│  │   Remediation Hub     │   │  Evidence Store  │   ┌────────────────────┐  │
│  │   - Auto-Recommend    │<──│  - SHA-256 Hash  │<──│ Security Alerts &  │  │
│  │   - Retest Pass/Fail  │   │  - Integrity Ver │   │ Findings Hub       │  │
│  └───────────────────────┘   └──────────────────┘   └────────────────────┘  │
│                                                                             │
│  ┌───────────────────────┐   ┌──────────────────┐                           │
│  │   Scanner Importers   │   │   Risk Engine    │                           │
│  │   - OWASP ZAP (JSON)  │──>│   - 4-Weight Model   │                           │
│  │   - Burp Suite (XML)  │   │   - Score: 0 - 100   │                           │
│  └───────────────────────┘   └──────────────────┘                           │
└───────────────────────────────────────┬─────────────────────────────────────┘
                                        │
                         ┌──────────────┴──────────────┐
                         ▼                             ▼
               ┌────────────────────┐        ┌──────────────────┐
               │ PostgreSQL 16 DB   │        │ Vite + React 19  │
               │ (Flyway 17 Migr.)  │        │ Tailwind v4 UI   │
               └────────────────────┘        └──────────────────┘
```

### Core Technologies
- **Backend:** Java 21+, Spring Boot 4.1.1, Spring Security, Spring Data JPA, Hibernate ORM 7.4.5, Flyway 10.
- **Database:** PostgreSQL 16 with forward-only Flyway migrations.
- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS v4, TanStack Query v5, Lucide Icons, Sonner.
- **Security & Cryptography:** BCrypt password hashing, HMAC-SHA256 JWT tokens, SHA-256 API key digests, SHA-256 evidence file integrity hashes, strict tenant BOLA/IDOR isolation.

---

## 2. Prerequisites & Quickstart

### Prerequisites
- Java JDK 21+ (`java -version`)
- Node.js 20+ and npm (`node -v`, `npm -v`)
- Docker & Docker Compose (`docker compose version`)

The backend includes a Maven wrapper (`mvnw` / `mvnw.cmd`), so Maven does not need to be installed globally.

---

### Step 1: Start PostgreSQL

Run the following from the project root:

```bash
docker compose up -d postgres
```

Check database status:

```bash
docker compose ps
```

The database connection parameters:
- **Host:** `127.0.0.1`
- **Port:** `55432`
- **Database:** `etra_cti`
- **Username:** `etra`
- **Password:** `etra_secret`

---

### Step 2: Build & Start Backend

#### Linux / macOS:
```bash
cd backend
chmod +x mvnw
./mvnw spring-boot:run
```

#### Windows (PowerShell):
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Backend initializes on `http://localhost:8081` with context path `/`. All Flyway migrations apply automatically on startup.

Verify backend health:
```bash
curl http://localhost:8081/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

---

### Step 3: Start Frontend Dashboard

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:3000` in your browser. The Vite development server automatically forwards `/api` and `/actuator` requests to the backend on port `8081`.

---

## 3. Seeded Demo Accounts & RBAC Matrix

The platform seeds sample accounts with distinct role profiles for demonstration and testing:

| Email | Default Password | Role | Permissions |
|---|---|---|---|
| `admin@etra.et` | `Admin1234!` | `ADMIN` | Full tenant & system administration, user management, key management |
| `analyst@etra.et` | `Analyst1234!` | `ANALYST` | Event analysis, alert acknowledgment, findings investigation, evidence verification |
| `manager@etra.et` | `Manager1234!` | `SECURITY_MANAGER` | Governance, audit review, alert disposition, remediation approval |
| `pentester@etra.et`| `Pentest1234!` | `PENTESTER` | Scanner report import, retest execution, manual findings recording |
| `developer@etra.et`| `Dev123456!` | `DEVELOPER` | Remediation status updates, fix note submission, verification requests |
| `auditor@etra.et` | `Audit1234!` | `VIEWER` | Read-only audit trail and compliance reporting |

---

## 4. Application Integration Guide (Machine-to-Machine)

External applications submit real-time security events using scoped API credentials.

### Step 4.1: Register Application & Generate API Key
1. In the Web UI, navigate to **Applications** (`/applications`).
2. Select your registered Application (or create one under **Projects**).
3. Under **Generate API Key**, provide a key name (e.g. `Prod-Gateway`) and expiration window.
4. Click **Generate Credential**. Copy the raw key immediately (prefixed with `etra_live_`). The platform stores only a SHA-256 digest.

### Step 4.2: Ingestion API Contract
- **Endpoint:** `POST /api/events`
- **Authentication:** `X-ETRA-API-KEY: etra_live_<key>` or `Authorization: Bearer etra_live_<key>`
- **Content-Type:** `application/json`

#### Node.js / Express (`examples/server_integration.mjs`):
```javascript
const res = await fetch("http://localhost:8081/api/events", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "X-ETRA-API-KEY": process.env.ETRA_API_KEY
  },
  body: JSON.stringify({
    applicationId: process.env.ETRA_APP_ID,
    source: "API_GATEWAY",
    timestamp: new Date().toISOString(),
    endpoint: "/api/search",
    method: "POST",
    statusCode: 200,
    metadata: { payload: "test' OR 1=1--" }
  })
});
const data = await res.json();
console.log("Event accepted:", data.id);
```

#### Python 3 (`examples/server_integration.py`):
```python
import os, json, urllib.request

req = urllib.request.Request(
    "http://localhost:8081/api/events",
    data=json.dumps({
        "applicationId": os.environ["ETRA_APP_ID"],
        "source": "APPLICATION_LOG",
        "timestamp": "2026-10-18T00:30:00Z",
        "endpoint": "/.env",
        "method": "GET",
        "statusCode": 404
    }).encode("utf-8"),
    headers={
        "Content-Type": "application/json",
        "X-ETRA-API-KEY": os.environ["ETRA_API_KEY"]
    }
)
with urllib.request.urlopen(req) as resp:
    print("Event response:", resp.read().decode())
```

---

## 5. Vulnerability Scanner Integration

ETRA-CTI natively integrates external security scanner findings into the normalized finding model.

### 5.1 Supported Formats
1. **OWASP ZAP (JSON):** Supports standard OWASP ZAP automated scanner JSON export (both nested `site[].alerts[]` and flat `alerts[]`).
2. **Burp Suite (XML):** Supports standard Burp Suite Professional XML issue export.

### 5.2 Upload API Contract
- **Endpoint:** `POST /api/integrations/zap/{applicationId}`
- **Authentication:** Bearer JWT token (`ADMIN`, `SECURITY_MANAGER`, `PENTESTER`, `ANALYST`)
- **Content-Type:** `multipart/form-data` (`file=@zap-report.json`)
- **Deduplication:** Subsequent imports automatically detect existing findings matching `(applicationId, title, endpoint, parameter)` and increment duplicate counts without cluttering the findings register.

---

## 6. Detection Engine & Risk Model

### 6.1 Deterministic Detection Rules
ETRA-CTI operates 9 deterministic, explainable detection rules:
1. `AUTH_FAILURE_BURST`: Triggers when 5 or more authentication failures occur from the same source IP within a sliding 5-minute UTC window.
2. `ETRA-SQLI-001`: Triggers on SQL injection signatures in request path, query string, or payload metadata (`UNION SELECT`, `' OR 1=1`, `SLEEP()`, `--`).
3. `ETRA-PROBE-001`: Triggers on suspicious reconnaissance probing (`/.env`, `/wp-login.php`, `/.git`, `/actuator/`, `/phpinfo.php`).
4. `ETRA-PTRAV-001`: Path traversal attempts (`../`, `..%2f`, `/etc/passwd`).
5. `ETRA-XSS-001`: Cross-site scripting vectors (`<script>`, `javascript:`, `onerror=`).
6. `ETRA-5XX-001`: Sustained 5xx application error surges indicating potential unhandled exceptions or fault injection.
7. `ETRA-MOBILE-ENUM-001`: Mobile account harvesting and endpoint enumeration.
8. `ETRA-MOBILE-AUTH-001`: Mobile authentication abuse patterns.
9. `ETRA-AUTH-001`: General authentication brute force detection.

### 6.2 Risk Score Calculation Formula
Risk scores are calculated deterministically on a 0–100 scale:
$$\text{RiskScore} = \text{round}(0.40 \times \text{Severity} + 0.25 \times \text{Confidence} + 0.20 \times \text{Criticality} + 0.15 \times \text{Exposure})$$

Where:
- $\text{Severity}$: `LOW` = 25, `MEDIUM` = 50, `HIGH` = 75, `CRITICAL` = 100.
- $\text{Confidence}$: `LOW` = 30, `MEDIUM` = 60, `HIGH` = 90.
- $\text{Criticality}$: `LOW` = 20, `MEDIUM` = 45, `HIGH` = 70, `CRITICAL` = 95.
- $\text{Exposure}$: `INTERNAL` = 20, `PARTNER` = 50, `INTERNET` = 90.

#### Risk Bands:
- `CRITICAL`: 85.0 – 100.0
- `HIGH`: 70.0 – 84.9
- `MEDIUM`: 40.0 – 69.9
- `LOW`: 0.0 – 39.9

---

## 7. Evidence Integrity & Remediation Lifecycle

### 7.1 SHA-256 Cryptographic Evidence Integrity
- Evidence artifacts (screenshots, logs, HTTP requests) uploaded via `POST /api/findings/{id}/evidence` are hashed using SHA-256 upon ingestion.
- The `/api/evidence/{id}/verify` endpoint computes the runtime digest from the persistent filesystem store and checks it against the database digest. Any tamper event yields status `TAMPERED` (otherwise `INTACT`).

### 7.2 Remediation State Machine
```
   [OPEN] ──> [IN_PROGRESS] ──> [READY_FOR_RETEST]
                                       │
                         ┌─────────────┴─────────────┐
                         ▼                           ▼
                  Retest: PASS                Retest: FAIL
                         │                           │
                         ▼                           ▼
                    [VERIFIED]                  [REOPENED]
                         │                           │
                         └───────> (Resolved) <──────┘
```

---

## 8. Automated Verification & Testing

### Run All Unit & Integration Tests (Backend)
```powershell
cd backend
.\mvnw.cmd test
```
*Result:* 15/15 tests pass (`BUILD SUCCESS`), covering API key authentication, tenant IDOR isolation, ZAP JSON parsing, deduplication, and remediation state transitions.

### Run Frontend Production Build Check
```powershell
cd frontend
npm run build
```
*Result:* Built with zero TypeScript errors.

### Run End-to-End Automated Verification Script
With the backend running on port 8081:
```powershell
node test_all.mjs
```
*Result:* Validates all platform capabilities end-to-end against live database and API endpoints.

---

## 9. Stopping the Application

To stop background services:
- Stop the backend and frontend with `Ctrl+C` in their respective terminals.
- Stop PostgreSQL:
```bash
docker compose down
```
To also remove database volume storage:
```bash
docker compose down -v
```

---

## 10. Service URLs Summary

| Service | URL | Description |
|---|---|---|
| Frontend UI | `http://localhost:3000` | Web Dashboard |
| Backend API | `http://localhost:8081` | REST API |
| Backend Health | `http://localhost:8081/actuator/health` | Spring Boot Actuator |
| PostgreSQL | `127.0.0.1:55432` | Database instance |
