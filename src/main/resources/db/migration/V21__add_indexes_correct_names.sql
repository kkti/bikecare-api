CREATE INDEX IF NOT EXISTS idx_bike_components_bike_removed
  ON bike_components (bike_id, removed_at);

CREATE INDEX IF NOT EXISTS idx_bike_components_bike_active
  ON bike_components (bike_id)
  WHERE removed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_component_event_component_time
  ON component_event (component_id, at_time);

CREATE INDEX IF NOT EXISTS idx_component_event_bike_time
  ON component_event (bike_id, at_time);

CREATE INDEX IF NOT EXISTS idx_odometer_bike_date
  ON odometer (bike_id, at_date);

CREATE INDEX IF NOT EXISTS idx_bike_owner
  ON bike (owner_id);
