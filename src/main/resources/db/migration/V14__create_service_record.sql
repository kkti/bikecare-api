-- Servisní záznamy (MVP)
CREATE TABLE IF NOT EXISTS service_record (
  id              BIGSERIAL PRIMARY KEY,
  bike_id         BIGINT          NOT NULL,
  component_id    BIGINT          NULL,
  performed_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),
  description     TEXT            NOT NULL,
  km_at_service   NUMERIC(12,2)   NULL,
  cost            NUMERIC(12,2)   NULL,
  currency        VARCHAR(3)      NOT NULL DEFAULT 'CZK',
  note            TEXT            NULL,
  created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Indexy pro rychlé listování a filtrování
CREATE INDEX IF NOT EXISTS ix_service_record_bike_id_performed_at
  ON service_record(bike_id, performed_at DESC);

CREATE INDEX IF NOT EXISTS ix_service_record_component_id
  ON service_record(component_id);
