# Remote Program Control (Spring WS REST)

Spring Boot application for remote control of programs on registered devices. The solution provides both a web UI (Thymeleaf) and a REST API, with authentication/authorization via Keycloak (OIDC/OAuth2).

## Contents

- [What the project does](#what-the-project-does)
- [Technology stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Quick start (local)](#quick-start-local)
- [Development login](#development-login)
- [Key API endpoints](#key-api-endpoints)
- [Configuration](#configuration)
- [Build, test, and run](#build-test-and-run)
- [Troubleshooting](#troubleshooting)
- [Project structure](#project-structure)

## What the project does

- Retrieves and manages devices per user.
- Creates, updates, deletes, and syncs programs on devices.
- Starts and stops programs via API.
- Sends real-time updates via SSE.
- Provides a web UI for dashboard and device/program editing.

## Technology stack

- Java 24
- Spring Boot 3.5.x
- Spring Web, Spring Security, OAuth2 Client/Resource Server
- Spring Data JPA
- Thymeleaf
- H2 (local/dev)
- PostgreSQL (prod)
- Keycloak (OIDC/OAuth2)

## Prerequisites

- JDK 24 installed and available in `PATH`
- Maven Wrapper is used (`mvnw`/`mvnw.cmd`), so a separate Maven installation is optional
- Keycloak 26.4.7 installed at:
    - `C:\Keycloak\keycloak-26.4.7`

> Note: `kc-dev.ps1` uses the hardcoded Keycloak path above.

## Quick start (local)

### 1. Start Keycloak with the dev realm

From the project root:

```powershell
./kc-dev.ps1
```

Alternatively, in VS Code you can run the `Start Keycloak` task.

The script imports:

- `infrastructure/keycloak/remote-control-realm.json`
- `infrastructure/keycloak/remote-control-users-0.json`

### 2. Start the application

```powershell
./mvnw.cmd spring-boot:run
```

By default, the app starts on `http://localhost:8090`.

### 3. Open the web UI

- `http://localhost:8090/`

## Development login

Default users from the Keycloak import:

- `admin` / `admin` (roles: `ADMIN`, `USER`)
- `user` / `user` (role: `USER`)

## Key API endpoints

Base path for API:

- `/remote-program-control/api/v1`

Examples:

- `GET /remote-program-control/api/v1/devices`
- `GET /remote-program-control/api/v1/devices/{id}`
- `POST /remote-program-control/api/v1/devices`
- `PUT /remote-program-control/api/v1/devices/{id}`
- `DELETE /remote-program-control/api/v1/devices/{id}`
- `POST /remote-program-control/api/v1/devices/{id}/programs`
- `PUT /remote-program-control/api/v1/devices/{id}/programs/sync`
- `GET /remote-program-control/api/v1/devices/{id}/events` (SSE)
- `GET /remote-program-control/api/v1/programs/{id}`
- `PUT /remote-program-control/api/v1/programs/{id}`
- `PUT /remote-program-control/api/v1/programs/{id}/status`
- `POST /remote-program-control/api/v1/programs/{id}/start`
- `POST /remote-program-control/api/v1/programs/{id}/stop`
- `DELETE /remote-program-control/api/v1/programs/{id}`
- `GET /remote-program-control/api/v1/users/search?q=<text>&limit=10`

Dashboard SSE:

- `GET /api/v1/dashboard/events`

All endpoints require authentication.

## Configuration

### Local/dev (`src/main/resources/application.properties`)

- Port: `8090`
- DB: H2 in-memory
- JWT issuer: `http://localhost:8080/realms/remote-control`
- OAuth2 client-id: `remote-control-api`

### Prod (`src/main/resources/application-prod.properties`)

Use environment variables (example):

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `DB_SCHEMA`

See also:

- `prod.env.example`
- `prod.env`

Enable the prod profile:

```powershell
./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

## Build, test, and run

Run tests:

```powershell
./mvnw.cmd test
```

Build artifact:

```powershell
./mvnw.cmd clean package
```

Run jar (after build):

```powershell
java -jar target/remote-program-control-spring-ws-rest-0.0.1-SNAPSHOT.jar
```

## Troubleshooting

- **401/403 from API**:
    - Verify that Keycloak is running on port `8080`.
    - Verify issuer-uri in `application.properties`.
- **Login does not work in the UI**:
    - Check that client `remote-control-api` exists in Keycloak.
    - Check redirect URI in the realm import.
- **Database-related errors in prod**:
    - Verify `DB_URL`, `DB_USER`, `DB_PASSWORD`, and network connectivity.

## Project structure

```text
src/main/java/.../controller/api   REST controllers
src/main/java/.../controller/web   Thymeleaf pages
src/main/java/.../service          Business logic
src/main/java/.../security         Security/OAuth2/JWT
src/main/resources/templates       HTML-templates
src/main/resources/static          CSS/JS/i18n
infrastructure/keycloak            Realm and user import
```

## License

No license has been defined yet. Add a `LICENSE` file if needed.
