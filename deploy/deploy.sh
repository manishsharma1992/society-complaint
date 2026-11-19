#!/usr/bin/env bash
# Run from your laptop. Each site has its own server: seva = Garden City Seva (complaints), feed = the public feed.
#   deploy/deploy.sh <site> setup     upload this folder and prepare the server (first time, or after changing
#                                     the systemd unit or Nginx config)
#   deploy/deploy.sh <site> tls       get the Let's Encrypt certificate for the domain and switch Nginx to HTTPS (once;
#                                     certbot renews it automatically). DNS must already point at the server.
#   deploy/deploy.sh <site> frontend  build the web app and publish it to /var/www/society-complaint
#   deploy/deploy.sh <site>           build + test, upload a new release of the app, restart, smoke-test
#
# Settings (environment variables):
#   SEVA_HOST, FEED_HOST  static IP of each site's server (the one for the chosen site is required)
#   SSH_KEY         private key downloaded from Lightsail (default ~/.ssh/LightsailDefaultKey-ap-southeast-1.pem)
#   SSH_USER        default ubuntu
#   LETSENCRYPT_EMAIL  optional, for certificate expiry warnings (tls only)
#   ADOPT=yes       setup only: lets the server from before the separation become this site (the switch-over)
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
SITE=${1:-}
case "$SITE" in
    seva) HOST=${SEVA_HOST:?Set SEVA_HOST to the static IP of the Garden City Seva server} ;;
    feed) HOST=${FEED_HOST:?Set FEED_HOST to the static IP of the feed server} ;;
    *) echo "Usage: deploy/deploy.sh <seva|feed> [setup|tls|frontend]" >&2; exit 1 ;;
esac
ACTION=${2:-}
# shellcheck disable=SC1090
. "$ROOT/deploy/sites/$SITE.sh"
SSH_KEY=${SSH_KEY:-$HOME/.ssh/LightsailDefaultKey-ap-southeast-1.pem}
SSH_USER=${SSH_USER:-ubuntu}
TARGET="$SSH_USER@$HOST"
SSH_OPTS=(-i "$SSH_KEY" -o StrictHostKeyChecking=accept-new)

remote() { ssh "${SSH_OPTS[@]}" "$TARGET" "$@"; }

if [ "$ACTION" = "setup" ]; then
    echo "==> Uploading deploy files"
    remote 'rm -rf /tmp/society-complaint-deploy'
    scp "${SSH_OPTS[@]}" -r "$ROOT/deploy" "$TARGET:/tmp/society-complaint-deploy"
    remote "sudo ADOPT=${ADOPT:-no} bash /tmp/society-complaint-deploy/setup-server.sh $SITE && rm -rf /tmp/society-complaint-deploy"
    exit 0
fi

# Everything below changes a running site: only on the server that was set up for this site.
MARKED=$(remote 'sudo cat /etc/society-complaint/site 2>/dev/null || true')
if [ "$MARKED" != "$SITE" ]; then
    echo "!! $HOST is marked as '${MARKED:-nothing}', not $SITE. Nothing changed. Check the host, or run setup first." >&2
    exit 1
fi

if [ "$ACTION" = "tls" ]; then
    if [ -n "${LETSENCRYPT_EMAIL:-}" ]; then ACCOUNT="-m $LETSENCRYPT_EMAIL"; else ACCOUNT=--register-unsafely-without-email; fi
    echo "==> Requesting the certificate for $DOMAIN and www.$DOMAIN"
    remote "sudo certbot certonly --webroot -w /var/www/letsencrypt -d $DOMAIN -d www.$DOMAIN \
        --agree-tos --no-eff-email --non-interactive $ACCOUNT --deploy-hook 'systemctl reload nginx'"
    echo "==> Switching Nginx to HTTPS"
    exec "$0" "$SITE" setup
fi

if [ "$ACTION" = "frontend" ]; then
    echo "==> Building both web apps and checking that neither contains the other (Node from frontend/.nvmrc)"
    (
        cd "$ROOT/frontend"
        set +u # nvm.sh reads unset variables
        export NVM_DIR=${NVM_DIR:-$HOME/.nvm}
        # shellcheck disable=SC1091
        . "$NVM_DIR/nvm.sh" && nvm use >/dev/null
        [ -d node_modules ] || npm ci --no-audit --no-fund
        npm run -s build:sites
    )
    BUNDLE="frontend-$(date +%Y%m%d-%H%M%S).tar.gz"
    tar -czf "/tmp/$BUNDLE" -C "$ROOT/$DIST" .
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
    for path in $PAGES; do
        echo "    $path -> $(curl -s -o /dev/null -w '%{http_code}' "https://$DOMAIN$path")"
    done
    exit 0
fi

echo "==> Building and testing (architecture tests must pass before anything ships)"
(cd "$ROOT" && mvn -q clean verify)

RELEASE="app-$(date +%Y%m%d-%H%M%S).jar"
echo "==> Uploading $RELEASE"
scp "${SSH_OPTS[@]}" "$ROOT/$JAR" "$TARGET:/tmp/$RELEASE"

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
    if [ "$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8081/api/identity/me)" = 401 ]; then
        echo "==> $RELEASE is up"
        exit 0
    fi
    sleep 2
done
echo "!! Service did not answer within 2 minutes. Last log lines:" >&2
journalctl -u complaint-service -n 50 --no-pager >&2
exit 1
REMOTE

echo "==> $SITE is live at https://$DOMAIN/"
