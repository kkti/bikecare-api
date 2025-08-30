-- catalog_merge.sql
\set ON_ERROR_STOP on

-- 1) doplň chybějící typy komponent
INSERT INTO component_type(name)
SELECT DISTINCT INITCAP(s.component_type)
FROM stg_bike_model_component s
LEFT JOIN component_type t ON lower(t.name)=lower(s.component_type)
WHERE t.id IS NULL;

-- 2) založ/aktualizuj bike_model
INSERT INTO bike_model(brand, model, "year", category_id)
SELECT s.brand, s.model, s."year", c.id
FROM (
  SELECT DISTINCT brand, model, "year",
         COALESCE(NULLIF(trim(category),''),'OTHER') AS category
  FROM stg_bike_model_component
) s
JOIN bike_category c ON lower(c.code)=lower(s.category)
LEFT JOIN bike_model bm
  ON lower(bm.brand)=lower(s.brand)
 AND lower(bm.model)=lower(s.model)
 AND bm."year"=s."year"
WHERE bm.id IS NULL;

-- případná změna kategorie existujících modelů
UPDATE bike_model bm
SET category_id = c.id
FROM (
  SELECT DISTINCT brand, model, "year",
         COALESCE(NULLIF(trim(category),''),'OTHER') AS category
  FROM stg_bike_model_component
) s
JOIN bike_category c ON lower(c.code)=lower(s.category)
WHERE lower(bm.brand)=lower(s.brand)
  AND lower(bm.model)=lower(s.model)
  AND bm."year"=s."year"
  AND COALESCE(bm.category_id,0) <> c.id;

-- 3) vlož/aktualizuj výchozí komponenty modelů
INSERT INTO bike_model_component (bike_model_id, component_type_id, name)
SELECT bm.id, ct.id, s.component_name
FROM stg_bike_model_component s
JOIN bike_model bm
  ON lower(bm.brand)=lower(s.brand)
 AND lower(bm.model)=lower(s.model)
 AND bm."year"=s."year"
JOIN component_type ct ON lower(ct.name)=lower(s.component_type)
ON CONFLICT (bike_model_id, component_type_id)
DO UPDATE SET name = EXCLUDED.name;

-- 4) volitelně doplň default_lifespan (pokud sloupec existuje)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_name='bike_model_component' AND column_name='default_lifespan') THEN
    UPDATE bike_model_component bmc
    SET default_lifespan = s.default_lifespan_km
    FROM stg_bike_model_component s
    JOIN bike_model bm ON bm.id=bmc.bike_model_id
    JOIN component_type ct ON ct.id=bmc.component_type_id
    WHERE s.default_lifespan_km IS NOT NULL
      AND lower(bm.brand)=lower(s.brand)
      AND lower(bm.model)=lower(s.model)
      AND bm."year"=s."year"
      AND lower(ct.name)=lower(s.component_type);
  END IF;
END $$;

-- 5) hotovo (staging nechávám – můžeš si ho prohlédnout)
