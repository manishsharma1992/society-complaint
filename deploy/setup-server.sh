#!/usr/bin/env bash
# One-time (and re-runnable) server setup for Ubuntu 24.04 on Lightsail. Run as root from the uploaded deploy folder.
# Usage: setup-server.sh <seva|feed>. Each server runs one site, and remembers which in /etc/society-complaint/site.
# Installs Java 25 and Nginx, creates the service user and folders, installs the systemd unit and Nginx site.
# Never overwrites an existing /etc/society-complaint/complaint.env.
set -euo pipefail

[ "$(id -u)" -eq 0 ] || { echo "Run with sudo" >&2; exit 1; }
HERE=$(cd "$(dirname "$0")" && pwd)
SITE=${1:?Usage: setup-server.sh seva or feed}
[ -f "$HERE/sites/$SITE.sh" ] || { echo "Unknown site: $SITE" >&2; exit 1; }
# shellcheck disable=SC1090
. "$HERE/sites/$SITE.sh"

MARKER=/etc/society-complaint/site
if [ -f "$MARKER" ]; then
    [ "$(cat "$MARKER")" = "$SITE" ] || { echo "!! This server runs $(cat "$MARKER"), not $SITE. Nothing changed." >&2; exit 1; }
elif [ -e /opt/society-complaint/app.jar ] && [ "${ADOPT:-}" != yes ]; then
    # the server from before the separation: it may only become a site on purpose, at the switch-over
    echo "!! This server already runs an app but is not marked as $SITE. Use ADOPT=yes at the switch-over. Nothing changed." >&2
    exit 1
fi

echo "==> Packages"
apt-get update -q
# shellcheck disable=SC2086
apt-get install -y -q wget gpg nginx certbot $EXTRA_PACKAGES
if ! command -v java >/dev/null || ! java -version 2>&1 | grep -q '"25'; then
    # Eclipse Temurin (Adoptium) builds of OpenJDK
    install -d /etc/apt/keyrings
    wget -qO- https://packages.adoptium.net/artifactory/api/gpg/key/public \
        | gpg --dearmor --yes -o /etc/apt/keyrings/adoptium.gpg
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo "$VERSION_CODENAME") main" \
        > /etc/apt/sources.list.d/adoptium.list
    apt-get update -q
    apt-get install -y -q temurin-25-jre
fi
java -version

echo "==> Swap (small Lightsail plans have little RAM)"
if ! swapon --show | grep -q .; then
    fallocate -l 1G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo "==> Service user and folders"
id complaint >/dev/null 2>&1 || useradd --system --no-create-home --shell /usr/sbin/nologin complaint
install -d -m 755 /opt/society-complaint/releases
install -d -m 700 /etc/society-complaint
echo "$SITE" > "$MARKER"
install -d -m 755 /var/www/society-complaint
install -d -m 755 /var/www/letsencrypt

if [ ! -f /etc/society-complaint/complaint.env ]; then
    install -m 600 "$HERE/complaint.env.example" /etc/society-complaint/complaint.env
    echo "!! Fill in /etc/society-complaint/complaint.env (sudo nano /etc/society-complaint/complaint.env)"
fi

echo "==> systemd unit"
sed "s|__HEAP__|$HEAP|" "$HERE/complaint-service.service" > /etc/systemd/system/complaint-service.service
chmod 644 /etc/systemd/system/complaint-service.service
systemctl daemon-reload
systemctl enable complaint-service

echo "==> Nginx"
# HTTPS once deploy.sh tls has obtained the certificate, plain HTTP before that
MODE=http
[ -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ] && MODE=tls
RENDERED=$(mktemp -d)
bash "$HERE/nginx/render.sh" "$SITE" "$RENDERED" "$MODE"
install -m 644 "$RENDERED"/snippets/*.conf /etc/nginx/snippets/
install -m 644 "$RENDERED/society-complaint.conf" /etc/nginx/sites-available/society-complaint.conf
rm -rf "$RENDERED"
# the temporary demo password is gone: phone + OTP login tokens protect the API now
rm -f /etc/nginx/society-complaint.htpasswd
ln -sfn /etc/nginx/sites-available/society-complaint.conf /etc/nginx/sites-enabled/society-complaint.conf
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx

echo "==> $SITE server ready. Next: fill in complaint.env (if new), then run deploy/deploy.sh $SITE from your laptop."
