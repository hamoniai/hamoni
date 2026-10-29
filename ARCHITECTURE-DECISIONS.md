# Hamoni — architecture decisions

The **Architecture Decision Records (ADRs)** that define how the Hamoni platform
is built. This is a **working document**: it grows as decisions are made, and it
will become the Architecture section of the Antora documentation.
[`README.md`](./README.md) and [`AGENTS.md`](./AGENTS.md) both point here.

## How to read and use this

- **`Status`** is one of **Accepted**, **Proposed**, **Superseded** or **Open**.
  Only *Accepted* records are binding; *Proposed* is a direction awaiting
  agreement; *Open* marks a decision deliberately deferred.
- **Each record states its own `Reason`.** The reasoning lives here in full — this
  document is self-contained and is the only architecture reference the build
  needs.
- **One decision per record.** To change a decision, add a new record that
  supersedes the old one — never edit the meaning of an accepted record in place.
- When you make a new architectural decision, add a record in the same change and
  reference it from the code and from
  [`CODING-STANDARDS.md`](./CODING-STANDARDS.md) where relevant.

Legend: Accepted · Proposed · Superseded · Open.

---

## A · Foundation

### ADR-0001 — Kotlin is the primary language; Java is the exception

**Status:** Accepted

**Decision.** Production code is written in **Kotlin**. Java is used only where a
bug or a library restriction prevents Kotlin — for example the logging facade,
`JBoss Logging`, which is Java-first.

**Reason.** Kotlin's null-safety removes an entire class of runtime faults; its
data classes, sealed types and coroutines fit the shape of this domain closely;
and one language across the platform lowers the cost of moving between
components. Java is available when a dependency forces it, but it is the
exception, never a second primary language.

**Consequences.** Build tooling compiles Kotlin first. A Java file in the tree
must name the restriction that required it.

### ADR-0002 — Gradle with the Kotlin DSL; dependencies centralised in the version catalog

**Status:** Accepted

**Decision.** The build uses **Gradle** with the **Kotlin DSL**. Every library
coordinate and version is declared once, centrally, in
`gradle/libs.versions.toml`. A new library is not introduced without the approval
of the project leadership.

**Reason.** Keeping the build in the same language as the code removes a context
switch. Centralising versions gives one place to see and change what the platform
depends on, and it turns "add a library" into a deliberate, reviewed act rather
than an incidental import — which is what keeps the dependency surface small and
auditable.

**Consequences.** No version literal appears in a module build file; no dynamic
versions (`1.+`) or snapshots in a release build. Build configuration otherwise
comes from the shared Hamoni Gradle convention scripts.

### ADR-0003 — The platform is organised into three project directories plus documentation

**Status:** Accepted

**Decision.** Work is grouped into three **directories** — `control-room/`,
`process-runner/` and `common/` — plus `docs/`. Each directory is a **holding
area for one or more build projects**, not a project itself.

**Reason.** The platform has two independently deployable applications and a
shared core, and each of those is large enough to hold several projects (for
example a user-interface project and a data-model project inside
`control-room/`). Grouping projects by area keeps that shape visible, keeps the
dependency direction honest, and lets each area grow without renaming anything.

**Consequences.** Project names and their exact split are decided area by area;
the directory names are stable and are referenced throughout this document.

### ADR-0004 — Teams and projects are orthogonal, and both are top-level objects

**Status:** Accepted

**Decision.** A **team** and a **project** are peers. A team answers *who is
accountable for this work and who does it*; a project answers *which processes
ship together, and where*. A process has one owning team and belongs to one
project, but **neither nests inside the other**. A project may package processes
owned by several teams, and a team may own processes in more than one project.

**Reason.** These are two different questions, and collapsing them causes real
harm. An *order-to-cash* project can package processes owned by sales, finance
and logistics without moving any of them; a team can contribute to several
projects. If a project sat inside a team, or a team inside a project, then
reorganising either side would break links and would confuse accountability with
packaging. Keeping them as peers is what lets teams change and projects ship
independently.

**Consequences.** There is no ownership cascade between teams and projects;
project membership is packaging, not accountability. This record is deliberately
placed with the foundation: it is a primary explanation of how Hamoni is shaped,
not a detail of one screen.

### ADR-0005 — A process definition and its instance are the central working object

**Status:** Accepted

**Decision.** The **process** is the central working object: a **process
definition** (the design) and a **process instance** (a running case). Tasks and
documents belong to an instance; versions and deployments belong to a project.

**Reason.** The process is what runs, and it is the thing people reason about
day to day, so it is where the detail naturally attaches. This does **not** make
it the parent of everything: **teams and projects remain peer top-level objects**
(ADR-0004). Teams own accountability; projects own packaging; a process is the
work itself.

**Consequences.** A task or a document is never free-floating (ADR-0031); a
version or a deployment is never held on a process (ADR-0008, ADR-0010).

### ADR-0006 — A team is a shallow container that owns a process

**Status:** Accepted

**Decision.** A team owns a process. There is **no nesting beyond one level**.

**Reason.** Teams are cheap to create and represent a real group of people. A
team in real life never contains a sub-team — there is no such object — so a
nested model would model something that does not exist. A person who works
across several groups does so by belonging to several teams, not by living in a
hierarchy. Nesting is also the single largest source of "where did it go?", and
reorganising must never break a link or a bookmark.

**Consequences.** Ownership is a single relationship, not a path. Reuse across
teams is expressed as labels, never by copying a process into a second home.

### ADR-0007 — Access is a grant, not a location

**Status:** Accepted

**Decision.** Membership of the owning container gives **sight** of a process;
**starting** it is a **separate** grant. A saved path or bookmark is never what
lets a person in. A per-process override remains possible.

**Reason.** Finding something and being allowed to use it are different
questions. Separating sight from the right to start means access survives
reorganisation (nothing is granted by location), and it makes the security model
readable: the same person can see a process and still be unable to run it.

**Consequences.** "Sight" and "open" are distinct checks in the service layer; a
list may show a row whose detail view will not open.

### ADR-0008 — A project is a deployment unit

**Status:** Accepted

**Decision.** A project packages **at least one process**, the **business rules**
those processes share, an **environment** and a **version**. It is the unit that
is published and deployed.

**Reason.** Deployment needs a boundary: something that collects what ships
together, is versioned as a whole, and points at where it runs. A single process
is too small (rules are shared; processes ship together) and the organisation is
too large. The project is that boundary.

**Consequences.** Publishing and deploying act on a project, not on an individual
process.

### ADR-0009 — Environments are labels; there is one by default

**Status:** Accepted

**Decision.** An environment (development · test · production) is a **label** on
a project, and there is **one by default**. An environment points at a **version**,
so promoting a build is moving a pointer, not copying anything.

**Reason.** Most organisations never need more than one environment, so the
concept must cost nothing when it is unused — it appears only when someone adds a
second. Modelling the environment as a label that points at a version means a
project can run one version in test and another in production without a copy to
keep in step.

**Consequences.** Adding a second environment is additive and must not change the
common case.

### ADR-0010 — Versions are linear and immutable; changes are forward-only

**Status:** Accepted

**Decision.** Editing happens in a single **head draft**. Publishing creates an
immutable version and increments the number. There is **no branching** and **no
reversion**: going backwards is a **redeploy** (move an environment's pointer),
and bringing content forward from an older version is a deliberate, manual act.

**Reason.** "Every change increments a version" is only affordable if history
stays simple — one path, no branches to reconcile, and no reversion that rewrites
the forward narrative by making a new version semantically equal to an old one.
Deploy-only problems (a bad release, an interdependent system that broke) are
served by moving a pointer; authoring problems are served by fixing forward. The
intermediate versions keep their meaning either way.

**Consequences.** One path of history per project; the version list is built for
recency, not completeness; a published version is read-only by construction.

### ADR-0011 — Assignment and delivery are separate concerns

**Status:** Accepted

**Decision.** **Assignment** — whose work this is — is set on the task per
environment, shared by default. **Delivery** — whether it reaches a real queue —
is gated by whether the deployment is **classified as production**. Classification
sits on a **deployment** (a version paired with an environment), not on the
environment's name.

**Reason.** If assignment and delivery are the same thing, then the moment an
organisation adds a test environment, its test work routes straight to the
production team — the exact failure the feature exists to prevent. Keeping them
apart makes adding an environment safe until someone deliberately points it at
real people. Classification keys on the deployment rather than the environment's
name because every customer has a "preprod" they consider production.

**Consequences.** The delivery guard applies to every delivery — tasks,
notifications, integrations and document creation, not only tasks.

---

## B · Process and activity lifecycles

### ADR-0012 — The process state model

**Status:** Accepted

**Decision.** A process instance lifecycle is
**Active · Pend · Exception · Paused · Completed · Cancelled**. *Active* is the
normal running state; *Completed* and *Cancelled* are terminal. The state is a
**runtime** property — it is not part of the process definition.

**Reason.** The lifecycle must distinguish the ways a process stops: waiting on
the outside world (pend), a technical fault (exception), a deliberate internal
pause, and a clean or forced ending. Bundling any of these loses the clock
behaviour and the recovery route. The state is runtime, not definition, because
the same definition runs many different ways.

**Consequences.** `end·normal` on all branches → Completed; `end·terminate` →
Cancelled; an external cancel → Cancelled. Cancelling is allowed from Exception,
not from Pend. Model it as a sealed type.

### ADR-0013 — The activity (task) state model

**Status:** Accepted

**Decision.** An activity's lifecycle is **Available** (sub-status **Waiting** or
open) · **Assigned** · **Active** · **Completed**, plus **Cancelled** when the
enclosing process is cancelled or terminated. Transitions are take/assign, start,
pause, complete and re-open. Re-opening creates a **new** activity, never a
resurrection.

**Reason.** Work has its own lifecycle, separate from the process's: a person
takes it, starts it, pauses it, completes it. *Completed* is a marking, not a
judgement — quality reads the output — and re-doing work is a new unit of
tracking, so a re-open is a new activity. The activity state is **orthogonal** to
the process state: a pended process still shows its task as *Assigned*, and a
running activity still finishes.

**Consequences.** Manual activities use the full model; agent activities use
Assigned·Active·Completed; automated activities collapse to Active→Completed.

### ADR-0014 — Pend is external, first-class, and stops the process

**Status:** Accepted

**Decision.** A **pend** stops normal execution because something **outside the
organisation's control** must move. It lifts a task action to a **process
state**, reverts the task to *Assigned*, and is configured at design time with a
clock rule (stop the clock, or not) and a mutually exclusive **resume method**
(manual · a time value · after the processes it started).

**Reason.** A pend exists to model the outside world; if the organisation could
resolve it, it is a referral or a step, not a pend — that boundary is the whole
point. Because it stops the process and lifts the team's obligation, it is a
process-level decision with a confirmed reason, and time lost to it must not
count against the team, so it carries its own clock rule.

**Consequences.** A pend is not an exception and must never share its clock
behaviour. While pended, all parallel paths are suspended without interrupting
running activities; only the resume method resumes.

### ADR-0015 — Exception is a technical fault and never stops the clock

**Status:** Accepted

**Decision.** An **exception** is an internal fault (an invalid state, a failed
integration) that requires external input. It **never stops the clock** — the
service-level agreement keeps running.

**Reason.** An exception is a fault, not an act of the outside world, so it has
the opposite clock behaviour to a pend: the consumer is unaware of the fault and
is still waiting, so the clock must keep running. Keeping exception and pend as
separate first-class states — rather than one "problem" state — is what allows
the service-level agreement to be expressed as a distribution ("X% in Y time",
ADR-0030) instead of a hard deadline.

**Consequences.** Exception and pend are distinct event types with opposite clock
behaviour.

### ADR-0016 — JSON Process is the executable process model

**Status:** Accepted

**Decision.** Hamoni has **one modelling style**. The simple flowchart a
designer draws compiles transparently to a **JSON Process** specification —
BPMN-inspired but not BPMN: **executable**, with **pend and exception
first-class**, **state data part of the model**, and **no design-time layers**
(one model, always executable, refined over time).

**Reason.** BPMN is dated and limited for this product, and it separates design
layers from execution. Hamoni needs one artefact that is always executable, with
the states this product actually has built in rather than bolted on. One model
means simulation, validation and execution all run against the same thing.

**Consequences.** "Standard/advanced" are user-interface modes, not two
notations. The stored artefact is a **process definition**, validated against the
**process schema**.

### ADR-0017 — JSON Process is defined as a JSON Schema; state data uses JSON Schema typing

**Status:** Accepted

**Decision.** The specification is defined as a **JSON Schema**. **State data**
uses the standard **JSON Schema** typing system (draft 2020-12), not a
Hamoni-specific one. Every field is a JSON Schema with a **required
description**, declared and never inferred. Validation happens at the boundary
(an interface payload, an activity output, a signal payload) and raises an
exception on mismatch. Money, tables and dates are **patterns**, not new types.

**Reason.** An executable model needs a precise, standard way to describe its
data. Reusing JSON Schema means standard tooling applies and there is nothing
bespoke to learn; requiring a description on every field, declared rather than
inferred, keeps the boundary honest.

**Consequences.** A parallel marker requires an array-typed field. The schema is
the single source of truth for validation and for execution.

### ADR-0018 — A process has up to three isolated components

**Status:** Accepted

**Decision.** A process is `main`, with an optional **pend handler** and an
optional **exception handler**. The handlers are **isolated** — they share no
flow line with the main process and are peer canvases, not drawn on the spine.
**State data is declared once, at process level, and shared by all three.**
Handlers cannot nest. A process completes when no executable token remains in any
component and it is not suspended.

**Reason.** A handler must be able to resolve a pend or an exception without
re-entering the main flow; if it could, it would create a parallel loop and leave
the old path hung. Isolating the handlers prevents that by construction, while
sharing one block of state data keeps them useful.

**Consequences.** A pend handler finishing does not resume the main process —
only the resume method does.

### ADR-0019 — Junctions are pure synchronous predicates

**Status:** Accepted

**Decision.** A junction decides **only from data it already holds** — **no
input or output, no waiting, no side effect**. Its logic may be arbitrarily
complex but must be synchronous. Any external value must first be materialised
by an activity. The condition lives **inside** the junction; a branch references
an edge identifier.

**Reason.** A decision that waits or calls out blocks the flow and is hard to
reason about. Restricting junctions to a pure predicate over in-memory data keeps
them fast and predictable, and forces external data to be fetched by an activity,
where it can be seen, retried and audited.

**Consequences.** Complex or inclusive gateways, and gateways that wait, are
permanently excluded. The designer offers to insert the fetching activity before
a junction that references an undeclared value.

### ADR-0020 — Signals use messaging semantics

**Status:** Accepted

**Decision.** Intermediate events are **sync** nodes. A signal's target is
**broadcast** (all listeners) · **any** (one of the listeners) · or **specific**
(a named process). Correlation has three parts: **channel** (the signal name) ·
**selector** (a pure predicate over the payload) · **payload mapping** into state.
An inbound signal **never creates an instance**.

**Reason.** Messaging already has well-understood semantics for "tell everyone",
"tell one listener" and "tell a specific listener". Reusing them avoids inventing
a parallel event model. An inbound signal is correlated to an existing instance,
so it cannot start one; starting from a signal is a separate, deferred
integration concern.

**Consequences.** The first release starts processes by activity, by interface
and by timer.

### ADR-0021 — The whole process definition file is versioned

**Status:** Accepted

**Decision.** A process definition versions **the whole file**. How a definition
is stored, diffed and rolled is **out of scope** for the specification.

**Reason.** The specification should describe processes, not version control.
Versioning the whole file and leaving storage and rolling to the implementer
keeps the specification small and portable, and lets Hamoni own versioning at the
project level (ADR-0010).

**Consequences.** The specification stays implementation-agnostic.

---

## C · Identity, tenancy and security

### ADR-0022 — Identity is OpenID Connect only; there are no passwords

**Status:** Accepted (revised 2026-10-15)

**Decision.** Hamoni stores no password; every route authenticates through an
external identity provider using **OpenID Connect (OIDC)**. A person is a
**provider and subject** pair. The same subject may belong to **many
organisations**, and the same human on two providers is **two identities**. The
word a person meets is **organisation**; *tenant* stays technical and internal.
Multi-factor authentication stays provider-side.

**Reason.** Storing passwords is a liability with no upside. Using the subject —
not the email address, which can change — gives a stable key for a person across
identity changes; letting one subject belong to many organisations lets a person
move between them without a second account, while two providers correctly remain
two identities.

**Consequences.** The profile surface shows the name **and** the email so a
person can tell which account they are on. There is no local credential store to
secure, and no Hamoni multi-factor screen. The sign-in surfaces and flows are
ADR-0041 to ADR-0046.

### ADR-0023 — Groups reference the directory; roles are derived and additive

**Status:** Accepted

**Decision.** A **group** is a Hamoni-owned object — a name, explicit managers,
and a membership rule — that references directory groups and users **by
identifier**, storing references and never a copied member list. Roles (**Team
manager**, **Process owner**, **Member**) are **derived at sign-in**, never
assigned, and are **additive** — a person holds the union of every role they
qualify for.

**Reason.** Directory groups rarely map cleanly onto a team that works a process,
so a local group object keeps directory quirks out of the permission logic and
lets the mapping be fixed without a release. Deriving roles at sign-in rather
than storing them means directory drift and ownership changes correct themselves.
Roles are additive because a person commonly works one process, designs a second
and owns a third, and each screen composes from the union.

**Consequences.** Permission is a function of derived role plus the security
model, not a stored role field.

### ADR-0024 — Ownership and permission are separate; the security model is named and small

**Status:** Accepted

**Decision.** Permission is derived from a few named organisation defaults —
**collaborative · normal · secure** — with a per-project override and optional
additional-team lists. The permission vocabulary is **view · edit · deploy ·
start**. **Ownership** (the owning team) is an accountability label and is **not**
a permission. **Who works an activity** is the designer's team assignment,
inheriting project-owner team → process-owner team → task-owner team, most
specific winning.

**Reason.** A permission model nobody can explain is a permission model nobody
trusts. Deriving permission from a few named defaults, with a per-project override
and a plain-language summary, means "why can this person edit?" is answered in
one place. Keeping ownership as a separate accountability-only label stops
permission and ownership competing, and treating "who works an activity" as a
designer's assignment rather than another grant keeps the model small.

**Consequences.** "Execute" ("who can start") is separate from "view" and lives
at process level; a reusable process may be startable by anyone.

### ADR-0025 — Multi-organisation, with in-session switching

**Status:** Superseded in part by ADR-0047, ADR-0048 and ADR-0052 (the isolation
mechanism and the session mechanism; the isolation *intent* stands).

**Decision.** The platform serves many organisations. The organisation is
switched **in-session** from the organisation menu; **sign-out** lives in the
user menu. The organisation context is carried through every request and
enforced at the data layer.

**Reason.** People move between organisations without signing out, so switching
must not force a round trip through sign-in. Carrying the context through every
request and enforcing it at the data layer is what keeps organisations isolated
when one deployment serves many.

**Consequences.** Every organisation-scoped query must carry the context; a
missing filter is a defect. The user-facing word is **organisation**, never
*tenant* (ADR-0022).

### ADR-0026 — Auditing uses the Hamoni audit framework

**Status:** Accepted

**Decision.** All auditing goes through the **Hamoni audit framework**. No custom
audit handler, and no ad-hoc logging, is used to record an auditable action.

**Reason.** Transparency is deliberate: seeing that every action is recorded is
what keeps people honest. That only holds if the record is uniform and complete.
A single framework, used everywhere and never bypassed, is what makes the record
trustworthy; bespoke handlers and ad-hoc log lines fragment it and cannot be
relied on.

**Consequences.** Audit writes are not optional and are not best-effort. A new
auditable action is registered with the framework rather than logged by hand.

---

## D · Application and interaction contracts

### ADR-0027 — The prompt acts only through a reviewable object

**Status:** Accepted

**Decision.** Natural language is cheap as **input**, never as **execution**.
Any write intent renders as a **plan** — scope, per-row rationale, capability
exceptions, what is deferred — with apply, review-individually and cancel, an
**undo** window, and an **audit entry** naming the authorising human.

**Reason.** Natural language is a poor execution format: ambiguous, and hard to
hold accountable. Turning every write intent into a plan a person can inspect —
with a way to undo it and a record of who authorised it — keeps a human
accountable for every change while still letting the prompt act.

**Consequences.** The write path is a two-phase propose-then-confirm contract,
not a direct mutation.

### ADR-0028 — Advanced mode is one application-wide setting

**Status:** Accepted

**Decision.** Advanced mode is a **single user-level preference**, not a
per-screen or per-panel toggle. Once it is on, anything advanced appears wherever
it applies.

**Reason.** A person who wants the advanced surface does not want to keep
switching it on for each panel, and a person who does not cannot be expected to
know when they should. One setting sidesteps both; where advanced content is
hidden, the surface says so and points at the preference.

**Consequences.** Complex surfaces are gated by one flag; no screen carries its
own advanced switch.

### ADR-0029 — Starting a process is an explicit interface

**Status:** Accepted — the entry point is now organisation-scoped through the
`X-Organisation-Id` header: `POST /api/processes/{processId}/instances` (ADR-0064).

**Decision.** The first release starts processes by activity, by a programmatic
interface and by timer. The interface entry point is
`POST /processes/{processId}/instances`, with the request body validated against
the process's state schema. Starting a process from a signal is deferred.

**Reason.** Starting a process must be available to other systems, not only to
people, so it needs one stable, validated entry point. Validating the body
against the state schema keeps the contract explicit. Starting from an event is a
harder integration problem — correlation, de-duplication and routing — and is
deferred until it can be done well.

**Consequences.** A signal-spawned instance is out of scope for the first
release.

### ADR-0030 — The service-level agreement is a percentage in a time window

**Status:** Accepted

**Decision.** A process's service-level agreement is expressed as **"X% in Y
time"** (for example, *90% in 5 business days*) — a target measured against
attainment, at **process level**. Activity-level maximum-wait and
maximum-execution timers are excluded.

**Reason.** Operations teams need a target that lets the majority of cases land in
time while some run long, without over-staffing for the worst case. A fixed
per-case deadline cannot express that; a distribution can. It also survives the
fact that exceptions keep the clock running (ADR-0015).

**Consequences.** Overrun is acceptable; the measure is a distribution, not a hard
deadline.

### ADR-0031 — Documents attach to a process instance; templates are distinct

**Status:** Accepted

**Decision.** A **document** belongs to a **process instance** — never
free-floating — and its origin determines who may read it, what it is called and
whether it can be replaced. A **template** is the content a document is generated
from, reused across processes.

**Reason.** A document that floats free has no owner, no reader and no home.
Attaching every document to an instance, so that its origin decides access and
replaceability, keeps meaning and permission derivable from one place. Templates
are a separate object because they are reused across processes — content with a
shape, not a file and not a process.

**Consequences.** There is no standalone document store; document access derives
from the instance's access rules.

---

## E · Technical stack — library selections

The library selections for the backend and the frontend. Each is an ADR.

### Backend

#### ADR-0032 — Backend runtime: Quarkus in Reactive mode

**Status:** Accepted

**Decision.** The backend applications run on **Quarkus**, in **Reactive mode**.
Every library and dependency brought into the backend must be reactive.

**Reason.** Hamoni is input/output bound: many process instances sit waiting on
external signals, timers, databases and integrations. Reactive Quarkus lets a
modest deployment hold many concurrent waits without dedicating a thread to each.
Choosing reactive at the foundation means a non-reactive library would become the
single blocking piece that undermines the rest, so dependencies are chosen to
match.

**Consequences.** Blocking calls on an input/output thread are forbidden; a
library that only offers blocking access is not adopted (coding standards,
Backend).

#### ADR-0033 — Persistence: Hibernate ORM with Panache, Reactive

**Status:** Accepted

**Decision.** Data access uses **Hibernate ORM with Panache** in its **Reactive**
form.

**Reason.** Persistence is the most common place a reactive application
accidentally blocks, because a database call is where a thread is easiest to
stall. Using the reactive form of Panache keeps data access non-blocking and
consistent with the rest of the stack (ADR-0032).

**Consequences.** Persistence operations are reactive; a blocking database driver
is not used.

#### ADR-0034 — HTTP interface: Quarkus REST (RESTEasy Reactive)

**Status:** Accepted

**Decision.** The HTTP interface uses **Quarkus REST (RESTEasy Reactive)**.

**Reason.** The interface must be non-blocking end to end. RESTEasy Reactive runs
on the same reactive core as the rest of the stack, so it does not reintroduce
blocking at the edge where a request first arrives.

**Consequences.** Resources are non-blocking and thin, delegating to services.

#### ADR-0035 — Schema migration: Flyway

**Status:** Accepted

**Decision.** Database schema changes are applied by **Flyway**, as forward-only,
reviewable migrations, at startup.

**Reason.** Reactive is a runtime property; the schema still needs deterministic,
versioned change. Forward-only migrations match the platform's forward-only
versioning culture (ADR-0010) and keep schema evolution explicit and reviewable.

**Consequences.** Auto-generated schema is not used in production; an applied
migration is never edited.

#### ADR-0036 — Logging: JBoss Logging (the Java exception)

**Status:** Accepted

**Decision.** Logging uses the **JBoss Logging** facade.

**Reason.** This is the sanctioned exception to the Kotlin-first rule
(ADR-0001): JBoss Logging exposes Java-first types, so a small amount of Java is
unavoidable here. It is used only for this facade, not as a general return to
Java.

**Consequences.** Application code logs through the facade; direct console output
is forbidden.

### Frontend

#### ADR-0037 — The frontend is hosted by Quarkus Quinoa

**Status:** Accepted

**Decision.** The user-facing application is an Angular single-page application
that is built and served by the same Quarkus process, linked through **Quarkus
Quinoa**.

**Reason.** One runtime and one deployment for the interface and its backend
removes a separate front-end server to build, deploy and keep in step, and keeps
local development to a single command.

**Consequences.** The frontend build is part of the backend application's build.

#### ADR-0038 — Frontend framework: Angular with PrimeNG and Tailwind

**Status:** Accepted

**Decision.** The frontend uses **Angular**, with **PrimeNG in styled mode** and
**Tailwind** as the layout layer. PrimeNG is driven by a generated **Hamoni
preset** built from the design system's tokens — the palette, the radius scale
and the two type families — mapped onto PrimeNG's semantic tokens. Unstyled
(pass-through) mode is used per component only, for a control the preset cannot
express.

**Reason.** Driving the whole component library from one generated preset means a
design-system change re-colours every component from one place instead of being
chased per screen. Tailwind carries the shell and bespoke layout from the same
tokens, so the two layers cannot drift apart. Angular provides the application
structure the design system's screens assume.

**Consequences.** The design system is bound at one preset, not per screen.
Icons come from a single icon set on one size scale.

#### ADR-0039 — Frontend testing: Mock Service Worker (MSW)

**Status:** Accepted

**Decision.** Frontend tests and local development mock the interface with **Mock
Service Worker (MSW)**.

**Reason.** Front-end behaviour needs realistic request and response handling
without a running backend. MSW intercepts at the network layer, so the same
handlers serve both tests and the browser, and the component under test sees a
real request rather than a stubbed module.

**Consequences.** Interface handlers are shared between tests and local
development; components are not tested against hand-rolled fake clients.

**Refined by** ADR-0063 — the frontend's unit-test framework, and MSW's role.

---

## F · Documentation

### ADR-0040 — Documentation is an Antora component

**Status:** Accepted

**Decision.** Long-form documentation is authored in **AsciiDoc** under `docs/`
as an **Antora** component. The documentation build is wired into Gradle. This
decision log is the seed of the Antora Architecture section.

**Reason.** Documentation should be written once, kept with the code it
describes, and published as a site. Antora composes the AsciiDoc in this
repository into the platform documentation, so the architecture records here
graduate into structured pages as they stabilise.

**Consequences.** Documentation is built by the same build that builds the code.

---

## G · Authentication and organisation access

These records refine the identity and access model (section C) for the sign-in
surfaces and the flows behind them. They were added after the sign-in design pass
and are the current model; where they conflict with an earlier record, they win.

### ADR-0041 — Authentication and authorisation are separate concerns

**Status:** Accepted

**Decision.** **Authentication** proves *who you are*, once, through an OpenID
Connect provider. **Entering an organisation** is a separate grant. A signed-in
person who holds no access to an organisation is **not an error** — it is an
**authorisation outcome** with its own surface and next action.

**Reason.** Conflating the two makes "signed in, but not a member" read as a
failure, and tempts a switcher on the login page that cannot fix it. Kept apart,
each gets the right treatment: authentication is a plain provider round-trip, and
an authorisation failure is resolved where the person can actually act on it.

**Consequences.** There is **no authentication-error surface**; a provider cancel
or failure is not a designed page. Authorisation failures are handled by the
organisation chooser (ADR-0044).

### ADR-0042 — The three login pages and the three flows

**Status:** Accepted

**Decision.** There are **three login pages**: the **global (default)** page,
which authenticates only and carries no organisation; and an **organisation login
page**, its own route, configured as **A** public methods only · **B** public
methods and private ("sign in with my company") · **C** private methods only.
Config **C renders no page** — the visit redirects straight to the organisation's
provider. There are **three flows**: **J1 global sign-in** (authenticate → select
the organisation from session state, or determine the default → authorise);
**J2 organisation sign-in** (authenticate by any method the page allows → select
that organisation → authorise); **J3 authorisation** (with access, enter;
without access, break and show the organisation chooser).

**Reason.** The organisation's configuration, not a binary question, decides
which methods its page offers. Consolidating the surface to three pages and three
flows keeps the model small and matches a single-page frontend.

**Consequences.** For the public methods, the provider order is **Microsoft, then
Google, then Apple** — a method list is not a leaderboard: every public method
carries equal weight, and only the organisation's own configured method is
preferential.

### ADR-0043 — Organisation access requires an enabled provider and a profile

**Status:** Accepted

**Decision.** Access to an organisation requires **both**: the **provider is
enabled for the organisation** (configured explicitly, or an allowed public
method), and **a profile exists** (the person is the organisation's owner, or
holds an invited profile). **Revocation is organisational** — removal from the
organisation. An **invite creates the profile** (carrying the invite code) before
the person has an account; accepting fills it in. A directory sync **disables**
the profile, and a disabled profile has no access.

**Reason.** Team membership cannot express access — a person may leave every team
and still belong — so revocation must be removal from the organisation. Creating
the profile at invite time is what makes that removal possible before the person
has an account, and disabling the provider is what closes the door for every
identity that used it.

**Consequences.** Disabling a provider removes access for all identities that used
it. Assignments and team membership are never a substitute for access.

### ADR-0044 — The organisation chooser never auto-decides; authorisation runs once

**Status:** Superseded in part by ADR-0048, ADR-0054 and ADR-0057 (the session
mechanism and the cache; "never auto-decides" stands and is extended).

**Decision.** The **organisation chooser** validates the person's access to their
remaining organisations but **never silently selects one**. **Authorisation runs
once, at launch**; the grant holds for the session and is evaluated again only
when the **organisation is switched** or the **application is reloaded**.

**Reason.** Auto-selecting would answer one organisation and enter another, and
because the organisation is only visible from the profile popup, the person would
be quietly confused. A single-page frontend does not re-check authorisation on
every page or request.

**Consequences.** No access to the attempted organisation **breaks the journey**
and prompts the chooser. The chooser is the surface for every "you cannot enter
here" case — the happy case, an organisation turned private-only, revoked access,
an unaccepted invite, and disabling by directory sync.

### ADR-0045 — The login page is anonymous; it never names the organisation

**Status:** Accepted

**Decision.** The login page **never renders the organisation's name**, and
offers **no "switch organisation" affordance**. The URL carries the organisation
(`/auth/login?organisation=…`); the page shows none. With no organisation
parameter, the **public page** is chosen. Provider marks are the **registered
vendor glyphs**. Every method button carries the application's control fill
(`--bg` over the surface), and hover is uniform across the method and accent
buttons.

**Reason.** Rendering the name would let anyone enumerate login pages to discover
which organisations subscribe; the same risk removes the switcher. Authorisation
is not the login page's problem (ADR-0041), so the page stays a plain
authenticator.

**Consequences.** The **organisation chooser**
(`screens/organisation-selector.html`) is the separate, pre-organisation surface
listing the organisations a person may enter; it keeps the sign-in shell because
the person is not yet in an organisation.

### ADR-0046 — Invitations are against an organisation, and two-way

**Status:** Accepted

**Decision.** An invite link is **always against an organisation**, never the
public page (`/auth/login?organisation=…&invite=…`). The person **authenticates
first**, then lands on the invitation surface **after login**, where the
organisation is named **once** and a single **Join** action completes and
activates the profile. There is **no decline control** — doing nothing adds no
one, so closing the browser is the decline. The **24-hour expiry lives in the
invitation email**, not on the surface. Inviting is **two-way** even when the
person is already known.

**Reason.** The invite creates the profile before an account exists (ADR-0043),
which is what makes revocation organisational. Naming the organisation only once,
and only after authentication, avoids leaking its existence to an unauthenticated
visitor.

**Consequences.** The invitation surface keeps only the next action.

---

## H · API and database contract

These records settle the interface between the frontend and the backend and the
rules that govern the database beneath it. They refine the identity and access
model (section C) and the technical stack (section E); where they conflict with
an earlier record, they win. The requirements sit in
[`API-AND-DATABASE.md`](./API-AND-DATABASE.md).

### ADR-0047 — The backend is stateless; the credential identifies the caller only

**Status:** Accepted

**Decision.** The backend holds **no session state and no request state**. The
server issues one credential — a session cookie for the browser, a bearer token
for a non-browser caller — that identifies only the authenticated **principal**,
the inbound OIDC provider-and-subject pair. In Quarkus the principal is
serialised and encrypted with the platform key into the token. The organisation
and any other context are **not** placed in the credential and **not** held
server-side; they are resolved per request.

**Reason.** Holding the current organisation server-side forced one of two
unwanted designs: shared session state across instances, which produces
synchronisation faults; or a frontend-held cookie, which mutates the credential
and breaks non-browser access. Restricting the credential to authentication keeps
the backend horizontally scalable and makes browser and machine callers
symmetric.

**Consequences.** The platform key must be identical and stable across every
instance, or a token issued by one instance cannot be read by another; rotating
it invalidates live sessions. Organisation and authorisation are resolved per
request (ADR-0064, ADR-0054). Supersedes the session-context aspects of
ADR-0025 and ADR-0044.

### ADR-0048 — The organisation is a path segment; switching is a frontend concern

**Status:** Superseded by ADR-0064

**Decision.** Every business call carries the organisation as a path segment,
`/api/{organisationId}/…`. Switching organisation is a **frontend route change**
that redirects the application's calls to the new organisation; it is **not** a
server state change and is not a `POST`. The credential does not change.

**Reason.** With a stateless backend (ADR-0047) there is no server object to
change when the organisation is switched, so modelling the switch as a server
command would reintroduce session state or mutate the credential. A path segment
makes the organisation part of the request's identity, which is also what the
schema-based isolation keys on (ADR-0052).

**Consequences.** The `CODING-STANDARDS.md` §20.8 example, which used
organisation selection as its case for "never change state through `GET`", is
replaced. A request without a valid organisation segment is not a business call.
Supersedes the switch mechanism of ADR-0025 and refines ADR-0044.

### ADR-0049 — The interface is three annotated groups of APIs

**Status:** Accepted

**Decision.** The interface is divided into three groups, marked in the code by
an annotation that also drives documentation generation: **A** authentication and
account (`/api/auth/…`), **B** business (`/api/…`) and **C** screen flow
(`/api/…`). The organisation travels in the `X-Organisation-Id` header on every B
and C call and is not required for group A (ADR-0064). Groups A and B are **publicly
documented** and every one of their resources carries the annotation that
generates its OpenAPI description from the code. Group C is **available but not
publicly documented**; it carries the internal marker that keeps it out of the
published document, while its types are still generated for the frontend.

**Reason.** Some endpoints exist to assemble a screen and are not part of a
stable published contract, but they are still real endpoints the frontend calls.
Separating them by annotation keeps the published description honest without
forcing the frontend to hand-write the types for the endpoints it depends on.

**Consequences.** Documentation is generated, never hand-written beside the code
(§19). A group C endpoint is one the frontend may use but a third party should
not; promoting one to the public contract is a deliberate change of annotation.

### ADR-0050 — There are no API keys; every actor is an OIDC principal

**Status:** Accepted

**Decision.** Hamoni has **no API keys**. A system or an agent has **its own
account**, authenticates through **its organisation's identity provider**
(certificate authentication, domain trust or whatever the provider requires), and
receives a **principal of its own** through the same OIDC flow a person uses.
From Hamoni's point of view a system caller and a person caller are the same kind
of thing.

**Reason.** An API key lets a system act inside a person's context without being
that person, which breaks the rule that work is always identifiable to an actor
who participated. Because authentication is already OIDC (ADR-0022), a machine
can be a first-class principal instead of borrowing one.

**Consequences.** A system's actions are attributable to the system, not to a
person. The grant a system holds is an open question (see below).

### ADR-0051 — Principals: an inbound context resolved to an authentication principal

**Status:** Accepted

**Decision.** An inbound OIDC context is never used directly as a person. It is
resolved to a **principal** with a type: **normal** (a `person`, a `system`, or
an `agent`) or **on behalf of** (recording both the acting identity and the
identity it acts for — a person on behalf of a person, or an agent on behalf of a
person). Principals are **granted by the system**; for example, starting an
AI-agent chat grants the agent a principal naming the agent and the person it
works for. A principal identifies the **authentication** context only — never the
organisation and never the authorisation context.

**Reason.** Actors in this product are not only people: an agent may work in a
chat on a person's behalf, and an autonomous agent worker may act on its own.
Modelling these as principals keeps one authentication model while preserving the
distinction an audit needs — who acted, and for whom.

**Consequences.** Audit records the acting principal and, where one exists, the
person behind it (ADR-0026, ADR-0027). Organisation and authorisation are
resolved separately, per request (ADR-0047, ADR-0054).

### ADR-0052 — Two tiers of schema, with schema-per-organisation isolation

**Status:** Accepted

**Decision.** There are two kinds of schema. A **global schema** holds the
**authentication profiles** — the link from an inbound OIDC context to an
identity UUID and its organisation pointers. It is application-managed and
self-cleaning, and **no user transacts in it**. Each **organisation schema**
holds everything organisation-scoped: user profiles, team and project
configuration, process and deployment state, and the organisation's audit trail.
Global tables are addressed with an **explicit schema qualifier**; every other
table belongs to the organisation schema named by the organisation identifier.
Isolation is therefore by **schema, not by a discriminator column**, and a query
cannot cross organisations. A Quarkus OIDC `TenantConfigResolver` resolves the
organisation's OIDC configuration from the database and links it to the
organisation's Hibernate tenant configuration; Hibernate's tenant resolution
(`MultiTenantConnectionProvider`, `CurrentTenantIdentifierResolver`) applies that
tenant to the request, to be confirmed under reactive Panache. Group A requests
address the global schema outside tenant resolution (ADR-0064). A deployment-level
flag selects multi- or single-organisation mode (ADR-0053).

**Reason.** A single shared schema needs every query to remember a discriminator,
and a forgotten filter is a cross-organisation leak. Making the schema part of
the query's identity makes isolation structural: there is no filter to forget.
Separating global from organisation data also gives a clean break for
off-boarding — an organisation's data is removed by removing its schema, with no
effect on any other organisation — and lets a dedicated installation run a single
schema.

**Consequences.** Flyway work is split into global and per-organisation sets and
is applied to every organisation schema (ADR-0035); creating an organisation
creates and migrates its schema. There is no cross-schema foreign key.
Migrations must be safe under a rolling deployment (ADR-0058, Proposed).
Refines ADR-0025: the isolation intent stands, the mechanism changes.

### ADR-0053 — A deployment-level single-organisation flag, defaulting to single

**Status:** Accepted

**Decision.** A **deployment-level flag** selects multi-organisation or
single-organisation mode. The **default is single-organisation**, in which the
deployment supports exactly one organisation and **creating new organisations is
disabled**.

**Reason.** A dedicated or enterprise installation does not run a public sign-up
and should not be able to create organisations it will never use. Defaulting to
the smaller surface means the common dedicated case needs no configuration, and
the multi-organisation capability is opted into deliberately.

**Consequences.** The organisation-creation surface exists only in
multi-organisation mode. In single-organisation mode the authorisation outcome
"no access" is a hard stop rather than a choice among organisations (ADR-0057).

### ADR-0054 — Authorisation outcomes are cached, bounded by the session lifetime

**Status:** Accepted

**Decision.** Determining whether an identity may enter an organisation — the
provider is still enabled **and** the org-scoped user profile is still enabled
(ADR-0043) — is cached, **positive and negative**, for a bounded time. The
**time-to-live is never greater than the session lifetime**; a fixed short window
is the working figure. A change to a **user profile** (disabling a user,
accepting an invitation, creating an invitation) invalidates that identity's
entry; a change to the organisation's **OIDC configuration** invalidates the
whole organisation's entries. Listing an identity's organisations validates each
pointer by the same rule and uses the same cache.

**Reason.** The check touches the authentication profile, the provider
configuration and the user profile on every request, which is too heavy to run
uncached, and listing organisations repeats it for every pointer. A short,
invalidation-aware cache bounds the cost while keeping revocation effective
within the window. The cache is a derived structure, not authoritative state,
which is what keeps the backend stateless (ADR-0047).

**Consequences.** Revocation takes effect within the time-to-live, not
instantly; the invalidation rules above are what keep the window honest.
Refines ADR-0044.

### ADR-0055 — Multi-provider OIDC validation is issuer-first and never proxies

**Status:** Accepted

**Decision.** An organisation may configure more than one identity provider.
Validation reads the **issuer** claim without trusting the token, resolves the
matching provider's keys from its discovery endpoint, **caches those keys** (they
change rarely), and validates the **signature locally**. A token is accepted only
when it validates against exactly one configured provider, and it is **never
forwarded** to a provider.

**Reason.** To know which key to validate with, the issuer must be read first —
from an as-yet unverified token. Doing this carefully, with locally cached keys
and local signature verification, avoids turning Hamoni into a proxy for an
attack on a provider and avoids a request path that forwards credentials. Caching
keys keeps the discovery round-trip off the hot path while a short window bounds
the exposure.

**Consequences.** Provider-key caching and the issuer-to-provider mapping need
careful configuration so a token issued for one provider is never validated
against another and never cross-pollinated. The organisation whose provider
configuration is used is selected by the `X-Organisation-Id` header, or the
default organisation when the sign-in-methods call omits it (ADR-0064);
signature validation still applies in full.

### ADR-0056 — Identity attributes are owned by the OIDC claims

**Status:** Accepted

**Decision.** Name and email are **owned by the OIDC claims** and are **never
editable in Hamoni**. They are stored as a cached projection and refreshed on the
next sign-in or token validation. A person who changes their name or email does
so at the provider.

**Reason.** Identity attributes belong to the identity provider (ADR-0022);
letting Hamoni edit them would create a second source of truth that drifts from
the provider on the next sign-in. Reading and caching them keeps one owner.

**Consequences.** There is no Hamoni surface to edit name or email. A stale
projection is corrected automatically at the next authentication.

### ADR-0057 — Organisation choice never auto-enters; the access surface is neutral

**Status:** Accepted

**Decision.** A stored **default organisation** never auto-enters. If the person
cannot enter it, they land on the **organisation selector** for their next action
— select another, create one, or, in single-organisation mode, stop. The same
surface is shown for an organisation that **does not exist**, so the existence of
organisations cannot be enumerated. An optional **request access** action may
raise a task for the organisation's managers; where no such organisation exists,
the action leads nowhere.

**Reason.** Auto-entering would answer one organisation and enter another. A
neutral surface that behaves identically for "no access" and "no such
organisation" preserves ADR-0045's rule that an unauthenticated (or unauthorised)
visitor cannot enumerate organisations, and it gives the person one place to act.

**Consequences.** Authorisation failure is not an error surface; it is the
selector. Extends ADR-0044 and ADR-0045.

### ADR-0058 — Schema upgrades must be forward-compatible across a rolling deployment

**Status:** Proposed

**Decision.** Because a rolling deployment may run an old and a new application
version against the same schema at once, a migration may make only **safe,
additive changes**; a breaking change must be made in steps that keep both
versions viable.

**Reason.** A Kubernetes rollout replaces pods gradually, so two application
versions coexist. A migration that drops or renames something the old version
still reads breaks whatever is mid-rollout. Restricting upgrades to additive
change keeps either version runnable at every point.

**Consequences.** Some changes take several migrations to complete safely. This
record is **Proposed**: the mechanism needs investigation and testing before it
is relied upon.

### ADR-0059 — One type per concept; exposure is controlled by annotation, not by copying

**Status:** Accepted

**Decision.** Wherever possible a concept is modelled by **one type**. The same
class moves from the database to the interface; what is exposed is controlled by
**serialisation annotations** that hide or include fields — not by copying into a
parallel transport type or a separate domain type. For a **list or a search**, a
**lighter projection view** (the few fields a list shows) is queried and returned;
a **second call** retrieves the full object. So `list` and `search` return the
light view and `get` returns the full representation. On the client the full type
is `T` and `list`/`search` are typed `Partial<T>`. The concrete annotation and
projection pattern is pinned by a hand-written example to be added.

**Reason.** Parallel transport, domain and persistence types that mirror each
other are a large amount of mechanical copying and casting that buys no safety
and lets the three drift apart. Serialising a controlled view of one type keeps
them consistent by construction. A light projection keeps list and search queries
cheap — they do not drag the whole aggregate — while the detail call pays for the
full object.

**Consequences.** This revises `CODING-STANDARDS.md` §9 and §11: the rule is no
longer "transport types are separate from domain types" but "carry one type, hide
fields by annotation, and project a light view for lists." Exposure must still be
deliberate — organisation isolation and secret hygiene are unaffected — and the
language boundary still applies (the TypeScript client gets `Partial<T>`).

### ADR-0060 — The interface shape is a resource with at most one sub-resource level

**Status:** Accepted

**Decision.** Every endpoint follows
`/api/{component}/{id}/{sub-resource}`, with the organisation carried in the
`X-Organisation-Id` header (ADR-0064): a top-level **component** collection
scoped to the organisation, an object addressed by its identifier, and
**at most one** sub-resource level beneath it. A sub-resource is one of two kinds:

- a **value** — read with `GET`, overwritten with `PUT` — for example `state`
  (the current task state) and `data` (the task's data). A state change is
  therefore a `PUT` that overwrites the state sub-resource, and the transition is
  **validated on write**.
- an **operation** — invoked with `POST` — for example `assign`.

The shape is **not nested further**: a sub-object is addressed by its own term
(referenced by identifier in the parent's representation), never by a third path
level. The same shape applies to the task component and will extend to processes
and other components. On the task component:

```
GET  /api/tasks                 list/search (light view)
GET  /api/tasks/{taskId}        detail (query parameters set depth)
GET  /api/tasks/{taskId}/state  the current state
PUT  /api/tasks/{taskId}/state  a validated state change
GET  /api/tasks/{taskId}/data   the task's data
PUT  /api/tasks/{taskId}/data   overwrite the data (PUT, or PATCH — open)
GET  /api/tasks/{taskId}/history the audit-style history
POST /api/tasks/{taskId}/assign  assignment / take / refer
```

**Reason.** A verb-per-transition sub-resource forces a **dynamic** state model
into fixed URL segments, so every change to the state model becomes an API
version change. A single generic command endpoint loses the per-operation
schema, documentation and authorisation and is hard to unpick. A `PATCH` on the
task cannot express a guarded transition and leaves the server guessing what
changed. Modelling the **state as a sub-resource overwritten with `PUT`** keeps
the state model free to change behind one stable endpoint, keeps the operation
first-class and authorisable, and satisfies the HTTP semantics (`PUT`
overwrites; `POST` for an operation that is not an overwrite).

**Consequences.** Every B and C call carries the organisation in the
`X-Organisation-Id` header (ADR-0064). Human tasks only — an **automated task
does not appear** in the tasks component. The list is the light view and the
detail call controls depth by
query parameter (ADR-0059); there is no `create` or `update` on the task itself,
because a task is created by execution and its mutable parts are its state and
data. Refinements remain open: the state payload and legal transitions; whether
`data` is `PUT` or `PATCH`; how `assign` distinguishes assignment from referral
(and its overlap with the state change); the detail query parameters. Whoever
extends the shape to processes follows these same rules.

### ADR-0061 — Documents are references to a streamed blob, not API payloads

**Status:** Accepted

**Decision.** A document is a **pointer to a file blob**. Business data (a task's
data, for example) holds the **document identifier**, never the document itself;
the task's representation shows **which** documents it references, not their
content. Document access is a separate component, `/api/documents`, with the
organisation in the `X-Organisation-Id` header (ADR-0064). The document is **not
held in memory and not carried in a JSON body**; upload and download are
**multipart or streamed**, because a document may be large. Data may set a
document-type value.

**Reason.** A document can be massive, so carrying it in the JSON interface would
bloat every response, defeat caching and threaten the non-blocking runtime.
Treating it as a reference to a blob keeps business payloads small and lets the
file move by the mechanism suited to it.

**Consequences.** Upload and download are the only part of the interface that is
not JSON, and must still respect the reactive, non-blocking rule (ADR-0032).
Concrete storage, streaming and permission rules remain open — see the open
questions.

---

### ADR-0062 — Localisation is driven by the user profile

**Status:** Accepted

**Decision.** The front end is **localisable and internationalisable**, and the
**user profile is the single source of the locale**. The organisation sets a
default locale; the individual person may override it, and that choice is stored
on the profile as the person's locale information.

- **Static copy and messages live in the front end.** Every user-facing string —
  labels, messages, states, notifications and the assistant's replies — is held
  in the front-end resource bundle and looked up by a key (CODING-STANDARDS
  §18.3). No string is written inline in a component.
- **Problem messages from the interface carry human, displayable text.** An error
  the interface reports is a message written for a person, not a code; the
  **backend does its best to localise it** into the profile's locale before it is
  returned, so the client is not left translating server prose.
- **Numbers and dates come from the profile's locale.** Formatting — dates,
  times, numbers, currency and lists — is driven by the locale on the profile,
  never by the browser's or the development machine's default.

**Reason.** Hamoni serves business people across organisations and locales, and
the interface has always been required to be localisable (§18.3). One locale held
on the profile means a single decision — made by the organisation where it must
and by the person where it can — governs the whole interface, including any text
the backend produces.

**Consequences.** A new string is added to the resource bundle in every supported
locale. The interface error shape carries localised, displayable message text.
Formatting goes through the shared locale helpers bound to the profile. The
resource bundle and the organisation-default / user-override negotiation are
implementation detail; the contract is the profile. There is no second source of
locale.

---

## I · Frontend testing

### ADR-0063 — Frontend unit tests use vitest; Mock Service Worker is the browsable mock backend

**Status:** Accepted

**Decision.** The frontend has **two testing tools with two jobs**, and neither
replaces the other (refines ADR-0039).

- **`vitest` is the default unit-test framework**, and the default for testing
  **case handling** — the branches, error paths and edge cases of a unit of code.
- **Mock Service Worker (MSW) is a mock backend.** Its purpose is to let a real
  person **log in and browse the application** without the real backend, so
  **every new service ships happy-case handlers**. A service is not complete
  without them.

**Reason.** Unit tests prove the different cases a unit must handle, and a unit
runner is the right tool for that. MSW is not a unit-test tool: it stands in for
the backend so the whole application can be walked through by a person, which is
what makes a service demoable and reviewable the moment it exists. Conflating the
two either leaves edge cases unproven or makes browsing depend on the real
backend.

**Consequences.** A new service ships both its happy-case MSW handlers and its
`vitest` unit tests, and neither stands in for the other (CODING-STANDARDS §23).

---

## J · Organisation resolution

### ADR-0064 — The organisation travels in a request header resolved per request

**Status:** Accepted

**Decision.** Business calls (groups B and C) carry the organisation in the
request header `X-Organisation-Id`, whose value is the organisation identifier (a
UUID presented as a string). A Quarkus OIDC `TenantConfigResolver` uses the header
to load that organisation's OIDC configuration from the database; the resolved
tenant configuration must in turn link to the organisation's Hibernate tenant
(schema) configuration, which sets the current tenant for the request
(`CurrentTenantIdentifierResolver` supplies it to Hibernate). The organisation is
**not** a path segment: group B and C paths are `/api/{component}/…`. The
credential carries the principal only and no organisation, and the backend holds
no organisation state. Switching organisation remains a **frontend concern**: the
application changes the header it sends on subsequent calls; it is not a server
state change and the credential does not change. A B/C request without a valid
organisation header is **not a business call** and returns a problem-details
`400`.

The header is **not required when the request is outside any organisation**:

- **Authentication and account (group A, `/api/auth/…`).** These requests address
  the global schema (ADR-0052) and are not resolved to a Hibernate tenant. Access
  to the authentication profile therefore needs no organisation header.
- **Resolving the sign-in methods to show a person.** This group A request
  optionally accepts `X-Organisation-Id`: with the header it returns that
  organisation's authentication configuration; without it, it returns the
  **default organisation's** configuration. The default organisation is defined by
  the deployment: in **single-organisation mode** it is the sole organisation; in
  **multi-organisation mode** it is boot-time configuration, and if none is
  supplied on first boot a random identifier is assigned. This is what lets the
  public and private sign-in pages render without exposing the identifier of any
  organisation (ADR-0045). The header selects configuration only; it never grants
  access.

The header is request input, not authorisation: the principal's access to the
organisation is still checked per request (ADR-0043, ADR-0054). Selecting an
organisation's OIDC configuration by header is safe because the token must still
validate against that organisation's provider (ADR-0055). The OpenAPI description
declares the header as **required** on every group B and C operation and
**optional** on the sign-in-methods operation; the CORS policy permits it.

**Reason.** Quarkus's accepted multi-tenancy mechanism resolves the tenant per
request through `TenantConfigResolver`, and the organisation's OIDC configuration
is the record from which its tenant is resolved. A request header is the neutral
carrier that works identically for browser and machine callers and keeps resource
paths stable. It preserves the stateless backend (ADR-0047): the header is
request-scoped input, not server or credential state. Exempting group A keeps
authentication working before any organisation is known, and the optional default
for sign-in methods preserves ADR-0045's rule that an anonymous surface never
reveal which organisations exist. That default is a deployment-level default for
an anonymous request, distinct from a person's stored default organisation
(ADR-0057); it selects configuration and never auto-enters an organisation.

**Consequences.** Paths no longer contain `{organisationId}`; the shapes in
ADR-0060 and ADR-0061 become `/api/{component}/…` and `/api/documents`. Group A
requests bypass tenant resolution and address the global schema explicitly. The
sign-in-methods call may omit the header, in which case the default organisation's
configuration is used. Every group B and C endpoint, the generated client and the
Angular HTTP interceptor must send the header, bound from the selected
organisation. A missing or invalid header on B/C is a `400` problem-details. The
header is untrusted and never sufficient on its own. ADR-0029's entry point
becomes `POST /api/processes/{processId}/instances`. The behaviour of the default
organisation on a subsequent boot — whether the value must be supplied, may be
inferred from the database, and how a changed value is treated — is an open
question. Supersedes ADR-0048; refines ADR-0047, ADR-0049, ADR-0052, ADR-0055,
ADR-0060 and ADR-0061.

---

### ADR-0065 — The Control Room signs in with OIDC code flow and an encrypted session cookie

**Status:** Accepted

**Decision.** The Control Room uses the Quarkus OIDC **authorization code flow**
in **hybrid** mode. The frontend login page navigates the whole browser to
`GET /api/auth/login?provider=<provider uuid>&returnUrl=<path>`. The login
resource does not interpret the provider; the OIDC tenant resolver does. Quarkus
handles the callback at `/api/auth/callback` and returns an encrypted, HttpOnly
session cookie (`q_session`) that Quarkus uses as the security identity on later
requests. Subsequent browser calls are authenticated from that cookie, never from
a token held in JavaScript. Logout at `/api/auth/logout` is a local logout that
clears the session cookie; provider-initiated logout remains a separate decision.

**Reason.** The login page belongs to the frontend and must choose among providers
whose configuration varies by organisation, so the backend cannot decide the
provider from a single static configuration. Performing the code exchange in
Quarkus keeps the OIDC tokens out of the browser, gives the backend a normal
`SecurityIdentity`, and lets the same credential work for browser and machine
callers. Hybrid mode keeps bearer-token authentication available for a machine
caller without exposing the OIDC token to the browser.

**Consequences.** The post-login redirect is same-origin in production and falls
back to `/`; an off-site redirect is never allowed there. Development runs the
Quinoa frontend and the Quarkus backend on different ports, so a dev-only
mechanism is required; its form is not decided. An unauthenticated request under
`/api/` other than the login entry point returns `401` rather than a provider
redirect. Provider configuration resolution and the effect of the default
organisation on schema isolation remain open questions (below). Refines ADR-0047
and ADR-0055; relates to ADR-0052 and ADR-0064.

---

## Open questions

These are deliberately **not** decided. Do not invent an answer; raise an ADR
when one is settled.

- **The dev-mode login redirect.** Production keeps the post-login redirect
  same-origin. Development serves the Quinoa frontend and the Quarkus backend on
  different ports, so a dev-only mechanism is needed; whether it is CORS,
  configuration or something else is not decided.
- **OIDC provider configuration resolution.** The tenant resolver must load the
  provider configuration from the organisation's configuration and then from the
  default organisation's configuration. How that is expressed in the database is
  not designed, and the resolution must neither silently forward to the default
  organisation nor leak organisation identifiers.
- **The default organisation and schema isolation.** Each organisation is intended
  to have its own schema (ADR-0052), but the effect and implications of the default
  organisation must be evaluated before that isolation is implemented.
- Where environment **classification** lives precisely, and how far the delivery
  guard reaches.
- The exact home of **process settings** and the **process definition screen**.
- Whether a process override may **tighten** as well as widen the organisation
  default.
- The **process-loop** construct; the **embedded-subprocess** utility; the
  **script language and its signature**; the **agent specification**; and the
  **process-to-task data mapping** in both directions.
- The **identity lifecycle** (whether disable precedes delete), the
  **local-to-enterprise** identity transition, and the **unmanaged team** state.
- The **resolution rule** when session state and the default organisation
  ordering disagree.
- Whether the **global sign-in page** ever lists organisations *before* sign-in.
- **The default organisation across restarts in multi-organisation mode.** On
  first boot the default organisation identifier is boot-time configuration and,
  if absent, is assigned a random identifier. It is undecided whether a subsequent
  boot must always supply it or may infer it from the database, and whether
  supplying a different value means the default has deliberately changed or is a
  misconfiguration. See [`API-AND-DATABASE.md`](./API-AND-DATABASE.md) §6.
- The **"create a new organisation"** flow's own journey.
- The **process owner's** first journey: whether analytics (Activity) is the
  primary destination, or the owner stays on Home and asks for performance there.

The following were raised by the API and database workstream (section H) and are
deliberately not decided. See [`API-AND-DATABASE.md`](./API-AND-DATABASE.md) §6.

- The **state sub-resource's payload and legal transitions** (`PUT …/state`), and
  whether the assignment and referral operations (`POST …/assign`) overlap with
  it — an assignment changes the state map.
- Whether **`data`** is overwritten with `PUT`, updated with `PATCH`, or broken
  into finer assignments.
- The **task-detail query parameters** that control depth (history, data schema,
  current data, summary, process model).
- Whether the **summary** is templated on the server or in the browser, and how
  the template (process design) is kept apart from the data (task view).
- **Document** storage, streaming and permission rules (ADR-0061).
- The **list contract** — server-side filtering, sorting, the pagination model,
  and how the Angular "whole list, once" convention maps onto it.
- The **`control-room` ↔ `process-runner` boundary** and specifically **where the
  per-step execution history** lives, with the database-contention and scale
  consequences.
- **How much of the JSON Process** is normalised into tables versus stored as a
  document.
- **Dynamic (process-defined) parameters** — the arguments to start a process
  vary by process definition; how they are described in the OpenAPI document and
  validated at the boundary is deferred.
- The **session-credential lifetime** and the matching authorisation-cache
  time-to-live.
- The **system and agent grant model** — whether global, org-scoped grants exist
  as a separate grant axis, and whether they sit on the user profile.
- The **schema-change compatibility window** for rolling upgrades (ADR-0058).
- The **versioning scheme** — per API group, or across the whole interface.
- **Global-schema retention** — remove an unused authentication profile after a
  window, or keep it in perpetuity.
