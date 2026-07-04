<!-- SYNC IMPACT REPORT
  Version change: N/A → 1.0.0 (initial ratification)
  Bump rationale: MAJOR — first complete constitution with 5 foundational principles.

  Modified principles: none (initial creation)

  Added sections:
    - I.  Clean Architecture (Robert C. Martin)
    - II. BDD Testing Strategy
    - III. Programming Best Practices (SOLID, YAGNI, DRY)
    - IV. API First with OpenAPI
    - V.  Quality Metrics & Coverage

  Removed sections: none

  Templates requiring updates:
    - .specify/templates/plan-template.md  ✅ Constitution Check section already present; aligns with these principles
    - .specify/templates/spec-template.md  ✅ BDD Given/When/Then acceptance scenarios already in template
    - .specify/templates/tasks-template.md ✅ Phase/story structure compatible; add OpenAPI contract tasks to Foundational phase

  Deferred TODOs: none
-->

# radiostats-service Constitution

## Core Principles

### I. Clean Architecture (Robert C. Martin)

Every feature MUST be structured according to Clean Architecture layers. The dependency rule
is non-negotiable: outer layers depend inward; inner layers MUST NOT reference outer layers.

- **Domain layer** (entities, value objects, domain events): zero framework dependencies.
- **Use Cases / Application layer**: orchestrates domain objects; depends only on domain.
  Contains ports (interfaces) for all external resources (repositories, messaging, external APIs).
- **Interface Adapters layer**: controllers, presenters, repository implementations, gateways.
  Maps between domain and external formats (HTTP DTOs, persistence models).
- **Frameworks & Drivers layer**: Spring Boot wiring, JPA, database drivers, external clients.
  MUST be replaceable without touching use cases or domain.
- Cross-cutting concerns (logging, security, transactions) belong in the adapter or frameworks
  layer via decorators/AOP — never embedded in domain or use-case classes.
- Circular dependencies between packages are forbidden; enforce via ArchUnit tests.

**Rationale**: Preserves testability, postpones infrastructure decisions, and makes the system
resilient to framework evolution (demonstrated by the Spring Boot 4.x migration path).

---

### II. BDD Testing Strategy

All behaviour MUST be specified and verified using Behaviour-Driven Development (BDD). Tests
are the executable specification of the system.

- **Unit tests**: test a single class/use-case in isolation using mocks/stubs for ports.
  Written in JUnit 5 + Mockito. Scenario format: `Given_<state>_When_<action>_Then_<outcome>`.
- **Integration tests**: verify that adapters and infrastructure interact correctly
  (repository → DB, controller → use-case pipeline). Use Spring Boot Test slices
  (`@DataJpaTest`, `@WebMvcTest`). Database state MUST be reset between tests.
- **Functional / acceptance tests**: drive the full stack via HTTP (RestAssured or
  MockMvc against a running application context) against scenarios expressed in Gherkin
  (Cucumber) or plain BDD structured tests. Cover every user story acceptance scenario
  defined in the spec.
- Tests MUST be written **before** implementation (TDD/BDD red-green-refactor cycle).
- Test file naming: `<Subject>UnitTest`, `<Subject>IntegrationTest`, `<Subject>FunctionalTest`.
- Test classes MUST be co-located with the layer they test (`src/test/java` mirror of `src/main/java`).

**Rationale**: BDD bridges the gap between business requirements and executable tests,
ensuring that every implemented behaviour traces back to a user story acceptance scenario.

---

### III. Programming Best Practices — SOLID, YAGNI, DRY

All production code MUST adhere to the following principles; violations require explicit
justification in code review:

- **SOLID**
  - *Single Responsibility*: each class has one reason to change; max one public method
    group per class.
  - *Open/Closed*: extend via interfaces and composition; do not modify stable abstractions.
  - *Liskov Substitution*: subtypes MUST honour the contract of their supertypes.
  - *Interface Segregation*: define narrow, role-specific interfaces; clients MUST NOT
    depend on methods they do not use.
  - *Dependency Inversion*: depend on abstractions (ports), not concrete implementations.
- **YAGNI** (You Aren't Gonna Need It): implement only what a current user story
  requires. Generic utilities, configuration options, or extension points MUST have an
  active requirement to exist.
- **DRY** (Don't Repeat Yourself): every piece of business knowledge MUST have a single
  authoritative representation. Extract shared logic into domain services or utility
  classes — but only when duplication is observed at least twice in production code.

**Rationale**: These principles keep the codebase maintainable, reduce coupling, and
prevent speculative complexity that slows future development.

---

### IV. API First with OpenAPI

Every external-facing API endpoint MUST be specified in an OpenAPI 3.x contract **before**
any implementation begins. The contract is the single source of truth.

- OpenAPI contract files MUST live under `src/main/resources/openapi/` (e.g.,
  `radiostats-api.yaml`).
- Server-side stubs and model classes MUST be generated from the contract using
  `openapi-generator-gradle-plugin` (target: `spring` generator). Hand-written
  controllers MUST NOT duplicate model definitions already in the contract.
- Contract changes require a contract review step before implementation tasks are created.
- Breaking changes (removing/renaming fields, changing HTTP method or path) MUST bump
  the API major version and be communicated to all consumers.
- All API responses MUST include proper HTTP status codes and error schemas as defined
  in the contract.
- Non-REST integrations (events, async) MUST similarly have a documented schema contract
  before implementation.

**Rationale**: API First decouples client and server development, enforces design-time
thinking, and eliminates contract drift between documentation and implementation.

---

### V. Quality Metrics & Coverage

All quality gates are mandatory and enforced via the Gradle build. A build that does not
meet these thresholds MUST NOT merge to the main branch.

- **Per-class line/branch coverage**: MUST be > 80% for every class in the
  `domain`, `application`, and `adapter` packages.
- **Global coverage**: instruction coverage MUST be ≥ 80% across all production sources.
- **Tool**: JaCoCo MUST be configured in `build.gradle` with `jacocoTestReport` and
  `jacocoTestCoverageVerification` tasks. Coverage reports are generated in HTML and XML
  formats under `build/reports/jacoco/`.
- The `check` task MUST depend on `jacocoTestCoverageVerification` so that CI failures
  are unambiguous.
- Coverage exclusions (e.g., generated OpenAPI models, Spring Boot entry point class)
  MUST be explicitly listed in the JaCoCo configuration and reviewed in PRs.
- Coverage reports MUST be published as build artifacts in CI for every pull request.

**Rationale**: Quantitative coverage gates provide a measurable, automatable quality
signal. JaCoCo integrates natively with Gradle and produces reports consumable by
SonarQube and standard CI dashboards.

---

## Code Quality Standards

- Static analysis via Checkstyle or SpotBugs MUST pass before merge.
- No `System.out.println` in production code; use SLF4J logging exclusively.
- Lombok is permitted for boilerplate reduction (`@Data`, `@Builder`, `@RequiredArgsConstructor`)
  but MUST NOT be used to hide business logic.
- All exceptions thrown across layer boundaries MUST be domain-defined or mapped via
  an exception handler — raw framework exceptions MUST NOT leak to the API response body.
- All new code MUST be reviewed by at least one other developer before merge.

---

## Development Workflow

1. **Spec first**: create a feature spec with BDD acceptance scenarios before opening a branch.
2. **Contract first**: write or update the OpenAPI contract; generate server stubs.
3. **Tests first**: write failing unit and integration tests matching acceptance scenarios.
4. **Implement**: make tests pass following Clean Architecture layers inside-out
   (domain → use cases → adapters → framework wiring).
5. **Coverage gate**: run `./gradlew check` — verify JaCoCo thresholds pass locally.
6. **PR review**: constitution check, ArchUnit report, coverage report, and at least one
   peer approval required before merge.
7. Commits MUST be atomic and reference the task/story ID.

---

## Governance

- This constitution supersedes all prior verbal agreements and informal conventions.
- Any amendment requires: (a) a written proposal describing the change and rationale,
  (b) consensus among active contributors (or team lead approval for solo projects),
  (c) a migration plan if existing code violates the new rule, and
  (d) a version bump following semantic versioning rules defined above.
- Compliance is verified during PR review using the **Constitution Check** section of
  `plan.md` and the ArchUnit test suite.
- The governance section MUST be re-read at the start of every new feature cycle.
- Amendments are recorded in this file; historical versions are traceable via git history.

**Version**: 1.0.0 | **Ratified**: 2026-07-04 | **Last Amended**: 2026-07-04
