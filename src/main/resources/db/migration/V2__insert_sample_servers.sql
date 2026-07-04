-- DEV SEED: excluded from production via spring.flyway.locations override (see application-prod.yaml)
INSERT INTO streaming_server (name, server_type, host, port, config_order, credentials_ref)
VALUES
    ('Main Icecast Stream', 'ICECAST',      'stream.example.com', 8000, 1, NULL),
    ('Backup Shoutcast v2', 'SHOUTCAST_V2', 'backup.example.com', 8001, 2, NULL),
    ('Legacy Shoutcast v1', 'SHOUTCAST_V1', 'legacy.example.com', 8000, 3, NULL);
