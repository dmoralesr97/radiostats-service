package com.morales.radiostatsservice.domain.service;

import com.morales.radiostatsservice.domain.model.ServerType;
import com.morales.radiostatsservice.domain.model.StreamingServer;
import com.morales.radiostatsservice.domain.port.out.StreamingServerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListServersServiceUnitTest {

    @Mock
    private StreamingServerRepository repository;

    @InjectMocks
    private ListServersService service;

    @Test
    void listAll_returnsAllFromRepository() {
        StreamingServer s = new StreamingServer(UUID.randomUUID(), "Radio", ServerType.ICECAST, "host.com", 8000, 1, null);
        when(repository.findAll()).thenReturn(List.of(s));

        List<StreamingServer> result = service.listAll();

        assertThat(result).hasSize(1).containsExactly(s);
        verify(repository).findAll();
    }

    @Test
    void listAll_returnsEmptyWhenRepositoryIsEmpty() {
        when(repository.findAll()).thenReturn(List.of());

        List<StreamingServer> result = service.listAll();

        assertThat(result).isEmpty();
    }
}
