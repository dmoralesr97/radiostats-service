package com.morales.radiostatsservice.adapter.out.polling;

import tools.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.morales.radiostatsservice.domain.model.ServerState;
import com.morales.radiostatsservice.domain.model.ServerStatus;
import com.morales.radiostatsservice.domain.model.ServerType;
import com.morales.radiostatsservice.domain.model.StreamingServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class IcecastPollingAdapterUnitTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private IcecastPollingAdapter adapter;

    @BeforeEach
    void setUp() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(10));
        adapter = new IcecastPollingAdapter(RestClient.builder().requestFactory(factory).build(), new ObjectMapper());
    }

    @Test
    void poll_returnsActiveWithListenerSum_whenValidSourceArray() {
        wireMock.stubFor(get(urlEqualTo("/status-json.xsl"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "icestats": {
                                    "source": [
                                      {"listeners": 30},
                                      {"listeners": 15}
                                    ]
                                  }
                                }
                                """)));

        StreamingServer server = buildServer();
        ServerStatus result = adapter.poll(server);

        assertThat(result.state()).isEqualTo(ServerState.ACTIVE);
        assertThat(result.listenerCount()).isEqualTo(45);
    }

    @Test
    void poll_returnsDown_whenSourceArrayIsEmpty() {
        wireMock.stubFor(get(urlEqualTo("/status-json.xsl"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"icestats": {"source": []}}
                                """)));

        ServerStatus result = adapter.poll(buildServer());

        assertThat(result.state()).isEqualTo(ServerState.DOWN);
        assertThat(result.listenerCount()).isZero();
    }

    @Test
    void poll_returnsDown_whenSourceIsAbsent() {
        wireMock.stubFor(get(urlEqualTo("/status-json.xsl"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"icestats": {}}
                                """)));

        ServerStatus result = adapter.poll(buildServer());

        assertThat(result.state()).isEqualTo(ServerState.DOWN);
    }

    @Test
    void poll_returnsUnavailable_whenRequestTimesOut() {
        wireMock.stubFor(get(urlEqualTo("/status-json.xsl"))
                .willReturn(aResponse()
                        .withFixedDelay(11_000)
                        .withBody("{}")));

        long start = System.currentTimeMillis();
        ServerStatus result = adapter.poll(buildServer());
        long elapsed = System.currentTimeMillis() - start;

        assertThat(result.state()).isEqualTo(ServerState.UNAVAILABLE);
        assertThat(elapsed).isLessThan(11_000L);
    }

    private StreamingServer buildServer() {
        return new StreamingServer(UUID.randomUUID(), "Test Icecast",
                ServerType.ICECAST, "localhost", wireMock.getPort(), 1, null);
    }
}
