# Build specs and implementation approaches

The **plan team** writes feature specifications here before the Build team builds
them. A spec and its implementation approach are the hand-off: the spec says *what*
to build, the approach says *how*.

## The files

- A **build spec** is `specs/NNNN-<Title>-spec.md`, written from
  [`SPEC-TEMPLATE.md`](./SPEC-TEMPLATE.md).
- Its **implementation approach** is `specs/NNNN-<Title>-impl.md`, written from
  [`IMPL-TEMPLATE.md`](./IMPL-TEMPLATE.md) — same stem, `-impl` instead of `-spec`.

`NNNN` is the next **four-digit** number in a global sequence, and `<Title>` is the
feature's name. Spaces are welcome — a human-readable filename is easier to scan
than kebab-case. For example: `0001-New team creation-spec.md` and
`0001-New team creation-impl.md`. The feature is referred to as **feature 0001**.

## The spec lifecycle

| State | Meaning |
| --- | --- |
| **Draft** | Being written; requirements still moving. |
| **In review** | The solution architect is challenging it; the user is answering open questions. |
| **Approved** | Signed off by the user; carries the approval date, time and the approver's git identity. |
| **In build** | Handed to the Build team. The soft gate: changes are discouraged, not forbidden. |
| **Re-opened** | Explicitly re-opened on instruction; the reason and the date are recorded. |
| **Superseded** | A later spec has replaced part of it. Kept for the record. |

**The gate is soft.** Once approved and handed to build, a spec is changed only on
explicit instruction; re-opening sets the state to **Re-opened** and records the
reason. A later feature still gets its own new spec, and the older one keeps its
text.

## Who writes what

- **All plan subagents may update the build spec.** The **plan lead** confirms it
  meets the quality bar and sets it to *Approved* **on the user's instruction**;
  the approver recorded is the user's **git identity** (`git config user.name` /
  `user.email`).
- **Only the technical planner updates the implementation approach.**
- The **solution architect** may also update `ARCHITECTURE-DECISIONS.md`,
  `API-AND-DATABASE.md` and `CODING-STANDARDS.md` when a rule genuinely changes.
  The coding standards may be updated **independently** — for example, in
  response to a new pattern. The **Build team** also updates them in a separate
  workflow, usually on the user's instruction when a pattern they dislike appears.
- **The plan team never edits code.**

## Design references

The build project is **not** the design project. **`design-decisions.md` stays in
the design project and is not here.** The **`screens/`** directory is synced into
the implementation project at its root, so it *is* available: it is the UI
reference for a UX feature. If a UX requirement depends on a design decision that
is not available, stop and ask rather than assume it.

## The build hand-off

The Build team receives the standards (`AGENTS.md`, `CODING-STANDARDS.md`,
`ARCHITECTURE-DECISIONS.md`, `API-AND-DATABASE.md`) plus this spec and its
implementation approach. It is started with a prompt like:

> I want you to implement feature 0001, …

`NNNN` is the feature's ID; the spec and approach are the two files for it.

## What a spec must contain

Every section of `SPEC-TEMPLATE.md` is filled, or explicitly recorded as a risk:

1. creation and approval date/time;
2. a feature summary;
3. the spec state;
4. a functional description, including the **personas** the feature targets;
5. **start and end states**;
6. **architectural changes** — software structure (which files change and what to
   do), data models, state flows and API contracts;
7. **functional and non-functional requirements**, written with **RFC 2119**
   keywords (`MUST`, `MUST NOT`, `SHOULD`, `SHOULD NOT`, `MAY`, …);
8. **pre- and post-conditions**;
9. **security** — which roles may access or perform what;
10. **testing scenarios** — both positive and negative;
11. **risks, edge cases and assumptions** — anything that cannot be evaluated now;
12. **exceptions to the architecture** — accepted deviations, with the reason and
    the user's decision.

The spec references the standards it obeys: `CODING-STANDARDS.md`,
`ARCHITECTURE-DECISIONS.md` and `API-AND-DATABASE.md`.
