# GUARDIAN Backend Service (Spring Boot 3)

The central orchestration, scan management, and security backend for the **GUARDIAN** code review and bug prediction platform.

---

## 1. Features
- **Security & Authentication**: Stateless JWT authentication with BCrypt hashing and role-based access control (`ROLE_USER`, `ROLE_ADMIN`).
- **IDOR Protection**: Strict scan ownership verification ensuring users can only read and append findings to their own scans (unauthorized access returns `404 Not Found` to prevent resource existence leakage). Admins can inspect platform-wide scans.
- **Persistence & ORM**: Dual-database architecture:
  - **H2 in-memory DB** for zero-configuration local development and test execution.
  - **PostgreSQL** profile for production containerized deployment.
  - `spring.jpa.open-in-view` disabled for clean transaction boundaries and connection pooling efficiency.
- **ML Service Orchestration**: Reactive Spring WebClient with Reactor Netty connect/read timeouts calling the Python FastAPI ML Service (`${ML_SERVICE_URL}/predict`) with decoupled transactions and status tracking (`IN_PROGRESS`, `COMPLETED`, `FAILED`).
- **Pagination & Performance**: Cursor/page-based queries on scan history with `X-Total-Count` headers and `@EntityGraph` query optimization.
- **Comprehensive Error Handling**: Structured error responses with `traceId` and RFC-compliant HTTP status codes.
- **API Documentation**: Interactive Swagger / OpenAPI UI at `/swagger-ui.html` (enabled in `dev` profile).

---

## 2. API Endpoints

### Authentication
- `POST /api/v1/auth/register`: Register a new developer account (`ROLE_USER`).
- `POST /api/v1/auth/login`: Authenticate by username or email and receive a Bearer JWT.

### Scans & Bug Analysis (Requires Bearer Token)
- `POST /api/v1/scans/analyze`: Analyze a single code snippet via ML service and persist finding. Field `isBuggy` indicates prediction.
- `POST /api/v1/scans`: Create a repository scan with batch snippets (max 100 snippets, decoupled ML transactions).
- `GET /api/v1/scans`: Retrieve user scan history. Supports pagination:
  - `page`: Page index, default `0`.
  - `size`: Page size (1–100), default `20`.
  - `all`: Boolean flag (`true` returns all platform scans for `ROLE_ADMIN`; regular users receive `403 Forbidden`).
  - Response header `X-Total-Count` contains total count.
- `GET /api/v1/scans/{id}`: Detailed scan findings report (scoped to owner or `ROLE_ADMIN`).
- `GET /api/v1/scans/{id}/summary`: Risk distribution metrics (High/Medium/Low counts, health status).
- `GET /api/v1/scans/stats`: Metrics dashboard (returns platform-wide metrics for admins, user-scoped metrics for standard users).

### Health & Monitoring
- `GET /api/v1/health`: System health and Python ML service connectivity status.
- `GET /h2-console`: Web console for in-memory database inspection (`dev` profile only).
- `GET /swagger-ui.html`: OpenAPI Interactive API documentation (`dev` profile only).

---

## 3. HTTP Status Codes & Error Responses

All error responses share the consistent JSON structure:
```json
{
  "timestamp": "2026-10-12T23:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "traceId": "c4b123d4-..."
}
```

| HTTP Status | Exception / Condition | Response Shape |
|---|---|---|
| **400 Bad Request** | Validation failed (`MethodArgumentNotValidException`), malformed body (`HttpMessageNotReadableException`), type mismatch (`MethodArgumentTypeMismatchException`, e.g. `/scans/abc`) | Contains `message` or `fieldErrors` map |
| **401 Unauthorized** | Missing, invalid, or expired JWT (`AuthenticationEntryPoint`), bad credentials (`BadCredentialsException`) | `{"status": 401, "error": "Unauthorized"}` |
| **403 Forbidden** | Insufficient role permissions (`AccessDeniedException`, e.g. non-admin calling `all=true`) | `{"status": 403, "error": "Forbidden"}` |
| **404 Not Found** | Resource not found or IDOR protection hiding another user's scan (`ResourceNotFoundException`), unmapped routes (`NoResourceFoundException`) | `{"status": 404, "error": "Not Found"}` |
| **405 Method Not Allowed** | Unsupported HTTP verb (`HttpRequestMethodNotSupportedException`) | `{"status": 405, "error": "Method Not Allowed"}` |
| **409 Conflict** | Duplicate username or email on registration (`ConflictException`), DB unique constraint violation (`DataIntegrityViolationException`) | `{"status": 409, "error": "Conflict"}` |
| **502 Bad Gateway** | Python ML service returned an internal 5xx server error (`MlServiceException`) | `{"status": 502, "error": "Bad Gateway"}` |
| **503 Service Unavailable** | Python ML service connection refused, offline, or timed out (`MlServiceUnavailableException`) | Sanitized error body without internal ports or localhost URLs |
| **500 Internal Server Error** | Unexpected server errors (`Exception`) | Sanitized `"Unexpected error"` message with logged stack trace and `traceId` |

---

## 4. Environment Variables & Configuration

| Variable | Description | Default (Dev) | Production |
|---|---|---|---|
| `SERVER_PORT` | HTTP port for Spring Boot backend | `8081` | `${SERVER_PORT:8081}` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`dev` or `prod`) | `dev` | `prod` |
| `JWT_SECRET` | 256-bit Base64-encoded HMAC-SHA256 secret key | Hardcoded development key | **Required** (Startup fails if absent) |
| `ML_SERVICE_URL` | Base URL for the Python FastAPI ML microservice | `http://localhost:8000` | E.g. `http://ml-service:8000` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed origins | `http://localhost:3000,http://localhost:5173` | Production domain origins |
| `SPRING_DATASOURCE_URL` | JDBC connection URL for PostgreSQL | N/A (H2 in-memory) | `jdbc:postgresql://localhost:5432/guardiandb` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `sa` (H2) | `guardian` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | empty (H2) | `guardian_secret` |
| `ADMIN_SEED_PASSWORD` | Default password for seeded `admin` account in `dev` profile | `Admin@12345` | N/A (Disabled in `prod`) |

---

## 5. Admin Seeding (Dev Profile)

In the `dev` profile, a default administrator account is seeded on startup via `AdminUserSeeder`:
- **Username**: `admin`
- **Email**: `admin@guardian.io`
- **Password**: Property `guardian.seed.admin-password` (default `Admin@12345`)
- **Role**: `ROLE_ADMIN`

In the `prod` profile, automatic seeding is disabled. User registration via `/api/v1/auth/register` always assigns `ROLE_USER`.

---

## 6. Testing with Postman

A comprehensive Postman collection is included in the project root:
[`GUARDIAN_Postman_Collection.json`](../GUARDIAN_Postman_Collection.json)

### Importing and Running:
1. Open Postman, click **Import**, and select `GUARDIAN_Postman_Collection.json`.
2. Configure the `baseUrl` collection variable to `http://localhost:8081`.
3. Test the workflow:
   - **Authentication**:
     - Register user: `POST /api/v1/auth/register`
     - Login with username or email: `POST /api/v1/auth/login` (auto-saves Bearer token to collection environment)
     - Login as default admin: `POST /api/v1/auth/login` (`admin` / `Admin@12345`)
   - **Scan Operations**:
     - Real-time snippet analysis: `POST /api/v1/scans/analyze` (verifies `isBuggy` in response)
     - Batch repository scan: `POST /api/v1/scans` (validates `totalFilesScanned` and findings)
     - Paginated scans: `GET /api/v1/scans?page=0&size=10` (inspect `X-Total-Count` response header)
     - Admin platform scans: `GET /api/v1/scans?all=true`
   - **Security & IDOR Isolation**:
     - Test unauthenticated access (`401 Unauthorized`)
     - Cross-user scan access (`404 Not Found`)
     - Duplicate registration (`409 Conflict`)
     - Type mismatch (`400 Bad Request` on `/api/v1/scans/invalid`)

---

## 7. Running Locally

### Prerequisites
- Java 17
- Maven 3.8+ (or `./mvnw`)

### Build & Run
```bash
# Clean and run all unit & integration tests
./mvnw clean test

# Run the backend service locally (dev profile with H2)
./mvnw spring-boot:run
```
The service will start on `http://localhost:8081`.
Swagger UI is available at `http://localhost:8081/swagger-ui.html`.
