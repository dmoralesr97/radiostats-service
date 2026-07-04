package com.morales.radiostatsservice.domain.service;

import com.morales.radiostatsservice.domain.model.ServerType;
import com.morales.radiostatsservice.domain.model.StreamingServer;
import com.morales.radiostatsservice.domain.port.out.StreamingServerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterServerServiceUnitTest {

    @Mock
    private StreamingServerRepository repository;

    @InjectMocks
    private RegisterServerService service;

    @Test
    void register_delegatesToRepository_andReturnsResult() {
        StreamingServer input = new StreamingServer(null, "Radio One", ServerType.ICECAST, "host.example.com", 8000, 1, null);
        StreamingServer saved = new StreamingServer(UUID.randomUUID(), "Radio One", ServerType.ICECAST, "host.example.com", 8000, 1, null);
        when(repository.save(any())).thenReturn(saved);

        StreamingServer result = service.register(input);

        assertThat(result.id()).isNotNull();
        assertThat(result.name()).isEqualTo("Radio One");
        verify(repository).save(input);
    }

    @Test
    void register_throwsWhenNameIsBlank() {
        StreamingServer input = new StreamingServer(null, "", ServerType.ICECAST, "host.example.com", 8000, 1, null);

        assertThatThrownBy(() -> service.register(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void register_throwsWhenNameIsNull() {
        StreamingServer input = new StreamingServer(null, null, ServerType.ICECAST, "host.example.com", 8000, 1, null);

        assertThatThrownBy(() -> service.register(input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void register_throwsWhenPortBelowOne() {
        StreamingServer input = new StreamingServer(null, "Radio One", ServerType.ICECAST, "host.example.com", 0, 1, null);

        assertThatThrownBy(() -> service.register(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Port");
    }

    @Test
    void register_throwsWhenPortAbove65535() {
        StreamingServer input = new StreamingServer(null, "Radio One", ServerType.ICECAST, "host.example.com", 65536, 1, null);

        assertThatThrownBy(() -> service.register(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Port");
    }

    @Test
    void register_acceptsValidBoundaryPorts() {
        StreamingServer input1 = new StreamingServer(null, "Radio One", ServerType.ICECAST, "host.example.com", 1, 1, null);
        StreamingServer input2 = new StreamingServer(null, "Radio Two", ServerType.ICECAST, "host.example.com", 65535, 1, null);
        when(repository.save(any())).thenReturn(input1);

        service.register(input1);
        service.register(input2);

        verify(repository).save(input1);
        verify(repository).save(input2);
    }
}
