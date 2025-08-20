-- enum pro pozici komponenty
DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'component_pos') THEN
            CREATE TYPE component_pos AS ENUM ('FRONT','REAR','LEFT','RIGHT');
        END IF;
    END$$;

-- komponenty na kole
CREATE TABLE IF NOT EXISTS bike_component (
                                              id                    BIGSERIAL PRIMARY KEY,
                                              bike_id               BIGINT NOT NULL REFERENCES bike(id) ON DELETE CASCADE,
                                              type_id               BIGINT NOT NULL REFERENCES component_type(id),
                                              spec_id               BIGINT NULL,
                                              label                 VARCHAR(120),
                                              position              component_pos NULL,
                                              installed_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
                                              removed_at            TIMESTAMPTZ NULL,
                                              installed_odometer_km NUMERIC(10,1),
                                              lifespan_override     NUMERIC(10,1),
                                              price                 NUMERIC(10,2),
                                              currency              VARCHAR(255),   -- Hibernate bez @Column(length=3) čeká 255
                                              shop                  TEXT,
                                              receipt_photo_url     TEXT
);

CREATE INDEX IF NOT EXISTS idx_bike_component_active
    ON bike_component(bike_id, removed_at);
