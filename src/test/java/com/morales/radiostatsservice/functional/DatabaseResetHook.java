package com.morales.radiostatsservice.functional;

import io.cucumber.java.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

public class DatabaseResetHook {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before(order = 0)
    public void resetToSeedData() {
        jdbcTemplate.execute("DELETE FROM streaming_server");
        insertSeed("aaaaaaaa-0000-0000-0000-000000000001", "Test-Server-01", "ICECAST",      "test-host-1",  8000,  1);
        insertSeed("aaaaaaaa-0000-0000-0000-000000000002", "Test-Server-02", "SHOUTCAST_V2", "test-host-2",  8001,  2);
        insertSeed("aaaaaaaa-0000-0000-0000-000000000003", "Test-Server-03", "SHOUTCAST_V1", "test-host-3",  8000,  3);
        insertSeed("aaaaaaaa-0000-0000-0000-000000000004", "Test-Server-04", "ICECAST",      "test-host-4",  8000,  4);
        insertSeed("aaaaaaaa-0000-0000-0000-000000000005", "Test-Server-05", "SHOUTCAST_V2", "test-host-5",  8001,  5);
        insertSeed("aaaaaaaa-0000-0000-0000-000000000006", "Test-Server-06", "SHOUTCAST_V1", "test-host-6",  8000,  6);
        insertSeed("aaaaaaaa-0000-0000-0000-000000000007", "Test-Server-07", "ICECAST",      "test-host-7",  8000,  7);
        insertSeed("aaaaaaaa-0000-0000-0000-000000000008", "Test-Server-08", "SHOUTCAST_V2", "test-host-8",  8001,  8);
        insertSeed("aaaaaaaa-0000-0000-0000-000000000009", "Test-Server-09", "SHOUTCAST_V1", "test-host-9",  8000,  9);
        insertSeed("aaaaaaaa-0000-0000-0000-000000000010", "Test-Server-10", "ICECAST",      "test-host-10", 8000, 10);
        insertSeed("aaaaaaaa-0000-0000-0000-000000000011", "Test-Server-11", "SHOUTCAST_V2", "test-host-11", 8001, 11);
        insertSeed("aaaaaaaa-0000-0000-0000-000000000012", "Test-Server-12", "SHOUTCAST_V1", "test-host-12", 8000, 12);
    }

    private void insertSeed(String id, String name, String type, String host, int port, int configOrder) {
        jdbcTemplate.update(
            "INSERT INTO streaming_server (id, name, server_type, host, port, config_order, credentials_ref) VALUES (?, ?, ?, ?, ?, ?, NULL)",
            UUID.fromString(id), name, type, host, port, configOrder
        );
    }
}
