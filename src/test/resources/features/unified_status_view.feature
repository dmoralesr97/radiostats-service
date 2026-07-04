Feature: Unified Status View
  As a Technical Administrator
  I want to see the real-time status and listener count of all configured servers on one screen
  So that I can monitor all streaming servers without extra navigation

  Scenario: Dashboard shows status for up to 10 configured servers
    Given there are 5 configured streaming servers
    And each server is active with 10 listeners
    When I request the server dashboard
    Then the response contains 5 server status entries
    And each entry has state "ACTIVE" and listener count 10

  Scenario: Dashboard caps results at 10 servers when more than 10 are configured
    Given there are 12 configured streaming servers
    And each server is active with 5 listeners
    When I request the server dashboard
    Then the response contains 10 server status entries

  Scenario: Dashboard shows empty state when no servers are configured
    Given there are no configured streaming servers
    When I request the server dashboard
    Then the response contains 0 server status entries
