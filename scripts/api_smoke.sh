#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "==[1/6] Registrace uživatele (idempotentně)…"
EMAIL="test.user+bikecare@example.com"
PASS="Passw0rd!"
curl -sS -X POST "${BASE_URL}/api/auth/register" -H 'Content-Type: application/json' \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASS}\"}" >/dev/null || true
echo "Registrováno ok."

echo "==[2/6] Login → JWT…"
TOKEN="$(curl -sS -X POST "${BASE_URL}/api/auth/login" -H 'Content-Type: application/json' \
   -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASS}\"}" | jq -r '.token')"
echo "JWT získán."
AUTH=(-H "Authorization: Bearer ${TOKEN}")

echo "==[3/6] Kontrola/vytvoření kola…"
BIKE_ID="$(curl -sS "${BASE_URL}/api/bikes" "${AUTH[@]}" | jq -r '.[0].id // empty')"
if [[ -z "${BIKE_ID}" ]]; then
  BIKE_ID="$(curl -sS -X POST "${BASE_URL}/api/bikes" "${AUTH[@]}" -H 'Content-Type: application/json' \
    -d '{"name":"SmokeTest Bike"}' | jq -r '.id')"
fi
echo "BIKE_ID=${BIKE_ID}"

echo "==[4/6] Vytvoření komponenty (s installedAt+initialKm)…"
NOW="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
CREATE_JSON="$(curl -sS -X POST "${BASE_URL}/api/bikes/${BIKE_ID}/components" -H 'Content-Type: application/json' \
  "${AUTH[@]}" -d "{\"name\":\"Řetěz\",\"type\":\"CHAIN\",\"installedAt\":\"${NOW}\",\"initialKm\":0.0}")"
echo "${CREATE_JSON}" | jq .
COMP_ID="$(echo "${CREATE_JSON}" | jq -r '.id // empty')"
if [[ -z "${COMP_ID}" || "${COMP_ID}" == "null" ]]; then
  echo "Create nevrátilo id – zkontroluj prosím serverové logy"; exit 1
fi
echo "COMP_ID=${COMP_ID}"

echo "==[5/6] Detail komponenty…"
curl -sS "${BASE_URL}/api/bikes/${BIKE_ID}/components/${COMP_ID}" "${AUTH[@]}" | jq .

echo "==[6/6] Výpis komponent…"
curl -sS "${BASE_URL}/api/bikes/${BIKE_ID}/components" "${AUTH[@]}" | jq .
