#!/bin/bash
set -euo pipefail

cd /opt/antenatal
set -a
. ./.env
set +a

: "${MAX_BOT_TOKEN:?MAX_BOT_TOKEN is required}"
: "${MAX_BOT_USERNAME:?MAX_BOT_USERNAME is required}"
: "${MAX_WEBHOOK_SECRET:?MAX_WEBHOOK_SECRET is required}"
: "${DOMAIN:?DOMAIN is required}"

BASE="${MAX_API_BASE_URL:-https://platform-api2.max.ru}"
WEBHOOK="https://${DOMAIN}/api/max/webhook/${MAX_WEBHOOK_SECRET}"

curl -sS -X POST "${BASE}/subscriptions" \
  -H "Authorization: ${MAX_BOT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"url\":\"${WEBHOOK}\",\"update_types\":[\"message_created\",\"bot_started\"],\"secret\":\"${MAX_WEBHOOK_SECRET}\"}"
echo

curl -sS "${BASE}/subscriptions" -H "Authorization: ${MAX_BOT_TOKEN}"
echo
