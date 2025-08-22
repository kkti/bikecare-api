-- V8: create bike_components table (idempotentně) + indexy + FK
-- Nejdřív bez FK (některé DB mají 'bike' místo 'bikes')

CREATE TABLE IF NOT EXISTS bike_components (
                                               id                      BIGSERIAL PRIMARY KEY,
                                               bike_id                 BIGINT NOT NULL,
                                               type_key                VARCHAR(64)  NOT NULL,
    type_name               VARCHAR(128) NOT NULL,
    label                   VARCHAR(128),
    position                VARCHAR(16)  NOT NULL,
    installed_at            TIMESTAMPTZ  NOT NULL,
    installed_odometer_km   NUMERIC(12,1),
    lifespan_override       NUMERIC(12,1),
    price                   NUMERIC(12,2),
    currency                VARCHAR(8),
    shop                    VARCHAR(255),
    receipt_photo_url       VARCHAR(512),
    removed_at              TIMESTAMPTZ
    );

-- Indexy
CREATE INDEX IF NOT EXISTS idx_bike_components_bike
    ON bike_components(bike_id);

CREATE INDEX IF NOT EXISTS idx_bike_components_bike_active
    ON bike_components(bike_id, removed_at);

CREATE INDEX IF NOT EXISTS idx_bike_components_type
    ON bike_components(type_key);

-- Podmíněně přidej FK na správnou tabulku kol (bikes nebo bike)
DO $$
DECLARE
bikes_exists BOOLEAN;
    bike_exists  BOOLEAN;
BEGIN
SELECT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'bikes'
) INTO bikes_exists;

SELECT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'bike'
) INTO bike_exists;

-- Odeber případné zbytky constraintů (bez IF EXISTS ADD není v PG)
IF EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        WHERE t.relname = 'bike_components' AND c.conname = 'fk_bike_components_bike'
    ) THEN
ALTER TABLE bike_components DROP CONSTRAINT fk_bike_components_bike;
END IF;

    IF EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        WHERE t.relname = 'bike_components' AND c.conname = 'fk_bike_components_bike_singular'
    ) THEN
ALTER TABLE bike_components DROP CONSTRAINT fk_bike_components_bike_singular;
END IF;

    IF bikes_exists THEN
ALTER TABLE bike_components
    ADD CONSTRAINT fk_bike_components_bike
        FOREIGN KEY (bike_id) REFERENCES bikes(id) ON DELETE CASCADE;
ELSIF bike_exists THEN
ALTER TABLE bike_components
    ADD CONSTRAINT fk_bike_components_bike_singular
        FOREIGN KEY (bike_id) REFERENCES bike(id) ON DELETE CASCADE;
ELSE
        RAISE NOTICE 'Tabulka bikes/bike neexistuje – FK nebude vytvořen (sloupec bike_id zůstane bez omezení).';
END IF;
END$$;

-- FK z component_event.component_id na bike_components.id (pokud ještě není)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        WHERE c.conname = 'fk_component_event_component'
          AND t.relname = 'component_event'
    ) THEN
ALTER TABLE IF EXISTS component_event
    ADD CONSTRAINT fk_component_event_component
    FOREIGN KEY (component_id) REFERENCES bike_components(id) ON DELETE CASCADE
            NOT VALID;
        RAISE NOTICE 'FK fk_component_event_component added as NOT VALID (orphan events allowed temporarily).';
END IF;
END$$;


-- Index pro historii událostí
CREATE INDEX IF NOT EXISTS idx_component_event_component_time
    ON component_event(component_id, at_time);
