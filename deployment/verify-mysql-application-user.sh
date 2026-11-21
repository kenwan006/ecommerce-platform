#!/usr/bin/env bash
set -euo pipefail

MYSQL_HOST="sunridge-dev-mysql.c96gmq002ust.us-east-2.rds.amazonaws.com"
APPLICATION_SECRET="$(aws secretsmanager get-secret-value \
  --secret-id sunridge-dev/app/mysql \
  --region us-east-2 \
  --query SecretString \
  --output text)"
APPLICATION_USER="$(printf '%s' "$APPLICATION_SECRET" | python3 -c 'import json,sys; print(json.load(sys.stdin)["username"])')"
APPLICATION_PASSWORD="$(printf '%s' "$APPLICATION_SECRET" | python3 -c 'import json,sys; print(json.load(sys.stdin)["password"])')"

sudo docker run --rm \
  -e MYSQL_PWD="$APPLICATION_PASSWORD" \
  mysql:8.4 \
  mysql \
    --host="$MYSQL_HOST" \
    --port=3306 \
    --ssl-mode=REQUIRED \
    --user="$APPLICATION_USER" \
    --execute="SHOW DATABASES LIKE 'sunridge_%';"

unset APPLICATION_SECRET APPLICATION_PASSWORD
