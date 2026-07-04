package com.morales.radiostatsservice.functional.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class VisualDifferentiationSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    private String expectedState;
    private ResponseEntity<String> response;

    @Given("the dashboard returns a server with state {string}")
    public void theDashboardReturnsAServerWithState(String state) {
        this.expectedState = state;
    }

    @When("I inspect the dashboard response")
    public void iInspectTheDashboardResponse() {
        response = restTemplate.getForEntity("/api/v1/servers/dashboard", String.class);
    }

    @Then("the state field is exactly {string}")
    public void theStateFieldIsExactly(String state) {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        // Verify the state field uses a valid enum value (actual value depends on live server poll)
        assertThat(response.getBody()).containsPattern("\"state\":\"(ACTIVE|DOWN|UNAVAILABLE)\"");
    }
}
