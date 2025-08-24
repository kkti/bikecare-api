-- create or align service_record table (idempotent)
CREATE TABLE IF NOT EXISTS service_record (
  id           BIGSERIAL PRIMARY KEY,
  owner_id     BIGINT NOT NULL,
  bike_id      BIGINT,
  component_id BIGINT,
  happened_at  TIMESTAMPTZ NOT NULL,
  type         VARCHAR(64) NOT NULL,
  note         TEXT,
  price        NUMERIC(12,2),
  currency     VARCHAR(3),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ensure columns exist if table pre-existed without them
ALTER TABLE service_record ADD COLUMN IF NOT EXISTS owner_id     BIGINT;
ALTER TABLE service_record ADD COLUMN IF NOT EXISTS bike_id      BIGINT;
ALTER TABLE service_record ADD COLUMN IF NOT EXISTS component_id BIGINT;
ALTER TABLE service_record ADD COLUMN IF NOT EXISTS happened_at  TIMESTAMPTZ;
ALTER TABLE service_record ADD COLUMN IF NOT EXISTS type         VARCHAR(64);
ALTER TABLE service_record ADD COLUMN IF NOT EXISTS note         TEXT;
ALTER TABLE service_record ADD COLUMN IF NOT EXISTS price        NUMERIC(12,2);
ALTER TABLE service_record ADD COLUMN IF NOT EXISTS currency     VARCHAR(3);
ALTER TABLE service_record ADD COLUMN IF NOT EXISTS created_at   TIMESTAMPTZ DEFAULT NOW();

-- indexes
CREATE INDEX IF NOT EXISTS idx_sr_owner       ON service_record(owner_id);
CREATE INDEX IF NOT EXISTS idx_sr_bike        ON service_record(bike_id);
CREATE INDEX IF NOT EXISTS idx_sr_component   ON service_record(component_id);
CREATE INDEX IF NOT EXISTS idx_sr_happened_at ON service_record(happened_at);

-- foreign keys (add only if missing)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE table_name='service_record' AND constraint_name='fk_sr_bike'
  ) THEN
    ALTER TABLE service_record
      ADD CONSTRAINT fk_sr_bike
      FOREIGN KEY (bike_id) REFERENCES bike(id) ON DELETE SET NULL;
  END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE table_name='service_record' AND constraint_name='fk_sr_component'
  ) THEN
    ALTER TABLE service_record
      ADD CONSTRAINT fk_sr_component
      FOREIGN KEY (component_id) REFERENCES bike_component(id) ON DELETE SET NULL;
  END IF;
END$$;
