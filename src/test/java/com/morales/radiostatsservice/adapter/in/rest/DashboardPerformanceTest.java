package com.morales.radiostatsservice.adapter.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
class DashboardPerformanceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void dashboardRespondsWithinFiveSecondsForUpToTenServers() {
        long start = System.currentTimeMillis();

        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/servers/dashboard", String.class);

        long elapsed = System.currentTimeMillis() - start;

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        // SC-003: dashboard renders within 5 seconds for up to 10 servers
        assertThat(elapsed).isLessThan(5_000L);
    }
}
