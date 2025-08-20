CREATE TABLE IF NOT EXISTS app_user (
                                        id         BIGSERIAL PRIMARY KEY,
                                        email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(32)  NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS bike (
                                    id         BIGSERIAL PRIMARY KEY,
                                    owner_id   BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    brand      VARCHAR(100),
    model      VARCHAR(100),
    type       VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );
