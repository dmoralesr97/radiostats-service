package com.morales.radiostatsservice.domain.port.out;

import com.morales.radiostatsservice.domain.model.ServerStatus;
import com.morales.radiostatsservice.domain.model.StreamingServer;

public interface ServerPollingPort {
    ServerStatus poll(StreamingServer server);
}
