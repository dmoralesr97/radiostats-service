package com.morales.radiostatsservice.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ServerStatus(
        UUID serverId,
        String serverName,
        ServerType serverType,
        ServerState state,
        int listenerCount,
        Instant lastCheckedAt
) {}
