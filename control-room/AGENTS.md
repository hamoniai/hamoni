# AGENTS.md — `control-room` (holding directory)

This directory holds the **projects for the user-facing application** — the
Control Room. It is a holding area, not a project itself. Read the
repository-level [`../AGENTS.md`](../AGENTS.md),
[`../CODING-STANDARDS.md`](../CODING-STANDARDS.md),
[`../ARCHITECTURE-DECISIONS.md`](../ARCHITECTURE-DECISIONS.md) and
[`../API-AND-DATABASE.md`](../API-AND-DATABASE.md) first; this file only adds what
is specific to the projects here.

## What this directory holds

Projects for everything the user sees and drives: the **process designer**, the
operational **lists** (Inbox, Processes), **Teams**, **Projects** and their
security, the **Home** prompt-first surface, dashboards, and the organisation and
preference surfaces. There are two projects today:

- `control-room/control-room` — the Kotlin · Quarkus backend assembly (Gradle
  `:control-room`), which serves the front end through Quinoa.
- `control-room/control-room-ui` — the Angular front end. It is outside backend
  scope: never read or edit it.

## Responsibilities (of the projects here)

- The **design-time model**: drafting, validating and publishing process
  definitions (the simple flowchart that compiles to JSON Process, ADR-0016).
- **Management** operations: teams, projects, versions, deployments and security
  (ADR-0008, ADR-0010, ADR-0024).
- The **read models** the screens consume: lists, filters, summaries, audit.
- The **interface** consumed by the front end.
- Tenant and preference handling, including the single advanced-mode setting
  (ADR-0028).

## Boundaries — what these projects must NOT do

- **Never execute a process.** Starting, advancing, pending, resuming, cancelling
  and completing a running case belong to the projects under `process-runner/`.
  These projects issue commands and render the results.
- **Do not own the runtime state machine.** The process and activity state models
  live in `common/` and are executed by `process-runner/` (ADR-0012, ADR-0013).
- **Do not depend on a project under `process-runner/`.** Talk to it over its
  interface; both application areas depend only on `common/`.
- **Never bypass the security model.** Permission is resolved from the effective
  mode (ADR-0024); do not invent per-screen checks.

## Layout

The backend projects follow the same convention as `common/`: a project named
`control-room-<name>` maps to the package `ai/hamoni/control_room/<name>`. The
assembly project `control-room` maps to `ai/hamoni/control_room` itself.

```
control-room/
  AGENTS.md
  <project>/                        e.g. control-room, control-room-<name>
    build.gradle.kts
    README.md
    src/main/docker/
      Dockerfile.native             native-image build
    src/main/kotlin/ai/hamoni/control_room/<name>/   omit <name> for the assembly project
      data/                         entities and data objects
      processor/                    Quarkus Reactive Messaging endpoints
      service/                      service beans
      resource/                     REST API endpoints
    src/main/resources/
      application.properties
    src/test/kotlin/ai/hamoni/control_room/<name>/
  control-room-ui/                  Angular front end (separate agent; do not read or edit)
```

The assembly project carries application-wide code at the area root (such as
`ModeOverrides.kt`) alongside the standard packages. Create only the packages a
project actually uses.

### Files and classes

Classes are grouped by **package**, not one class per file. Within a standard
package, either:

- put all of the package's classes in one file when they are cohesive — for
  example `data/Objects.kt`; or
- split them into files grouped by related area — for example
  `resource/AuthResources.kt`.

A Kotlin file may contain several related classes, and that grouping is the
preferred default for ease of reading and maintenance. Split a file only when it
grows unwieldy or its classes stop belonging together.

Keep the resource thin (CODING-STANDARDS §11) and business logic in the service.

## Bootstrapping

The backend project is a Gradle project. Build and run it by name:

```bash
./gradlew :<project>:test             # this project's tests
./gradlew :<project>:quarkusDev       # dev mode, live reload (serves control-room-ui)
./gradlew :<project>:quarkusBuild     # build the runnable artefact
```

The backend applies the shared Hamoni Gradle convention plugin; do not add build
logic locally.

## Interfaces with the rest of the platform

- **→ `common/`**: the domain model, the JSON Process model and the state models.
- **→ `process-runner/`**: start, advance, pend, resume and cancel commands, and
  instance read models, over its interface. A published, deployed process
  definition is the artefact handed across this boundary.
- **→ the interface and the database** — the endpoints these projects expose and
  the database rules they follow are defined in
  [`../API-AND-DATABASE.md`](../API-AND-DATABASE.md).

## Before you finish

- The affected projects build with lint and tests.
- Every new screen or write path respects the reviewable-object rule (ADR-0027)
  and the advanced-mode gate (ADR-0028).
- New behaviour traces to an Architecture Decision Record.
