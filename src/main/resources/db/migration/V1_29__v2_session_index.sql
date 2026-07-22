-- last accepted packet index per mac+session+channel (LIVE / HISTORY)
CREATE TABLE IF NOT EXISTS v2_session_index (
    mac             VARCHAR(12)  NOT NULL,
    session_number  INTEGER      NOT NULL,
    channel         VARCHAR(16)  NOT NULL,
    last_index      INTEGER      NOT NULL,
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (mac, session_number, channel)
);
