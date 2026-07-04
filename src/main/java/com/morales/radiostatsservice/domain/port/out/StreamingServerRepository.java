package com.morales.radiostatsservice.domain.port.out;

import com.morales.radiostatsservice.domain.model.StreamingServer;

import java.util.List;

public interface StreamingServerRepository {
    List<StreamingServer> findFirstTenByConfigOrder();
    StreamingServer save(StreamingServer server);
    List<StreamingServer> findAll();
}
