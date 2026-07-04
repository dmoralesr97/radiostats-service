# Implementation Plan: Unified Server Dashboard (US-01)

**Branch**: `001-unified-server-dashboard` | **Date**: 2026-07-04 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/001-unified-server-dashboard/spec.md`

> **Amendment 2026-07-04 (v1)**: Added `src/main/resources/db/` with Flyway migration scripts
> (schema + preloaded seed data) to replace JPA auto-DDL. See §Research §9.
>
> **Amendment 2026-07-04 (v2 — post-analysis remediation)**: Added `DispatchingPollingAdapter` (C1);
> added `RegisterServerUseCase`, `ListServersUseCase`, `RegisterServerService`, `ListServersService` (C2);
> added `application-prod.yaml` with separate Flyway `schema/` path for production seed isolation (H3);
> added SC-003 performance baseline test task (M2); clarified zero-listener = ACTIVE in data-model.md (M4).

---

## Summary

Enable a Technical Administrator to view, on a single screen, the real-time status
(ACTIVE / DOWN / UNAVAILABLE) and current listener count of up to 10 configured streaming
servers (Icecast / Shoutcast). The backend REST API polls each server live on each request;
a lightweight HTML dashboard polls the API every 60 seconds to keep the view current
without a full page reload. Database schema and initial seed data are managed via Flyway
migrations stored under `src/main/resources/db/migration/`.

---

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**:
- Spring Boot 4.1.0 (WebMVC, Data JPA, existing)
- Lombok (existing)
- Flyway Core (new — versioned schema + seed data migrations)
- openapi-generator-gradle-plugin 7.12.0 (new — API First code generation)
- Cucumber 7.22.2 + cucumber-spring (new — BDD acceptance tests)
- ArchUnit 1.4.1 (new — Clean Architecture enforcement)
- JaCoCo 0.8.13 (new — coverage reporting and gate)
- WireMock 3.x (new — stub Icecast/Shoutcast in tests)

**Storage**: H2 (existing) — schema managed by Flyway migrations in
`src/main/resources/db/migration/`; `ServerStatus` is computed at runtime and never stored.
`spring.jpa.hibernate.ddl-auto=validate` (Flyway owns DDL; JPA only validates).

**Testing**: JUnit 5 (existing), Mockito, Spring Boot Test slices (`@WebMvcTest`,
`@DataJpaTest`), Cucumber 7, ArchUnit, JaCoCo, WireMock

**Target Platform**: Web service — Spring Boot 4.x on JVM; HTML static dashboard served as
a static resource

**Project Type**: Web service / REST API + minimal HTML frontend

**Performance Goals**:
- Dashboard endpoint responds within 5 seconds for up to 10 servers (10 s timeout per
  server, polled concurrently)
- Auto-refresh every 60 seconds (client-side JavaScript `setInterval`)

**Constraints**:
- Server polling timeout: 10 seconds per server
- Maximum 10 servers displayed (first 10 by `configOrder`)
- Coverage gate: per-class line > 80%, global instruction ≥ 80% (JaCoCo)
- Generated OpenAPI model classes excluded from coverage measurement
- Flyway migration scripts MUST be additive — no destructive `ALTER` or `DROP` without
  a new versioned script

**Scale/Scope**: Single radio station; up to 10 streaming servers

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

### Principle I — Clean Architecture ✅

Package layout enforces the dependency rule:

```
domain/         → no outbound dependencies to adapter or infrastructure
application/    → depends only on domain ports
adapter/        → depends on domain; implements ports; depends on infrastructure interfaces
infrastructure/ → Spring wiring, JPA, REST client config, Flyway config
```

Flyway configuration lives in the `infrastructure/config` package — only the frameworks
layer knows about it; the domain is untouched. Enforced at test time by ArchUnit.

### Principle II — BDD Testing ✅

- Unit tests (JUnit 5 + Mockito): `ServerDashboardServiceUnitTest`,
  `IcecastPollingAdapterUnitTest`, `ShoutcastPollingAdapterUnitTest`
- Integration tests (`@WebMvcTest`, `@DataJpaTest`): `DashboardControllerIntegrationTest`,
  `StreamingServerJpaAdapterIntegrationTest`
- Functional/acceptance (Cucumber): three `.feature` files mapping each acceptance scenario
  from the spec 1-to-1.
- Tests MUST be written and FAIL before implementation.

### Principle III — SOLID / YAGNI / DRY ✅

- `ServerPollingPort` interface abstracts Icecast vs Shoutcast polling. `DispatchingPollingAdapter`
  provides OCP-compliant routing — adding a new server type requires a new adapter, not modifying the
  domain service (Open/Closed Principle).
- `RegisterServerUseCase` and `ListServersUseCase` give the controller single-responsibility injection
  points (Single Responsibility + Interface Segregation).
- No configurable refresh interval in the MVP (YAGNI).
- Schema defined once in a Flyway SQL file (DRY — no duplication between JPA entities and
  hand-written DDL; JPA validates against Flyway-managed schema).

### Principle IV — API First ✅

- OpenAPI contract at `src/main/resources/openapi/radiostats-api.yaml` created before
  any implementation tasks begin.
- Controllers implement generated delegate interfaces.

### Principle V — Quality Metrics ✅

- JaCoCo configured in `build.gradle`; `check` task depends on
  `jacocoTestCoverageVerification`.
- Exclusions: generated OpenAPI models, `*Application.java`, infrastructure config,
  Flyway migration SQL files (not Java code; not subject to coverage).

**No constitution violations.**

---

## Project Structure

### Documentation (this feature)

```text
specs/001-unified-server-dashboard/
├── plan.md              # This file
├── research.md          # Phase 0 output (§1–§9)
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── radiostats-api.yaml  # OpenAPI contract
└── tasks.md             # Phase 2 output (/speckit-tasks)
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/com/morales/radiostatsservice/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── StreamingServer.java          # Domain entity (no framework deps)
│   │   │   │   ├── ServerStatus.java             # Value object (not persisted)
│   │   │   │   ├── ServerState.java              # Enum: ACTIVE, DOWN, UNAVAILABLE
│   │   │   │   └── ServerType.java               # Enum: ICECAST, SHOUTCAST_V1, SHOUTCAST_V2
│   │   │   ├── port/
│   │   │   │   ├── in/
│   │   │   │   │   ├── GetServerDashboardUseCase.java
│   │   │   │   │   ├── RegisterServerUseCase.java     # C2 fix: controller calls port, not repo
│   │   │   │   │   └── ListServersUseCase.java        # C2 fix: controller calls port, not repo
│   │   │   │   └── out/
│   │   │   │       ├── StreamingServerRepository.java
│   │   │   │       └── ServerPollingPort.java
│   │   │   └── service/
│   │   │       ├── ServerDashboardService.java        # calls ServerPollingPort only (no adapter refs)
│   │   │       ├── RegisterServerService.java         # implements RegisterServerUseCase
│   │   │       └── ListServersService.java            # implements ListServersUseCase
│   │   ├── adapter/
│   │   │   ├── in/
│   │   │   │   └── rest/
│   │   │   │       ├── DashboardController.java
│   │   │   │       └── mapper/
│   │   │   │           └── DashboardMapper.java
│   │   │   └── out/
│   │   │       ├── persistence/
│   │   │       │   ├── StreamingServerJpaAdapter.java
│   │   │       │   ├── StreamingServerJpaRepository.java
│   │   │       │   └── entity/
│   │   │       │       └── StreamingServerJpaEntity.java
│   │   │       └── polling/
│   │   │           ├── IcecastPollingAdapter.java
│   │   │           ├── ShoutcastPollingAdapter.java
│   │   │           └── DispatchingPollingAdapter.java # C1 fix: routes poll() by ServerType; domain service calls this
│   │   ├── infrastructure/
│   │   │   └── config/
│   │   │       ├── ApplicationConfig.java
│   │   │       └── RestClientConfig.java
│   │   └── RadiostatsServiceApplication.java
│   └── resources/
│       ├── db/
│       │   └── migration/
│       │       ├── V1__create_streaming_server_table.sql   # DDL — streaming_server table
│       │       └── V2__insert_sample_servers.sql           # Seed data — 3 example servers
│       ├── openapi/
│       │   └── radiostats-api.yaml
│       ├── static/
│       │   └── dashboard.html
│       └── application.yaml                               # spring.flyway.locations=classpath:db/migration
└── test/
    ├── java/com/morales/radiostatsservice/
    │   ├── architecture/
    │   │   └── CleanArchitectureTest.java
    │   ├── domain/service/
    │   │   └── ServerDashboardServiceUnitTest.java
    │   ├── adapter/
    │   │   ├── in/rest/
    │   │   │   └── DashboardControllerIntegrationTest.java
    │   │   └── out/
    │   │       ├── persistence/
    │   │       │   └── StreamingServerJpaAdapterIntegrationTest.java
    │   │       └── polling/
    │   │           ├── IcecastPollingAdapterUnitTest.java
    │   │           └── ShoutcastPollingAdapterUnitTest.java
    │   └── functional/
    │       ├── CucumberRunnerTest.java
    │       └── steps/
    │           ├── UnifiedStatusViewSteps.java
    │           ├── VisualDifferentiationSteps.java
    │           └── AutoRefreshSteps.java
    └── resources/
        ├── db/
        │   └── migration/
        │       └── V3__insert_test_servers.sql             # Test-only seed data (wiped per @DataJpaTest)
        ├── features/
        │   ├── unified_status_view.feature
        │   ├── visual_differentiation.feature
        │   └── auto_refresh.feature
        └── application-test.yaml
```

### DB Migration Files — Content Overview

| File | Purpose | When applied |
|------|---------|--------------|
| `V1__create_streaming_server_table.sql` | Creates `streaming_server` table with index on `config_order` | App startup (prod + test) |
| `V2__insert_sample_servers.sql` | Inserts 3 representative servers (1 Icecast, 1 Shoutcast v2, 1 Shoutcast v1) | App startup (dev/local only via Spring profile; skipped in prod if `spring.flyway.locations` overridden) |
| `V3__insert_test_servers.sql` (test) | Additional servers for integration / Cucumber tests | `@DataJpaTest` and Cucumber test context only |

**Structure Decision**: Single Spring Boot project. Backend REST API with a static HTML/JS
dashboard page. Schema managed by Flyway (classpath `db/migration/`). No separate frontend
build system — YAGNI for MVP scope.

---

## Complexity Tracking

No constitution violations. This section is intentionally empty.
