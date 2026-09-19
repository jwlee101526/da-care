#!/usr/bin/env bash
set -Eeuo pipefail

if ! command -v docker >/dev/null; then
  apt-get update
  apt-get install -y docker.io docker-compose-v2 curl
fi

systemctl enable --now docker
mkdir -p /opt/dacare/runtime /opt/dacare/bin
chmod 750 /opt/dacare /opt/dacare/runtime /opt/dacare/bin
