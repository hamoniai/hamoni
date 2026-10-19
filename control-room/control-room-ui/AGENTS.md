# AGENTS.md — `control-room-ui`

This is the **Angular project for the Control Room's user interface**. It is one
project inside the `control-room/` holding directory. Read the repository-level
[`../../AGENTS.md`](../../AGENTS.md), the directory-level
[`../AGENTS.md`](../AGENTS.md), [`../../CODING-STANDARDS.md`](../../CODING-STANDARDS.md)
(Part III, Frontend), [`../../ARCHITECTURE-DECISIONS.md`](../../ARCHITECTURE-DECISIONS.md)
and [`../../API-AND-DATABASE.md`](../../API-AND-DATABASE.md) — the interface it
calls — first; this file adds only what is specific to this Angular project.

## What this project is

The single-page application a person uses: the process designer, the operational
lists, teams, projects, security, Home and the sign-in surfaces. It is built with
**Angular**, hosted by the backend through **Quinoa** (ADR-0037), and attuned to
the design system through **PrimeNG in styled mode** and **Tailwind**
(ADR-0038). It is a client: it renders, it calls the backend, and it holds no
business rules.

## Responsibilities

- Every user-facing screen and its interaction states (loading, empty, error,
  populated).
- The **typed service layer** that talks to the backend — one service per feature
  (CODING-STANDARDS §20).
- Local and session state, held as **Angular signals**.
- The design system in the browser: the PrimeNG preset, the Tailwind theme and
  the shared components.

## Boundaries — what this project must NOT do

- **No business logic.** Rules, permissions and state transitions are the
  server's; the client reflects them. Never treat a client-side check as the
  gate — the server enforces it (ADR-0041).
- **Never call the HTTP client from a component.** Components call a service;
  only a service makes a request (CODING-STANDARDS §19, §20).
- **No local colour, font or radius overrides.** Bind the design system through
  the generated PrimeNG preset and the Tailwind theme (§21).
- **No raw transport errors crossing the service boundary.** Map them to
  `AppError` (§20.6).
- **No hard-coded, unlocalised copy.** Every user-facing string is localisable
  (§18.3).

## Layout

```
control-room-ui/
  AGENTS.md
  src/
    app/
      <feature>/            a feature: components, its service, its state, its routes
      core/                 app-wide: the auth session, the shell, interceptors
      shared/               reusable components, directives and pipes
    assets/
    styles/                 the Tailwind entry and the generated PrimeNG preset
  package.json
  angular.json
  eslint.config.js
  tsconfig.json
```

Structure by **feature**, mirroring the backend: a feature owns its components,
its service and its state together (CODING-STANDARDS §19).

## Bootstrapping

The frontend is assembled into the `:control-room` project by the shared Gradle
build and Quarkus Quinoa, which is outside the scope of this project.

For focused interface work, run the Angular dev server directly, against a
running backend:

```bash
npm ci            # install the pinned dependencies
npm start         # Angular dev server
npm run build     # production build
npm test          # unit tests
npm run lint      # ESLint
```

Use the project's own package scripts where they differ.

## Angular rules for this project

- **Standalone components** and the current control-flow syntax; do not add an
  NgModule for a new feature.
- **Signals for state.** A service exposes a private writable signal and a public
  read-only view (CODING-STANDARDS §20.5); components read signals, not raw
  subjects.
- **One direction of data.** Inputs down, events up; no two-way binding on shared
  state.
- **Default change detection is `OnPush`.**
- **Typed reactive forms** — model-driven, typed end to end.
- **Lazy-load** a feature's routes.
- **Services follow §20 exactly**: a verb name, optionally a noun (`.approve(id)`,
  `.assignOwner(id, ownerId)`); the five standard names (`list`, `search`, `get`,
  `update`, `create`) for the ordinary operations; every method returns an
  `Observable`; a list is returned whole, once; identifiers come from the URL.
- **HTTP semantics** (§20.9): `GET` retrieves only; `PUT` overwrites — including
  the `state` and `data` sub-resources (ADR-0060); `PATCH` updates partially;
  `POST` creates and invokes an operation (such as `assign`). Nothing that changes
  server state is reachable through `GET`.

## Components and the design system

- **PrimeNG in styled mode**, driven by the generated Hamoni preset; re-colour
  from the preset, never per component (§21).
- **Tailwind is the layout layer**; do not mix in a second layout system.
- **Compose, do not fork**: a variant is a prop or a slot on the shared component,
  not a copy.
- Provider marks are the **registered vendor glyphs**, never drawn look-alikes
  (ADR-0045).
- Meet the accessibility rules (§18.5) and the contrast the design system
  declares.

## Testing

- **`vitest` is the default unit-test framework** — the default for testing case
  handling: the branches, error paths and edge cases of a unit.
- **Mock Service Worker (MSW)** is a mock backend so a real person can **log in
  and browse the application** (CODING-STANDARDS §23, ADR-0039). **Every new
  service ships happy-case handlers** for it.
- **MSW does not replace `vitest`**, and `vitest` does not replace the browsing
  handlers — the two are complementary.
- Test **observable behaviour**, not implementation detail; cover loading, empty,
  error and populated.
- Do not test against a hand-rolled fake client — go through MSW for the browsing
  path.

## Interfaces with the rest of the platform

- **→ the Control Room backend**: its HTTP interface, through the typed services
  in this project. The interface and its error shape are defined in
  [`../../API-AND-DATABASE.md`](../../API-AND-DATABASE.md); types are generated
  from, or kept in step with, the backend's OpenAPI description — never
  hand-written twice (§19).
- **← the design system**: the palette, type and radius tokens, and the PrimeNG
  preset, are the only source of visual truth.

## Before you finish

- `npm run build`, `npm run lint` and `npm test` pass.
- The affected screens follow Part III of `CODING-STANDARDS.md`.
- No secret, key or token is bundled into the application.
