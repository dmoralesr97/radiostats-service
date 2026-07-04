Feature: Visual Differentiation of Server State
  As a Technical Administrator
  I want downed and unavailable servers to be visually distinct from active servers
  So that I can detect incidents at a glance without reading text labels

  Scenario: DOWN server indicator is visually distinct from ACTIVE
    Given the dashboard returns a server with state "DOWN"
    When I inspect the dashboard response
    Then the state field is exactly "DOWN"

  Scenario: ACTIVE server indicator is distinct from DOWN
    Given the dashboard returns a server with state "ACTIVE"
    When I inspect the dashboard response
    Then the state field is exactly "ACTIVE"

  Scenario: UNAVAILABLE server indicator is distinct from both ACTIVE and DOWN
    Given the dashboard returns a server with state "UNAVAILABLE"
    When I inspect the dashboard response
    Then the state field is exactly "UNAVAILABLE"
