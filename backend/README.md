# GUARDIAN Backend Service (Spring Boot 3)

The central orchestration, scan management, and security backend for the **GUARDIAN** code review and bug prediction platform.

---

## 1. Features
- **Security & Authentication**: Stateless JWT authentication with BCrypt hashing and role-based access control.
- **Persistence**: Dual-database architecture:
  - **H2 in-memory DB** for zero-configuration local development and test execution.
  - **PostgreSQL** profile for production containerized deployment.
- **ML Service Orchestration**: Reactive Spring WebClient calling the Python FastAPI ML Service (`http://localhost:8000/predict`) with timeout and error resilience.
- **Scan Management**:
  - Real-time single snippet bug risk analysis.
  - Full repository scan creation and history tracking.
  - Aggregated risk metric summaries (High/Medium/Low counts, health status).
  - Platform-wide statistics.

---

## 2. API Endpoints

### Authentication
- `POST /api/v1/auth/register`: Register a new developer account.
- `POST /api/v1/auth/login`: Authenticate and receive a Bearer JWT.

### Scans & Bug Analysis (Requires Bearer Token)
- `POST /api/v1/scans/analyze`: Analyze a single code snippet via ML service and persist findings.
- `POST /api/v1/scans`: Create a repository scan with batch snippets.
- `GET /api/v1/scans`: Retrieve user scan history.
- `GET /api/v1/scans/{id}`: Detailed scan findings report.
- `GET /api/v1/scans/{id}/summary`: Risk distribution metrics.
- `GET /api/v1/scans/stats`: Global statistics (total scans, total bugs detected, avg risk).

### Health & Monitoring
- `GET /api/v1/health`: System health and Python ML service connectivity status.
- `GET /h2-console`: Web console for in-memory database inspection (dev profile only).

---

## 3. Running Locally

### Prerequisites
- Java 17
- Maven 3.8+ (or `./mvnw`)
- Python ML Service running on `http://localhost:8000`

### Build & Run
```bash
# In backend directory:
mvn clean test
mvn spring-boot:run
```
Service starts on `http://localhost:8080`.
