package com.morales.radiostatsservice.functional.steps;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

public class AutoRefreshSteps {

    private static final WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ResponseEntity<String> firstResponse;
    private ResponseEntity<String> secondResponse;

    @When("I call the server dashboard API twice in sequence")
    public void iCallTheServerDashboardApiTwiceInSequence() {
        firstResponse = restTemplate.getForEntity("/api/v1/servers/dashboard", String.class);
        secondResponse = restTemplate.getForEntity("/api/v1/servers/dashboard", String.class);
    }

    @Then("both responses return HTTP 200 with the same JSON schema")
    public void bothResponsesReturnHttp200WithSameSchema() {
        assertThat(firstResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(firstResponse.getBody()).contains("servers").contains("totalServers").contains("refreshedAt");
        assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(secondResponse.getBody()).contains("servers").contains("totalServers").contains("refreshedAt");
    }

    @Given("a server is initially active")
    public void aServerIsInitiallyActive() {
        jdbcTemplate.execute("DELETE FROM streaming_server");
        jdbcTemplate.update(
            "INSERT INTO streaming_server (id, name, server_type, host, port, config_order) VALUES (?, 'WireMock-Icecast', 'ICECAST', 'localhost', ?, 1)",
            UUID.randomUUID(), wireMockServer.port()
        );
        wireMockServer.resetMappings();
        wireMockServer.stubFor(get(urlEqualTo("/status-json.xsl"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"icestats\":{\"source\":{\"listeners\":5}}}")));
    }

    @When("the server state changes to {string}")
    public void theServerStateChangesTo(String state) {
        wireMockServer.resetMappings();
        if ("DOWN".equals(state)) {
            // Valid Icecast response with no active source → IcecastPollingAdapter returns DOWN
            wireMockServer.stubFor(get(urlEqualTo("/status-json.xsl"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"icestats\":{}}")));
        } else if ("UNAVAILABLE".equals(state)) {
            wireMockServer.stubFor(get(urlEqualTo("/status-json.xsl"))
                .willReturn(aResponse().withFixedDelay(11_000)));
        }
    }

    @When("I call the server dashboard API again")
    public void iCallTheServerDashboardApiAgain() {
        secondResponse = restTemplate.getForEntity("/api/v1/servers/dashboard", String.class);
    }

    @Then("the response reflects the new state {string}")
    public void theResponseReflectsTheNewState(String state) {
        assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(secondResponse.getBody()).contains("\"state\":\"" + state + "\"");
    }

    @When("I call the server dashboard API")
    public void iCallTheServerDashboardApi() {
        firstResponse = restTemplate.getForEntity("/api/v1/servers/dashboard", String.class);
    }

    @Then("the response is immediately available without blocking")
    public void theResponseIsImmediatelyAvailable() {
        assertThat(firstResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(firstResponse.getHeaders().getContentType()).isNotNull();
    }
}
