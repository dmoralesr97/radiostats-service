# Feature Specification: Unified Server Dashboard (US-01)

**Feature Branch**: `001-unified-server-dashboard`
**Created**: 2026-07-04
**Status**: Draft
**Epic**: E-01 · 5 pts

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Unified Status View (Priority: P1)

As a Technical Administrator, I want to open a single dashboard and immediately see the status
(active/down) and current listener count of every configured streaming server, so I no longer
need to open a separate tab per server to get an overall picture of the broadcast health.

**Why this priority**: This is the core value of the feature. Without a unified view, the
administrator has no time savings and the feature has no MVP value.

**Independent Test**: Open the dashboard with between 1 and 10 servers configured.
All servers' status and listener counts must be visible on screen without scrolling to
another page or opening any additional navigation.

**Acceptance Scenarios**:

1. **Given** there are between 1 and 10 streaming servers configured, **When** the
   administrator opens the dashboard, **Then** the status (active/down) and current
   listener count of every server are visible on the same screen without any additional
   navigation.

2. **Given** there are more than 10 servers configured, **When** the dashboard loads,
   **Then** only the first 10 servers (by configuration order) are displayed and the
   remaining servers are not shown.

3. **Given** no servers are configured, **When** the administrator opens the dashboard,
   **Then** an informative empty state is shown explaining that no servers are configured
   and guiding the administrator to add one.

---

### User Story 2 - Visual Differentiation of Server State (Priority: P2)

As a Technical Administrator, I want downed servers to be visually distinct from active ones
on the dashboard, so I can immediately identify problem servers at a glance without reading
every status label.

**Why this priority**: Operational safety — the administrator needs to spot failures
instantly without careful reading. Depends on US-1 being delivered first.

**Independent Test**: Configure at least one active and one downed server. Load the
dashboard and verify the two servers render with visually distinct indicators (not just a
text label difference).

**Acceptance Scenarios**:

1. **Given** a server is down, **When** it appears in the dashboard, **Then** it is
   distinguished with a visual error indicator that is clearly different from the
   indicators used for active servers (not solely a text label).

2. **Given** a server is active, **When** it appears in the dashboard, **Then** its
   indicator clearly communicates an operational state, separate from the error indicator.

3. **Given** a server cannot be reached (connection timeout), **When** it appears in
   the dashboard, **Then** it is displayed with an "unavailable" indicator distinct from
   both the active and known-down indicators.

---

### User Story 3 - Automatic Data Refresh (Priority: P3)

As a Technical Administrator, I want the dashboard to automatically refresh server data
every 60 seconds without reloading the page, so I always have near-real-time visibility
without manual intervention.

**Why this priority**: Enhances US-1 and US-2 by keeping the data live. Requires the
unified view and visual states from P1 and P2 to be meaningful.

**Independent Test**: Open the dashboard, wait 60 seconds without interacting, and verify
that displayed data updates (e.g., listener counts change or server states reflect current
reality) without a full page navigation.

**Acceptance Scenarios**:

1. **Given** the dashboard is open, **When** 60 seconds have elapsed since the last data
   refresh, **Then** all server data is refreshed automatically without reloading the page.

2. **Given** a server was active on the last refresh and has since gone down, **When** the
   next automatic refresh completes, **Then** that server's indicator updates to the error
   state without any administrator action.

3. **Given** the dashboard is open, **When** an automatic refresh is in progress, **Then**
   the administrator can still see the previously loaded data (no blank screen or spinner
   blocking the view).

---

### Edge Cases

- What happens when all configured servers are down simultaneously? All server cards display
  the error indicator; no card shows an active state.
- What happens when a server does not respond within a reasonable time? The server is treated
  as "down/unavailable" for that refresh cycle and displays an "unavailable" indicator.
- What happens when the number of configured servers changes from more than 10 to 10 or fewer
  between refreshes? The dashboard updates to show all servers on the next refresh cycle.
- What happens when a previously downed server comes back up? Its indicator updates to active
  on the next automatic refresh without any administrator action.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display, on a single screen, the status (active/down) and current
  listener count for each configured streaming server, up to a maximum of 10.
- **FR-002**: System MUST limit the displayed servers to the first 10 in configuration order
  when more than 10 servers are configured.
- **FR-003**: System MUST visually distinguish servers that are down from active servers using
  a distinct error indicator that is not solely a text difference.
- **FR-004**: System MUST display servers that cannot be reached (connection failure/timeout)
  with an "unavailable" state indicator, separate from the known-down indicator.
- **FR-005**: System MUST automatically refresh all displayed server data every 60 seconds
  without requiring a full page reload.
- **FR-006**: System MUST keep previously loaded data visible to the administrator while an
  automatic refresh cycle is in progress.
- **FR-007**: System MUST show an empty state with guidance when no streaming servers are
  configured.
- **FR-008**: System MUST update server status and listener counts on the dashboard within
  one refresh cycle (≤ 70 seconds) of a status change occurring.

### Key Entities

- **StreamingServer**: Represents a configured streaming server instance. Key attributes:
  name, type (Icecast or Shoutcast), connection address, configuration order index, and
  optional credentials reference. At most 10 are displayed; all must be stored.
- **ServerStatus**: The real-time snapshot of a server at a point in time. Attributes:
  state (active, down, unavailable), current listener count, and last-checked timestamp.
  Derived on each refresh; not persisted.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A Technical Administrator can view the status and listener count of all
  configured servers (up to 10) from a single screen, with zero additional navigation steps.
- **SC-002**: When a server's state changes (active ↔ down), the updated state is
  reflected on the dashboard within 70 seconds (one refresh cycle plus rendering time).
- **SC-003**: The dashboard renders all server data within 5 seconds of being opened,
  for configurations of up to 10 servers.
- **SC-004**: The number of manual page interactions required to keep server health visible
  during normal monitoring is reduced to zero.
- **SC-005**: An administrator can visually identify a downed server within 5 seconds of
  looking at the dashboard, without reading any status text.

## Assumptions

- The MVP supports between 1 and 10 streaming servers of type Icecast or Shoutcast.
  Stations with more than 10 servers will only see the first 10 by configuration order;
  this limit will be revisited in a future iteration.
- Icecast and Shoutcast APIs are accessible from the application host — either without
  authentication on an internal network, or via credentials stored in environment variables
  configured during the initial setup. A 1-day technical spike is scheduled for iteration 0
  to validate API access patterns for both server types.
- The dashboard requires the user to be authenticated via the existing application
  authentication. No new authentication mechanism is introduced by this feature.
- The automatic refresh interval (60 seconds) is fixed for the MVP. Configurable intervals
  are out of scope.
- A server that does not respond within 10 seconds is treated as "unavailable" for the
  current refresh cycle.
- Mobile responsiveness is out of scope for the MVP. The dashboard targets desktop browsers.
- A server listed in configuration but whose connection details are incomplete or invalid
  is treated as "unavailable" (not as an error in the dashboard itself).
