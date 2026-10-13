# AGENTS.md — `process-runner` (holding directory)

This directory holds the **projects for the execution runtime** — the Process
Runner. It is a holding area, not a project itself. Read the repository-level
[`../AGENTS.md`](../AGENTS.md), [`../CODING-STANDARDS.md`](../CODING-STANDARDS.md)
and [`../ARCHITECTURE-DECISIONS.md`](../ARCHITECTURE-DECISIONS.md) first; this
file only adds what is specific to the projects here.

## What this directory holds

Projects for the runtime that **executes process definitions**: given a published
process definition, it starts instances, advances activities, handles junctions
and signals, manages pend and exception, enforces timers and the service-level
agreement clock, and records the audit trail. The projects are Kotlin · Quarkus,
and their number and split are decided area by area.

## Responsibilities (of the projects here)

- The **process engine**: interpreting a JSON Process definition and running it
  end to end (ADR-0016).
- The **process state machine** (Active · Pend · Exception · Paused · Completed ·
  Cancelled) and the **activity state machine** (Available · Assigned · Active ·
  Completed, plus Cancelled) at runtime (ADR-0012, ADR-0013).
- **Pend and exception** semantics, including the clock rule and resume methods
  (ADR-0014, ADR-0015).
- **Signals** and timer handling (ADR-0020, ADR-0030).
- **Persistence of instances**, their state data, and the **audit trail**
  (ADR-0026).
- The **start and lifecycle interface** — `POST /processes/{processId}/instances`
  (ADR-0029) and the transitions the Control Room issues.
- **Durability**: a run survives a restart; no in-memory-only state that loses
  work.

## Boundaries — what these projects must NOT do

- **Own user-facing configuration.** Teams, projects, security, preferences and
  the designer belong to the projects under `control-room/`.
- **Own the design-time schema.** The JSON Process model and schema live in
  `common/`; these projects consume and validate against them, they do not define
  them.
- **Depend on a project under `control-room/`.** Both application areas depend
  only on `common/`.
- **Interpret presentation.** Layout and anything presentation-namespaced are
  ignored by execution (ADR-0016, ADR-0017).

## Layout

```
process-runner/
  AGENTS.md
  <project>/
    src/main/kotlin/ai/hamoni/<area>/
      engine/               the execution interpreter
      state/                process + activity state machines
      pend/                 pend and exception handling
      signals/              signals, timers
      api/                  start + lifecycle endpoints
      persistence/          instance, state-data and audit repositories
    src/main/resources/
      application.properties
    src/test/kotlin/ai/hamoni/<area>/
```

## Bootstrapping

Each project is its own Gradle project. Build and run it by name:

```bash
./gradlew :<project>:test             # this project's tests
./gradlew :<project>:quarkusDev       # dev mode, live reload
./gradlew :<project>:quarkusBuild     # build the runnable artefact
```

Each project applies the shared Hamoni Gradle convention plugin; do not add build
logic locally.

## Testing these projects hard

Execution is the platform's highest-risk area. Every state transition must have a
test (CODING-STANDARDS §16). In particular:

- A pend **suspends all parallel paths** without interrupting running activities;
  only the resume method resumes (ADR-0014).
- An exception **never stops the clock** (ADR-0015).
- A process completes when **no executable token remains in any component** and
  it is not suspended — a finished pend handler does not resume the main process
  (ADR-0018).
- A junction performs **no input or output and no waiting** (ADR-0019).
- An inbound signal **never creates an instance** (ADR-0020).
- State-data validation at the boundary raises a mapped exception on mismatch
  (ADR-0017).

## Interfaces with the rest of the platform

- **→ `common/`**: the JSON Process model, the state models and shared contracts.
- **← `control-room/`**: commands (start, advance, pend, resume, cancel) and the
  published, deployed process definition. These projects execute; they do not
  decide who may.

## Before you finish

- The affected projects build with lint and tests.
- New execution behaviour is covered by tests for every affected transition.
- A runtime decision that changes the model is captured as an Architecture
  Decision Record.
