#!/usr/bin/env python3
"""Generate the untracked Docker Compose runtime.env on the EC2 host.

Run this only on an instance with the Sunridge EC2 IAM role. Values are read
directly from Secrets Manager and are never printed.
"""

import json
import os
import shlex
import subprocess
from urllib.parse import quote
from pathlib import Path

REGION = "us-east-2"


def secret(name: str) -> dict[str, str]:
    value = subprocess.check_output(
        [
            "aws",
            "secretsmanager",
            "get-secret-value",
            "--secret-id",
            name,
            "--region",
            REGION,
            "--query",
            "SecretString",
            "--output",
            "text",
        ],
        text=True,
    )
    return json.loads(value)


mysql = secret("sunridge-dev/app/mysql")
postgres = secret("sunridge-dev/rds/postgres/agent")
jwt = secret("sunridge-dev/app/jwt")
internal_api_key = secret("sunridge-dev/app/internal-api-key")
external = secret("sunridge-dev/app/external-config")

values = {
    "DB_USERNAME": mysql["username"],
    "DB_PASSWORD": mysql["password"],
    "POSTGRES_USER": postgres["username"],
    "POSTGRES_PASSWORD": postgres["password"],
    "RAG_DATABASE_URL": (
        "postgresql://"
        f"{quote(postgres['username'], safe='')}:{quote(postgres['password'], safe='')}"
        "@sunridge-dev-postgres.c96gmq002ust.us-east-2.rds.amazonaws.com:5432/"
        "sunridge_agent?sslmode=require"
    ),
    "JWT_SECRET": jwt["value"],
    "INTERNAL_API_KEY": internal_api_key["value"],
    "OPENAI_API_KEY": external.get("OPENAI_API_KEY", ""),
    "GITHUB_TOKEN": external.get("GITHUB_TOKEN", ""),
    "STRIPE_SECRET_KEY": external.get("STRIPE_SECRET_KEY", ""),
    "STRIPE_WEBHOOK_SECRET": external.get("STRIPE_WEBHOOK_SECRET", ""),
    "STRIPE_PUBLISHABLE_KEY": external.get("STRIPE_PUBLISHABLE_KEY", ""),
    "MAIL_HOST": external.get("MAIL_HOST", ""),
    "MAIL_PORT": external.get("MAIL_PORT", "587"),
    "NOTIFICATION_FROM": external.get("NOTIFICATION_FROM", ""),
    "APP_FRONTEND_URL": external.get("APP_FRONTEND_URL", ""),
}

destination = Path(__file__).with_name("runtime.env")
temporary = destination.with_suffix(".env.tmp")
with temporary.open("w", encoding="utf-8") as output:
    for key, value in values.items():
        output.write(f"{key}={shlex.quote(str(value))}\n")

os.chmod(temporary, 0o600)
temporary.replace(destination)
print(f"Wrote {destination} with restricted permissions.")
