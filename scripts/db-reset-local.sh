#!/usr/bin/env bash
set -euo pipefail

echo "This will remove local Docker volumes for the project."
echo "Use only for local development data that can be discarded."

docker compose down -v
docker compose up -d mysql redis minio
