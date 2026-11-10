# AGENTS.md — `extension` (holding directory)

This directory holds the **Quarkus extensions** the platform is built on. It is a
holding area, not a project itself, and it sits **outside** the three project
areas — `control-room/`, `process-runner/` and `common/`. Read the repository-level
[`../AGENTS.md`](../AGENTS.md), [`../CODING-STANDARDS.md`](../CODING-STANDARDS.md)
and [`../ARCHITECTURE-DECISIONS.md`](../ARCHITECTURE-DECISIONS.md) first; this file
only adds what is specific to the extension projects here.

## What this directory holds

Quarkus extensions that supply **platform setup or a capability** Hamoni needs
from Quarkus itself — for example build-time instrumentation, a runtime recorder,
or configuration the application projects cannot express themselves. An extension
is **platform plumbing, not application code**: it carries no Hamoni domain logic
and no user-facing configuration.

Extensions are not a fourth application or shared area. Work still belongs in
`control-room/`, `process-runner/` or `common/`; an extension is created only when
the requirement is specifically to extend Quarkus (ADR-0068).

## The extension pair

An extension is always **two related Gradle projects**:

- **`<extension name>`** — the **runtime** project.
- **`<extension name>-deployment`** — the **deployment** project, which depends
  on the runtime project.

The extension name has two parts:

```
<extension group>-<extension capability>
```

- The **extension group** cannot contain hyphens.
- The **extension capability** may contain hyphens.

For example `quarkus-multi-tenant` has the group `quarkus` and the capability
`multi-tenant`; its runtime project is `quarkus-multi-tenant` and its deployment
project is `quarkus-multi-tenant-deployment`.

Both projects apply the shared **`quarkus-extension-conventions`** Gradle plugin.
The build discovers the pair from the `<extension name>-deployment` project, so do
not register the projects by hand in `settings.gradle.kts`, and do not copy build
logic into a project.

## Package and source layout

The base package is always:

```
ai.hamoni.quarkus.ext.<extension group>.<extension capability>
```

Hyphens in the extension group and the extension capability become underscores.
For `quarkus-multi-tenant` the base package is
`ai.hamoni.quarkus.ext.quarkus.multi_tenant`.

### Runtime project — `<extension name>`

```
<extension name>/
  build.gradle.kts
  src/main/kotlin/ai/hamoni/quarkus/ext/<group>/<capability>/
    Recorders.kt            all of the extension's Quarkus recorders
    Config.kt               all of the extension's configuration parameters
    runtime/                the runtime classes
  src/main/resources/META-INF/quarkus-extension.yaml
```

- **`Recorders.kt`** in the base package holds **all** of the extension's
  recorders. Recorders form Quarkus's bytecode-recording boundary; keep them
  together in this one file.
- **`Config.kt`** in the base package holds **all** of the extension's
  configuration parameters.
- The **`runtime`** sub-package, directly under the base package, holds the
  runtime classes.
- **`src/main/resources/META-INF/quarkus-extension.yaml`** must exist and
  describe the extension metadata (name, short name, keywords, guide, categories
  and status as applicable). It is what makes the project a Quarkus extension.

### Deployment project — `<extension name>-deployment`

```
<extension name>-deployment/
  build.gradle.kts
  src/main/kotlin/ai/hamoni/quarkus/ext/<group>/<capability>/deployment/
    Processors.kt           all of the extension's build-time processors
  src/test/kotlin/ai/hamoni/quarkus/ext/<group>/<capability>/test/
```

- **All main-source classes** in the deployment project live inside the
  **`deployment`** sub-package of the base package.
- **`Processors.kt`** holds **all** of the extension's Quarkus build steps and
  processors. Every processor class ends with the suffix **`Processor`**.
- Other build-time helpers — class visitors, transformers and so on — stay in the
  same `deployment` sub-package. Group related classes in one file as the rest of
  the platform does (CODING-STANDARDS §2); do not create one file per class.

## Tests

**All tests for both projects live in the deployment project**, under
`src/test/kotlin`, in the **`test`** sub-package of the base package:

```
src/test/kotlin/ai/hamoni/quarkus/ext/<group>/<capability>/test/
```

- The runtime project has no tests of its own. Test the runtime and the
  build-time behaviour from the deployment project.
- This is a deliberate deviation from the "test class lives in the same package
  as the class under test" rule (CODING-STANDARDS §16.2): a Quarkus extension
  pair has one deployment module that carries the tests for both halves.
- A test class is named after the class or source file under test, and tests
  follow Given-When-Then (CODING-STANDARDS §16.2, §16.6).
- Quarkus extension tests use the Quarkus test harness — `QuarkusUnitTest` or
  `QuarkusProdModeTest` — in the deployment project.
- Deterministic tests only: no reliance on the wall clock and no sleeps; inject a
  clock where time matters (CODING-STANDARDS §16.6).

## Bootstrapping

Both projects are Gradle projects owned by the extension pair. Build and test them
by name:

```bash
./gradlew :extension:<extension name>:build              # compile the runtime project
./gradlew :extension:<extension name>-deployment:build   # compile and test the pair
./gradlew :extension:<extension name>-deployment:test    # all tests for the pair
```

The deployment project depends on the runtime project, so building the
deployment project builds both. Do not add build logic locally; consume the
shared convention plugin.

## Boundaries — what an extension must NOT do

- **Contain Hamoni domain logic or user-facing configuration.** Those belong to
  the application and shared areas.
- **Depend on a project under `control-room/`, `process-runner/` or `common/`.**
  The extension supplies a Quarkus capability to the applications; the dependency
  does not run the other way.
- **Replace a platform decision recorded in `ARCHITECTURE-DECISIONS.md`.** If an
  extension changes how the platform uses Quarkus, record the decision first.

## Adding an extension

1. Decide whether the requirement is genuinely a Quarkus platform setup or
   capability. If it is Hamoni domain work, it belongs in `control-room/`,
   `process-runner/` or `common/`.
2. Create the runtime `<extension name>` project and the deployment
   `<extension name>-deployment` project with the shared extension conventions.
3. Apply the naming, package and file layout above.
4. Add `META-INF/quarkus-extension.yaml` to the runtime project.
5. Put every test in the deployment project's `test` sub-package.
6. Build and test the pair.

## Before you finish

- Both projects build, lint and test; every test lives in the deployment project.
- The runtime project carries `Recorders.kt`, `Config.kt`, the `runtime`
  sub-package and `META-INF/quarkus-extension.yaml`.
- Every processor is in `Processors.kt`, and every processor class ends with
  `Processor`.
- No Hamoni domain logic or application dependency has leaked into the extension.
