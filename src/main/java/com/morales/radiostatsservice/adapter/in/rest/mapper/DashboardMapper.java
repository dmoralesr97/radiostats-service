package com.morales.radiostatsservice.adapter.in.rest.mapper;

import com.morales.radiostatsservice.domain.model.ServerStatus;
import com.morales.radiostatsservice.domain.model.StreamingServer;
import com.morales.radiostatsservice.generated.model.DashboardResponse;
import com.morales.radiostatsservice.generated.model.ServerDto;
import com.morales.radiostatsservice.generated.model.ServerListResponse;
import com.morales.radiostatsservice.generated.model.ServerStatusDto;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class DashboardMapper {

    public DashboardResponse toResponse(List<ServerStatus> statuses) {
        List<ServerStatusDto> dtos = statuses.stream()
                .map(this::toStatusDto)
                .toList();
        DashboardResponse response = new DashboardResponse();
        response.setServers(dtos);
        response.setTotalServers(dtos.size());
        response.setRefreshedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return response;
    }

    public ServerDto toServerDto(StreamingServer server) {
        ServerDto dto = new ServerDto();
        dto.setId(server.id());
        dto.setName(server.name());
        dto.setServerType(com.morales.radiostatsservice.generated.model.ServerType
                .fromValue(server.serverType().name()));
        dto.setHost(server.host());
        dto.setPort(server.port());
        dto.setConfigOrder(server.configOrder());
        return dto;
    }

    public ServerListResponse toListResponse(List<StreamingServer> servers) {
        ServerListResponse response = new ServerListResponse();
        response.setServers(servers.stream().map(this::toServerDto).toList());
        return response;
    }

    private ServerStatusDto toStatusDto(ServerStatus status) {
        ServerStatusDto dto = new ServerStatusDto();
        dto.setServerId(status.serverId());
        dto.setServerName(status.serverName());
        dto.setServerType(com.morales.radiostatsservice.generated.model.ServerType
                .fromValue(status.serverType().name()));
        dto.setState(com.morales.radiostatsservice.generated.model.ServerState
                .fromValue(status.state().name()));
        dto.setListenerCount(status.listenerCount());
        dto.setLastCheckedAt(status.lastCheckedAt().atOffset(ZoneOffset.UTC));
        return dto;
    }
}
