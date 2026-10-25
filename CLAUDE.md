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
| Notification | `notification-service` | Generic | Outgoing messages |

- Each context has its own model and its own **ubiquitous language** (Evans ch. 2). The same word may mean different things in different contexts; never share entity classes across contexts.
- Contexts reference each other **by ID only** (e.g. `FlatId`, `UserId`), never by object reference.
- Calls to another service go through an **anticorruption layer** (Evans ch. 14): an outbound port interface in `application.port`, implemented by a client in `infrastructure.client` that translates the other service's DTOs into this context's own types.
- Asynchronous integration uses domain events. Later, when needed, publish them reliably with a transactional outbox table per service.

## Database per service
- One Neon database, **one Postgres schema per service** (`complaint`, `residency`, `identity`, `notification`), each with its own DB role. A service never reads or writes another service's schema.
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

## Security (who is calling, who may do what)
- The app module checks the login token (JWT, HS256, signed with a key derived from AUTH_SECRET) on every request;
  only `/identity/login/**` is public. Stateless, no sessions.
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

## Other design rules
- Multi-tenancy: every complaint and category carries `societyId`; always filter by it.
- Roles are scoped: RESIDENT, TOWER_ADMIN, SOCIETY_ADMIN, STAFF; a role without `towerId` is society-wide.
- Complaint scope: FLAT or COMMON_AREA (common-area complaints have no flatId).
- Complaint lifecycle (inside the Complaint aggregate): OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED, or REJECTED. Invalid transitions throw a domain exception. Every transition is recorded in status history and raises a domain event.
- Photos go to object storage, never in the DB; only the object key is stored. At most 5 per complaint, JPEG/PNG/WebP,
  at most 5 MB each (the Angular app resizes and re-encodes them, which also strips EXIF GPS data).
- Residents' personal data: collect only what's needed (India DPDP Act).

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
- Deploy: `deploy/deploy.sh frontend` (see deploy/README.md); the backend with `deploy/deploy.sh`.

## Neon / Spring Boot notes
- DB credentials come from environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`), one set per service. Never commit them.
- Use the Neon connection string with `sslmode=require`.
- HikariCP per service: small pool (max 3-5), `minimum-idle: 0` and a short `idle-timeout` so Neon can scale to zero.
- Expect a short delay on the first query after Neon has been idle.
- Each Spring Boot service needs roughly 250-400 MB RAM. Set `-Xmx` per service and size the Lightsail plan for the number of running services.

## Where things stand (2026-10-25)
Live at https://runwalltdfakepromises.com (web app) and /api (one Spring Boot app, 512 MB Lightsail). Done: all
contexts with their REST APIs, phone + OTP login, self sign-up with approval, photos, comments, the full complaint
lifecycle, and the web app for residents, staff and admins in three languages.

## Next step
1. Before real residents: SMS through MSG91 (after DLT approval); remove the demo users and `OTP_DEMO_CODE`;
   delete the SMOKE TEST complaints; have the Hindi and Marathi texts reviewed by native speakers; rotate the Neon
   password.
2. After the demo: decide the Lightsail plan (1 GB or 4 GB), then consider splitting the contexts into services.
3. Notifications (`notification-service`): tell residents when their complaint changes, staff when work is assigned.
4. Later hardening: a Content-Security-Policy header (allow the photo bucket's address for images and uploads).
