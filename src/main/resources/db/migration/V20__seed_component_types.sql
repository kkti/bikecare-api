-- Zajisti sloupec key (idempotentně)
ALTER TABLE component_type ADD COLUMN IF NOT EXISTS key VARCHAR(64) UNIQUE;

-- Základní typy komponent (idempotentně dle key)
INSERT INTO component_type (key, name, default_lifespan, default_service_interval, unit) VALUES
  ('chain',         'chain',         3000.0,  500.0,  'KM'),
  ('cassette',      'cassette',      8000.0,  1500.0, 'KM'),
  ('chainring',     'chainring',    20000.0,  4000.0, 'KM'),
  ('brake_pads',    'brake_pads',    1500.0,  300.0,  'KM'),
  ('brake_rotor',   'brake_rotor',  10000.0,  2000.0, 'KM'),
  ('tire',          'tire',          4000.0,  800.0,  'KM'),
  ('bb',            'bb',           10000.0,  2000.0, 'KM'),
  ('jockey_wheels', 'jockey_wheels', 8000.0,  1500.0, 'KM')
ON CONFLICT (key) DO NOTHING;
