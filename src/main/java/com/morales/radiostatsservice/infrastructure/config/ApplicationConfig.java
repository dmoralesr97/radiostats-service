package com.morales.radiostatsservice.infrastructure.config;

import tools.jackson.databind.ObjectMapper;
import com.morales.radiostatsservice.adapter.out.persistence.StreamingServerJpaAdapter;
import com.morales.radiostatsservice.adapter.out.polling.DispatchingPollingAdapter;
import com.morales.radiostatsservice.adapter.out.polling.IcecastPollingAdapter;
import com.morales.radiostatsservice.adapter.out.polling.ShoutcastPollingAdapter;
import com.morales.radiostatsservice.domain.model.ServerType;
import com.morales.radiostatsservice.domain.port.out.ServerPollingPort;
import com.morales.radiostatsservice.domain.service.ListServersService;
import com.morales.radiostatsservice.domain.service.RegisterServerService;
import com.morales.radiostatsservice.domain.service.ServerDashboardService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Configuration
public class ApplicationConfig {

    @Bean
    public IcecastPollingAdapter icecastPollingAdapter(RestClient restClient, ObjectMapper objectMapper) {
        return new IcecastPollingAdapter(restClient, objectMapper);
    }

    @Bean
    public ShoutcastPollingAdapter shoutcastPollingAdapter(RestClient restClient, ObjectMapper objectMapper) {
        return new ShoutcastPollingAdapter(restClient, objectMapper);
    }

    @Bean
    public DispatchingPollingAdapter dispatchingPollingAdapter(
            IcecastPollingAdapter icecastAdapter,
            ShoutcastPollingAdapter shoutcastAdapter) {
        Map<ServerType, ServerPollingPort> adapters = Map.of(
                ServerType.ICECAST, icecastAdapter,
                ServerType.SHOUTCAST_V2, shoutcastAdapter,
                ServerType.SHOUTCAST_V1, shoutcastAdapter
        );
        return new DispatchingPollingAdapter(adapters);
    }

    @Bean
    public ServerDashboardService serverDashboardService(
            StreamingServerJpaAdapter streamingServerJpaAdapter,
            DispatchingPollingAdapter dispatchingPollingAdapter) {
        return new ServerDashboardService(streamingServerJpaAdapter, dispatchingPollingAdapter);
    }

    @Bean
    public RegisterServerService registerServerService(StreamingServerJpaAdapter streamingServerJpaAdapter) {
        return new RegisterServerService(streamingServerJpaAdapter);
    }

    @Bean
    public ListServersService listServersService(StreamingServerJpaAdapter streamingServerJpaAdapter) {
        return new ListServersService(streamingServerJpaAdapter);
    }
}
