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
if [[ ! -r "$RUNTIME_DIR/app.env" ]]; then
  echo "app.env가 설정되지 않았습니다." >&2
  exit 65
fi

read_env() {
  sed -n "s/^$1=//p" "$RUNTIME_DIR/app.env" | head -n 1
}

GHCR_USERNAME="$(read_env GHCR_USERNAME)"
GHCR_TOKEN="$(read_env GHCR_TOKEN)"
APP_DOMAIN="$(read_env APP_DOMAIN)"
: "${GHCR_USERNAME:?GHCR_USERNAME이 필요합니다}"
: "${GHCR_TOKEN:?GHCR_TOKEN이 필요합니다}"
: "${APP_DOMAIN:?APP_DOMAIN이 필요합니다}"
printf '%s' "$GHCR_TOKEN" | docker login ghcr.io --username "$GHCR_USERNAME" --password-stdin
grep -vE '^(GHCR_USERNAME|GHCR_TOKEN)=' "$RUNTIME_DIR/app.env" > "$RUNTIME_DIR/app.env.next"
mv "$RUNTIME_DIR/app.env.next" "$RUNTIME_DIR/app.env"
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
