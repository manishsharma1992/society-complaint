#!/usr/bin/env bash
# Run from your laptop.
#   deploy/deploy.sh setup   upload this folder and prepare the server (first time, or after changing
#                            the systemd unit or Nginx config)
#   deploy/deploy.sh tls     get the Let's Encrypt certificate for the domain and switch Nginx to HTTPS (once;
#                            certbot renews it automatically). DNS must already point at the instance.
#   deploy/deploy.sh frontend  build the Angular app and publish it to /var/www/society-complaint
#   deploy/deploy.sh         build + test, upload a new release of the app, restart, smoke-test
#
# Settings (environment variables):
#   LIGHTSAIL_HOST  static IP of the instance (required)
#   SSH_KEY         private key downloaded from Lightsail (default ~/.ssh/LightsailDefaultKey-ap-southeast-1.pem)
#   SSH_USER        default ubuntu
#   LETSENCRYPT_EMAIL  optional, for certificate expiry warnings (tls only)
set -euo pipefail

: "${LIGHTSAIL_HOST:?Set LIGHTSAIL_HOST to the static IP of the instance}"
SSH_KEY=${SSH_KEY:-$HOME/.ssh/LightsailDefaultKey-ap-southeast-1.pem}
SSH_USER=${SSH_USER:-ubuntu}
TARGET="$SSH_USER@$LIGHTSAIL_HOST"
DOMAIN=runwalltdfakepromises.com
SSH_OPTS=(-i "$SSH_KEY" -o StrictHostKeyChecking=accept-new)
ROOT=$(cd "$(dirname "$0")/.." && pwd)

remote() { ssh "${SSH_OPTS[@]}" "$TARGET" "$@"; }

if [ "${1:-}" = "setup" ]; then
    echo "==> Uploading deploy files"
    remote 'rm -rf /tmp/society-complaint-deploy'
    scp "${SSH_OPTS[@]}" -r "$ROOT/deploy" "$TARGET:/tmp/society-complaint-deploy"
    remote 'sudo bash /tmp/society-complaint-deploy/setup-server.sh && rm -rf /tmp/society-complaint-deploy'
    exit 0
fi

if [ "${1:-}" = "tls" ]; then
    if [ -n "${LETSENCRYPT_EMAIL:-}" ]; then ACCOUNT="-m $LETSENCRYPT_EMAIL"; else ACCOUNT=--register-unsafely-without-email; fi
    echo "==> Requesting the certificate for $DOMAIN and www.$DOMAIN"
    remote "sudo certbot certonly --webroot -w /var/www/letsencrypt -d $DOMAIN -d www.$DOMAIN \
        --agree-tos --no-eff-email --non-interactive $ACCOUNT --deploy-hook 'systemctl reload nginx'"
    echo "==> Switching Nginx to HTTPS"
    exec "$0" setup
fi

if [ "${1:-}" = "frontend" ]; then
    # shortcut: blocked on this branch until the deploy step of the separation; ng build now makes Garden City Seva only
    echo "!! Frontend deploy is off during the feed/complaints separation." >&2
    exit 1
    echo "==> Building the Angular app (Node from frontend/.nvmrc)"
    (
        cd "$ROOT/frontend"
        set +u # nvm.sh reads unset variables
        export NVM_DIR=${NVM_DIR:-$HOME/.nvm}
        # shellcheck disable=SC1091
        . "$NVM_DIR/nvm.sh" && nvm use >/dev/null
        [ -d node_modules ] || npm ci --no-audit --no-fund
        npx ng build
    )
    BUNDLE="frontend-$(date +%Y%m%d-%H%M%S).tar.gz"
    tar -czf "/tmp/$BUNDLE" -C "$ROOT/frontend/dist/frontend/browser" .
    echo "==> Uploading $BUNDLE"
    scp "${SSH_OPTS[@]}" "/tmp/$BUNDLE" "$TARGET:/tmp/$BUNDLE"
    rm -f "/tmp/$BUNDLE"
    remote sudo bash -s "$BUNDLE" <<'REMOTE'
set -euo pipefail
BUNDLE=$1
WEB=/var/www/society-complaint
STAGE=$(mktemp -d)
tar -xzf "/tmp/$BUNDLE" -C "$STAGE"
rm -f "/tmp/$BUNDLE"
# new files first (the homepage's hi/ and mr/ index.html files included) and the app's index.html last, so nobody gets a page whose scripts are not there yet; files of the
# previous release stay a week for tabs still open on it
(cd "$STAGE" && find . -type f ! -path ./index.html -exec install -D -m 644 {} "$WEB/{}" \;)
install -m 644 "$STAGE/index.html" "$WEB/index.html"
rm -rf "$STAGE"
find "$WEB" -type f -mtime +7 -delete
find "$WEB" -mindepth 1 -type d -empty -delete
REMOTE
    echo "==> Checking https://$DOMAIN"
    for path in / /complaints/7 /i18n/mr.json; do
        echo "    $path -> $(curl -s -o /dev/null -w '%{http_code}' "https://$DOMAIN$path")"
    done
    exit 0
fi

# shortcut: backend deploys are blocked while the app is split in two (app = complaints only, feed-app = feed only);
# the live server still runs the combined jar. Remove this when the deploy step of the separation is done.
echo "!! Backend deploy is off during the feed/complaints separation: either new jar alone would take half the live site down." >&2
exit 1

echo "==> Building and testing (architecture tests must pass before anything ships)"
(cd "$ROOT" && mvn -q clean verify)

RELEASE="app-$(date +%Y%m%d-%H%M%S).jar"
echo "==> Uploading $RELEASE"
scp "${SSH_OPTS[@]}" "$ROOT/app/target/app-1.0-SNAPSHOT.jar" "$TARGET:/tmp/$RELEASE"

echo "==> Switching to $RELEASE and restarting"
remote sudo bash -s "$RELEASE" <<'REMOTE'
set -euo pipefail
RELEASE=$1
DIR=/opt/society-complaint
install -m 644 "/tmp/$RELEASE" "$DIR/releases/$RELEASE"
rm -f "/tmp/$RELEASE"
ln -sfn "$DIR/releases/$RELEASE" "$DIR/app.jar"
systemctl restart complaint-service

# keep the 3 newest releases for rollback
ls -1t "$DIR"/releases/app-*.jar | tail -n +4 | xargs -r rm --

echo "==> Waiting for the service (also wakes Neon)"
for _ in $(seq 1 60); do
    # without a login token the API answers 401: that means it is up
    if [ "$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8081/api/complaint/societies/1/complaints)" = 401 ]; then
        echo "==> $RELEASE is up"
        exit 0
    fi
    sleep 2
done
echo "!! Service did not answer within 2 minutes. Last log lines:" >&2
journalctl -u complaint-service -n 50 --no-pager >&2
exit 1
REMOTE

echo "==> Public URL: https://$DOMAIN/api/complaint/societies/1/complaints (http://$LIGHTSAIL_HOST/... before tls)"
