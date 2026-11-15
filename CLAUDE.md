# Society Complaint App

A complaint management app (web + mobile) for a housing society in Dombivli, built by Manish.
Work in **baby steps**: one small, working, testable step at a time. Explain briefly, then do it.
After every step, `mvn clean verify` must pass, including the architecture tests.

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

## Scale
- Now: 10 towers x 30 floors x 4 flats = 1,200 flats, ~6,000 residents
- Planned: expand to ~100 more towers nearby (~13,000 flats, ~66,000 residents)
- Load is low (a few thousand complaints/day at most). Do not add infrastructure that this load does not need.

## Stack (decided)
- Backend: Java + Spring Boot microservices (Maven multi-module), JPA/Hibernate, Flyway
- Frontend: Angular web app in `frontend/`, mobile-first and also desktop (see **Frontend** below)
- Database: PostgreSQL on **Neon** (Free plan, region AWS Singapore `aws-ap-southeast-1`)
- Hosting: **AWS Lightsail**, Singapore region (same region as the DB). Nginx in front routes `/api/<service>/**` to each service and serves the Angular build.
  - **For now: the 2 GB plan (upgraded from 512 MB on 2026-11-15, `-Xmx1g`) and ONE Spring Boot app.** Every bounded context
    is still its own Maven module, package, schema and ArchUnit boundary, but they are packaged into a single JVM.
    Cross-context calls go through the same `application.port` interfaces, implemented in-process instead of over HTTP.
    Split into separate services only when there is a reason to (the 2 GB plan has room for one or two more JVMs).
- Photo storage: **Lightsail Object Storage** (S3-compatible, Singapore), private bucket. The database stores only the
  object key (`complaints/{societyId}/{complaintId}/<uuid>.<ext>`), never a public URL; clients upload and view through
  short-lived presigned URLs. Bucket credentials come from environment variables, like the DB credentials.
- Login: phone number + OTP (identity module). SMS through MSG91 once DLT registration is approved; until then codes
  go to the server log, and the placeholder demo numbers 9000000001-4 accept OTP_DEMO_CODE. AUTH_SECRET (env only)
  signs login tokens and hashes OTP codes.
- Testing: JUnit 5, ArchUnit (`archunit-junit5`), Testcontainers for persistence tests

## Project structure
Maven multi-module (see the poms): `shared` (kernel), `architecture-rules`, one module per context (`core`, `residency`,
`identity`, `feed`, `workforce`), `app` (the one deployable Spring Boot app) and `frontend` (Angular).
- Residency is seeded with Runwal MyCity: 10 towers, flats labelled "<tower code>-<floor><unit>", e.g. B2-503.
- `com.mycity.runwal.integration` (in `app`) holds the in-process anticorruption adapters between contexts (e.g.
  `InProcessFlatDirectory`: complaint's FlatDirectory port -> Residency); it is the only code that may see two contexts.
- `notification-service` does not exist yet. Add a new module only when a baby step needs it.

## Bounded contexts (Evans, Part IV: Strategic Design)

| Context | Module | Type | Owns |
|---|---|---|---|
| Complaint Management | `core` | Core domain | Complaint, comments, attachments, status history, categories, SLA |
| Residency | `residency` | Supporting | Society, Tower, Flat, flat membership |
| Identity & Access | `identity` | Generic | App users, OTP, scoped roles |
| Public Feed | `feed` | Supporting | Posts, media evidence, moderation, reports, takedowns |
| Workforce | `workforce` | Supporting | Staff shifts (clock in / clock out), hours on duty |
| Notification | `notification-service` | Generic | Outgoing messages |

- Each context has its own model and its own **ubiquitous language** (Evans ch. 2). The same word may mean different things in different contexts; never share entity classes across contexts.
- Contexts reference each other **by ID only** (e.g. `FlatId`, `UserId`), never by object reference.
- Calls to another service go through an **anticorruption layer** (Evans ch. 14): an outbound port interface in `application.port`, implemented by a client in `infrastructure.client` that translates the other service's DTOs into this context's own types.
- Asynchronous integration uses domain events. Later, when needed, publish them reliably with a transactional outbox table per service.

## Database per service
- One Neon database, **one Postgres schema per service** (`complaint`, `residency`, `identity`, `feed`, `workforce`, `notification`), each with its own DB role. A service never reads or writes another service's schema.
- No foreign keys or joins across schemas. Cross-context references are plain ID columns.
- Each service owns its migrations with Flyway (`src/main/resources/db/migration/<schema>`), run by its own `Flyway`
  bean (`<Context>SchemaConfig` in `infrastructure.persistence`); JPA entities name their schema in `@Table(schema = ...)`.
  Each context prefixes its endpoints with `/<context>` (a `WebMvcConfigurer` in `infrastructure.config`); the app's
  context path is `/api`. Module tests use a test-only `@SpringBootApplication` in the context's base package.
- `db/schema.sql` and `db/seed.sql` are the original single-schema v1 draft already run on Neon (public schema). Treat them as a **reference only**. Recreate the tables per service through Flyway, drop cross-context FKs, then remove the public-schema tables once migrated.

## Strict DDD rules (Eric Evans, *Domain-Driven Design*, 2003)

### Layers (Evans ch. 4, Isolating the Domain: Layered Architecture)
Every context uses exactly these packages under `<groupId>.<context>`: `domain` (`model/<aggregate>`, `service`, `event`,
`exception`), `application` (`command`, `query`, `service`, `port`), `infrastructure` (`persistence`, `client`, `storage`,
`media`, `security`, `messaging`, `config`) and `interfaces.rest`. Allowed dependencies: `interfaces` -> `application` ->
`domain`; `infrastructure` -> `application`, `domain`; `domain` -> only the JDK and the shared kernel (LAYER rules enforce it).

### Building blocks (Evans ch. 5 and 6)
- **Domain model is framework-free.** No Spring, JPA, Jackson, Hibernate, validation annotations or Lombok in `domain`. Persistence uses separate JPA entity classes in `infrastructure.persistence`, mapped to and from the domain model.
- **Entities** have identity and a lifecycle. Identity is a typed value object (`ComplaintId`, not `Long`).
- **Value objects** are immutable: Java records or final classes with final fields, no setters, equality by value. Validate in the constructor.
- **No setters in the domain.** Use intention-revealing methods (Evans ch. 10, Supple Design): `complaint.assignTo(staffId)`, not `setAssignedTo(...)`.
- **Aggregates** (ch. 6) are consistency boundaries. Only the aggregate root is public outside its package; inner entities are package-private. Invariants are enforced inside the aggregate. Change one aggregate per transaction; reference other aggregates by ID.
- **Repositories** (ch. 6) exist only for aggregate roots. The interface lives in `domain.model.<aggregate>` and speaks the domain language; the implementation is an adapter in `infrastructure.persistence`.
- **Factories** (ch. 6) create complex aggregates when a constructor is not enough. They live with the aggregate.
- **Domain services** (ch. 5) hold domain logic that doesn't fit an entity. They are stateless and domain-only.
- **Application services** are thin: load aggregate, call domain behavior, save, publish events. No business rules. `@Transactional` is allowed only here.
- **Domain events** (from Evans' later *DDD Reference*): immutable records, named in past tense (`ComplaintRaised`, `ComplaintResolved`), implement the shared kernel `DomainEvent` interface.
- **Domain exceptions** extend a shared kernel `DomainException`. Never throw bare `RuntimeException` or `IllegalStateException` from the domain.

### Shared kernel (`shared` module, Evans ch. 14)
- Pure Java. No Spring, no JPA, and it must not depend on any service module.
- Contains only: building-block marker annotations (`@AggregateRoot`, `@DomainEntity`, `@ValueObject`, `@DomainService`, `@DomainRepository`, `@DomainFactory`, `@ApplicationService`, all with RUNTIME retention), `DomainEvent`, `DomainException`, a base `AggregateRoot` that records domain events, and cross-context ID types (`SocietyId`, `TowerId`, `FlatId`, `UserId`).
- Keep it minimal. Anything added here must be agreed by all contexts.

## Strict ArchUnit rules (`architecture-rules` module)

The rules live in `architecture-rules` (`LayerRules`, `DomainPurityRules`, `BuildingBlockRules`, `NamingRules`,
`BoundedContextRules`, `CodingRules`, `SharedKernelRules`), each with its ID (LAYER-xx, DDD-xx, BB-xx, NAME-xx, BC-xx,
CODE-xx); every context's `ArchitectureTest` runs them. Read the classes for the full catalogue.
- No `FreezingArchRule`, no ignore files, no `@Disabled`: rules are strict from day one. Keep `failOnEmptyShould=true`;
  only rules about *optional* building blocks use `.allowEmptyShould(true)` (BB-03, BB-06, NAME-03), never to silence
  a rule that should match something.
- Every rule has `.as("<ID>: <name>")` and a `.because(...)` with three parts: WHY (the principle, with the Evans
  chapter), FIX (what to do instead) and EXAMPLE (before/after). Follow an existing rule's shape. After adding a rule,
  break it once on purpose and check that `mvn test` explains the fix, then remove the violation.

## Security (who is calling, who may do what)
- The app module checks the login token (JWT, HS256, signed with a key derived from AUTH_SECRET) on every request;
  only `/identity/login/**` and the public feed (`/feed/public/**`: read, react, report; `/feed/share/**`) are public.
  Stateless, no sessions.
- Each context learns the caller through its own outbound port (`application.port.CurrentActor`, or `CurrentUser` in
  Identity), implemented in `infrastructure.security` by translating the token claims into the context's own model
  (`domain.model.actor.Actor`). Commands do not carry the caller's ID.
- Permission rules are domain logic: e.g. `ComplaintAccessPolicy` (domain service). Application services check them
  and throw `application.service.ActionNotAllowedException`, which the context's exception handler maps to 403.
- Identity checks admin rights against the stored user (not the token), so revoking a role works immediately.
- Spring beans with the same class name in two contexts clash in the one app: give such beans explicit names.
- The token carries the user's status and roles. `POST /identity/me/session` renews it from the stored user (it
  ends when the current login ends, so it never extends a login); the web app renews on every load, so an approval
  or a new role takes effect without a new code.
- Who lives in a flat (flat members) is shown only to the admins of its tower; everyone else gets flats without
  members (DPDP: show personal data only to those who need it). Names of other users are shown, phone numbers never
  (except to the society admin reviewing a sign-up).
- Public Feed authors are anonymous to the public: see **Public Feed** below.

## Other design rules
- Multi-tenancy: every complaint and category carries `societyId`; always filter by it.
- Roles are scoped: RESIDENT, TOWER_ADMIN, SOCIETY_ADMIN, STAFF; a role without `towerId` is society-wide.
- Complaint scope: FLAT or COMMON_AREA (common-area complaints have no flatId).
- Complaint lifecycle (inside the Complaint aggregate): OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED, or REJECTED. Invalid transitions throw a domain exception. Every transition is recorded in status history and raises a domain event.
- Photos go to object storage, never in the DB; only the object key is stored. At most 5 per complaint, JPEG/PNG/WebP,
  at most 5 MB each (the Angular app resizes and re-encodes them, which also strips EXIF GPS data).
- Residents' personal data: collect only what's needed (India DPDP Act).
- Add-ons (Residency owns them): each society has FEED and COMPLAINTS. The platform owner (`app_user.platform_owner`,
  set in the database only; the demo admin, user 1, for now) makes an add-on available; the society admin switches
  available ones on or off (`/residency/societies/{id}/add-ons/{addOn}[/availability]`). Each context refuses requests
  for a society whose add-on is off (an interceptor asking its own port, answered in `integration`); staff shifts
  and the staff report belong to COMPLAINTS. The web app lands on the feed when it is on, hides off add-ons, and keeps
  at most five tabs (admin tools live under Profile -> Manage).
- COMPLAINTS can also be switched off for single towers that handle complaints their own way (e.g. their own app)
  and back on when they join (`PUT .../add-ons/COMPLAINTS/towers/{towerId}`, society admin; `Society` keeps the
  set). Raising a complaint about such a tower (its flats or its common areas) is refused; society-wide areas and
  complaints already raised are not affected. Only COMPLAINTS works per tower.
- Automatic assignment (`core`, a switch per society, on for Runwal MyCity): the society admin says which categories
  each staff member handles (`/complaint/societies/{id}/auto-assignment[/staff/{staffId}]`). When a complaint is
  raised, resolved or rejected, a shift starts, or the setup changes, `AssignmentPlanner` gives the most urgent
  waiting complaints to free staff (on shift, no job assigned or in progress, the category, their towers): one job
  each, one transaction per complaint. Event-driven, never polled (Neon must be able to sleep). On shift comes
  from Workforce and staff towers from Identity, through ports answered in `integration`. Tower managers can be
  given categories too (lifts: City Lift's AMC technician has no app): they are "on call", so no shift, any number
  of jobs, only complaints in their own towers, and staff on shift come first. The history note
  "Assigned automatically" is translated by the web app; residents of an open complaint are told it waits.
- The feed is the home page for signed-in approved members: published posts from their society and every society
  within `feed.nearby-meters` (10 km) that uses the feed (`GET /feed/societies/{id}/feed`). Everyone else sees the
  society's published posts at `/public` (web app, no login; `GET /feed/public/societies/{id}/posts`).
- Reactions (`feed`, its own `Reaction` aggregate): anyone, signed in or not, reacts to a published post with one of
  five emoji, one per visitor and post (a keyed hash of the address and the post, as for reports); reacting again
  replaces it. Counts come with every post. No comments yet.
- Staff report (`core`, `GET /complaint/societies/{id}/reports/staff`): per staff member, complaints raised in the
  period and assigned to them (counts, median time to start and to fix, share fixed within SLA) plus their current
  backlog. Society admin sees everyone, a tower admin only their tower, staff only themselves. The Angular screen
  joins it with shift hours from `workforce`; no cross-context call on the server. A shift counts for at most 16 h.
- Shift evidence (`workforce`): clocking in and out needs a live front-camera selfie (never from the gallery) and
  sends the phone's location at that moment. Locations come from the browser, not photo metadata (the app strips
  EXIF). The server checks the location against the society's site (`workforce.sites.<id>` in application.yml:
  Runwal MyCity 19.186062, 73.074812, 300 m); outside or missing is flagged for review, never refused. Selfie keys
  `shifts/{societyId}/{staffId}/{issuedAtMillis}-{uuid}.jpg` must be the staff member's own and at most 10 minutes
  old. Tower admins and the society admin confirm or dispute ended shifts by comparing the selfies (no automatic
  face matching); nobody reviews their own shift. Selfies and coordinates are deleted 90 days after the shift
  started (nightly job); times, site checks and reviews stay. Nginx allows `geolocation` and `camera` for self.

## Public Feed (`feed` context)
Residents publish documented evidence of what the builder promised versus what exists; moderated before publishing.
The details (lifecycle, moderation, media phases, sharing, tests) are in `feed/CLAUDE.md`. Always, wherever you work:
- The author's `UserId` stays in the `feed` schema and never appears in any API response, DTO, log line or public page;
  only SOCIETY_ADMIN can see it, and every such access is written to the audit log.
- Only logged-in, approved residents can post; claims about the builder come only from residents' moderated posts.

## Frontend (`frontend/`)
Angular web app; its conventions are in `frontend/CLAUDE.md`, the public homepage and legal pages in
`frontend/landing/CLAUDE.md`.
- Deploy: `deploy/deploy.sh frontend` (see deploy/README.md); the backend with `deploy/deploy.sh`; Nginx changes need
  `deploy/deploy.sh setup`.
- The public homepage states no fact about the builder itself (residents' voice only); see `frontend/landing/CLAUDE.md`.

## Neon / Spring Boot notes
- DB credentials come from environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`), one set per service. Never commit them.
- Use the Neon connection string with `sslmode=require`.
- HikariCP per service: small pool (max 3-5), `minimum-idle: 0` and a short `idle-timeout` so Neon can scale to zero.
- Expect a short delay on the first query after Neon has been idle.
- Each Spring Boot service needs roughly 250-400 MB RAM. Set `-Xmx` per service and size the Lightsail plan for the number of running services.

## Where things stand (2026-11-08)
Live at https://runwalltdfakepromises.com (web app) and /api (one Spring Boot app, 2 GB Lightsail with a static IP). Done: all
contexts with their REST APIs, phone + OTP login, self sign-up with approval, photos, comments, the full complaint
lifecycle, and the web app for residents, staff and admins in three languages. A public homepage at `/`, `/hi/`
and `/mr/` for search engines; Google Search Console verification (a DNS TXT record) is the owner's step.

## Next step
1. Before real residents: SMS through MSG91 (after DLT approval); remove the demo users and `OTP_DEMO_CODE`;
   delete the SMOKE TEST complaints; take down the example feed posts (`deploy/demo-feed.sh remove`); have the
   Hindi and Marathi texts reviewed by native speakers; rotate the Neon password.
2. The Lightsail plan is 2 GB since 2026-11-15; consider splitting the contexts into services only when needed.
3. Notifications (`notification-service`): tell residents when their complaint changes, staff when work is assigned.
4. Later hardening: a Content-Security-Policy header (allow the photo bucket's address for images and uploads, and
   Google Fonts for the homepage). Before the homepage is shared widely: a lawyer reviews it with the domain name,
   and the society photos (from listing sites) are replaced by residents' own or their rights confirmed.
5. Public Feed (`feed`), in order: Post aggregate + moderation lifecycle (domain + unit tests) -> post, report and
   moderation APIs with phase 1 images -> public bucket + CDN publish/unpublish -> share endpoint -> frontend
   screens -> legal pages. Phase 2 (server-side compression and video) only after the plan upgrade in step 2.
