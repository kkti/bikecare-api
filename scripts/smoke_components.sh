#!/usr/bin/env bash
set -euo pipefail

JQ=${JQ:-jq}

base() { echo "http://localhost:8080"; }

email="it+$RANDOM@example.com"
pass="test1234"

say() { printf "\n\033[1;36m%s\033[0m\n" "$*"; }

# 1) Register
say "1) Register $email"
curl -s -X POST "$(base)/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$email\",\"password\":\"$pass\"}" | ${JQ} . || true

# 2) Login -> TOKEN
say "2) Login"
TOKEN=$(
  curl -s -X POST "$(base)/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"$pass\"}" | ${JQ} -r .token
)
echo "TOKEN len: ${#TOKEN}"

auth=(-H "Authorization: Bearer $TOKEN")

# 3) Create bike
say "3) Create bike"
BIKE_JSON=$(curl -s -X POST "$(base)/api/bikes" \
  "${auth[@]}" -H "Content-Type: application/json" \
  -d '{"name":"Defy","brand":"Giant","model":"Advanced"}')
echo "$BIKE_JSON" | ${JQ} .
BIKE_ID=$(echo "$BIKE_JSON" | ${JQ} -r .id)

# 4) Create component (override lifespan -> nejsme závislí na component_type seed)
say "4) Create component"
COMP_JSON=$(curl -s -X POST "$(base)/api/bikes/$BIKE_ID/components" \
  "${auth[@]}" -H "Content-Type: application/json" \
  -d '{"typeKey":"chain","position":"REAR","installedOdometerKm":1200,"lifespanOverride":2500,"price":450,"currency":"CZK"}')
echo "$COMP_JSON" | ${JQ} .
COMP_ID=$(echo "$COMP_JSON" | ${JQ} -r .id)

# 5) GET with metrics/status (warnAt=75 -> 76% => WARN)
say "5) Get component with metrics/status"
curl -s "$(base)/api/bikes/$BIKE_ID/components/$COMP_ID?currentOdometerKm=3100&warnAt=75&criticalAt=95" \
  "${auth[@]}" | ${JQ} .

# 6) PATCH (změna label/price)
say "6) PATCH component (label+price)"
PATCHED=$(curl -s -X PATCH "$(base)/api/bikes/$BIKE_ID/components/$COMP_ID" \
  "${auth[@]}" -H "Content-Type: application/json" \
  -d '{"label":"HG701-12","price":499}')
echo "$PATCHED" | ${JQ} .

# 7) Soft delete (DELETE)
say "7) Soft delete"
curl -s -X DELETE "$(base)/api/bikes/$BIKE_ID/components/$COMP_ID" \
  "${auth[@]}" -H "Content-Type: application/json" \
  -d '{"odometerKm":3200}' -o /dev/null -w "HTTP:%{http_code}\n"

say "   List activeOnly=true (mělo by být prázdné)"
curl -s "$(base)/api/bikes/$BIKE_ID/components?activeOnly=true" "${auth[@]}" | ${JQ} .

say "   List activeOnly=false (komponenta s removedAt != null)"
curl -s "$(base)/api/bikes/$BIKE_ID/components?activeOnly=false" "${auth[@]}" | ${JQ} .

# 8) Restore
say "8) Restore"
curl -s -X POST "$(base)/api/bikes/$BIKE_ID/components/$COMP_ID/restore" \
  "${auth[@]}" -H "Content-Type: application/json" -d '{}' -o /dev/null -w "HTTP:%{http_code}\n"

say "   List activeOnly=true (komponenta zpět)"
curl -s "$(base)/api/bikes/$BIKE_ID/components?activeOnly=true" "${auth[@]}" | ${JQ} .

# 9) Hard delete
say "9) Hard delete"
curl -s -X DELETE "$(base)/api/bikes/$BIKE_ID/components/$COMP_ID/hard" \
  "${auth[@]}" -H "Content-Type: application/json" -d '{}' -o /dev/null -w "HTTP:%{http_code}\n"

say "   List activeOnly=false (už by měla zmizet)"
curl -s "$(base)/api/bikes/$BIKE_ID/components?activeOnly=false" "${auth[@]}" | ${JQ} .

say "✅ Done"
