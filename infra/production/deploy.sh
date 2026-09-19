#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 2 ]]; then
  echo "사용법: deploy.sh <server-image> <web-image>" >&2
  exit 64
fi

readonly SERVER_IMAGE="$1"
readonly WEB_IMAGE="$2"
readonly APP_DIR=/opt/dacare
readonly RUNTIME_DIR="$APP_DIR/runtime"
readonly VAULT_SECRET_OCID_FILE="$APP_DIR/vault-secret-ocid"

if [[ ! -r "$VAULT_SECRET_OCID_FILE" ]]; then
  echo "Vault secret OCID가 설정되지 않았습니다." >&2
  exit 65
fi

mkdir -p "$RUNTIME_DIR"
NEXT_ENV="$(mktemp "$RUNTIME_DIR/app.env.XXXXXX")"
cleanup() { rm -f "$NEXT_ENV"; }
trap cleanup EXIT

oci secrets secret-bundle get \
  --auth instance_principal \
  --secret-id "$(<"$VAULT_SECRET_OCID_FILE")" \
  --query 'data."secret-bundle-content".content' \
  --raw-output | base64 --decode > "$NEXT_ENV"

set -a
source "$NEXT_ENV"
set +a
: "${GHCR_USERNAME:?GHCR_USERNAME is required in Vault secret}"
: "${GHCR_TOKEN:?GHCR_TOKEN is required in Vault secret}"
: "${APP_DOMAIN:?APP_DOMAIN is required in Vault secret}"
printf '%s' "$GHCR_TOKEN" | docker login ghcr.io --username "$GHCR_USERNAME" --password-stdin
grep -vE '^(GHCR_USERNAME|GHCR_TOKEN)=' "$NEXT_ENV" > "$RUNTIME_DIR/app.env"
chmod 600 "$RUNTIME_DIR/app.env"

DEPLOY_ENV="$RUNTIME_DIR/deploy.env"
PREVIOUS_ENV="$RUNTIME_DIR/deploy.env.previous"
[[ -f "$DEPLOY_ENV" ]] && cp "$DEPLOY_ENV" "$PREVIOUS_ENV"
{
  printf 'SERVER_IMAGE=%s\n' "$SERVER_IMAGE"
  printf 'WEB_IMAGE=%s\n' "$WEB_IMAGE"
  printf 'APP_DOMAIN=%s\n' "$APP_DOMAIN"
} > "$DEPLOY_ENV"

rollback() {
  if [[ -f "$PREVIOUS_ENV" ]]; then
    cp "$PREVIOUS_ENV" "$DEPLOY_ENV"
    docker compose --project-directory "$RUNTIME_DIR" --env-file "$DEPLOY_ENV" up -d --remove-orphans
  fi
}

if ! docker compose --project-directory "$RUNTIME_DIR" --env-file "$DEPLOY_ENV" pull || \
   ! docker compose --project-directory "$RUNTIME_DIR" --env-file "$DEPLOY_ENV" up -d --remove-orphans || \
   ! curl --fail --silent --show-error --retry 12 --retry-delay 5 \
      --resolve "$APP_DOMAIN:443:127.0.0.1" "https://$APP_DOMAIN/actuator/health"; then
  rollback
  exit 1
fi

cp "$DEPLOY_ENV" "$PREVIOUS_ENV"
