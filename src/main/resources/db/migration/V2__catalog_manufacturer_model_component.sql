BEGIN;

-- Výrobce kol i komponent (rozlišení přes kind)
CREATE TABLE IF NOT EXISTS manufacturer (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(120) NOT NULL UNIQUE,
  kind VARCHAR(16) NOT NULL DEFAULT 'BOTH' CHECK (kind IN ('BIKE','COMPONENT','BOTH')),
  country_code VARCHAR(2),
  website TEXT
);

-- Model kola u výrobce
CREATE TABLE IF NOT EXISTS bike_model (
  id BIGSERIAL PRIMARY KEY,
  manufacturer_id BIGINT NOT NULL REFERENCES manufacturer(id) ON DELETE CASCADE,
  name VARCHAR(120) NOT NULL,
  model_year INTEGER,
  bike_type VARCHAR(32),
  UNIQUE (manufacturer_id, name, model_year)
);
CREATE INDEX IF NOT EXISTS ix_bike_model_brand ON bike_model(manufacturer_id);

-- Katalogová specifikace komponenty
CREATE TABLE IF NOT EXISTS component_spec (
  id BIGSERIAL PRIMARY KEY,
  manufacturer_id BIGINT REFERENCES manufacturer(id) ON DELETE SET NULL,
  type_key VARCHAR(64) NOT NULL REFERENCES component_type(key),
  name VARCHAR(160) NOT NULL,
  position VARCHAR(16),
  default_lifespan NUMERIC(12,1),
  default_service_interval NUMERIC(12,1),
  unit VARCHAR(16),
  UNIQUE (manufacturer_id, name, type_key, position)
);
CREATE INDEX IF NOT EXISTS ix_component_spec_type ON component_spec(type_key);

-- Vazba: jaké komponenty má daný model kola
CREATE TABLE IF NOT EXISTS model_component_map (
  bike_model_id BIGINT NOT NULL REFERENCES bike_model(id) ON DELETE CASCADE,
  component_spec_id BIGINT NOT NULL REFERENCES component_spec(id) ON DELETE CASCADE,
  quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
  is_required BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (bike_model_id, component_spec_id)
);

COMMIT;
