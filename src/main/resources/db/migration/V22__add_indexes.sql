-- Bike components: rychlé řazení a aktivní kusy
CREATE INDEX IF NOT EXISTS idx_bike_components_bike_installedat
  ON bike_components (bike_id, installed_at DESC);

-- jen aktivní (removed_at IS NULL) – partial index
CREATE INDEX IF NOT EXISTS idx_bike_components_bike_active
  ON bike_components (bike_id)
  WHERE removed_at IS NULL;

-- label LIKE (case-insensitive)
CREATE INDEX IF NOT EXISTS idx_bike_components_bike_lower_label
  ON bike_components (bike_id, lower(label));

-- Component event: historie v čase
CREATE INDEX IF NOT EXISTS idx_component_event_component_time
  ON component_event (component_id, at_time);

CREATE INDEX IF NOT EXISTS idx_component_event_bike_time
  ON component_event (bike_id, at_time);

-- Odometer: poslední a k datu
CREATE INDEX IF NOT EXISTS idx_odometer_bike_date
  ON odometer (bike_id, at_date);
