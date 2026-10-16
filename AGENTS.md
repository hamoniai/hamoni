# AGENTS.md — the Hamoni platform (parent project)

This file is the entry point for any agent (or person) working in this
repository. Read it, then read the `AGENTS.md` of the directory you are about to
touch. The two governing documents are
[`CODING-STANDARDS.md`](./CODING-STANDARDS.md) and
[`ARCHITECTURE-DECISIONS.md`](./ARCHITECTURE-DECISIONS.md); read both before
writing code.

## What this is

Hamoni is a cloud workflow platform: an Operations-first product where teams
bring AI into their business processes as an actively contributing participant.
The audience is **business people, not technologists** — the product is easy by
default and exposes complexity only behind **advanced mode**. See
[`README.md`](./README.md) for the product summary and the list of acronyms.

## Repository layout

```
project/
├── README.md                  product summary, build/test, acronyms
├── AGENTS.md                  ← you are here
├── CODING-STANDARDS.md        Common · Backend · Frontend standards
├── ARCHITECTURE-DECISIONS.md  Architecture Decision Records, with reasoning
├── API-AND-DATABASE.md        frontend–backend interface + database rules
├── control-room/AGENTS.md     holding directory: user-facing projects
├── process-runner/AGENTS.md   holding directory: execution-runtime projects
├── common/AGENTS.md           holding directory: shared projects
└── docs/                      Antora component (AsciiDoc)
```

## Directory map

The three directories are **holding areas**: each holds one or more build
projects, not a project itself. The projects inside are named area by area.

| Directory | Holds | Never holds |
| --- | --- | --- |
| `control-room/` | The user-facing projects: the process designer, management, lists, teams, projects, security. | The process engine, or the runtime state machines. |
| `process-runner/` | The runtime projects that execute a published process definition end to end. | User-facing configuration, or the design-time process schema. |
| `common/` | Models, contracts and utilities genuinely shared by both application areas. | Runtime behaviour, or a dependency on either application area. |

The dependency direction is **one way**: projects under `control-room/` and
`process-runner/` may depend on projects under `common/`. The two application
areas never depend on each other.

## Bootstrapping

Use the Gradle wrapper; never a system `gradle`.

```bash
./gradlew build          # compile + test everything
./gradlew test           # tests only
./gradlew projects       # show the projects in the build
```

Run an application in development mode:

```bash
./gradlew :control-room:quarkusDev        # Control Room (UI included)
./gradlew :process-runner:quarkusDev      # Process Runner
```

The build is driven by the shared **Hamoni Gradle convention scripts** (installed
separately). Consume them through the version catalog and convention plugins; do
not copy build logic into a project.

## Where to find things

- **Architecture decisions** — [`ARCHITECTURE-DECISIONS.md`](./ARCHITECTURE-DECISIONS.md),
  each record stating its own reasoning. This is the only architecture reference
  the build needs.
- **How to write the code** — [`CODING-STANDARDS.md`](./CODING-STANDARDS.md).
- **The interface and the database rules** —
  [`API-AND-DATABASE.md`](./API-AND-DATABASE.md): the frontend–backend contract
  and the rules that govern the database. Read it before adding an endpoint or a
  table.
- **A directory's local rules** — that directory's `AGENTS.md`.
- **Long-form documentation** — `docs/` (Antora).

## Working rules

1. **Read before you build.** Read this file, the target directory's `AGENTS.md`,
   `CODING-STANDARDS.md` and the relevant decisions first.
2. **Write plain language in user-facing copy.** Do not use acronyms or
   abbreviations in copy shown to a user, and spell out anything unavoidable.
   See [`CODING-STANDARDS.md`](./CODING-STANDARDS.md) §18.2 and the
   [README acronyms](./README.md#acronyms).
3. **Kotlin is the primary language.** Java only when a bug or a restriction
   prevents Kotlin (§1.1). Target Java 25.
4. **Reactive by default.** All backend libraries and dependencies must be
   reactive (ADR-0032).
5. **Centralise dependencies.** Every library lives in `gradle/libs.versions.toml`;
   a new library needs leadership approval (ADR-0002).
6. **Audit with the framework.** Use the Hamoni audit framework; never a custom
   audit handler or ad-hoc logging (ADR-0026).
7. **Do not re-litigate settled decisions.** Decisions are recorded in
   [`ARCHITECTURE-DECISIONS.md`](./ARCHITECTURE-DECISIONS.md). Implement an
   *Accepted* record; if you believe it is wrong, raise a new record rather than
   diverging silently.
8. **Record new decisions.** When you make a technical decision, add or update a
   record in the same change.
9. **Respect the boundaries.** A directory holds only its own projects (see the
   directory map).
10. **Never commit secrets.** No credentials, keys or tokens in the tree or logs.
11. **Advanced mode.** Anything complex sits behind the single application-wide
    advanced-mode setting — never a per-screen toggle (ADR-0028).
12. **Design tokens and tone.** In any user-facing work, bind the *Design System
    for Hamoni* tokens and use the product's voice — see
    [`CODING-STANDARDS.md`](./CODING-STANDARDS.md) §18.

## Before you finish

- `./gradlew build` passes, and every project you touched is tested.
- New or changed decisions are recorded.
- No secrets, no dead code, no commented-out blocks, no TODO left without a
  tracked note.
