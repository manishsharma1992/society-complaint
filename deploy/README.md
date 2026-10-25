# Deploying to AWS Lightsail

One Lightsail instance (Singapore, same region as Neon) runs Nginx and the services.
Database credentials live only on the server in `/etc/society-complaint/complaint.env` (root-only);
systemd passes them to the service as environment variables.

```
internet -> Nginx :443 (https://runwalltdfakepromises.com) -> /api/complaint/** -> 127.0.0.1:8081 -> Neon
                                                                          -> /**  -> Angular build (later)
         -> Nginx :80  -> /.well-known/acme-challenge/ (Let's Encrypt), everything else redirects to HTTPS
```

| File | Goes to |
|---|---|
| `complaint-service.service` | `/etc/systemd/system/` |
| `nginx/society-complaint.conf` | `/etc/nginx/sites-available/` (HTTP only, until the certificate exists) |
| `nginx/society-complaint-tls.conf` | same place, once the certificate exists |
| `nginx/society-complaint-app.conf`, `nginx/acme-challenge.conf` | `/etc/nginx/snippets/` |
| `complaint.env.example` | template for `/etc/society-complaint/complaint.env` |
| `setup-server.sh` | runs on the server (via `deploy.sh setup`) |
| `deploy.sh` | runs on your laptop |

## 1. Create the instance (once, in the Lightsail console)

1. Instance: **Linux/Unix -> OS Only -> Ubuntu 24.04 LTS**, region **Singapore (ap-southeast-1)**,
   plan with at least **1 GB RAM** (each Spring Boot service needs ~250-400 MB).
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
export LIGHTSAIL_HOST=<static-ip>
deploy/deploy.sh setup
```

Then put the real Neon credentials on the server (same three lines as your local `.env`):

```bash
ssh -i ~/.ssh/LightsailDefaultKey-ap-southeast-1.pem ubuntu@$LIGHTSAIL_HOST
sudo nano /etc/society-complaint/complaint.env
```

Besides the DB and photo bucket settings, `complaint.env` holds `AUTH_SECRET` (signs login tokens; generate once
with `openssl rand -hex 32`) and, for demos, `OTP_DEMO_CODE` (see the project README).

HTTPS: at GoDaddy (DNS of runwalltdfakepromises.com) set the `A` record `@` to the static IP (`www` is a CNAME to `@`),
wait until `dig +short runwalltdfakepromises.com` shows the static IP, then once:

```bash
LETSENCRYPT_EMAIL=you@example.com deploy/deploy.sh tls   # email optional: expiry warnings
```

certbot renews the certificate automatically (`systemctl list-timers certbot.timer`) and reloads Nginx.

Re-run `deploy/deploy.sh setup` whenever you change the systemd unit or the Nginx config.
It never overwrites `complaint.env`.

## 3. Deploy a new version

```bash
export LIGHTSAIL_HOST=<static-ip>
deploy/deploy.sh
```

This runs `mvn clean verify` (all tests, including the architecture rules), uploads the jar as
`/opt/society-complaint/releases/app-<timestamp>.jar`, points `app.jar` at it, restarts the service and waits
until it answers. The first start runs the Flyway migrations on Neon.

Check it from your laptop (401 without a login token means it is up; see the project README to log in):
```bash
curl -i https://runwalltdfakepromises.com/api/complaint/societies/1/complaints
```

## 4. Deploy a new version of the web app

```bash
export LIGHTSAIL_HOST=<static-ip>
deploy/deploy.sh frontend
```

This builds the Angular app (Node from `frontend/.nvmrc`, through nvm), uploads it and copies it into
`/var/www/society-complaint`: new files first, `index.html` last, so nobody gets a page whose scripts are not there
yet. Files of earlier releases are removed after a week (tabs still open on an old release keep working). It then
checks that `/`, a page link (`/complaints/7`) and a translation file answer 200.

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

- Residency and Identity run inside the same app while the instance has 512 MB (see CLAUDE.md). After upgrading, split
  them out: one systemd unit, env file and Nginx `location /api/<service>/` per service, each with its own Neon role.
