Feature: Automatic Data Refresh
  As a Technical Administrator
  I want the dashboard to auto-refresh every 60 seconds without a full page reload
  So that I can monitor streaming servers continuously without manual intervention

  Scenario: Dashboard API is idempotent across multiple calls
    When I call the server dashboard API twice in sequence
    Then both responses return HTTP 200 with the same JSON schema

  Scenario: State changes are visible after refresh
    Given a server is initially active
    When the server state changes to "DOWN"
    And I call the server dashboard API again
    Then the response reflects the new state "DOWN"

  Scenario: Data is available during in-flight refresh
    When I call the server dashboard API
    Then the response is immediately available without blocking
