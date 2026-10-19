# Hamoni — coding standards

The technical standards every person **and every agent** follows when building
Hamoni. This is a **living document**: it grows as the codebase does. Where a
rule here conflicts with [`ARCHITECTURE-DECISIONS.md`](./ARCHITECTURE-DECISIONS.md),
the decision record wins for *what* the system does and this document wins for
*how* it is written. Raise a change rather than diverging silently.

The document has three parts:

- **Part I — Common.** Rules that apply to all code, back end and front end.
- **Part II — Backend.** Kotlin and Quarkus (reactive).
- **Part III — Frontend.** Angular, PrimeNG, Tailwind and Quinoa.

Each part ends with an **anti-patterns** section: the mistakes to avoid
explicitly.

---

# Part I — Common

## 1. Language

This section covers the **language the software is written in**. The language
shown to a user — the words and messages in the interface — is a separate
concern, covered under UI design and language (Part III).

### 1.1 The primary language is Kotlin

- **Kotlin is the primary language.** All production code is written in Kotlin.
- **Java is used only when a bug or a restriction prevents Kotlin** — for
  example the logging facade `JBoss Logging`, which is Java-first (ADR-0036). A
  Java file must name the restriction that required it.
- Use the Kotlin version and compiler pinned by the shared Gradle convention
  scripts. Do not override the pinned version locally.
- **Target Java 25.** Do not use language features above the pinned target, and
  do not write code that assumes an older one.

### 1.2 Kotlin style

- Follow the **official Kotlin coding conventions**, enforced by **ktlint** and
  **detekt** through the shared build. A build that fails lint fails the change.
- **Explicit API mode** for published library projects: public declarations carry
  an explicit visibility and an explicit return type.
- **Immutability first.** Prefer `val`; prefer immutable collections; prefer data
  classes for value objects. Mutability is a deliberate, local decision — a JPA
  entity is the one standing exception, where properties are `var` (§10).
- **Null safety is not optional.** No `!!`, and no platform-type leakage across a
  boundary. Model absence explicitly (a nullable type or a sealed type) — never a
  sentinel value.
- **No wildcard imports.** No unused imports.
- **Sealed hierarchies** for domain states and results. An exhaustive `when` over
  a sealed type is required; do not add an `else` branch that hides a new case.
- **Errors are values where recoverable.** Use a result type, not an exception,
  for an expected business outcome. Exceptions are for programming errors and
  unrecoverable faults.
- **No direct console output** (`println`). Use the logging facade (§14).

## 2. Project structure and namespaces

- The three directories `control-room/`, `process-runner/` and `common/` are
  **holding areas for one or more build projects** (ADR-0003). The projects
  inside are named for what they are (for example a user-interface and a
  data-model project inside `control-room/`).
- Each project uses the standard Gradle layout: `src/main/kotlin`,
  `src/main/resources`, `src/test/kotlin`, `src/test/resources`.
- Base package: `ai.hamoni`, with the area as the next segment (for example
  `ai.hamoni.control_room`, `ai.hamoni.process_runner`, `ai.hamoni.common`).
  *(Confirm the base group identifier with the shared Gradle scripts; see
  ADR-0002.)*
- Organise by **feature or domain**, not by technical layer alone. A feature owns
  its interface, service, persistence and tests together.
- **Dependency direction is one-way.** Projects under `control-room/` and
  `process-runner/` may depend on projects under `common/`; the two application
  areas never depend on each other; `common/` depends on neither.
- Keep a shared project **dependency-light**: a type belongs in `common/` only
  when both application areas need it and it brings no meaningful transitive
  dependency.

## 3. Dependencies and version management

- Every library coordinate and version is declared once, centrally, in
  `gradle/libs.versions.toml` (ADR-0002). No version literal appears in a project
  build file; no dynamic versions (`1.+`); no snapshots in a release build.
- **A new library is not introduced without the approval of the project
  leadership.** Propose it with a reason; on approval, add it to the version
  catalog, not inline.
- The build uses the **Kotlin DSL** only (`build.gradle.kts`, `settings.gradle.kts`).
- Common configuration (Kotlin, Quarkus, testing, lint, publishing) comes from the
  shared Hamoni Gradle convention plugins. Apply the plugin and declare only the
  project's own dependencies; never copy convention logic into a project.
- **Reproducible builds:** pin and commit the Gradle wrapper; enable dependency
  verification where the shared scripts do.

## 4. Security and secrets

- **No passwords, ever.** Authentication is OpenID Connect; Hamoni stores no
  password. A person is a provider-and-subject pair, and the same subject may
  belong to many organisations (ADR-0022).
- **Authentication and authorisation are separate** (ADR-0041). Authentication
  proves identity through the provider; entering an organisation is a separate
  grant. Never treat a signed-in person as a member, and never resolve an
  authorisation failure with a sign-in control.
- **Organisation isolation is enforced at the data layer**, by **schema**: a
  global schema for authentication profiles and one schema per organisation for
  everything else (ADR-0025, ADR-0052). A missing or wrong tenant selection is a
  defect.
- **The backend is stateless.** No session or request state is held server-side;
  the credential identifies the caller only, and the organisation travels in the
  URL (ADR-0047, ADR-0048).
- **There are no API keys.** Every actor — person, system account or agent — is
  an OIDC principal of its own (ADR-0050, ADR-0051).
- **Identity attributes are owned by the OIDC claims.** Name and email are read,
  never edited in Hamoni (ADR-0056).
- **Organisation access requires an enabled provider and a profile**; revocation
  is removal from the organisation, not a change of team membership (ADR-0043).
- **Never reveal an organisation on an unauthenticated surface** — render no
  organisation name and give no way to enumerate which organisations exist
  (ADR-0045).
- **Authorisation is explicit** at the service boundary: view · edit · deploy ·
  start, resolved from the security model (ADR-0024). Do not invent per-screen
  checks.
- **Least privilege** for service accounts and database roles.
- **Secrets** come from configuration or a secret store, by environment
  variable — never in the repository, never in logs, never in a test fixture with
  a real value.

## 5. Documentation and decisions

- **Every function, method and class carries documentation of its intended
  function.** A short one-line message is enough where the intent is obvious or
  determinable from the file, the name or the location.
- **Use inline comments for the non-obvious.** A code-level comment documents
  something done in an abstract or non-obvious way — the reason, the trick, the
  thing a reader would otherwise have to reverse-engineer. Do not narrate what
  the code already says.
- **Names must be human-readable.** A **variable** names the data it holds; a
  **function, method and class** names what it does or is. No cryptic
  abbreviations, and no name that needs a comment to decode.
- **Document any deviation from these standards or the architecture.** Where a
  method, function or class breaks a coding standard or an architectural decision,
  the code **must carry documentation** at that declaration that names the
  standard or decision being deviated from and **why** it cannot be met here. An
  undocumented deviation is a defect.
- **Every architectural decision is recorded** as an ADR in
  [`ARCHITECTURE-DECISIONS.md`](./ARCHITECTURE-DECISIONS.md). A code change that
  makes a new decision writes the record in the same change.
- **Long-form documentation** is AsciiDoc under `docs/`, an Antora component
  (ADR-0040). Anything here that stabilises graduates into Antora.

## 6. Commits and reviews

- One concern per commit; a clear message stating what and why.
- Changes are small and reviewable; the build, lint and tests pass before review.
- **A pull request must be readable by a human.** Code may be authored with AI,
  but **a human reviews the code for quality on every pull request**, so the
  change must be understandable by a person. A pull request that is not readable
  or understandable by a human is **rejected** — this is the reason names are
  human-readable and intent is documented (§5).
- No commented-out code, no debug scaffolding, no TODO left without a tracked
  note.

## 7. Anti-patterns — Common

Do not:

- **Store credentials anywhere in the project.** No secret, key, token or
  password in the repository, in a build script, in a committed configuration
  file, in a test fixture with a real value, or in a log. Secrets come from the
  environment or a secret store (§4).
- Hard-code a library version outside `gradle/libs.versions.toml`, or introduce a
  library without leadership approval (§3).
- Copy convention logic from the shared Gradle scripts into a project (§3).
- Let one application area depend on the other, or on `common/` in a way that
  drags a framework into a shared library (§2).
- Use `!!`, a wildcard import, a sentinel value for absence, or an `else` branch
  that hides a new sealed case (§1.2).
- Use an exception for an expected business outcome (§1.2).
- Write to the console instead of the logging facade (§1.2).
- Leave commented-out code, debug scaffolding, or an untracked TODO behind (§6).
- Deviate from a coding standard or an architectural decision without documenting
  the deviation and its reason at the point of the code (§5).

---

# Part II — Backend

## 8. Runtime and reactive policy

- The backend runs on **Quarkus in Reactive mode** (ADR-0032). **All libraries
  and dependencies must be reactive.** A library that only offers blocking access
  is not adopted.
- **Never block an input/output thread.** If a call could block, it belongs off
  the event loop or must be replaced with a reactive alternative.
- Prefer a Quarkus extension over a hand-rolled integration. Only drop to a
  lower-level dependency when no extension exists, and record why in an ADR.

## 9. Domain modelling

- **Model state machines as sealed types.** The process state model
  (Active · Pend · Exception · Paused · Completed · Cancelled) and the activity
  state model (Available{Waiting|open} · Assigned · Active · Completed, plus
  Cancelled) are first-class domain types, not string constants (ADR-0012,
  ADR-0013).
- **Value objects are immutable and self-validating.** Construct a valid object
  or fail; never carry an invalid value and hope a caller validates it.
- **Identifiers are `java.util.UUID`**, generated by the application at the
  boundary — never a raw string and never a bespoke value class. The interface
  presents an identifier as a string, because OpenAPI and TypeScript have no UUID
  type; the backend holds the `UUID`, and the database holds its native UUID type.
- **Equality uses the technical identifier.** A Hamoni object has **no user-facing
  business key** — a team is *named* for people, but names are not unique — so
  `equals` and `hashCode` compare the technical `UUID`, never a name, label or
  other display field.
- **Money and other decimals are `BigDecimal`** — never a binary floating-point
  type — with **precision 15 and scale 5** (10 integer digits, 5 fractional).
  Where a value is money the **currency is explicit**. Because JSON has a single
  number type, the serialisation maps numbers to `BigDecimal` explicitly rather
  than to `double`. **Time** is `java.time.Instant` in Coordinated Universal
  Time; convert only at the edge. Do not do raw epoch arithmetic.
- **Business rules are data, not code branches**, where the product models them
  as data (ADR-0016). A business rule is a project-scoped artefact, not a
  conditional in a service.
- **Reuse one type per concept where possible.** The same class moves from the
  database to the interface; what is exposed is controlled by **serialisation
  annotations**, not by copying into a parallel transport or domain type. Hide or
  remove fields rather than mapping to a duplicate object. For a list or a
  search, query and return a **lighter projection view**; a second call retrieves
  the full object (ADR-0059).

## 10. Persistence

- Data access uses **Hibernate ORM with Panache in its reactive form** (ADR-0033).
- **Schema is migrated, never auto-generated in production.** Use **Flyway**;
  every schema change is a new, forward-only migration file, and an applied
  migration is never edited (ADR-0035).
- **Transactions are explicit and short.** No remote call inside a transaction.
- **Optimistic locking** on aggregates that can be edited concurrently; surface a
  conflict as a domain outcome, not a server error.
- **Queries live on repositories**; no query construction in a resource or a
  transport type.
- **Isolation is by schema, not by a discriminator column** (ADR-0052). Global
  tables are addressed with an explicit schema qualifier; every other table
  belongs to an organisation schema whose name is the organisation identifier.
  The current tenant is resolved from the organisation path segment and set for
  the request (Hibernate tenant resolution); this is the intended mechanism, to
  be confirmed under reactive Panache.
- **Migrations are split** into global and per-organisation sets, and applied to
  every organisation schema (ADR-0035, ADR-0052). Creating an organisation
  creates and migrates its schema. A rolling deployment may run two application
  versions against one schema, so an upgrade may only make **safe, additive
  changes** (ADR-0058, Proposed).
- **No cross-schema foreign key.** Structural foreign keys live within a schema.
- **Identity attributes come from the OIDC claims**; store them as a cached
  projection refreshed on sign-in, never as an editable source of truth
  (ADR-0056).
- **Naming maps across the boundary.** Tables and columns are `snake_case`; the
  Kotlin and TypeScript identifiers are the corresponding `camelCase`, mapped
  automatically (a `given_name` column is `givenName`).
- **Every table's technical identifier is one column, `id`** — a `java.util.UUID`
  stored as the database's native UUID type. A **foreign key is named after its
  table**: the column `<table>_id`, the property `<table>Id` (for example
  `project_id` / `projectId`).
- **Times are `java.time.Instant`** — never a Kotlin time type, which does not
  serialise well with Hibernate.
- **A versioned table uses an `active_from` / `active_to` pair.** `active_from` is
  **inclusive** and `active_to` is **exclusive**, so a replacement's `active_from`
  may equal the previous row's `active_to` without two rows being valid at one
  instant. This row-level validity is a different thing from ADR-0010's linear,
  immutable **process** version.
- **A JPA entity's properties are `var`**, never `val` — Hibernate must be able to
  set them (§1.2).
- **A projection uses `val`** and exposes a constructor that builds it from the
  main type (ADR-0059).

## 11. Interface

- The HTTP interface uses **Quarkus REST (RESTEasy Reactive)** (ADR-0034). It is
  non-blocking end to end.
- Resources are **thin**: validate, delegate to a service, map to a response. No
  business logic in a resource.
- The public entry point for starting a process is
  `POST /api/{organisationId}/processes/{processId}/instances`, with the body
  validated against the process's state schema (ADR-0029, ADR-0048).
- **Errors use the problem-details format (`application/problem+json`, RFC
  9457).** No stack traces on the wire, and no internal identifiers in error
  messages.
- **Validation at the boundary.** A payload is validated against the process's
  declared state data (ADR-0017); a mismatch raises a mapped exception.
- **One type crosses the boundary.** The interface carries the same class the
  database holds, with **serialisation annotations** controlling which fields are
  exposed; a list or a search returns a **lighter projection view** and a detail
  call returns the full object. Exposure is deliberate, never accidental
  (ADR-0059).
- **Versioning** appears in the path only when a breaking change requires it;
  additive change is preferred. Record a breaking change in an ADR.
- The interface is organised into **three annotated groups** (ADR-0049): **A**
  authentication and account under `/api/auth/`, and **B** business and **C**
  screen flow under `/api/{organisationId}/`. Groups A and B are documented from
  the code; group C is marked internal and kept out of the published description.
- **Resources are stateless.** The credential carries the **principal** only; the
  organisation is a path segment resolved per request (ADR-0047, ADR-0048).
- **OIDC validation is issuer-first**: read the issuer, resolve and cache the
  provider's keys, validate the signature locally, and never forward a token to a
  provider (ADR-0055).
- **Authorisation outcomes may be cached**, positive and negative, for a
  time-to-live that never exceeds the session lifetime, with invalidation on a
  user-profile or organisation-provider change (ADR-0054).
- **The interface shape is a resource with at most one sub-resource level**
  (ADR-0060): `/api/{organisationId}/{component}/{id}/{sub-resource}`. A
  sub-resource that is a **value** is read with `GET` and overwritten with `PUT`
  (`state`, `data`); one that is an **operation** is invoked with `POST`
  (`assign`). Do not nest deeper — address a sub-object by its own term, not by
  another path level. A write is never a `GET` (ADR-0048).
- **Documents are references, not payloads** (ADR-0061): business data holds a
  document identifier, and the file itself is streamed from
  `/api/{organisationId}/documents`; it is never held in memory or carried inside
  a JSON body.

## 12. Concurrency and reactive programming

- Compose asynchronous work with the reactive types; never start a thread to work
  around a blocking call.
- Keep the hand-off between stages **immutable**.
- **Long-running process execution is resumable across restarts**; no
  in-memory-only state that loses work.

## 13. Auditing

- **All auditing uses the Hamoni audit framework** (ADR-0026). Never write a
  custom audit handler, and never record an auditable action with ad-hoc logging.
- Register a new auditable action with the framework; do not log a parallel
  record by hand.
- Audit writes are not optional and are not best-effort.

## 14. Logging and observability

- Log through the **`JBoss Logging` facade** (ADR-0036). No direct console output.
- **Structured, level-correct logs.** Informational for lifecycle events, debug
  for detail, warning and error for things a human must act on.
- **Never log secrets or personal data.** No tokens, no credentials, no raw state
  payloads at informational level.
- Carry a **correlation identifier** across a process run so a case can be traced
  end to end.
- Health, metrics and traces are exposed through the standard Quarkus
  extensions; do not invent a bespoke endpoint.

## 15. Configuration

- Use Quarkus configuration (`application.properties`, profile-based), typed with
  a configuration mapping where there is more than one related property.
- **No magic defaults for environment-specific values.** Fail fast on missing
  required configuration.
- Profiles are `dev`, `test` and `prod`. Test configuration is self-contained;
  development services supply local backing services.

## 16. Testing (backend)

- **JUnit is the test framework**, and every test runs under **`@QuarkusTest`**.
- **Mock collaborators with Mockk.** Mock only what is genuinely external; use
  real backing services through **Testcontainers or development services** — the
  database engine is never mocked.
- **Assert through the Quarkus `UniAsserter`.** Because the project is reactive, a
  subscription is asserted inside a `UniAsserter`; within it, use **AssertJ**
  fluent assertions for the actual checks.
- **Structure every test as Given-When-Then** — the setup, the action and the
  assertion, in that order.
- **Reactive tests run on the Vert.x event loop:** annotate with
  **`@RunOnVertxContext`**. **Secured paths are tested with `@TestSecurity`.**
- **ArchUnit** guards the boundaries (no dependency from one application area
  to the other; `common/` depends on neither).
- Name tests for behaviour: `` `a pended process suspends all parallel paths` ``.
- **Every domain state transition has a test.** The state models (ADR-0012,
  ADR-0013) and the pend and exception semantics (ADR-0014, ADR-0015) are the
  highest-risk areas and must be covered.
- Deterministic tests only: no reliance on the wall clock and no sleeps; inject a
  clock.

## 17. Anti-patterns — Backend

Do not:

- Call a blocking library or a blocking API on an input/output thread, or adopt a
  library that does not support reactive access (§8).
- Use blocking database access; use reactive Panache (§10).
- Auto-generate the production schema, or edit a migration that has been applied
  (§10).
- Put business logic in a resource (§11); and do not maintain a parallel copy of
  a type merely to serialise it — control exposure by annotation (ADR-0059).
- Return a stack trace, or an internal identifier, in an error response (§11).
- Write a custom audit handler, or log an auditable action by hand instead of
  using the Hamoni audit framework (§13).
- Log a secret, a token or personal data; or expose a bespoke health or metrics
  endpoint (§14).
- Rely on in-memory-only state for a running process, or mock the database engine
  in a test (§12, §16).
- Use a binary floating-point type for money or any decimal — use `BigDecimal`,
  precision 15 and scale 5 (§9).
- Use a Kotlin time type on a persisted entity — use `java.time.Instant` (§9, §10).
- Declare a JPA entity property as `val` — it must be `var` (§10).
- Build `equals`/`hashCode` on a name or other display field instead of the
  technical identifier (§9).
- Name the technical identifier or a foreign key outside the convention — `id`,
  `<table>_id` / `<table>Id` (§10).

---

# Part III — Frontend

The front end is an Angular single-page application, built and served by Quarkus
through Quinoa (ADR-0037), using PrimeNG and Tailwind (ADR-0038), unit-tested with
`vitest`, and made browsable by a real person through Mock Service Worker
(ADR-0039).

## 18. UI design and language

This section covers what a user sees: the words, the visual system and the
controls.

### 18.1 Tone of voice

- Hamoni is **people-powered and people-led**. Use those words.
- **Avoid** *staff*, *resources* and *replacement* in every user-facing string —
  they describe people as inventory.
- The tone is professional, outcomes-driven and enterprise-focused, balancing
  high-technology capability with human-centric collaboration: friendly,
  approachable, competent, empathetic and supportive.
- The product's one message to keep in view: *bring your people and AI together,
  as one workforce, built to scale*.
- **The tone is guiding.** Copy is informative of **the next action** — what to do
  to resolve something or return to normal business. It names the step, not just
  the state.
- **It is forward-looking.** Lead with **what is coming up or what is next**, not
  with what has already happened.
- **It is supportive and encouraging.** Most people using Hamoni do **repetitive
  work**, so earned appreciation — *well done*, *congratulations* — is welcome.
  **Never scold:** where a negative has to be conveyed, do not state it plainly
  ("you did not do well yesterday", "you have 30% more tasks today") — fall back
  to the guiding and forward-looking approaches, for example *"yesterday was
  tough, but the good news is that you're already ahead for today"*. Encouragement
  must be sincere and specific, never condescending.
- **It is precise** — in **reporting and analytics only**. Be specific about what
  is being displayed or shown, without wording so literal that it obscures the
  intent.

### 18.2 Plain language in the interface

- **Copy shown to a user is plain and simple.** Prefer short, ordinary words.
- **Do not use acronyms or abbreviations in copy shown to a user.** Each one is a
  hurdle the user pays for.
- Where an acronym is genuinely unavoidable in the interface, spell it out on
  first use, capitalising the first letter of each word in the expanded form —
  for example *Architecture Decision Record* — and do not use the short form in
  that copy.
- Acronyms specific to the project are defined once in the
  [README](./README.md#acronyms).
- Use the word **organisation**, never *tenant*, in copy shown to a user — *tenant*
  stays technical and internal (ADR-0022).

### 18.3 All copy is localisable

- **Every user-facing string must be localisable.** No string is written inline
  in a component; all copy is held in the resource bundle and looked up by a key.
- **Never build a sentence from fragments.** Use whole, translatable strings with
  placeholders, because word order differs between languages.
- Route dates, times, numbers, currency and lists through the shared formatting
  helpers, so one locale decision applies everywhere.
- Do not assume a language, a direction or a date format: the interface must
  render correctly for a right-to-left language and for a locale whose date and
  number formats differ from the development machine.
- **The locale comes from the user profile** (ADR-0062). The organisation sets a
  default, which the person may override; the choice is held on the profile as
  locale information. There is no second source of locale — not the browser, not
  the build, not the development machine.
- **Static copy lives in the front end.** Every label, message and state — and
  the assistant's replies — is held in the front-end resource bundle. The backend
  is never asked to supply interface prose it does not already own.
- **Problem messages from the interface are human text** (ADR-0062). An error the
  interface shows is a message written for a person, not a code; the **backend
  localises it into the profile's locale** before returning it, and the client
  renders it as given.

### 18.4 Design system and restraint

- Bind the **Design System for Hamoni** tokens. **Never invent colours, fonts or
  radii** — use the registered palette and the declared type and radius scales.
- **The registered palette is eleven colours:** the seven originals — canvas
  `--bg`, surface `--surface`, foreground `--fg`, muted `--muted`, border
  `--border`, tertiary `--dim`, accent `--accent` — the teal `--info` (the AI
  voice and the next action in *form* only), and three RAG status colours,
  `--success` / `--warn` / `--error`, for status only. Everything else is a
  `color-mix()` of them; no other colour literal appears in the stylesheets.
- **Pills are information, never actions.** A status pill carries **no hue** and
  reads through fill, border and label — *normal* (aware of, nothing to do),
  *increased* (aware of, acting on), *normal · alternative* (slightly off, done
  on purpose), *reduced* (history). A pill never navigates: the forward move is
  a control, not a pill. **RAG status** is the one coloured family, reserved for
  status against a target (on track / at risk / breached), and it always carries
  an icon, so it never relies on colour alone.
- **One accent, used sparingly.** In any view, at most one primary action and at
  most one forward accent; everything else earns its place by hierarchy, not by
  hue.
- **Advanced mode** gates complexity. A complex control is shown only when the
  single application-wide setting is on (ADR-0028); no screen carries its own
  advanced switch.
- Every interactive element has a clear focus indicator, and every state —
  hover, focus, active, selected, disabled — is designed, not left to the
  browser.

### 18.5 Accessibility

- Every control is reachable and operable by keyboard, with a visible focus ring.
- Maintain the contrast ratios the design system declares; never reduce text
  contrast on hover or focus.
- Label every input, give meaningful images alternative text, and give decorative
  images empty alternative text.

### 18.6 Fonts and third-party assets

- **Self-host every font; never hotlink or `@import` a font CDN.** The faces
  (Inter, JetBrains Mono and the monochrome Noto Emoji fallback) are pinned
  woff2 files under the app's `public/fonts/` (served at `/fonts/…`), served with the one
  deployment. A font CDN would send a visitor's request to a third party, can
  change a family's metrics without notice, breaks under a strict `font-src`
  content-security policy and in an air-gapped deployment, and — because
  browsers now partition font caches per site — no longer shares a cache
  benefit (ADR-0037).
- **Carry the licence with the asset, in the `NOTICE` file's fixed structure.**
  Every redistributed font or icon set keeps its notice in the repository, in the
  project-root `NOTICE` file. That file has a **legal structure that must not be
  reformatted**: a `Licenses` introduction, then one
  `Components used by <component>` section per area, each component listed with
  its `Path`, `Licence` and `Copyright`. A new third-party asset is not added
  until its entry is recorded in that structure. Inter, JetBrains Mono and Noto
  Emoji are SIL Open Font License 1.1; PrimeIcons is MIT.
- **Do not recolour a third-party mark.** Fonts and icons are used as shipped;
  the design system's colours are applied through the preset and the theme, not
  by re-processing an asset.

## 19. Framework and project structure

- The front end is **Angular**, built with **Quinoa** into the Quarkus
  application (ADR-0037). There is one runtime and one deployment.
- Structure the application by **feature**, mirroring the backend: a feature owns
  its components, its state and its interface client together.
- Keep presentation logic out of components where it can be a **pure function**;
  keep data access in a service, not in a component.
- Use **typed** interface clients generated from, or kept in step with, the
  backend's OpenAPI description. Do not hand-write a client type that duplicates
  a contract. Types are generated for all three API groups; only the publication
  of group C is suppressed (ADR-0049).

## 20. Angular services

A **service** is the only place that talks to the server; a component or a store
calls a service and never performs a request itself. A feature owns its service
beside its components and its state (§19).

### 20.1 Every method returns an Observable

- **Every public method on a service returns an `Observable`.** A method that
  writes returns `Observable<void>`; a method that reads returns the value it
  reads. Nothing on a service returns a bare value or a promise.

### 20.2 The standard method names

**A service method is named as a verb, optionally followed by the noun it acts
on — `<verb>(<noun>)?`.** The noun is **omitted when the method operates on the
service's own entity**, and included when the method acts on something else, or
when a noun is needed to disambiguate.

- `approve(id)` — the noun is left out: the service's entity is a process, and
  the verb alone reads correctly.
- `assignOwner(id, ownerId)` — the noun is kept: the method acts on the owner,
  not on the process itself.

For the **ordinary read and write operations**, use these five standard names,
and no others; the name states the contract.

- **`list()`** returns a list of objects with **no server-side filtering**.
- **`search()`** returns a list of objects and **expects server-side
  filtering**; it **requires the search criteria as an argument**.
- **`get()`** returns a single object; it **requires an identifier as an
  argument**.
- **`update()`** performs the update and returns `Observable<void>`; it
  **requires the identifier and the object to update as arguments**.
- **`create()`** performs the create and returns the **hydrated object**; it
  **requires the partial object as an argument**. Do **not** put an identifier
  in the request URL or in the embedded object — it is ignored.

### 20.3 A list is the whole list, delivered once

- A method that returns a list returns **`Observable<T[]>`** — the whole list as
  one value — **not** the streaming form `Observable<T>` that emits one item at a
  time.
- Reason: most lists are small, and one array maps directly to a table in
  Angular. Streaming would force the table to assemble and reconcile the
  collection itself for no benefit at this size.
- A **list** or a **search** may return a **lighter representation** of the type —
  `Observable<Partial<T>[]>` — than `get(id)`, which returns the full `T`
  (ADR-0059).

### 20.4 The identifier comes from the URL, never the body

- For an update or a create, the object identifier is taken from a **URL
  parameter**, not from the object being sent.
- Leave the identifier field on the embedded object **blank**; the server reads
  the identifier from the URL and ignores the embedded one. The URL is the single
  source of truth for which object is addressed.

### 20.5 State: a private writable signal, a public read-only signal

- A service may also expose an **HTTP resource** and hold state as an **Angular
  signal**.
- A **writable signal is private**; the service exposes a **public, read-only
  view** of it:

```ts
readonly #jobs = signal<Job[]>([]);
readonly jobs = this.#jobs.asReadonly();
```

- Expose the public signal as **writable only when a caller genuinely must write
  to it**; otherwise update the private signal inside a method on the caller's
  behalf.

### 20.6 Map transport errors to the application error type

- Where an HTTP call is made, map the `HttpErrorResponse` to the application's
  error type, **`AppError`**, using **`getProblemDetailsFromError`**. Do not let a
  raw `HttpErrorResponse` leave the service.
- Reason: the interface reports failures as problem details (§11), and
  `getProblemDetailsFromError` reads them into the one error shape the rest of
  the application handles. A raw transport error is not a domain type.

### 20.7 Log at the point of the call

- Where an HTTP call errors, log it **at the point of the call** — inside the
  service, where the operation, the method and the identifier are known.
- Do not rely on a global interceptor or on a component to log it, and do not
  log the same failure a second time further up.
- Reason: the call site has the context that makes the log useful, and one record
  per failure keeps the log trustworthy.

### 20.8 Never change server state through GET

- An operation that **changes server state must never be reachable through an
  HTTP `GET`**. A `GET` retrieves; it never changes anything.
- Example: **selecting an organisation is a frontend concern** — it changes the
  organisation segment of the URL and redirects the application's calls, so it is
  **not** a server request at all (ADR-0048). An operation that *does* change
  server state — **completing a task**, for instance — is a **`POST`**, never a
  `GET`.

### 20.9 Use HTTP method semantics

- **`PUT`** updates the **whole** existing object — an overwrite.
- **`PATCH`** **partially** updates an object.
- **`POST`** **creates** a new object.
- **`POST`** is the **fallback** for any request that updates state or an object
  and does not fit `PUT` or `PATCH` — for example, an action that changes a
  process instance's state.
- **`GET`** only **retrieves** data.
- Treat retrieved data as valid **only at the moment of retrieval**: as time
  passes it is increasingly likely to be stale, so a view that must be current
  re-reads rather than trusting a cached copy.

## 21. Components and the design system

- **PrimeNG in styled mode**, driven by the generated Hamoni preset
  (ADR-0038). Re-colour the library from the preset; never override a component's
  colour locally.
- **Tailwind is the layout layer**, from the same tokens. Do not mix a second
  layout system in.
- **Unstyled (pass-through) mode is per component only**, for a control the
  preset cannot express — and only when there is no preset-based answer.
- Use a single icon set on the declared size scale.
- **Provider marks are the registered vendor glyphs**, never drawn look-alikes
  (ADR-0045).
- On the sign-in surface, method buttons carry the application's control fill
  (`--bg` over the surface), and hover is uniform across the method and accent
  buttons (ADR-0045).
- **Compose, do not fork.** A variant of an existing component is a prop or a
  slot on that component, not a copy of it.

## 22. Frontend quality: linting, formatting and types

- Enforce linting and formatting with the shared front-end configuration
  (ESLint and Prettier), consistent with the backend's quality gates. A change
  that fails lint or format fails the build.
- **Strict TypeScript.** No implicit `any`; no non-null assertion to silence the
  compiler; model absence explicitly.
- No dead code, no commented-out blocks, no console output left in committed
  code.
- Keep components small and single-purpose; extract a shared component the second
  time it is needed.

## 23. Testing (frontend)

Two tools, two jobs. They are complementary and neither replaces the other.

- **`vitest` is the default test runner**, and the default for testing **case
  handling** — the different branches, error paths and edge cases of a unit of
  code.
- **Mock Service Worker (MSW) is a mock backend** (ADR-0039): its purpose is to
  let a real person **log in and browse the application** without the real
  backend. **Every new service ships happy-case handlers** for it, so the
  application is usable end to end the moment the service exists.
- **MSW does not replace `vitest`**, and `vitest` does not replace the browsing
  handlers — a unit's edge cases are proven by a `vitest` test, not by a
  happy-case handler.
- Test components and services for **behaviour the user can observe**, not
  implementation detail.
- Cover the states each screen can be in: loading, empty, error and populated.
- Do not test against a hand-rolled fake client; go through MSW so the component
  sees a real request.

## 24. Anti-patterns — Frontend

Do not:

- Hard-code a colour, font or radius outside the design system tokens (§18.4).
- Override a PrimeNG component's colour locally, or introduce a second layout
  system beside Tailwind (§21).
- Write user-facing copy inline in a component, concatenate a sentence from
  fragments, or hard-format a date, number or currency by hand (§18.3).
- Use an acronym in copy shown to a user (§18.2).
- Add a per-screen or per-panel advanced-mode switch (§18.4).
- Use implicit `any`, a non-null assertion to silence the compiler, or a
  hand-rolled fake client instead of MSW (§22, §23).
- Treat the Mock Service Worker handlers as a substitute for `vitest`, or ship a
  new service without its happy-case handlers (§23).
- Leave console output, dead code or commented-out blocks in committed code
  (§22).
- Fork an existing component instead of composing a variant of it (§21).
- Return a bare value or a promise from a service method; name a method without
  a leading verb; or use a non-standard name for an ordinary operation instead of
  `list`, `search`, `get`, `update`, `create` (§20.1, §20.2).
- Stream a list as `Observable<T>` instead of delivering it once as
  `Observable<T[]>` (§20.3).
- Send the object identifier in the body of an update or a create instead of in
  the URL (§20.4).
- Expose a service's writable signal publicly, or read or write a private signal
  from outside (§20.5).
- Let a raw `HttpErrorResponse` leave a service, or handle an HTTP error without
  mapping it to `AppError` with `getProblemDetailsFromError` (§20.6).
- Log an HTTP failure anywhere but the call site, or log it more than once
  (§20.7).
- Change server state through an `HTTP GET` request (§20.8).
- Use a method that does not match the semantic — a `GET` that changes state, a
  `PUT` for a partial update, a `PATCH` where a full `PUT` is meant, or a `POST`
  where `PUT` or `PATCH` fits (§20.8, §20.9).

---

*This document is expected to change. When a rule is wrong, fix the rule — and
record the reasoning in an ADR if it is architectural.*
