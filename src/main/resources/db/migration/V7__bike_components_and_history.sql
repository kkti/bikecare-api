-- V7__bike_components_and_history.sql
-- Idempotentní úklid a doplnění tabulek pro historii komponent.

-- 1) Smaž staré/špatné názvy, pokud by tam náhodou byly
DROP TABLE IF EXISTS bikes CASCADE;
DROP TABLE IF EXISTS bike_components CASCADE;

-- 2) Vytvoř history tabulky jen pokud ještě nejsou
CREATE TABLE IF NOT EXISTS component_event (
                                               id            BIGSERIAL PRIMARY KEY,
                                               component_id  BIGINT NOT NULL REFERENCES bike_component(id),
    bike_id       BIGINT NOT NULL REFERENCES bike(id),
    user_id       BIGINT NOT NULL REFERENCES app_user(id),
    event_type    TEXT   NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    odometer_km   NUMERIC(10,2),
    price         NUMERIC(10,2),
    currency      TEXT,
    note          TEXT
    );

CREATE INDEX IF NOT EXISTS idx_component_event_component
    ON component_event(component_id);

CREATE TABLE IF NOT EXISTS component_history (
                                                 id            BIGSERIAL PRIMARY KEY,
                                                 component_id  BIGINT NOT NULL REFERENCES bike_component(id),
    bike_id       BIGINT NOT NULL REFERENCES bike(id),
    user_id       BIGINT NOT NULL REFERENCES app_user(id),
    recorded_at   TIMESTAMP NOT NULL DEFAULT now(),
    odometer_km   NUMERIC(10,2) NOT NULL,
    note          TEXT
    );

CREATE INDEX IF NOT EXISTS idx_component_history_component
    ON component_history(component_id);
