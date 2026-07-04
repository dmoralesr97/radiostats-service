---
description: "Task list for Unified Server Dashboard (US-01) — v2 post-analysis remediation"
---

# Tasks: Unified Server Dashboard (US-01)

**Input**: Design documents from `specs/001-unified-server-dashboard/`
**Prerequisites**: plan.md ✅ spec.md ✅ research.md ✅ data-model.md ✅ contracts/ ✅

**Tests**: BDD tests are REQUIRED by the constitution (Principle II). Tests MUST be written
and confirmed to FAIL before the corresponding implementation tasks begin (red-green-refactor).

**Changelog from v1**:
- C1 fix: added `DispatchingPollingAdapter` (T023); `ServerDashboardService` now calls only `ServerPollingPort`
- C2 fix: added `RegisterServerUseCase` (T015), `ListServersUseCase` (T016), `RegisterServerService` (T024), `ListServersService` (T025); controller calls use-case ports
- H2 fix: corrected T006 Flyway duplicate location
- H3 fix: added T007b for production Flyway location isolation
- M1 fix: updated T050 timing assertion (assert `refreshedAt` within ±2 s of call time)
- M2 fix: added T056 SC-003 performance assertion task
- M4 fix: zero-listener Shoutcast rule clarified in T036 description

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1 / US2 / US3)
- Exact file paths included in every description

## Path Conventions

Base package: `src/main/java/com/morales/radiostatsservice`
Test package:  `src/test/java/com/morales/radiostatsservice`
Abbreviation:  `…` = `com/morales/radiostatsservice`

---

## Phase 1: Setup (Project Initialization)

**Purpose**: Add all new dependencies, configure Gradle plugins, create Flyway config,
copy OpenAPI contract.

- [X] T001 Add Gradle dependencies to `build.gradle`: `org.flywaydb:flyway-core` (implementation), `org.openapi.generator:openapi-generator-gradle-plugin:7.12.0` (plugin), `io.cucumber:cucumber-java:7.22.2`, `io.cucumber:cucumber-spring:7.22.2`, `io.cucumber:cucumber-junit-platform-engine:7.22.2`, `org.junit.platform:junit-platform-suite:1.11.4`, `com.tngtech.archunit:archunit-junit5:1.4.1`, `com.github.tomakehurst:wiremock-standalone:3.9.2` (all testImplementation)
- [X] T002 Configure `openApiGenerate` task in `build.gradle`: generator=spring, useSpringBoot3=true, delegatePattern=true, inputSpec=src/main/resources/openapi/radiostats-api.yaml, outputDir=build/generated/openapi; add `build/generated/openapi/src/main/java` to `compileJava` source set
- [X] T003 [P] Configure JaCoCo in `build.gradle`: `jacoco { toolVersion = "0.8.13" }`, `jacocoTestReport` (HTML+XML reports), `jacocoTestCoverageVerification` (global instruction ≥ 0.80; per-class line > 0.80; exclude `**/generated/**`, `**/*Application.java`, `**/infrastructure/config/**`); wire `check.dependsOn jacocoTestCoverageVerification`
- [X] T004 [P] Copy OpenAPI contract from `specs/001-unified-server-dashboard/contracts/radiostats-api.yaml` to `src/main/resources/openapi/radiostats-api.yaml`
- [X] T005 [P] Configure `src/main/resources/application.yaml`: H2 datasource (`url: jdbc:h2:mem:radiostatsdb`), `spring.jpa.hibernate.ddl-auto: validate`, `spring.flyway.enabled: true`, `spring.flyway.locations: classpath:db/migration`
- [X] T006 [P] Create `src/test/resources/application-test.yaml`: H2 in-memory datasource (url: `jdbc:h2:mem:testdb` — separate name for isolation), `spring.flyway.enabled: true`, `spring.flyway.locations: classpath:db/migration` (V3 is discovered automatically because test resources are on the test classpath alongside main resources)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Flyway SQL migrations, all domain ports and services, JPA persistence stack,
dispatcher adapter, and ArchUnit enforcement.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### 2a — Flyway Migrations

- [X] T007 Create Flyway DDL migration `src/main/resources/db/migration/V1__create_streaming_server_table.sql`:
  ```sql
  CREATE TABLE streaming_server (
      id              UUID         DEFAULT RANDOM_UUID() PRIMARY KEY,
      name            VARCHAR(255) NOT NULL,
      server_type     VARCHAR(20)  NOT NULL,
      host            VARCHAR(255) NOT NULL,
      port            INTEGER      NOT NULL CHECK (port BETWEEN 1 AND 65535),
      config_order    INTEGER      NOT NULL,
      credentials_ref VARCHAR(255)
  );
  CREATE INDEX idx_streaming_server_config_order ON streaming_server (config_order ASC);
  ```
- [X] T008 [P] Create Flyway dev-seed migration `src/main/resources/db/migration/V2__insert_sample_servers.sql`: INSERT 3 rows — ICECAST (stream.example.com:8000, order 1), SHOUTCAST_V2 (backup.example.com:8001, order 2), SHOUTCAST_V1 (legacy.example.com:8000, order 3); all credentials_ref NULL. Add a comment at the top: `-- DEV SEED: excluded from production via spring.flyway.locations override (see application-prod.yaml)`
- [X] T008b [P] Create `src/main/resources/application-prod.yaml` (or document in `application.yaml` under profile `prod`): set `spring.flyway.locations: classpath:db/migration/schema` so Flyway only applies V1 in production. Create empty directory placeholder `src/main/resources/db/migration/schema/` and move a copy of V1 there. Add a README comment explaining the split.
- [X] T009 [P] Create test-only Flyway seed migration `src/test/resources/db/migration/V3__insert_test_servers.sql`: INSERT 12 rows with deterministic UUIDs (aaaaaaaa-0000-0000-0000-00000000000N, N=01–12), names Test-Server-01 through Test-Server-12, types cycling ICECAST/SHOUTCAST_V2/SHOUTCAST_V1, configOrder 1–12, credentials_ref NULL — enables testing the "first 10 by configOrder" business rule

### 2b — Domain Model

- [X] T010 Create enum `ServerState` (ACTIVE, DOWN, UNAVAILABLE) in `src/main/java/…/domain/model/ServerState.java`; zero framework imports
- [X] T011 [P] Create enum `ServerType` (ICECAST, SHOUTCAST_V2, SHOUTCAST_V1) in `src/main/java/…/domain/model/ServerType.java`; zero framework imports
- [X] T012 Create domain entity `StreamingServer` (fields: id UUID, name String, serverType ServerType, host String, port int, configOrder int, credentialsRef String nullable) in `src/main/java/…/domain/model/StreamingServer.java`; Lombok `@Value` or Java record; zero framework imports
- [X] T013 Create value object `ServerStatus` (fields: serverId UUID, serverName String, serverType ServerType, state ServerState, listenerCount int, lastCheckedAt Instant) in `src/main/java/…/domain/model/ServerStatus.java`; zero framework imports

### 2c — Domain Ports (Input + Output)

- [X] T014 Create input port `GetServerDashboardUseCase` (method: `List<ServerStatus> execute()`) in `src/main/java/…/domain/port/in/GetServerDashboardUseCase.java`; zero framework imports
- [X] T015 [P] Create input port `RegisterServerUseCase` (method: `StreamingServer register(StreamingServer server)`) in `src/main/java/…/domain/port/in/RegisterServerUseCase.java`; zero framework imports — resolves C2 (controller was directly calling JPA repo; now calls this port)
- [X] T016 [P] Create input port `ListServersUseCase` (method: `List<StreamingServer> listAll()`) in `src/main/java/…/domain/port/in/ListServersUseCase.java`; zero framework imports — resolves C2 (controller was directly calling JPA repo; now calls this port)
- [X] T017 [P] Create output port `StreamingServerRepository` (methods: `List<StreamingServer> findFirstTenByConfigOrder()`, `StreamingServer save(StreamingServer server)`, `List<StreamingServer> findAll()`) in `src/main/java/…/domain/port/out/StreamingServerRepository.java`; zero framework imports
- [X] T018 [P] Create output port `ServerPollingPort` (method: `ServerStatus poll(StreamingServer server)`) in `src/main/java/…/domain/port/out/ServerPollingPort.java`; zero framework imports

### 2d — Domain Services (Use Case Implementations)

- [X] T019 Create `ServerDashboardService implements GetServerDashboardUseCase`: constructor-injected with `StreamingServerRepository` and `ServerPollingPort` (a single port — the dispatcher wired externally); `execute()` calls `repository.findFirstTenByConfigOrder()`, then calls `pollingPort.poll(server)` for each server; collects `List<ServerStatus>`; MUST NOT reference any adapter-layer class directly in `src/main/java/…/domain/service/ServerDashboardService.java`
- [X] T020 [P] Create `RegisterServerService implements RegisterServerUseCase`: constructor-injected with `StreamingServerRepository`; `register()` validates port range (1–65535) and name not blank, then calls `repository.save(server)` in `src/main/java/…/domain/service/RegisterServerService.java`
- [X] T021 [P] Create `ListServersService implements ListServersUseCase`: constructor-injected with `StreamingServerRepository`; `listAll()` calls `repository.findAll()` in `src/main/java/…/domain/service/ListServersService.java`

### 2e — Adapter Layer: Persistence

- [X] T022 Create JPA entity `StreamingServerJpaEntity` with `@Entity`, `@Table(name="streaming_server")`, columns matching V1 DDL exactly, `@Index(columnList="config_order")` in `src/main/java/…/adapter/out/persistence/entity/StreamingServerJpaEntity.java`
- [X] T023 Create Spring Data interface `StreamingServerJpaRepository extends JpaRepository<StreamingServerJpaEntity, UUID>` with query method `findTop10ByOrderByConfigOrderAsc()` in `src/main/java/…/adapter/out/persistence/StreamingServerJpaRepository.java`
- [X] T024 Create `StreamingServerJpaAdapter implements StreamingServerRepository` (implements all three methods: `findFirstTenByConfigOrder`, `save`, `findAll`); maps JPA entity ↔ domain model; `@Component` in `src/main/java/…/adapter/out/persistence/StreamingServerJpaAdapter.java`

### 2f — Adapter Layer: Polling + Dispatcher

- [X] T025 [P] Create `RestClientConfig` (`@Configuration`) building a `RestClient` bean with 10 s connect and read timeout in `src/main/java/…/infrastructure/config/RestClientConfig.java`
- [X] T026 Create `DispatchingPollingAdapter implements ServerPollingPort` — resolves C1 (domain service was referencing concrete adapters): constructor-injected with `Map<ServerType, ServerPollingPort>` named `pollingAdapters`; `poll(server)` delegates to `pollingAdapters.get(server.serverType()).poll(server)`; throws `IllegalStateException` if no adapter found for type in `src/main/java/…/adapter/out/polling/DispatchingPollingAdapter.java`
- [X] T027 Create `ApplicationConfig` (`@Configuration`): produces `IcecastPollingAdapter` bean, `ShoutcastPollingAdapter` bean, `DispatchingPollingAdapter` bean wired with `Map.of(ICECAST, icecastAdapter, SHOUTCAST_V2, shoutcastAdapter, SHOUTCAST_V1, shoutcastAdapter)`; produces `ServerDashboardService` wired with `StreamingServerJpaAdapter` and `DispatchingPollingAdapter`; produces `RegisterServerService` and `ListServersService` wired with `StreamingServerJpaAdapter` in `src/main/java/…/infrastructure/config/ApplicationConfig.java`

### 2g — Architecture Enforcement

- [X] T028 Create `CleanArchitectureTest` (JUnit 5 + ArchUnit) enforcing: `domain` packages have zero Spring/Jakarta imports; `domain` does not depend on `adapter` or `infrastructure`; `adapter` packages do not cross-reference each other (only via `domain` ports); use-case services depend only on domain ports, not on concrete adapter classes in `src/test/java/…/architecture/CleanArchitectureTest.java`

**Checkpoint**: `./gradlew test --tests "*.CleanArchitectureTest"` must pass. Flyway V1 DDL
must apply cleanly to an in-memory H2 database (`./gradlew bootRun` starts without errors).

---

## Phase 3: User Story 1 - Unified Status View (Priority: P1) 🎯 MVP

**Goal**: Administrator opens the dashboard and sees status + listener count of all
configured servers (up to 10) on one screen without extra navigation.

**Independent Test**: `GET /api/v1/servers/dashboard` returns ≤ 10 `ServerStatusDto` entries
with state and listenerCount populated; `/dashboard` renders all cards on first load.

### Tests for User Story 1 (WRITE FIRST — must FAIL before implementation)

- [X] T029 [US1] Write Cucumber feature file for US1 (3 scenarios: 1–10 servers visible, >10 servers capped at 10, 0 servers shows empty state) in `src/test/resources/features/unified_status_view.feature`; use Given/When/Then phrasing verbatim from spec acceptance scenarios
- [X] T030 [P] [US1] Write failing unit test `ServerDashboardServiceUnitTest`: mock `StreamingServerRepository` returning 12 items, verify service caps at 10; mock single `ServerPollingPort` (the dispatcher), verify `poll()` called once per server and results collected in `src/test/java/…/domain/service/ServerDashboardServiceUnitTest.java`
- [X] T031 [P] [US1] Write failing integration test `DashboardControllerIntegrationTest` (`@WebMvcTest`): mock `GetServerDashboardUseCase`, mock `RegisterServerUseCase`, mock `ListServersUseCase`; verify `GET /api/v1/servers/dashboard` returns HTTP 200 with JSON matching `DashboardResponse` schema (servers array, totalServers, refreshedAt) in `src/test/java/…/adapter/in/rest/DashboardControllerIntegrationTest.java`
- [X] T032 [P] [US1] Write failing integration test `StreamingServerJpaAdapterIntegrationTest` (`@DataJpaTest`): Flyway V1+V3 applied automatically via test classpath; verify `findFirstTenByConfigOrder()` returns exactly 10 rows from 12 V3-inserted rows, ordered by config_order ASC in `src/test/java/…/adapter/out/persistence/StreamingServerJpaAdapterIntegrationTest.java`
- [X] T033 [P] [US1] Write failing unit test `IcecastPollingAdapterUnitTest` (WireMock): stub `GET /status-json.xsl` returning valid JSON → verify ACTIVE + listener sum; stub 10 s delay → verify UNAVAILABLE; stub empty `source` array → verify DOWN in `src/test/java/…/adapter/out/polling/IcecastPollingAdapterUnitTest.java`
- [X] T034 [P] [US1] Write failing unit test `ShoutcastPollingAdapterUnitTest` (WireMock): stub `GET /statistics?output=json` (SHOUTCAST_V2) with `currentlisteners=55` → verify ACTIVE + count=55; stub `GET /7.html` (SHOUTCAST_V1) with `42,100,200,...` → verify ACTIVE + count=42; stub timeout → verify UNAVAILABLE; stub `currentlisteners=0` with valid stream → verify ACTIVE (zero listeners ≠ DOWN) in `src/test/java/…/adapter/out/polling/ShoutcastPollingAdapterUnitTest.java`
- [X] T035 [US1] Create `CucumberRunnerTest` (`@Suite @SelectClasspathResource("features")`) and blank `UnifiedStatusViewSteps` class (annotated `@SpringBootTest + @ActiveProfiles("test")`) in `src/test/java/…/functional/CucumberRunnerTest.java` and `src/test/java/…/functional/steps/UnifiedStatusViewSteps.java`

### Implementation for User Story 1

- [X] T036 [P] [US1] Implement `IcecastPollingAdapter implements ServerPollingPort`: `poll()` calls `GET http://<host>:<port>/status-json.xsl` via injected `RestClient`; parse `icestats.source[*].listeners`, sum → ACTIVE; empty/null source → DOWN; timeout or parse error → UNAVAILABLE in `src/main/java/…/adapter/out/polling/IcecastPollingAdapter.java`
- [X] T037 [P] [US1] Implement `ShoutcastPollingAdapter implements ServerPollingPort`: branch on `server.serverType()`; SHOUTCAST_V2 → `GET /statistics?output=json`, read `currentlisteners` (zero listeners with valid response = ACTIVE, not DOWN — active stream with no audience); SHOUTCAST_V1 → `GET /7.html`, parse CSV field[0] as listener count; timeout → UNAVAILABLE in `src/main/java/…/adapter/out/polling/ShoutcastPollingAdapter.java`
- [X] T038 [P] [US1] Implement `DashboardMapper`: convert `List<ServerStatus>` → generated `DashboardResponse` DTO; set `refreshedAt = Instant.now()`, `totalServers = servers.size()` in `src/main/java/…/adapter/in/rest/mapper/DashboardMapper.java`
- [X] T039 [US1] Implement `DashboardController` (implements generated `DashboardApiDelegate`): constructor-injected with `GetServerDashboardUseCase`, `RegisterServerUseCase`, `ListServersUseCase`, and `DashboardMapper`; `getServerDashboard()` → use case → mapper; `createServer()` → `RegisterServerUseCase.register()`; `listServers()` → `ListServersUseCase.listAll()` — MUST NOT reference any repository or JPA class directly in `src/main/java/…/adapter/in/rest/DashboardController.java`
- [X] T040 [US1] Implement Cucumber step definitions in `UnifiedStatusViewSteps`: use WireMock to stub Icecast `/status-json.xsl` for each seed server; use RestAssured to call `GET /api/v1/servers/dashboard`; assert `servers.size() ≤ 10`, `state` and `listenerCount` match stub data; assert empty-state scenario returns `servers` array of length 0 in `src/test/java/…/functional/steps/UnifiedStatusViewSteps.java`
- [X] T041 [US1] Create `src/main/resources/static/dashboard.html`: on page load `fetch('/api/v1/servers/dashboard')`, render a card per server (name, state label as text, listenerCount, lastCheckedAt); show empty-state paragraph when `servers.length === 0`; no auto-refresh yet (added in US3)

**Checkpoint**: `./gradlew test` passes including Cucumber US1 scenarios. `GET /api/v1/servers/dashboard` returns data; `/dashboard` renders cards from V2 seed. ArchUnit test still passes.

---

## Phase 4: User Story 2 - Visual Differentiation of Server State (Priority: P2)

**Goal**: Downed and unavailable servers are visually distinct from active servers at a glance.

**Independent Test**: `/dashboard` with ACTIVE, DOWN, and UNAVAILABLE servers shows three
distinct CSS badge colours without requiring reading of any text label.

### Tests for User Story 2 (WRITE FIRST — must FAIL before implementation)

- [X] T042 [P] [US2] Write Cucumber feature file for US2 (3 scenarios: DOWN indicator distinct from ACTIVE; ACTIVE indicator distinct from DOWN; UNAVAILABLE distinct from both) in `src/test/resources/features/visual_differentiation.feature`
- [X] T043 [US2] Write step definitions `VisualDifferentiationSteps`: stub WireMock to return all three server states; call `GET /api/v1/servers/dashboard`; assert JSON `state` field is exactly `"ACTIVE"`, `"DOWN"`, or `"UNAVAILABLE"` (string values the frontend uses for CSS class mapping) in `src/test/java/…/functional/steps/VisualDifferentiationSteps.java`

### Implementation for User Story 2

- [X] T044 [P] [US2] Add CSS classes to `src/main/resources/static/dashboard.html`: `.state-active` (green left border + green badge), `.state-down` (red left border + red badge), `.state-unavailable` (yellow/grey left border + grey badge); update JS card-rendering function to apply `state-${server.state.toLowerCase()}` class to each card element
- [X] T045 [US2] Verify `DashboardMapper` maps all three `ServerState` enum values to their exact string representations in generated `ServerStatusDto` (ACTIVE → `"ACTIVE"`, DOWN → `"DOWN"`, UNAVAILABLE → `"UNAVAILABLE"`); add assertions for all three states in `DashboardControllerIntegrationTest` in `src/main/java/…/adapter/in/rest/mapper/DashboardMapper.java`

**Checkpoint**: US1 and US2 functional. Dashboard renders three visually distinct state indicators.

---

## Phase 5: User Story 3 - Automatic Data Refresh (Priority: P3)

**Goal**: Dashboard auto-refreshes every 60 seconds without a full page reload.

**Independent Test**: `/dashboard` open in browser → DevTools Network tab shows
`GET /api/v1/servers/dashboard` at ~60 s intervals; page does not reload; cards update in-place.

### Tests for User Story 3 (WRITE FIRST — must FAIL before implementation)

- [X] T046 [P] [US3] Write Cucumber feature file for US3 (3 scenarios: 60 s auto-refresh fires API call without reload; state-change visible after refresh; data visible during in-flight refresh) in `src/test/resources/features/auto_refresh.feature`
- [X] T047 [US3] Write step definitions `AutoRefreshSteps`: assert `GET /api/v1/servers/dashboard` is idempotent (same JSON schema on repeated calls); simulate state change by swapping WireMock stubs (ACTIVE → DOWN) between calls; assert second response reflects new state in `src/test/java/…/functional/steps/AutoRefreshSteps.java`
- [X] T048 [P] [US3] Add timeout edge-case tests to `IcecastPollingAdapterUnitTest` and `ShoutcastPollingAdapterUnitTest`: use WireMock `withFixedDelay(11_000)` to simulate >10 s response; assert `poll()` returns UNAVAILABLE and the total elapsed time is less than 11 000 ms (10 s timeout + 1 s OS scheduling headroom) in `src/test/java/…/adapter/out/polling/IcecastPollingAdapterUnitTest.java` and `ShoutcastPollingAdapterUnitTest.java`

### Implementation for User Story 3

- [X] T049 [US3] Add JavaScript auto-refresh to `src/main/resources/static/dashboard.html`: `setInterval(() => fetch('/api/v1/servers/dashboard').then(r => r.json()).then(updateCards), 60_000)`; `updateCards(data)` replaces only the card container `innerHTML`, keeping the page chrome visible; display "Last updated: HH:MM:SS" below the header, updated on each successful fetch
- [X] T050 [P] [US3] Add statelessness assertion to `DashboardControllerIntegrationTest`: call `GET /api/v1/servers/dashboard` twice in sequence; assert both responses have `refreshedAt` values that are each within ±2 seconds of the UTC time at which the test issued the call (do NOT compare the two `refreshedAt` values against each other — that assertion is brittle in fast-running tests) in `src/test/java/…/adapter/in/rest/DashboardControllerIntegrationTest.java`

**Checkpoint**: All three user stories functional. Dashboard auto-refreshes every 60 s.

---

## Final Phase: Polish & Cross-Cutting Concerns

**Purpose**: Build gate verification, SC-003 performance baseline, documentation, and security.

- [X] T051 [P] Run `./gradlew clean check`; verify JaCoCo HTML report (`build/reports/jacoco/test/html/index.html`) shows ≥ 80% global instruction coverage and > 80% per-class line coverage for domain, adapter, and service packages; fix any class below threshold
- [X] T052 [P] Run ArchUnit test in isolation (`./gradlew test --tests "*.CleanArchitectureTest"`); confirm no violations; resolve any that appear before merge
- [X] T053 Run `quickstart.md` end-to-end: `./gradlew check`, `./gradlew bootRun`, verify `SELECT * FROM flyway_schema_history` shows V1 + V2 applied, verify 3 seed servers in `streaming_server` table, open `http://localhost:8080/dashboard`, verify server cards render, wait 60 s and observe auto-refresh network request, verify DOWN server shows red badge
- [X] T054 [P] Security review: confirm `credentials_ref` value is NOT emitted in SLF4J logs at INFO or DEBUG level; confirm `credentialsRef` field is absent from all API response DTOs (`ServerStatusDto`, `ServerDto`); confirm that a missing env-var reference produces a clear startup log warning, not a silent null
- [X] T055 [P] Add SC-003 performance baseline test: write a `@SpringBootTest` integration test that calls `GET /api/v1/servers/dashboard` with 10 WireMock-stubbed servers each responding in ≤ 200 ms; assert total elapsed wall-clock time < 5 000 ms; this validates SC-003 ("dashboard renders within 5 seconds for up to 10 servers") in `src/test/java/…/adapter/in/rest/DashboardPerformanceTest.java`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Phase 2 — independent of US2 and US3
- **US2 (Phase 4)**: Depends on US1 (extends dashboard.html from US1)
- **US3 (Phase 5)**: Depends on US1 + US2 (auto-refresh makes visual states meaningful)
- **Polish (Final)**: Depends on all user stories complete

### Key Within-Phase Dependencies

- T007 (V1 DDL) must exist before T022 (JPA entity) can pass `ddl-auto=validate`
- T010, T011 (enums) before T012, T013 (entity and value object that use them)
- T014–T018 (ports) before T019–T021 (service implementations that call them)
- T026 (DispatchingPollingAdapter) before T027 (ApplicationConfig wiring)
- T027 (ApplicationConfig) before any US implementation that requires the wired beans
- BDD tests (T029–T035) MUST be confirmed FAIL before T036–T041 implementation

### Parallel Opportunities

- T003, T004, T005, T006 in parallel with T002
- T008, T008b, T009 in parallel with T007 (SQL files only)
- T010 and T011 (separate enum files) in parallel
- T015, T016 in parallel with T014 (all input ports, separate files)
- T017 and T018 (output ports) in parallel
- T019, T020, T021 (domain services) in parallel after their ports exist
- T023 → T024 → T024's mapping (JPA chain, sequential within persistence)
- T030–T035 (US1 test files) all in parallel
- T036 and T037 (polling adapters) in parallel
- T042, T044 in parallel within Phase 4

---

## Parallel Example: User Story 1

```bash
# Write all US1 test files in parallel (no implementation dependencies):
T030: ServerDashboardServiceUnitTest.java
T031: DashboardControllerIntegrationTest.java
T032: StreamingServerJpaAdapterIntegrationTest.java
T033: IcecastPollingAdapterUnitTest.java
T034: ShoutcastPollingAdapterUnitTest.java

# Implement polling adapters in parallel:
T036: IcecastPollingAdapter.java
T037: ShoutcastPollingAdapter.java
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup (T001–T006)
2. Phase 2: Foundational — CRITICAL (T007–T028, includes all ports, services, dispatcher)
3. Write US1 tests, confirm FAIL (T029–T035)
4. Implement US1 (T036–T041)
5. **VALIDATE**: `./gradlew test` passes including Cucumber US1; `/dashboard` renders cards
6. Demo — unified live status view delivered

### Incremental Delivery

1. Setup + Foundational → Flyway-initialized DB, full port hierarchy, DispatchingPollingAdapter
2. US1 → MVP: REST API + HTML dashboard with live server status
3. US2 → enhanced: visual incident detection in < 5 s
4. US3 → continuous: zero-action live monitoring every 60 s
5. Polish → release-ready: JaCoCo gate green, SC-003 baseline validated, security confirmed

### Parallel Team Strategy

With two developers after Phase 1:
- Developer A: T010 → T012 → T014 → T015 → T016 → T019 → T020 → T021 (domain pipeline)
- Developer B: T011 → T013 → T017 → T018 → T022 → T023 → T024 (persistence stack)
- Both converge on T026 (DispatchingPollingAdapter) and T027 (ApplicationConfig wiring)
