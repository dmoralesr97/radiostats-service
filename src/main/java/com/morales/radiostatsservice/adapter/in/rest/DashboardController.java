package com.morales.radiostatsservice.adapter.in.rest;

import com.morales.radiostatsservice.adapter.in.rest.mapper.DashboardMapper;
import com.morales.radiostatsservice.domain.model.StreamingServer;
import com.morales.radiostatsservice.domain.model.ServerType;
import com.morales.radiostatsservice.domain.port.in.GetServerDashboardUseCase;
import com.morales.radiostatsservice.domain.port.in.ListServersUseCase;
import com.morales.radiostatsservice.domain.port.in.RegisterServerUseCase;
import com.morales.radiostatsservice.generated.api.DashboardApiDelegate;
import com.morales.radiostatsservice.generated.model.CreateServerRequest;
import com.morales.radiostatsservice.generated.model.DashboardResponse;
import com.morales.radiostatsservice.generated.model.ServerDto;
import com.morales.radiostatsservice.generated.model.ServerListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class DashboardController implements DashboardApiDelegate {

    private final GetServerDashboardUseCase getDashboardUseCase;
    private final RegisterServerUseCase registerServerUseCase;
    private final ListServersUseCase listServersUseCase;
    private final DashboardMapper dashboardMapper;

    public DashboardController(
            GetServerDashboardUseCase getDashboardUseCase,
            RegisterServerUseCase registerServerUseCase,
            ListServersUseCase listServersUseCase,
            DashboardMapper dashboardMapper) {
        this.getDashboardUseCase = getDashboardUseCase;
        this.registerServerUseCase = registerServerUseCase;
        this.listServersUseCase = listServersUseCase;
        this.dashboardMapper = dashboardMapper;
    }

    @Override
    public ResponseEntity<DashboardResponse> getServerDashboard() {
        return ResponseEntity.ok(dashboardMapper.toResponse(getDashboardUseCase.execute()));
    }

    @Override
    public ResponseEntity<ServerDto> createServer(CreateServerRequest request) {
        StreamingServer server = new StreamingServer(
                null,
                request.getName(),
                ServerType.valueOf(request.getServerType().getValue()),
                request.getHost(),
                request.getPort(),
                0,
                request.getCredentialsRef()
        );
        StreamingServer saved = registerServerUseCase.register(server);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dashboardMapper.toServerDto(saved));
    }

    @Override
    public ResponseEntity<ServerListResponse> listServers() {
        return ResponseEntity.ok(dashboardMapper.toListResponse(listServersUseCase.listAll()));
    }
}
