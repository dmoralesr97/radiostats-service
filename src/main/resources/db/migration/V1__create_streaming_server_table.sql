CREATE TABLE streaming_server (
    id              UUID         DEFAULT RANDOM_UUID() PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    server_type     VARCHAR(20)  NOT NULL,
    host            VARCHAR(255) NOT NULL,
    port            INTEGER      NOT NULL CHECK (port BETWEEN 1 AND 65535),
    config_order    INTEGER      NOT NULL,
    credentials_ref VARCHAR(255)
);

CREATE INDEX idx_streaming_server_config_order ON streaming_server (config_order ASC);
