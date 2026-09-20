#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 3 ]]; then
  echo "사용법: deploy.sh <server-image> <web-image> <ghcr-username> (토큰은 stdin)" >&2
  exit 64
fi

readonly SERVER_IMAGE="$1"
readonly WEB_IMAGE="$2"
readonly GHCR_USERNAME="$3"
readonly APP_DIR=/opt/dacare
readonly RUNTIME_DIR="$APP_DIR/runtime"
if [[ ! -r "$RUNTIME_DIR/app.env" ]]; then
  echo "app.env가 설정되지 않았습니다." >&2
  exit 65
fi

read_env() {
  sed -n "s/^$1=//p" "$RUNTIME_DIR/app.env" | head -n 1
}

APP_DOMAIN="$(read_env APP_DOMAIN)"
: "${APP_DOMAIN:?APP_DOMAIN이 필요합니다}"
umask 077
export DOCKER_CONFIG
DOCKER_CONFIG="$(mktemp -d)"
trap 'rm -rf -- "$DOCKER_CONFIG"' EXIT
docker login ghcr.io --username "$GHCR_USERNAME" --password-stdin

DEPLOY_ENV="$RUNTIME_DIR/deploy.env"
PREVIOUS_ENV="$RUNTIME_DIR/deploy.env.previous"
{
  printf 'SERVER_IMAGE=%s\n' "$SERVER_IMAGE"
  printf 'WEB_IMAGE=%s\n' "$WEB_IMAGE"
  printf 'APP_DOMAIN=%s\n' "$APP_DOMAIN"
} > "$DEPLOY_ENV"

start_services() {
  docker compose --project-directory "$RUNTIME_DIR" --env-file "$DEPLOY_ENV" up -d --remove-orphans &&
    docker compose --project-directory "$RUNTIME_DIR" --env-file "$DEPLOY_ENV" up -d --no-deps --force-recreate web gateway
}

rollback() {
  if [[ -f "$PREVIOUS_ENV" ]]; then
    echo "이전 이미지로 복구합니다. DB 변경은 자동 복구하지 않습니다." >&2
    cp "$PREVIOUS_ENV" "$DEPLOY_ENV"
    start_services
  else
    rm -f "$DEPLOY_ENV"
  fi
}

if ! docker compose --project-directory "$RUNTIME_DIR" --env-file "$DEPLOY_ENV" pull || \
   ! start_services || \
   ! curl --fail --silent --show-error --retry 12 --retry-delay 5 \
      --retry-all-errors --connect-timeout 10 --max-time 20 \
      --resolve "$APP_DOMAIN:443:127.0.0.1" "https://$APP_DOMAIN/actuator/health" | \
      grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'; then
  rollback
  exit 1
fi

cp "$DEPLOY_ENV" "$PREVIOUS_ENV"
echo "배포 완료: https://$APP_DOMAIN"
