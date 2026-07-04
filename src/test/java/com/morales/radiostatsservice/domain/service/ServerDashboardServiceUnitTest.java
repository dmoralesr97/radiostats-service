package com.morales.radiostatsservice.domain.service;

import com.morales.radiostatsservice.domain.model.ServerState;
import com.morales.radiostatsservice.domain.model.ServerStatus;
import com.morales.radiostatsservice.domain.model.ServerType;
import com.morales.radiostatsservice.domain.model.StreamingServer;
import com.morales.radiostatsservice.domain.port.out.ServerPollingPort;
import com.morales.radiostatsservice.domain.port.out.StreamingServerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerDashboardServiceUnitTest {

    @Mock
    private StreamingServerRepository repository;

    @Mock
    private ServerPollingPort pollingPort;

    @InjectMocks
    private ServerDashboardService service;

    @Test
    void execute_returnsPolledStatusForEachServer() {
        List<StreamingServer> servers = buildServers(3);
        when(repository.findFirstTenByConfigOrder()).thenReturn(servers);
        when(pollingPort.poll(any())).thenAnswer(inv -> {
            StreamingServer s = inv.getArgument(0);
            return new ServerStatus(s.id(), s.name(), s.serverType(), ServerState.ACTIVE, 10, Instant.now());
        });

        List<ServerStatus> result = service.execute();

        assertThat(result).hasSize(3);
        verify(pollingPort, times(3)).poll(any(StreamingServer.class));
    }

    @Test
    void execute_capsAtTenWhenRepositoryReturnsTen() {
        List<StreamingServer> servers = buildServers(10);
        when(repository.findFirstTenByConfigOrder()).thenReturn(servers);
        when(pollingPort.poll(any())).thenAnswer(inv -> {
            StreamingServer s = inv.getArgument(0);
            return new ServerStatus(s.id(), s.name(), s.serverType(), ServerState.ACTIVE, 5, Instant.now());
        });

        List<ServerStatus> result = service.execute();

        assertThat(result).hasSize(10);
        verify(pollingPort, times(10)).poll(any(StreamingServer.class));
    }

    @Test
    void execute_returnsEmptyListWhenNoServers() {
        when(repository.findFirstTenByConfigOrder()).thenReturn(List.of());

        List<ServerStatus> result = service.execute();

        assertThat(result).isEmpty();
        verify(pollingPort, never()).poll(any());
    }

    private List<StreamingServer> buildServers(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new StreamingServer(
                        UUID.randomUUID(), "Server-" + i, ServerType.ICECAST,
                        "host-" + i, 8000, i, null))
                .toList();
    }
}
