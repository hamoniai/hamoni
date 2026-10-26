# AGENTS.md — `common` (holding directory)

This directory holds the **projects for the shared libraries** used by both
application areas. It is a holding area, not a project itself. Read the
repository-level [`../AGENTS.md`](../AGENTS.md),
[`../CODING-STANDARDS.md`](../CODING-STANDARDS.md) and
[`../ARCHITECTURE-DECISIONS.md`](../ARCHITECTURE-DECISIONS.md) first; this file
only adds what is specific to the projects here.

## What this directory holds

Projects for the Kotlin libraries **genuinely shared** by the projects under
`control-room/` and `process-runner/`: the domain model, the JSON Process model
and schema, the state models, shared contracts and events, and small utilities.
These are libraries, not applications; they have no entry point of their own, and
their number and split are decided area by area.

The projects today are `common-platform` (shared Quarkus platform plumbing),
`common-core` (core concepts used across the components) and `common-test` (test
support for the other projects).

## Responsibilities (of the projects here)

- The **domain types** both application areas agree on — identifiers, value
  objects, the core aggregate types.
- The **JSON Process model** and the **process schema** it validates against
  (ADR-0016, ADR-0017).
- The **process** and **activity** state models as sealed types (ADR-0012,
  ADR-0013), and the pend and exception event types (ADR-0014, ADR-0015).
- Shared **contracts**: events and transport types exchanged over the boundary,
  and shared query and command shapes.
- Small, dependency-free **utilities** (a clock abstraction, identifier types,
  result types).

## Boundaries — what these projects must NOT do

- **Depend on a project under `control-room/` or `process-runner/`.** The
  dependency direction is one-way: both areas depend on `common/`; `common/`
  depends on neither (ADR-0003).
- **Hold application runtime behaviour.** No scheduler, no engine, no
  application entry point, no user-facing configuration. Shared request and
  response plumbing (filters, exception mappers) is the limit of what belongs
  here; a running application lives in `control-room/` or `process-runner/`.
- **Grow heavy.** A type belongs here only when **both** application areas need
  it and it brings no meaningful transitive dependency. A shared library that
  pulls in a framework is a defect (CODING-STANDARDS §2 and §3).
- **Leak persistence or transport types.** Entities and transport types do not
  cross this boundary; map at the edge.

## Layout

A project is named `common-<name>` and maps to the package
`ai/hamoni/common/<name>`. For example, `common-platform` maps to
`ai/hamoni/common/platform` and `common-core` to `ai/hamoni/common/core`.

Every project uses the same package layout beneath that root:

```
common/
  AGENTS.md
  <project>/                        e.g. common-platform, common-core, common-test
    build.gradle.kts
    README.md
    src/main/kotlin/ai/hamoni/common/<name>/
      data/                         entities and data objects
      processor/                    Quarkus Reactive Messaging endpoints
      service/                      service beans
      resource/                     REST API endpoints and shared request/response plumbing
    src/main/java/ai/hamoni/common/<name>/   Java only where a restriction requires it (ADR-0001)
    src/main/resources/
    src/test/kotlin/ai/hamoni/common/<name>/
```

Create only the packages a project needs: a library with no persistence has no
`data` package, and one with no messaging endpoint has no `processor` package.
A fourth area (`configuration`, `exception` and so on) is added only when it has
a clear purpose of its own. Project-wide declarations, such as `Config.kt` and
`Constants.kt`, may sit directly at the package root. Because these are shared
libraries, `resource/` here normally holds shared request and response plumbing
(filters, exception mappers) rather than a user-facing endpoint.

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

## Bootstrapping

Each project is its own Gradle project. Build and test it by name:

```bash
./gradlew :<project>:build     # compile, lint and test the library
./gradlew :<project>:test      # tests only
```

The same commands apply to `:common-core` and `:common-test`.

These are library modules built with the shared Quarkus application conventions,
not runnable applications: they carry no `application.properties` and no start-up
of their own. Add **no** runtime dependency unless an ADR justifies it.

## Testing these projects

- The **schema round-trips**: a valid definition validates; an invalid one is
  rejected with the failing path. State-data validation raises on mismatch
  (ADR-0017).
- **State-model tests**: every legal transition is possible, every illegal one is
  unrepresentable or rejected; `when` over the sealed types is exhaustive.
- **ArchUnit** asserts that these projects import neither application area and
  stay dependency-light (CODING-STANDARDS §16).

## Interfaces with the rest of the platform

- **← `control-room/`** and **← `process-runner/`**: both consume the model, the
  schema, the state types and the contracts defined here. A change here affects
  both — treat the public interface as stable and version it deliberately
  (explicit API mode, CODING-STANDARDS §1.2).

## Before you finish

- The affected projects build with lint and tests.
- Public API carries KDoc and an explicit visibility and return type.
- A change to the shared model or schema is reflected in an Architecture Decision
  Record and considered against **both** consumers.
