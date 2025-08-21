#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="${EMAIL:-me$RANDOM@example.com}"
PASSWORD="${PASSWORD:-pass}"

echo "== 0) OpenAPI check =="
curl -s "$BASE_URL/v3/api-docs" | jq -r '.openapi'

echo
echo "== 1) Register/login =="
TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | jq -r .token || true)

if [[ -z "${TOKEN:-}" || "$TOKEN" == "null" ]]; then
  echo "No account, registering..."
  TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | jq -r .token)
fi

echo "TOKEN length: ${#TOKEN}"

echo
echo "== 2) Component types (JWT) =="
curl -s "$BASE_URL/api/component-types" -H "Authorization: Bearer $TOKEN" | jq '.[0:5]'

echo
echo "== 3) Create or get a bike =="
BIKES_JSON=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/bikes")
if echo "$BIKES_JSON" | jq -e 'type=="array"' >/dev/null; then
  BIKE_ID=$(echo "$BIKES_JSON" | jq -r '.[0].id // empty')
else
  BIKE_ID=$(echo "$BIKES_JSON" | jq -r '.content[0].id // empty')
fi
if [[ -z "${BIKE_ID:-}" || "$BIKE_ID" == "null" ]]; then
  BIKE_ID=$(curl -s -X POST "$BASE_URL/api/bikes" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"Trail","brand":"Canyon","model":"Neuron","type":"MTB"}' | jq -r .id)
fi
echo "BIKE_ID=$BIKE_ID"

echo
echo "== 4) Install a component (chain) =="
COMP_JSON=$(curl -s -X POST "$BASE_URL/api/bikes/$BIKE_ID/components" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"typeKey":"chain","position":"REAR","installedOdometerKm":1200,"lifespanOverride":2500,"price":450,"currency":"CZK"}')
echo "$COMP_JSON" | jq
COMP_ID=$(echo "$COMP_JSON" | jq -r .id)
echo "COMP_ID=$COMP_ID"

echo
echo "== 5) Soft DELETE -> RESTORE -> Hard DELETE =="
curl -si -X DELETE "$BASE_URL/api/bikes/$BIKE_ID/components/$COMP_ID" \
  -H "Authorization: Bearer $TOKEN" | sed -n '1,10p'
curl -si -X POST "$BASE_URL/api/bikes/$BIKE_ID/components/$COMP_ID/restore" \
  -H "Authorization: Bearer $TOKEN" | sed -n '1,10p'
curl -si -X DELETE "$BASE_URL/api/bikes/$BIKE_ID/components/$COMP_ID?hard=true" \
  -H "Authorization: Bearer $TOKEN" | sed -n '1,10p'

echo
echo "Done."
