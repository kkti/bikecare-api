INSERT INTO component_type (key, name, unit, default_lifespan, default_service_interval)
VALUES ('chain','chain','KM',3000,500)
ON CONFLICT (key) DO UPDATE
SET name = EXCLUDED.name,
    unit = EXCLUDED.unit,
    default_lifespan = EXCLUDED.default_lifespan,
    default_service_interval = EXCLUDED.default_service_interval;
