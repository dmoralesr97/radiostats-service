package com.morales.radiostatsservice.adapter.out.polling;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.morales.radiostatsservice.domain.model.ServerState;
import com.morales.radiostatsservice.domain.model.ServerStatus;
import com.morales.radiostatsservice.domain.model.ServerType;
import com.morales.radiostatsservice.domain.model.StreamingServer;
import com.morales.radiostatsservice.domain.port.out.ServerPollingPort;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

public class ShoutcastPollingAdapter implements ServerPollingPort {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ShoutcastPollingAdapter(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ServerStatus poll(StreamingServer server) {
        try {
            if (server.serverType() == ServerType.SHOUTCAST_V2) {
                return pollV2(server);
            } else {
                return pollV1(server);
            }
        } catch (RestClientException e) {
            return unavailable(server);
        } catch (Exception e) {
            return unavailable(server);
        }
    }

    private ServerStatus pollV2(StreamingServer server) {
        String url = "http://" + server.host() + ":" + server.port() + "/statistics?output=json";
        String responseBody = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            return unavailable(server);
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            int listeners = root.path("currentlisteners").asInt(0);
            // Zero listeners with valid response = ACTIVE (stream is live, no audience)
            return new ServerStatus(server.id(), server.name(), server.serverType(),
                    ServerState.ACTIVE, listeners, Instant.now());
        } catch (Exception e) {
            return unavailable(server);
        }
    }

    private ServerStatus pollV1(StreamingServer server) {
        String url = "http://" + server.host() + ":" + server.port() + "/7.html";
        String body = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        if (body == null || body.isBlank()) {
            return unavailable(server);
        }

        String[] fields = body.split(",");
        if (fields.length == 0) {
            return unavailable(server);
        }

        int listeners = Integer.parseInt(fields[0].trim());
        return new ServerStatus(server.id(), server.name(), server.serverType(),
                ServerState.ACTIVE, listeners, Instant.now());
    }

    private ServerStatus unavailable(StreamingServer server) {
        return new ServerStatus(server.id(), server.name(), server.serverType(),
                ServerState.UNAVAILABLE, 0, Instant.now());
    }
}
