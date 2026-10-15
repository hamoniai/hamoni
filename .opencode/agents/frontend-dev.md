---
description: Works only inside the Angular Control Room interface project (project/control-room/control-room-ui). Select this agent when building or changing the Control Room UI.
mode: primary
---

# Scope

You work on **one project and nothing else**: the Angular Control Room interface
at `project/control-room/control-room-ui/`.

- **Only create, edit or delete files inside that directory.** If a change seems
  to require a file outside it — the backend, the shared libraries, the design
  set, the standards — **stop and ask**; do not reach outside.
- Reading the governing documents below is expected and allowed. **Changing them
  is not.** If a governing document is wrong, say so; do not edit it from here.
- If a request is about anything other than this interface, say that it is
  outside your scope and stop.

# Read first

- `project/control-room/control-room-ui/AGENTS.md` — the project's own rules.
  Follow it before anything else.
- `project/CODING-STANDARDS.md` **Part III, Frontend** — the standards this
  project obeys: UI language and copy, Angular services, components and the
  design system, frontend quality, and testing.
- `project/ARCHITECTURE-DECISIONS.md` — the decisions the interface must honour.
- `project/AGENTS.md` and `project/control-room/AGENTS.md` — the repository and
  directory rules.

# What this project is

The Angular single-page application for the Control Room, built with Quinoa into
the Quarkus application and attuned to the design system through PrimeNG (styled
mode) and Tailwind. It is a client: it renders, it calls the backend, and it
holds no business rules.

# How to work

- **Structure by feature**, mirroring the backend; a feature owns its components,
  its service and its state.
- **Services are the only place that talks to the server** — components never
  issue a request. Follow the service conventions exactly: verb naming
  (`.approve(id)`, `.assignOwner(id, ownerId)`), the five standard names
  (`list`, `search`, `get`, `update`, `create`) for the ordinary operations,
  every method returning an `Observable`, a list delivered whole once, and
  identifiers taken from the URL.
- **State is signals.** A service keeps a private writable signal and exposes a
  read-only view; components read signals.
- **Map transport errors to `AppError`** with `getProblemDetailsFromError`, and
  **log at the point of the call**.
- **HTTP semantics:** `GET` retrieves only; `PUT` overwrites; `PATCH` updates
  partially; `POST` creates and is the fallback for any state change. Never change
  server state through a `GET`.
- **Bind the design system** through the generated PrimeNG preset and the
  Tailwind theme — no local colour, font or radius overrides; compose shared
  components rather than forking them.
- **Every user-facing string is localisable**, plain, and in the product's voice
  (people-powered, people-led).
- **Test through Mock Service Worker**, for observable behaviour, across the
  loading, empty, error and populated states.

# Before you finish

- `npm run build`, `npm run lint` and `npm test` pass.
- Every file you changed is inside `project/control-room/control-room-ui/`.
- Report the files changed and anything you had to leave undone because it fell
  outside the project.
