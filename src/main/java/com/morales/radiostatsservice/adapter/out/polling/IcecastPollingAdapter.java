package com.morales.radiostatsservice.adapter.out.polling;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.morales.radiostatsservice.domain.model.ServerState;
import com.morales.radiostatsservice.domain.model.ServerStatus;
import com.morales.radiostatsservice.domain.model.StreamingServer;
import com.morales.radiostatsservice.domain.port.out.ServerPollingPort;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

public class IcecastPollingAdapter implements ServerPollingPort {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public IcecastPollingAdapter(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ServerStatus poll(StreamingServer server) {
        String url = "http://" + server.host() + ":" + server.port() + "/status-json.xsl";
        try {
            String responseBody = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return unavailable(server);
            }

            JsonNode root = objectMapper.readTree(responseBody);

            if (root == null) {
                return unavailable(server);
            }

            JsonNode icestats = root.get("icestats");
            if (icestats == null) {
                return unavailable(server);
            }

            JsonNode source = icestats.get("source");
            if (source == null || source.isMissingNode()) {
                return down(server);
            }

            // source can be a single object or an array
            int totalListeners = 0;
            if (source.isArray()) {
                if (source.isEmpty()) {
                    return down(server);
                }
                for (JsonNode s : source) {
                    totalListeners += s.path("listeners").asInt(0);
                }
            } else if (source.isObject()) {
                totalListeners = source.path("listeners").asInt(0);
            } else {
                return down(server);
            }

            return new ServerStatus(
                    server.id(), server.name(), server.serverType(),
                    ServerState.ACTIVE, totalListeners, Instant.now()
            );

        } catch (RestClientException e) {
            return unavailable(server);
        } catch (Exception e) {
            return unavailable(server);
        }
    }

    private ServerStatus down(StreamingServer server) {
        return new ServerStatus(server.id(), server.name(), server.serverType(),
                ServerState.DOWN, 0, Instant.now());
    }

    private ServerStatus unavailable(StreamingServer server) {
        return new ServerStatus(server.id(), server.name(), server.serverType(),
                ServerState.UNAVAILABLE, 0, Instant.now());
    }
}
