package com.morales.radiostatsservice.domain.port.in;

import com.morales.radiostatsservice.domain.model.StreamingServer;

import java.util.List;

public interface ListServersUseCase {
    List<StreamingServer> listAll();
}
