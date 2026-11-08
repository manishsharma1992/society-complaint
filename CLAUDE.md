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
  - **For now (until after the demo to the society secretary): the 512 MB plan and ONE Spring Boot app.** Every bounded context
    is still its own Maven module, package, schema and ArchUnit boundary, but they are packaged into a single JVM.
    Cross-context calls go through the same `application.port` interfaces, implemented in-process instead of over HTTP.
    Split into separate services only when the plan is upgraded (1 GB or 4 GB, decided after the demo).
- Photo storage: **Lightsail Object Storage** (S3-compatible, Singapore), private bucket. The database stores only the
  object key (`complaints/{societyId}/{complaintId}/<uuid>.<ext>`), never a public URL; clients upload and view through
  short-lived presigned URLs. Bucket credentials come from environment variables, like the DB credentials.
- Login: phone number + OTP (identity module). SMS through MSG91 once DLT registration is approved; until then codes
  go to the server log, and the placeholder demo numbers 9000000001-4 accept OTP_DEMO_CODE. AUTH_SECRET (env only)
  signs login tokens and hashes OTP codes.
- Testing: JUnit 5, ArchUnit (`archunit-junit5`), Testcontainers for persistence tests

## Project structure

```
society-complaint/            parent pom (packaging pom), dependency + plugin management only
├── shared/                   SHARED KERNEL: pure Java, no Spring, no JPA
├── architecture-rules/       ArchUnit rules (DDD + layering), used by every service's tests
├── core/                     Complaint Management context = the CORE DOMAIN (library jar)
├── residency/                Residency context: Society, Tower, Flat, Membership (library jar; seeded with
│                             Runwal MyCity: 10 towers, flats labelled "<tower code>-<floor><unit>", e.g. B2-503)
├── app/                      the one deployable Spring Boot app: main class in com.mycity.runwal, application.yml,
│                             depends on every context module (512 MB server, see Stack). Package
│                             com.mycity.runwal.integration holds in-process anticorruption adapters between
│                             contexts (e.g. InProcessFlatDirectory: complaint's FlatDirectory port -> Residency);
│                             it is the only code that may see two contexts.
├── identity/                 Identity & Access context: app users (phone + name only), phone + OTP login, self sign-up
│                             with society approval, scoped roles, JWT login tokens (library jar)
├── feed/                     Public Feed context: anonymous, moderated evidence posts (library jar)
├── workforce/                Workforce context: maintenance staff shifts, clock in / clock out (library jar)
├── notification-service/     (later) Push / WhatsApp / SMS
└── frontend/                 Angular web app, served by Nginx from /var/www/society-complaint
```

All modules above exist except `notification-service`. Add a new module only when a baby step needs it.

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
Every service uses exactly these packages under `<groupId>.<context>` (read groupId from `pom.xml`):

```
<context>
├── domain
│   ├── model/<aggregate>/   aggregate root, entities, value objects, repository interface, factory
│   ├── service/             domain services (stateless, domain logic only)
│   ├── event/               domain events
│   └── exception/           domain exceptions
├── application
│   ├── command/             use-case inputs that change state (immutable records)
│   ├── query/               use-case inputs that read state + read models
│   ├── service/             application services: orchestration and transactions only
│   └── port/                outbound ports to other contexts and external systems
├── infrastructure
│   ├── persistence/         JPA entities, Spring Data repositories, repository adapters, mappers
│   ├── client/              anticorruption layer clients for other services
│   ├── storage/             object storage adapters (photos)
│   ├── media/               external media tools (libvips, ffmpeg), feed phase 2 only
│   ├── security/            token signing, hashing (identity)
│   ├── messaging/           event publishing
│   └── config/              Spring configuration
└── interfaces
    └── rest/                controllers, request/response DTOs, DTO mappers
```

Allowed dependencies (nothing else):
- `interfaces` -> `application`
- `application` -> `domain`
- `infrastructure` -> `application`, `domain`
- `domain` -> nothing except the JDK and the shared kernel

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

### Setup
- `architecture-rules` is a plain jar with `archunit-junit5` as a compile dependency. It contains rule classes only (`LayerRules`, `DomainPurityRules`, `BuildingBlockRules`, `NamingRules`, `BoundedContextRules`, `CodingRules`) holding `public static final ArchRule` fields.
- Every service has one test class, `ArchitectureTest`, using `@AnalyzeClasses(packages = "<its base package>", importOptions = ImportOption.DoNotIncludeTests.class)` and `@ArchTest static final ArchTests rules = ArchTests.in(<RuleClass>.class)` for each rule class.
- The `shared` module has its own `ArchitectureTest` running `SharedKernelRules`.
- No `FreezingArchRule`, no ignore files, no `@Disabled`: rules are strict from day one. Keep ArchUnit's default `failOnEmptyShould=true` so a rule can't pass by matching nothing.
  Exception, per rule: rules about *optional* building blocks that a correct context may simply not have use
  `.allowEmptyShould(true)` (BB-03 inner entities, BB-06 domain services, NAME-03 clients). Never use it to silence a rule that should
  match something.

### Self-explanatory failure messages (mandatory for every rule)
Every rule must use `.as(...)` and `.because(...)` so the failure reads as a complete explanation:
- `.as("DDD-03: Domain layer must not use JPA")`: a rule ID and a one-line name
- `.because(...)` must contain three parts:
  1. **Why**: the principle, with the Evans chapter
  2. **Fix**: what to do instead, concretely
  3. **Example**: a short before/after hint

Example:
```java
public static final ArchRule DOMAIN_MUST_NOT_USE_JPA =
    noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
        .as("DDD-03: Domain layer must not use JPA")
        .because("""
            WHY: The domain model must be isolated from technical concerns (Evans ch. 4, Isolating the Domain).
            FIX: Remove JPA annotations from the domain class. Create a separate JPA entity in
                 infrastructure.persistence and map it in a <Aggregate>Mapper.
            EXAMPLE: domain.model.complaint.Complaint (no annotations)
                     -> infrastructure.persistence.ComplaintJpaEntity (@Entity) + ComplaintMapper""");
```

After writing the rules, verify the messages: add one deliberate violation, run `mvn test`, check that the output tells a developer exactly what is wrong and how to fix it, then remove the violation.

### Rule catalogue (implement all of these)

**Layers (LAYER-xx)**
- LAYER-01: Layered architecture: `interfaces` -> `application` -> `domain`; `infrastructure` -> `application`, `domain`; no other direction.
- LAYER-02: `domain` must not depend on `application`, `infrastructure` or `interfaces`.
- LAYER-03: `application` must not depend on `infrastructure` or `interfaces`.
- LAYER-04: `interfaces` must not depend on `domain` or `infrastructure` (controllers talk only to application services and return DTOs, never domain objects).
- LAYER-05: No package cycles within a service (slices by top-level subpackage and by aggregate).

**Domain purity (DDD-xx)**
- DDD-01: Domain must depend only on `java..`, the shared kernel and its own context's domain.
- DDD-02: Domain must not use Spring (`org.springframework..`).
- DDD-03: Domain must not use JPA or Hibernate.
- DDD-04: Domain must not use Jackson, Bean Validation (`jakarta.validation..`) or Lombok.
- DDD-05: Domain classes must not have public methods named `set*`.
- DDD-06: Domain must not throw `RuntimeException`, `IllegalStateException` or `IllegalArgumentException` directly; use subclasses of `DomainException`.

**Building blocks (BB-xx)**
- BB-01: Classes annotated `@AggregateRoot` must reside in `domain.model..`.
- BB-02: Classes annotated `@ValueObject` must be records or have only final fields.
- BB-03: Classes annotated `@DomainEntity` (non-root entities) must not be public.
- BB-04: `@DomainRepository` interfaces must reside in `domain.model..`, be interfaces, and only be declared for aggregate roots (their name must be `<AggregateRoot>Repository`).
- BB-05: Classes implementing `DomainEvent` must reside in `domain.event`, be records, and have past-tense names (must not end in a verb stem such as `Create`, `Update`, `Raise`; enforce with an allowlist of past-tense suffixes like `ed`, `Opened`, `Closed`).
- BB-06: `@DomainService` classes must reside in `domain.service` and have no fields except final ones.
- BB-07: `@ApplicationService` classes must reside in `application.service`.
- BB-08: `@Transactional` may only be used in `application.service`.
- BB-09: JPA `@Entity` classes must reside in `infrastructure.persistence` and be named `*JpaEntity`.
- BB-10: Spring Data repositories must reside in `infrastructure.persistence` and only be used there.

**Naming (NAME-xx)**
- NAME-01: Classes in `interfaces.rest` annotated `@RestController` must be named `*Controller`.
- NAME-02: Records in `application.command` must be named `*Command`; in `application.query`, `*Query` or `*View`.
- NAME-03: Classes in `infrastructure.client` must be named `*Client` or `*Adapter`.
- NAME-04: Classes in `infrastructure.persistence` implementing a domain repository must be named `*RepositoryAdapter`.

**Bounded contexts (BC-xx)**
- BC-01: A service must not depend on another service's packages; only on itself, the shared kernel, and libraries.
- BC-02: The shared kernel must not depend on any service, Spring, JPA or Jackson.
- BC-03: Classes in `application.port` must be interfaces.
- BC-04: Only `infrastructure.client` may call other services over HTTP (`RestClient`, `WebClient`, `RestTemplate`).

**Coding (CODE-xx)**
- CODE-01: No field injection (`@Autowired` on fields); use constructor injection.
- CODE-02: No `System.out`/`System.err`; use SLF4J.
- CODE-03: No `java.util.Date` or `java.sql.Timestamp` in domain or application; use `java.time`.
- CODE-04: `java.lang.ProcessBuilder` and `Runtime.exec` may only be used in `infrastructure.media` (libvips and ffmpeg parse
  untrusted files; keep that code in one audited place).

## Security (who is calling, who may do what)
- The app module checks the login token (JWT, HS256, signed with a key derived from AUTH_SECRET) on every request;
  only `/identity/login/**` and the read-only feed endpoints `/feed/public/**` and `/feed/share/**` are public.
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
- The feed is the home page and, for now, only for signed-in approved members: published posts from their society
  and every society within `feed.nearby-meters` (10 km) that uses the feed (`GET /feed/societies/{id}/feed`).
  `/feed/public/**` and `/feed/share/**` need a login until the legal pages are published.
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

Purpose: residents publish documented evidence (photos, text, later short videos) of construction defects and
commitments the developer has not met. Published posts are public and easy to share.
Same rules as every context: its own module (library jar packaged into `app`), schema `feed`, endpoints under
`/feed`, DDD and ArchUnit. Cross-context needs (e.g. the poster's tower) go through ports implemented in
`com.mycity.runwal.integration`.
Ubiquitous language: Post, Evidence (a media item), Moderation, Report, Takedown.

### Anonymous to the public, verified internally
- Only logged-in, approved residents can post (via `CurrentActor`). No unauthenticated posting: it prevents spam
  and fake posts, and keeps the platform accountable.
- The public sees "Verified resident", plus the tower only if the author chooses to show it.
- The author's `UserId` is stored only in the `feed` schema and must never appear in any API response, DTO, log line
  or public page. Only SOCIETY_ADMIN can see it, and every such access is written to the audit log.
- At most 5 posts per user per day.

### Moderation before publishing
- Post lifecycle (Post aggregate): PENDING_REVIEW -> PUBLISHED or REJECTED; PUBLISHED -> HIDDEN -> PUBLISHED or
  REMOVED. Invalid transitions throw a domain exception; every transition raises a domain event.
- Moderators: SOCIETY_ADMIN for now. Add a scoped MODERATOR role in Identity only when the committee asks for it.
- Content policy, shown to authors before they post and checked by moderators:
  - factual and documented: what, where (tower or common area), when, and what was promised (brochure, agreement,
    approved plan) versus what exists
  - no names, faces, phone numbers or other personal details of individual employees or private people
  - no abusive language, no threats, no unverified allegations of crimes
- Anyone, even without logging in, can report a published post; 3 reports hide it until a moderator reviews it.
- Every moderation action goes to an append-only audit log (who, what, when, why).

### Media storage
- Uploads go to the existing private bucket under `feed/{societyId}/{postId}/<uuid>.<ext>` through presigned URLs,
  as for complaint photos.
- Shared links must keep working, so published media can't use short-lived presigned URLs. When a post becomes
  PUBLISHED, its files are copied to a separate **public** Lightsail bucket served through a Lightsail distribution
  (CDN). When it becomes HIDDEN or REMOVED, the public copies are deleted and the CDN cache is invalidated.
  Media of pending or rejected posts never becomes public.

### Media compression and metadata, in two phases
The server has 512 MB today, which is not enough for libvips or ffmpeg next to the JVM. So:

**Phase 1 (now): images only**
- At most 5 photos per post; JPEG/PNG/WebP; at most 5 MB each; check the type by magic bytes, not by extension;
  reject images over 50 megapixels.
- The Angular app compresses before upload with the existing photo resize in `src/app/shared` (it also strips
  EXIF). Reuse it; do not write a second one.
- Server-side guarantee, because the poster's anonymity must not depend on the client alone: when an upload is
  confirmed, the server streams the object and checks it with the `metadata-extractor` library. If it still
  contains GPS, EXIF device data, XMP or IPTC, the upload is rejected with a clear reason.

**Phase 2 (only after the Lightsail plan is upgraded to at least 1 GB): server-side processing and video**
- Evidence lifecycle: UPLOADED -> PROCESSING -> READY or FAILED. A post can be submitted for moderation only when
  all its evidence is READY.
- Job queue: a DB table in `feed`, polled by a `@Scheduled` worker with `SELECT ... FOR UPDATE SKIP LOCKED`.
  One job at a time; at most 3 retries; timeout 30 s per image and 5 min per video; kill the process on timeout.
  No message broker.
- Ports `ImageProcessor` and `VideoTranscoder` in `application.port`; adapters `VipsImageProcessor` and
  `FfmpegVideoTranscoder` in `infrastructure.media` (CODE-04). Call the tools through `ProcessBuilder` with an
  argument list, never through a shell. Temp files in a dedicated directory, deleted in a `finally` block.
- Images (libvips `vips` CLI; now HEIC is also accepted): auto-rotate, strip all metadata, convert to sRGB, resize
  without upscaling. Outputs: `thumb` 320 px WebP q75, `full` 2048 px WebP q82, `og` 1200x630 JPEG q82
  (for link previews). Target: 150-500 KB per photo.
- Videos (FFmpeg): at most 60 s / 50 MB. Validate with `ffprobe` first: MP4, MOV or WebM, one video stream,
  <= 60 s; never pass an unvalidated file to ffmpeg. Encode H.264 + AAC in MP4 with capped CRF:
  ```
  ffmpeg -hide_banner -nostdin -protocol_whitelist file -i <in>
    -map 0:v:0 -map 0:a:0? -map_metadata -1 -map_chapters -1
    -vf "scale='min(1280,iw)':'min(720,ih)':force_original_aspect_ratio=decrease:force_divisible_by=2,fps='min(30,source_fps)'"
    -c:v libx264 -preset medium -crf 26 -maxrate 2M -bufsize 4M -profile:v high -pix_fmt yuv420p
    -c:a aac -b:a 96k -ac 2
    -movflags +faststart -fflags +bitexact <out.mp4>
  ```
  (If the installed ffmpeg doesn't accept the `fps` expression, keep the intent: cap at 30 fps, never increase.)
  Why: H.264 plays everywhere, including WhatsApp, X and all browsers; CRF gives the best quality per byte;
  `maxrate`/`bufsize` cap shaky, high-motion clips; `+faststart` lets playback start early; `-map_metadata -1`
  removes GPS and device data. Target: 5-15 MB per 60 s clip. Also produce a poster frame at 1 s
  (WebP + 1200x630 JPEG).
- Server setup: install `libvips-tools` (with HEIF/WebP) and `ffmpeg`, and keep them updated, since they parse
  untrusted files.

### Sharing
- Each published post has a stable URL `/p/{slug}`.
- WhatsApp and X read Open Graph tags without running JavaScript, so the Angular app alone gives blank previews.
  Nginx routes `/p/{slug}` to a small public endpoint in the feed context, `GET /feed/share/{slug}`, which returns
  minimal HTML with `og:title`, `og:description` and `og:image` (the public CDN URL), then loads the Angular post
  page. No Angular SSR and no Node process: there is no memory for it on 512 MB.
- Public read endpoints (`/feed/public/**`, `/feed/share/**`) need no login and are cacheable; everything else needs
  the token.

### Frontend
- Screens: Feed and Post (public, no login), New post (residents; shows the content policy first), Moderation queue
  (SOCIETY_ADMIN). All texts in en, hi and mr. Post content written by users is not translated.

### Before launch
- Terms of Use, Community Guidelines (the content policy), Privacy Policy and Grievance Redressal are published in
  all three languages (`/legal/`). Still to do: the Grievance Officer's name and contact, and a report link on
  every post.
- Have a lawyer review the content policy and the takedown process.

### Tests
- Unit tests for the Post and Evidence lifecycles and the report threshold.
- Phase 1: a JPEG with GPS EXIF is rejected; a wrong type disguised by its extension is rejected.
- Phase 2: fixtures with GPS EXIF, an iPhone HEIC, and a rotated portrait video with location metadata. Assert
  output size, dimensions and orientation, and **no GPS or device metadata in any output** (`metadata-extractor`
  for images, `ffprobe` for video).
- No feed API response or public page contains the author's ID or name.

## Frontend (`frontend/`)
- Angular 22 (standalone, zoneless, signals), Node 24 from `frontend/.nvmrc` (use nvm; the system Node is too old).
  UI components: Spartan UI (`libs/ui`, shadcn-style, imported as `@spartan-ng/helm/*`), Tailwind 4, Lucide icons.
- Design: Hallmark "Atelier" theme with the Runwal MyCity brand green. All colours and fonts are tokens in
  `src/theme/tokens.css` (renamed to avoid Spartan clashes: `--color-brand`, `--color-stock`, `--color-ink-muted`);
  `src/styles.css` maps Spartan's shadcn variables onto them. Light mode only. Body text 17px, touch targets 44px+.
- Three languages, chosen by the user and remembered on the device: English, Hindi, Marathi (Transloco,
  `public/i18n/{en,hi,mr}.json`). Every new text goes into all three files with the same keys. Category names are
  translated by their English name (`category.<name>`).
- Structure: `src/app/core` (API services, session, roles, guards), `src/app/shared` (complaint card and list,
  language switch, photo resize), `src/app/features/<screen>`. Screens by role: residents see Complaints and Profile;
  staff also Jobs; admins also To assign; the society admin also Sign-ups. Users who are not yet approved land on
  sign-up (`/register`). The UI only decides what to offer: the server checks every action.
- `ng serve` proxies `/api` to the live server (`proxy.conf.json`). Build: `npx ng build` (budgets in angular.json).
- Deploy: `deploy/deploy.sh frontend` (see deploy/README.md); the backend with `deploy/deploy.sh`. It installs every
  built file before the app's own `./index.html` (only that one is held back, so the homepage's `hi/` and `mr/`
  `index.html` files ship too). Nginx changes (e.g. `location = /`) need `deploy/deploy.sh setup`.
- The app's screens carry `<meta name="robots" content="noindex">`: only the public homepage is for search engines.

### Public homepage (`frontend/landing/`)
- Plain HTML, not Angular, so search engines and link previews read it without JavaScript. Nginx serves
  `public/home.html` at `/` and `public/hi/index.html`, `public/mr/index.html` at `/hi/`, `/mr/` (hreflang-linked;
  `robots.txt`, `sitemap.xml` and `og-image.png` sit beside them). Visitors already signed in go to `/start`, which
  sends them on to their home in the app.
- Edit `landing/template.html` (layout, styles, script) and the texts in `landing/build_landing.py` (en, hi and mr
  must have the same keys), then run `python3 frontend/landing/build_landing.py`; commit its outputs.
- Voice: the residents' (“You paid for a promise.”). It states no fact about the builder itself: claims come only
  from residents' own moderated posts. The example post on the page is labelled as one.
- Motion (motion.dev, a frontend dev dependency) drives the scroll scenes; the build copies it to
  `public/vendor/motion.js`. With reduced motion, or if it fails to load, every section is a plain, visible screen.
- Font: Noto Sans with Noto Sans Devanagari (Google Fonts), so the three languages match. App screenshots in
  `public/images/app/<lang>/` were captured from the built app with a mock API and sample data; recapture them
  when those screens change. Never use the feed's demo posts in them (they contain invented claims).
- Legal pages at `/legal/`, `/hi/legal/`, `/mr/legal/` (Disclaimer, Terms of Use, Privacy Policy, Community
  Guidelines, Grievance Redressal), built by the same script from `landing/legal.html` and `landing/legal_texts.py`
  (a small Markdown subset; English prevails, hi and mr are translations and say so). Courts: Thane. The Privacy
  Policy must list everything the app stores; update it with any new personal data. The Grievance Officer's details
  are "To be announced" until the owner names one. The footer links every section.
- One header and one footer for the homepage and the legal pages: `landing/shared.css` plus `header()` and `footer()`
  in `build_landing.py`; change them there, never per page. The header is fixed (clear over the homepage photo,
  a frosted bar once the page scrolls, solid when transparency is reduced); pages keep `scroll-padding-top` so
  section jumps and keyboard focus land below it. On a phone the header shows the logo without the name.
- Themes (homepage and legal pages only; the Angular app stays light): automatic (the system's
  `prefers-color-scheme`), light or dark, cycled by the header button and saved as `mycity.theme` in
  localStorage, applied in `<head>` before paint. Colour roles are tokens in `shared.css` (`--surface`,
  `--on-surface`, `--surface-2`, ...); use them, never fixed colours, so every section follows the theme. Photo
  sections keep their dark scrim and light text (`--on-photo`) in both themes.

## Neon / Spring Boot notes
- DB credentials come from environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`), one set per service. Never commit them.
- Use the Neon connection string with `sslmode=require`.
- HikariCP per service: small pool (max 3-5), `minimum-idle: 0` and a short `idle-timeout` so Neon can scale to zero.
- Expect a short delay on the first query after Neon has been idle.
- Each Spring Boot service needs roughly 250-400 MB RAM. Set `-Xmx` per service and size the Lightsail plan for the number of running services.

## Where things stand (2026-11-08)
Live at https://runwalltdfakepromises.com (web app) and /api (one Spring Boot app, 512 MB Lightsail). Done: all
contexts with their REST APIs, phone + OTP login, self sign-up with approval, photos, comments, the full complaint
lifecycle, and the web app for residents, staff and admins in three languages. A public homepage at `/`, `/hi/`
and `/mr/` for search engines; Google Search Console verification (a DNS TXT record) is the owner's step.

## Next step
1. Before real residents: SMS through MSG91 (after DLT approval); remove the demo users and `OTP_DEMO_CODE`;
   delete the SMOKE TEST complaints; have the Hindi and Marathi texts reviewed by native speakers; rotate the Neon
   password.
2. After the demo: decide the Lightsail plan (1 GB or 4 GB), then consider splitting the contexts into services.
3. Notifications (`notification-service`): tell residents when their complaint changes, staff when work is assigned.
4. Later hardening: a Content-Security-Policy header (allow the photo bucket's address for images and uploads, and
   Google Fonts for the homepage). Before the homepage is shared widely: a lawyer reviews it with the domain name,
   and the society photos (from listing sites) are replaced by residents' own or their rights confirmed.
5. Public Feed (`feed`), in order: Post aggregate + moderation lifecycle (domain + unit tests) -> post, report and
   moderation APIs with phase 1 images -> public bucket + CDN publish/unpublish -> share endpoint -> frontend
   screens -> legal pages. Phase 2 (server-side compression and video) only after the plan upgrade in step 2.
