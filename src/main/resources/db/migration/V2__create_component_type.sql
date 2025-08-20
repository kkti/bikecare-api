CREATE TABLE IF NOT EXISTS component_type (
                                              id BIGSERIAL PRIMARY KEY,
                                              key  VARCHAR(64)  NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    default_lifespan NUMERIC(10,1),          -- přesné číslo, sedí na BigDecimal
    default_service_interval NUMERIC(10,1),  -- CHYBĚJÍCÍ SLOUPEC (doplnit)
    unit VARCHAR(16) NOT NULL
    );
