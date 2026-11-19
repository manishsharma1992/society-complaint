# Deploying to AWS Lightsail

Two sites, each on its own Lightsail server (Singapore, same region as Neon). They share no server, database, bucket
or login. Each server runs Nginx and one Spring Boot app.

| Site | Domain | App | Web app | Server |
|---|---|---|---|---|
| `seva` | gardencityseva.com | `app` (complaints) | `frontend/dist/seva` | 1 GB, heap 512 MB |
| `feed` | runwalltdfakepromises.com | `feed-app` (feed) | `frontend/dist/feed` | 2 GB, heap 1 GB, ffmpeg |

Credentials live only on each server in `/etc/society-complaint/complaint.env` (root-only); systemd passes them to the
service as environment variables.

```
internet -> Nginx :443 -> /api/** -> 127.0.0.1:8081 -> Neon
                       -> /**     -> the site's web app
         -> Nginx :80  -> /.well-known/acme-challenge/ (Let's Encrypt), everything else redirects to HTTPS
```

**Safeguard:** `setup` records the site in `/etc/society-complaint/site`. Every other command refuses a server marked
for another site, or not marked at all. A server that already runs an app but has no mark only accepts `setup` with
`ADOPT=yes`, which is meant for the switch-over of the server from before the separation.

| File | Goes to |
|---|---|
| `sites/<site>.sh` | each site's domain, jar, heap, packages and checks |
| `complaint-service.service` | `/etc/systemd/system/` (heap filled in) |
| `nginx/render.sh` | builds the site's Nginx files from the templates in `nginx/` |
| `nginx/society-complaint.conf`, `nginx/society-complaint-tls.conf` | `/etc/nginx/sites-available/` (HTTP until the certificate exists, then HTTPS) |
| `nginx/society-complaint-app.conf`, `nginx/home.<site>.conf`, `nginx/acme-challenge.conf` | `/etc/nginx/snippets/` |
| `complaint.env.example` | template for `/etc/society-complaint/complaint.env` |
| `setup-server.sh` | runs on the server (via `deploy.sh <site> setup`) |
| `deploy.sh` | runs on your laptop |

## 1. Create the instance (once, in the Lightsail console)

1. Instance: **Linux/Unix -> OS Only -> Ubuntu 24.04 LTS**, region **Singapore (ap-southeast-1)**,
   plan with at least **1 GB RAM** for `seva`, **2 GB** for `feed` (ffmpeg re-encodes videos).
2. **Networking -> attach a static IP**, so the address survives restarts.
3. **Networking -> IPv4 firewall**: SSH (22) restricted to your own IP; HTTP (80) and HTTPS (443) open to everyone
   (Let's Encrypt must reach port 80, and demo users come from any network).
   Every API call needs a login token (phone + OTP), so the API is safe to expose. Remove the IPv6 rules or
   disable IPv6.
4. **Account -> SSH keys**: download the default key for Singapore, then
   ```bash
   mv ~/Downloads/LightsailDefaultKey-ap-southeast-1.pem ~/.ssh/ && chmod 600 ~/.ssh/LightsailDefaultKey-ap-southeast-1.pem
   ```

## 2. Prepare the server (once)

```bash
export SEVA_HOST=<static-ip>     # or FEED_HOST for the feed
deploy/deploy.sh seva setup
```

Then put the real Neon credentials on the server (same three lines as your local `.env`):

```bash
ssh -i ~/.ssh/LightsailDefaultKey-ap-southeast-1.pem ubuntu@$SEVA_HOST
sudo nano /etc/society-complaint/complaint.env
```

Besides the DB and photo bucket settings, `complaint.env` holds `AUTH_SECRET` (signs login tokens; generate once
with `openssl rand -hex 32`) and, for demos, `OTP_DEMO_CODE` (see the project README).

HTTPS: at the domain's DNS provider set the `A` record `@` to the static IP (`www` is a CNAME to `@`), wait until
`dig +short <domain>` shows the static IP, then once:

```bash
LETSENCRYPT_EMAIL=you@example.com deploy/deploy.sh seva tls   # email optional: expiry warnings
```

certbot renews the certificate automatically (`systemctl list-timers certbot.timer`) and reloads Nginx.

Re-run `deploy/deploy.sh <site> setup` whenever you change the systemd unit or the Nginx config.
It never overwrites `complaint.env`.

## 3. Deploy a new version

```bash
export SEVA_HOST=<static-ip>
deploy/deploy.sh seva
```

This runs `mvn clean verify` (all tests, including the architecture rules), uploads the jar as
`/opt/society-complaint/releases/app-<timestamp>.jar`, points `app.jar` at it, restarts the service and waits
until it answers. The first start runs the Flyway migrations on Neon.

Check it from your laptop (401 without a login token means it is up; see the project README to log in):
```bash
curl -i https://gardencityseva.com/api/identity/me
```

## 4. Deploy a new version of the web app

```bash
export SEVA_HOST=<static-ip>
deploy/deploy.sh seva frontend
```

This builds both web apps (Node from `frontend/.nvmrc`, through nvm), stops if either contains the other, uploads this
site's build and copies it into
`/var/www/society-complaint`: new files first, `index.html` last, so nobody gets a page whose scripts are not there
yet. Files of earlier releases are removed after a week (tabs still open on an old release keep working). It then
checks that the pages listed in `sites/<site>.sh` answer 200.

Nginx (`nginx/society-complaint-app.conf`): page links get `index.html`; `index.html` and `i18n/*.json` are never
cached, built files with a content hash in their name are cached for good, photos in `images/` for a week; a missing
file is a 404, never the app's page.

## Day-to-day

| Task | Command (on the server) |
|---|---|
| Logs | `journalctl -u complaint-service -f` |
| Status / restart | `systemctl status complaint-service` / `sudo systemctl restart complaint-service` |
| Rotate DB password | edit `/etc/society-complaint/complaint.env`, then `sudo systemctl restart complaint-service` |
| Roll back | `ls -t /opt/society-complaint/releases`, then `sudo ln -sfn /opt/society-complaint/releases/<older>.jar /opt/society-complaint/app.jar && sudo systemctl restart complaint-service` |

Rolling back the jar does not undo Flyway migrations; keep migrations backward compatible.

## Photo bucket CORS (once)

Browsers upload photos straight to the bucket, so the bucket must allow the app's origin.
`bucket-cors.json` allows GET and PUT from the domain (and `http://localhost:4200` for Angular development).
This needs an account-level AWS action, not the bucket's access keys. Easiest from **AWS CloudShell**
(AWS console, region Singapore, the `>_` icon in the top bar), after uploading `bucket-cors.json` there
(Actions -> Upload file):

```bash
aws lightsail update-bucket --bucket-name runwal-complaint-photos --cors file://bucket-cors.json
aws lightsail get-buckets --bucket-name runwal-complaint-photos --include-cors     # check
```

Re-run it after editing the file (the new rules replace the old ones).

## Later

- Residency and Identity run inside the same app for now (2 GB instance, see CLAUDE.md). When needed, split
  them out: one systemd unit, env file and Nginx `location /api/<service>/` per service, each with its own Neon role.
