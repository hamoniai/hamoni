# Hamoni — API and database contract

This document details the **requirements and the architecture of the Control
Room's interface** — the contract between the frontend and the **Control Room**
application — and the **rules that govern the database** beneath it. It sits
behind the UX: it defines the contract the Angular application is written
against, not the screens themselves. It will graduate into the Antora
architecture section (ADR-0040).

**Scope — the Control Room's interface only.** This document covers the Control
Room's interface and its database. The Control Room's communication with the
**Process Runner** — its queues, message types and the events exchanged over
Quarkus Reactive Messaging — is **not defined here** and is **not yet designed**;
the Process Runner exposes **no REST interface** of its own. That contract is a
separate piece of work.

It is written to be obeyed. Where it conflicts with
[`CODING-STANDARDS.md`](./CODING-STANDARDS.md) or
[`ARCHITECTURE-DECISIONS.md`](./ARCHITECTURE-DECISIONS.md), the decision record
wins for *what* the system does and the coding standards win for *how* it is
written. Every rule here traces to a decision record.

---

## 1. Invariants

Every rule below serves four invariants. A change that breaks one of them is a
defect, not a style preference.

1. **Reactive, end to end.** Nothing blocks an input/output thread (ADR-0032,
   ADR-0033, ADR-0034).
2. **Stateless backend.** The server holds no session or request state; the
   credential identifies the caller only (ADR-0047).
3. **Organisation-isolated at the data layer.** Isolation is by schema, and a
   query cannot cross organisations (ADR-0025, ADR-0052).
4. **Audited.** Every auditable action goes through the Hamoni audit framework;
   there is no ad-hoc audit path (ADR-0026).

And two standing constraints on secrets and language: no secret in the tree,
the build or a log (`CODING-STANDARDS.md` §4); the user-facing word is
**organisation**, never *tenant*.

---

## 2. Terminology

These terms are precise and are not interchangeable. They are technical and
internal; the only user-facing word is *organisation*.

| Term | Meaning |
| --- | --- |
| **Inbound OIDC context** | The provider-and-subject pair an identity provider asserts. This is the only thing an inbound credential carries. |
| **Principal** | Hamoni's reflection of an inbound context. A principal identifies the **authentication** context only — never the organisation or the authorisation context (ADR-0051). |
| **Authentication profile** | A **global**-schema record linking an inbound OIDC context to a Hamoni identity UUID and the organisation pointers that identity may enter. Application-managed; no user transacts in it (ADR-0052). |
| **User profile** | The **organisation-schema** configuration for an identity: organisation-level permissions, calendars and similar. Keyed by the identity UUID, one per organisation (ADR-0052). |
| **OIDC claims** | The attributes the provider owns (name, email). Hamoni reads them; it never edits them (ADR-0056). |
| **Session credential** | The server-issued, opaque, encrypted token that carries the principal. A cookie for the browser, a bearer token for a non-browser caller (ADR-0047). |

---

## 3. The interface

This is the **Control Room's** interface — the REST contract the frontend and
other API clients use. It is **not** the Process Runner's: the runner exposes no
REST interface and is reached over Quarkus Reactive Messaging, whose queues and
message types are not yet defined (see the scope note at the top).

### 3.1 Three groups of APIs

The interface is divided into three groups, marked in the codebase by an
annotation that also drives documentation generation (ADR-0049).

| Group | Scope | Documented | Purpose |
| --- | --- | --- | --- |
| **A — Authentication and account** | `/api/auth/*` | Public | Sign-in, the session credential, the caller's own account, organisation discovery. |
| **B — Business** | `/api/{organisationId}/*` | Public | The domain: teams, projects, processes, instances, tasks, deployments. |
| **C — Screen flow** | `/api/{organisationId}/*` | Internal | Reads the screens need to assemble a view; not in published documentation. |

A quality gate: every group A and group B resource carries the code annotation
that generates its OpenAPI description. Group C carries the internal marker that
keeps it out of the published document.

### 3.2 Paths

- Group A: `/api/auth/…`, for example `/api/auth/me`.
- Groups B and C: `/api/{organisationId}/…`, for example
  `/api/{organisationId}/tasks/{taskId}/complete`.

The organisation is a **path segment on every business call** (ADR-0048). A
request with no valid organisation segment is not a business call.

### 3.3 The backend is stateless

The backend holds **no session state and no current-organisation state**
(ADR-0047). The server issues one credential — a session cookie for the browser,
a bearer token for a machine — that identifies the authenticated principal and
nothing else. In Quarkus the principal is serialised and encrypted with the
**platform key** into the token; the organisation is never placed in it.

Consequences that are easy to forget:

- The **platform key** must be identical and stable across every application
  instance, or a token issued by one instance cannot be read by another. Key
  rotation invalidates every live session.
- Machine access and browser access are **symmetric**: both present the same
  principal, and both carry the organisation in the path.

The Angular application stores only **local preferences** — the default
organisation, the display theme — in local state. It holds **no business state**.

### 3.4 The principal model

An inbound OIDC context is never trusted as a person directly. It is resolved to
a **principal**, and the principal has a **type** (ADR-0051):

- **Normal** — a `person`, a `system` account, or an `agent`.
- **On behalf of** — a principal that records both the acting identity and the
  identity it acts for (a person on behalf of a person, or an agent on behalf of
  a person).

Principals are **granted by the system**. When a person starts an AI-agent chat,
the platform grants the agent a principal that identifies the agent *and* the
person it works for. A principal identifies only *who is authenticated*; it
carries no organisation and no authorisation.

The organisation and the grant are resolved **per request** from the path
segment and the cache (§3.7). This keeps browser and machine callers on one
model and keeps the backend scalable (ADR-0047).

### 3.5 No API keys

There are **no API keys** (ADR-0050). An API key lets a system act inside a
person's context without being that person, which breaks the rule that work is
always identifiable to an actor who participated. Instead, a system or an agent
has **its own account**, authenticates through **its organisation's provider**
(certificate authentication, domain trust or whatever the provider requires), and
receives a principal of its own through the same OIDC flow a person uses. From
Hamoni's point of view a system caller and a person caller are the same kind of
thing: a principal.

Where a system's grants live, and whether they exist as a separate grant axis,
is an open question (§6).

### 3.6 Multi-provider OIDC validation

An organisation may configure **more than one** identity provider — for example
to admit partner or third-party identities into a business process (ADR-0043,
ADR-0055). Validating a token across providers must not turn Hamoni into a
proxy for an attack on a provider, so validation is **issuer-first** (ADR-0055):

1. Read the **issuer** (`iss`) claim, without trusting the token.
2. Fetch the matching provider's **JSON Web Key Set** from its discovery
   endpoint, and **cache the keys** (they change rarely, and a short cache window
   bounds the risk while keeping the round-trip off the hot path).
3. Validate the **signature** locally against the resolved keys.
4. Only a token that validates against exactly one configured provider is
   accepted; it is never forwarded on to a provider.

Careful configuration is required so a token issued for one provider is never
validated against another and never cross-pollinated.

### 3.7 The authorisation cache

Determining whether a person may enter an organisation is **heavy**: it must
check that the provider is still enabled for the organisation **and** that the
org-scoped **user profile** is still enabled (ADR-0043, ADR-0054). The result is
cached, both positive and negative, for a bounded time (ADR-0054):

- The **time-to-live is never greater than the session lifetime**; a fixed short
  window (an hour is the working figure) is the starting point.
- A change that alters the **user profile** invalidates that user's entry —
  disabling a user in the organisation, accepting an invitation, creating an
  invitation.
- A change to the organisation's **OIDC configuration** invalidates the whole
  organisation's entries.

The cache is a **performance structure, not authoritative state**: it may be
dropped at any time and rebuilt from the database, which is what keeps the
backend stateless (ADR-0047).

Listing an identity's organisations is likewise heavy — each pointer in the
authentication profile must be validated by the same rule before it is returned —
and uses the same cache.

### 3.8 The Angular service shapes

The interface exists to satisfy `CODING-STANDARDS.md` §20. Every endpoint reads
naturally as a service method:

- `list()` — a collection, no server-side filtering.
- `search(criteria)` — a collection, server-side filtering, criteria required.
- `get(id)` — one object, identifier required.
- `update(id, object)` — a write; the identifier comes from the URL (§20.4).
- `create(partial)` — a create; returns the hydrated object; no identifier in the
  body.
- **verb-plus-noun** methods for everything else — `.start(processId)`,
  `.updateState(taskId, state)`, `.assignOwner(taskId, ownerId)`,
  `.getHistory(taskId)`.

Every method returns an `Observable`; a list is the whole list, delivered once,
as `Observable<T[]>` (§20.1, §20.3). No operation that changes state is
reachable through `GET` (§20.8). The concrete endpoint shape is settled in §3.11:
a resource with at most one sub-resource level.

### 3.9 Documentation and the typed client

- Group A and B endpoints are annotated so their **OpenAPI description is
  generated from the code** — never hand-written alongside it (ADR-0049).
- The Angular client is **typed from that description** and kept in step with it
  (`CODING-STANDARDS.md` §19). Types for group C are still generated; only the
  *publication* of group C is suppressed (ADR-0049).

### 3.10 Versioning

The standing rule holds: additive change is preferred, and a version appears in
the path only when a breaking change requires it, recorded in a decision
(ADR-0050 is the nearest precedent). Whether versioning is **per group** or
applies to the whole interface is not yet settled (§6).

### 3.11 The resource and sub-resource shape

Every endpoint is `/api/{organisationId}/{component}/{id}/{sub-resource}`, at most
**one** sub-resource level deep (ADR-0060). A sub-resource that is a **value** is
read with `GET` and overwritten with `PUT`; one that is an **operation** is
invoked with `POST`. The **task component** is the reference shape, and the same
shape extends to processes and the other components:

| Endpoint | Method | Service method (§20) | Returns |
| --- | --- | --- | --- |
| `/tasks` | `GET` (no filter) | `list()` | the light task view |
| `/tasks` | `GET` (with criteria) | `search(criteria)` | the light task view |
| `/tasks/{taskId}` | `GET` | `get(id)` | task detail; query parameters set depth |
| `/tasks/{taskId}/state` | `GET` | `getState(id)` | the current state |
| `/tasks/{taskId}/state` | `PUT` | `updateState(id, state)` | the validated transition |
| `/tasks/{taskId}/data` | `GET` | `getData(id)` | the task's data |
| `/tasks/{taskId}/data` | `PUT` | `updateData(id, data)` | overwrite (or `PATCH`: open) |
| `/tasks/{taskId}/history` | `GET` | `getHistory(id)` | the audit-style history |
| `/tasks/{taskId}/assign` | `POST` | `assign(id, request)` | assignment · take · refer |

Two consequences worth stating plainly:

- A **human** task only; an **automated** task does not appear in the tasks
  component (ADR-0060).
- There is **no `create` and no `update` on the task itself**: a task is created
  by execution, and its mutable parts are its **state** and its **data**.

### 3.12 Documents

A document is a **reference to a file blob** (ADR-0061). Business data carries the
document **identifier**; the task's representation shows which documents it
references, never their content. Document access is its own component,
`/api/{organisationId}/documents`, and upload and download are **multipart or
streamed** — the one part of the interface that is not JSON. The concrete
storage, streaming and permission rules are open (§6).

---

## 4. The database

### 4.1 Two tiers of schema

There are two kinds of schema (ADR-0052):

- **The global schema.** Application-managed and self-cleaning. It holds the
  **authentication profiles** — the link from an inbound OIDC context to an
  identity and its organisation pointers. **No user, admin or otherwise,
  transacts in the global schema**; only the application touches it.
- **The organisation schema.** One per organisation. It holds everything
  organisation-scoped: **user profiles**, team and project configuration,
  process and deployment state, and the organisation's audit trail.

### 4.2 Schema-per-organisation isolation

Isolation is by **schema, not by a discriminator column** (ADR-0052). Global
tables are addressed by their **explicitly qualified** schema; every other table
belongs to an organisation schema whose name is the organisation identifier.
There is no way for a query against one organisation to reach another, because
the schema is part of the query's identity.

- The organisation is **resolved from the path segment** (§3.2) and set as the
  **current tenant** for the request.
- Hibernate's tenant resolution — `MultiTenantConnectionProvider` and
  `CurrentTenantIdentifierResolver` — is the intended mechanism; its exact
  behaviour under **reactive Panache** is to be confirmed before the first
  migration is written.
- A missing or wrong tenant selection is a **defect**, exactly as a missing
  filter was under the earlier rule (ADR-0025).

This is the enforcement mechanism that makes organisation safety structural
rather than a discipline a developer must remember.

### 4.3 The single-organisation flag

A **deployment-level flag** selects multi-organisation mode or single-
organisation mode. The **default is single-organisation** (ADR-0053). In
single-organisation mode the deployment supports exactly one organisation and
**creating new organisations is disabled** — this is the shape a dedicated or
enterprise installation runs in.

### 4.4 Migrations and rolling upgrades

- Schema is migrated **forward-only with Flyway**; an applied migration is never
  edited (ADR-0035).
- Because organisations are separate schemas, Flyway work is **split**:
  migrations for the global schema, and migrations applied **to every
  organisation schema**.
- Creating an organisation **invokes Flyway to create and bring up that schema**.
- Upgrading every organisation schema must be a **safe, repeatable** operation.

A rolling deployment (Kubernetes replacing pods) means an old and a new version
of the application may run **at the same time**, so **two schema versions must
coexist** while the rollout completes. The working direction is that Flyway may
only make **safe, additive changes** in an upgrade — no breaking change — so that
either application version can run against the current schema. This is recorded
as a **proposed** decision (ADR-0058) and needs investigation before it is
relied upon.

### 4.5 Identity attributes and the claims

Name and email are **owned by the OIDC claims** and are **never editable in
Hamoni** (ADR-0056). When a person changes their name or email, the change is
made at the provider; Hamoni picks it up on the next sign-in or token
validation. Hamoni stores them as a cached projection, refreshed from the claims,
not as a source of truth.

### 4.6 Global-schema hygiene

Authentication profiles accumulate. The working intent is a **periodic job** that
removes an authentication profile once it has gone unused for a set time **and**
no longer has any active user profile. Because the record is small and holds
little personal information (a name and an email, both refreshed from the
claims), it is also acceptable to **leave it in perpetuity** — a person who
returns in a new organisation simply re-links. The policy is not yet fixed (§6);
either way it is a job, not a request path.

### 4.7 Conventions

These apply unless a decision says otherwise, and refine `CODING-STANDARDS.md`
§9 and §10:

- **Identifiers are `java.util.UUID`**, generated at the boundary; the database
  stores the native UUID type and the interface presents a string (OpenAPI and
  TypeScript have no UUID type). A **business key** is rare and is never the
  system identifier; object equality uses the technical identifier.
- **Foreign keys** express structural relationships within a schema; there is no
  cross-schema foreign key.
- **Naming** is `snake_case` for tables and columns; the Kotlin and TypeScript
  identifiers are the corresponding `camelCase` (a `given_name` column is
  `givenName`). The technical identifier is the column `id`, property `id`; a
  foreign key is `<table>_id` / `<table>Id`.
- **Timestamps** are `java.time.Instant` in **Coordinated Universal Time**.
- **Money and other decimals are `BigDecimal`** (precision 15, scale 5), never a
  binary floating-point type; money carries an explicit currency.
- **Booleans** are booleans. **Enumerated states** are stored as a constrained
  string; the holding entity's serialisation rule maps the value to the sealed
  domain type, and the interface lists the valid values through a Swagger
  annotation.
- **Optimistic locking** guards aggregates that can be edited concurrently; a
  conflict surfaces as a **domain outcome** (a `409`), never a server error.
- **Transactions** are explicit and short; **no remote call sits inside one**
  (§10).
- **Audit** writes go through the Hamoni audit framework into the organisation's
  audit store; they are not best-effort (ADR-0026).

---

## 5. The frontend link

The Angular application is a client: it renders, it calls the interface, and it
holds no business rules (`control-room-ui` AGENTS.md). It follows
`CODING-STANDARDS.md` §20 exactly:

- A **service per feature**, one service talking to the server, the components
  never making a request themselves.
- The **five standard names** — `list`, `search`, `get`, `update`, `create` — for
  the ordinary operations, and **verb-plus-noun** for everything else.
- Identifiers **from the URL**, never the body.
- Every failure mapped to **`AppError`** through `getProblemDetailsFromError`;
  the error body is part of this contract.

Three points that are specific to this platform:

- **Switching organisation is a frontend concern.** It changes the organisation
  segment of the URL and redirects the application's calls; it is not a server
  request, and the credential does not change (ADR-0048).
- **A stored default organisation never auto-enters.** If the person cannot
  enter it, they land on the **organisation selector** for their next action —
  select another, create one, or, in single-organisation mode, stop. The same
  surface is shown for an organisation that does not exist, so the existence of
  organisations cannot be enumerated (ADR-0045, ADR-0057). An optional **request
  access** action may raise a task for the organisation's managers; where no such
  organisation exists, the action leads nowhere.

- **A list or a search returns a lighter representation than a detail call**
  (ADR-0059): `list()` and `search()` yield the summary a list shows; `get(id)`
  yields the full object. On the client the full type is `T`, and a list is
  `Partial<T>`.

**Authorisation presented in the interface is never the gate.** The client uses
the credential to decide which areas to *show*; the server enforces the boundary
on every request (ADR-0041). A client-side check is a courtesy to the person, not
a security control.

---

## 6. Open questions

These are deliberately **not** decided. They are recorded here and in
`ARCHITECTURE-DECISIONS.md` so that no one invents an answer.

1. **The state sub-resource's payload and legal transitions** (`PUT …/state`),
   and whether the assignment and referral operations (`POST …/assign`) overlap
   with it — an assignment changes the state map.
2. **The list contract** — server-side filtering, sorting, the pagination model,
   and how the Angular "whole list, once" convention maps onto it. Revisit once
   the technical and UX constraints are on the table.
3. **The `control-room` ↔ `process-runner` boundary.** The working idea is that
   the runner is essentially **stateless**, triggered by a **Quarkus Reactive
   Messaging** message from the control room, running until it blocks (a manual
   task, a pend, an exception, a completion) and reporting back. Open within
   that: **where the per-step
   execution history** — assigned, started, completed — lives, whether in a
   separate operational database the runner owns, in the organisation schema, or
   read directly by the control room, and how database contention and scale are
   managed.
4. **How much of the JSON Process is normalised** into tables versus stored as a
   document. The definition versions whole (ADR-0021); what the engine
   normalises to run and resume is undecided.
5. **The session-credential lifetime** and the corresponding **authorisation-cache
   time-to-live**.
6. **The system and agent grant model.** Whether a system or agent may hold
   **global, org-scoped grants** — "start any process", "complete any task" — as a
   separate grant axis, and whether those grants sit on the **user profile** (the
   leaning) rather than a team. This complicates the authorisation model and needs
   more thought.
7. **The schema-change compatibility window** for rolling upgrades (ADR-0058,
   Proposed).
8. **The versioning scheme** — per API group, or across the whole interface.
9. **Global-schema retention** — remove an unused authentication profile after a
   window, or keep it in perpetuity (the leaning).
10. **Dynamic (process-defined) parameters.** Some payloads — the arguments to
    start a process, for instance — vary by process definition. How they are
    described in the OpenAPI document and validated at the boundary is deferred.
11. **Task-detail depth** — the query parameters that control how much the detail
    call returns (history, data schema, current data, summary, process model).
12. **`data` update semantics** — overwrite with `PUT`, partial with `PATCH`, or
    finer-grained assignments.
13. **Summary templating** — server or browser, and how the template (process
    design) is kept apart from the data (task view).
14. **Document storage, streaming and permissions** — the blob store and the
    multipart/streaming rules (ADR-0061).
15. **The Control Room ↔ Process Runner contract** — the queues, message types
    and events exchanged over Quarkus Reactive Messaging. Out of scope here, and not yet designed.
