-- V9__component_event_add_user_and_bike.sql
DO $$
DECLARE
bike_table regclass :=
      COALESCE(to_regclass('public.bikes'),
               to_regclass('public.bike'));

  user_table regclass :=
      COALESCE(to_regclass('public.app_users'),
               to_regclass('public.app_user'),
               to_regclass('public.users'));
BEGIN
  -- Sloupce (idempotentně)
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema='public' AND table_name='component_event' AND column_name='user_id'
  ) THEN
    EXECUTE 'ALTER TABLE component_event ADD COLUMN user_id BIGINT';
END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema='public' AND table_name='component_event' AND column_name='bike_id'
  ) THEN
    EXECUTE 'ALTER TABLE component_event ADD COLUMN bike_id BIGINT';
END IF;

  -- Indexy (pro FK a výkon)
EXECUTE 'CREATE INDEX IF NOT EXISTS idx_component_event_user_id ON component_event(user_id)';
EXECUTE 'CREATE INDEX IF NOT EXISTS idx_component_event_bike_id ON component_event(bike_id)';

-- Pokud existují staré constrainty, zkus je shodit
IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_component_event_user') THEN
    EXECUTE 'ALTER TABLE component_event DROP CONSTRAINT fk_component_event_user';
END IF;
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_component_event_bike') THEN
    EXECUTE 'ALTER TABLE component_event DROP CONSTRAINT fk_component_event_bike';
END IF;

  -- Najdi tabulku s uživateli
  IF user_table IS NULL THEN
    RAISE EXCEPTION 'User table not found (looked for app_users/app_user/users)';
END IF;

  -- Najdi tabulku s koly
  IF bike_table IS NULL THEN
    RAISE EXCEPTION 'Bike table not found (looked for bikes/bike)';
END IF;

  -- Přidej FK (NOT VALID, ať to nespadne na případných sirotcích)
EXECUTE format(
        'ALTER TABLE component_event ADD CONSTRAINT fk_component_event_user
         FOREIGN KEY (user_id) REFERENCES %s(id) ON DELETE CASCADE NOT VALID',
        user_table::text
        );

EXECUTE format(
        'ALTER TABLE component_event ADD CONSTRAINT fk_component_event_bike
         FOREIGN KEY (bike_id) REFERENCES %s(id) ON DELETE CASCADE NOT VALID',
        bike_table::text
        );
END $$;
