#!/usr/bin/env bash
# Writes one site's Nginx files into a folder: render.sh <seva|feed> <folder> <tls|http>
set -euo pipefail
SITE=$1 OUT=$2 MODE=$3
HERE=$(cd "$(dirname "$0")" && pwd)
# shellcheck disable=SC1090
. "$HERE/../sites/$SITE.sh"
fill() { sed -e "s|__DOMAIN__|$DOMAIN|g" -e "s|__PERMISSIONS__|$PERMISSIONS|g" "$1"; }

mkdir -p "$OUT/snippets"
fill "$HERE/society-complaint-app.conf" > "$OUT/snippets/society-complaint-app.conf"
cp "$HERE/acme-challenge.conf" "$OUT/snippets/acme-challenge.conf"
cp "$HERE/home.$SITE.conf" "$OUT/snippets/society-complaint-home.conf"
if [ "$MODE" = tls ]; then
    fill "$HERE/society-complaint-tls.conf" > "$OUT/society-complaint.conf"
else
    cp "$HERE/society-complaint.conf" "$OUT/society-complaint.conf"
fi
