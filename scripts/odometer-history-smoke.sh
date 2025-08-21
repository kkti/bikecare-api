#!/usr/bin/env bash
set -euo pipefail

# Základní kontroly nástrojů
command -v curl >/dev/null || { echo "ERROR: curl not found"; exit 1; }
command -v jq >/dev/null   || { echo "ERROR: jq not found"; exit 1; }

BASE="${BASE:-http://localhost:8080}"

echo "== OpenAPI ==" && curl -s "$BASE/v3/api-docs" | jq -r '.openapi'

# Registrace → TOKEN
EMAIL="odo$(date +%s)@example.com"
PASSWORD="Secret123!"
TOKEN=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"fullName\":\"Odo User\"}" | jq -r .token)
echo "TOKEN: ${#TOKEN} chars"

# Vezmi existující kolo (první) nebo vytvoř
BIKES_JSON=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE/api/bikes")
BIKE_ID=$(echo "$BIKES_JSON" | jq -r 'if type=="array" then (.[0]?.id // empty) else (.content[0]?.id // empty) end')
if [ -z "${BIKE_ID:-}" ] || [ "$BIKE_ID" = "null" ]; then
  BIKE_ID=$(curl -s -X POST "$BASE/api/bikes" \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d '{"name":"Trail","brand":"Canyon","model":"Neuron","type":"MTB"}' | jq -r .id)
fi
echo "BIKE_ID=$BIKE_ID"

echo "== Odometer upsert + current =="
curl -s -X POST "$BASE/api/bikes/$BIKE_ID/odometer" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"date":"2025-08-21","km":3100}' | jq -c '{id:.id,date:.atDate,km:.km}'
curl -s "$BASE/api/bikes/$BIKE_ID/odometer/current" \
  -H "Authorization: Bearer $TOKEN" | jq -c

echo "== Install component (chain) =="
COMP_ID=$(curl -s -X POST "$BASE/api/bikes/$BIKE_ID/components" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"typeKey":"chain","position":"REAR","installedOdometerKm":1200,"lifespanOverride":2500}' | jq -r .id)
echo "COMP_ID=$COMP_ID"

echo "== History after install =="
curl -s "$BASE/api/bikes/$BIKE_ID/components/$COMP_ID/history" \
  -H "Authorization: Bearer $TOKEN" | jq '.[0:5] | map({eventType,atTime,odometerKm})'

echo "== Soft → Restore → Soft → Hard delete =="

for cmd in \
  "DELETE /api/bikes/$BIKE_ID/components/$COMP_ID" \
  "POST   /api/bikes/$BIKE_ID/components/$COMP_ID/restore" \
  "DELETE /api/bikes/$BIKE_ID/components/$COMP_ID" \
  "DELETE /api/bikes/$BIKE_ID/components/$COMP_ID?hard=true"
do
  read -r METHOD URL_PATH <<< "$cmd"
  echo "== $METHOD $URL_PATH =="
  curl -sS -v -X "$METHOD" \
    -H "Authorization: Bearer $TOKEN" -H "Accept: application/json" \
    "$BASE$URL_PATH" \
    -o >(head -c 300; echo) -w "\n[HTTP %{http_code}]\n"
  echo
done

echo "== Final history =="
curl -s "$BASE/api/bikes/$BIKE_ID/components/$COMP_ID/history" \
  -H "Authorization: Bearer $TOKEN" | jq 'map({eventType,atTime})'
