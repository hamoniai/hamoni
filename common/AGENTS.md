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
These are libraries, not applications; they have no runtime of their own, and
their number and split are decided area by area.

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
- **Hold runtime behaviour.** No scheduler, no engine, no HTTP resource, no
  user-facing configuration. If it runs something, it belongs in an application.
- **Grow heavy.** A type belongs here only when **both** application areas need
  it and it brings no meaningful transitive dependency. A shared library that
  pulls in a framework is a defect (CODING-STANDARDS §2 and §3).
- **Leak persistence or transport types.** Entities and transport types do not
  cross this boundary; map at the edge.

## Layout

```
common/
  AGENTS.md
  <project>/
    src/main/kotlin/ai/hamoni/common/
      model/            domain value objects and aggregates
      process/          the JSON Process model + schema
      state/            process + activity state models
      contract/         shared events, commands and transport types
      util/             clock, identifiers, result types
    src/main/resources/
      schema/           the JSON Process JSON Schema (source of truth)
    src/test/kotlin/ai/hamoni/common/
```

## Bootstrapping

Each project is its own Gradle project. Build and test it by name:

```bash
./gradlew :<project>:build     # compile, lint and test the libraries
./gradlew :<project>:test      # tests only
```

These are plain Kotlin libraries (not Quarkus applications). Apply the shared
convention plugin; add **no** runtime dependency unless an ADR justifies it.

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
