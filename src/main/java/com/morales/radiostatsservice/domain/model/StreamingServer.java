package com.morales.radiostatsservice.domain.model;

import java.util.UUID;

public record StreamingServer(
        UUID id,
        String name,
        ServerType serverType,
        String host,
        int port,
        int configOrder,
        String credentialsRef
) {}
