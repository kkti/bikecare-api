-- Ensures column exists even if older DB was created without it
ALTER TABLE component_type
    ADD COLUMN IF NOT EXISTS default_service_interval NUMERIC(10,1);
