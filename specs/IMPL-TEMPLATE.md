# Implementation approach — <Feature title>

| Field | Value |
| --- | --- |
| Feature | `NNNN` — <Feature title> |
| Spec | `specs/NNNN-<Title>-spec.md` |
| Approach | `specs/NNNN-<Title>-impl.md` |
| Screens | <`specs/NNNN-<Title>-screens/` — complete, or none> |
| State | Draft · Agreed · In build · Re-opened |
| Updated | <YYYY-MM-DD HH:MM> |
| Author | Technical planner |

## 1 · Approach summary

<The shape of the change in a few sentences.>

## 2 · Backend changes

<Projects, files, classes and endpoints to add or change — with the specific
change in each.>

## 3 · Screens

<The screens from the spec and the designer's summary: each screen's file and
purpose, and how the frontend implements it. "None" for a backend-only feature.>

| Screen | File | What to implement |
| --- | --- | --- |
| <screen name> | `specs/NNNN-<Title>-screens/<screen name>.html` | <components, states and interactions> |

## 4 · Frontend changes

<Components, services, routes and state to add or change. Derived from the
completed screens above; this approach is written only after they are complete.>

## 5 · Data model and migrations

<Tables, columns, keys and indexes; the forward-only migration(s) — or "none".>

## 6 · API changes

<Resources, methods, payloads and errors — or "none".>

## 7 · Tests to implement

<The positive and negative cases from the spec, mapped to the tests that will
cover them.>

## 8 · Sequencing

<What must land before what.>

## 9 · Risks and uncertainties

<Where this plan is uncertain, and what would resolve it.>

## 10 · Open questions

<Anything the spec does not settle, for the plan lead.>
