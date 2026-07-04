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

class ShoutcastPollingAdapterUnitTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private ShoutcastPollingAdapter adapter;

    @BeforeEach
    void setUp() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(10));
        adapter = new ShoutcastPollingAdapter(RestClient.builder().requestFactory(factory).build(), new ObjectMapper());
    }

    @Test
    void poll_shoutcastV2_returnsActiveWith55Listeners() {
        wireMock.stubFor(get(urlEqualTo("/statistics?output=json"))
                .willReturn(okJson("""
                        {"currentlisteners": 55, "peaklisteners": 100}
                        """)));

        StreamingServer server = buildServer(ServerType.SHOUTCAST_V2);
        ServerStatus result = adapter.poll(server);

        assertThat(result.state()).isEqualTo(ServerState.ACTIVE);
        assertThat(result.listenerCount()).isEqualTo(55);
    }

    @Test
    void poll_shoutcastV2_returnsActiveEvenWithZeroListeners() {
        // Zero listeners with valid response = ACTIVE (active stream, no audience)
        wireMock.stubFor(get(urlEqualTo("/statistics?output=json"))
                .willReturn(okJson("""
                        {"currentlisteners": 0}
                        """)));

        ServerStatus result = adapter.poll(buildServer(ServerType.SHOUTCAST_V2));

        assertThat(result.state()).isEqualTo(ServerState.ACTIVE);
        assertThat(result.listenerCount()).isZero();
    }

    @Test
    void poll_shoutcastV1_returnsActiveWith42Listeners() {
        wireMock.stubFor(get(urlEqualTo("/7.html"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/html")
                        .withBody("42,100,200,0,128,128,Stream Title")));

        StreamingServer server = buildServer(ServerType.SHOUTCAST_V1);
        ServerStatus result = adapter.poll(server);

        assertThat(result.state()).isEqualTo(ServerState.ACTIVE);
        assertThat(result.listenerCount()).isEqualTo(42);
    }

    @Test
    void poll_returnsUnavailable_whenConnectionTimesOut() {
        wireMock.stubFor(get(urlEqualTo("/statistics?output=json"))
                .willReturn(aResponse()
                        .withFixedDelay(11_000)
                        .withBody("{}")));

        long start = System.currentTimeMillis();
        ServerStatus result = adapter.poll(buildServer(ServerType.SHOUTCAST_V2));
        long elapsed = System.currentTimeMillis() - start;

        assertThat(result.state()).isEqualTo(ServerState.UNAVAILABLE);
        assertThat(elapsed).isLessThan(11_000L);
    }

    private StreamingServer buildServer(ServerType type) {
        return new StreamingServer(UUID.randomUUID(), "Test Shoutcast",
                type, "localhost", wireMock.getPort(), 1, null);
    }
}
