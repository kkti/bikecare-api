-- Vytvoření tabulky odometer
CREATE TABLE IF NOT EXISTS odometer (
  id       BIGSERIAL PRIMARY KEY,
  bike_id  BIGINT      NOT NULL,
  at_date  DATE        NOT NULL,
  km       NUMERIC(12,2) NOT NULL
);

-- 1 zápis za den a kolo
CREATE UNIQUE INDEX IF NOT EXISTS ux_odometer_bike_date
  ON odometer(bike_id, at_date);

-- rychlé dotazy na poslední stav
CREATE INDEX IF NOT EXISTS ix_odometer_bike_date_desc
  ON odometer(bike_id, at_date DESC);
