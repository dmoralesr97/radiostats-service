# Data Model: Unified Server Dashboard (US-01)

**Date**: 2026-07-04
**Feature**: specs/001-unified-server-dashboard/spec.md

---

## Entities

### StreamingServer (Domain Entity — persisted)

Represents a configured streaming server. Stored in the database; loaded at dashboard
request time to know which servers to poll.

| Field            | Type        | Constraints                                    |
|------------------|-------------|------------------------------------------------|
| id               | UUID        | PK, auto-generated                             |
| name             | String      | NOT NULL, max 255 chars                        |
| serverType       | ServerType  | NOT NULL; enum: ICECAST, SHOUTCAST_V1, SHOUTCAST_V2 |
| host             | String      | NOT NULL; hostname or IP                       |
| port             | Integer     | NOT NULL; 1–65535                              |
| configOrder      | Integer     | NOT NULL; used to select first 10 when >10 exist |
| credentialsRef   | String      | NULLABLE; env-var key pointing to credentials |

**Business rules**:
- Only the first 10 `StreamingServer` records ordered by `configOrder` are displayed.
- `serverType` determines which polling adapter is used.
- `credentialsRef` is optional; absence means the public (unauthenticated) endpoint is used.

---

### ServerStatus (Value Object — not persisted)

The real-time snapshot of a single server at the moment of polling. Derived fresh on each
dashboard request; never stored.

| Field          | Type        | Description                                                 |
|----------------|-------------|-------------------------------------------------------------|
| serverId       | UUID        | References the `StreamingServer.id`                         |
| serverName     | String      | Copied from `StreamingServer.name`                          |
| serverType     | ServerType  | Copied from `StreamingServer.serverType`                    |
| state          | ServerState | ACTIVE, DOWN, or UNAVAILABLE                                |
| listenerCount  | Integer     | Total current listeners; 0 when DOWN or UNAVAILABLE         |
| lastCheckedAt  | Instant     | UTC timestamp of the polling call                           |

---

### ServerState (Enum)

| Value       | Meaning                                                                                      |
|-------------|----------------------------------------------------------------------------------------------|
| ACTIVE      | Server responded within 10 s and has an active broadcast stream (listener count may be 0)   |
| DOWN        | Server responded but has no active stream — Icecast: `icestats.source` absent or empty; Shoutcast: stream explicitly offline |
| UNAVAILABLE | Connection timed out (> 10 s) or response was unparseable / HTTP error                      |

**Zero-listener rule**: A Shoutcast server returning `currentlisteners=0` with a valid HTTP 200
response is classified as **ACTIVE** (stream is live, no audience). It is **not** DOWN. DOWN
means the stream itself is not broadcasting, not that there are zero listeners. This distinction
matters in `ShoutcastPollingAdapter`: test `currentlisteners == 0` does NOT map to `DOWN`.

**Icecast zero-listener rule**: An Icecast server returning a non-empty `icestats.source` array
with `listeners=0` entries is also classified as **ACTIVE**. The source array present means at
least one mount-point is streaming.

---

### ServerType (Enum)

| Value         | Meaning                                   | Polling endpoint used            |
|---------------|-------------------------------------------|----------------------------------|
| ICECAST       | Icecast 2.x                               | `/status-json.xsl`               |
| SHOUTCAST_V2  | Shoutcast DNAS 2.x                        | `/statistics?output=json`        |
| SHOUTCAST_V1  | Shoutcast v1 (legacy)                     | `/7.html` (comma-separated text) |

---

## Aggregates & Relationships

```
StreamingServer (1) ─── (derived at runtime) ──→ ServerStatus
```

`StreamingServer` is the only persisted aggregate root. `ServerStatus` is computed by the
polling use case and returned in the API response; it is never written to the database.

---

## State Transitions for ServerStatus

```
 Poll issued
     │
     ├─ Response < 10 s AND valid data ──→  ACTIVE
     ├─ Response received BUT no stream ──→ DOWN
     └─ Timeout OR parse error           ──→ UNAVAILABLE
```

---

## Database Schema (Flyway-managed)

Schema is created and evolved exclusively via Flyway migration scripts located in
`src/main/resources/db/migration/`. JPA is configured with `ddl-auto=validate`.

### V1__create_streaming_server_table.sql

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

**Index**: `idx_streaming_server_config_order (config_order ASC)` — supports the
`ORDER BY config_order LIMIT 10` query.

### V2__insert_sample_servers.sql (dev seed data)

```sql
INSERT INTO streaming_server (id, name, server_type, host, port, config_order, credentials_ref)
VALUES
  (RANDOM_UUID(), 'Main Icecast Stream',   'ICECAST',      'stream.example.com', 8000, 1, NULL),
  (RANDOM_UUID(), 'Backup Shoutcast v2',   'SHOUTCAST_V2', 'backup.example.com', 8001, 2, NULL),
  (RANDOM_UUID(), 'Legacy Shoutcast v1',   'SHOUTCAST_V1', 'legacy.example.com', 8000, 3, NULL);
```

Three representative entries covering all `ServerType` values, enabling local development
without needing a real streaming server (use WireMock stubs defined in tests).

### V3__insert_test_servers.sql (test classpath only)

Located in `src/test/resources/db/migration/`. Applied only when the test Spring context
activates Flyway with the test classpath. Provides deterministic UUIDs for assertion in
`StreamingServerJpaAdapterIntegrationTest`.

```sql
INSERT INTO streaming_server (id, name, server_type, host, port, config_order, credentials_ref)
VALUES
  ('aaaaaaaa-0000-0000-0000-000000000001', 'Test Icecast 1',  'ICECAST',      'test-host-1', 8000, 1, NULL),
  ('aaaaaaaa-0000-0000-0000-000000000002', 'Test Icecast 2',  'ICECAST',      'test-host-2', 8000, 2, NULL),
  ('aaaaaaaa-0000-0000-0000-000000000003', 'Test Shoutcast',  'SHOUTCAST_V2', 'test-host-3', 8001, 3, NULL);
-- ... up to 12 rows to test the "limit to 10" business rule
```
