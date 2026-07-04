package com.morales.radiostatsservice.domain.port.in;

import com.morales.radiostatsservice.domain.model.StreamingServer;

public interface RegisterServerUseCase {
    StreamingServer register(StreamingServer server);
}
