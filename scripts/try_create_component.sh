#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

if [[ -z "${TOKEN:-}" ]]; then
  echo "Chybí \$TOKEN (JWT). Nastav ho: export TOKEN='...'"
  exit 1
fi
AUTH=(-H "Authorization: Bearer ${TOKEN}")

# Získat BIKE_ID (první v seznamu), nebo použít z env
BIKE_ID="${BIKE_ID:-}"
if [[ -z "${BIKE_ID}" ]]; then
  if command -v jq >/dev/null 2>&1; then
    BIKE_ID="$(curl -sS "${BASE_URL}/api/bikes" "${AUTH[@]}" | jq -r '.[0].id // empty')"
  else
    BIKE_ID="$(curl -sS "${BASE_URL}/api/bikes" "${AUTH[@]}" | sed -n 's/.*"id":[ ]*\([0-9]\+\).*/\1/p' | head -n1)"
  fi
fi

if [[ -z "${BIKE_ID}" ]]; then
  echo "Nenalezeno žádné kolo. Napřed nějaké vytvoř (POST ${BASE_URL}/api/bikes)."
  echo "Příklad (možná uprav schema):"
  echo "  curl -X POST ${BASE_URL}/api/bikes -H 'Content-Type: application/json' \"\${AUTH[@]}\" -d '{\"name\":\"Test Bike\"}'"
  exit 1
fi
echo "Používám BIKE_ID=${BIKE_ID}"

echo "==[A] Minimal payload (name+type)=="
curl -v -sS -w '\nHTTP_CODE:%{http_code}\n' \
  -X POST "${BASE_URL}/api/bikes/${BIKE_ID}/components" \
  -H 'Content-Type: application/json' \
  "${AUTH[@]}" \
  -d '{"name":"Řetěz","type":"CHAIN"}'

echo
echo "==[B] Rozšířený payload (installedAt, initialKm)=="
NOW="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
curl -v -sS -w '\nHTTP_CODE:%{http_code}\n' \
  -X POST "${BASE_URL}/api/bikes/${BIKE_ID}/components" \
  -H 'Content-Type: application/json' \
  "${AUTH[@]}" \
  -d "{\"name\":\"Řetěz\",\"type\":\"CHAIN\",\"installedAt\":\"${NOW}\",\"initialKm\":123.45}"
