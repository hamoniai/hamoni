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

**Status:** Accepted

**Decision.** Hamoni never stores a password. Every route authenticates through
an external identity provider using **OpenID Connect (OIDC)**. Identity is the
provider's subject claim — **not** the email address, which can change. The
**route decides the tenant**: a tenant-specific route lands in that tenant; the
public route lands in the first tenant alphabetically; the person may switch
tenant in-session.

**Reason.** Storing passwords is a liability with no upside. Authenticating every
route through an external provider removes that liability, and using the subject
claim rather than the email gives a stable key for a person whose address may
change. Deciding the tenant by the route lets a company offer a single link
without presenting a chooser.

**Consequences.** The profile surface shows the name **and** the email so a
person can tell which account they are on. There is no local credential store to
secure.

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

### ADR-0025 — Multi-tenancy with in-session tenant switching

**Status:** Accepted

**Decision.** The organisation (tenant) is switchable in-session from the
identity menu; there is no sign-in gate on the switch. Tenant context is carried
through every request and enforced at the data layer.

**Reason.** People move between organisations without signing out, so switching
should not force a round trip through sign-in. Carrying tenant context through
every request and enforcing it at the data layer is what keeps organisations
isolated when one deployment serves many.

**Consequences.** Every tenant-scoped query must carry the tenant context; a
missing tenant filter is a defect.

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

**Status:** Accepted

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

## Open questions

These are deliberately **not** decided. Do not invent an answer; raise an ADR
when one is settled.

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
