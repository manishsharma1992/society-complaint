#!/usr/bin/env bash
# Puts five example posts, with illustrations stamped "EXAMPLE PHOTO", on the feed for a demo, or takes them down. Runs against the live API with the
# demo accounts: the demo resident (9000000004) writes the posts, the demo society admin (9000000001) publishes them.
# The posts are invented examples and say so in the title and the first line; take them down after the demo.
#
#   OTP_DEMO_CODE=123456 deploy/demo-feed.sh create
#   OTP_DEMO_CODE=123456 deploy/demo-feed.sh remove
#
# Needs curl and jq. BASE defaults to the live site. Each run signs in twice (the OTP limit is 5 an hour per number).
set -euo pipefail

BASE="${BASE:-https://runwalltdfakepromises.com}"
API="$BASE/api"
SOCIETY=1
RESIDENT=9000000004
ADMIN=9000000001
MARK="Example post:"
IMAGES="$(cd "$(dirname "$0")" && pwd)/demo-feed" # rendered from demo-feed/scenes.html
: "${OTP_DEMO_CODE:?set OTP_DEMO_CODE to the demo code set on the server}"

# The JSON bodies go through pipes: macOS's bash 3.2 misparses quotes and braces inside $(...).
login() {
  jq -n --arg phone "$1" '{phone: $phone}' |
    curl -fsS -X POST "$API/identity/login/otp" -H 'Content-Type: application/json' -d @- >/dev/null
  jq -n --arg phone "$1" --arg code "$OTP_DEMO_CODE" '{phone: $phone, code: $code}' |
    curl -fsS -X POST "$API/identity/login/verify" -H 'Content-Type: application/json' -d @- | jq -r .token
}

send() { # method token path, JSON body on stdin
  curl -fsS -X "$1" "$API$3" -H "Authorization: Bearer $2" -H 'Content-Type: application/json' -d @-
}

get() { # token path
  curl -fsS "$API$2" -H "Authorization: Bearer $1"
}

NOTE="EXAMPLE FOR A DEMO: this is not a real report. It shows how a resident documents a promise."

# title|tower to show (7 = B2 Radiant, or empty)|images in demo-feed/|promised|today
POSTS=(
  "Boundary wall around Phase 1|7|wall-1.jpg,wall-2.jpg|A boundary wall around Phase 1 (brochure, page —).|Two stretches near the back gate have no wall; the open side faces the main road."
  "Swimming pool at the clubhouse||pool-1.jpg|A swimming pool open to residents at possession (amenities list).|The pool area is fenced off. Residents have not been given an opening date."
  "Internal road to tower B2|7|road-1.jpg|Finished internal roads to every tower (approved plan).|About 80 m of the road to B2 is unpaved; water stands there after rain."
  "Rainwater harvesting||rain-1.jpg|Rainwater harvesting for the whole layout (brochure, page —).|Residents could not find the pits or tanks; the society office has asked where they are."
  "Children's play area||play-1.jpg|Play equipment for three age groups (brochure, page —).|Equipment for one age group is installed; the other two areas are empty lawn."
)

post_json() { # title body tower
  jq -n --arg t "$MARK $1" --arg b "$2" --arg tower "$3" \
    '{title: $t, body: $b, showTowerId: (if $tower == "" then null else ($tower | tonumber) end)}'
}

attach() { # token postId file: the same three steps as the app (upload link, upload, attach)
  local ticket key url size
  size=$(wc -c <"$3" | tr -d ' ')
  ticket=$(jq -n --argjson size "$size" '{contentType: "image/jpeg", sizeBytes: $size}' |
    send POST "$1" "/feed/societies/$SOCIETY/posts/$2/evidence/upload-url")
  key=$(jq -r .uploadKey <<<"$ticket")
  url=$(jq -r .uploadUrl <<<"$ticket")
  curl -fsS -X PUT "$url" -H 'Content-Type: image/jpeg' --data-binary "@$3" >/dev/null
  jq -n --arg key "$key" '{uploadKey: $key}' |
    send POST "$1" "/feed/societies/$SOCIETY/posts/$2/evidence" >/dev/null
}

create() {
  local resident admin
  resident=$(login "$RESIDENT")
  admin=$(login "$ADMIN")
  for entry in "${POSTS[@]}"; do
    IFS='|' read -r title tower images promised today <<<"$entry"
    body=$(printf '%s\n\nPromised: %s\n\nToday: %s' "$NOTE" "$promised" "$today")
    id=$(post_json "$title" "$body" "$tower" | send POST "$resident" "/feed/societies/$SOCIETY/posts" | jq -r .id)
    for image in ${images//,/ }; do
      attach "$resident" "$id" "$IMAGES/$image"
    done
    echo '{}' | send POST "$admin" "/feed/societies/$SOCIETY/moderation/$id/publish" >/dev/null
    echo "published $id: $MARK $title"
  done
  echo "Open $BASE/public"
}

moderate() { # token postId decision; a published post is hidden before it can be removed
  echo '{"reason": "Demo example, taken down after the demo"}' |
    send POST "$1" "/feed/societies/$SOCIETY/moderation/$2/$3" >/dev/null
}

remove() {
  local resident admin
  resident=$(login "$RESIDENT")
  admin=$(login "$ADMIN")
  get "$resident" "/feed/societies/$SOCIETY/my-posts?size=50" |
    jq -r --arg mark "$MARK" '.[] | select(.title | startswith($mark)) | "\(.id) \(.status)"' |
    while read -r id status; do
      case "$status" in
        PUBLISHED) moderate "$admin" "$id" hide && moderate "$admin" "$id" remove ;;
        HIDDEN) moderate "$admin" "$id" remove ;;
        PENDING_REVIEW) moderate "$admin" "$id" reject ;;
        *) continue ;;
      esac
      echo "taken down $id"
    done
}

case "${1:-}" in
  create) create ;;
  remove) remove ;;
  *) echo "usage: OTP_DEMO_CODE=... $0 create|remove" >&2; exit 2 ;;
esac
