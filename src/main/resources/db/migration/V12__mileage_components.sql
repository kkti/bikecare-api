-- Odometer pro kazde kolo
CREATE TABLE IF NOT EXISTS bike_stats (
  id BIGSERIAL PRIMARY KEY,
  bike_id BIGINT NOT NULL UNIQUE REFERENCES bike(id) ON DELETE CASCADE,
  odometer_meters BIGINT NOT NULL DEFAULT 0,
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- init pro existujici kola
INSERT INTO bike_stats (bike_id, odometer_meters)
SELECT b.id, 0
FROM bike b
LEFT JOIN bike_stats s ON s.bike_id = b.id
WHERE s.bike_id IS NULL;

-- Komponenty
CREATE TABLE IF NOT EXISTS bike_component (
  id BIGSERIAL PRIMARY KEY,
  bike_id BIGINT NOT NULL REFERENCES bike(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  category TEXT,
  expected_life_km INT NOT NULL,
  installed_at_meters BIGINT NOT NULL DEFAULT 0,
  note TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_bike_component_bike ON bike_component(bike_id);

-- Servisni zaznamy
CREATE TABLE IF NOT EXISTS service_record (
  id BIGSERIAL PRIMARY KEY,
  bike_id BIGINT NOT NULL REFERENCES bike(id) ON DELETE CASCADE,
  performed_at TIMESTAMP NOT NULL DEFAULT now(),
  odometer_meters BIGINT NOT NULL,
  note TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_service_record_bike ON service_record(bike_id);

-- trigger na updated_at (idempotentne)
CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS bike_stats_set_updated_at ON bike_stats;
CREATE TRIGGER bike_stats_set_updated_at
BEFORE UPDATE ON bike_stats
FOR EACH ROW EXECUTE PROCEDURE trg_set_updated_at();

DROP TRIGGER IF EXISTS bike_component_set_updated_at ON bike_component;
CREATE TRIGGER bike_component_set_updated_at
BEFORE UPDATE ON bike_component
FOR EACH ROW EXECUTE PROCEDURE trg_set_updated_at();
