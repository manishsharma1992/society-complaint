#!/usr/bin/env bash
# Run from your laptop. Copies Garden City Seva's schemas (complaint, workforce, identity, residency) from the
# database the server from before the separation uses into Seva's own Neon database, then renames the society.
# The feed schema is never copied. The dump runs on the Seva server; no credential passes through the laptop's screen.
#   FROM_HOST=<old server ip> SEVA_HOST=<seva server ip> deploy/copy-data.sh
#   REPLACE=yes ...   drops Seva's copy first (the fresh copy at the switch-over; stop the old app before it)
set -euo pipefail

FROM_HOST=${FROM_HOST:?Set FROM_HOST to the static IP of the server from before the separation}
SEVA_HOST=${SEVA_HOST:?Set SEVA_HOST to the static IP of the Garden City Seva server}
SSH_KEY=${SSH_KEY:-$HOME/.ssh/LightsailDefaultKey-ap-southeast-1.pem}
SSH_OPTS=(-i "$SSH_KEY" -o StrictHostKeyChecking=accept-new)
ENV=/etc/society-complaint/complaint.env

echo "==> Handing the old database's login to the Seva server (root-only file, removed afterwards)"
ssh "${SSH_OPTS[@]}" "ubuntu@$FROM_HOST" "sudo grep -E '^DB_(URL|USER|PASSWORD)=' $ENV" \
    | ssh "${SSH_OPTS[@]}" "ubuntu@$SEVA_HOST" 'sudo install -m 600 /dev/stdin /root/old-db.env'

ssh "${SSH_OPTS[@]}" "ubuntu@$SEVA_HOST" "sudo REPLACE=${REPLACE:-no} bash -s" <<'REMOTE'
set -euo pipefail
trap 'rm -f /root/old-db.env /root/seva.dump' EXIT
SCHEMAS="complaint workforce identity residency"

# read KEY=value without sourcing (the URL holds '&'); jdbc URL -> libpq URI, direct host (pg_dump needs it)
val() { grep -m1 "^$1=" "$2" | cut -d= -f2- | sed -e 's/^"//' -e 's/"$//'; }
uri() { val DB_URL "$1" | sed -e 's|^jdbc:||' -e 's|-pooler||' -e 's|channelBinding=|channel_binding=|'; }
old() { PGUSER=$(val DB_USER /root/old-db.env) PGPASSWORD=$(val DB_PASSWORD /root/old-db.env) "$@"; }
new() { PGUSER=$(val DB_USER /etc/society-complaint/complaint.env) PGPASSWORD=$(val DB_PASSWORD /etc/society-complaint/complaint.env) "$@"; }
OLD=$(uri /root/old-db.env); NEW=$(uri /etc/society-complaint/complaint.env)
[ "$OLD" != "$NEW" ] || { echo "!! Seva's complaint.env points at the old database; give Seva its own" >&2; exit 1; }

if ! command -v pg_dump >/dev/null; then
    echo "==> Installing the Postgres client"
    apt-get install -y -qq postgresql-common >/dev/null
    /usr/share/postgresql-common/pgdg/apt.postgresql.org.sh -y >/dev/null
    apt-get install -y -qq postgresql-client-18 >/dev/null
fi

if systemctl is-active -q complaint-service; then echo "!! Stop complaint-service first (its Flyway would race the copy)" >&2; exit 1; fi
present=$(new psql "$NEW" -tAc "select string_agg(schema_name, ' ') from information_schema.schemata where schema_name in ('complaint','workforce','identity','residency')")
if [ -n "$present" ]; then
    [ "$REPLACE" = yes ] || { echo "!! Seva's database already has: $present (REPLACE=yes drops and copies again)" >&2; exit 1; }
    echo "==> Dropping Seva's copy: $present"
    for s in $present; do new psql "$NEW" -qc "DROP SCHEMA $s CASCADE"; done
fi

echo "==> Copying $SCHEMAS"
old pg_dump "$OLD" -Fc --no-owner --no-privileges $(printf -- '-n %s ' $SCHEMAS) -f /root/seva.dump
new pg_restore -d "$NEW" --no-owner --no-privileges --single-transaction --exit-on-error /root/seva.dump
new psql "$NEW" -qc "UPDATE residency.society SET name = 'Runwal Garden City' WHERE id = 1"

echo "==> Rows per table (old -> Seva)"
counts() { "$1" psql "$2" -tA -F' ' <<'SQL'
select format('select %L, count(*) from %I.%I', table_schema || '.' || table_name, table_schema, table_name)
from information_schema.tables where table_schema in ('complaint','workforce','identity','residency') and table_type = 'BASE TABLE'
order by 1 \gexec
SQL
}
diff <(counts old "$OLD") <(counts new "$NEW") && counts new "$NEW" | sed 's/^/    /'
echo "    society: $(new psql "$NEW" -tAc "select name from residency.society where id = 1"); automatic assignment on: $(new psql "$NEW" -tAc "select coalesce((select enabled from complaint.auto_assignment where society_id = 1), false)")"
REMOTE
echo "==> Copied. Next: deploy/deploy.sh seva (starts the app on the copy)"
