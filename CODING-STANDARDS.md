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
  classes for value objects. Mutability is a deliberate, local decision.
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
  `ai.hamoni.controlroom`, `ai.hamoni.processrunner`, `ai.hamoni.common`).
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
  password and derives identity from the provider's subject claim (ADR-0022).
- **Tenant isolation is enforced at the data layer** — every tenant-scoped query
  carries the tenant context (ADR-0025). A missing tenant filter is a defect.
- **Authorisation is explicit** at the service boundary: view · edit · deploy ·
  start, resolved from the security model (ADR-0024). Do not invent per-screen
  checks.
- **Least privilege** for service accounts and database roles.
- **Secrets** come from configuration or a secret store, by environment
  variable — never in the repository, never in logs, never in a test fixture with
  a real value.

## 5. Documentation and decisions

- **Comment** where the code cannot say it itself; put explanatory prose on
  public declarations in a shared project and on domain types.
- **Every architectural decision is recorded** as an ADR in
  [`ARCHITECTURE-DECISIONS.md`](./ARCHITECTURE-DECISIONS.md). A code change that
  makes a new decision writes the record in the same change.
- **Long-form documentation** is AsciiDoc under `docs/`, an Antora component
  (ADR-0040). Anything here that stabilises graduates into Antora.

## 6. Commits and reviews

- One concern per commit; a clear message stating what and why.
- Changes are small and reviewable; the build, lint and tests pass before review.
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
- **Identifiers are typed** (a `ProcessId`, not a raw string) and are generated as
  universally unique identifiers at the boundary.
- **Money is a decimal value with an explicit currency** — never a binary
  floating-point type. **Time** is `java.time` in Coordinated Universal Time
  internally; convert only at the edge. Do not do raw epoch arithmetic.
- **Business rules are data, not code branches**, where the product models them
  as data (ADR-0016). A business rule is a project-scoped artefact, not a
  conditional in a service.
- **Do not leak persistence types** into the domain. Map entities to and from
  domain types at the repository boundary.

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

## 11. Interface

- The HTTP interface uses **Quarkus REST (RESTEasy Reactive)** (ADR-0034). It is
  non-blocking end to end.
- Resources are **thin**: validate, delegate to a service, map to a response. No
  business logic in a resource.
- The public entry point for starting a process is
  `POST /processes/{processId}/instances`, with the body validated against the
  process's state schema (ADR-0029).
- **Errors use the problem-details format (`application/problem+json`, RFC
  7807).** No stack traces on the wire, and no internal identifiers in error
  messages.
- **Validation at the boundary.** A payload is validated against the process's
  declared state data (ADR-0017); a mismatch raises a mapped exception.
- **Transport types are separate from domain types.** Never serialise an entity
  directly.
- **Versioning** appears in the path only when a breaking change requires it;
  additive change is preferred. Record a breaking change in an ADR.

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

- **JUnit 5** for all tests, **Quarkus Test** for anything that touches
  dependency injection, configuration or persistence, and a REST client test
  library for HTTP resources.
- **Testcontainers or development services** for real backing services; do not
  mock the database engine.
- **ArchUnit** guards the boundaries (no dependency from an application area to
  the other; `common/` depends on neither).
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
- Put business logic in a resource, or serialise an entity across the interface
  (§11).
- Return a stack trace, or an internal identifier, in an error response (§11).
- Write a custom audit handler, or log an auditable action by hand instead of
  using the Hamoni audit framework (§13).
- Log a secret, a token or personal data; or expose a bespoke health or metrics
  endpoint (§14).
- Rely on in-memory-only state for a running process, or mock the database engine
  in a test (§12, §16).

---

# Part III — Frontend

The front end is an Angular single-page application, built and served by Quarkus
through Quinoa (ADR-0037), using PrimeNG and Tailwind (ADR-0038) and tested with
Mock Service Worker (ADR-0039).

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

### 18.4 Design system and restraint

- Bind the **Design System for Hamoni** tokens. **Never invent colours, fonts or
  radii** — use the registered palette and the declared type and radius scales.
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

## 19. Framework and project structure

- The front end is **Angular**, built with **Quinoa** into the Quarkus
  application (ADR-0037). There is one runtime and one deployment.
- Structure the application by **feature**, mirroring the backend: a feature owns
  its components, its state and its interface client together.
- Keep presentation logic out of components where it can be a **pure function**;
  keep data access in a service, not in a component.
- Use **typed** interface clients generated from, or kept in step with, the
  backend's OpenAPI description. Do not hand-write a client type that duplicates
  a contract.

## 20. Components and the design system

- **PrimeNG in styled mode**, driven by the generated Hamoni preset
  (ADR-0038). Re-colour the library from the preset; never override a component's
  colour locally.
- **Tailwind is the layout layer**, from the same tokens. Do not mix a second
  layout system in.
- **Unstyled (pass-through) mode is per component only**, for a control the
  preset cannot express — and only when there is no preset-based answer.
- Use a single icon set on the declared size scale.
- **Compose, do not fork.** A variant of an existing component is a prop or a
  slot on that component, not a copy of it.

## 21. Frontend quality: linting, formatting and types

- Enforce linting and formatting with the shared front-end configuration
  (ESLint and Prettier), consistent with the backend's quality gates. A change
  that fails lint or format fails the build.
- **Strict TypeScript.** No implicit `any`; no non-null assertion to silence the
  compiler; model absence explicitly.
- No dead code, no commented-out blocks, no console output left in committed
  code.
- Keep components small and single-purpose; extract a shared component the second
  time it is needed.

## 22. Testing (frontend)

- **Mock Service Worker (MSW)** provides the interface during tests and local
  development (ADR-0039). Share the handlers between the two.
- Test components for **behaviour the user can observe**, not implementation
  detail.
- Cover the states each screen can be in: loading, empty, error and populated.
- Do not test against hand-rolled fake clients; go through MSW so the component
  sees a real request.

## 23. Anti-patterns — Frontend

Do not:

- Hard-code a colour, font or radius outside the design system tokens (§18.4).
- Override a PrimeNG component's colour locally, or introduce a second layout
  system beside Tailwind (§20).
- Write user-facing copy inline in a component, concatenate a sentence from
  fragments, or hard-format a date, number or currency by hand (§18.3).
- Use an acronym in copy shown to a user (§18.2).
- Add a per-screen or per-panel advanced-mode switch (§18.4).
- Use implicit `any`, a non-null assertion to silence the compiler, or a
  hand-rolled fake client instead of MSW (§21, §22).
- Leave console output, dead code or commented-out blocks in committed code
  (§21).
- Fork an existing component instead of composing a variant of it (§20).

---

*This document is expected to change. When a rule is wrong, fix the rule — and
record the reasoning in an ADR if it is architectural.*
