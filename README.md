# Society Complaint App

Residents of **Runwal MyCity** (Dombivli) raise complaints from their phone (lift, water, plumbing, ...), add photos,
and follow them until they are fixed. The society's admins and staff assign, work on and close them.

- Web app: **https://runwalltdfakepromises.com** (Angular, in `frontend/`; English, हिन्दी, मराठी)
- API: **https://runwalltdfakepromises.com/api/...**
- Backend: Java 25, Spring Boot, PostgreSQL on Neon, photos in Lightsail Object Storage; all contexts run as one app
  on a 512 MB Lightsail server for now (see `CLAUDE.md`, and `deploy/README.md` for the server)

## The web app

Open **https://runwalltdfakepromises.com** on a phone or computer (nothing to install; on a phone, "Add to Home
Screen" makes it an icon). Pick English, हिन्दी or मराठी at the top. What each person sees:

| Who | Tabs | What they do |
|---|---|---|
| Resident | Complaints, Profile | Raise a complaint (photos, category, own flat or a common area), follow it, comment, close it once fixed |
| Staff | Jobs, Complaints, Profile | Start work on assigned complaints, mark them resolved with a note |
| Tower admin | To assign, Complaints, Profile | Assign new complaints of their tower to staff, or reject them with a reason |
| Society admin | To assign, Complaints, Sign-ups, Profile | Everything for every tower, and approve new residents |
| New number | Sign-up | Name, tower, floor, flat, owner or tenant; waits for the society admin's approval |

A plain-language guide for committee members (which demo login to use for what) is shared separately by Manish.

## Demo logins

Login is **phone number + one-time code (OTP)**. Until SMS works (MSG91, after DLT approval), use these placeholder
numbers. They never receive an SMS; they accept the **demo code** set as `OTP_DEMO_CODE` on the server (ask Manish).

| Phone number | Name in the app | Role | What they can do |
|---|---|---|---|
| 9000000001 | Demo Society Admin | Society admin | Everything in the society: assign, reject, approve sign-ups, give roles |
| 9000000002 | Demo Tower Admin B2 | Tower admin of B2 Radiant | Assign, reject and manage complaints of tower B2 |
| 9000000003 | Demo Staff | Staff | Start and resolve the complaints assigned to them |
| 9000000004 | Demo Resident B2-503 | Resident, owner of B2-503 | Raise complaints for B2-503 or common areas, close them once fixed |

Any other (real) number can sign up too: its code is written to the server log until SMS works
(`journalctl -u complaint-service | grep "OTP for"` on the server), and the society admin approves it.

Before going live: delete the demo users and remove `OTP_DEMO_CODE` from the server.

## Logging in (API)

```bash
API=https://runwalltdfakepromises.com/api

# 1. ask for a code (for demo numbers nothing is sent; use the demo code)
curl -X POST $API/identity/login/otp -H 'Content-Type: application/json' -d '{"phone": "9000000004"}'

# 2. exchange the code for a login token (valid 30 days)
curl -X POST $API/identity/login/verify -H 'Content-Type: application/json' \
     -d '{"phone": "9000000004", "code": "<demo code>"}'
# -> {"token": "eyJ...", "expiresAt": "...", "user": {"id": 4, "status": "ACTIVE", "roles": [...]}}

# 3. send the token with every request
TOKEN=eyJ...
curl $API/complaint/societies/1/complaints -H "Authorization: Bearer $TOKEN"
```

Without a token every endpoint (except the two login calls) answers **401**; doing something your role does not
allow answers **403**; a broken business rule (e.g. closing an open complaint) answers **422**.

## Who may do what

| Action | Allowed for |
|---|---|
| Raise a FLAT complaint | Residents of that flat, staff, admins |
| Raise a COMMON_AREA complaint | Any active user of the society |
| See complaints | Admins and staff: all. Residents: their flats', the ones they raised, all common-area ones |
| Assign, reject | Society admin; tower admin of the complaint's tower |
| Start work, resolve | The assigned staff member; society admin; tower admin of that tower |
| Close (confirm it is fixed) | Whoever raised it; society admin; tower admin of that tower |
| Add photos | Whoever raised it, the assignee, society admin, tower admin of that tower |
| View towers and flats | Anyone logged in (needed to pick a flat when signing up); who lives in a flat: only the admins of its tower |
| Link / unlink flat members | Society admin; tower admin of that tower |
| Review sign-ups, give roles | Society admin |

Users who signed up but are not approved yet can only see themselves and register for a flat.

## Main endpoints

All under `https://runwalltdfakepromises.com/api`, society 1 = Runwal MyCity.

**Login and sign-up** (`/identity`)
- `POST /identity/login/otp` `{phone}`, `POST /identity/login/verify` `{phone, code}`
- `GET /identity/me`, `POST /identity/me/registration` `{name, societyId, flatId, occupancy: OWNER|TENANT}`
- `POST /identity/me/session`: a fresh token with the current status and roles (e.g. after approval or a new
  role), ending when the current login ends; the app calls it instead of asking for a new code
- `GET /identity/societies/1/registrations`, `POST .../registrations/{userId}/approve`, `POST .../{userId}/reject` `{reason}`
- `POST /identity/societies/1/users/{userId}/roles` `{role, towerId?}`, `DELETE ...roles?role=STAFF&towerId=7`
- `GET /identity/societies/1/users?ids=4,3` names for IDs shown on complaints and comments (any active member; names
  only, never phone numbers); `GET /identity/societies/1/users?role=STAFF` whom to assign (admins)

**Towers and flats** (`/residency`)
- `GET /residency/societies/1`, `GET .../towers`, `GET .../towers/{towerId}/flats`, `GET .../flats/{flatId}`
- `GET .../flats?memberId={userId}` (your own flats)
- `POST .../flats/{flatId}/members` `{userId, type: OWNER|TENANT}`, `DELETE .../flats/{flatId}/members/{userId}`

**Complaints** (`/complaint`)
- `POST /complaint/societies/1/complaints` `{scope: FLAT|COMMON_AREA, flatId?, towerId?, categoryId, title, description?}`
- `GET .../complaints?status=OPEN&towerId=7&assignedTo=3&page=0&size=20` (all filters optional; `assignedTo` lists
  a staff member's jobs), `GET .../complaints/{id}`
- `POST .../complaints/{id}/assign` `{staffId}`, `/start`, `/resolve` `{note?}`, `/close`, `/reject` `{reason}`
- Comments: `POST .../complaints/{id}/comments` `{text}` (anyone who can see the complaint; up to 1,000 characters;
  not on closed or rejected complaints). Comments come back with the complaint.
- Photos: `POST .../complaints/{id}/photos/upload-url` `{contentType, sizeBytes}` -> PUT the file to the returned
  `uploadUrl` -> `POST .../complaints/{id}/photos` `{storageKey}`. Complaints come back with short-lived photo links.

Complaint categories (`categoryId`): 1 Plumbing, 2 Electrical, 3 Lift, 4 Water supply, 5 Security, 6 Housekeeping,
7 Parking, 8 Garden & amenities, 9 Other. Towers (`towerId`): 1 A1 Astoria, 2 C1 Celestia, 3 A2 Aster, 4 C2 Cosmos,
5 A3 Orion, 6 C3 Sirius, 7 B2 Radiant, 8 C4 Vega, 9 B1 Alpha, 10 A4 Polaris. Flats are labelled like `B2-503`
(floor 5, flat 3); find their IDs with `GET .../towers/{towerId}/flats`.

## Running it locally

Needs JDK 25 and Maven. Put the settings in a git-ignored `.env` in the project root (same names as
`deploy/complaint.env.example`: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PHOTO_*`, `AUTH_SECRET`, optionally
`OTP_DEMO_CODE`), then:

```bash
mvn clean verify                                   # all tests, including the architecture rules
java -jar app/target/app-1.0-SNAPSHOT.jar          # http://localhost:8080/api/...
```

Database tests use Testcontainers and are skipped when Docker is not available.

The web app (Node 24, from `frontend/.nvmrc`):

```bash
cd frontend
nvm use                      # Node 24
npm ci                       # first time only
npx ng serve                 # http://localhost:4200, its /api calls go to the live server (proxy.conf.json)
```

To use a local backend instead, point `proxy.conf.json` at `http://localhost:8080`.

## Deploying

`deploy/deploy.sh` builds, tests and deploys the backend to the Lightsail server; `deploy/deploy.sh frontend` builds
and publishes the web app. See `deploy/README.md`.
