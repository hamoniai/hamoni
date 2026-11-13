# Build specs and implementation approaches

The **plan team** writes feature specifications here before the Build team builds
them. A spec and its implementation approach are the hand-off: the spec says *what*
to build, the approach says *how*.

## The files

- A **build spec** is `specs/NNNN-<Title>-spec.md`, written from
  [`SPEC-TEMPLATE.md`](./SPEC-TEMPLATE.md).
- Its **implementation approach** is `specs/NNNN-<Title>-impl.md`, written from
  [`IMPL-TEMPLATE.md`](./IMPL-TEMPLATE.md) — same stem, `-impl` instead of `-spec`.
- Its **screen designs**, for a UI feature, are
  `specs/NNNN-<Title>-screens/<screen name>.html` — one self-contained page per
  screen.

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

## The screens

The **technical business analyst always starts the elicitation** — even from a
one-line concept. Whether the feature needs screens may only become clear as the
requirement is understood. When it does, the business analyst hands back to the
plan lead (or hands over directly) with a **screen brief**: what is being built,
the personas and flows, the candidate screens and states, the constraints and the
open questions.

The **plan lead decides whether the UI/UX designer is involved**. A purely backend
feature has **none**, and the designer is not involved. Otherwise the **UI/UX
designer** produces reviewable screen designs at
`specs/NNNN-<Title>-screens/<screen name>.html` — one file per screen, named for
the screen (`team-list.html`, `team-detail.html`). They reference the shared CSS
and JS in `screens/` (`../../screens/assets/app.css`, `app.js`,
`sample-data.js`) and use the components on the component board at
`control-room/control-room-ui/src/app/pages/home/components.html`.

The designer hands back a **screen summary**: each screen created and its purpose,
the states and components, and any update to the requirements, assumptions,
specification or implementation approach that became evident during the design.
The plan lead records that summary in the spec's **screens** section, and the
technical planner references it in the implementation approach. The screens must
be **complete and reviewed before** the implementation approach is written.

## Who writes what

- **All plan subagents may update the build spec.** The **plan lead** confirms it
  meets the quality bar and sets it to *Approved* **on the user's instruction**;
  the approver recorded is the user's **git identity** (`git config user.name` /
  `user.email`).
- **The UI/UX designer produces the screens** under
  `specs/NNNN-<Title>-screens/` when the plan lead decides the feature needs
  them, and hands back the **screen summary** for the spec and the approach.
- **Only the technical planner updates the implementation approach**, and only
  after the screens are complete for a UI feature.
- The **solution architect** may also update `ARCHITECTURE-DECISIONS.md`,
  `API-AND-DATABASE.md` and `CODING-STANDARDS.md` when a rule genuinely changes.
  The coding standards may be updated **independently** — for example, in
  response to a new pattern. The **Build team** also updates them in a separate
  workflow, usually on the user's instruction when a pattern they dislike appears.
- **The plan team never edits code.**

## Design references

The build project is **not** the design project. The design project's
**`design-decisions.md` is imported as a read-only handover snapshot** at
[`DESIGN-DECISIONS.md`](../DESIGN-DECISIONS.md); the design project remains the
source of truth, so do not edit it here. The **`screens/`** directory is synced
into the implementation project at its root, so it *is* available: it is the UI
reference for a UX feature. `screens/controls.html` is the design-language and
component reference, and `screens/assets/app.css` is the token source.

The UI/UX designer works **within** this language: compose from the components on
the component board, and if a feature needs a component that is not there, stop
and ask — it may need designing first. If a UX requirement depends on a design
decision that is not available, stop and ask rather than assume it.

## The build hand-off

The Build team receives the standards (`AGENTS.md`, `CODING-STANDARDS.md`,
`ARCHITECTURE-DECISIONS.md`, `API-AND-DATABASE.md`) plus this spec, its screens
(for a UI feature) and its implementation approach. It is started with a prompt
like:

> I want you to implement feature 0001, …

`NNNN` is the feature's ID; the spec and approach are the two files for it.

## What a spec must contain

Every section of `SPEC-TEMPLATE.md` is filled, or explicitly recorded as a risk:

1. creation and approval date/time;
2. a feature summary;
3. the spec state;
4. a functional description, including the **personas** the feature targets and
   whether it is UI-facing with the screens it needs (or **none: backend only**);
5. **start and end states**;
6. **architectural changes** — software structure (which files change and what to
   do), data models, state flows and API contracts;
7. **screens** — each screen the UI/UX designer produced, its file and purpose,
   with the designer's summary and any requirement or assumption it changed (or
   **none** for a backend-only feature);
8. **functional and non-functional requirements**, written with **RFC 2119**
   keywords (`MUST`, `MUST NOT`, `SHOULD`, `SHOULD NOT`, `MAY`, …);
9. **pre- and post-conditions**;
10. **security** — which roles may access or perform what;
11. **testing scenarios** — both positive and negative;
12. **risks, edge cases and assumptions** — anything that cannot be evaluated now;
13. **exceptions to the architecture** — accepted deviations, with the reason and
    the user's decision.

The spec references the standards it obeys: `CODING-STANDARDS.md`,
`ARCHITECTURE-DECISIONS.md` and `API-AND-DATABASE.md`.
