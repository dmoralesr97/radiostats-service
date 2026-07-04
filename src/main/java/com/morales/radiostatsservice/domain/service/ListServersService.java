package com.morales.radiostatsservice.domain.service;

import com.morales.radiostatsservice.domain.model.StreamingServer;
import com.morales.radiostatsservice.domain.port.in.ListServersUseCase;
import com.morales.radiostatsservice.domain.port.out.StreamingServerRepository;

import java.util.List;

public class ListServersService implements ListServersUseCase {

    private final StreamingServerRepository repository;

    public ListServersService(StreamingServerRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<StreamingServer> listAll() {
        return repository.findAll();
    }
}
