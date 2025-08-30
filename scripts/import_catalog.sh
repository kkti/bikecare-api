#!/usr/bin/env bash
set -euo pipefail

CSV_PATH="${1:-samples/catalog_components.csv}"

# DB připojení (můžeš přepsat envy)
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-${BC_DB_USER:-bikecare}}"
PGDATABASE="${PGDATABASE:-${BC_DB_NAME:-bikecaredb}}"
PGPASSWORD="${PGPASSWORD:-${BC_DB_PASS:-bikecare}}"
export PGPASSWORD

echo "== Import CSV =="
echo "CSV: $CSV_PATH"
echo "DB : $PGUSER@$PGHOST:$PGPORT/$PGDATABASE"

# 1) staging tabulka (pro jistotu – kdyby migrace ještě neběžela)
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -v ON_ERROR_STOP=1 -f - <<'SQL'
CREATE TABLE IF NOT EXISTS stg_bike_model_component (
  brand TEXT NOT NULL,
  model TEXT NOT NULL,
  "year" INT  NOT NULL,
  category TEXT,
  component_type TEXT NOT NULL,
  component_name TEXT NOT NULL,
  default_lifespan_km DOUBLE PRECISION NULL
);
SQL

# 2) nahraj CSV do stagingu
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -v csv="$CSV_PATH" <<'SQL'
\set ON_ERROR_STOP on
TRUNCATE TABLE stg_bike_model_component;
\copy stg_bike_model_component (brand,model,year,category,component_type,component_name,default_lifespan_km) FROM :'csv' WITH (FORMAT csv, HEADER true);
SQL

# 3) spusť merge do produkčních tabulek
MERGE_SQL="$(cd "$(dirname "${BASH_SOURCE[0]}")"/.. && pwd)/src/main/resources/sql/catalog_merge.sql"
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -v ON_ERROR_STOP=1 -f "$MERGE_SQL"

echo "✅ Hotovo. Ukázka výpisu:"
psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -c "
SELECT bm.brand, bm.model, bm.year, bc.code AS category, ct.name AS type, bmc.name AS component
FROM bike_model_component bmc
JOIN bike_model bm ON bm.id=bmc.bike_model_id
JOIN component_type ct ON ct.id=bmc.component_type_id
LEFT JOIN bike_category bc ON bc.id=bm.category_id
ORDER BY bm.brand, bm.model, ct.name;"
