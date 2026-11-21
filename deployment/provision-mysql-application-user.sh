#!/usr/bin/env bash
set -euo pipefail

# Run on the SSM-managed application host after the ApplicationSecrets stack
# is deployed. Both passwords remain in process environment only and are never
# printed. The CDK password generators exclude SQL quote characters.
MYSQL_HOST="sunridge-dev-mysql.c96gmq002ust.us-east-2.rds.amazonaws.com"
AWS_REGION="us-east-2"

ADMIN_SECRET="$(aws secretsmanager get-secret-value \
  --secret-id sunridge-dev/rds/mysql/admin \
  --region "$AWS_REGION" \
  --query SecretString \
  --output text)"

APPLICATION_SECRET="$(aws secretsmanager get-secret-value \
  --secret-id sunridge-dev/app/mysql \
  --region "$AWS_REGION" \
  --query SecretString \
  --output text)"

ADMIN_USER="$(printf '%s' "$ADMIN_SECRET" | python3 -c 'import json,sys; print(json.load(sys.stdin)["username"])')"
ADMIN_PASSWORD="$(printf '%s' "$ADMIN_SECRET" | python3 -c 'import json,sys; print(json.load(sys.stdin)["password"])')"
APPLICATION_USER="$(printf '%s' "$APPLICATION_SECRET" | python3 -c 'import json,sys; print(json.load(sys.stdin)["username"])')"
APPLICATION_PASSWORD="$(printf '%s' "$APPLICATION_SECRET" | python3 -c 'import json,sys; print(json.load(sys.stdin)["password"])')"

SQL="CREATE USER IF NOT EXISTS '${APPLICATION_USER}'@'%' IDENTIFIED BY '${APPLICATION_PASSWORD}';
ALTER USER '${APPLICATION_USER}'@'%' IDENTIFIED BY '${APPLICATION_PASSWORD}';
GRANT ALL PRIVILEGES ON sunridge_commerce.* TO '${APPLICATION_USER}'@'%';
GRANT ALL PRIVILEGES ON sunridge_warehouse.* TO '${APPLICATION_USER}'@'%';
GRANT ALL PRIVILEGES ON sunridge_payment.* TO '${APPLICATION_USER}'@'%';
GRANT ALL PRIVILEGES ON sunridge_notification.* TO '${APPLICATION_USER}'@'%';"

sudo docker run --rm \
  -e MYSQL_PWD="$ADMIN_PASSWORD" \
  mysql:8.4 \
  mysql \
    --host="$MYSQL_HOST" \
    --port=3306 \
    --ssl-mode=REQUIRED \
    --user="$ADMIN_USER" \
    --execute="$SQL"

unset ADMIN_SECRET ADMIN_USER ADMIN_PASSWORD APPLICATION_SECRET APPLICATION_PASSWORD
echo "MySQL application user provisioned for the four Sunridge service databases."
