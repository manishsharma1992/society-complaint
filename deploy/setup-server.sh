#!/usr/bin/env bash
# One-time (and re-runnable) server setup for Ubuntu 24.04 on Lightsail. Run as root from the uploaded deploy folder.
# Installs Java 25 and Nginx, creates the service user and folders, installs the systemd unit and Nginx site.
# Never overwrites an existing /etc/society-complaint/complaint.env.
set -euo pipefail

[ "$(id -u)" -eq 0 ] || { echo "Run with sudo" >&2; exit 1; }
HERE=$(cd "$(dirname "$0")" && pwd)

echo "==> Packages"
apt-get update -q
apt-get install -y -q wget gpg nginx certbot
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
install -d -m 755 /var/www/society-complaint
install -d -m 755 /var/www/letsencrypt

if [ ! -f /etc/society-complaint/complaint.env ]; then
    install -m 600 "$HERE/complaint.env.example" /etc/society-complaint/complaint.env
    echo "!! Fill in /etc/society-complaint/complaint.env (sudo nano /etc/society-complaint/complaint.env)"
fi

echo "==> systemd unit"
install -m 644 "$HERE/complaint-service.service" /etc/systemd/system/complaint-service.service
systemctl daemon-reload
systemctl enable complaint-service

echo "==> Nginx"
install -m 644 "$HERE/nginx/society-complaint-app.conf" /etc/nginx/snippets/society-complaint-app.conf
install -m 644 "$HERE/nginx/acme-challenge.conf" /etc/nginx/snippets/acme-challenge.conf
# HTTPS once deploy.sh tls has obtained the certificate, plain HTTP before that
if [ -f /etc/letsencrypt/live/runwalltdfakepromises.com/fullchain.pem ]; then
    install -m 644 "$HERE/nginx/society-complaint-tls.conf" /etc/nginx/sites-available/society-complaint.conf
else
    install -m 644 "$HERE/nginx/society-complaint.conf" /etc/nginx/sites-available/society-complaint.conf
fi
# the temporary demo password is gone: phone + OTP login tokens protect the API now
rm -f /etc/nginx/society-complaint.htpasswd
ln -sfn /etc/nginx/sites-available/society-complaint.conf /etc/nginx/sites-enabled/society-complaint.conf
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx

echo "==> Server ready. Next: fill in complaint.env (if new), then run deploy/deploy.sh from your laptop."
