package com.morales.radiostatsservice.adapter.out.polling;

import com.morales.radiostatsservice.domain.model.ServerStatus;
import com.morales.radiostatsservice.domain.model.ServerType;
import com.morales.radiostatsservice.domain.model.StreamingServer;
import com.morales.radiostatsservice.domain.port.out.ServerPollingPort;

import java.util.Map;

public class DispatchingPollingAdapter implements ServerPollingPort {

    private final Map<ServerType, ServerPollingPort> pollingAdapters;

    public DispatchingPollingAdapter(Map<ServerType, ServerPollingPort> pollingAdapters) {
        this.pollingAdapters = pollingAdapters;
    }

    @Override
    public ServerStatus poll(StreamingServer server) {
        ServerPollingPort adapter = pollingAdapters.get(server.serverType());
        if (adapter == null) {
            throw new IllegalStateException("No polling adapter found for server type: " + server.serverType());
        }
        return adapter.poll(server);
    }
}
