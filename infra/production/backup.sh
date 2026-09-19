#!/usr/bin/env bash
set -Eeuo pipefail

readonly RUNTIME_DIR=/opt/dacare/runtime
readonly BACKUP_DIR=/opt/dacare/backups
mkdir -p "$BACKUP_DIR"
source "$RUNTIME_DIR/app.env"
: "${OCI_BACKUP_BUCKET:?OCI_BACKUP_BUCKET is required}"

backup_file="$BACKUP_DIR/dacare-$(date -u +%Y%m%dT%H%M%SZ).sql.gz"
docker compose --project-directory "$RUNTIME_DIR" exec -T mariadb \
  mariadb-dump -u"$MARIADB_USER" -p"$MARIADB_PASSWORD" "$MARIADB_DATABASE" | gzip > "$backup_file"
oci os object put --auth instance_principal --bucket-name "$OCI_BACKUP_BUCKET" --file "$backup_file" --name "$(basename "$backup_file")"
rm -f "$backup_file"
