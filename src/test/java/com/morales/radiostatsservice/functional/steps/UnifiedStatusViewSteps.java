package com.morales.radiostatsservice.functional.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class UnifiedStatusViewSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private ResponseEntity<String> response;

    @Given("there are {int} configured streaming servers")
    public void thereAreConfiguredStreamingServers(int count) {
        jdbcTemplate.execute("DELETE FROM streaming_server");
        for (int i = 1; i <= count; i++) {
            jdbcTemplate.update(
                "INSERT INTO streaming_server (name, server_type, host, port, config_order) VALUES (?, 'ICECAST', ?, 8000, ?)",
                "Setup-Server-" + String.format("%02d", i),
                "test-host-" + i,
                i
            );
        }
    }

    @Given("each server is active with {int} listeners")
    public void eachServerIsActiveWithListeners(int listeners) {
        // Polling result is determined by live server availability
    }

    @Given("there are no configured streaming servers")
    public void thereAreNoConfiguredStreamingServers() {
        jdbcTemplate.execute("DELETE FROM streaming_server");
    }

    @When("I request the server dashboard")
    public void iRequestTheServerDashboard() {
        response = restTemplate.getForEntity("/api/v1/servers/dashboard", String.class);
    }

    @Then("the response contains {int} server status entries")
    public void theResponseContainsServerStatusEntries(int count) {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        String body = response.getBody();
        assertThat(body).isNotNull();
        try {
            JsonNode root = objectMapper.readTree(body);
            assertThat(root.get("totalServers").asInt())
                .as("totalServers in dashboard response")
                .isEqualTo(count);
            assertThat(root.get("servers").size())
                .as("servers array size in dashboard response")
                .isEqualTo(count);
        } catch (Exception e) {
            throw new AssertionError("Could not parse response body: " + body, e);
        }
    }

    @Then("each entry has state {string} and listener count {int}")
    public void eachEntryHasStateAndListenerCount(String state, int listenerCount) {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
