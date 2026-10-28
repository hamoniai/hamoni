# Build spec — <Feature title>

| Field | Value |
| --- | --- |
| Feature | `NNNN` — <Feature title> |
| Spec | `specs/NNNN-<Title>-spec.md` |
| State | Draft · In review · Approved · In build · Re-opened · Superseded |
| Created | <YYYY-MM-DD HH:MM> |
| Approved | <YYYY-MM-DD HH:MM> — blank until approved |
| Approved by | <git identity: name <email>> |
| Handed to build | <YYYY-MM-DD HH:MM> — blank until the Build team receives it |
| Plan lead | <name> |
| Standards | `CODING-STANDARDS.md`, `ARCHITECTURE-DECISIONS.md`, `API-AND-DATABASE.md` |
| UI reference | `screens/` (synced into the implementation project) |
| Supersedes | <earlier feature NNNN, if this one replaces part of it> |

## 1 · Feature summary

<One paragraph: what the feature is and why it exists.>

## 2 · Functional description

<What the feature does, in the vocabulary of the product.>

**Personas this targets:** <The team participant / The team manager / The process
owner — and which of them this is primarily for.>

## 3 · Start and end states

- **Start:** <the state of the system and the person before the feature.>
- **End:** <the state after.>

## 4 · Architectural changes

### 4.1 Software structure

<Which projects, files and modules change, and what to do in each.>

### 4.2 Data models

<Tables, fields, keys, relationships — or "none".>

### 4.3 State flows

<Any process, activity or session state that changes — or "none".>

### 4.4 API contracts

<Resources, methods, payloads, errors — or "none".>

## 5 · Requirements

Requirements use **RFC 2119** keywords: **MUST**, **MUST NOT**, **SHOULD**,
**SHOULD NOT**, **MAY**.

### 5.1 Functional requirements

- <The system MUST …>
- <The system SHOULD …>

### 5.2 Non-functional requirements

- <Performance / scalability / availability / accessibility / localisation …>

## 6 · Pre- and post-conditions

- **Pre-conditions:** <what must be true before.>
- **Post-conditions:** <what must be true after.>

## 7 · Security

<Which roles may access what, and which actions each may perform. Reference the
security model and the organisation isolation.>

## 8 · Testing scenarios

### 8.1 Positive

- <A valid path that must succeed.>

### 8.2 Negative

- <An invalid or unauthorised path that must fail as specified.>

## 9 · Risks, edge cases and assumptions

<Anything that is assumed, cannot be evaluated now, or is a known edge. Each item
says what would resolve it.>

## 10 · Exceptions to the architecture

<Any accepted deviation from a decision, with its reason and the user's decision —
or "none".>
