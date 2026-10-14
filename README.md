# Hamoni

Hamoni is a cloud workflow platform with an **Operations-first** focus. It lets
teams bring AI directly into their business processes as an **actively
contributing participant** — not as a bolt-on. Our people and our AI are
orchestrated as **one workforce** and built to scale.

The people who use Hamoni are overwhelmingly **business people, not
technologists**. Three rules follow from that and shape the whole build:

1. **80% of things should be easy.** The common path is configured by the team
   itself, without help.
2. **The remaining 20% is advanced mode** — a technical surface for the person
   who needs full control.
3. **In advanced mode the user has full control**, with verification only to
   ensure a change does not break the platform.

## How the code is organised

Work is grouped into three **holding directories** plus the documentation set.
Each directory holds **one or more build projects**, not a project itself.

| Directory | Holds |
| --- | --- |
| [`control-room/`](./control-room/) | The projects for everything **user-facing**: the process designer/builder, management, lists, teams, projects, security. One project may be the user interface and another the data model, for example `control-room/hamoni-cr-ui` and `control-room/hamoni-cr-model`. |
| [`process-runner/`](./process-runner/) | The projects for the **runtime** that executes process definitions — starting, advancing, pending, resuming and completing processes and their tasks. |
| [`common/`](./common/) | The projects for building blocks genuinely **shared** by both — the process model, the state models, shared contracts and utilities. |
| [`docs/`](./docs/) | The documentation set, an **Antora** component. |

```
hamoni/
├── control-room/     projects for the user-facing application
├── process-runner/   projects for the process execution runtime
├── common/           projects for shared libraries
└── docs/             Antora (AsciiDoc)
```

The names of the projects inside each directory are decided area by area; the
examples above are illustrative.

## Build system

- **Gradle** with the **Kotlin DSL**.
- **Kotlin** is the primary language. Java is used only when a bug or a
  restriction prevents Kotlin (for example the logging facade `JBoss Logging`).
- **Quarkus**, in **reactive** mode, is the runtime for the deployable
  components.
- Every library and version is declared once in `gradle/libs.versions.toml`. A
  new library is not added without leadership approval.
- The build is driven by the shared **Hamoni Gradle convention scripts**,
  installed separately. Do not duplicate their logic in a project — consume it.

## Prerequisites

- **Java 25.**
- The Gradle wrapper is committed; use `./gradlew`, never a system `gradle`.
- **Docker** is required for local development services and integration tests.

Exact toolchain and dependency versions are pinned by the shared convention
scripts and the Gradle version catalog. Do not hard-code versions in a project.

## Build and test

```bash
./gradlew build        # compile and test everything
./gradlew test         # tests only
./gradlew projects     # list the projects in the build
```

## Run locally

Run a project from its directory, for example the Control Room's user-interface
project, in development mode (live reload):

```bash
./gradlew :control-room:quarkusDev
```

Development mode enables live reload and the development interface. Development
services start the backing services a project needs automatically when Docker is
available.

## Documentation

Design documentation lives in `docs/`, an **Antora** component authored in
AsciiDoc. The documentation site is built as part of the Gradle build.

## Working standards

These two documents are the contract for anyone — human or agent — who builds
the software. Read them before you write code:

- **[`CODING-STANDARDS.md`](./CODING-STANDARDS.md)** — the technical standards, in
  three parts: Common, Backend and Frontend. A living document.
- **[`ARCHITECTURE-DECISIONS.md`](./ARCHITECTURE-DECISIONS.md)** — the
  Architecture Decision Records (ADRs) the platform is built on, each with its
  reasoning. A living document that will grow into the Antora architecture
  section.

Each directory carries its own `AGENTS.md` with the local rules, layout and
commands for the projects it holds.

## Acronyms

Acronyms specific to the Hamoni project, written out in full. The first letter
of each word in the expanded form is capitalised. Commonly understood industry
acronyms are not listed — spell any acronym out on first use where it appears.

| Short form | Expanded form |
| --- | --- |
| ADR | Architecture Decision Record |
| MSW | Mock Service Worker |
