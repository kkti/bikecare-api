-- Add 'cost' column expected by JPA; keep old 'price' for zpětnou kompatibilitu
ALTER TABLE service_record
  ADD COLUMN IF NOT EXISTS cost NUMERIC(12,2);

-- Pokud existuje starší 'price', předvyplň cost z price
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'service_record' AND column_name = 'price'
  )
  THEN
    UPDATE service_record SET cost = price WHERE cost IS NULL;
  END IF;
END$$;

-- Sloupec currency by už měl být, ale pro jistotu:
ALTER TABLE service_record
  ADD COLUMN IF NOT EXISTS currency VARCHAR(3);
