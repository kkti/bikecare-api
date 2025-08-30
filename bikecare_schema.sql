-- BikeCare minimal schema (PostgreSQL)
-- DB: bikecaredb
-- Core tabulky, bez seedů/importů, bez triggerů. Lze pouštět opakovaně.

BEGIN;

-- === USERS ===
CREATE TABLE IF NOT EXISTS app_user (
  id         BIGSERIAL PRIMARY KEY,
  email      VARCHAR(255) NOT NULL UNIQUE,
  password   VARCHAR(255) NOT NULL,
  role       VARCHAR(32)  NOT NULL DEFAULT 'USER',
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- === BIKES ===
CREATE TABLE IF NOT EXISTS bike (
  id         BIGSERIAL PRIMARY KEY,
  owner_id   BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  name       VARCHAR(100) NOT NULL,
  brand      VARCHAR(100),
  model      VARCHAR(100),
  type       VARCHAR(50),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_bike_owner ON bike(owner_id);

-- === COMPONENT TYPES (bez seedů) ===
CREATE TABLE IF NOT EXISTS component_type (
  id BIGSERIAL PRIMARY KEY,
  key  VARCHAR(64)  NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  default_lifespan NUMERIC(10,1),
  default_service_interval NUMERIC(10,1),
  unit VARCHAR(16) NOT NULL
);

-- === BIKE COMPONENTS (odpovídá @Table(name="bike_components")) ===
CREATE TABLE IF NOT EXISTS bike_components (
  id                      BIGSERIAL PRIMARY KEY,
  bike_id                 BIGINT NOT NULL REFERENCES bike(id) ON DELETE CASCADE,
  type_key                VARCHAR(64)  NOT NULL,
  type_name               VARCHAR(128) NOT NULL,
  label                   VARCHAR(128),
  position                VARCHAR(16)  NOT NULL,      -- např. FRONT/REAR/LEFT/RIGHT/ANY
  installed_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
  installed_odometer_km   NUMERIC(12,1),
  lifespan_override       NUMERIC(12,1),
  price                   NUMERIC(12,2),
  currency                VARCHAR(8),
  shop                    VARCHAR(255),
  receipt_photo_url       TEXT,
  removed_at              TIMESTAMPTZ NULL
);
CREATE INDEX IF NOT EXISTS ix_bike_components_bike_active ON bike_components(bike_id, removed_at);
CREATE INDEX IF NOT EXISTS ix_bike_components_type_key    ON bike_components(type_key);

-- === ODOMETER ENTRIES ===
CREATE TABLE IF NOT EXISTS odometer_entry (
  id bigserial primary key,
  bike_id bigint not null references bike(id) on delete cascade,
  at_date date not null,
  km numeric(10,1) not null check (km >= 0),
  created_at timestamptz not null default now(),
  unique (bike_id, at_date)
);
CREATE INDEX IF NOT EXISTS idx_odo_bike_date_desc ON odometer_entry (bike_id, at_date desc);

-- === COMPONENT EVENTS (historie změn) ===
CREATE TABLE IF NOT EXISTS component_event (
  id bigserial primary key,
  component_id bigint not null references bike_components(id) on delete cascade,
  bike_id      bigint not null references bike(id) on delete cascade,
  user_id      bigint not null references app_user(id) on delete cascade,
  event_type   text not null check (event_type in ('INSTALLED','REMOVED','RESTORED','HARD_DELETED','UPDATED')),
  at_time      timestamptz not null default now(),
  odometer_km  numeric(10,2),
  note         text
);
CREATE INDEX IF NOT EXISTS idx_comp_event_component_time ON component_event (component_id, at_time desc);
CREATE INDEX IF NOT EXISTS idx_comp_event_bike_time      ON component_event (bike_id, at_time desc);
CREATE INDEX IF NOT EXISTS idx_comp_event_user_time      ON component_event (user_id, at_time desc);

COMMIT;

-- (Volitelný úklid starých importních tabulek — spusť PŘED schématem, pokud potřebuješ)
-- DO $$
-- BEGIN
--   DROP TABLE IF EXISTS bike_component, bike_model_component, catalog_component,
--                        bike_model, bike_category,
--                        stg_bike_model_component, stg_bike_model,
--                        stg_component, stg_component_type
--   CASCADE;
-- END $$;
