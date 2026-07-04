# radiostats-service

REST service that provides real-time status and listener counts for Icecast and Shoutcast streaming servers through a unified dashboard.

## Overview

Each request to the dashboard endpoint polls the configured streaming servers live (no cached state) and returns their current state and listener count. Up to 10 servers are shown, ordered by configuration sequence.

Server states:

| State | Meaning |
|---|---|
| `ACTIVE` | Server is reachable and streaming |
| `DOWN` | Server is reachable but not streaming |
| `UNAVAILABLE` | Connection timed out or response unparseable |

## Tech Stack

- **Java 21** · **Spring Boot 4.1.0**
- **Spring Data JPA** + **H2** (in-memory) + **Flyway** migrations
- **OpenAPI Generator** (Spring delegate pattern)
- **JUnit 5** · **Cucumber 7** · **WireMock 3** · **ArchUnit**
- **Jacoco** (≥ 80 % instruction coverage enforced on `check`)

## Architecture

Hexagonal (Clean Architecture) with three layers:

```
adapter/in/rest          ← DashboardController (REST)
domain/service           ← ServerDashboardService, RegisterServerService, ListServersService
adapter/out/persistence  ← StreamingServerJpaAdapter (H2 / Flyway)
adapter/out/polling      ← IcecastPollingAdapter, ShoutcastPollingAdapter, DispatchingPollingAdapter
```

Domain has no dependencies on Spring or infrastructure. Port interfaces (`GetServerDashboardUseCase`, `StreamingServerRepository`, `ServerPollingPort`) decouple each layer.

## Prerequisites

- Java 21
- Gradle wrapper included (`./gradlew`)

## Running

```bash
./gradlew bootRun
```

The application starts on `http://localhost:8080`. The H2 console is available at `http://localhost:8080/h2-console` (dev only).

## API

Base path: `/api/v1`

### GET /servers/dashboard

Returns live status of up to 10 configured servers ordered by `configOrder`.

```json
{
  "servers": [
    {
      "serverId": "550e8400-e29b-41d4-a716-446655440000",
      "serverName": "Main Stream",
      "serverType": "ICECAST",
      "state": "ACTIVE",
      "listenerCount": 128,
      "lastCheckedAt": "2026-07-04T10:00:00Z"
    }
  ],
  "totalServers": 1,
  "refreshedAt": "2026-07-04T10:00:00Z"
}
```

### GET /servers

Lists all configured servers (no live polling).

### POST /servers

Registers a new streaming server.

```json
{
  "name": "Main Icecast Stream",
  "serverType": "ICECAST",
  "host": "stream.example.com",
  "port": 8000
}
```

`serverType` values: `ICECAST`, `SHOUTCAST_V2`, `SHOUTCAST_V1`

## Database Migrations

Managed by Flyway under `src/main/resources/db/migration/`:

| Migration | Description |
|---|---|
| `V1` | Creates `streaming_server` table |
| `V2` | Dev seed data (excluded in prod via `application-prod.yaml`) |

Test migrations (`src/test/resources/db/migration/`):

| Migration | Description |
|---|---|
| `V3` | Inserts 12 test servers for integration tests |

## Testing

```bash
# Run all tests + coverage report
./gradlew test jacocoTestReport

# Run only tests (coverage verification included via check)
./gradlew check
```

Test reports:
- HTML: `build/reports/tests/test/index.html`
- Coverage: `build/reports/jacoco/test/html/index.html`

Test suite (44 tests):

| Layer | Tests |
|---|---|
| Unit — domain services | `ServerDashboardServiceUnitTest`, `RegisterServerServiceUnitTest`, `ListServersServiceUnitTest` |
| Unit — polling adapters | `IcecastPollingAdapterUnitTest`, `ShoutcastPollingAdapterUnitTest` (WireMock) |
| Integration — REST | `DashboardControllerIntegrationTest` (MockMvc), `DashboardPerformanceTest` |
| Integration — persistence | `StreamingServerJpaAdapterIntegrationTest` |
| Functional | `CucumberRunnerTest` — 9 scenarios across `auto_refresh`, `unified_status_view`, `visual_differentiation` features |
| Architecture | `CleanArchitectureTest` (ArchUnit — enforces layer dependency rules) |

## Project Structure

```
src/
├── main/
│   ├── java/com/morales/radiostatsservice/
│   │   ├── adapter/
│   │   │   ├── in/rest/          # DashboardController + DashboardMapper
│   │   │   └── out/
│   │   │       ├── persistence/  # StreamingServerJpaAdapter + JPA entity
│   │   │       └── polling/      # IcecastPollingAdapter, ShoutcastPollingAdapter, DispatchingPollingAdapter
│   │   ├── domain/
│   │   │   ├── model/            # StreamingServer, ServerStatus, ServerState, ServerType
│   │   │   ├── port/in/          # Use-case interfaces
│   │   │   ├── port/out/         # Repository + polling port interfaces
│   │   │   └── service/          # Domain service implementations
│   │   └── infrastructure/config/ # ApplicationConfig, RestClientConfig
│   └── resources/
│       ├── application.yaml
│       ├── application-prod.yaml
│       ├── db/migration/         # V1, V2 Flyway scripts
│       └── openapi/              # radiostats-api.yaml (OpenAPI 3.1)
└── test/
    ├── java/com/morales/radiostatsservice/
    │   ├── adapter/              # Controller + persistence + polling tests
    │   ├── architecture/         # ArchUnit clean architecture enforcement
    │   ├── domain/service/       # Domain unit tests
    │   └── functional/           # Cucumber runner, Spring config, step definitions
    └── resources/
        ├── application-test.yaml
        ├── db/migration/V3       # Test seed data
        └── features/             # Cucumber feature files
```

## Polling Behavior

- **Connect timeout**: 10 s · **Read timeout**: 10 s (configured in `RestClientConfig`)
- Icecast: polls `GET /status-json.xsl`, parses `icestats.source` for listener count
- Shoutcast V2: polls `GET /statistics?output=json`, reads `currentlisteners`
- Shoutcast V1: polls `GET /7.html`, reads comma-separated listener field
- Any connection error or timeout → `UNAVAILABLE`
- Successful response with no active source → `DOWN`
