package com.morales.radiostatsservice.domain.port.in;

import com.morales.radiostatsservice.domain.model.ServerStatus;

import java.util.List;

public interface GetServerDashboardUseCase {
    List<ServerStatus> execute();
}
