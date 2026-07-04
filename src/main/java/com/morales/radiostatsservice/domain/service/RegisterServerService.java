package com.morales.radiostatsservice.domain.service;

import com.morales.radiostatsservice.domain.model.StreamingServer;
import com.morales.radiostatsservice.domain.port.in.RegisterServerUseCase;
import com.morales.radiostatsservice.domain.port.out.StreamingServerRepository;

public class RegisterServerService implements RegisterServerUseCase {

    private final StreamingServerRepository repository;

    public RegisterServerService(StreamingServerRepository repository) {
        this.repository = repository;
    }

    @Override
    public StreamingServer register(StreamingServer server) {
        if (server.name() == null || server.name().isBlank()) {
            throw new IllegalArgumentException("Server name must not be blank");
        }
        if (server.port() < 1 || server.port() > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        return repository.save(server);
    }
}
