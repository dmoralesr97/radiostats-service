package com.morales.radiostatsservice.domain.service;

import com.morales.radiostatsservice.domain.model.ServerStatus;
import com.morales.radiostatsservice.domain.model.StreamingServer;
import com.morales.radiostatsservice.domain.port.in.GetServerDashboardUseCase;
import com.morales.radiostatsservice.domain.port.out.ServerPollingPort;
import com.morales.radiostatsservice.domain.port.out.StreamingServerRepository;

import java.util.List;

public class ServerDashboardService implements GetServerDashboardUseCase {

    private final StreamingServerRepository repository;
    private final ServerPollingPort pollingPort;

    public ServerDashboardService(StreamingServerRepository repository, ServerPollingPort pollingPort) {
        this.repository = repository;
        this.pollingPort = pollingPort;
    }

    @Override
    public List<ServerStatus> execute() {
        List<StreamingServer> servers = repository.findFirstTenByConfigOrder();
        return servers.stream()
                .map(pollingPort::poll)
                .toList();
    }
}
