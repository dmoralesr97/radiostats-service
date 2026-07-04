# Research: Unified Server Dashboard (US-01)

**Date**: 2026-07-04
**Feature**: specs/001-unified-server-dashboard/spec.md

---

## 1. Icecast 2.x Status API

**Decision**: Use the public `/status-json.xsl` endpoint (no auth required for read access).

**Rationale**: Icecast 2.4+ exposes a JSON status endpoint that returns listener counts and
mount-point health without credentials. This matches the spec assumption that APIs are
accessible without authentication on internal networks.

**Endpoint**: `http://<host>:<port>/status-json.xsl`

**Key response fields**:
```json
{
  "icestats": {
    "source": [
      { "listeners": 42, "listenurl": "...", "server_name": "..." }
    ]
  }
}
```

If `icestats.source` is absent or the response is empty, the server is treated as DOWN.
Total listener count = sum of `listeners` across all `source` entries.

**Alternatives considered**:
- `/admin/stats` (XML, requires admin credentials) — rejected for MVP due to credential
  management complexity. Revisit for detailed mount-point analytics.

---

## 2. Shoutcast Status API

**Decision**: Support Shoutcast DNAS 2.x via `GET /statistics?output=json` (no auth for
read stats); support Shoutcast v1 via `GET /7.html` (plain text parsing as fallback).

**Rationale**: Shoutcast v2 DNAS provides a JSON API that is straightforward to parse.
Shoutcast v1 exposes a small plain-text page (`7.html`) with comma-separated values
including listener count at position 5.

**Shoutcast v2 endpoint**: `http://<host>:<port>/statistics?output=json`
Key field: `"currentlisteners"` at root of JSON response.

**Shoutcast v1 endpoint**: `http://<host>:<port>/7.html`
Response format: `<currentlisteners>,<peaklisteners>,<maxlisteners>,...`
Listener count = field at index 0.

**Alternatives considered**:
- Shoutcast `admin.cgi` (auth required) — rejected for MVP same reason as Icecast admin.

---

## 3. HTTP Client for Polling (timeout-aware)

**Decision**: Use Spring Boot's `RestClient` (introduced in Spring 6.1, available in
Spring Boot 4.x) with a 10-second connection and read timeout.

**Rationale**: `RestClient` is the modern, fluent API replacing `RestTemplate` in the
Spring ecosystem. It is available in the existing Spring Boot 4.x baseline, has no extra
dependency, and supports configurable timeouts out of the box.

**Configuration**:
```java
RestClient.builder()
    .requestFactory(clientHttpRequestFactory(Duration.ofSeconds(10)))
    .build();
```

**Alternatives considered**:
- `WebClient` (reactive) — rejected; project uses WebMVC (blocking), not WebFlux.
- `HttpClient` (Java 11+ standard library) — viable fallback but adds boilerplate; Spring's
  RestClient is idiomatic for this stack.
- Apache HttpClient 5 — additional dependency, not justified by YAGNI.

---

## 4. OpenAPI Generator with Spring Boot 4.x (Jakarta EE)

**Decision**: Use `openapi-generator-gradle-plugin` version 7.x with the `spring` generator,
targeting `useSpringBoot3 = true` (Jakarta namespace) and `delegatePattern = true`.

**Rationale**: Spring Boot 4.x uses Jakarta EE 10 (`jakarta.*` namespace). The
`openapi-generator-gradle-plugin` 7.x supports this via the `useSpringBoot3 = true` flag.
The `delegatePattern = true` option generates delegate interfaces that the implementation
bean implements — keeping generated code separate from business logic and satisfying the
Clean Architecture principle (adapters depend on ports, not on generated controllers).

**Plugin coordinates**: `org.openapi.generator:openapi-generator-gradle-plugin:7.12.0`

**Generated artifacts**:
- Model DTOs → excluded from JaCoCo coverage (generated code)
- API delegate interfaces → implemented by `DashboardController` in the adapter layer

**Alternatives considered**:
- Hand-written controllers — rejected; violates Principle IV (API First). The contract
  must be the source of truth.
- `springdoc-openapi` annotation-driven approach — rejected; code is the source of truth
  in that model, which inverts the API-First principle.

---

## 5. BDD Test Stack: Cucumber + JUnit 5

**Decision**: Use Cucumber 7.x with `cucumber-spring` and JUnit 5 platform for functional
acceptance tests; JUnit 5 + Mockito for unit tests; Spring Boot `@WebMvcTest` for
integration tests.

**Rationale**: Cucumber integrates natively with JUnit 5 via `@Suite` + Cucumber platform
engine. Feature files map directly to the acceptance scenarios in the spec, creating
traceability from business requirements to runnable tests.

**Dependencies**:
```groovy
testImplementation 'io.cucumber:cucumber-java:7.22.2'
testImplementation 'io.cucumber:cucumber-spring:7.22.2'
testImplementation 'io.cucumber:cucumber-junit-platform-engine:7.22.2'
testImplementation 'org.junit.platform:junit-platform-suite:1.11.4'
```

Feature files: `src/test/resources/features/`

**Alternatives considered**:
- JBehave — less Spring integration tooling; Cucumber is more idiomatic in the Java ecosystem.
- Plain JUnit 5 BDD-style (Given/When/Then in method names) — acceptable for unit tests;
  Cucumber adds business-readable Gherkin for acceptance tests.

---

## 6. ArchUnit for Clean Architecture Enforcement

**Decision**: Use ArchUnit 1.x to enforce the dependency rule at test time.

**Rationale**: ArchUnit runs as a standard JUnit 5 test and fails the build if the
dependency rule is violated (e.g., a domain class imports a Spring annotation). Zero
runtime overhead; enforces Principle I automatically.

**Dependency**: `testImplementation 'com.tngtech.archunit:archunit-junit5:1.4.1'`

**Rules to enforce**:
- `domain` packages MUST NOT depend on `adapter` or `infrastructure`.
- `domain` packages MUST NOT import any `org.springframework` or `javax.*` / `jakarta.*` classes.
- `adapter` packages MUST NOT depend on other `adapter` packages directly (only through `domain`).

---

## 7. JaCoCo Gradle Configuration

**Decision**: Configure JaCoCo 0.8.13 with per-class ≥ 80% line coverage and global ≥ 80%
instruction coverage. Exclude generated OpenAPI models and the Spring Boot application
entry-point class.

**Coverage verification task** gates the `check` task to fail the build when thresholds
are not met. HTML + XML reports are generated for CI artifact upload.

**Exclusions (justified)**:
- `**/generated/**` — openapi-generator output; not hand-written code.
- `**/*Application.java` — Spring Boot entry point; contains only `SpringApplication.run()`.
- `**/infrastructure/config/**` — Spring wiring; verified by integration tests, not unit tests.

---

## 9. Database Schema & Seed Data Management

**Decision**: Use Flyway Core for versioned schema migrations and preloaded seed data,
stored under `src/main/resources/db/migration/`.

**Rationale**: Flyway is the de-facto standard for schema lifecycle management in Spring
Boot applications. Spring Boot 4.x auto-configures Flyway when it is on the classpath and
`spring.flyway.*` properties are set. Using Flyway instead of JPA `ddl-auto=create` gives:
- A single auditable SQL file per schema change (DRY — no DDL duplication across JPA entities
  and hand-rolled scripts).
- Safe migration in production (Flyway refuses to run if the checksum of an applied script
  changes, preventing accidental schema corruption).
- Separation of seed data from business logic: `V2__insert_sample_servers.sql` provides
  three representative servers for local development without baking data into application code.

**Migration file layout**:
```
src/main/resources/db/migration/
├── V1__create_streaming_server_table.sql   # DDL
└── V2__insert_sample_servers.sql           # Dev/local seed data

src/test/resources/db/migration/
└── V3__insert_test_servers.sql             # Test-only data (Flyway test classpath)
```

**`application.yaml` keys**:
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate   # Flyway owns DDL; JPA only validates
```

**Test configuration** (`application-test.yaml`): Flyway enabled; H2 in-memory; `V3` test
script applied via test classpath override of `spring.flyway.locations`.

**Alternatives considered**:
- Spring Boot `schema.sql` + `data.sql` (Spring SQL init) — simpler but no version tracking,
  no checksum protection, and no clean migration history. Rejected for production safety.
- Liquibase — full-featured alternative; heavier XML/YAML syntax not justified by YAGNI
  for this single-table MVP.
- JPA `ddl-auto=create-drop` — convenient for tests but dangerous for any shared environment;
  creates invisible schema drift between JPA annotations and actual DB state. Rejected.

---

## 8. Client-side Auto-Refresh Strategy

**Decision**: The backend exposes a REST endpoint. The frontend (simple HTML + JavaScript
served as a static resource by Spring Boot) polls the REST endpoint every 60 seconds using
`setInterval` + `fetch()`.

**Rationale**: The spec says "auto-refresh without reloading the page." A simple
`setInterval` in JavaScript is the YAGNI-compliant solution. SSE or WebSockets would add
infrastructure complexity not justified for a 60-second polling interval.

**Alternatives considered**:
- SSE (Server-Sent Events) — adds streaming endpoint complexity; overkill for 60s interval.
- WebSocket — adds persistent connection management; strongly over-engineered for this use case.
- Thymeleaf full-page server render with meta refresh — causes page reload; violates SC-004.
