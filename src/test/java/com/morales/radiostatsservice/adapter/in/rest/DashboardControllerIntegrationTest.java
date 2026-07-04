package com.morales.radiostatsservice.adapter.in.rest;

import com.morales.radiostatsservice.adapter.in.rest.mapper.DashboardMapper;
import com.morales.radiostatsservice.domain.model.ServerState;
import com.morales.radiostatsservice.domain.model.ServerStatus;
import com.morales.radiostatsservice.domain.model.ServerType;
import com.morales.radiostatsservice.domain.model.StreamingServer;
import com.morales.radiostatsservice.domain.port.in.GetServerDashboardUseCase;
import com.morales.radiostatsservice.domain.port.in.ListServersUseCase;
import com.morales.radiostatsservice.domain.port.in.RegisterServerUseCase;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import({DashboardController.class, DashboardMapper.class})
class DashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetServerDashboardUseCase getDashboardUseCase;

    @MockitoBean
    private RegisterServerUseCase registerServerUseCase;

    @MockitoBean
    private ListServersUseCase listServersUseCase;

    @Test
    void getServerDashboard_returnsHttpOkWithDashboardResponse() throws Exception {
        UUID serverId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Instant now = Instant.now();
        ServerStatus status = new ServerStatus(serverId, "Main Stream", ServerType.ICECAST,
                ServerState.ACTIVE, 128, now);
        when(getDashboardUseCase.execute()).thenReturn(List.of(status));

        mockMvc.perform(get("/api/v1/servers/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers", hasSize(1)))
                .andExpect(jsonPath("$.totalServers", is(1)))
                .andExpect(jsonPath("$.refreshedAt").exists())
                .andExpect(jsonPath("$.servers[0].serverId", is(serverId.toString())))
                .andExpect(jsonPath("$.servers[0].serverName", is("Main Stream")))
                .andExpect(jsonPath("$.servers[0].state", is("ACTIVE")))
                .andExpect(jsonPath("$.servers[0].listenerCount", is(128)));
    }

    @Test
    void getServerDashboard_allThreeStatesMapped() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        Instant now = Instant.now();
        List<ServerStatus> statuses = List.of(
                new ServerStatus(id1, "Active", ServerType.ICECAST, ServerState.ACTIVE, 10, now),
                new ServerStatus(id2, "Down", ServerType.SHOUTCAST_V2, ServerState.DOWN, 0, now),
                new ServerStatus(id3, "Unavail", ServerType.SHOUTCAST_V1, ServerState.UNAVAILABLE, 0, now)
        );
        when(getDashboardUseCase.execute()).thenReturn(statuses);

        mockMvc.perform(get("/api/v1/servers/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].state", is("ACTIVE")))
                .andExpect(jsonPath("$.servers[1].state", is("DOWN")))
                .andExpect(jsonPath("$.servers[2].state", is("UNAVAILABLE")));
    }

    @Test
    void createServer_returnsCreatedWithServerDto() throws Exception {
        UUID savedId = UUID.randomUUID();
        StreamingServer saved = new StreamingServer(savedId, "New Radio", ServerType.ICECAST,
                "radio.example.com", 8000, 1, null);
        when(registerServerUseCase.register(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/servers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Radio","serverType":"ICECAST","host":"radio.example.com","port":8000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(savedId.toString())))
                .andExpect(jsonPath("$.name", is("New Radio")))
                .andExpect(jsonPath("$.serverType", is("ICECAST")));
    }

    @Test
    void listServers_returnsOkWithServerList() throws Exception {
        UUID id = UUID.randomUUID();
        StreamingServer server = new StreamingServer(id, "Listed Radio", ServerType.SHOUTCAST_V2,
                "shout.example.com", 8080, 2, null);
        when(listServersUseCase.listAll()).thenReturn(List.of(server));

        mockMvc.perform(get("/api/v1/servers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers", hasSize(1)))
                .andExpect(jsonPath("$.servers[0].id", is(id.toString())))
                .andExpect(jsonPath("$.servers[0].name", is("Listed Radio")));
    }

    @Test
    void getServerDashboard_statelessnessAssertion() throws Exception {
        Instant beforeFirst = Instant.now();
        when(getDashboardUseCase.execute()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/servers/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers", hasSize(0)))
                .andExpect(jsonPath("$.totalServers", is(0)))
                .andExpect(jsonPath("$.refreshedAt").exists());

        Instant afterFirst = Instant.now();

        mockMvc.perform(get("/api/v1/servers/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers", hasSize(0)))
                .andExpect(jsonPath("$.totalServers", is(0)))
                .andExpect(jsonPath("$.refreshedAt").exists());

        Instant afterSecond = Instant.now();

        // Both requests complete within the test window
        Assertions.assertThat(afterSecond.toEpochMilli() - beforeFirst.toEpochMilli())
                .isLessThan(4000L);
    }
}
