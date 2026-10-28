#!/bin/sh
set -eu

: "${BACKUP_PASSPHRASE:?BACKUP_PASSPHRASE is required}"

mkdir -p /backup
stamp="$(date +%Y%m%d-%H%M)"
pg_dump "$PGDATABASE" \
  | gzip \
  | openssl enc -aes-256-cbc -pbkdf2 -salt -pass env:BACKUP_PASSPHRASE \
  > "/backup/antenatal-${stamp}.sql.gz.enc"

find /backup -type f -name 'antenatal-*.sql.gz.enc' -mtime +30 -delete
