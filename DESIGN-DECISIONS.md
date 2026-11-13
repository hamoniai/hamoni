> **Handover snapshot — read only.** This is a copy of the design project's
> `design-decisions.md`, imported as a handover reference for the implementation
> project. The design project remains the source of truth: **do not edit this
> file here.** It is regenerated from the design project on export.

# Hamoni · design decisions

Working log for the Process Designer / team manager wireframe prototype.
Status per entry: **Decided** · **Open** · **Deferred** (agreed, deliberately not built yet).

---

## 1 · Identity, groups and roles

The model behind the role labels, the rail, and what each screen composes.

### 1.1 A group is a local Hamoni concept — not an LDAP group · **Decided**

A **group** is an object Hamoni owns: a nJSOName, one or more managers, and a membership rule.
It is deliberately *not* an LDAP/AD group.

**Why:** organisations already maintain LDAP groups, but their naming, nesting and hygiene vary wildly and rarely map onto our concepts (a team that works a process is not the same shape as an org unit). If role resolution reads LDAP directly, every customer's directory quirks leak into our permission logic. One local indirection means the mapping is maintained once, in Hamoni, by the customer — and can be fixed without a release.

### 1.2 Membership is by reference, not by duplication · **Decided**

A group's membership is defined by **one or more references**, which may mix:

- LDAP/AD groups (by directory identifier), and
- individual users.

We store the reference, never a copied member list. The directory stays the source of truth for its half; people can be added by hand for what the directory does not cover (contractors, cross-team contributors, an exec who needs oversight).

### 1.3 Managers are explicit · **Decided**

A group names its own managers rather than inferring them from a directory attribute. Multiple managers per group, and one person may manage several groups.

### 1.4 Roles are derived at sign-in, not assigned · **Decided**

| Role | Derivation rule |
| --- | --- |
| **Team manager** | The user is a manager of one or more groups |
| **Process owner** | The user is configured as owner of a process (or of a sub-process / node) |
| **Member** | Default when nothing above is derived |

Derivation runs at sign-in, so directory drift and ownership changes correct themselves without an admin tidying role fields.

**Revised (2026-10-05)** — process ownership attaches to a **team**, not an individual. The **Process owner** role is derived from managing the team that owns the process. See §28.7.

### 1.5 Roles are additive, never exclusive · **Decided**

A person can hold several at once, and each screen composes from the union. The motivating example: on KYC the compliance head owns the whole process while individual sub-processes sit with specific managers — and the same person can perform tasks on one process, design a second and own a third.

Consequences to design against:

- **Ownership attaches at two levels** (process and sub-process). "Processes I own" is a *union* across levels, not a flat list. Getting this wrong breaks the KYC case.
- **Owner vs custodian.** A shared utility may have no operational manager. It still needs a **custodian** — the account of the owning container — or shared assets become orphans. *Owner* is accountable for the outcome; *custodian* is accountable for the asset.

### 1.6 The default role is "Member" · **Decided**

"User" was rejected: it describes everyone, so it distinguishes nobody. **Member** is the baseline everyone holds — they can work the tasks they are assigned, see and start processes they are granted, and create processes. Everything else stacks on top.

### 1.7 Open questions

- **Open** — Does a process owner inherit visibility of sub-processes they do not own, or is each node's access independent?
- **Open** — Are directory references resolved at sign-in (a snapshot) or evaluated live? Affects what happens mid-session when someone joins a group.
- **Open** — Can a group contain another group, or is nesting always via a directory reference?
- **Open** — Who creates and edits groups: self-service for managers, or admin-only?
- **Open** — What happens to a manager's queue when the manager is away? This is the reallocation plan-and-confirm flow on the home screen.
- **Open** — Group creation needs a governance rule (an owner and a stated purpose).
- **Open** — Where do personal drafts live? Working assumption: a private draft area per person, promoted into a container before it can run.

---

## 2 · How work is organised

Settled after prototyping three models against real content — nested folders, containers + collections, and a containerless library driven by saved views. The prototypes are gone; the reasoning is kept here.

### 2.1 One shallow container owns a process · **Decided**

A team owns a process. No nesting beyond one level.

**Why:** depth was the single largest source of "where did it go?", and reorganisation must never break a link or a bookmark. Folders read as dated and hide work below the second level. A containerless library has no home for a team's own work and turns access into policy scopes that cannot be explained to a non-technical majority.

### 2.2 Reuse is a label, not a move · **Decided**

Collections and a **reusable** flag let a process be found from anywhere without being copied into a second home. Labels never change who can see a process. The library's best idea — saved views and facets — survives as a filter layer over the container.

### 2.3 Access is a grant, not a location · **Decided**

Belonging to the container gives you sight of a process; **starting it is a separate grant**. A bookmarked path is never the thing that lets you in. Per-process override remains possible.

### 2.4 Is a Team the same object as a container? · **Open**

Strong recommendation: **yes, unify them.** The instruction that the *custodian* concept belongs on the team detail screen only makes sense if the team *is* the owning container — otherwise a process is owned by a container and the custodian is a team, and we have two objects doing one job. Unifying gives one object that owns processes and has people, which also makes "who is accountable for this shared utility?" answerable from the team page.

### 2.5 A project is a deployment unit · **Grounding agreed**

A **project** collects processes and business rules into one thing that can be deployed:

- **at least one process**, often many — a project with no process is not a project
- **business rules** shared across those processes
- **deployment**: which environment it is deployed to
- **a version**, incremented on each publish

Environments are **development**, **test** and **production**, and a project may be **not deployed** at all.

### 2.6 Team and project are orthogonal · **Decided**

| | Answers |
| --- | --- |
| **Team** | who owns this, who works in it — accountability (§2.1) |
| **Project** | which processes ship together, and where — packaging |

A process has one owning team and belongs to one project. Neither nests inside the other: an *Order to cash* project can package processes owned by Sales, Finance and Logistics without moving any of them.

**The risk is naming, not structure.** Adding *project* gives a non-technical audience a third noun to hold apart from *process* and *team*. The mitigation is to keep projects away from the 80% path: a designer works from their team, and a process shows its project as a **field** ("deployed as part of Order to cash") rather than a place they must navigate to. The Projects screen is the technical, advanced-mode view.

### 2.7 Environments are labels, and there is one by default · **Decided**

An environment is a **label** on a project — development, test, production — not a structure the frontend owns. Two consequences:

- **One environment by default.** A non-enterprise organisation never meets the concept at all; it appears only when somebody adds a second. The whole capability costs nothing when unused, which is the 80/20 rule applied to infrastructure.
- **Environments are configured in the infrastructure piece**, which is not designed yet, because process execution is remote from the frontend. The frontend labels and targets; it does not run anything.

An environment points at a **version** (§2.8), so a project can be at v7 in test while production runs v5. Promoting is moving a pointer, not copying anything.

### 2.8 Versions are linear and immutable · **Decided**

Editing happens in a **draft**. Publishing a draft creates an immutable version and increments the number.

- **No branching.** v(n+1) is always a descendant of v(n). There is never a second line of history to reconcile.
- **No reversion operation.** A published version is read-only and can be **deployed by pointer**, but it is never edited or auto-restored. Changes are made **forward** in the head draft and published as the next version; bringing anything from an older version forward is a deliberate, manual act. See §26.

This is what keeps "each change increments a version" affordable. Branching, and reverting by rewriting the forward line, are the two features that would make it expensive, and they are the two being declined — deliberately, now, rather than discovered later.

### 2.9 Assignment and delivery are two different questions · **Open — shape agreed**

Each task — and any other action that reaches a person or a system — carries an **assignment per environment**, because an enterprise may want testing done by a QA set of users rather than the production team. **By default one assignment is shared across every environment**; splitting it per environment is an option, not a requirement, so the common case is configured once.

That answers *whose work is this*. It does not answer *does it reach them*, and keeping the two apart is what makes the shared default safe:

| | Answers | Decided by |
| --- | --- | --- |
| **Assignment** | whose work is this? | the task, per environment, shared by default |
| **Delivery** | does it land in a real queue? | whether the environment is **classified production** |

Without that separation, a shared-by-default assignment means the moment an organisation adds a test environment, its test tasks route straight to the production team — the exact failure the feature exists to prevent. With it, adding an environment is safe until somebody deliberately points it at real people, and testing can still be done by the real team when that is what is wanted.

Classification, not naming, is what the rule keys on: every customer will have a "preprod" they consider production. This is the same instinct as §4.13's most-specific-wins and §4.14's deletion conditions — **make the unsafe state unreachable before making it configurable** — but applied to environments, where the user does want the configuration.

**Open — narrowed (2026-10-05).** Classification sits on a **deployment** — a specific version paired with an environment — not on the environment's name and not on the project, because a project can be at different versions in different environments. It may surface as a **view on the project**. The exact home depends on the versioning model and the infrastructure piece, so it is not closed yet. It reaches **every delivery**, not only tasks: notifications, integrations and document creation are deliveries too. See §25.

---

## 3 · Navigation and information architecture

- **The rail has four groups, each answering a different question.** · **Decided**

  | Group | Question it answers | Holds |
  | --- | --- | --- |
  | **Work** | what should I be doing? | Inbox, Activity |
  | **Manage** | what is happening day to day? | Processes, Tasks, People, Documents |
  | **Design** | what do we build? | Projects, Templates |
  | **Configure** | how is the organisation set up? | Organisation, Teams, Security |

  **`Inbox`, and it matches the icon.** The route here was *Tasks* → *Todo* → *Inbox*, and the last is the right one. *Tasks* is push-oriented: it describes what is being demanded of you. *Todo* fixed the direction but is a slightly nagging word. **Inbox is neutral** — a container things arrive in, which the system, a manager or you can put something into, and which you can look at for someone else. It is also the only one of the three that the icon already draws.

  **`Activity`, not `Performance`.** *Performance* carries a staff connotation the product is otherwise careful to avoid — this screen is about how the *work* is running, and in a people-first product the word reads as a judgement on people. *Activity* is neutral and describes what the screen actually shows. It is a safe choice rather than a striking one, which is the right trade for a nav label.

  **Work and Manage do not share a word.** `Work > Inbox` is *your* list, split by the views inside that screen (§5). `Manage > Tasks` is every task running across teams, at task level — an operational job rather than a personal one.

  **`Manage > People` is the distinction that was missing.** Teams is the **structure** — who is in which team, and who manages it. People is the **day-to-day state** — somebody is off sick, on leave, back on Monday. A manager does not change team membership daily; they change availability daily. That also answers the awkwardness of "configure teams": **Configure is the shape of the organisation; Manage is what is happening inside it today**, which is why the same subject can appear in both without being duplicated.

  **Processes sits under `Manage`, not `Design`.** An earlier pass moved the catalogue to Design on the reasoning that it lists definitions. It belongs where it is used: a manager looking at a process is asking what is running and who owns it, and the same screen carries both.

  **Filter by scope and keyword; not by role.** · **Decided**
  The Teams list filters on scope (*my teams* / *all teams*) and a keyword over name or manager. A **role** filter was built and then removed: **My teams** already means "every team I am a member of", which necessarily includes the teams I manage, so a role filter repeats a distinction the scope has already made — and it invites the question of what "not a member" means in a list that is meant to be browsable by anyone.

- **My work and My team's work are two items, not one.** · **Decided**
  A manager's own queue and the team's waiting work are different questions. The team screen is **Deferred** — the work-manager surface owns load, priority and KPIs and will be designed deliberately, not as a side effect of the home screen. The item is present but not clickable.
- **The rail collapses.** · **Decided** (persisted locally; counts survive as figures under each icon)
- **The tenant chip is not in the top-left.** · **Decided**
  The rail footer carries identity instead: **name** over **organisation**, with the **organisation menu** on it — and that menu **switches the tenant in place** (§7). *(The earlier line "the organisation is a sign-in decision, not a mid-session switcher" is superseded.)*
- **"Performance", not "Dashboards".** · **Decided** — "dashboard" carries a reputation for being decorative; the label should describe an activity.
- **"Shared library" is not a nav destination.** · **Decided** — it was a lower-order concept competing with Processes. Reusable processes are offered *inside* the create flow, which is where they earn their keep.
- **Nav counts mean "items in that list."** · **Decided** — Teams shows every team visible to you, not only the ones you manage.
- **The topbar carries the breadcrumb and one bar.** · **Decided**
  Row one is the topbar: rail toggle, breadcrumb, and a right-aligned **bar** holding the global search and the conversation as two peers of equal width. **Actions** is the only other entry permitted, because approvals and access requests have no page of their own. Help was removed — help has to be contextual, not a page-level element.
  **Row two is dropped on a list screen.** A list has no page head: the rail names the section, the breadcrumb names the path, and a third title repeats them. The screen keeps a **visually-hidden `h1`** (for structure) and starts on its **toolbar**; the content's top padding is the only gap below the header separator. A **detail** screen keeps its head — a description, a lead, or the process bar (§19) — because there the content below is not the section itself.
- **Home is reached from the breadcrumb.** · **Decided**
  The rail carries no Home item, so the leading crumb is a link on every screen. Removing it silently would leave no way back except the browser.
  **The breadcrumb follows locations, not the rail's groups.** Work / Manage / Design / Configure are navigation categories, not pages, so they never appear as crumbs — a crumb must be somewhere you can go, and there is no "Manage" page. A top-level section reads **Home / Processes**; a nested screen reads the full path (**Home / Processes / View**, §19.12). The rail answers *which section*, the breadcrumb answers *what path and how to get back up* — different jobs, so they are not redundant.
- **The rail no longer carries a create button.** · **Decided**
  The old `New process` button in the rail footer was the same action as the Processes page action sitting in the same viewport, so it went. Create now lives in exactly one place per screen.
- **Search is global; filtering is local.** · **Decided**
  The search in the header jumps to a resource from anywhere — team, process or screen — and never filters the page you are standing on. Filtering the list in front of you is a separate control inside the page body. (The prototype previously conflated the two; the Teams and Processes screens now carry their own filters.)
- **The page action lives at the top of the rail.** · **Decided (2026-10-08)** *(supersedes "the page action is the only solid button in the header")*
  On a list screen the one solid page action — Processes → *New process*; Teams → *New team*; Inbox → *New* — sits in the **rail, under the logo**, **below the level of the header separator** so it reads as belonging to the page, not the header, and **aligned with the top of the table**. The rail navigation moves **below it**, with clear space above and below the button to separate it from the nav. It is the only solid button in the viewport; it collapses to its icon when the rail is collapsed. On a **detail** screen the page action stays with that screen's head (the process state action, §19.4). Two solid buttons for one action in a viewport is still a failure.
- **Secondary header actions are unresolved.** · **Open**
  Processes carries a ghost *Export view* today. Whether secondary actions sit beside the page action, behind an overflow, or move into the page body is still open — and it interacts with the search field's width.

- **The rail is now identical on every screen.** · **Decided**
  The older screens carried a different rail (`Work` / `Needs you`, no Teams) long after the model had moved on. They have been migrated, and the `Needs you` group is gone: review-type work lives in Actions (§5), and waiting work lives in the two work queues.

- **The interaction pattern: show → preview → act.** · **Decided**
  Endorsed as an architecture pattern rather than a screen decision: **show** the most relevant thing first (your work, never a catalogue); **click to preview**, optionally expanding in place; **act — open or confirm** — only for destructive or confirmable operations.
  - A row in a table is a summary. Opening it in place gives summary detail: enough to decide, not everything. The full page holds the depth — editing, history, anything that can grow without bound.
  - The rule that keeps the middle level honest: **if a section inside an expansion can exceed roughly a dozen rows, it belongs on the page instead.** The Teams roster follows it — eight preview, the rest one click away, the full page beyond about a dozen.
  - The full page should be one page whose affordances depend on permission, not two near-identical read-only and editable pages.
- **Considered and rejected: the AWS selection-first model.** · **Decided**
  AWS puts a table, a row selection and a toolbar action on every screen. It buys action safety and consistency at the price of latency — three intents where direct manipulation needs one — which is why it suits technical audiences who favour control and tolerate ceremony. Rejected as our default: the audience is time-poor and interrupt-driven.
  Two parts of it are kept. First, its consistency comes from every screen sharing one pattern, and we get that by making show → preview → act a single shared component rather than a per-screen invention. Second, selection still earns its place where **bulk** is real — a manager transferring 15–50 people's scope cannot do that row by row. So selection is an explicit mode, off by default, and entering it replaces the page action with a contextual action bar.

- **Row interaction is direct manipulation; actions are not on the row.** · **Decided**
  Clicking a row looks; it never acts. Administrative and destructive actions live inside what you opened, next to the thing they affect, because a row click is cheap and reversible while a row action is neither.
- **Prefer one enumerated column to a set of boolean columns.** · **Decided**
  A row should be readable on its own. Boolean columns put the meaning in the *combination* of row and column, so the reader has to hold the table in their head, and it degrades with each column added. Where two states can coexist, express them as one value — `member`, `manager`, `member + manager` (§4.13) — not as two checkboxes. The tell is a table that needs a legend.

- **A conversation is state, not a destination.** · **Open — see §6**

---

## 4 · The Teams page

### 4.1 Open to browse · **Decided**

Anyone can find and open any team. Teams are not a restricted directory.

### 4.2 The process owner is never hidden · **Decided**

Who is accountable for running a process is visible to everyone, on every team, whether or not you belong. Visibility of *that* is a deliberate contrast with visibility of people.

### 4.3 Participant lists can be restricted · **Decided**

Membership itself is not public. A team's roster is visible if you belong to it, or if the team has been marked open. Otherwise the list shows the count with a restricted marker and offers *Request to join*. The count itself is not sensitive; the roster is.

### 4.4 Expanded rows, not a sibling detail pane · **Decided**

The list is a three-column table — **team, team manager, my role** — and clicking a row expands that row in place to reveal its detail. One row open at a time.

The sibling pane was built and then rejected on evidence: it forced a horizontal scroll beside a vertical one, and the pane ran ~1,050px tall, taller than most viewports, so it could never be glanceable. It also fails in the layout it collapses into — on tablet and mobile a sibling pane sits below the table, which with infinite scroll never gets placed and with paging is tiresome. An expanded row has no scroll axis of its own and works at every width.

Sorting is by clicking a column header. Marked in the wireframe, deliberately not implemented.

### 4.5 Three security tiers govern the expansion · **Decided**

| Viewer | Sees |
| --- | --- |
| **Not a member** | Nothing about the team's interior. One line of explanation and **Request to join**. Even the process count and backlog are withheld. |
| **Member** | Production processes involved, task backlog, member count, the manager, and who else is in the team. |
| **Manager** | Everything a member sees, plus **Edit team**, which opens team configuration on its own screen. |

The team manager stays visible in the table column to everyone — accountability for running a process is not confidential, even though the interior of a team is.

### 4.6 Scope tabs: My teams first and default · **Decided**

Two tabs, not three. "Managed by me" was dropped because it is a subset of "my teams" — a manager is a participant — and because a person manages only one to three teams, so it never needed to be a first-class destination. **My teams** (any team you participate in, as member or manager) is first and selected by default; **All teams** is the wider, possibly-thousands-long list.

### 4.7 Filter options · **Decided, incomplete**

Shipped: scope, keyword on name or manager, and my role (I manage / I am a member / I am not a member).
Candidates for later: activity in the last 30 days, team size, number of processes, domain. Listed as a design note on the screen rather than built.

### 4.8 Custodian is still a detail-page concept · **Decided**

Out of scope for the browse table, pending §2.4.

### 4.9 Manual versus SCIM onboarding · **Open — needs a discussion**

Enterprises provision people two ways: hand-entered in Hamoni, or synced from a directory via SCIM. This determines what team configuration can even offer — a list of people, a list of LDAP group references, or both — and whether membership is owned by Hamoni or by the customer's directory. Must be settled before the team configuration screen is designed. See §1.2 for how membership references are already modelled.


### 4.10 How much of the team's work a member can see · **Decided — three levels, set on the team**

A **team setting**, not a user preference and not a place in the rail. Three levels:

1. **Own work only** — the enterprise-control default.
2. **Own work plus unassigned work in the team** — people can pick up slack.
3. **Own work plus unassigned plus work assigned to other members** — the collaborative default.

The driver is real, not theoretical. In insurance claims an assessor routinely refers a case to a more senior colleague for advice, and that referral is also how authority is grown — judgement is exercised before it is formally granted. A pure control model breaks that behaviour; an assumed-collaborative model leaks exposure. So it has to be a choice the organisation makes per team.

**Seeing is not acting.** Level 3 lets a member see a colleague's item; it does not let them action it. Acting on work you do not own requires either the manager role or an explicit **referral**, which is a grant: targeted, attributable, revocable, recorded. And the referral record is the training evidence — who advised on what, which is precisely what justifies granting higher authority later.

Sub-teams are deliberately **not** modelled. Organisations do have managers who manage a slice of a 50-person team, but that is an organisational structure problem, and Hamoni is built for managers taking on scope from roughly 15 up to 40–50 rather than for nesting.

### 4.11 Configuration is staged, not immediate · **Decided**

The edit page shows the team as it is and lets a manager stage changes against it: added and removed rows are marked in place, and a bar counts what is pending until it is saved or discarded. This is the show → preview → act pattern applied to editing, and it matters more here than elsewhere because every change to membership is a change to who can see work. Nothing should be applied by accidentally touching a row.

### 4.12 Membership is a rule, not a list · **Decided**

A team stores **references**, not people: zero or more individuals and zero or more enterprise groups. What a person is entitled to is worked out from those references **at use-time**, never frozen when the team is configured.

That distinction is the whole point. If references were unpacked at add-time, adding *Claims Assessors* would capture today's membership; when the organisation later changed that group, the team would silently not follow. Tracking the reference means the team tracks the directory with nobody editing it.

- **Three kinds of reference**, named for what they are: **local** (a person added in Hamoni), **enterprise user** (a person from the directory), **enterprise group** (a directory group). The labels are placeholders; the distinction is not.
- **One person is one row.** A person reached by several references is still one person: the sources merge and both provenances are shown. *Dana Okafor* reads **Enterprise user, HR Management** because she is named directly *and* sits in that group. Local references have no notion of grouping, so a local person always has exactly one source.
- **Two people can share a name.** The resolved list may legitimately hold two rows reading *Mei Chen* — one added directly, one through *HR Management* — because they are different people. Nothing in the product treats a name as an identity, and the configuration page is where that becomes visible.
- Removing a group removes everyone it brought in **who has no other reference**. Someone named directly survives the group being removed and falls back to their remaining source.
- Without directory provisioning the same control invites by email, and those references are local. The model does not change, only the source.

Because resolution is live, the page also shows a **resolved membership** list — and marks it as a snapshot, since it will differ from the stored configuration the moment the directory moves. A long roster previews and reveals rather than being printed in full (§3).

### 4.12a The configuration page does not restate the organisation · **Decided**

The organisation is implied by where you are — the rail footer names it and the breadcrumb shows the tenant. Repeating it as a disabled field on a team page is duplication, and duplicated context is the kind that goes stale.

### 4.13 One row, one role · **Decided**

The membership list carries a **Role** column: a single control per row with three values.

| Role | Meaning |
| --- | --- |
| **Member** | performs the team's work |
| **Manager** | controls the team, and does not receive work |
| **Member + manager** | both |

The role implies work assignment, and that is the substance of the distinction — a manager is not a member with extra permissions, they are someone the team's work is **not** routed to. It is the same idea as available work hours of 0 (§4.16b), expressed as a role rather than a number.

**Why one control and not two flags.** The alternative — a *member* column and a *manager* column of checkboxes — makes the meaning live in the combination of row and column. The reader has to hold the table in their head, and it degrades with every column added. A single value column means **you only ever read the row**.

### 4.13a Roles resolve most specific first · **Decided**

The role is set once, on the reference, and the resolved list shows the result. Where a person is reached by more than one reference, **the most specific wins**: a named person beats a group they belong to.

That is what makes a group usable for membership without handing it the manager role — add *HR Management* as members, then name *Dana Okafor* individually as a manager. She manages; the rest of the group does not. The limitation of the previous model, where a group was all-or-nothing, is gone without adding an override state to clear.

Two consequences follow, and both are visible in the sample:

- **Named directly *and* in a group is one person, not two.** The enterprise identity de-duplicates, the row shows both provenances (*Enterprise user, HR Management*), and the named reference sets the role.
- **Removing the named reference is a demotion, not a removal**, when a group still reaches that person — the role falls back to whatever the group says. Removing the group, conversely, leaves the named person untouched.

**A manager must be a member.** Membership is the precondition: if a person stops resolving to the team, the role has nothing to attach to and they do not appear at all. An "appointed manager with no source" is not a state the product has.

**Removal rules**, which follow from the role being known up front:

- removing a reference that would take **you** out of the managers asks for a typed confirmation;
- removing one that would take out **the last manager** is **refused**, not confirmed;
- anything else is an ordinary staged change.

Because removal is simulated against the *resolved* result, the check accounts for precedence: taking away a named reference that was overriding a group demotes rather than removes, and only loses the last manager if the fallback says so too.

### 4.13b Identity has a two-stage lifecycle: disable, then delete · **Decided**

The same shape as deleting a team (§4.14), and the same for local and enterprise identities:

- **Disable** is reversible, keeps every reference and role intact, and is what a directory update does. Long-term leave, a leaver mid-handover, a temporary suspension — all disable.
- **Delete** is manual, conditional, and only possible when nothing references the identity.

SCIM **only ever disables** — it never deletes, because deleting on a directory signal would destroy history the moment someone was momentarily removed from a group. "Really delete" is always a deliberate local act.

Consequence worth stating: a team can lose its last manager without anyone editing the team, because a directory update disabled them. That is why the *unmanaged* state below has to exist at all.

### 4.13c A team can be inactive · **Decided**

A team carries an **active** flag, on by default. An inactive team keeps running the work already assigned to it and stays readable, but **cannot be chosen when designing a process**.

The driver is mergers: imported teams arrive in bulk, are parked inactive, and are consolidated one at a time. Without the flag the only options would be to leave them selectable (wrong work gets routed to them) or to delete them (impossible while they reference anything — §4.14).

It lives in **Team actions**, not in the team's identity (§4.13c-1). Renaming a team is ordinary housekeeping; taking it out of service is a decision with consequences, and grouping it with deletion makes that visible. The block reads as a small danger zone: things that change whether the team *works*, as opposed to what it is called.

### 4.13c-1 Overview holds identity only · **Decided**

The Overview panel carries the name and nothing else. The organisation is implied by the tenant (§4.12a), activity is an action (§4.13c), and the membership rules belong with membership. A configuration page where every section holds one thing is easier to scan than one where identity, state and membership are interleaved.

### 4.13d A team can be unmanaged · **Open — needs a design**

A team whose last manager is disabled, or is lost in a directory update, has no active manager. That is a reachable state, not a hypothetical, and the product must represent it rather than pretend it cannot happen: the team is flagged, the Teams list can filter for it, and the operations that need a manager — publishing, deleting, changing work handling — are blocked until one is appointed.

This is the same check that runs in bulk during a local-to-enterprise transition below, so it should be built once.

### 4.14 Delete, and why merge is not built · **Decided**

**Delete is conditional, not ceremonial.** A team can only be deleted when nothing references it: no active processes, no archived processes, no references in the process designer. This is only possible because **audit records are resolved into immutable items and never hold a live reference to a team** — which is what makes deleting anything safe at all. The page shows the three checks and their results, and when one fails that table becomes the list of what to fix.

**Merge is deliberately not built.** It is a rare operation with two unsolved problems: a team has to be found among thousands, so a dropdown is the wrong control; and it raises a permission question we have not answered — what happens when the person merging does not control the destination team? References make it worse again. Until those have answers the workaround is manual: add the people, then remove the old team.

### 4.15 Work handling · **Decided**

**What members can see** — three levels, defaulting to the widest (§4.10). There is deliberately **no "all work" option**: there is no global see-every-task setting, and cross-team oversight belongs to Manage › Tasks.

**How work is assigned** — three modes, defaulting to the first:
1. **AI assigns.** The virtual manager applies process rules and each person's workable hours.
2. **Manual.** Everything sits in *Available*; a manager assigns, or people pick.
3. **Capability.** The same rules, plus declared capabilities.

Operationally a manager assigns daily or half-daily, and that act sets the day's expectation against the hours a person has. The AI mode is doing the same job continuously rather than once a morning.

**Two states, and only two.** Work allocated to a person is *assigned*; work allocated to the team but to nobody is *available* and any member may pick it up. Every manual task must have a team; it may or may not have a person.

**Capabilities are added and removed on the configuration page**, and each is boolean (`trainee = true`) or numeric (`claims_approval_limit = 1000`).

**Open — coverage has to be testable.** Switching on Capability mode while nobody in the team meets a capability the incoming work asks for would leave work with no eligible person. Before the mode can be enabled the platform must check coverage — which capabilities the work requires, and whether anyone here holds them — and refuse or warn when nothing matches. That is a test feature, not a validation message. The task state model itself is defined with Manage › Tasks.

### 4.16 Calendars are derived, then adjusted · **Decided**

A team's calendar is the union of the organisation's holidays, **one country calendar**, and the team's own specific events. The reason is concrete: an offshore team does not share head office's public holidays.

**One country per team, inherited by default.** The page offers *use the organisation's country* or *use a different country*, with a picker behind the second. Listing countries as options on the page would mean maintaining every country in the world inside the layout.

**Inherited holidays are not listed on the configuration page.** If they ever need to be visible, the right surface is a **preview of the resolved calendar** — a calendar with everything merged — not a column of inherited rows sitting beside the team's own events. Two lists pretending to be one calendar is how a configuration page starts misrepresenting the outcome.

### 4.16a A calendar event is half a day at minimum · **Decided**

An event the team adds is a **name plus a date plus a length**, not a free-text line. The name is its own field; the length is chosen from a fixed list:

- **Half day — morning** / **Half day — afternoon**
- **1 day** (the default: morning through evening)
- **2, 3, 5 days**

**Half a day is the floor and there is no hourly granularity.** A working calendar answers *which days are worked*, not which hours — an hour of a day would not change any routing decision, and offering it would invite a precision the model cannot honour. A day starts in the morning and ends in the evening; the length is the only thing that stretches, from half a day upward.

### 4.16b A person's available work hours · **Open — to design**

Work assignment needs one more number per person: their **available work hours**.

- Stored **per person**, defaulting to the **organisation's working hours**.
- An **editable decimal**, so part-time is expressible without a separate "part-time" flag — 3.5 rather than "full time".
- **0 is still a legitimate value**, but it is no longer how "receives no work" is expressed — **the role does that** (§4.13). Hours are **capacity**: how much work fits into a person's working day. Eligibility is the role. Keeping those two apart means part-time at 3.5 and a manager who performs no work are different concepts, rather than one number doing two jobs.

This is the concrete backing for a claim already made in §4.15: the AI assigns using "each person's workable hours". Without this field that sentence has nothing behind it.

### 4.17 Security rules · **Decided**

- **With directory provisioning**, people are provisioned up front, so access reduces to *is this person known*.
- **Without it**, someone who reaches an organisation where they hold no role — not an organisation owner and in no team — is denied outright.
- Managing a team and working in it are separate grants (§4.13).

### 4.18 Moving from local to enterprise identity · **Open — needs a design**

The setting an organisation reaches for as it outgrows casual use. The problem as stated:

- Enterprises do not want a permanently mixed mode — identity should come from one place.
- It **cannot be a pre-check**. A team whose only manager is local would fail it, and there is no way to satisfy the check before the directory is connected.
- You cannot enable SCIM without enabling enterprise mode, so enterprise mode has to be reachable *before* the directory is live.

**Why the deadlock dissolves.** The constraint is not really about the transition. A team can lose its only manager at any time today — someone leaves, or a directory update disables them (§4.13b) — so "a team must have a manager" was never a true invariant. The real rule is **a team must never be *silently* unmanaged** (§4.13d). Once that state is representable, the transition stops being a special case: it is the same check, run across every team at once.

**Proposed shape, for discussion:**

1. **Identity source** (Local / Enterprise) and **directory connection** are two separate settings, not one. Enterprise mode can be on with the directory not yet connected, which is exactly the transition window — derived, not invented, from the constraint above.
2. Inside the window the organisation is deliberately mixed, and the UI says so and shows the window **closing**: a migration checklist, not a status message.
3. The work in the window is **identity claiming** — match local people to directory people, usually by verified email. Most local people *are* directory people waiting to be claimed; claiming moves their references and roles to the directory identity rather than recreating them.
4. What cannot be claimed — a contractor, a leaver — becomes **disabled**, keeps its references, and flags any team left unmanaged (§4.13d) for someone to fix.
5. The window closes when no local reference holds a role. Only then is the organisation enterprise-only, which is the thing enterprises actually asked for: a mixed *transition*, not a mixed *mode*.
6. **The reverse direction is not symmetric.** Local → enterprise is a migration (there is something to preserve). Enterprise → local is a reset that throws away the directory mapping, and materialising local identities for everyone is a downgrade, not a migration. Recommendation: do not build it until someone needs it, and consider exposing only "disconnect the directory" — which keeps enterprise mode and simply stops syncing.

**Open** — who performs the claiming (organisation owner, or each manager for their own teams), what happens to a team whose manager was never claimed, whether a time limit applies, and whether the window can be entered twice.

### 4.18a Existing profiles when the directory arrives · **Open — leaning decided**

Someone signs up locally with their work email. Later, SCIM syncs a profile with the same address. Three ways to handle it, and the answer matters more than it looks:

1. **Upgrade** — the local account becomes the enterprise account. Least work for the customer, and identity stays singular.
2. **Differentiate sources** — the same human exists as two profiles from two sources, and the organisation resolves them.
3. **One-way** — enterprise mode cannot be reversed, so the question never arises. The customer cost of that is the problem.

**Why (1) cannot be automatic.** Matching on an email address is not proof of identity — it is an **account-takeover path**. Anyone who signs up locally with a colleague's work address would, at the moment the directory syncs, be handed that colleague's profile and everything attached to it. The local signup was never verified as belonging to the person it claims.

**So the shape is (2) as storage, with (1) as an act.** Sources are differentiated by default — nothing is silently merged, which is the safe property — and **claiming** is explicit and verified: either the person proves control by signing in through the directory, or an owner approves the link. On claim, the references and roles move across; the local profile becomes an alias of the enterprise one rather than a second person. Differentiate by default, merge on proof.

**The cost of coexistence, and its mitigation.** While the window is open, one human can hold two identities, and work could be assigned to either — splitting history and confusing "who did this". Two mitigations worth designing:

- **Flag likely duplicates** — same verified email, or same name — so the organisation resolves them rather than discovering them later.
- **Freeze local identities once a matching directory person exists** — a local profile that looks like an enterprise one can no longer be granted new roles or references, which forces resolution instead of letting the overlap grow.

Option (3) is worth naming honestly: if the reverse direction is never built, "cannot back out" is what we have anyway — but as an accident rather than a decision. Better to say so explicitly than to discover it in a support ticket.

---

## 5 · Work model

- **Home leads with the person's own work, not the team's.** · **Decided**
  Counter-intuitive but deliberate: in most organisations the "manager" is an active participant in the process rather than a dedicated overseer. On KYC the manager is usually the person who knows the business rules best — that is *why* they are the manager. Our thesis is that Hamoni takes over the process-and-people management that a manager would otherwise delegate to a junior while tracking it in spreadsheets.
  There is an **education** dimension — the product should gradually teach a manager to manage rather than to do. But that is an organisational trait, not a setting; it cannot be flipped overnight, so the product must not assume it has already flipped.
- **One queue, several sources.** · **Decided**
  Assigned work, claimable team-queue work, visible teammate work and referrals are merged into one ordered list. On the home it collapses to a single count; on the work screen the sources survive as views.
- **The work screen is scoped by views, not by tabs and not by separate screens.** · **Decided**
  One list carries *Everything · Mine · Unassigned · My team's work · Referred to me*. Tabs were considered, since My teams / All teams already works that way, and rejected for two reasons. A tab is a **choice the user owns**, while "my team's work" is a **policy the team's owner sets** — modelling a policy as a tab puts the decision in the wrong hands and produces a dead end ("why is this tab empty?"). And a single *My team's work* destination cannot resolve at all: a person belongs to several teams — Celia belongs to three in our own sample data — so it needs a team picked before it means anything, which is a filter's job rather than a destination's. Views give the consistency tabs were reaching for without either problem, and they match the saved-view row already used on Processes.
- **The app orders the queue; the person does not triage.** · **Decided**
- **Load balancing and priority weighting are out of scope for these screens.** · **Decided** — those KPIs belong to the work-manager surface.
- **Review-type work is not work.** · **Decided** — approving a change or granting access has no deadline and blocks no run, so it lives in **Actions** in the topbar.
- **Home shows summaries, not lists.** · **Decided** — one strip: the urgent figure, the totals, and two ways in (*See work*, *Do next*).

---

## 6 · Prompt-first entry

- **The prompt is the primary entry point; everything else is a shortcut into the same mechanism.** · **Decided**
- **The prompt acts, but only through a reviewable object.** · **Decided**
  Natural language is cheap as *input*, never as *execution*. Any write intent renders as a plan — scope, per-row rationale, capability exceptions, what is deferred — with **Apply** / **Review individually** / **Cancel**, followed by an **undo** window and an audit entry naming the authorising human.
- **Five intent classes, each mapping to a renderer, not a menu:** Find, Explain, Act, Create, Measure. · **Decided**
- **Interception happens at the moment of intent.** · **Decided** — for a create intent, the reply is reuse / fork / start blank with the reason stated, and "start blank anyway" always one click away.
- **Permissions are shown, not hidden.** · **Decided** — a process someone cannot start stays visible and greyed with a request action.
- **The home never shows a catalogue.** · **Decided**

- **The conversation is resumable from anywhere, through the bar.** · **Decided**
  The topbar holds two narrow peers of equal width — **search** and **conversation**. They are mutually exclusive, and each expands in the direction its content needs.
  - **Search widens leftwards** on focus, consuming the rest of the bar. `/` focuses it: one keystroke rather than a modifier, and it is ignored while you are already typing in a field.
  - **Conversation expands downwards** as a full-width band beneath the bar, pushing the page down instead of covering it. Collapsing returns the screen as it was.
  - Pushing rather than overlaying is the point: nothing hides behind a scrim and the page you were on stays visible underneath. The cost, accepted knowingly, is that opening it moves the page — so the band is height-capped and scrolls internally rather than pushing content arbitrarily far.
  - The band shows the last few turns, the state in one line (“2 decisions waiting on you”), and a single way back into the conversation.
  - It **does not accept input.** The prompt, the intent classifier and the rich renderers live on Home; duplicating them in the bar would fork the behaviour of one conversation.
  - **Home has no conversation control**, because Home already shows the conversation at full width. It is the one screen where the bar holds only the search.
  - The hard part is state, not the affordance. A conversation can hold a **plan awaiting confirmation**, so the bar reports that in words and as a count, and says plainly that nothing has changed yet. Resuming must never quietly restore a half-finished decision.
  - **Open** — whether the bar should ever accept input, and whether the compact summary is sufficient or the renderers should move into a shared component so both surfaces can show the full cards.

  Options considered and rejected: a return-home link alone (costs a navigation, thread invisible from where you are); folding the conversation into the search box (one control doing two jobs, degrading both); a side sheet (competes with page width and hides content).

### 6.1 First-run onboarding is a conversation · **Decided (2026-10-07)**

A brand-new organisation has nothing to prompt about, so **Home opens on an onboarding conversation instead of an empty prompt**. The assistant opens, and the ordinary conversation mechanism carries it — there is no separate wizard screen, and the same transcript is held and resumable through the bar (§6).

- **The assistant opens and drives.** It puts the organisation into **collaborative mode** (the assumed default — personas §171), asks for a **team name**, assumes the author is the **team manager** (“changeable later”), then asks for the **process** in plain language. It never assumes a technical actor is present.
- **It works from a description, not a form.** From one paragraph it creates the **team**, sends the **invitation**, renames the **organisation**, and creates a **process inside a new project**, naming the data it tracks. Each step is shown as the **artefact it produced** — a done bar or a process card — so the person sees exactly what changed.
- **It asks only for what it cannot infer** — the team name, the invitees and the **SLA** — and stops when the person says there is no more data.
- **It closes on the next action.** It publishes the project, then offers to **upload the claims list** and start the processes; that hands the conversation back as an ordinary thread with real work in it.
- **The composer moves to the foot of the conversation** on first run, and the try-line (examples for a blank start) is hidden until the onboarding is done; the page head reframes from “What do you want to do?” to “Let’s get you set up”. `?onboard=1` forces the first-run state for review.

---

## 7 · Tenant and preferences

- **The organisation is switchable in-session.** · **Decided (2026-10-08)** — the rail footer carries identity (name over organisation) and the **organisation menu switches the tenant in place**; there is no sign-in gate on it. *(Supersedes "the organisation is chosen at sign-in".)*
- **Sign-in: no passwords, and the route decides the tenant.** · **Decided (2026-10-08)**
  - **No local password method.** Hamoni never stores a password. Every route authenticates through an external identity provider.
  - **"Local" identity is not a password.** A person Hamoni knows without a directory still signs in through a provider — the email is the key (§1.2). *Local* says where the identity is **managed**, not how the person authenticates, so §4.18's local → enterprise migration is unaffected.
  - **The public route.** The default sign-in page lets anyone sign in to any tenant through a **public identity provider** (Google, Microsoft, …) — the public/free tier, and the entry point when there is no tenant-specific link.
  - **The tenant route.** Each tenant is individually configurable and may carry its own **SSO**. A tenant-specific sign-in page offers **two options: sign in with a public method, or sign in with the company**. If the tenant is **enterprise SSO only**, no sign-in page is shown — the visit **redirects straight to the tenant's OIDC provider**.
  - **The route decides the default tenant.** Arriving through a tenant-specific route lands you in that tenant. Arriving through the public route with no prior session lands you in the **first tenant in the list (alphabetical)**. The person can **switch at any time** from the organisation menu.
  - **Identity is the provider's `sub` claim.** Email is a claim that can change, so it is not the key; the same person may hold two accounts (two `sub`s) with the same display name. The profile menu therefore shows the **name and the email** (from the OIDC claim) so a person can tell which account they are on.
  - **Open** — which public providers ship first; whether a public sign-in may reach an **enterprise-SSO-only** tenant (leaning: no — that tenant is reachable only through its own route); and whether a remembered last tenant beats "first alphabetically" on a return visit (leaning: yes).
- **Advanced mode is one setting for the whole application.** · **Decided**
  It is a single user-level preference, not a control per panel and not a switch per screen. Anything advanced appears wherever it applies once it is on. Per-control switches were removed from the process detail, the create wizard and team configuration: someone who wants advanced mode does not want to keep switching it on, and someone who does not cannot be expected to know when they should. Where advanced content is hidden, the screen says so and points at the user menu rather than offering its own toggle.
- **Dark mode** defaults to on. · **Deferred** — the design system registers a dark palette only, so this needs a light token set before it can do anything.

### 7.1 The sign-in model, the three login pages and the flows · **Revised (2026-10-14)**

The first attempt at this section assumed the wrong shape: a binary "does a public sign-in reach an SSO-only organisation", a remembered-organisation-beats-first-alphabetical question, Google-first ordering, single-organisation language, and a fabricated **auth-error** page. This pass unwinds all of it, and consolidates the flows into **three**. The model is drawn as **one flow map**, `journeys/auth-flows.html`.

**Organisation, not tenant.** The platform is multi-organisation, but the person always meets the word **organisation** (*tenant* stays technical, internal).

**Authentication is not authorisation.** Authentication proves **who you are**, once, through an OIDC provider. Entering an organisation is a separate grant. A signed-in person who holds no access is **not an error** — it is an authorisation outcome with its own page and next action.

**Identity is the provider's `subject`.** A person is a **provider + subject** pair; the same subject may belong to **many organisations**, and the same human on two providers is two identities (the profile shows the name and the email so a person can tell which account they are on, §7).

**The three login pages.**

- The **global (default) sign-in page** authenticates only. It carries no organisation: after sign-in the organisation is selected from **session state**, or the default is **determined** when there is none.
- An **organisation login page** is its own route, configured as **A** public methods only · **B** public methods and private ("sign in with my company") · **C** private methods only.
- **Config C renders no page** — the visit redirects straight to the organisation's identity provider.

**Provider order.** Microsoft first, then Google, then Apple — the business-first default for the public methods.

**The three flows.**

1. **J1 · Global sign-in** — authenticate by any public method on the global page → if there is session state, select the organisation; otherwise determine the default → the authorisation flow (J3).
2. **J2 · Organisation sign-in** — authenticate by any method the page allows (public, private, or both) → select the organisation whose page this is → the authorisation flow (J3).
3. **J3 · Authorisation** — if the person has access to the organisation, select and enter it; if not, **break the journey and show the organisation chooser**.

**Access to an organisation** requires **both**:

1. **The provider is enabled for the organisation** — either configured explicitly, or an allowed public method. Disable it and every identity that used it loses the door.
2. **A profile exists** — the person is the organisation's **owner**, or holds an **invited profile**.

**The profile is why revocation is organisational.** Team membership cannot express access — a person may leave every team and still belong — so revocation must be **removal from the organisation**. To make that possible, **an invite creates the profile** (carrying the **invite code**) before the person has an account; **accepting** fills it out. A directory sync **disables** the profile, and a disabled profile has no access. Inviting is **two-way**: the organisation invites and the person accepts, even when the person is already known.

**The chooser never auto-decides.** It validates the person's access to their **remaining** organisations, but never silently selects one: auto-selecting would answer one organisation and enter another, and since the organisation is only visible from the profile popup, that quietly confuses. **No access to the attempted organisation breaks the journey and prompts.**

**Every other case is an example of J3** — including the happy case, an **organisation turning private-only** (condition 1 fails; assignments remain, the door does not), **access revoked** (condition 2 fails; the profile is removed from the organisation), an **invite not yet accepted**, and **disabling by directory sync**. **Authorisation runs once, at launch** — matching a single-page frontend, the check is *not* repeated on every page or request; the grant holds for the session and is evaluated again only when the **organisation is switched** or the **app is reloaded**.

**Settled consequences.**

- **No stored password anywhere** — the historical wireframe form is fully superseded, and there is no local password method.
- **No "auth-error" surface.** The first attempt's error page was a fabrication and is **removed** (`screens/auth-error.html`); authorisation failures are the *chooser* outcome above, and provider cancel/failure is not a designed page.
- The **organisation is switched in-session** from the organisation menu (§7); **sign-out** stays in the user menu.
- **MFA stays provider-side** — confirmed, never a Hamoni screen.
- The **local → enterprise transition window (§4.18)** is an organisation-configuration concern and is not surfaced at sign-in.
- **Provider marks** are the registered PrimeIcons vendor glyphs, never drawn look-alikes.

**State of the build.** `journeys/auth-flows.html` is the flow map. `screens/login.html` is reworked to the anonymous, URL-driven model, and `screens/invitation.html` is the invitation screen (§7.2); their interaction design is still iterating. `screens/auth-callback.html` remains to be reworked. `screens/assets/app.css` keeps the `.auth*` block. A new **`journeys/`** directory now holds the journey documentation: `journeys/overview.html` (the high-level journey by persona, and onboarding under the organisation owner) and `journeys/auth-flows.html` (the sign-in flow). The old `wireframes/index.html` — which was mislocated there — moved to `journeys/overview.html` and is unlinked.

**Open** — the resolution rule when session state and default ordering disagree; whether the global page ever lists organisations *before* sign-in; and the "create a new organisation" flow's own journey.

### 7.2 The login page is anonymous, and the invitation flow · **Decided (2026-10-15)**

- **The login page never names the organisation.** The URL carries it (`/auth/login?organisation=xxxx`), but the page renders **no organisation name** — otherwise anyone could enumerate login pages to discover which organisations subscribe. The same risk removes any "not this organisation? switch" affordance from the page.
- **No organisation in the URL → the public page.** The path decides: an `organisation` parameter yields that organisation's page (its configuration decides the methods, §7.1); without one, the public page is chosen.
- **A method list is not a leaderboard.** Every public method (Microsoft, Google, Apple) is **equal weight**; only the organisation's own configured method is **preferential**.
- **Controls carry the app's control fill.** Every method button uses `--bg` over the card's `--surface` — the same dark fill every input uses — never a transparent outline that blends into the surface. **Hover is uniform:** the fill lifts and a light `--fg` surround appears, on the neutral method buttons and on the accent button alike.
- **Authorisation is not the login page's problem.** A person at the wrong page may authenticate successfully while authorisation fails — that is J3's job (§7.1), not a switcher on the login page.
- **The organisation chooser** is `screens/organisation-selector.html` — the pre-organisation surface that lists the organisations a person may enter (the table's row anatomy, **no expanded view**) with **Create a new organisation** at the foot. It keeps the auth shell because the person is not yet in an organisation.

**The invitation.** An invite link is always **against an organisation**, never the public page: `/auth/login?organisation=xxxx&invite=yyyy`. The person **authenticates first**, then lands on the invitation screen (`screens/invitation.html`) **after login**, where the organisation is named **once** and a single **Join** action completes and activates the profile (the invite created it with a code, §7.1). There is **no decline control** — doing nothing simply does not add the person, so closing the browser *is* the decline. The screen keeps only the next action; the **24-hour expiry lives in the invitation email**, not on the screen.

---

## 8 · Content: documents and templates

### 8.1 What a document is · **Decided**

A **document** is a file object attached to a process. It arrives or is produced at a specific point in that process:

- **Created to start the process** — an order form received by email, a signed contract, an incident report.
- **Created during the process** — a shipping confirmation, an approval note.
- **Generated inside Hamoni from a template.**
- **Created by an external system** — SAP, a logistics API.
- **Uploaded by a person** as part of a user task.

The key property: a document is never free-floating. It belongs to a process instance, and its origin determines who can read it, what it is called, and whether it can be replaced. How documents behave *inside* a process is deferred — it needs its own pass, and it will touch the process detail screen.

### 8.2 Templates is a distinct concept · **Decided**

A **template** is the thing a document is generated from. It is content with a shape — a form, a letter, a notice — not a process and not a file. It was missing from the model and now sits on the `MANAGE` rail underneath Documents.

Templates are reused across processes, which is exactly why they are their own object rather than a checkbox on a process. Reuse is the whole point of them.

### 8.3 Screens are not built · **Deferred**

Documents and Templates are in the rail as placeholders. Neither has a screen, and both wait on §8.1's deferred pass so that the browse experience can be designed around how documents actually attach to processes rather than around a file list in the abstract.

---

## 9 · Deliberately not built yet

| Item | Why |
| --- | --- |
| My team's work screen | Belongs to the work-manager surface, which is a system of its own |
| Single task screen behind *Do next* | `my-work.html?next=1` is the honest stand-in |
| Read-only team page (the third level) | Worth building when a team carries more than people — processes, custodian, documents |
| People screen | Day-to-day availability: time off, sickness — `Manage > People` |
| Projects screen | **Built** — `projects.html` and `project-detail.html` (§25) |
| Environment classification and the delivery guard | Assignment agreed; classification sits on a deployment, exact home open — §2.9, §25 |
| Business rules (upload-and-reference vs. bound to a decision activity) | Deferred to the process designer — §25 |
| The advanced-mode gate on Projects | Deferred — settled while reviewing the list — §25 |
| Version list / visual (created, author, what changed, deployability now) | Needed; the project view carries only a partial list today — §26 |
| Deploy-time reference check on rollback or restore (stale teams, capabilities) | Not built — §26 |
| The unit of a project version (whole project vs manifest) | **Resolved** — whole-project and forward-only; manifest declined — §26 |
| Restore-from-version / compare-versions convenience | Advanced mode only; not built — §26.2a |
| Team configuration screen (name, people, LDAP groups) | **Built** — `team-edit.html` |
| Organisation screens (owner, people, calendars) | Rail item placed; screens not designed |
| Organisation locale settings (default language, timezone, location) | Organisation-level config; recorded, not designed — §21 |
| Configure › Security screens (SSO, SCIM) | Rail item placed; screens not designed |
| Manage › Tasks screen | State model settled (§13.1); the cross-team operational screen is not built |
| Person search for assign / refer | The search UX — list, full-screen picker, recent people — is not designed — §13.4 |
| Team work-visibility setting (own / +unassigned / +whole team) | **Built** — on `team-edit.html` |
| Capability coverage test | Blocks enabling Capability mode — §4.15 |
| Identity lifecycle: disable versus delete | Decided in shape (§4.13b); no screen |
| Local to enterprise transition | Needs a design — §4.18 |
| Unmanaged team state | Reachable today; needs a design — §4.13d |
| Merging two teams | Rare, and two questions unanswered — §4.14 |
| Resolved-calendar preview | The right surface for inherited holidays — §4.16 |
| Better names for local / enterprise user / enterprise group | Distinction settled, labels are placeholders — §4.12 |
| Selecting rows in bulk | Only where bulk is real; not built anywhere yet |
| Documents screen | Waits on how documents attach to processes — §8.1 |
| Templates screen | Waits on the same pass — §8.2 |
| Input in the conversation bar | The bar resumes; it does not yet continue — §6 |
| Shared conversation renderers | Needed if the bar is ever to show the full cards — §6 |
| Column sorting on the Teams table | Marked in the header; not implemented |
| People in the global search | No person or profile screen exists yet, so people are not indexed |
| Light palette for Dark mode | Token work, not UI work |
| Wiring Advanced mode globally | Depends on the final shape of advanced surfaces |
| Tenant switch route | Sign-in owns this |
| Multiple active tasks — pause the others on open | Organisation default with a personal override; not built — §13.7 |
| Complete from the task list | Team-level config; a later feature — §13.8 |
| Re-open a completed task | Breaks process ordering; valid only before the next activity starts — §13.5 |
| Task or process comments | Leaning process-level threaded discussion; not designed — §13.9 |
| Task work screen | Dynamic + static components; opened by start — §14.1 |
| Ordering and estimate calculation | Complex (historical effort per process); detail deliberately deferred — §14.8 |
| Completion / close model | Decided as a process-task “requires confirmation of close” mark; confirm it lands — §14.7 |
| Conflict and error handling (flash messages) | Application concern; not designed — §14.9 |
| Bulk take / reassign | Manual mouse handling for now — §14.10 |
| Mobile layout | Desktop-first; deferred — §14.10 |
| List scale / paging approach | Deferred; stable first page plus refresh is the leaning — §14.5 |
| Start opens in a new tab | Personal preference; deferred with list scale — §14.1, §14.5 |
| Refresh as a common list control | Candidate for every list, not only the Inbox — §14.5 |
| Task screen — edit-back-into-Hamoni variant | View variant built (`task.html`); the edit variant is deferred — §15.1 |
| Process summary bar — relevance to the person | Resolved: it belongs to the process view; the task screen keeps an echo — §19.3 |
| Process definition screen (steps as designed, versions, who may start) | The catalogue row is a definition; `process-detail.html` is now the instance view, so the catalogue link needs a definition screen — §19.1 |
| Process link addresses an instance by name only | The process view should be addressable by case reference (`?name=…&case=…`) — §19.9 |
| Access rule: may the process override tighten the org default? | Still open — §16.3, §19.9 |
| Dynamic region in the expanded inbox row | Marked in the wireframe; design after the process design screen |
| Pended processes view | Referenced by the pend screen; screen not built — §17.7 |
| Exception handling | Deferred with the process actions and resume — the process detail carries neither for now (§19.16); how a designer authors exceptions is not built — §17.4 |
| Cancel screen | A process state change needs its own surface, not an in-page control; modelled on pend — §19.15 |
| Resume screen | As cancel; a manual resume may carry a free-text reason — §19.15 |
| Quicklink to start any process mid-process | Same mechanism as pend actions, wider surface — §17.6 |
| Pend Time control | Behaviour settled; the control needs UI work — §17.11 |
| Organisation collaboration default (most collaborative / neutral / most secure) | Sets the collaboration posture once, at organisation level; needs a design — §23.1 |
| Team structure drift | Teams will drift from reality as people leave and move; teams are embedded in processes, so a stale structure quietly routes or blocks work. Needs a deliberate reconciliation route, including natural language — §23.2 |

---

## 10 · How this prototype is built

Everything in this section exists so a new session — or a new person — can extend the prototype without re-deriving decisions that are already settled.

### 10.1 Fidelity is wireframe, on purpose · **Decided**

Greyscale within the palette, no imagery, no colour semantics beyond the tokens. Regions that are not designed yet are marked with a dashed box and a mono caption (`.zone`, `.note`) rather than filled with something plausible.

The reason is that these screens are being used to settle **structure, permissions and flow**. Raising fidelity early locks in visual decisions before the interaction model stops moving, and it makes reviewers argue about the shade of a badge instead of whether a team can see another team's work.

### 10.2 Token discipline · **Decided**

The registered Hamoni hexes live in `:root` and appear nowhere else. Every other colour is a `color-mix()` of them. The palette is **eleven**: the seven originals (canvas, surface, foreground, muted, border, dim, accent), the registered teal `--info` (#03dac5 — the AI voice and the next action in *form*), and the three **RAG status colours** — `--success` (#4AD371), `--warn` (#F4CF4A), `--error` (#FF8389), registered 2026-10-16. **Accent appears at most twice per screen.** This is checked by grep on every change, not by eye.

### 10.3 Shared chrome is injected, not repeated · **Decided**

`assets/app.js` builds the parts of every screen that are identical: it wraps the topbar search into the bar, creates the conversation sheet and the jump list, collapses the rail, persists preferences, and drives the dialogs. `assets/app.css` holds the whole component system.

A new screen therefore starts by including the stylesheet, the script and the rail markup — that alone gets the bar, the search, the conversation, the collapse and the identity menu. **Do not hand-roll those per screen.**

### 10.4 Conventions every screen follows

- **`data-od-id` on every section**, in kebab-case, so comment mode can target a region.
- **One page action**, top right of the content, and at most one solid button in the header.
- **One accent per view**, spent on the primary action or a single emphasis — never both plus decoration.
- **The rail is identical everywhere**, with `aria-current` moved to the current screen.
- **The flowbar is identical everywhere**; it is prototype scaffolding for moving between screens, not product chrome.
- **`processes.html` is the gold-standard table** (§24) — the Inbox and Teams tables reconcile to it, and a new list starts from it rather than from a blank table.

### 10.5 The verification habit · **Decided**

Every change ends with the same checks across all screens: tag balance, every inline script parsed, no duplicate `id`s, no broken internal links, no hex outside `:root`, and at most one render. It has earned its place — it caught an **unclosed `<script>`** that had quietly disabled a whole page's interactions while still looking correct, and a **dropped closing slash** from a bulk rename that had stripped the closing tag from every `<a>`, `<span>` and `<h1>` around one word, in all ten files.

### 10.6 The cast is fixed — do not invent a parallel world · **Decided**

- **Organisation** Acme Corporation. **Signed in as** Celia Yu, `celia.yu@acme.com`, process designer, one of two managers of HR Operations.
- **People** Mei Chen, Dana Okafor, Ravi Patel, Sofia Berg, Tom Alvarez, Yuki Tanaka, Amara Osei, Ananya Rao, Lucas Moreau, Ingrid Halvorsen, Kwame Mensah, Nadia Rahman, Freya Lindqvist.
- **Teams** HR Operations (8 references, 13 resolved people), Recruitment, Procurement, Logistics Dispatch, Finance Control, Site Safety, Fleet Maintenance, Payroll.
- **Directory groups** HR Management, Claims Assessors.
- **Processes** New hire onboarding, Offer approval, Interview scheduling, Résumé screening, Contractor offboarding, Purchase order approval, Supplier onboarding, Invoice exception handling, Expense claim triage, Monthly payroll close, Fleet maintenance request, Warehouse pick-and-pack escalation, Site incident report, Access request review, Customer refund authorisation.

Numbers in these screens are **sample data, labelled as such where they appear**. They are internally consistent — counts match the rows shown — and must stay that way when edited.

### 10.7 Screen map, and two filenames that no longer match

| File | Screen |
| --- | --- |
| `index.html` | Overview — flow, comparison, screen index, decisions |
| `login.html` | Sign in |
| `home.html` | **Home** |
| `my-work.html` | **Inbox** |
| `teams.html` | Teams |
| `team-edit.html` | Team configuration |
| `processes.html` | Processes |
| `process-detail.html` | **Process detail** — a running case (the process view) — §19 |
| `dashboards.html` | **Activity** |
| `new-process.html` | Create process |
| `task.html` | **Task work** (the classic screen) — §15 |
| `task-chat.html` | **Task work** (chat variant) — §15.9 |
| `pend.html` | **Pend** — §17 |

**Open** — two filenames no longer match their screen names: `my-work.html` and `dashboards.html`. Nothing is broken, because every link points at the file rather than the name, but a reader browsing the project will be misled. Renaming is a mechanical pass (rewrite `href`s, rewrite `data-href`s) and should happen before any external review. The home is already done — `home-v2.html` was renamed to `home.html`.

Also **Open** — `task-chat.html` renders only the **Discussion** band control. Its assistant is *the conversation widget on the canvas*, which is the §15.10 rule, so the missing header control is correct. Any future edit must keep that: the two are the same screen with a different claim widget.

---

## 11 · Retained artifacts

- **The earlier `home.html` prototype (v1) has been removed.** It held the three organisational models as live prototypes. Their outcome and the reasons for rejecting two of them now live in §2 — the reasoning is preserved here rather than in a dead screen.
- **`home.html`** is the home (renamed from `home-v2.html`) — the only home now — see §10.7.
- **`index.html`** is the entry point and doubles as the flow map.

---

## 12 · The Inbox table and the task state model

### 12.1 The task state model · **Decided**

A task moves through five states, and the transition is the work the person does on the Inbox itself.

| State | Meaning | Leaves it via |
| --- | --- | --- |
| **Available** | Unassigned, rendered two ways: **Available** — open to *you*; **Waiting** — you cannot perform it (permissions or capability), so it stays with the team. | Take → Assigned |
| **Assigned** | Held by one person — you, or a colleague in the team scope. | Start → Active |
| **Active** | Being performed. A person may suspend or pause it, which returns it to Assigned. | Pause → Assigned; Complete → Completed |
| **Completed** | The person has marked it done. It says nothing about the output or its quality. | Re-open → a *new* task with its own state and tracking; the completed one stays for the record |

Quality control and quality assurance read the *output*, never this flag. Re-opening does not resurrect a task — it creates a new one, because re-doing work is a new unit of tracking.

### 12.2 The Inbox is two scopes, not five views · **Decided**

The five views (Everything / Assigned / Unassigned / My team's work / Referred) mixed two questions — where the work came from and who may act on it — into one row of tabs. They are replaced by two scopes that match the state model:

- **My inbox** — work that is yours or open to you: Available-for-you, Assigned, Active, and your Completed.
- **Team inbox** — your teams' work: unassigned tasks, including the Waiting ones you cannot perform, and a colleague's assigned or active task, which you may watch but not move on your own. A team is picked before the team scope means anything, because a person belongs to several teams.

### 12.3 A row shows what decides, and nothing else · **Decided**

`<process reference> · <task name>` over `<process name> · <state>`, the estimated work left against the process target, and the state controls. Not shown: the internal system identifier (API-only), the assignee on your own work, and the assigned / referred / overdue labels — none of them changes the decision to take the work. The process reference is the business key (a claim id, a purchase order), never the system id.

### 12.4 Order, not deadlines · **Decided**

The list is ordered by the expected work remaining on each process, estimated from history. The order is a recommendation, not a lock, because a task can be done on time while its process is late. "Due in" and overdue flags are removed at the task level: they pressure a unit the customer never sees, and a person's throughput is measured elsewhere. **Open** — the row prompt is an estimate *against the process target*; whether it shows the task's own effort, the process's remaining effort, or both is not yet settled.

### 12.5 Controls live on the row · **Decided** (an exception to §3)

Each row carries a small cluster of graphical controls on its right — Take, Start, Pause, Complete, Re-open — that move the task between states without leaving the list. This is a knowing exception to *actions are not on the row* (§3): this screen's whole job is throughput, clicks and navigation are the cost, and the person is measured on work handled. The cluster is the same everywhere, so the pattern is learnt once.

### 12.6 Still open

- The estimate framing (§12.4).
- Whether a Completed task stays in My inbox indefinitely or ages out.
- Whether a Waiting task, if the capability is granted later, notifies the team that it is now takeable.

---

## 13 · The Inbox, roles and the action table

This supersedes parts of §12: the row now carries the **owner**, not the state (§13.2); the estimate is the **task-type effort**, not remaining against the process target (§13.3); and the controls are the **union of the roles** a person holds (§13.4).

### 13.1 The task state model · **Decided**

| Status | Sub-status | Meaning |
| --- | --- | --- |
| **Available** | **Waiting** | Unassigned, and not possible for me to perform — permissions or capabilities |
| | **Available** | Unassigned, and open for me to do |
| **Assigned** | | Held by one individual |
| **Active** | | Being performed; pause returns it to Assigned |
| **Completed** | | Marked done. Says nothing about the output’s quality. Re-opening creates a *new* task |

Active and Completed can each be left: pause returns Active → Assigned; re-open takes Completed → a new task with its own state and tracking. Quality control and quality assurance read the *output*, never the Completed flag.

### 13.2 The row shows the owner, not the state · **Decided**

The state is a technical fact the person does not need to hold, so it is not shown. The second line is `<process name> · <owner>` — **me**, a colleague’s name, or **—** when the task is unassigned — which also makes the row work in the Team scope, where the owner is usually someone else. In progress versus assigned is carried by the controls. No extra column is spent on it.

### 13.3 The estimate column · **Decided**

The column is **Estimate**: the typical effort for this kind of task, aggregated from history. It exists so a person can pick work that fits the time they have — two 15-minute tasks in a 30-minute gap, not the one that takes an hour. It replaces the earlier “remaining against the process target” idea (§12.4), which is dropped from the row.

### 13.4 Roles are distinct, and a person sees the union · **Decided**

Role is resolved per team. A person who is both a participant and a manager sees the combination of every action. *List view* actions sit in the row cluster; *expanded view* actions sit in the opened row.

**Team participant**

| Status | Sub-status | My inbox | Team inbox | List actions | Expanded actions |
| --- | --- | --- | --- | --- | --- |
| Available | Waiting | No | Yes | — | — |
| | Available | Yes | Yes | start | — |
| Assigned | | Yes | Yes | start | refer |
| Active | | Yes | Yes | pause, resume | refer |
| Completed | | No | No | — | — |

**Team manager**

| Status | Sub-status | My inbox | Team inbox | List actions | Expanded actions |
| --- | --- | --- | --- | --- | --- |
| Available | Waiting | No | Yes | assign, start | — |
| | Available | No | Yes | assign, start | — |
| Assigned | | Yes | Yes | assign, start | — |
| Active | | Yes | Yes | assign, pause, resume | — |
| Completed | | No | No | — | — |

**start** and **resume** are the same action in different contexts — start for work that has not begun, resume for work returning from a pause. Anyone who starts a task becomes its owner: starting an Available task activates it and sets the assignee, and **start also opens the work** (§14.1). Security stops one participant starting work assigned to another; a manager may. A manager taking over someone else’s task is not a fault to guard against — it is **work stealing**, and operations depend on it (§14.2). Waiting and Available currently render identically when unassigned; that suits a participant, who is never offered a Waiting task, but a manager needs to tell them apart, and a cue is owed (§14.3).

**Assign** and **refer** both choose a person. Assign is a manager action and a reassignment; refer is a participant action on Assigned or Active work and a lighter grant — the person reads and advises, they do not complete. Assign opens the expanded row onto a person control: recent people first, then search, because the common case is referring several tasks to one person rather than searching afresh each time. The person-search experience is **not designed** — a flat list costs space and a popup is poor on mobile, so a full-screen overlay carrying recent history is the leaning, not a decision.

The row cluster is exactly **start · pause · refer — always in that order, shown as blanks where a control is not allowed**, never greyed out. A pending blank is easier to scan than a disabled button, which reads as broken. “Refer” is the whole refer family; **assign is its manager-side form** and is not a fourth button.

**Start is the resume.** There is no separate resume control: a task the person can open and continue shows **Start**, and once it is **Active** the same position shows **Pause**. Pausing returns the task to assigned, so start reappears. The pair is one control changing its meaning with the row’s state, which removes the earlier three-button set and the ambiguity of resume-versus-pause.

**The header mirrors the row.** The filter controls keep their positions by ending with **two control-spaces held open**, so Refresh lands exactly two button-spaces in from the right, over the row controls.

### 13.5 Completed work · **Decided**

Completed tasks are hidden by default and revealed by a toggle; when shown they sort to the bottom, most recent first, because the reason to ask for them is usually something just finished. Re-opening is a later feature: it breaks process ordering and is only valid while the next activity has not started. How a close is confirmed is open — §14.7.

### 13.6 Inbox scopes · **Decided** (unchanged from §12.2)

My inbox and Team inbox. Role decides visibility per the table above; the Team scope additionally needs a team chosen.

### 13.7 Multiple active tasks · **Decided — shape agreed, not built**

What happens when a task is opened is a configuration, because the situations differ: it may **pause every other open task** (attention is fully consumed, and leaving the others is a break) or **leave the others untouched** (the case is waiting on something external, such as a system report).

It cannot be team level — a person in several teams would get inconsistent behaviour from the same action. It should be an **organisation default with a personal override**: wide enough to be set once, narrow enough for the person whose work it actually is. Added to §9; not built.

### 13.8 Completing from the list · **Decided — shape agreed, not built**

Completing a task from the list is a no by default — it is too easy to complete without reviewing. A high-volume, low-risk team may need it, so it is a **team-level** configuration. Design-time in the process designer is too detailed a place for it. Added to §9; not built.

### 13.9 Comments · **Open**

Should a comment live on the task or on the process? Leaning: **process level**, threaded (a comment and its replies), so a discussion can be read later — in a review step, for example — and task-specific notes attach to the process thread rather than scattering. Not designed.

---

## 14 · Task work, completion, scale and mobile

### 14.1 Start opens the work · **Decided**

Start — or resume — does two things at once: it moves the task to Active and it opens the task work screen. It is not a bare state change that leaves the person standing in the list. The work screen itself has two parts and is yet to be designed: a **dynamic** part (the fields and decisions this particular transaction needs) and a **static** part (the process context, the documents, the discussion). Whether start opens the work in the same tab or a new one is a personal preference, deferred with the list-scale question (§14.5).

### 14.2 Work stealing is legitimate · **Decided**

A manager may start or reassign a task that belongs to someone else, and taking it makes them the owner. This is **work stealing**, and operations depend on it: a manager taking a phone call may need to process a task immediately themselves, or hand it to whoever is free. It is not a mistake to guard against. Only a participant starting a colleague’s work is blocked.

### 14.3 Waiting and Available are a manager distinction · **Decided — a cue is owed**

Waiting (nobody eligible) and Available (open to take) render identically when unassigned. That is acceptable for a participant, who is never offered a Waiting task, but a manager needs to tell them apart — the reason a row is stuck is exactly what the manager is scanning for. The cue is owed; no state label is.

### 14.4 Finding a task in the list · **Decided — built**

The list carries a local filter over the process reference, the task name, the process name, the team and the owner, alongside the two scopes. Global search still jumps rather than filters (§3); this is the page-level filter, and it follows the Teams table’s pattern, sitting at the right of the table head. No row-count summary is shown. The “Completed” toggle sits in the table foot.

### 14.5 List scale — infinite scroll or pagination · **Open — deferred, needs further thought**

The Teams table assumed infinite scroll. This table is the **worst case**: highly dynamic, with contents that can change fast, so a scroll-through model and a live re-rank pull against each other. The common case, though, is not scrolling at all — a person works the top, opens a few cases in browser tabs and comes back, and most people never leave the first page. That points away from heavy paging and toward a **stable first page plus a way to refresh**.

Two sub-questions remain: whether a row updates its status in place (which means re-querying), and whether **start opens the work in a new tab** — a personal preference. Opening a case in a new tab means the list has to refresh on return, so a **refresh control sits just past the list’s search box** — search, then refresh, reading left to right. On the Inbox the refresh is nudged left so it lines up over the row’s leading control (start or pause), shortening the eye’s trip from refresh to start. It is built on both the Inbox and Teams list headers, and is a candidate common feature on every list. As always, never a scrollbar-inside-a-page — the failure §4.4 already rejected.

### 14.6 Referral · **Open — explore on the task work screen**

Two shapes: treat a referral as a reassignment (simplest), or keep the owner and run a referral *chain* in which the referred person can refer back. It changes the task work screen, so it is explored there.

### 14.7 Completion and close · **Decided — a process-task property; TODO**

How a close is confirmed is a property of the **process task**, not of the person: a task marks that it **requires confirmation of close**. Some transactions auto-close, some confirm, because the right behaviour depends on the transaction. This keeps a confirmation rare and earned — a time-poor person should not be asked to confirm the obvious. Completing from the list is still not offered (§13.8). Recorded as a **TODO** to confirm it lands when the process-task configuration is built (§9).

Rejected for now: *complete with undo* — the next activity may auto-start, so the window is nil; *preliminary close, then real close* — leaves partially-closed tasks in the state model; *an unconditional confirm* — tires the person who has already decided (“I told you once, get on with it”).

### 14.8 Why the estimate exists · **Decided**

Two purposes: it sets the expectation a person should work to, and it lets them self-manage — with a few minutes left, pick something small. The second also lets sustained over-achievement show. The calculation that produces it is complex and deliberately deferred (§9).

### 14.9 Concurrency · **Decided**

Two people taking the same task at once is an application concern, not a user one: the database transaction decides, and the person who missed gets a flash message. The error-handling approach is not designed. Real collision probability is low — tasks run in the order of minutes to hours.

### 14.10 Mobile · **Decided — deferred**

Staff and operations work on desktop. Mobile is for activity and reporting, and a little case work — mostly approvals by senior people, which can arrive through ad-hoc email or notifications, since Hamoni is not their main working surface. A mobile treatment is deferred, and bulk moving of many tasks is a mouse task (§9).

---

## 15 · The task work screen

### 15.1 It is the view variant · **Decided — the edit variant is deferred**

A task screen has two operational styles, and they are a maturity progression rather than a choice per customer: staff either keep Hamoni beside the core admin system and copy between them, or enter the information directly into Hamoni as process data that is pushed back to the core system later. The first is a **view**; the second is an **edit back into Hamoni**. Only the **view** variant is built (`task.html`); the edit variant is deferred.

### 15.2 Layout · **Decided**

The task name sits inline with the controls — title left, controls right — with the process summary in its own thin line above. Below that, two columns: the **claim information** on the left and the **documents** on the right. Nothing else competes for the first glance.

### 15.3 Discussion and the assistant are bands, not a rail · **Decided**

Operational people want the case in front of them first, so the discussion and the assistant do not occupy a permanent column. Each is a textual control that expands a band beneath the header, and only one is open at a time — the same shape as the conversation band (§6). They sit in the header to the **left of the task controls**, stay one click away, and are never removed. The discussion control carries a count, because a comment can be left on the case independently and the person needs to see that without opening it.

### 15.4 Documents · **Decided**

A tab strip selects the document; the viewer takes the **full width** of the widget. A side-by-side browser was rejected here: it stole width from the document, which is the thing the person is there to read. A process has one or a few documents, rarely more than ten, so a single row of tabs is enough. Documents are **immutable**: uploading adds a document, it never replaces or edits one. The viewer offers **Open in desktop**, handing the file to the application registered for its type. Inside Hamoni the viewer is a labelled placeholder until the platform detail is settled.

### 15.5 Controls match the Inbox · **Decided**

Pause, refer and complete use the same icon control as the Inbox rows (`.sctl`), so the vocabulary is learnt once. Refer uses an **upward curved arrow** — the concept is passing the task on to someone; a paper plane was rejected because it reads as mail. Pause toggles to resume; complete asks for confirmation because this task is marked as requiring it (§14.7).

### 15.6 The process summary bar · **Decided — resolved in §19.3**

The bar keeps the process name, its business key and the stage in one thin line. It **belongs to the process view**: its facts are process facts, not task facts. It stays on the task screen as a one-line context echo and a link, and it is authoritative on the process detail, where it also carries the process **state**. Settled in §19.3.

### 15.7 The main area is a dynamic canvas · **Decided**

Below the heading, the main area is not a fixed layout. The claim block and the document block are **widgets a process designer places**, and the document block is a single widget. This is what lets one task screen serve different processes without a bespoke screen each time.

### 15.8 Discussion and assistant are widgets too · **Decided**

The discussion panel and the assistant panel are widgets of the same kind, not only header controls. The default keeps them as controls that expand a band (§15.3), but a process designer may **place either on the canvas** when a process calls for it — most obviously the assistant, for a case where it is the prominent working surface. The chat-style variant (§15.9) is exactly that placement.

### 15.9 A chat-style variant · **Decided**

A second variant (`task-chat.html`) keeps the classic chrome and changes **only the claim widget**, turning it into a conversation: a summary of the claim, the person’s question, and the assistant’s answer. Because the assistant is the main widget here, its header band control is dropped — the same placement rule as §15.8 — while the discussion control stays, the documents widget is unchanged, and the deep link to the claims system moves onto the conversation. The classic screen and this one are the same screen with a different claim widget, which is the point of the canvas.

### 15.10 Header controls mirror the canvas · **Decided — the task screen designer’s logic**

The discussion and assistant header controls **always exist by default**. The single exception is placement: if the matching widget already appears on the page, its header control is **not** rendered, so a feature never has two entry points.

- The assistant on the canvas → no Assistant control in the header; the conversation *is* the assistant. The chat variant is this case (§15.9).
- The discussion on the canvas → no Discussion control in the header.
- Nothing placed on the canvas → both controls are present, each expanding a band (§15.3).

This is one rule, not a per-process choice about the header, so the same feature always behaves the same way. The outcome is the same as “always show the control, and place the widget elsewhere”, but it keeps a single door to each feature.

---

## 16 · The process view and who may see it

### 16.1 A process view is linked from the task · **Decided**

Both task screens link to the **process view** — the whole process, its current step and its audit log — from the process bar, on the process name. It is a plain link, and there is no confidential content on that screen.

**On the task list it cannot sit on the row.** The row already opens an in-place preview, so a second gesture on the same target — expand *or* navigate — would be ambiguous. The link therefore lives in the **expanded row’s static part** (never the dynamic region), and is styled as a button with an outward arrow so it reads as navigation rather than another expansion. The mismatch between “click to expand” and “click to navigate” is a **known awkwardness**, accepted because the alternative — a row that navigates — breaks the list’s own gesture.

### 16.2 Who may open a process · **Decided — shape**

Transparency is deliberate: seeing that every action is recorded keeps people honest, so the view is opened to anyone with a *current* relationship to the process rather than hidden from regular users. But not everyone may see every process. Access is held by:

- the **process owner**, always;
- the **manager of a team** a task is currently assigned to;
- the **person** a task is currently assigned to or being worked by.

Access **ends when that person or team finishes their work**, so a later step that may be confidential is not exposed to them. Some teams — customer services is the example — may hold **persistent** access regardless.

### 16.3 Where the setting lives · **Decided — shape**

It is a **process configuration with an organisation default**:

- **organisation default** — process visibility is *owner only*, or *owner and the current task actioner*;
- **process override** — optionally add specific **teams with persistent access**.

**Open** — whether the process override may only widen the organisation default, or may also tighten it.

### 16.4 The list scopes · **Decided**

The processes list (§20) has two scopes, and they answer different questions:

- **My processes** — a **current relationship**: I own the process, I am the **actioner** of a task on it, or I **manage the team** a current task is assigned to. This is the Inbox's question, at process level.
- **All processes** — everything visible to me: my processes, plus processes owned by a **team I belong to** (sight follows the container, §2.3), plus anything covered by a **persistent-access** grant.

**Sight is not the same as opening.** The list may show a row whose detail §16.2 does not open to me — team membership gives sight of the *process*, while the current-relationship rule governs the *detail view*. A row I can see but not open is read-only and offers **Request access**. This keeps §2.3 and §16.2 consistent rather than letting one silently override the other.

---

## 17 · Pend — stopping a process for the outside world

### 17.1 A pend, by definition, is external · **Decided**

A process follows its defined flow. A **pend** stops normal execution because something **outside the organisation’s control** must move: a missing document, the customer’s approval, a third party or an external system. If the organisation could resolve it, it is not a pend — it is a referral, an escalation, or a step that should be modelled into the process in the first place. This boundary is the whole point of the concept.

### 17.2 A pend is a process state, reached from a task action · **Decided**

It appears as a task action but **escalates to a process state**. Once pended, the task’s own state stops mattering and the task **reverts to Assigned**, leaving the Inbox — the person is no longer working it, and the process cannot move forwards.

### 17.3 The clock · **Decided**

Time lost to a pend never counts against the person or the team; it is outside their control. There are two modes, chosen at process **design time**:

- **stop the clock** — the process SLA is paused and extended by the pend duration;
- **don’t stop the clock** — the SLA keeps ticking and the process may become overdue even though nobody here can move it.

### 17.4 Pend is not an exception · **Decided**

An **exception** is a technical fault — an invalid state, a failed integration. It must be resolved, and it **never stops the clock**. Exceptions belong to the process designer. A pend is about the outside world and does stop the process. The two must not be conflated, because they carry opposite clock behaviour.

### 17.5 A pend is a confirmed action with a free-text reason · **Decided**

It stops the process and lifts the team’s obligation, so it is confirmed, not a quick toggle. The **reason is free text**, deliberately: pends are external and varied, and a fixed list would be a modelling burden that never fits. Quality work reduces pends over time, and a slightly painful action is the point — it should not be easy to misuse.

### 17.6 Pend actions · **Decided**

A pend may start other processes alongside it, defined in the **process designer** as **pend actions**. Each is a **quicklink**: it names a process and the data or documents to pass from this one, and **zero or more** are selectable at pend time. It is expected to be a short list. Starting an *arbitrary* process from a quicklink at any point in a process is deferred (see §9, “Quicklink to start any process mid-process”).

### 17.7 How it comes back · **Decided**

The resume method is **one of a mutually exclusive set**, defaulting to Manual:

- **Manual** — the default, and deliberately broad: it covers a person *and* any external asynchronous action, including an API call or signal.
- **Time** — an explicitly chosen **Relative** or **Absolute** value. Relative combines a number of **business days** with **hours and minutes** (see the grammar below); Absolute is a fixed date and time. The two are mutually exclusive, so only one can be in force — there is no precedence to infer from what has been typed. Its configuration is folded away until Time is chosen.
- **After subprocesses** — resume once all of the started pend actions are complete; **requires at least one** pend action to be selected, and is unavailable otherwise.

(Cancellation while stopped remains possible, by a person or an external system.)

**The time grammar, to remove the ambiguity.** Most of the time it is a set number of business days only; the extreme case — “3 business days, 5 hours and 2 minutes” — must still be expressible. The rule:

- **days only** → resume at the **start of business** on the chosen business day;
- **hours or minutes given** → count **from the start of business of the days, plus the hours and minutes from that point**. This keeps the *days* in business-day terms and the *hours and minutes* absolute, rather than mixing midnight, now, and start of business. (The alternative — hours and minutes from *now* — is noted in §17.11.)
- **a specific date and time** → the **Absolute** mode, taken literally, with no relative value in force beside it.

The screen always shows the resolved effect — “Resumes at the start of business on Tue 6 Oct” — so there is no doubt about what was chosen.

This is one control of the same family as the state cluster — a single choice read as a list — and the row treatment is the same idea, though the exact widget is a UI decision. BPMN does not fit cleanly: a pend can happen asynchronously at any time rather than at a specific modelled event, though it most often starts from a human or agent task.

### 17.11 Open — the time control

The behaviour is settled (relative vs absolute, and the grammar above), but the **control itself is not intuitive** and needs further work as UI design. The relative/absolute choice is modelled as an explicit switch in the wireframe; the exact treatment — how a duration of days, hours and minutes reads, and how the resolved effect is presented — is deferred to the widget pass.

Also open:

- **Hours-and-minutes anchor.** §17.7 anchors them to the **start of business** of the resolved day (days in business-day terms, time in absolute terms). The alternative, closer to the plain reading, is hours and minutes **from now**, with days still business days — but then a mixed value spans two different anchors, which is exactly the ambiguity being resolved. Needs one decision when Time is built.

### 17.8 Security is a fixed rule · **Decided**

No configuration: the **process owner**, the person the task is **currently assigned to**, or a **manager of that person’s team**.

### 17.9 While pended · **Decided — superseded by §13.4 update**

Pended work is **hidden from the Inbox** by default. **The task stays `Assigned` when its process is pended** — the pend is a *process* state, not a task state — so panding is **not** a people/scope filter. The earlier “Show pended” scope note under the header is **removed**; pended rows are surfaced through the Inbox foot’s **`Also include`** control instead (§13.4): `Completed | Pended`, both off by default.

When shown, pended rows sort **between the live work and the completed** — never mixed into the queue someone is working. A pended row’s second line carries the **process state** instead of the owner — “Pended · waiting on the customer’s approval · until the customer approves” — because that is the thing worth reading, and its action becomes **Resume process** rather than Pend. The pended view is where the owner resumes it by hand.

### 17.10 Where the pend lives · **Decided**

Its own screen (`pend.html`) — the decisions are process-level, so a task-row toggle cannot hold them. Its process name links to the process view, and the process view should carry a **back-link to the pend** once it is designed. It is linked from the **task screen**, from the **process overview** once that exists, and in the Inbox from the expanded row **beside “View process”**. The Inbox row itself cannot carry it, for the same reason the process link could not (§16.1).

---

## 18 · The process detail screen — brief for the next session

`process-detail.html` exists today but predates §13–§17 and is **not** the design we want. It is the next screen to build, in a **new conversation**. What is already settled and must be honoured:

1. **It is the process view linked from the task.** The task screens link the process name to it (§16.1); the pend screen links to it too. No confidential content lives on it.
2. **Its access is scoped, not public** (§16.2, §16.3) — process owner; the manager of a team a task is currently assigned to; the person currently assigned or working it; access ends when that relationship ends. Organisation default (*owner only* / *owner and current task actioner*) with a process override that may add **teams with persistent access**. **Open** — whether the override may only widen the default.
3. **It carries a back-link to the pend** where a process is pended (§17.10).
4. **It shows the whole process** — steps and current step, the audit log, and status — which is what the task screen’s link promises.
5. **The process summary bar** on the task screen may move here (§15.6) — decide that when the screen is designed.
6. **The dynamic canvas idea** (§15.7) is about the *task* screen; the process screen is its own composition. Do not assume the same widget model.
7. **Pend** is a process-level state (§17) and this screen is the natural home for the **pended** facts and the **resume** action.

Reference for tone and structure: `design-decisions.md` §1–§17, and the existing screens — the task screens (`task.html`, `task-chat.html`) for the process bar and link, `pend.html` for the process-level treatment, `teams.html` for the table idiom.

---

## 19 · The process detail screen — information architecture

§18 was the brief for this session. What was settled while designing `process-detail.html`.

### 19.1 It is the process *instance*, not the definition · **Decided**

The screen shows **one running case**: its current step, its steps, its audit log, its owner and its state. This is what the task screens, the Inbox's expanded row and the pend screen link to (§16.1, §17.10). The **definition** view — the steps as designed, versions, who may start, run history — is a different screen and is not this one.

**Open** — the Processes catalogue opens this screen by name (`?name=`), but a catalogue row is a *definition*, so it needs a definition screen. Until that exists the catalogue link is a known mismatch; §18.6's "the process screen is its own composition" holds.

### 19.2 The four questions, in order · **Decided**

The screen answers, top to bottom:

1. **Where is it now** — the current step, and its position in the process.
2. **What is it waiting on** — the current step's state: a task and its owner, or a pend and what holds it.
3. **Who owns it** — process owner, owning team, project and definition version.
4. **What has happened** — the audit log.

Access is a fifth, permission-gated region (§19.5). This order is the answer to "what a person actually needs when they open a process".

### 19.3 The process summary bar belongs to the process view · **Decided** (resolves §15.6)

The bar's facts — process name, business key, stage — are **process** facts, so its canonical home is here. It stays on the task screen as a one-line context echo and a link, but the process view is where it is authoritative; here it also carries the process **state** badge. The bar does **not** repeat the Pend control on the process view: the process action sits with the waiting-on facts and in the page action, so the same action never has two doors in one viewport.

### 19.4 The page action is the process state action · **Decided**

**Running → Pend process** (secondary: a process action that should stay a little painful, §17.5). **Pended → Resume process** (the one solid button in the header). This supersedes §3's "Process detail → *Run now*": running a *definition* belongs to the definition screen, not to an instance that is already running. The two controls are mutually exclusive, so the header never holds two buttons for one action.

### 19.5 Access scoping is shown, not assumed · **Decided**

Every viewer gets a one-line **basis** strip: why they can see this process and when that access ends — "you are working the current step; access ends when it completes". The **owner** additionally sees an access panel listing every current accessor with their basis and expiry: the owner, the current actioner, the manager of the team the task is assigned to, and any persistent-access team (§16.2, §16.3).

Access is **binary** — you have it or you do not — and ends when the relationship ends, so the screen never shows a partial process. Non-owners do not see the access panel at all; they see only the basis strip. This is how the §16 scoping changes what is shown.

### 19.6 Pend is a first-class part of the screen · **Decided**

A pended process shows, in the waiting-on region: the reason in the process's own words, who holds it, when it was pended, the resume method and its resolved effect, and whether the clock is stopped (§17.3). It carries the **back-link to the pend screen** that screen expects (§17.10, §18.3), and the **Resume** action. Running and pended are **one composition with the state-specific region swapped** — not two screens.

### 19.7 The step list is the whole process · **Decided**

The steps region lists every step with its type, actor, state and time, current one marked. It is the detail behind the bar's pips, and it is why the screen can promise "the whole process". System steps are shown as plainly as human ones, because a person needs to see what is machine work and what is someone's.

### 19.8 The audit trail is the record · **Decided — built**

Immutable, chronological, earliest entries behind **Show earlier**. Three columns: **date & time**, **actioner**, **action**, written as simple sentences ("Created task 'Review claim diagnosis' and assigned it to Finance Control"). An **agent** is identified by its connection, with a badge; anything the system itself runs is recorded as **System**. The **filter is removed** — it was All / People / System / Access, and on a trail this short it was a control with no payoff. A **Download** button exports the whole trail. It is visible to everyone with access — transparency is deliberate (§16.2) — and it is half the reason the screen exists alongside the current state.

**Stage moves are audit entries, in both directions.** Moving between stages is recorded, and a process can move **back** a stage — sent back for review: if it sits on a task in stage 3 and then transitions to a task in stage 2, the process stage returns to 2.

### 19.9 Still open

- Whether the process override may **tighten** as well as widen the organisation default (§16.3).
- The **definition screen**, and the catalogue link that needs it (§19.1).
- Whether the process link should carry the **case reference**, not only the name, so the screen can be addressed by instance (`?name=…&case=…`).
- Whether a viewer whose access derives from a step should see the **full** audit log or only from their involvement onward. Leaning: full — the log is the point, and access ends with the step.

### 19.10 The layout, rebuilt · **Decided — built**

The screen was rebuilt around the panels the owner named. **Nothing sits above the process name.** The process bar is the header (§19.3), and the assistant and the process actions sit **inline with it, on the page background** — the controls card is gone. The order is:

- **Process header** — the process bar (name, key, stage) and the standing description, with the **Assistant** and the actions (running → *Pend* + *Cancel*; pended → *Resume* + *Cancel*) inline beside it. The assistant expands a band (§15.3).
- **Process header block** — §19.11.
- **Process summary** — the designer's dynamic widget, the same key / value / description grid as the task view and the list expansion (§20.8). It carries **no process-design link** — the definition is reached from the project (§19.11).
- **Active tasks** — the work in flight, each row carrying its own action (§19.13).
- **Process diagram** — a placeholder box.
- **Process data** — variable name and value only; **editing needs its own screen** (name, type, description, a control matching the type) and is a **TODO**. The reference types are **string, number, money, boolean, array, json, table and object**; advanced mode shows the exact type (for example `java.time.Instant`). **Money** is a currency and an amount together, so a value reads "1,480.00 EUR" rather than a bare number. An object type has no direct editor — it is edited through its **serialised form**.
- **Audit trail** — §19.8.
- **Documents, then discussion** — documents first because they are the more relevant case context. Add, remove and view; never rendered inline, so viewing hands the file to the OS (§15.4). The discussion is a fixed card, not a designer-placed band.
- **Design notes** — the foot, carrying the access criteria (§16.3) rather than a live panel.

**Removed from the earlier build:** the **process controls card** (the actions moved inline), the **"where it is now"** panel (it repeated the stage, the summary, the audit and the tasks; the only unique fact was the pend reason, deferred with the resume work), the **"who owns it"** panel (process-designer information — the project and version matter here and move to the banner), the **"who can see this"** panel (irrelevant on the instance; its design criteria move into the design notes), and the **exception-handling** card (deferred — §19.16).

### 19.11 The process header block · **Decided — built**

A compact **banner directly under the header** carries the process facts, in a fixed order for every process. It has **three blocks**: **time**, **state**, **project**. It is deliberately **not** at the foot: the audit trail grows without bound, and metadata below it drifts further away the longer a case runs.

- **Time** stacks two facts: **started** over **due** while the case runs — pended, exception or active — or over **completed** / **cancelled** once it closes. The second label follows the state, so a finished case reads *started* over *completed*, not *due*.
- **Due** is a countdown in the short form the other screens use ("1d 3h 21m"), **not** a business-day target.
- **State** moves here **from the process bar**, because a process has a state to *manage* where a task's is only tracking (§19.17). Three blocks give the state block the room a message needs. The bar keeps the name, the business key and the stage.
- **Project and version are one fact** — *"Claims handling (v4)"* — because both resolve to the same item, so one deep link carries both. The definition is reached **from the project**, so the process summary carries no process-design link.

**The clock mode is not on the screen.** "Clock stopped while pended" is too technical for the reader; it is a process design-time setting (§17.3) and lives in these notes, not on the banner. This replaces the earlier "who owns it" rail: **owner is process-designer information**, while project and version are the metadata a reader wants at a glance, and a project may carry more than one process.

### 19.12 The title is not repeated · **Decided**

On the task screen the process name and the task name are different, so both are shown. On the process view they are the same thing, so the process bar carries the name as the page's **single `h1`** and the content does not repeat it — the content leads with the **description** instead. The description is a **standing description of the process**, with **no dynamic information** in it ("a minor outpatient claim for processing claims under €3,000 from a panel clinic"), unlike the task screen, where the line under the title tells the person what to do. The **breadcrumb's last crumb reads `View`**, not the case name, for the same reason — the name already leads the page.

### 19.13 Active tasks carry their own action · **Decided**

The list can hold more than one task, so **Open sits on the task row**, not in the panel header. A **script or system task has no work screen and therefore no Open link** — only a person's task can be opened.

### 19.14 The actions use the task vocabulary · **Decided — built**

The process actions are the task screen's **icon controls** (`.sctl`), not text buttons, so the vocabulary is learnt once: **resume** is the play mark (the one primary control), **cancel** is the cross, **pend** is the pause mark. The **assistant** stays a text control because it is not an action. This supersedes the earlier text buttons on this screen.

### 19.15 Cancel and resume become their own screens · **Decided — TODO**

**A state change is not cheap** — it needs a confirmation — so process states get a full surface rather than an in-page control. **Cancel** is its own screen because it is non-returnable. **Resume** is low-impact in principle (it only clears the pend), but a manual resume may need a **reason**, and those reasons are as varied as pend reasons — free text, for example "signed claim form attached". Both are built as **copies of the pend screen** (§17), which is the example; the in-page cancel dialog is retired. **Not built now** — recorded as a TODO, to be built alongside the reconsidered pend screen.

### 19.16 Deferred to the next pass

**Exception handling, and how a pend and its resume are modelled**, are the next pass, together with the cancel and resume screens (§19.15). The exception-handling card was removed rather than left half-described.

### 19.17 The state block carries its message · **Decided — built**

The **state** carries a one-line **summary**, with the **raw message** one click away. For a **pended** process the summary is the pend reason — this recovers the reason that left the screen when "where it is now" was removed. For an **exception** it is the exception line, and the raw message is the **underlying platform text** — possibly technical and outside Hamoni's own vocabulary ("invalid object id") — because that text is what actually resolves the fault, even where the designer has global exception handling. The raw text is shown as **monospace text**, not prose, so it is not mistaken for a Hamoni-authored message.

---

## 20 · The processes list

`processes.html` was the definition catalogue; it is now the **list of running cases**, and the **gold-standard table** the Inbox and Teams reconcile to (§24). What was settled.

### 20.1 It lists running cases, not definitions · **Decided**

The page is the operational entry point: a list of **process instances**, ordered nearest-SLA first, with the state controls on the row. The **definition** view — the steps as designed, versions, who may start — is a separate, unbuilt screen (§19.1). This supersedes the earlier catalogue screen, whose "last run" and "who can start" columns belonged to the definition.

**Columns** — process (`id · name`, over the **reason it is in the list**), due in, started (local time), state, and the state controls.

**The screen's purpose is diagnosis, not overview** (§20.13). The default sort and the default scope both follow from that, and both are still being settled.

**The second line is the reason, not the owner.** §16.4 made scoping a reason-based idea, so the row states the reason directly: *You own it · You started it · You are working on it · In a team you belong to*. It changes with the active filter, which is what makes the scope legible while you are using it. **Open** — the wording: "You started it" reads as an event rather than a state, and "working on" needs settling against the people-first vocabulary (avoid *staff*, prefer *people-led*).

### 20.2 Two scopes · **Decided** (see §16.4)

**My processes** (a current relationship) and **All processes** (everything visible). A visible-but-not-openable row is read-only and offers **Request access**.

### 20.3 The My-processes filter · **Decided**

A **multi-select dropdown** on the scope, prefixed **Include**, using the same component as the foot's **Show** control (§20.11): **All** (default) · I own · I started · I'm working on. *I started* is the run's initiator, which is not the same as the owner. Multiple selections union. Collapsed, it reads the one option's name, or **Several** when more than one is on.

**Open** — whether a starter keeps detail access after their other relationship ends; leaning yes, because starting is a recorded act. **Open** — merging the scope tabs (*My processes* / *All processes*) into this same dropdown, so one control expresses the whole filter; considered and deferred.

### 20.4 Subprocesses do not inherit ownership · **Proposed**

A subprocess started from a process I own is a **separate process with its own owner**; ownership does not cascade. It appears in My processes only while a **live link** holds it to me — while the parent is pended waiting on it (§17.7) or while I hold a current relationship through it. If it has no owner, its custodian is the parent's owning team. **Open** — whether the parent's owner should hold standing visibility of its subprocesses regardless of the live link.

### 20.5 Order and sort · **Decided**

**Default order is newest started first**, and the columns run **Process · Started · Due in · State · controls** — started before due in. Start time is the primary axis because the SLA is a **constant offset from start**: ordering by start nearly mirrors ordering by due, while remaining correct when SLA targets differ between processes. Newest first, so the freshest cases lead. Columns sort on click; pended and finished rows have no live due time and sort last under Due in.

**A finished case freezes its clock.** Completed and Cancelled show **no due value** — the straightforward countdown no longer applies, because it would keep moving after the case stopped. The column shows *—* (muted) for finished work and *paused* for pended; only live work shows a countdown. Due in is a touch wider to hold *paused* and *2h 10m* on one line without wrapping.

**The column is labelled "Due in", not "Time to SLA".** It reads as a countdown — "35m" when there is time left, "40m ago" once it has passed — rather than a remaining-time metric. This deliberately creates pressure, and that is acceptable **because the process list is a monitoring surface**: it is read by someone watching the process, not by the person doing the work. Pressure is the point on a monitoring view; on the task view it was removed for exactly the opposite reason (§12.4).

### 20.6 State controls · **Decided**

**Active → Pend** (opens the pend screen, §17.10) and **Cancel**. **Pended → Resume** (immediate, no confirmation — resuming is the safe direction) and **Cancel**. A read-only row carries no controls. The controls are the shared `.sctl` icon cluster, so the vocabulary matches the Inbox.

**Pend wears the pause symbol**, not a clock: the process is *paused*, and the same symbol reads as the Pended state badge. A clock said "time is passing", which is the opposite of what a pend with a stopped clock does (§17.3).

### 20.7 Cancel · **Decided — shape, screen open**

Cancel is a critical confirmation that takes a **reason** and cannot be undone; it is the destructive direction, so it is confirmed where resume is not. The wireframe uses a confirm-and-reason dialog. Whether it should become its own screen, like pend, is **open** — the reason it might is that a cancellation, like a pend, is a process-level decision worth a full surface.

### 20.8 The expanded row · **Decided — built**

The expansion shows the process's **Dynamic area** — renamed from *default summary view*. It is the same summary the process's tasks use, overridable at task configuration level, and it is the region a process designer fills; the name lines up with the **dynamic canvas** on the task screen (§15.7).

**It is a key / value / description widget.** Each field renders as the key above the value with the description in the meta line beneath — the same `.exp-facts` grid the Inbox expanded view uses — rather than a label-left list, which left the values bunched in the first third of the box. The fields flow left to right and fill the width of the box: **three across** at desktop, **two** below ~980px, and a **single column** below ~640px. For the claim case the six fields are the claim facts from `task.html` (claim number, insured, policy, treatment date, claimed, received), figures sample data.

**The Dynamic area is marked as a region.** It carries a 1px dashed border mixed from the accent token (`color-mix(in oklch, var(--accent) 55%, var(--border))` — never a raw hex), the same "content not final" convention as §10.1's wireframe zone.

**The static "Process information" block is gone.** Owning team, process owner, running version, started and the current-tasks list were **removed from the expansion on the owner's direction**, superseding the earlier three-column accountability grid and the earlier call for current tasks in the static area. Those facts duplicate the process detail, which is their canonical home (§19.2, §19.6), and the current tasks are the process detail's step list, not the list expansion. The action row — the inclusion note plus **Open process** — is what remains beneath the Dynamic area.

**One curated first row.** `CLM-2026-44871 · Outpatient minor claim` — the same instance `task.html` works — is seeded as the newest start so it leads the default order and layout review always opens the same case. Its six facts are the claim facts; figures are sample data (§10.6).

### 20.9 Export · **Leaning**

Export moves off the page action onto the **table header, beside refresh**, as a download icon. It is a basic data dump — column control is not offered, so it is not a real reporting surface. **Activity** is the better place for a detailed export, and the download control may open a dedicated **download-configuration** screen. **Open**.

### 20.10 The page action · **Decided — destination open**

**New process** stays as the one solid page action. The page it opens needs a **significant redesign** and is recorded for a later pass; the action itself is right.

### 20.11 The state filter at the foot · **Decided**

The foot carries a **Show** control — a multi-select dropdown, not a row of segmented toggles, because the toggle row broke as states were added. It lists **Active** (selected by default), Pended, Completed, Cancelled and Exception. Collapsed, it reads the single selected state's name, or **Several states** when more than one is on. *Several states* is a placeholder; the word still needs work. The control stays open while choosing, and closes on an outside click or Escape.

### 20.13 The screen is a management/diagnosis surface · **Decided — purpose, not yet built**

The process list is where someone goes **because something is wrong**, not to watch work flow. Seeing how things are running is served by **Activity** and analytics. Three usage profiles, in frequency order:

1. **Diagnose** — "which processes need attention?" Exception, pended, and overdue-live. This is the primary case and should be the screen's identity.
2. **Find** — "find that specific case" (a complaint, a reference, a late process). Search-first, not browse.
3. **History** — "what happened on this finished process?" Rare, always triggered by something specific; a bounded time window or a drill-in, never a browse of the completed population.

**This corrects an assumption inherited from the Inbox.** On the Inbox, order-by-relevance is right because the person is working a queue. Here it is not: the case that needs attention is identified by **state**, not by proximity to the SLA. A nearest-SLA process is merely near a threshold; it is not necessarily the problem.

**Consequence, still open** — whether "needs attention" is the default **saved view** inside one table, or the **whole screen** with the broader list demoted. The default sort has since been set to newest-started (§20.5) as the first step of this reframe; the default **scope** and the "find" entry point are not yet decided.

### 20.14 Still open

- Whether a starter retains detail access (§20.3).
- The wording of the row's inclusion reason — "started", "working on" (§20.1).
- Merging the scope tabs into the Include dropdown (§20.3).
- A better word than "Several" / "Several states" for a multi-selected dropdown (§20.3, §20.11).
- Standing visibility of subprocesses (§20.4).
- The export and download-configuration surface (§20.9).
- The create/start-process page redesign (§20.10).
- The search model — prompt box versus structured panel, canonical query, and whether exact-match lookup survives (§22).

*(Resolved: the expanded-row cleanup — the Dynamic area replaces the default summary view and the static Process information block is removed — §20.8; and cancel is its own screen, like pend, not a dialog — §19.15.)*

---

## 21 · Organisation locale settings — recorded for later

**Recorded now, designed later.** An **organisation-level configuration** for locale. The **country** is the primary setting; everything else is derived from it and then independently overridable:

- **Country** — the one setting the person actually picks (e.g. Japan)
- **Language** — derived from country (Japanese)
- **Timezone** — derived from country (Japan Standard Time)
- **Date format** — derived from country (`YYYY/MM/DD` for Japan)
- **Location** — also drives number and currency formatting, and the default business calendar (§4.16)

**The country derives the rest; each derived value is independent.** Selecting Japan sets Japanese, JST and `YYYY/MM/DD` together, so the common case is one choice rather than four. But every sub-component can then be changed on its own — a person in Japan may prefer English — without disturbing the others. This is the same instinct as §4.13a's most-specific-wins: a sensible default the user rarely has to think about, with an explicit override where the default is wrong.

**Every setting is overridable per person.** The organisation value is the **default**, and an individual may set their own country, language, timezone, date format and location on their profile. This is the organisation-default-with-a-personal-override shape the project already uses for advanced mode (§7) and multiple active tasks (§13.7): wide enough to be set once, narrow enough for the person it actually affects. Nobody who is happy with the organisation's settings ever sees the override.

It is an **organisation** setting, not a per-screen control, which is what makes the "time in your local time" claim on the processes table (the *Started* column) well-defined: the label reads in the viewer's timezone, while the organisation's timezone is the reference the business runs on.

**Open** — where the organisation settings live (Organisation configuration), where the personal override sits (the user menu, alongside advanced mode and dark mode — §7), and how country/location relates to the one-country-per-team calendar rule (§4.16). Added to §9.
---

## 22 · Search: the prompt box and the structured panel — approach agreed, not built

The table's filter is rethought as **one query with two ways to compose it**, not two competing filters.

- **The prompt box is the entry point.** Its prompt is *"tell me what you're looking for"* — plain and warm, and it deliberately avoids *search*, which reads as a keyword box. It accepts a sentence ("all minor claims between Monday and Wednesday for Phillip Yu"), a phrase ("everything with an exception"), or an exact reference.
- **Clicking the box reveals the structured panel** — table-specific fields (date range, status, owner, and so on). Filling the panel is the fast path; typing is the powerful but slower path. **The structured form is the default and the optimisation**, because a structured query is always cheaper to execute than a parsed one.
- **Enter runs the query and collapses the panel to a summary sentence** — "Showing all minor claims between 14 and 16 Sep for Phillip Yu" — which includes the **active scope** ("in my processes…"). The sentence is the view's self-description, and it doubles as the query's reviewable form.

### 22.1 Rules that keep it honest · **Decided**

1. **The structured query is canonical; the text is a projection of it.** Typing is a fast way to *fill the same form*, not a parallel query — on Enter, the text resolves into the fields and then *is* the structure. This is the only thing preventing two disagreeing sources of truth.
2. **Unparsed text is surfaced, not dropped.** If part of a sentence has no structured equivalent, say so ("I didn't understand: *slow*") rather than silently ignoring it. Same principle as showing permissions rather than hiding them (§6).
3. **The summary sentence carries the scope**, because the same phrase means different results in *my processes* versus *all processes*.
4. **Apply is deliberate.** Enter commits; nothing re-renders mid-interaction — which also retires the moving-control defect the table had.
5. **The filter is one way or the other, visibly.** The prompt never runs alongside a filled panel as a hidden second filter; the two compose into one structure.

### 22.2 What stays · **Decided**

- **Column sorting stays** as-is — it orders the result set, and is orthogonal to the query.
- **The "My processes / All processes" scope stays**, even though it duplicates part of the query model, because it is a high-frequency shortcut and consistency across Inbox / Teams / Processes is worth more than purity there.

### 22.3 Mobile · **Decided — the shape resolves it**

The panel is an **in-place expanding form, not a popover** (§6's "pushing rather than overlaying"). That is what makes it work at narrow widths: a stacked form has no viewport to fight, and structured entry is likely *easier* on mobile than typing precisely. No mobile-specific branch is needed.

### 22.4 Open

- Whether the prompt box **replaces** the keyword filter or **sits beside** it — i.e. whether exact-match lookup (an id) survives as a first-class behaviour or becomes one case of the prompt.
- Where the **date range** field sits, since it is also where the historical-window question (§20) lands.
- Whether the summary sentence is editable in place (edit the sentence to refine the query) or read-only with an edit affordance that reopens the panel.

**Built** in `processes.html` (v1 of the model): the prompt box reads *"I'm looking for"*; clicking it reveals an in-place panel with **Include** (I own / I started / I'm working on), **state** (Active · Pended · Exception selected; Completed · Cancelled off), a **start-date from/to**, and a **keyword**, with **Clear** / **Apply filter**; Enter applies and collapses to a summary sentence. The default scope is **my processes** (the three relationships) over **running** states.

### 22.5 Scope now carries three tabs · **Decided — one open question**

`My processes · Team processes · All processes`. **Team processes** = a process whose owning team is one the person manages (worked on in that team). It is recorded as **complicated to explain**, and the open question is whether it earns its tab or should be a *view within* My processes — the same shape question as §20.13's "needs attention".

---

## 23 · Organisation configuration — recorded from the personas pass

Two organisation-level questions surfaced while writing the personas (`personas.md`). Both are configuration concerns, not persona topics, and both are recorded for a later design pass.

### 23.1 Platform defaults · **Decided — resolves §29.1 (2026-10-06)**

The organisation-level control is **Platform defaults**: a three-stop selection — **collaborative · normal · secure** — that sets the default **view / edit / deploy** posture for every project, so teams do not each configure it. Sensible default, leaning **normal**. A per-project override, and additional teams that may edit/publish/view, live on the **project security card** — the individual controls sit behind **advanced mode** (§7). Full model in §29.1–§29.2.

This is where the personas' rule that *sensible defaults cut onboarding time* lands: a small number of organisation-level choices remove a large number of per-team decisions. It is the sibling of §21's locale settings — one setting, sensibly defaulted, overridable only where the default is wrong. Added to §9.

### 23.2 Team structure drift · **Open — needs careful thought**

People leave, teams are moved across, and reorganisations happen, so **Hamoni's team structure will drift from reality over time**. The consequence is concrete rather than cosmetic: teams are embedded in the processes (§2.1, §4.12), so a structure that lags reality quietly routes work to the wrong team or blocks a process that needs a manager (§4.13d).

This is a fact to design for, not a hypothetical. It must not become a wall of small chores — a manager editing team after team by hand. The leaning is a **deliberate route to reconcile drift**, including through the natural-language input (§6, §22), rather than per-team editing alone. Needs its own pass. Added to §9.

---

## 24 · The table pattern — `processes.html` is the gold standard

**Decided.** The Processes table is the **reference design** for the three list screens — **Inbox**, **Processes**, **Teams** — because it is the most complete: it carries every element the other two carry only some of. Where the three disagree, `processes.html` is the pattern and the other two are reconciled to it. The page carries a **Gold standard** note saying so.

**The elements that make it the gold standard:**

1. **Scope control** — a segmented control in the panel head (Inbox: My / Team inbox; Teams: My / All teams; Processes: My / Team / All processes).
2. **One query, two ways to compose it** — the prompt box *and* the structured panel, one structure (§22), never two competing filters.
3. **A summary sentence** — the view describes its own active query and scope in words, so the reader knows what they are looking at.
4. **Sortable columns** — clicking a column header sorts; the active sort is shown on the header.
5. **Shared row anatomy** — `<identity> · <name>` over the **reason it is in the list**, the time facts, the state, and the state controls.
6. **In-place expansion** — one row open at a time; the expansion is the summary plus the actions, never a second page (§3, §4.4).
7. **The shared state controls** — the `.sctl` icon cluster, so the vocabulary is learnt once.
8. **One solid page action**, with the table utilities (export, refresh) kept on the header as icon controls rather than as page actions.
9. **Persisted view state** — the chosen scope survives a reload (the prototype rule, §10.6).
10. **An empty state**, and the shared chrome (rail, topbar, bar) every screen carries.

**Consequence.** The Inbox and Teams tables should be reviewed against this list and brought into line, and any *new* list screen starts from `processes.html` rather than from a blank table. This does not freeze the pattern — a change to it is a change to all three.

**Anti-pattern — do not duplicate information between a table's collapsed row and its expanded view.** · **Decided** (2026-10-05)
The expansion is the *next* level of detail, not a restatement of the row. Anything already legible in the collapsed row — a fact, a state, a count — must not reappear in the expansion; if it appears in both, one of them is wrong. This is show → preview → act (§3) applied to the preview: the row is the summary, the expansion is what the summary cannot hold, and the page holds the rest. Found while designing Projects, where the row's deployment chips and the expansion's deployment block said the same thing; the expansion now carries only the packaged processes (§25).

**Open** — the two other tables are not yet reconciled; that pass is the follow-up.

---

## 25 · The Projects screen — what was settled

`projects.html` (the list) and `project-detail.html` (one deployment unit) were built in this pass. What was settled.

### 25.1 It is a list plus a detail · **Decided**

The list answers the four questions at a glance; the detail holds the depth. A project's packaged processes and its version history exceed the dozen-row ceiling the expansion rule sets (§3), so they belong on a page rather than in an expanded row.

### 25.2 The list reconciles to the gold standard, with two knowing differences · **Decided**

The list follows `processes.html` (§24): a scope control, one query composed two ways (the prompt box and the structured panel), a summary sentence, sortable columns, the shared row anatomy, in-place expansion and the shared chrome. Two differences, each with a reason:

- **The scope is `My projects · All projects`.** *My projects* is the projects the person **owns or holds the custodian account for** — a team is a participant, not a permission, so team membership is deliberately **not** a scope here. *All projects* is everything the person is allowed to view; that view rule is worked out with the security model (§25.5). Deployment posture is **not** a scope — it is a filter flag in the structured panel (draft ahead, behind an environment, not deployed, up to date) alongside an environment filter.
- **A row carries no state controls.** A project has no run state to move from a list — the `.sctl` cluster is the task/process vocabulary and does not apply. Movement happens on the detail (publish), not on the row.

**Row anatomy** — `<business area> · <project name>` over the **reason it is in the list** (*You own this project* / *You are the custodian* / *Owned by ‹name›*); a **Deployed to** column of environment → version chips (a dashed chip marks an environment behind the latest version); **Latest** (the highest published version, plus a `draft v«n»` tag when a draft exists); **Last published** (the date that version was created). The **Packages count** and the verbose `packages N processes · M teams` line were removed — the count is version-dependent and ambiguous, and the packaged processes belong in the expansion, where the collapsed row is not repeated (§24 anti-pattern). The `Updated` column was renamed **Last published** because "updated" did not say *what* was updated; it is the latest published version's date, not any edit.

### 25.3 Environment classification sits on a deployment · **Grounding agreed, home open**

The answer to §2.9's open question, narrowed: classification lives on the **deployment** — a version paired with an environment — because a project can be at different versions in different environments. It is not a property of the environment's name (a customer's "preprod" is often production) and not of the project. It may surface as a view on the project. The delivery guard keys on it and reaches **every** delivery, not only tasks. **Open** — the exact home depends on the versioning model and the infrastructure piece, which is not designed yet.

### 25.4 Business rules are deferred to the process designer · **Decided**

Business rules shared by a project's processes are not designed on the Projects screen. Two approaches are on the table: **upload-and-reference** a rule document, or **upload into the scope of a decision activity** so the rule is bound to where it is used. The detail carries a labelled zone rather than an invented editor.

### 25.5 Who sees Projects · **Open — settle as the list is reviewed**

Projects is the technical, advanced-mode view (§2.6). The working assumption is **advanced mode only**, with the rail item gated and the screen pointing at the user menu when advanced mode is off (§7). Whether the gate covers the whole screen or only its write actions is still open.

### 25.6 The process banner's project link opens the project · **Decided**

The process view's project field — "Claims handling (v4)" on `process-detail.html` — now opens the **project** (`project-detail.html?project=…`), wiring the link that was inert. The process **definition** is reached from the project (§19.11), and remains its own unbuilt screen.

### 25.7 The detail · **Decided**

The project view is: a **banner** (version · environments · **last published**); a **processes** list (process · owning team, showing team and project are orthogonal, §2.6 — and with **no forward link**, because running cases are version-specific); a **versions** list (linear and immutable, draft at the top, each published version showing which environments point at it, §2.8); a **business-rules zone**; and design notes. **Deployments moved to their own page** (§27.4), because they carry the real actions. The page action is **Publish draft**, confirmed in a dialog — publishing freezes an immutable version and increments the number, and does not promote anything by itself. The `Packages` banner block was replaced by **Last published**, which is defined.

### 25.9 Business area, and the version vocabulary · **Decided — display now, design later**

- **Business area** is a free-text grouping field on a project — “Claims”, “HR”, “Operations / Claims”. It is a collector, not a taxonomy; its authoring design is deferred. It is **displayed as a prefix**, so a project reads **“HR · Onboarding”** on the list and in the detail title.
- **Latest** is the highest **published** version. A **draft** exists when the project has been edited since that version was created, so an unpublished, higher number is waiting; it is shown as a `draft v«n»` tag, defined on hover and in the detail's version list.
- **Last published** is the date the latest published version was created — never “any change”.
- **No over-header.** The page-title eyebrow that repeated the rail group (“Design”, “Manage”) was removed on every screen; it duplicated the rail and added nothing. Section-level eyebrows (`anno`) inside panels remain.

---

## 26 · Versioning · **Decided — forward-only (2026-10-05)**

§2.8's "reversion is copy-forward" was challenged and reworked. The model is now settled: **one path forward, no branches, no manifest**. This section records the problem, the options that were rejected, and the rule that replaces copy-forward.

### 26.1 Rolling back and reverting are two operations · **Decided**

They were conflated, and that is where copy-forward fails.

- **Roll back — operational.** Point an environment at an earlier version. The content does not change, nothing is written, and it reverses by moving the pointer back (§2.7). This is the correct answer to *"an interdependent system failed, get back to a version that works"* and *"an unexpected bug, revert to a safe version"* — both deploy-only reasons, and both already served by the pointer model. Any version is deployable; no new mechanism is needed.
- **Revert — authoring.** Make earlier content the basis for future work. Copy-forward does this by publishing v2's content as v5, but it does **not** describe what happened: v5 is structurally a descendant of v4 while semantically equal to v2, so the forward narrative of v3 and v4 is lost, even though the versions remain. Across years and multiple maintainers that is not reconstructable.

The first reason to revert — *"a bug in a workflow, but the other workflows and business rules get erased"* — is really a **targeted fix**, and a whole-project snapshot cannot express it: reverting the project takes every process and rule with it.

### 26.2 Forward-only, with deploy-by-pointer · **Decided**

**Rejected — the manifest model.** Making a version a manifest of pinned component versions creates component versions that must then be linked into a package. It adds version dimensions where people hold one, and drags enterprise complexity onto the 80% path. **Rejected — branch-on-edit** (v2.1, then v2.1.1 when a save is not off the branch head): the same complexity plus a UI for it.

**Adopted — a single path forward:**

- **A published version is immutable and read-only.** It can be opened to inspect, never edited in place.
- **Going backwards is a redeploy, not an edit.** An environment points at the older version (§2.7). This one operation covers both deploy-only reasons — an interdependent system failed, or a bug landed in the latest — because nothing is written and it reverses by moving the pointer back.
- **Changes are made forward.** The head draft is the only editable thing. You fix the current workflow and publish the next version; **fix-forward is the default and the whole 80% path.**
- **Bringing content from an older version forward is manual.** The old version is read for reference; re-applying any of it in the head draft is a deliberate act, never a one-click restore. This is what removes the earlier flaw — nothing is silently reverted, so the interim versions' forward narrative is never overwritten.

The model is knowingly restrictive: it trades a rare, whole-project convenience for a model with no second line of history, no component linking, and no ambiguity about what a version contains.

### 26.2a Reversion-class conveniences are advanced mode · **Decided**

If a *restore this version into the draft* action or a *compare two versions* view is ever built, it is an enterprise, complex scenario: it sits behind **advanced mode** (§7), and it must say plainly that it replaces the **draft's** content — never history. It is not on the 80% path.

### 26.3 Stale references are a deploy-time check · **Agreed**

A rollback or restore can name a team, capability or reference that no longer exists — a team since disabled (§4.13b), a directory group that moved, a capability that was removed (§4.15). That is a **compile/deploy-time check**, not a design-time one. A version that fails it is not struck from history; it is marked **not deployable now** in the version list, with the failing references named.

### 26.4 The version list is required · **Agreed**

A project's versions need a list or visual showing, at minimum: the **number**, **when created**, **by whom**, a one-line **summary of what changed**, **which environments point at it**, and **deployability now**. `project-detail.html` carries a version list today with the number, date and environment pointers; **author** and **change summary** are owed. It is also the artifact that makes years-and-many-maintainers legible, and the surface where a version is opened read-only (§26.5).

### 26.5 Editing an old version · **Decided**

You do not edit a published version. Opening one is **read-only**. The only authoring path is the head draft, published as the **next** number — never a sub-number, never a second line. "Which version am I in" is therefore a matter of **visibility** — a persistent version context, a switcher, deep links — not of branching.

### 26.6 Scale — hundreds of versions, and where they are read · **Decided**

A project can accumulate a large number of immutable versions.

- The **head draft** is the only mutable version, and it exists only when changes have been made since the latest published version.
- The **default is the latest**; **going back is the exception**, and when it happens it is usually into the **recent** history — by time or by version — not deep antiquity.
- So the **version list is built for recency, not completeness**: the head and the most recent versions lead, and older ones are reached on demand, never printed hundreds of rows at a time.

### 26.7 The expansion shows versions, and the entry declares the mode · **Decided** (supersedes §26.6's earlier "the expansion does not change")

- **The project list's expanded view shows the project's recent versions** — the head plus the four most recent published versions — with a **Show older versions** control for the tail.
- **The head is always a draft row.** When a draft already exists it is that version ("Draft — the editable head"); when none exists yet, the row names the version editing will create — *v8* when production runs *v7* — and reads "Draft — not started", with the action **Start draft**. This is what keeps a published version from ever being presented as editable, and it keeps the latest published version visible in its own dated row. Opening the row starts the draft.
- **The process list was removed from the expansion.** A project's package is **version-specific**: a historical version may add or remove processes, so a process list sitting in the project list would silently mean "the latest version" — ambiguous the moment versions differ. The package is shown where a version is actually open.
- **The entry point declares the mode**, which removes the read-only / editable ambiguity:
  - the draft row carries **Edit** (**Start draft** when none exists yet) → opens the **editable** head draft;
  - each published row carries **View** → opens that version **read-only**.
- **There is no version selector on the project page.** The page is either the editable head or a read-only version, decided by the URL (`?project=…&version=…`), and is never switched in place. Opening a version shows a **Read-only** strip naming the version, its date and author, with a single **Edit latest draft** action. This is what avoids the "why are there no edit buttons?" confusion, and the multi-tab ambiguity of an in-page switcher.
- A specific version remains **addressable directly** and opens **read-only** (§26.5).

---

## 27 · Project settings — inventory and editability · **Open — compiled for the settings pass**

There was no consolidated list of project-level settings in this log; this section compiles what the decisions already imply, so the settings surface can be designed against it. This is also the answer to "which settings are project level" — settings that belong to the *organisation*, a *team*, a *process* or a *task* are called out at the end so they are not mistaken for project settings.

### 27.1 Identity metadata is editable, and is not versioned · **Decided (2026-10-05)**

The project's **name** and **business area** are editable. They are **identity metadata** — labels on the container — not part of the **package** that deploys. The package is the processes, the business rules and the environment pointers; editing a label is housekeeping and **does not create a version** and does not touch history. The split is the same one §4.13c-1 draws for a team: renaming is housekeeping, and what changes whether the thing *runs* is versioned, while what it is *called* is not.

- **Why there is no history impact:** the deployed artifact — what a user can reach — is the package, which does not contain the name or area. A terminology change after a reorg or new leadership is therefore safe to make at any time.
- **Consequence:** a rename updates the display everywhere it resolves — the project list prefix (`HR · Onboarding`), the process's project field, deep links — and **no draft appears**, because nothing in the package changed. Historical records keep the text they recorded, or resolve by a stable id, per §4.14's rule that audit never holds a live reference.
- **Description, owner and custodian are the same class** (decided, 2026-10-05): the name, business area and description are all editable, and owner and custodian are editable assignments — none of them needs a version, because none of them changes what runs.

### 27.2 The inventory

| Setting | Class | Status |
| --- | --- | --- |
| Project name | identity | editable, unversioned (§27.1) |
| Business area | identity | editable, unversioned (§27.1) |
| Description (the standing line under the title) | identity | editable, unversioned (§27.1) |
| Owner(s) | identity | **multiple**; the creator first, plus anyone added; an owner may remove themselves; deployment is owners-only (§27.7) |
| People / teams who may open or edit the project | access | **decided in shape** — a named set; process access is separate (§27.7) |
| Processes — add / rename / duplicate / remove | package (versioned) | a rename or change is in the draft; a remove is blocked while a dependent process references it, and a project keeps at least one process (§27.6, §27.8) |
| Business rules shared across the project | package (versioned) | **project-scoped, versioned, time-specific**; invoked through a process, never directly (§27.5) |
| Environments the project is deployed to | deployment | its **own page** now (§27.4) |
| Version each environment points at | deployment | pointer; promoting moves it, never copies (§2.7) |
| Environment **classification** (production or not) | deployment | on the deployment, chosen at deploy time (§27.4, §2.9) |
| Delivery guard — which deliveries reach a real queue | deployment | decided in shape (§2.9) |
| Publish a draft → the next version | versioning | §2.8, §26.2 |
| Restore a version into the draft / compare two versions | versioning | **advanced mode only, not built** (§26.2a) |
| Archive a project | lifecycle | **Removed** — there is no archive; a project is undeployed instead (§27.8) |
| Delete a project | lifecycle | **decided in shape** — the danger area; blocked while any process is referenced (§27.8) |
| Project-level assignment override (force work to a user/team in a non-production environment) | deployment | **superseded** — §2.9's classification holds non-production delivery, so a separate override is unnecessary |

### 27.3 What is *not* a project setting · **Decided**

Named so they are not moved onto the project: **locale** (organisation, §21), the **collaboration default** (organisation, §23.1), **advanced mode** (the person, §7), **work handling** — visibility, assignment mode, capabilities (the team, §4.15), the **calendar** (the team, §4.16), **clock mode** (process design-time, §17.3), and **process access** — the owner-and-actioner rule with its override (the process, §16.3). A project *packages* processes that each carry their own access; it does not re-scope them.

### 27.4 Deployments get their own surface · **Decided (2026-10-05)**

Deployment moved off the project overview onto its own page (`project-deployments.html`), because it carries the real actions — choose an environment, choose a version, classify the deployment, confirm — and those were making the overview busy. The overview keeps only a one-line deployment summary (the environment count and names in the banner) and a link; the page holds the table (environment → version · classification · delivery) and the **Deploy a version** action, whose dialog is **environment + version + classification + confirm**. **Undeploy** is a pointer removal, not a delete.

### 27.5 Business rules are project-scoped; templates are organisation-scoped · **Decided (2026-10-05)**

- **Templates are organisation-scoped** — global: everyone uses the same template. Where variants are wanted, they come from what a template accepts (a template that takes data directly versus one that takes a formatted body section), not from cloning the template per project.
- **Business rules are project-scoped.** They are **versioned and time-specific** — a rule changes over time and that change belongs to a version. A rule is **enclosed in a process wrapper**: it is not accessed directly, it is invoked through a process. The project owns the rules, the process invokes them, and the version pins both.

### 27.6 A process inside a project — rename, duplicate, remove · **Proposed — to confirm**

- **Can be renamed.** A process name is content in the package, not container metadata, so a rename changes the draft and produces the next version — unlike a project rename (§27.1).
- **Can be duplicated.** "Duplicate" copies a process as a starting point — the same reuse/fork idea the create flow uses (§6) — producing a new process in the project.
- **Can be removed**, subject to two checks: the project must keep **at least one process** (§2.5), and removal is blocked while another process **depends** on it (§27.8).

### 27.7 Project ownership and permissions · **Superseded (2026-10-06) by §29**

> **Superseded.** Project ownership is a **team**, and permissions come from **Platform defaults** with a per-project override — see **§29**. The individual-owner rule below is kept only as the record of what it replaced.

- **Owners.** A project has one or more owners. The **creator** is the first, as with a team's first manager; more can be added, and **an owner can remove themselves**. The idea is the same as team owners (§1.4).
- **Open / edit.** A set of **people and teams** may be named as able to open and edit the project. This is the project's own access; it does **not** govern the processes inside, which carry their own access (§16.3). A project setting may later become a **default** for its processes' access — noted, not decided.
- **Deploy.** Restricted to **owners**. Considered and rejected: anyone who can edit, and a separate "custodian" role for deployment — owners is simpler and matches accountability.

### 27.8 Lifecycle — no archive; delete is the danger area · **Decided (2026-10-05)**

- **Archive is removed.** It did not mean anything distinct; a project that should not run is simply **undeployed**.
- **Delete** lives in the project's **danger-area settings**. It is blocked unless **nothing references any process in the project** — the conditional-delete shape a team already uses (§4.14).
- **Reference checks gate deploy and undeploy too.** You may not undeploy, or deploy a package that **omits a process**, while a **dependent process** still references it. **Dynamic invocation cannot be checked** — a call made at run time is outside the check — so that limit is named rather than hidden.

---

## 28 · The project page — refinements

### 28.1 The header name is editable, and names need not be unique · **Decided (2026-10-05)**

The **one pencil** beside the title opens **name, business area and description together** — the first editable header in the app. The **business area is optional**: clear it and the title is just the name. All three are **labels**, not versioned content (§27.1), so saving them creates no version and shows no draft. **Two projects may share a name**: uniqueness is deliberately *not* enforced — the name is only a label, and a duplicate rule would add screen complexity for no functional gain. The ambiguity that can create is accepted.

### 28.2 The version sits at title level · **Decided**

Because the list opens a **specific version**, the page is version-scoped, so the **version is shown as a badge beside the title**. The three-bar banner then carries the **facts of that version**, not of the project: **Deployed to** · **Published** · **Delivery**. **Open** — for a draft, *Deployed to* is always "not deployed", which wastes a slot; what else belongs in the banner is unsettled.

### 28.3 Versions and deployments are one matrix · **Decided**

Versions and environments are the **two axes of one matrix** — version down the side, environment across the top, a filled cell where an environment points at a version — so they share **one screen** (`project-deployments.html`, titled **Versions & deployments**). The project overview no longer lists versions; it links to the matrix. Deploy and undeploy are **icons in the environment header**, not per-version rows. (This refines §27.4, which had split deployments off; the split is kept, but as *one* combined matrix screen rather than a deployment-only one.)

### 28.4 Publishing and deploying are two steps; the UX may join them · **Decided — shape**

The earlier idea — *a draft may be deployed to a non-production environment* — is **rejected**, because it breaks the meaning of a package. If a draft could be deployed, a package would be only a snapshot at a point in time; the moment the draft is edited, or a process removed, and it is redeployed, **in-flight processes have no stable version to run** — a complexity we do not need.

- **A version is the only deployable thing.** A draft is never deployed; deploying always names a published version, so anything already running keeps running the version it started on. Integrity by construction.
- **The API keeps publish and deploy apart**: publishing a draft creates an immutable version; deploying points an environment at a version.
- **The UX offers a one-step shortcut** — **Publish now, then deploy** — which publishes the draft as the next version and deploys that version. **Standard mode** uses it; **advanced mode** splits publish from deploy for those who want control.
- **This removes the free-tier edge case.** With a single environment, the shortcut publishes and deploys in one step, so there is no need to allow a draft into production, and no need to force the sole environment to be non-production.

### 28.5 Table actions are icons; links are textual · **Decided — general rule**

On tables and lists, **buttons are icons** — to save space, and because a text label can run long and wrap or break the layout — while **links stay textual**, because a link is often a more detailed or more serious action and the reader should read what it does. Applied on the project page: **Add process** and **delete process** are icons; a delete expands the row to an in-place **confirmation** with **Cancel / Remove** — those two take text, because a confirmation is read, not scanned.

### 28.6 The processes card names its columns · **Decided**

The processes table has titled columns — **Process**, **Owning team** (the team badge was previously unlabelled) and an actions column — and the page no longer links forward to running cases, because those are version-specific (the process view links back instead, §27.6).

### 28.7 Process ownership is a team · **Decided — core concept (2026-10-05)**

A process is owned by a **team**, not an individual. This supersedes the individual process-owner role (§1.4–1.5).

- **Why.** Accountability is hard to place on one person — it drifts to someone very senior, or into technology. A team makes accountable ownership natural: **whoever manages the team manages the process**, the owning team is the **default pool for the process's manual tasks**, and organisational change touches only the team, not every process. A team is a core concept that is already managed and controlled, so this removes complexity rather than adding it.
- **Ownership vs participation.** Many teams may *participate* in a process — its manual steps are assigned to teams — but **one team owns** it. In KYC, the steps are performed by a **KYC Operations** team while the process is owned by **Compliance leadership**.
- **Impact.** §4.2's "the process owner is never hidden" becomes **the owning team is never hidden**; §16.3's access rule keys on the owning team; the screen shows the owning team (e.g. **Claims Processors**).
- **Project and process owners align.** A **project** is owned by a **team** too (§29), and the **process owner defaults to the project owner**, separable in advanced mode; the process owner is a **management placeholder**, not a permission (§29.3).

### 28.8 The processes table carries two actions · **Decided**

Each process row has **edit** and **delete**, in that order — both **icons** (§28.5). Edit opens the **process designer** (`process-designer.html?process=…`, §36); delete expands the row to an in-place confirmation (§28.6).

### 28.9 Business rules are files · **Decided — refines §27.5**

A business rule is a **file**, held the way the process view holds documents, with an **add** control and a **replace / remove** action on each row. The file types are **decision tables (.xlsx, .csv)**, **Drools (.drl)** and **DMN (.dmn)**. The project does **not** version the file — the **uploader manages its own versioning** — the project records only the **events** (added, replaced, removed) on the history timeline (§28.10). This refines §27.5: the rule is still project-scoped and invoked through a process, but the *file's* versioning sits with its owner, not with the project version.

### 28.10 The project history is a timeline · **Decided**

The versions/deployments card on the overview becomes the **project history**: a **vertical timeline** — equally spaced dots down a single line, **one dot per event**: on the head line the **version as a small outlined box** followed by that event's own **date and time** in mono, and the **event** beneath it ("Celia Yu edited process “Outpatient minor claim”", "Ravi Patel deployed to test"). A **version selector** sits in the card header: **All versions** at the top, then each version; it **defaults to the version you opened**, so the page stays version-scoped by default while "All" shows the whole history. This replaces the table on this card (§28.5's table rule does not apply — it is a timeline, not a table). The **deployments matrix** remains a separate screen (§28.3); the timeline is where a project's history reads, including who edited what.

### 28.11 Small refinements to the project page · **Decided (2026-10-05)**

- **Processes and business rules sit two-up**, each about half width, stacking above/below below ~980px — they are short, parallel lists.
- **The processes table's first column is Name**, with the process **description** beneath it; the description line is kept even when empty, so rows stay equal height.
- **Every history event carries its own timestamp** — two events never share one — rather than a single stamp for the whole version group.
- **A version is a small outlined box everywhere** (`v5`), not a pill or plain text; the shared `.vbox` treatment lives in `app.css` so it is the same on every screen.
- **The draft is labelled simply “draft”**, not `v6 (draft)`. There is only ever one draft and it is the head, so the next number is noise: a version is just a view in time, and the draft is what is being edited. Applied to the title, the history selector and the timeline. (This also settles the reader's instinct from §28.2 — the draft reads as the working state, not a numbered version.)

### 28.12 The project banner has two variants · **Decided (2026-10-06)**

The three-bar header adapts to what it is showing:

- **Draft:** **Published** → *draft, not published*; **Deployed to** → *cannot deploy drafts*; **Owner**.
- **Published:** **Published** → the **date and time** of publication; **Deployed to** → only the **environment names** (the audit history shows when and by whom); **Owner**.

The draft variant is mostly static and uses prime space, but it is kept on purpose: it walks a non-technical user through what a draft is and what can and cannot happen to it.

### 28.13 Name, business area and description changes are auditable · **Decided (2026-10-06)**

Changing the project's **name**, **business area** or **description** records an event on the **project history timeline**, and the event carries the **previous value** — *“renamed the project from “Claims handling” to “Claims Processing””*, *“changed the business area from “Operations” to “Claims””*. It is still a **label change** and creates **no version** (§27.1); the audit is what makes it traceable.

---

## 29 · Ownership and security — the model · **Agreed in shape (2026-10-06)**

Supersedes §27.7's per-project "individuals own projects" and the previous recommendation in this section. That approach failed a test worth keeping: **a reader could not tell _why_ a person can or cannot edit.** This model derives permission from a few named organisation defaults, with a per-project override and a plain-language summary.

### 29.1 Platform defaults — the organisation control · **Decided — supersedes §23.1**

One organisation-level control, **Platform defaults**, is a three-stop selection — **collaborative · normal · secure** — that sets the default posture for every project:

| Mode | Who can view | Who can edit | Who can deploy / publish |
| --- | --- | --- | --- |
| **Collaborative** | everyone | the owning team | the owning team |
| **Normal** | the owning team | the owning team | managers only |
| **Secure** | managers only | managers only | managers only |

One setting, defaulted to **collaborative** — most small organisations pick it up and want to get on with using it. **Collaborative is not a free-for-all**: the controls that prevent *excessive* access remain available without leaving the default — the per-project override, the additional-team lists, and the view-versus-start split (§29.4). "Manager" means a manager of the owning team. (The mode names replace §23.1's *most collaborative · neutral · most secure*.)

### 29.2 The project security card · **Decided — contents**

1. **Owning team** — one, and replaceable.
2. **Override the platform default for this project** — only a **manager** may change it; advanced mode.
3. **View scope** — derived from the effective mode: *collaborative → everyone; normal → the owning team; secure → managers*.
4. **Additional teams that can edit** — advanced mode.
5. **Additional teams that can publish** — advanced mode.
6. **Additional teams that can view** — advanced mode.
7. **A plain-language summary** of the **effective** permissions, saying whether they come from the **organisation default** or a **local override**.

The additional-team lists **widen** the derived set. The override changes which default applies, so a project can be more open **or** more closed than the organisation — which resolves §16.3's "may an override tighten?" question for projects.

### 29.3 Project owner, process owner, and who edits a process · **Decided — refines §27.7 and §28.7**

- **Project-level security decides who can edit the process.** There is no separate per-process editing grant.
- **The process owner defaults to the project owner** (the owning team, §28.7); in **advanced mode** it can be separated.
- **The process owner is a placeholder for management** — an accountability label, not a permission.
- So permission and ownership stop competing: **permission** lives on the security card; **ownership** is who is answerable.

### 29.4 Execute is one control — "who can start" · **Decided (2026-10-06)**

**Execute is one control: who can start** a process. It is separate from **view**, and lives at the **process** level, not the project. A **reusable** process may be **startable by anyone** even when the process it wraps is secure. It gives a name to §2.3's "starting is a separate grant".

**Who can *work* a task is not part of this control** — it is the **team assignment of the activity**, set in the process designer. By default it **inherits down the chain**: **project owner team → process owner team → task owner team**, with the most specific winning. So the security model stays small and named: **view · edit · deploy · start**, and **who works an activity** is the designer's team assignment rather than another permission.

**Open** — where these process settings live (the process definition screen, §19.1) is the next discussion.

### 29.5 Why this is clearer · **Decided**

"Why can this person edit?" is answered in one place — the **effective mode**, shown in words on the security card, with the organisation default and any local override named. The previous model attached edit rights to a creator, which could not be explained this simply.

### 29.6 Built · **`project-detail.html`**

The security card is built on the project overview (`data-od-id="project-security"`): the **owning-team control** and the **effective-permissions summary** are always visible, and the **override** plus the three **additional-team lists** (edit / publish / view) sit behind **advanced mode**, with a note pointing at the user menu.

---

## 30 · Common components — to move into the design language

A running list of controls that have emerged on these screens and should be **promoted into the design language** as shared, documented components rather than re-implemented per screen. Recorded so nothing is lost while the screens are still moving. **Status** is per row.

| Component | Where it is used | Status |
| --- | --- | --- |
| **Team selector** | project security card (§29.2); intended wherever a team is chosen | built — `app.css` / `app.js`; notes below |
| **Icon control (`.sctl`)** | list and table row actions, refresh / export | built; used widely (§28.5) |
| **Version box (`.vbox`)** | everywhere a version is shown (§28.11) | built |
| **Table pattern** | Processes / Inbox / Teams / Projects (§24) | built — the gold standard; rules in §30.2 |
| **Segmented scope control** | the lists | built |
| **Prompt box + structured panel** | the lists (§22) | built |
| **Dialog / scrim** | confirmations | built |
| **Wireframe zone (`.zone`)** | undesigned regions (§10.1) | built |

### 30.1 The team selector — UX notes · **Decided**

- **It is a type-search, not a dropdown.** A team list can be hundreds long, so it is a **typeahead**: type, then **click or Enter** to add. Selected teams are **chips** with a remove ×. Arrow keys move through the list; Escape closes.
- **The default list is the user's most recently used / referenced teams**, not the whole list. **Search kicks in at two or more characters**; below that the panel shows **Recent**.
- **One control, any cardinality.** `data-max="1"` gives a **single** team with **replacement**; no max gives a **set (0..n)**. The **owning team** is the single case; the **additional edit / publish / view teams** are the set case.
- **Empty is allowed, but not to publish.** Clearing the field shows an **error** on the control (`.is-invalid`; the palette has no destructive colour, so the border uses the highest-contrast token, as elsewhere). The page may be **left and saved with no team**, because team selection is **not retroactive** over existing versions; but **publishing requires an owning team**, and the publish action is **blocked** with the error state surfaced.
- **Not multi-owner.** Multiple owners are not offered; when an organisation grows and wants several, it assigns the project to a **technology team** (§29.7).
- **To do when promoted:** move the markup, styles and `initTeamSelects` into the design-language package; document `data-max`, `data-recent`, the `teamchange` event and `hamoni.teamSelect.{get,set}`.

### 30.2 The table — the gold-standard row · **Decided (2026-10-08)**

The Processes table is the reference for the lists (Inbox, Processes, Teams; §24). These are its rules — the design decisions a new list starts from. The high-fidelity record is §38.6; the live demo is `screens/controls.html`.

**Structure**
- A **scope control** (segmented), **one query composed two ways** (the prompt box and the structured panel, §22), a **centred summary sentence in its own bar**, **sortable columns**, **in-place expansion** (one row open at a time), the **shared state controls**, and **one page action**.

**Colour — now / next** (§38.5)
- **Purple = where you are**: the page action, and the **selected row's name** (on the process view, the current stage pip). One purple per view.
- **Teal = the next action**: the **forward** control only — *Open process*, *Resume* on a selected row, *Apply filter* while the panel is open. Pause and cancel go *backwards*, so they are always neutral.
- **Hover is neutral** (a surface raise); only **selection** takes the accent.
- **States are neutral** — read by **icon and label, never colour**. **Exception carries no highlight** either: we cannot assume an exception is what the user is looking for.

**Type — three voices** (§38.6)
- The **name is the only foreground text** in a row. The id, Started, Due in, rank-position and state all share **one muted tone**.
- **Mono only for identifiers** (the process id), versions and code. Dates and numerics are **Inter `tabular-nums`**; labels are **Inter uppercase**.

**Row anatomy**
- `<process id> · <process name>` on one line — id in mono, name in Inter 600.
- A **leading chevron** announces the expansion, at the **left edge** so it reads as structure, never as an action (the actions sit on the right). It is the row's only disclosure affordance; a **one-time hint** teaches it.
- **No row number** — position is the rank.
- **Long names truncate** with an ellipsis on one line; the full `id · name` is on hover.
- The **relationship** — "You started this process", "You are the process owner", "You have an active task", "Your team has an active task" — reads in **words, muted, in the expansion**, left-aligned with the action. **No relationship, no line.** Never a per-row icon.

**Time**
- One date format, the longest: datetime `D MMM YYYY, H:MM AM/PM`; date `D MMM YYYY`; time `H:MM AM/PM`.
- **Due in carries no highlight.** "Overdue" is a **structured boolean filter** (*Overdue only*), and the field is **empty** when the clock is not running (no "paused" text). The **Started** filter is a single **date range** (dates only, localised) — the filter is day-granular.

**The expansion**
- One row open at a time. It holds the **dynamic area** (the designer's key/value/description widget, shared with the task and process views, §20.8), the **relationship** line, and the **forward action** (teal border). The collapsed row and the expansion **never repeat a fact**.

**States, empty, loading, error**
- Five states (Active, Pended, Exception, Completed, Cancelled), each an **icon + label**; no state is special-cased by colour.
- A designed **empty state** (with the clear/create affordance), a **loading skeleton**, and an **inline error band**.

---

## 31 · Process settings — inventory and placement · **Open — design next**

The process has two pieces: the **designer** (the flow) and the **settings**. This compiles the process-level configuration and argues where it should live, before any of it is built.

### 31.1 The inventory · **the process level**

| Setting | Class | Status / source |
| --- | --- | --- |
| Process name | identity | editable; a label — unversioned, like the project's metadata (§27.1) |
| Description | identity | the standing description (§19.12) |
| Owning team | identity | defaults to the **project owner team**, separable in advanced mode (§29.3) |
| Reusable flag / collections | identity | a label, not a move (§2.2) |
| **Who can start** (execute) | access | one control, process-level (§29.4) |
| View / edit | access | **inherited** from the project security card (§29.2) — not set here |
| Per-environment assignment (whose work a task is) | access | task-level, **shared across environments by default** (§2.9) |
| Who may open a **running case** | access | owner / current actioner / persistent teams (§16.3) — **to reconcile with §29** |
| Activities / steps — order and type | content | human · script · integration · decision; authored in the designer |
| Activity team assignment | content | **who works each step** (§29.4), inheriting project → process → task |
| Task canvas widgets | content | dynamic area, documents, business-rule invocation, decision-activity scope (§15.7, §27.5) |
| Start — form / data, and the start permission | content | what a start collects; start (§29.4) |
| Completion — requires confirmation of close | content | a process-task property (§14.7) |
| Exception handling | content | authored in the designer (§17.4) |
| **Pend** — clock mode | pend | stop / keep the clock (§17.3) |
| **Pend** — actions (quicklinks) | pend | processes to start, and the data passed (§17.6) |
| **Pend** — resume method | pend | manual / time / after subprocesses (§17.7) |
| **SLA target** | timing | an **X% in Y time** target (e.g. “90% in 5 days”), not a single duration — §31.4 |
| Calendar | timing | **inherited** from the team (§4.16), not set here |
| Publish a draft → a version | versioning | §2.8 |

### 31.2 Placement — the two approaches · **Recommended**

- **Most of these are derived**, not set: ownership inherits from the project, view/edit from the project security card, the calendar from the team, the working hours from the organisation. That is the opening that makes the settings small.
- **Approach A — settings on the designer page.** Correct home: settings are part of *designing* the process. The risk is space. The fix is not to inline them into the canvas but to give them their own area: the definition screen is **Design · Settings · Versions**, so the canvas stays clean and settings are grouped. And because they inherit, the area shows the **resolved value with its source named** ("Owning team — HR Operations, from the project") and only offers an **override**; grouping (identity · access · content · pend · timing) keeps it scannable, and advanced mode reveals the technical ones.
- **Approach B — settings in the project list's expanded row.** The wrong home: that expansion is a **browse and diagnosis** surface (§3, §24), and a person does not go there to configure. It also cannot hold the settings depth without breaking the expansion's dozen-row rule. At most the expansion should carry a **read-only effective-settings summary** — enough to see what a process is — with a **link to its design**.
- **Recommendation:** **Approach A.** The process design surface owns the settings in a dedicated **Settings** area (a peer surface, §32.3); inherited values are shown resolved and override-able; the project list expansion shows a read-only summary and links out.

---

## 32 · Design mode, and how we navigate the large surfaces · **Proposed — to agree**

The problem: the process's design surfaces are **large** — the flow, the summary-view designer, the task form designer, the settings — and climbing to them through team → project → process setup feels like a burden before the thing the person actually came for. This proposes a navigation model.

### 32.1 The three things a process is · **Agreed**

1. **The process** — the flow, with its identity.
2. **The summary view** — the default summary shown in expanded table rows (the dynamic area, §15.7/§20.8).
3. **Settings** — the derived-with-overrides configuration (§31).

Task surfaces (a **task's expanded view** and its **dynamic-area form**) hang off a **step** inside the process, not off the process directly.

### 32.2 Design mode · **Proposed**

- **Design mode is a mode, not a screen.** Entering it from a process **takes over the viewport** — the rail is set aside — with a clear **Exit design** back to the project. This is a deliberate exception to "the rail is identical on every screen" (§3): you have moved from operating to designing, and the canvas gets the room. It makes the "we almost leave the app behind" instinct explicit rather than accidental.
- **One shell everywhere.** Every design surface uses the same chrome: **top bar** (breadcrumb · the **editable name** · **one draft/publish** · Exit design), **left** = the surface switcher or the structure, **centre** = the canvas, **right** = **context properties**.
- **The properties panel is context, not a place.** With **nothing selected** it shows **the process's own properties** — identity, ownership, the overrides — which is the Figma pattern. Select a step → its properties; select a control on the summary view → that control's properties.
- **One draft, one publish.** The process draft is **shared by every surface** — flow, summary view, settings — and published together from the top bar. The summary view is never published separately from the flow.
- **Exit is explicit and non-destructive.** Leaving design mode keeps the draft (it is saved on edit); there is no forced publish.

### 32.3 Peer surfaces and nested modes · **Proposed**

Two kinds of large surface, told apart by **ownership**:

- **Peer surfaces** — owned by the **process**: **Process** (the flow) · **Summary view** · **Settings**. They sit in a **surface switcher** and replace the canvas; they share the process properties panel and the one draft.
- **Nested modes** — owned by a **step**: **Task design**, which itself has two full-screen parts — the **task expanded view** (what the row shows when opened) and the **task form** (the dynamic-area designer, with its own palette, testing and control configuration). These are entered **from the step** ("Design task") and **push a level**: the breadcrumb grows (`Projects / <project> / <process> / Task: <name>`), and **Esc / Exit backs out one level** to the flow, step still selected.

The rule: **a surface is peer if the process owns it; nested if a step owns it.** The summary-view designer is therefore a **peer surface** (the process owns the summary); the task form is **nested** (a step owns the task).

### 32.4 Getting to the process without the climb · **Proposed**

The setup burden is real: team → project → process, then the designer. The fix is to **cut the required inputs** and **default the rest**:

- **Create with a name, and nothing else required.** The owning team is **inherited** (the project's), the project is created or picked in one control, and everything else — security, pend, SLA, calendar — takes **sensible defaults**.
- **Identity is the editable header**, not a form (§28.1's pattern): you name it in place and keep going.
- **Configuration is reachable, not required.** Security, pend, SLA and the rest live in **Settings**; the create path never blocks on them. **Project settings are reachable from the process settings** without leaving for the Projects screen.
- **An agent / quick-start collapses the whole thing.** The prompt (§6) can create the **team, project and process in one step** and drop you into the designer; the manual path is the same flow with the user naming things. That is the onboarding fix — the three nouns stop being three chores.

### 32.5 What this gives the navigation · **Proposed**

- From the app: **Projects / ‹project› / Processes** → a row's **Edit** opens **design mode** on the **Process** surface.
- Inside design mode: the **surface switcher** moves between **Process · Summary view · Settings**; the **right panel** follows selection; the **top bar** holds the name and the single **draft/publish**.
- A step opens **task design** as a **nested mode**; backing out returns to the flow.
- **Exit design** returns to the project. Nothing is lost — it is a mode you enter and leave.

### 32.6 The contextual switcher, and getting out · **Proposed — guess, then see**

- **The switcher is context-scoped.** At the **process** context it offers three slots: **Process · Summary view · Settings**. Enter **a task** and the **same three slots re-scope**: **Task · Summary · Form** (the task's own summary builder and its dynamic-area form). The slots are the same *kinds* of thing — a canvas, a summary builder, a configuration surface — renamed by context. This is the "contextual overlay" instinct, made a rule.
- **Placement — the guess to try:** a **fixed slim left rail**, contextual, echoing the app's rail and **costing no vertical space**, which matters for a vertical process (§33). The alternative — a fixed top bar — is a small change if the rail does not sit well. Either way it is **fixed**, not in the scrolling canvas.
- **Getting out is the breadcrumb plus Esc.** The breadcrumb is the anchor and grows with context: `Projects / ‹project› / ‹process›`, then `… / Task: ‹name›`. Clicking a crumb **pops to that level**; **Esc pops one level**; and the switcher **always names the current context** above it ("Process" / "Task: Review claim diagnosis"), so changing surfaces never loses where you are. That is the answer to *easy in, hard out* — **in is a click, out is the breadcrumb**.
- **Summary editing is design-mode only** (confirmed): the **process** summary here, a **task-level summary builder** in the task context. Neither is edited from the running-case screens.

### 32.7 Audience · **Decided**

Small organisations: the **team lead** does the design — so the flow is linear and the defaults carry it. Larger or more mature ones: a **business analyst** or **technology staff** — so advanced mode and the Settings/overrides give them depth. The same surface serves both; the difference is how much is revealed.

---

## 33 · Process orientation — vertical · **Proposed — to agree**

Traditional process modelling runs **horizontally** (left to right, as in BPMN). It does not fit a browser: a real process is many times **wider than it is tall**, so it forces **two-axis scrolling**, which browsers handle badly and which inverts the page's natural vertical scroll — almost nothing but a map pans well in two directions.

**Recommendation: model and display the process vertically — top to bottom, one scroll axis.**

- **Only as wide as the page.** A process is **as long as it needs to be**, but never wider than the viewport, so the designer scrolls in **one direction** and horizontal scroll never appears. This is what browsers are good at.
- **Consistent with the process view.** The process detail already presents a process as a **vertical step list** (§19.7), and its **state** is a vertical element. Designing vertically means **what you draw is what the running case shows** — designer, process view and summary all read down the page.
- **Mobile follows for free.** A vertical process reads on a narrow screen, so the process **view** becomes mobile-viable (§14.10 deferred mobile) while the **designer** stays **desktop-only** — you design on a desktop, you *view* on a phone.
- **How the shapes map vertically:**
  - **Flow** — a vertical spine, top to bottom.
  - **Stages** — the coarse sections of the spine; the **status rail reads vertically** (the stage pips become a vertical rail).
  - **Branches / parallel** — indented columns or a split-and-rejoin that widens slightly, then rejoins below, **never beyond the page**.
  - **Roles / owning team** — **not a lane** (§33.2): the step carries its own assignment, so the width never widens for a participant.
  - **Loops / back-edges** — a labelled return within the column, not a long back-line across the canvas.
- **The cost, accepted knowingly.** Vertical is a **substantial shift** for anyone who has modelled horizontally, and some conventions (wide swimlanes, many parallel lanes) fit awkwardly. The audience (§32.7) is a team lead, a BA or a technology person modelling a **business sequence**, not a BPMN diagram.

### 33.2 No swimlanes — assignment lives on the step · **Decided (2026-10-06)**

Swimlanes are a **dated modelling concept** and, on a vertical process, they would force the canvas to widen for every participant — many times more width than the flow itself needs. **Swimlanes are not implemented.** Each step carries its **own assignment**: it is performed by a **team**, an **agent**, or the **system as a script** — read from the step, never inferred from a lane. This is also what keeps the vertical geometry bounded.

### 33.3 Stages are structural — part of the vertical axis · **Decided (2026-10-06)**

**Agreed.** A stage is **first-class and vertical**: a stage is a **section of the spine**, and the process is the **ordered sequence of its stages**. A **task's stage is derived from the section it sits in**, not set as a field on the task.

The common UX convention — a stage dropdown on the task — is rejected: stages **segment the process**, so they belong **on the axis**, where the whole segmentation is visible and reorderable at once.

- The **status rail is the vertical sequence of stages** (§33), and the running case shows the same segmentation.
- A step changes stage by **moving between sections** — a structural edit, not a field change.
- **Parallel** steps inside one stage sit **within the same section**.
- This reconciles with the stage facts the screens already carry — the process bar's stage and pips, and the process detail (§19.7, §19.11, §20.1) — which the stage now *is*, rather than a label repeated onto each task.

**Stage rules (2026-10-07):**

- **Reordering is adjacent.** A stage can be **moved up or down**, but it **cannot cross another stage's boundary** — order changes by adjacency, and boundaries stay distinct.
- **A node belongs to exactly one stage.** A node **cannot cross a stage border**; a crossing node is a **validation failure** (§36.2).
- **One stage by default.** A new process starts with **Stage 1**; a **`+` / `−` control on the stage bubble** adds or removes a stage.
- **A new stage lands at a sensible point.** Its intended position is unknowable, so it is placed **down the page, not crossing nodes and not at the very end**; the **user moves it** from there.

---

## 34 · The diagram notation — Basic and JSON Process · **Proposed — to agree**

### 34.1 Two notation levels · **Agreed in shape**

- **Basic (standard mode)** — a limited, consumable flowchart set: **start · stop · activity (human / script / agent) · decision**. Sequential flow plus exclusive branching; **no** parallelism, loops or subprocesses. **Stages** (§33.3) still segment the spine.
- **JSON Process (advanced mode)** — the full specification. It **draws on BPMN** for notation and layout but is **not true BPMN**: BPMN is dated and limited for this product, so the spec is updated and **pend and exception are first-class** in it rather than bolted on as events. Later additions: **task loops · parallelism · subprocesses · embedded subprocesses · intermediate signals**. The name **"JSON Process" is a placeholder** until it settles. The advanced user is assumed to have **formal process experience** (§32.7).
- Both render **vertically** (§33): advanced adds symbols, it does not change the axis.

### 34.2 The palette is the mode · **Agreed**

- **Standard mode** → the designer's palette is the **basic set**.
- **Advanced mode** → the palette is the **full JSON Process set**.
- Driven by the existing **advanced mode** setting (§7: one application-wide setting, not a per-panel switch).

### 34.3 Switching between notations · **Agreed**

- **Basic → advanced: always allowed.** The design upgrades; basic symbols map onto their JSON Process equivalents. Nothing is lost.
- **Advanced → basic: allowed only if the design uses no advanced-only features.** Otherwise the switch is **refused**, and it **names what blocks it**. It is **non-destructive**: a refusal changes nothing. This is the "make the unsafe state unreachable" instinct (§2.9, §4.14) applied to a **downgrade**.
  - **Corrected (2026-10-07, §37.1:9)** — the blocker list is **the features that actually exist**, not the earlier "loops, parallel branches, subprocesses or signals": **`begin.time` / `begin.signal` · any `sync` · `end.exception` / `end.terminate` · `activity.rule` / `activity.rpa` · `junction.parallel` · a parallel marker · a pend or exception handler.** Primitive-only state data does **not** block a downgrade — it is preserved.

### 34.4 When standard mode cannot render the diagram · **Agreed**

- **In the designer:** show a **warning in place of the canvas** — *"This process uses advanced steps. Enable advanced mode to view or edit it."* — a labelled region, not a broken canvas (§10.1). The process's other surfaces (identity, settings) remain reachable, and the warning is the one action.
- **In the process summary / read-only view:** **do not gate it** — present the **advanced rendering** to everyone. Advanced mode is needed to *design* a complex process, not to *read* one.
- So the mode gates the **designer**, never the **reader**.

**Resolved (2026-10-06)** — the "process summary view" is the **process detail** page's diagram: the visual that shows the process and the **stage it is currently at**. It renders the **advanced (JSON Process) view to everyone**, ungated, so a reader never needs advanced mode. **Still open** — whether "JSON Process" is the final name; whether the mode should gate anything in a read-only view at all.

---

## 35 · The process designer — structure built · **Built (2026-10-06)**

`process-designer.html` is **design mode** (§32.2): it takes over the viewport below the prototype flowbar — the app rail is set aside — with:

- a **top bar**: **Exit design** · breadcrumb (`Projects / Claims handling / Design`) · a **draft** badge · the single **Publish** action;
- the **contextual switcher** (fixed left): **Process · Summary · Settings** (§32.6);
- the **vertical canvas** (§33): **stage sections** sectioning the spine, and the **basic-notation** node set — **start · stop · activity (human / agent / script) · decision** — with an in-page **Yes / No branch**;
- the **context properties** panel (§32.2): the process's own properties when **nothing is selected** — project, owning team (the team selector), notation, who can start, SLA target — and a **step's properties** when a step is selected (name, stage, performed-by team/agent/script).

The editable **process name** uses the header-pencil pattern (§28.1). **Summary** and **Settings** are **placeholder surfaces** for now — the shell and the process canvas are the deliverable; their canvases fill in next.

### 35.1 Revisions — second pass · **Built (2026-10-06)**

Six corrections after the first build:

- **The switcher moved to the top bar.** A fixed left rail consumed a full stripe of mostly empty width; the surface switcher (**Process · Summary · Settings**) now sits in the **top bar**, so the canvas keeps the full width. (The §32.6 guess of a left rail is overturned.)
- **No draft badge and no publish.** The draft is the *mechanism*, not a badge — and if shown at all it belongs with the **process header**; **publishing is project-level** and reachable only from the **project** publish. A design surface publishes nothing itself.
- **A component palette**, **horizontal and sticky**, so it adds a palette without stealing canvas width: the basic set — **Start · Activity · Decision · Stop** (§34.1); advanced swaps in the JSON Process set.
- **Stage bands replace stage boxes.** A dashed box around a stage implied the flow *stopped* there, which is wrong the moment a path returns. A stage is now a **band on a continuous spine** (§33.3), and an example **return** ("returns to stage 1") shows flow crossing stages in both directions.
- **Compact nodes.** A node is a **name and an actor icon** only — no type label, no second line — so the boxes stay small.
- **The properties panel is trimmed to its fields** — no placeholder prose.

---

## 36 · The process designer — canvas UX · **major piece**

`process-designer.html` was **rebuilt from scratch** on this UX. The earlier page-based shell (`process-designer-v1.html`) was reviewed field by field and then **deleted** — every v1 function is accounted for: surfaces, palette, rename, stage bands, the node set and the access/timing fields all survive a level down or up. v1's **Notation** toggle was an **advanced-mode side effect** (§34), and its **SLA / pend / timing** fields were **deliberately not modelled** (§31.1, the Settings surface says so); the **right-hand properties panel** is superseded by the **layer stack** (§36.1).

- **A pannable canvas, not a page.** The designer is a **blank canvas with a grid** — **~5 mm** cells with a stronger line every fifth — not a scrolling document.
- **Positive space only.** `0,0` is the **top-left**; it is **impossible to pan left of 0 or above 0**. Panning is drag and wheel; the grid moves, the overlays do not.
- **The flow runs vertically** (confirms §33). A process **can be wider than the page**, but flows **downwards**, so it is **longer than it is wide**; if it is ever too wide for a view, **shrink to fit the width** later.
- **Floating overlays stay fixed** while the grid pans, aligned to the **left**, at the same inset as the right edge:
  - **Title (top-left)** — the process **name** with a **pencil** to rename.
  - **Contextual control (directly beneath the title)** — a **Back** button and a **mode switcher**: **Process · Summary · Settings**. **Back moves up one layer**; the layers are **process → subprocess → activity** (or **process → activity**), and as you descend, **the switcher shows that layer's key views**.
- **Settings is a complete overlay view**, not a panel on the canvas. Because settings, prompts and help are **complex**, **Settings opens as its own full overlay** rather than a floating box; the floating settings panel was removed.
- **Component selector and placement.** A **vertically stacked, icon-only** selector sits below the contextual control. Each icon has an **arrow beneath it** that opens its **subtypes to the right** (icon + name); picking a subtype **arms** placement — the **cursor becomes a plus**, and the next canvas click **places** the node. The advanced node set is **begin · sync · end · activity · junction**, with subtypes: **Begin** ad-hoc / time / signal; **Sync** time / inbound-signal / outbound-signal; **End** normal / exception / terminate; **Activity** manual / agent / automated-script / automated-rule / automated-RPA; **Junction** exclusive / parallel (the **BPMN X and + gates**). **Nodes are fixed-size** and **connecting lines are out of scope for this pass**. Stages are **active in advanced mode**. A few nodes are **pre-placed to visualise** the notation.
- **Account chip (bottom-left, mirroring the app).** Initials and an **up arrow** open **preferences upward** — **advanced mode** and **dark mode**. The **organisation is not switchable here**: it *is* switchable on the main screens, but switching from inside a process would **jump out to the top level**, which is obtrusive — so it is deliberately absent from the process view.

### 36.1 The layer stack and its surfaces · **Decided (2026-10-06)**

The designer is a **stack**, not a single screen: **process → subprocess → activity** (or **process → activity**). The **process is level 0**; its contextual control shows the level's surfaces — **Process · Summary · Settings**. **Clicking a node descends** to that node's level, the control **re-scopes** to that level's key views, and **Back returns one level** — and at **level 0, Back leaves the designer for the project view**. Every Back move is **validated** first (§36.2).

### 36.2 Leaving a layer is validated · **Decided (2026-10-07)**

Going **back** — up a layer, or out of the designer to the project view — **validates the layer first**. If there is an error the move is **blocked** and the error is reported, so an invalid layer can never be left behind as if it were valid. The report is a **toast at the bottom of the screen**: at the **top** it would overlay the active controls (the page action, the palette, the nav), whereas the bottom is clear — bottom-centre, clear of the account chip (bottom-left) and the pan hint (bottom-right). Errors to check as the designer is built: a node crossing a stage border (§33.3), a stage crossing another, and an activity missing a name or a performing team.

- **Activity level — `Settings · Data`.** *Settings* is the task's name, performing team, estimate, completion confirmation and work-screen widgets (§31.1, §15). *Data* is a **data modeller**: map process state to the task's **input** variables, and the task's **outputs** back to process state (state data is a JSON Process concept, §37:14). **Not modelled now** — each is a **labelled placeholder that references design-decisions.md**.
- **Summary (level 0 surface) — a screen designer on the same grid.** It shows an **outline of the summary view**, a **device selector** (desktop · tablet · mobile, **icon only**) at the top right, and a component palette of **frames · sections · text · buttons · fields · images · widgets** (the last being complex Hamoni-defined components). **Not modelled now.**
- **Settings (level 0 surface) — the basic process settings.** An **owning-team** team selector, plus **view / edit / execute** team selectors, and a **resolved-security line** — *"Executable by HR Operations, inherited from the project"* (§29.3, §31.3). The remaining settings (pend, timing, SLA, exceptions, §31.1) are left out.
- **The activity box carries its type glyph** (manual / agent / script / rule / RPA) at the top-right, so the subtype reads on the canvas.
- **The stage bubble carries `+` / `−` controls** for adding and removing a stage (§33.3).

### 36.3 The SLA is a percentage in a time window · **Decided (2026-10-07)**

The process **SLA target** is expressed as **X% in Y time** — e.g. **“90% in 5 days”**, meaning **90% of the processes started should complete within 5 business days** — not as a fixed duration every case must meet. The reason is operational: it lets **some** cases run long while the **majority land in time**, which is how operations teams — and the **AI** scheduling work — **avoid over-staffing** for the worst case.

- **Charting is hard** because the measure depends on whether it is read on an **hours or a day** basis.
- **Tracking is easy**: it is a **check at a point in time** — are 90% done by day 5?
- So the target is a **distribution**. The per-case **countdown** on the process view (§19.11) and the "due in" column (§20.5) are per-case conveniences, not the SLA itself.
- **Some of this may not be modelled in the prototype yet** — when that happens, the full UX is recorded **here** rather than only in the build.

---

## 37 · JSON Process — the specification (working notes) · **reviewed (2026-10-07) — see §37.1**

The advanced notation (§34) is **JSON Process**: BPMN-inspired but updated, **executable**, and defined as a **JSON Schema**. Working notes:

1. **Begin** — the start node; variants **ad-hoc · time · signal**. An automated process that performs logic and then starts the process is still **ad-hoc**. A process may have **many begin points**, not necessarily at the start — a begin can sit **mid-process**.
2. **End** — variants **normal · exception · terminate**. **Normal** is the common case. **Terminate** ends **all other active branches/subprocesses**, is always **interruptive** (running work is interrupted without completion; the activity implementation manages interrupts), and a process may have **many endpoints** of different types.
3. **Activity** — the basic unit of work, with variants: **Manual** (performed by a person, who may be supported by technology, an agent, or paper — **the person is responsible**); **Automated** (procedural, performed by a system — a script task, an RPA callout, or a business rule; **deterministic, likely short-running**); **Agentic** (uses an AI / stochastic system).
4. **Junction** — the general name for a decision / gateway; variants **exclusive** and **parallel**, simplified BPMN. A junction **decides only from data it already has and never calls out for external data** — it can run **asynchronously**, with **no blocking code**. So "is A in B (in memory)" is fine; "is A in document B" is **not** (reading storage blocks) — the pattern is to **extract/compare in an activity, then decide at the junction**. This rules out BPMN complex gateways (no waiting on / acting on multiple data). Decisions may be **arbitrarily complex but must be async**; asking an **agent or a business-rule engine for a value is not supported** (both can block or throw).
5. **Pend and exception are built in and are global.** With parallel paths, a pend suspends **all**; an exception likewise — if something is missing, other paths may be unable to complete, so it is best to wait.
6. A pend/exception **does not interrupt running activities** — they run to completion — but **the next activities on every path are blocked from starting**.
7. JSON Process targets **business situations involving people and technology**; it is **not** for lower-level flows such as RPA or data workflows.
8. **Handler processes are isolated.** A pend/exception handler uses begin-event types but is a **separate process that does not interact with the main flow** — if it could re-enter the main process earlier it would create a parallel loop and leave the old path hung. So a process has up to **three components**: the **main process**, the **pend handler** and the **exception handler**.
9. A process **completes when all parallel branches and both handlers complete**.
10. **Exception bubbling**: with **no** exception handler, an exception **bubbles to the parent process**; with a handler it can be **contained** (e.g. send a message and wait) or **re-thrown** with an **end-exception**.
11. Pend and exception handlers **share state data** with the main process.
12. An **end-terminate** also terminates any pend/exception handlers.
13. **Parallel markers on activities** are supported — perform the activity **for each item in a state-data array**.
14. **State data is part of the model** (unlike BPMN), using **JSON datatypes**; as it is designed to be **executable**, the constructs are close to physical implementation — **a name, a type and a description**.
15. **Activity types are subclassable by implementation** — e.g. an **Automated** activity might have an **RPA** subclass. The stored values/attributes must be **generic enough** to represent the real implementation (vendor, endpoint, queue name).
16. **JSON Process will be defined as a JSON Schema.**
17. **Not everything is supported at the outset** — several rare features are deliberately deferred.

**Updates (2026-10-06):**

18. **"Sync" nodes.** BPMN's *intermediate events* are called **sync** nodes, because they are used mostly to **synchronise** — within a process or between processes. Two subclasses: **sync inbound** (**time** and **event**) and **sync outbound** (**event**). A **sync outbound** can target **any / all processes** (**broadcast**), **a single process** — pick any one listening, **unicast** — or **a specific process** (unicast with a message selector). These follow **JMS semantics**.
19. **Starting a process from an event is not yet decided — wants careful thought.** It is complicated: there may be **existing processes listening** to the event *and* the platform looking to **start new processes**, and a signal carries **data that must be matched**. The safer route is an **activity that starts a process** — easier to understand, and the data can be **statically mapped**. Starting from an event is really an **external integration pattern**; in Hamoni it should be just as easy to **start by API**. Logged so it is considered against what happens in the real world.
20. **A node has a textual name**; there is **no uniqueness requirement**.
21. **A junction's condition lives *inside* the junction**, not on the outgoing lines (unlike BPMN). It is more logical, and it removes the ambiguity of a conditional line sitting between two activities.
22. **Lines only indicate connectivity.** A line may carry a **name for display**, but it has **no functional impact**.

**Updates (2026-10-06, cont.):**

23. **"Event" and "signal" are unified as "signal".** The two were used interchangeably; the single term is **signal**, because it is more practical — an *event* could be anything happening in the world, whereas a process wants the **signal of that event**. So: sync **inbound signal** / **outbound signal**.
24. **Activity subtypes are first-class**, not one "automated" box with a type chosen in settings. The set is **Manual · Agent · Automated-Script · Automated-Rule · Automated-RPA** (and the advanced node set is **begin · sync · end · activity · junction**). **Reason for not collapsing to one box + a settings type:** a per-type setting makes the control set vary by subtype, so it **changes the screen layout**; and a **plugin** that adds a new activity subclass (say an RPA vendor) must be a **first-class subtype** — a value buried in settings makes such subclasses second-class and forces the UI to reshape. Distinct subtypes keep the canvas and the properties stable, at the cost of a longer palette.

### 37.1 Review resolutions · **Decided (2026-10-07)**

The review settled the ten open points, adopted the **process state model** (11), fixed the modelling-style corrections (12), settled **where the visual layer lives** (13), and formalized the **activity state model** (14). **Decisions are recorded here; the elaboration — node semantics, the state models, the JSON Schema shape and a worked example — is in [`json-process-spec.md`](./json-process-spec.md), which is derived from this section.** Where they ever disagree, this section wins.

1. **Name and one modelling style.** **JSON Process is a single modelling style** (§11): Hamoni's **simplified flowchart compiles to a JSON Process spec**, and "standard"/"advanced" are Hamoni **UI modes**, not two notations. The specification is called **JSON Process**; a saved document is a **process definition**; the schema is the **process schema**. "JSON Process", "BPMN" and "notation" are **not** user-facing words — the reader sees the flowchart and advanced mode only (§34.2, *the palette is the mode*). *(A product brand name remains open — spec §15.)*
2. **The three components.** **main · pend handler · exception handler.** Handlers are **isolated**, so they **share no flow line with main** and are **peer component canvases** reached by an explicit components affordance — not drawn on the spine (which would imply re-entry). They sit **at process level**, orthogonal to the §36.1 layer stack. **State data is declared once, at process level, and shared by all three** (§37:11). In the schema: `components: { main, pendHandler?, exceptionHandler? }`; the trigger is implicit in the role. Handlers use normal begin nodes for their own entry; the **component** is activated by its role.
3. **Starting from a signal (note 19).** **Deferred** — signal-spawning an instance is an external integration pattern (correlation, de-dup, routing), not a notation branch. **v1 starts processes by activity, by API, and by timer.** The API route is **`POST /processes/{processId}/instances`**, body validated against the state schema. A `begin.signal` is **valid mid-process only** (a token spawned at an existing instance by a correlated signal); a process whose only entries are signals is a validation failure until signal-start ships.
4. **Junction constraints.** Tenable. The rule is **no I/O, no wait, no side effect** — a **pure synchronous predicate over state already in memory** (logic may be arbitrarily complex). Any external datum must first be materialised by an activity. **Escape hatch:** when a condition references an undeclared value, the designer offers to insert the fetching activity before the junction. **Permanently out:** complex/inclusive gateways, gateways that wait, gateways with side effects. Join semantics: **exclusive = merge/choose-one (with `otherwise`); parallel = AND-join/fork.**
5. **Sync semantics.** Outbound targets are **`broadcast` (topic, all listeners) · `any` (queue, one listener) · `specific` (selector to a named process)** — JMS (§37:18). Correlation has three parts: **channel (signal name) · selector (a pure predicate over the payload) · payload mapping** into state. An inbound sync **never creates an instance**. *Time* is a timer, not a signal.
6. **State data.** Uses the **JSON Schema standard typing system** (draft 2020-12), **not a Hamoni-specific one**. The `type` keyword is `string` · `number` · `integer` · `boolean` · `object` · `array` · `null`; everything else is standard JSON Schema — **money, table, date and "json" are patterns, not types** (money = an object with `amount`/`currency`; table = an `array` of object items; date/datetime = `string` + `format`; the escape hatch = no `type`). Every field **is a JSON Schema with a required `description`**, declared never inferred; **validation at the boundary** (API payload / activity output / signal payload) raises an exception on mismatch. **Parallel marker requires an `array`-typed field.**
7. **Pend / exception vs terminate.** Pend and exception **suspend all paths without interrupting running activities**; the next activity on every path is blocked. **Terminate is the interruptive one** (running work interrupted; handlers terminated). **Suspension is not a token:** an instance is **complete when no executable token remains in any component and it is not suspended** — a pend handler finishing does not resume the main flow; the **resume method** does (§17.7). **Handlers cannot nest** — there are exactly two slots; an exception in a handler bubbles to the parent.
8. **Deferred set.** **In v1:** all settled node subtypes; parallel junction + parallel marker; both handlers; shared state; signals with selector/payload mapping; start by activity/API/timer. **Deferred:** signal-start, task loops, subprocess, embedded subprocess, dynamic/unbounded parallelism. **Out permanently:** complex gateways, swimlanes, conditions on lines, gateways that call out. **"Intermediate signals" leaves the deferred list — it was renamed sync (§37:18, :23) and shipped.**
9. **Simplified flowchart → JSON Process.** There is **one modelling style** (§2, §11); Hamoni's **simplified flowchart transparently compiles to a JSON Process spec** and is not a second notation. Mapping: start→`begin.adhoc`, stop→`end.normal`, activity·human/script/agent→`manual`/`script`/`agent`, decision→`junction.exclusive`. The reverse (rendering a spec as the simplified flowchart) is allowed only if the spec uses no feature outside the simplified set, else refused non-destructively and the blockers are named. **The §34.3 blocker list is corrected** to the features that exist (see §34.3, 2026-10-07).
10. **Diagram vs model.** **Lines are connectivity only** (`{ id, from, to, name? }`), conditions live **inside the junction**. **A branch references an edge id**, so edges stay the single source of connectivity and the junction the single source of routing. Renders on the vertical canvas with the condition **inside the diamond** and edge labels **muted** (display only); parallel is a conditionless fork/join; junctions belong to one stage.
11. **Process state model.** The instance lifecycle is the **Hamoni state model**, adopted for JSON Process: **Active · Pend · Exception · Paused · Completed · Cancelled**. **Active** is the initial entry point and the normal running state; **Completed** and **Cancelled** are **terminal**; **Pend** waits on an **external** dependency (clock configurable, §17.3) and returns to Active by its **resume method**; **Exception** requires external input and returns to Active once resolved, and its **clock keeps running** (the failure is internal and the consumer is unaware of it — the reason the SLA is "X% in Y time", §36.3); **Paused** is a temporary suspension for an **internal** action with the **clock still running** (the consumer is still waiting). The state is **runtime — it is not part of the process definition**. `end·normal` on all branches → **Completed**; **`end·terminate` → Cancelled** (a forceful ending even with a single thread — no parallel branches required); external cancel → **Cancelled**. **Cancelling is allowed from Exception, not from Pend** — a pend must **resolve**, not be cancelled; a pend that cannot be satisfied resolves by its resume method and the flow may then **complete early without producing outputs** (an early `end·normal`; no new construct, §8.6.1). The states exist **because pend and exception are first-class**: a pend/exception handler runs **within** its state and does **not itself change it** — the resume method, or the contain/re-throw end, does. Elaboration: `json-process-spec.md` §8.6.
12. **Iterations in time, not design layers — BPMN layering rejected.** **BPMN works in layers of design** (each more specific, until an executable layer, walked through once); **JSON Process has no layers** — it is refined in **iterations of time**. There is **one model, and it is always executable**, and it is **mixed-resolution**: a **manual task may stay high-level**, while an **agent or automated task must be well-defined — specific data in, specific actions out**. It is **not required that the whole process sit at one logical level** (impossible in a large enterprise); the invariant is that the process **executes** — **every node connected and all data mapped**. BPMN's layer walk and its **abstract·analytical·executable split** are **excluded by construction**; **iteration in time** is JSON Process's own mechanism. Construct-level exclusions are recorded in `json-process-spec.md` §12.
13. **The visual layer is stored in the model.** Unlike BPMN — which separates semantics from **diagram interchange (BPMN DI)** so one model can have many diagrams — JSON Process has **one grammar and one canonical view**, and its layout is largely **derivable** from the graph and stages. So the visual data is **co-located**: an **optional `view` block on each node and edge**, plus a **process-level `view`** for canvas state. Absent `view` → the renderer **auto-lays-out**; a drag writes `{ x, y, pinned: true }`. This removes the id cross-referencing and "missing/duplicate layout entry" class of validation errors. **`view` is presentation-namespaced: execution and executability validation ignore it.** A future *second* view of the same model would keep its geometry separately. Elaboration: `json-process-spec.md` §4.1, §13.
14. **Activity state model.** Activities have their **own** state, distinct from the process state — **Hamoni's task state model**, formalized as a **status plus an optional sub-status**. Statuses: **Available** (sub-status **Waiting** / open) · **Assigned** · **Active** · **Completed**, plus **Cancelled** when the enclosing process is cancelled or terminated. Transitions: **take/assign** (Available→Assigned), **start** (Assigned→Active), **pause** (Active→Assigned), **complete** (Active→Completed), **re-open** (Completed → a **new** activity instance, never a resurrection), and process-induced **cancel/terminate** → Cancelled. **`Completed` is a marking, not an assessment** — quality reads the output. The status set is shared, but **manual** uses the full model, **agent** uses Assigned·Active·Completed, and **automated** (script/rule/rpa) collapses to **Active→Completed** with no assignment. The activity state is **orthogonal to the process state**: a process **Pend** shows the task as Assigned (§17.9) and a running activity still finishes, while a process **cancel/terminate** moves any non-terminal activity to **Cancelled**. Runtime, not part of the process definition. Elaboration: `json-process-spec.md` §8.7.

### 37.2 JSON Process v2 backlog — owner direction · **(2026-10-07)**

A comparison of JSON Process with BPMN produced a gap register; the owner has given direction, captured in full in [`json-process-backlog.md`](./json-process-backlog.md). Settled directions:

- **Deadlines are the process SLA, not activity timers.** Activity-level **max-wait / max-execution** are **excluded** — they prioritise the task over the process. The deadline instrument is the **process-level SLA** ("X% in Y time", §36.3), measured as **target vs attainment**; **overrun is acceptable**. *(Closes comparison G1.)*
- **Loops.** **No task-level structured loops** — a repeat is a **process loop**, because the process is what is trackable. **Loops over data** ("for each item in an array") are the **parallel marker** and are welcome. *(G2, shape.)*
- **Subprocesses.** An **external process call** is the **`activity-subprocess`** activity subtype. An **embedded subprocess** is either a **design abstraction** (shares the main process data; **no SLA/pend/exception of its own**) or **isolated** (owns its lifecycle and data — an **external process stored in the same file**). Whether the abstraction earns its place — possibly **only combined with loops** — is **open**. *(G3.)*
- **Versioning.** **The whole file is versioned**; **version management is out of scope** — implementers handle versions and change in their own way. *(G14.)*
- **No formal standard/advanced mode** — confirmed: **one modelling style**, and the "mode" is the **palette selection** (§1).
- **Public specification.** JSON Process is **released publicly alongside Hamoni** and must stand on its own.

**To specify** (in the backlog): **activity properties for every type**, and the **process↔task data mapping** both ways (T5); the **script language, signature and round-trip rule** (A2). **Open:** the process-loop construct (T2); embedded-subprocess utility (T3); the **review task's home** — spec or Hamoni (A1); the agent specification (comparison G9).

### 31.3 Process permissions inherit and override · **Decided (2026-10-06)**

The process's three permissions — **view · edit · start** — **default to the project's values** and are **overridable per process**; **deploy** stays project-level. This resolves the §16.3 reconcile: the process no longer carries an independent visibility model, it inherits the project security card and may override it. **Who may open a *running case*** remains the §16.2 current-relationship rule — a separate, operational question, not this design-time one.

---

## 38 · High-fidelity screen set — build, assets and the component register · **started (2026-10-07)**

The high-fidelity screens (`screens/`) are **design references for an Angular + PrimeNG implementation**, not the implementation itself. They bind the *Design System for Hamoni* and share a component system in `screens/assets/`. Each screen is a self-contained HTML file.

### 38.1 The implementation stack · **Decided (2026-10-07)**

- **PrimeNG in styled mode**, driven by a **generated Hamoni preset**: the registered palette, the radius scale (3 / 5 / 8 / 10) and the two type families are mapped onto PrimeNG's semantic tokens (`primary`, `surface`, `content`, `text`, `form-field`), so the library re-colours from one preset.
- **Tailwind is the layout layer**, not an alternative to styled mode. It carries the shell, spacing and the bespoke surfaces. A Tailwind theme derived from the same tokens keeps the two from diverging.
- **Unstyled (pass-through) is per-component**, reserved for a control the preset cannot express. A full move to unstyled is not the default.
- **Icons: PrimeIcons.** The quick-pass SVGs from the wireframe are retired. Icons sit on one scale — **12 (caption/badge) · 14 (controls, rows) · 16 (navigation, headings)**.
- **Assets are project outputs.** Faces (Inter, JetBrains Mono), PrimeIcons and the logo live under `screens/assets/` (`fonts/`, `icons/`, `logo/`) and ship with `sync-screens.sh`.
- **The prototype keeps working behaviour where it is cheap, but each component must function with its styling.** The prototype CSS/JS is forked for this set; the wireframe set is not modified.

### 38.2 The component register

Controls identified so far, with their PrimeNG counterpart. **Status** is per row. Extensions are added here as they are found; a working demo lives at `screens/controls.html`.

| Component | Where it is used | PrimeNG | Status / extension |
| --- | --- | --- | --- |
| Button | everywhere; the one page action | `p-button` / ButtonDirective | preset — primary · secondary · ghost · danger; sizes default / sm / lg |
| Icon control (`.sctl`) | list and table row actions, export / refresh | `p-button` (text, rounded) | preset + a fixed square |
| Segmented scope control | the lists | `p-selectbutton` | preset |
| Status badge | row state | `p-tag` / `p-badge` | **extend** — **four neutral highlight levels, never a hue** (`default` → normal · `contrast` → increased · `info` → normal alternative · `secondary` → reduced); five process states on one icon vocabulary — Active · Pended · Exception · Completed · Cancelled; `success` / `warn` / `danger` reserved for RAG status |
| Alert pill | RAG status — a thing's health against its target | `p-tag` severity `success` / `warn` / `danger` | **extend** — the one pill that carries a hue: a soft fill, a matching hairline and the colour on the label (RAG green / amber / red), always with an icon; a separate axis from the neutral status levels |
| Filter chip | the structured panel | `p-checkbox` + a chip shell | **extend** — chip-wrapped checkbox |
| Prompt box + structured panel | the lists (§22) | `p-inputtext` + `p-overlaypanel` | **extend** — one query composed two ways |
| Field inputs | filters, dialogs | `p-inputtext`, `p-datepicker`, `p-select`, `p-inputtextarea` | preset |
| Switch | preferences | `p-toggleswitch` | preset |
| Popover / menu | rail identity, org menu | `p-popover` / `p-menu` | preset |
| Dialog / scrim | confirmations | `p-dialog` | preset; reason-required variant |
| Table + row expansion | the gold-standard list (§24) | `p-table` with row expansion | **extend** — the row anatomy, the dynamic area, the state-control cluster |
| Empty state | list with no results | custom | **extend** — a shared empty component |
| Loading skeleton | list refresh | `p-skeleton` | preset |
| Inline error / flash band | confirmations, failures | `p-message` / `p-toast` | preset |
| Tooltip | icon controls | `p-tooltip` | preset |
| Team selector | project security card (§30.1) | custom | **extend** — typeahead + chips (already specced in §30.1) |
| Version box (`.vbox`) | everywhere a version is shown | `p-tag` | **extend** — mono version chip |
| Search field (topbar) | every screen | `p-iconfield` + `p-inputtext` | **extend** — the `kbd` shortcut hint (⌘F / Ctrl F) |
| Conversation peer | topbar; the AI channel | `p-overlaypanel` | **extend** — teal border/icon only when there is AI activity |
| Conversation cards | Home (§6) | rendered content, not a control | **extend** — turns (agent plain on the panel, the person's as a bubble), structured objects capped at 60% (80% mobile), tables at 100%, the plan card; the conversation is a panel whose contents are the darker insets (§38.17–§38.18); documented on `controls.html` |
| Next-action bar | Home (§6); any surface that carries the conversation | rendered content, not a control | **extend** — one bar just above the chat bar; holds the **latest turn's** forward action(s) in **teal** (or the neutral suggestion set when no specific action applies); never transcript, so a superseded action disappears rather than greying; while the field has focus the send is the forward move, so the bar's teal stands down (§38.19) |
| Actions icon control | topbar, every screen | `p-button` (text, rounded) | **extend** — an `.sctl` square with a neutral count chip; approvals and access requests have no page of their own (§3) |

### 38.3 The logo · **Decided (2026-10-07)**

- **A logo appears on every page**, top-left in the rail — **except the process designer in immersive mode**, which is chromeless.
- The mark is the **abstract circular mark in brand purple**, transparent. The logo is **exempt from the registered palette and is never recoloured**, so it ships as its own asset (`screens/assets/logo/hamoni-logo.svg`, 280 × 288) rather than through `--accent`. It is copied verbatim from the active design language's `brand.json` → `logo.primary`.
- **Two lockups, two contexts.** The brand permits the **mark alone** or the **mark with `HAMONI` in caps**. The **rail keeps the mark alone**: it is in-app chrome, it collapses, and the page action sits directly beneath it (§38.9) — the mark is a landmark, not an introduction. The **standalone card** (sign-in, invitation, chooser, error) carries **mark + `HAMONI`**, because that is an arrival surface where the person may have come from an email, a bookmark or an SSO redirect and may not yet know whose page they are on.
- **The wordmark is text, not part of the asset.** `HAMONI`, Inter 700 at 13px with +0.06em tracking, `--fg`, beside the 22px mark and inside the same link. The official lockup SVG exists in the design language but is **not stored in this project** — the card sizes the mark at 22px to match the rail, and the lockup's proportions would force the mark to roughly 46px for legible letters. If Inter fails to load, the wordmark's letterforms shift slightly; accepted as a rare case.
- **The brand link's destination is contextual.** In the **prototype** the lockup points at `https://hamoni.ai` (new tab, `rel="noopener"`) so a review never navigates away from the design. In the **application** it must point at **`/`** — the product's own home, never the corporate site. A logo that bounces a signed-in person out of the app is a broken affordance. The error page's **Back to home** is `/` in both.
- **Correction (2026-10-27).** An earlier "never add a wordmark" reading was inferred from the only asset then available (`Logo-notext_transparent`). The brand specification permits the lockup, so the card's wordmark is compliant; the rail's mark-only treatment is a deliberate choice, not a restriction.

### 38.4 Control scale · **Decided (2026-10-07)**

- **Heights: 24 / 32 / 40**, with a **44px minimum touch target** on mobile. One focus ring (accent, 2px), one disabled treatment (opacity 0.42, `not-allowed`).

### 38.5 Colour — now / next, and the accent budget · **Decided (2026-10-07)**

Two signals, no more. **Purple is *where you are*; teal is *where you go next*.** Everything else is greyscale and earns its place by hierarchy, not hue.

- **Purple = now.** The page action (the command for this view) and the **selected row/panel** — the row's title turns accent when open, and a focused panel takes an accent border. One "now" per view.
- **Teal = next.** The single **forward** action out of the current context, carried as a **teal border** on the control (neutral text), not a second solid fill. The **AI/system voice** is the same signal — the system telling you what to do next — so AI content is teal in *form* (a chip, an anchor), never as large teal text.
- **Escapes are neutral.** Pause and cancel go *backwards*; they are never teal and never purple. The row's state controls are neutral icon controls.
- **Hover is neutral** (a surface raise); only **selection** takes the accent.
- **The interaction layer is part of *now*.** The focused control is where you are, so **focus is the accent** — the same purple — and it is **uniform on every focusable control**: inputs, selects, textareas, buttons, chips, segments, table rows and icon controls. Non-interactive elements never show it. A field's background is **`--bg` at all times**; the purple is the ring, not a background change. *(Carry to the design system — §38.8.)*
- **States stay neutral.** The status highlight levels carry **no hue** — state reads through fill, border and label — and Cancel keeps the highest-contrast neutral fill. (The seed's `success` / `warning` / `error` were unregistered; three **RAG status colours** now are — `--success` #4AD371, `--warn` #F4CF4A, `--error` #FF8389 — for RAG *status* only, a separate axis from the four neutral highlight levels.)
- **Fold count:** at most one purple and one teal per view.
- **PrimeNG / Tailwind:** purple maps to `primary`; teal is an `extend`ed `info` semantic token; both are the same CSS variables Tailwind reads.

### 38.6 Typography — three voices · **Decided (2026-10-07)**

Two families, three voices. The goal is to break the "wall of text" in dense views by *form*, not by more colour.

- **Content** — Inter 400 / 600, foreground. The words people read.
- **Label** — Inter 600, 11px, uppercase, +0.07em tracking, muted. Field labels, table headers, eyebrows. (The **rail group headings stay mono** — the one place the label voice is system chrome.)
- **System** — JetBrains Mono, muted: **identifiers** (the process id), versions, and code. **Numerics and dates use Inter with `font-variant-numeric: tabular-nums` — not mono.** Mono is a texture for system-generated strings, not the default voice.
- **Emoji fallback** — the standard stacks (**display** and **body**) carry a **monochrome emoji face, `Noto Emoji`**, as a fallback right after Inter, so emoji render and can be tinted by CSS (the hero mood mark, §38.23). **The mono stack never carries an emoji face — emoji are never set in mono.** The face is **bundled** in `screens/assets/fonts/noto-emoji/` — 11 self-hosted woff2 subsets from Google Fonts, wired by `@font-face` in `app.css` — so the monochrome glyph (and its CSS tint) is identical on every machine, Windows and macOS included. *(Inter stays first, so text is unaffected; the emoji face is only reached for codepoints Inter lacks.)*

**Row anatomy (the gold-standard table, rebuilt):**

- **Identity** — `<process id> · <process name>` on one line: the id in mono, the name in Inter 600. The **name and the state pill are the row's two foreground elements** — the name larger, 600 and unboxed on the left; the pill small, uppercase and boxed on the right — so the row balances rather than leaning left. The id, Started, Due in and rank stay muted, and a **reduced** state recedes to `--dim`, so the balance tracks relevance. A `·` separates the id from the name.
- **The relationship is words, in the expanded view.** A muted line, at the same level as the forward action, left-aligned: "You started this process", "You are the process owner", "You have an active task", "Your team has an active task". **No relationship, no line.** No icon and no per-row marker — the leading gutter is gone; the row number stays dropped (position is the rank).
- **Long process names truncate** with an ellipsis on one line (the full `id · name` is available on hover), so a dynamic name never stretches or wraps the row.
- **Hover** is a neutral surface raise; **selection** turns only the name accent and raises the row.
- **Due in carries no highlight** — overdue is not a colour, it is a **structured boolean filter** ("Overdue only": checked = only overdue, unchecked = all). The column is empty when the clock is not running (no "paused" text).
- **Dates** — one format, the longest: datetimes `D MMM YYYY, H:MM AM/PM`, dates `D MMM YYYY`, times `H:MM AM/PM`.
- **Forward actions are teal**: the expansion's "Open process" (teal border) and, on a selected pended row, "Resume". Pause and cancel stay neutral. The **query summary** (scope + states) sits centred and unhighlighted in **its own bar** above the structured panel.

### 38.7 Gold-standard table — notes carried · **(2026-10-07)**

- **Cancel is a screen, not a modal.** In the prototype, cancel opens a dialog; the real behaviour is a **dedicated confirmation screen modelled on pend** (§19.15), so a state change never happens in a popup. The dialog is marked in the markup as a stand-in.
- **The relationship is textual, not an icon.** A per-row relationship marker was tried in a leading gutter and dropped: it was ambiguous and got in the way. The relationship now reads as a muted sentence in the expanded view (§38.6). No custom icon set is needed.
- **The exception state is muted.** Exception carries no highlight — the badge uses the same muted tone as the others, distinguished by its icon and label. The product cannot assume an exception is what the user is looking for.
- **Exception is recoverable.** An exception row carries **resume (retry/continue) and cancel (not recoverable)** — the same two actions as a pended row, with resume teal only when the row is selected.
- **"Apply filter" is the next action** while the structured panel is open, so it carries the teal border.

### 38.8 To carry back to the design system · **(2026-10-08)**

These were settled while building the high-fidelity screens and belong in the design language, not only in this set.

- **The focus ring is the accent, and it is uniform.** Focus is the interaction layer and part of *now* (§38.5): every focusable control — inputs, selects, textareas, buttons, chips, segments, table rows, icon controls — shows the same accent ring; non-interactive elements never do. A field's background is `--bg` at all times; the purple is the ring, not a background change.
- **One dark-fill control language.** The inputs, the selected filter chip, the selected scope segment **and the checkbox / radio box when selected** all share `--bg` — the checkbox mark is the same quiet inset box the chip wraps (transparent off; `--bg` and a `--muted` border on, with a small `--fg` tick; a radio takes a `--fg` dot). A checkbox never takes a light fill. Selection is never shown with a light fill or a font-weight change.
- **The status pill: four neutral levels, one coloured family.** A process state carries **no hue** — it reads through fill, border and label: *normal* (`--fg` label, hairline), *normal · alternative* (the same, dashed), *increased* (the `--bg` control fill, like the selected checkbox and chip), *reduced* (`--dim`, so history recedes). A **RAG status** is the one pill that carries colour — a soft fill, a matching hairline and the colour on the label (green / amber / red) — and always with an icon, so it never relies on colour alone. State is neutral, RAG is coloured: two separate axes. A pill is information, never an action.
- **The filter chip** is a low-contrast toggle: transparent when off; `--bg` when on; hairline border off, `--muted` border on; muted label off, `--fg` label on; a small check. No weight change, so toggling never reflows.
- **The date range** is one box, **dates only**, localised; the filter is day-granular, because the start time is rarely the question.
- **The prompt box** is a natural-language field: **no chevron** (the structured panel below is seen, not signalled); on focus it **expands left, up to the scope control**; opening the panel does **not** steal focus from the prompt box.
- **`screens/controls.html` is the design-language artefact and must be back-propagated.** Each agreed component (buttons, icon control, scope control, filter chip, text input, date range) carries the **HTML as PrimeNG** and the **CSS** beside the rendered example, so the code is the source of truth and a screen bug is distinguishable from a design difference. The purple/teal description there is the now/next model (§38.5).
- **The topbar carries two peers of equal treatment** — **Search** and **Conversation** — both on the same field surface (`--bg`). Shortcuts: **`⌘/Ctrl+F` for search**, **`⌘/Ctrl+/` for the conversation** (one key per hand). The `/` and `⌘K` shortcuts are retired. The `kbd` hint stays on the search (a major architectural piece); the conversation carries its shortcut in the tooltip.
- **The user selector is one common component** with two settings: **show text** (boolean) and **preferences-only** (boolean — shows only the preferences, not the organisation selector), so the same control works on a normal screen and in immersive mode. It always uses the same background colour. Its on/off control (the Advanced-mode switch) turns **dark** when selected, like the pills, and its label never changes weight or colour — the track carries the state.
- **The boolean control (switch)** is a component: on turns the track **dark** (like the chip and segment); the label never changes weight or colour; a **locked** switch is dimmed and non-interactive. It is shown on the controls page with its HTML and CSS.
- **The user-selector popover.** It opens with an **account header** — avatar, **name**, and the **email** from the OIDC claim — so a person can tell which account they are on (§7). Section titles use the **rail style** (mono, uppercase, muted), so the menu reads as part of the rail. Organisation rows use **13px main / 12px sub**, never mono; the current organisation carries a **tick**, not a "Current" badge and a "Switch" button — the rows are the control. **Hairlines are for data rows only** (the organisation list); sections are separated by **space and a title**, not by a rule — so a bar always means "another row", never "another section". A **Dark mode** switch is placed but **locked on** (light mode is not designed yet). The org sub-text states the role for the current org and "Available" for the others — the signed-in state is not repeated.
- **The row-disclosure chevron leads the row.** It sits at the **left edge**, before the relationship marker, so it reads as structure and never as an action (the action cluster is on the right). Left = open this row; right = act on it. A **one-time hint toast** ("Click any row to open it in place") teaches the affordance to a new user and then never returns — the persistent chevron is the real affordance; the toast is a complement, not a substitute.
- **The conversation component carries its own conventions** (§38.17–§38.19). It is a **panel** — the lighter surface, like every panel — whose contents (the composer, the person's bubble, the structured objects) are the **darker insets within it**, so the chat stops blending into the page and matches the panel-then-control pattern of the rest of the system. **The agent's chat is plain on the panel, never a bubble**; only the person's messages are bubbles (right-leaning, `--bg`). A **structured object** — a team, project, process, task or document — is a recessed card carrying a **type eyebrow · a link title with the `pi-external-link` glyph (plus an optional mono version) · a what-was-done summary**, and it caps at **60% of the chat width (80% on mobile)**. A **table is not a structured object** and may use up to **100%**. A **next-action bar** sits just above the chat bar: one surface, always reflecting the **latest turn only**, holding the turn's forward action(s) in **teal** (or the neutral suggestion set when no specific action applies) — never part of the transcript, so a superseded action disappears rather than greying, and its teal stands down while the field has focus so only one forward control is ever live.

#### The technical export — the preset, the Tailwind theme and one token stylesheet · **(2026-10-15)**

The design system leaves the prototype as importable code under
`project/control-room/control-room-ui/src/styles/`. `screens/assets/app.css`'s
`:root` becomes **`tokens.css`**, the single source both layers read; nothing
below writes a colour of its own.

- **`hamoni.preset.ts`** — `definePreset(Aura, …)` maps the tokens onto
  PrimeNG's semantic tokens: `primary` = purple (`--accent`), the `info`
  severity = teal (`--info`), plus `surface`, `content`, `text`, `formField`,
  `overlay`, `list`, `navigation` and `highlight`. Every value is a
  `var(--…)` reference or a `color-mix(in oklch, …)`, so the library re-colours
  from one file and never from a component.
- **`tailwind.theme.css`** — Tailwind v4 `@theme inline` derived from the same
  variables: colour, radius (3/5/8/10), the three font voices, spacing (base
  4px, so 1/2/4/6/10 = 4/8/16/24/40 keeps the 8px grid), the control heights
  (`--ctl/--ctl-lg/--touch`) and the text scale. A Tailwind utility and a
  PrimeNG component resolve to one value.
- **`styles.css`** pins the cascade order — `theme, base, primeng, components,
  utilities` — so PrimeNG (wrapped in its own layer) sits above Tailwind's base
  and below utilities. The three type voices ship as `.label-voice`,
  `.system-voice` and `.numeric`.
- **States are designed pairs, not opacities.** Button hover is
  `color-mix(--accent 88%, --fg)`, active `--accent 82%`; a field's hover border
  is `--muted`, its focus the `--accent` ring plus a `--accent-soft` halo;
  disabled is `opacity 0.42`. No hover or focus lowers text contrast.
- **The surface ramp** (`surface.0`–`950`) is a mix-only ascension of
  `--bg/--inset/--surface/--raise/--fg`; borders are the explicit
  `*borderColor` tokens, never a surface step. Selection takes the accent
  (`--accent-soft` fill, `--accent` text); hover stays neutral.
- **`--info` is the AI voice and the "next" action in *form* only** — the
  `info` button severity is a teal border with neutral text (§38.5), the tag
  and message carry teal softs; success/warn/error stay neutral.
- **Light mode is provisional and derived, not designed.** The export carries a
  light scheme mixed from the same registered hexes (canvas toward white, the
  greys from `--fg`); a light-mode design pass re-syncs the real values. Dark
  mode remains the registered palette. The dark-mode selector is **`.app-dark`**,
  applied on `<html>` — the same class drives PrimeNG's `darkModeSelector` and
  Tailwind's `dark` variant.
- **Fonts are self-hosted, never loaded from a CDN.** The three families —
  Inter, JetBrains Mono and Noto Emoji — ship as pinned woff2 files under the
  app's `public/fonts/` (served at `/fonts/…`), and no stylesheet imports
  `fonts.googleapis.com`. Four reasons: **privacy** (a font CDN sends every
  visitor's address to a third party, which an on-prem product should not
  require — ADR-0037); **version pinning** (a CDN serves the family's current
  build and can change its metrics without notice, so pinned files keep the
  design reproducible); **reachability** (no external host for a strict
  `font-src` content-security policy, a corporate proxy or an air-gapped
  deployment to block); and the **monochrome emoji face** (Noto Emoji sits in
  the text stacks as a CSS-tintable fallback — §38.6, §38.23). Cross-origin font
  caching is now partitioned per site, so the CDN's shared-cache advantage no
  longer applies. Licences travel with the files and are separate from this
  project's own Apache 2.0: Inter, JetBrains Mono and Noto Emoji are **SIL Open
  Font License 1.1**; PrimeIcons is **MIT** — recorded in
  `screens/assets/fonts/README.md` and
  `control-room-ui/NOTICE.md`.
- **The type voice reaches the controls.** PrimeNG's button label is bound to
  the set's control text — `button.root.fontSize` at **13px** (`--fs-sm`) and
  `button.root.label.fontWeight` at **600** — so a PrimeNG button matches the
  screens' control type. (PrimeNG exposes a single label-weight token, so every
  variant takes 600; the prototype's lighter outline/ghost weight is not
  separately token-bound.) The same control text binds the fields —
  `inputtext`/`select`/`textarea` at **13px** — and the **filter chip** is the
  low-contrast toggle (§38.2 *extend*): a chip shell whose box is the dark inset
  treatment (hairline off; `--muted` border and a small `--fg` tick on), never
  the light checklist checkbox.

### 38.9 The shell — page head, page action and scrolling · **Decided (2026-10-08)**

- **No visible page title on list screens.** The rail names the section and the breadcrumb names the place; a third "Processes" heading repeats both. The page keeps a **hidden `h1`** for structure, landmarks and screen-reader headings.
- **The breadcrumb does not follow the rail.** It reads **Home / …**, exactly: the rail's groups (Work / Manage / Design / Configure) are categories, not destinations, so they never become crumbs; the leading **Home** link is the only route back.
- **The page action lives at the top of the rail, under the logo** — below the header-separator level so it reads as belonging to the page, and aligned with the top of the main content. There is extra space above and below it so it separates visually from both the header and the rail's navigation, and the main content keeps clear space below the header separator. No rule ever sits directly above the button.
- **The page scrolls as one document.** The topbar is **static and opaque** (`--bg`): no sticky positioning, no translucent blur, no backdrop filter. The header scrolls out of scope with the page, exactly like the rail and the content. Any scroll-offset logic in JS must assume no pinned bar.
- **Rules in the rail only encode data**, not chrome: the brand row carries **no border-bottom** and the profile selector carries **no border-top** — a hairline in the rail will not read as a divider between shell regions.

### 38.10 Usability review dispositions — carried to the build · **(2026-10-08)**

Findings from a best-practice review of the process list (data presentation, readability, control semantics). All are **accepted as deliberate prototype simplifications** — the demo is not the implementation record. The real Angular + PrimeNG page must not inherit them:

- **The pinned preview is intentional.** The reviewed case (CLM-2026-44871) leads the default order and the Started / Due in values display oldest-first through a display reversal — so in the default view a row's shown values are not its own, and the Started sort toggle does not change the visible order. **The real page sorts truthfully**; if oldest-first diagnostics and a pinned case are both wanted there, that is a compound/product sort decision to make in the implementation.
- **Any filter change re-queries the list.** The product intent is that process state is old the moment it is read, so every change re-queries. The demo's mixed commit model (some filters instant, dates + keyword behind Apply) is a page-local artifact, not a design.
- **Focus must survive update cycles.** A **requirement for real pages**: expanding/collapsing a row, re-sorting, and acting on a row (resume, cancel, refresh) must preserve and return focus — never drop it to the page body.
- **Correct control semantics come from PrimeNG.** The demo's tabs-as-scope (no arrow keys, no tabpanels), its prompt-box click/Escape edges and similar quirks are local; the real page uses `p-selectbutton` / `p-checkbox` and the library's focus and dismissal behaviour.
- **Polish items are noted, not actioned**: off-grid paddings (13/14/15/18px), 32px row controls on touch (44px minimum), the idle sort glyph on every sortable header, and "Due in … ago" wording — recorded here as future polish for the build.

### 38.11 Home — the prompt-first screen · **Built (2026-10-08)**

- **The prompt is the page; the conversation is at full width on it.** The five intent classes each map to one renderer — **Find, Explain, Act, Create, Measure** (§6) — and every reply carries its class as a teal chip (the AI voice in chip form, never large teal text). The tryline offers one example per class. Explain was deferred from the processes pass and is designed here: **one run, one timeline** — times as Inter tabular figures, plain sentences for what happened, a verdict ("waiting on you, not the system") and a teal *Open the case* anchor. Explain changes nothing.
- **The page action lives in the rail, under the logo (§38.9).** Home's is **Start process** (the one solid fill), opening the **start flyout** anchored below the rail action instead of a wide overlay from the page head. The page head keeps only the greeting and the prompt — the duplicate start control is gone; create/start lives in exactly one place per screen.
- **A plan awaiting confirmation is the only loud thing (§6).** While one pends, the rail action keeps its place but goes unseen (`visibility: hidden` — the rail's rhythm must not shift), the summary's actions hide, and **Apply** is the sole solid button in the viewport.
- **First-run onboarding (§6.1).** The composer moves to the foot of the conversation, the page head reframes to "Let's get you set up", the tryline hides, and the closing CTA (*Upload claims list*) reads **teal** — it is the next step, not a page action, so the rail action and the one teal coexist. `?onboard=1` forces the state.
- **Copy made honest against its own rows.** The Find reply's head stated "6 processes you can start" while two rows need access — it now reads "4 you can start, 2 need access". The reassign plan and its handoff name people from the fixed cast (Tom Alvarez took Bob's slot); the onboarding scripted world stays as §6.1 wrote it (the avowed fiction of a brand-new org).
- **Numbers read as one day.** Greeting aligned to the data world — "Monday 5 October", the day CLM-2026-44871 started. Summary reconciles with the rail: 3 due in the next hour (of 7 open), 11 in the Inbox (7 open + 4 completed this week), 12 waiting in the team queue.
- **Shell deltas shipped to `screens/assets/`:** the search field's focus is the **accent ring** (§38.5 made uniform — the topbar search was still a muted border), and plan-pending now also silences a rail-held page action. The rail navigation is identical to the processes pass (spans for unbuilt screens). The topbar keeps search only — Home is the one screen where the bar has no conversation peer, because the page *is* the conversation — and the Actions entry stays (approvals have no page, §3); it was unwired in the earlier port and now opens through the shared menu behaviour.

### 38.12 Home — second pass · **Built (2026-10-08)**

- **The greeting and the visible head are gone.** "Good morning, Celia" reads as patronising, and the screen already names itself twice (rail group, breadcrumb) — the breadcrumb carries the place, so the page keeps only a hidden `h1`. The onboarding step's old head reframe ("Let's get you set up") was carried by the `h1`; with it gone, the assistant's own first turn carries the setup.
- **The chat example came across from `home.html`.** Home now **opens on the onboarding conversation by default** (§6.1's first run) instead of an empty prompt, and `?demo=act|create|measure|find|explain` deep-links a single intent class for review; `?onboard=1` still forces the first-run state.
- **The conversation is one box (§6·§7 — option B).** History lives **inside** the box and the composer sits at its foot, so the box is a component, not a page section: later surfaces bring the same box up as an actual box, and the task/chat pattern (§15.9) already behaves as a canvas widget. The box is **vertically centred** in the start state with a sensible height (`min(56vh, 540px)` inside a 60vh stage), scroll contained inside the log; the tryline is the box's empty state, the held-state strip hides during the first-run example (the assistant carries the frame), and the newest turn is scrolled into view. Home-v2's alternative — history *above* a box that drops to the page's bottom — was declined for consistency's sake.
- **The page action is dynamic.** With assigned work it reads **Start work** (solid, after Inbox's *New* in the §3 vocabulary) and opens the next item — the single-task stand-in needs a console of its own (§9). With an empty queue it becomes **another action, as yet undefined** — open, recorded here and in the code path (it degrades to a neutral secondary). Candidates: a "New request" handoff, or refocusing the prompt.
- **The start flyout from §38.11 is retired.** Its trigger was "Start process"; the page action now answers the person's work, and starting a process collapses back to §6's one mechanism — the prompt (interception: reuse / fork / blank) or the Processes page note. The permissions rule (shown, not hidden) survives in the Find renderer.
- **The work card yields.** Its secondary actions are **unhighlighted/unavailable until we select that console** — we design the rest of the page before designing this card — so See work and Do next are disabled, and the page-level Start work is the only route to the work. The card's own design is deliberately untouched.
- **Actions got the recommended treatment (an option was requested).** The topbar was crowding, and the Actions items are non-process pick-ups (approvals, access requests) needed on every page: it is now an **icon control in the topbar's right cluster** (`pi-bell`), with a **neutral count chip**, consistent placement at the far end of the bar on every screen. The other options considered: (2) a persistent row in the rail above the identity chip — frees the topbar entirely but turns a momentary pick-up into a second permanent column; (3) a Work rail item — needs a page, contradicts §3's "no page of their own"; (4) a section of the user menu — one popover everywhere, but approvals are day-work for a manager and hiding them behind identity costs three clicks. Open: whether the Actions panel should remain top-right of the topbar or drop, on wider screens, near the rail.

### 38.13 Home — the conversation as chat · **Built (2026-10-08)**

- **The gold standard has moved on.** `processes.html` was the pattern source for tables; beyond tables it is not authoritative — screens do not read it for shell or list behaviour now. *(Recorded so future passes stop anchoring on it.)*
- **A conversation, not a document (§6).** The transcript re-rendered: as a chat — **my messages lean right, the AI's lean left, position carries identity** — so the participant mono-chips (CY / AI) are gone. AI text is plain; a person's message carries a surface bubble.
- **No per-block headers.** The reply head strip (title + intent badge + meta) was burning vertical space and fragmenting attention. Every agent output is an **object block** — an artefact beside the chat, like something you'd created — that shows its own information (its own first line names the object: "Processes you can start — 6 match…"), with its actions in a foot. Block heads, the intent badges and the plan's confirmation badge are all gone; the plan's "nothing changes until you apply" is body text, and Apply is its one solid button.
- **Layout follows the fill pattern.** Content starts high (the same top start as the tables' screens) and the box **stretches from the top toward the bottom**; the **My work card holds the bottom** of the viewport. By default the box is **smaller (46vh)**; **selecting it to type stretches it** into the free space (`:focus-within` grows it; small screens still flow).
- **Teal corrected.** The teal-as-AI-voice reading (§38.5, `controls.html`) was an **assumption to be verified** — and its over-use here proved the point: every turn spoke teal. On Home, teal is the **next action only**: the **send control — now iconic — turns teal while the field has focus**, and hover neutralises back. Intent badges, the AI chip and mid-block forward anchors ("Open the case", "Upload claims list") get no teal; the colour will return once the rule is verified and re-registered.
- **The send button is iconic** — a 36px round control with the arrow-up glyph, neutral until the field is focused. The **sparkles glyph is removed**; its space carries the attach affordance: **Attach** opens a two-source menu — *Upload from my computer* (a real file picker, chat-scoped: there is no requirement to save an upload into the document store) and *Attach from Documents* (the platform's document store, §8) — with removable chips above the composer line.
- **The box log's scrollbar is softened**: thin, rounded thumb in the bar tone, transparent track (WebKit + standard properties).
- **Deferred, before the screen is finalised (TODOs):** ① an **empty-chatbox example** of this new fill layout (box idle → stretched, with the tryline as the empty state); ② the **composer placeholder**, potentially dynamic ("The use of teal" review continues alongside); ③ the **Actions treatment tidy-up** (accepted as-is for this pass). `%demo=` deep-links for the new renderer structure stay available.

### 38.14 Home — placement, shape and the preview's icons · **Built (2026-10-08)** *(conversation state model is broken and is its own pass — next)*

- **28px rhythm.** 28px from the top of the page to the top of the box, and 28px from the box to the My work card — the same measure, and it killed the sense of the box floating.
- **Placement rules, encoded as one small state machine; the box must not bounce around:**
  - **No chat ongoing** — the box sits **vertically centred**, sized for the reply area plus a few lines of the agent's opening text (a 280px flex basis). The seeded onboarding opening *is* that pre-filled text; nothing counts as "ongoing" until a person has replied.
  - **Focusing the composer expands the box to full height (plus spacing)** — `flex-grow` animates it, 0.35s on the standard easing, so it is one motion and not a jump. **The attach icon alone never expands the box** (the menu must not move away from the mouse); holding it open suppresses collapse, and **selecting something to attach expands it** and returns focus to the field.
  - **A chat that is ongoing stays expanded.**
- **Attachments render as pills at the composer** — chat-scoped, removable, `from Documents` on the platform source.
- **The composer's text is the default size** (14px) — the 15px had no need.
- **The send control is square** (32px, radius-sm) — the circle was the only round control in the set; it keeps the teal-while-focused behaviour.
- **Structured object titles are the reference**: the title is **clickable** and is followed by the **`pi-external-link` glyph**; foot CTAs that duplicated the title link (Open the processes list, Open the case, Open Activity, Open the queue, Open in the designer) are gone. Plans and states ("Reassign …", "Applied") are plans, not referents — their titles stay text.
- **Icons in the OpenDesign preview broke intermittently.** Cause read as the icon font painting before it arrives (`font-display: block` window) during a scroll whose content re-renders late. Fix in shared `app.js`: all faces and the icon font **preload at page start**, before any interaction renders glyph content. *(To confirm on the affected screen after the next pass.)*
- **Still deferred:** the **conversation state engine itself** — it is broken and is the next pass — plus the empty-chatbox example, the dynamic placeholder, and the Actions tidy.

### 38.15 The composer — an outer box · **Built (2026-10-08)** *(the conversation engine still awaits its own pass)*

- **The prompt area is its own box.** Inside the conversation box the composer is a bordered outer box (surface, radius-lg) that encompasses the prompt input, the attach control and send: **attach sits at the bottom-left, pills between, send at the bottom-right** — the corners read as the same control from every angle. While the composer holds focus, **the outer conversation box stands its ring down** and the composer carries the accent — never two rings at once.
- **Agent-prompt semantics throughout: Enter sends, Shift+Enter makes a new line** (already carried; now definitive). Text is **the same size as the chat text** (13px) — the larger input had no need; line-height and spacing are kept.
- **Vertical growth rule:** new lines grow the input downward always keeping **one line of space above the attach/send band**; the composer's bottom is fixed in place, so growth **pushes the chat history upward** rather than moving the buttons. The pills live in that band, between the two controls; an empty row simply lets send hold the right corner.
- The expand/collapse placement rules of §38.14 are untouched by this restructure: the composer's height is part of the idle basis (280px covers a three-line field plus a few opening lines).

### 38.16 Home — the speech-bubble grammar · **Built (2026-10-08)**

- **One actor per bubble.** Every turn is a single bubble and the actor is the bubble's owner — the AI leans left, the person leans right (§38.13); a continued assistant turn is simply a second left bubble. **Newlines inside a message are separate paragraphs**, spaced by the bubble's `> * + *` rule — never one wrapped block.
- **Structured objects sit below the bubble they belong to**, in the same message column: bubble first, object under it. An object head is **name + version**, where the name is the clickable reference (`.obj-title`, external-link glyph after) and the version rides in the **mono identifier voice** (`.obj-ver`); a one-line description follows (`.obj-desc`).
- **Deep-links in speech are bold with the `pi-external-link` glyph after** (`.dlink`) and have **no target yet** — the destination is a later decision. This is the inline counterpart to a structured object's title, which *is* the live reference.
- **The onboarding conversation is rebuilt to the supplied ten-item script** (§6.1): welcome → team name → team object → invite → first process → process description → project object v1 → SLA → project object v2 → the closing upload invitation. Items with no structured object are just the bubble; the team object links to `teams.html`, the project objects to `project-detail.html`.
- The closing bubble carries its upload invitation as a small action row **inside the bubble** (`.msg-actions`), keeping item ten a bubble with no "object". The upload result and its "not now" branch follow as steps 7–8.
- Untouched: the §38.14 placement rules and the §38.15 composer. **Still deferred:** the conversation state engine pass, the empty-chatbox example, the dynamic placeholder, the Actions tidy.

### 38.17 The chat grammar — background, shortcuts, objects and tables · **Built (2026-10-09)**

General rules, settled here and binding on every screen that carries the conversation component:

- **The agent's chat is plain on the canvas — never in a speech bubble.** Only the person's turn is a bubble (raised `--surface`, right-leaning). Position carries identity: AI left, person right.
- **Shortcut actions sit just above the chat bar and change with context.** They are the moves that suit what is on screen, not a fixed menu. On Home they render as a quiet chip row (`.shortcut`); the seeded onboarding's close offers **Upload claims list**, every other state offers the default set (reassign / create / measure / find / explain).
- **A structured object** — a team, project, process, task, document — **is a recessed container** (`.obj`, `--inset` fill so it never reads as one of the person's bubbles): a **type eyebrow** (`.obj-type`, the label voice), a **link title** (`.obj-title`, `pi-external-link` glyph after) with an optional **version** (`.obj-ver`, the mono identifier voice), and a **summary of what was done** (`.obj-desc`) below.
- **Objects wrap their content and cap at 60% of the chat width on desktop, 80% on mobile** (`.obj { max-width }`; the 920px media query raises it). Width is dynamic to content up to that cap.
- **Tables are not structured objects.** A table in the agent's output wraps its content and may consume **up to 100% of the chat container** (`.chat-table { align-self: stretch }`). In `?demo=find` the object holds the title/summary and the table follows at full width; the plan (`?demo=act`) is a `--wide` object because its body is a table.
- **Differentiation:** objects are `--inset` recessed with a head band (`.obj-head`, hairline under it); the person's bubbles are raised `--surface`. That is the whole distinction — no accent bars, no shadows.

Tidy-ups carried this pass:
- The onboarding's item-10 action row is gone: **"Upload claims list" is now a shortcut above the chat bar**; **"Not now" removed** (a person simply does not act, or types something else); **"Excel or CSV" removed** (the file is already known to be an Excel file, and a file-format hint has no place in the shortcut row).
- `.convo-empty`/`.tryline`/`.linkish` retires; the empty log is just the box, with shortcuts above the composer.
- The demo renderers (find, create, act, explain, measure, handoff) all move to the head-band object anatomy so the spec and the screen agree.

**Still deferred:** the conversation state engine pass, the empty-chatbox example, the dynamic placeholder, the Actions tidy.

### 38.18 The chat surface model — inverted to match the panel standard · **Built (2026-10-09)**

- **The conversation is a panel, so it takes the panel surface.** `.convo` moves from `--bg` (where it blended into the page) to **`--surface`**, exactly like `.panel`. Its contents are therefore the **darker insets within it**: the composer is `--bg` (the same background every `.input` / `.select` / `.textarea` already uses), the person's bubble is `--bg`, and structured objects stay `--inset` (a hair lifted from the bubble, still darker than the panel). Tables and the agent's plain chat sit directly on the panel.
- **A panel does not hover and does not ring.** The `.convo:hover` and `.convo:focus-within` accent ring are removed, and the old `:has(.composer:focus-within)` "outer stands down" rule with them. **Focus belongs to the composer** — the grouped input, exactly as `.input:focus` behaves — so only one thing ever carries the accent when you type. This supersedes the §38.15 "outer carries the ring" arrangement.
- **This answers the a/b/c question with (c).** (a) ringing the whole chat area and (b) ringing the chat with a white outer both fight the panel standard: a panel is a surface, not a control, so it should not ring; and a large accent ring around the whole conversation would be the loudest thing on the page for a routine act. Inverting the surfaces keeps the accent on the thing you are actually using and aligns the chat with every other panel-then-control pair in the system.
- **Consequences:** the send control's own background goes transparent (it now sits on the dark composer, like the other icon controls); attachment pills move to `--inset` so they read as raised pills on the dark composer; the controls.html specimen is updated to the same model.

**Still deferred:** the conversation state engine pass, the empty-chatbox example, the dynamic placeholder, the Actions tidy.

### 38.19 The next-action bar — teal, latest-turn only, never transcript · **Built (2026-10-09)**

- **Yes — the next action is teal.** Teal means "where you go next" (§38.5), and "Upload claims list" is exactly that. In the bar it is the **one teal** on the screen.
- **The bar is a control surface, not transcript.** There is one bar, in one place, just above the chat bar. It **always reflects only the latest turn** and never travels up the chat with the turn that produced it.
  - Per-turn travelling actions were rejected: a stale action either stays teal (wrong — the action is gone) or goes neutral (a dead affordance). So **history keeps no actions**; when the chat moves on, the previous next action **disappears**, it is not greyed.
- **The bar holds the turn's forward action(s) — teal — or, when the latest turn implies no specific action, the neutral suggestion set.** Suggestions (reassign / create / measure / find / explain) are prompts, not next actions, so they are never teal. `.shortcut.is-next` marks the teal one.
- **One next action at a time.** While the composer has focus the send is the forward move, so the bar's `.is-next` **stands its teal down** (`.convo:has(.composer:focus-within) .shortcut.is-next`). Focus never leaves two teals on screen.
- This refines §38.17: the bar is still the contextual shortcut row above the chat bar; what changes is that its **latest-turn forward action is teal**, while generic suggestions stay neutral.

**Still deferred:** the conversation state engine pass, the empty-chatbox example, the dynamic placeholder, the Actions tidy.

### 38.20 "Your day" — the supportive component · **Direction agreed (2026-10-09)** *(placement open — agree before build)*

The Home close replaces "My work" (a stat card) with a supportive, time-aware companion — the system's human voice on Home. Two reasons the card failed: its **"Do next" duplicated the page action** (the rail already owns Start work), and it was a **static stat wall** read once and ignored.

- **The one rule — the anti-patronising test.** Every line must tell the person **something they don't already know**: a comparison, a consequence or a forecast. "Three are due before noon" passes; "7 open in total" fails; a bare salutation fails (why the greeting went, §38.12). One figure at most, only when it is the point.
- **Inputs (the fixed world §10.6).** From `HAMONI.processes`: where `mine` is `own`/`working`/`started` → **yours**; `slaMins` → **due soon / overdue**; state `Pended`/`Exception` → **waiting on a decision**; HR Operations with `mine: false` → **the queue you could pick up**. One extension: a **`standardDay` pace model** (expected completions by weekday and hour, plus today / yesterday / records) so the line can say *ahead / behind / best*. Extend `sample-data.js`; never fork it.
- **Message families** (one figure max, always specific):

  | Family (trigger) | Example line | Icon |
  | --- | --- | --- |
  | Start of day | "Morning. Three of yours are due before noon — one is already waiting on you." | `pi-sun` |
  | Ahead of pace | "Six done before lunch — ahead of a normal Monday." | `pi-arrow-up` |
  | Steady | "Nothing needs you for the next hour. Next up is due at 2." | `pi-clock` |
  | Heavier than usual | "A heavier Monday than usual. Nothing's overdue yet — the queue can absorb the rest." | `pi-chart-line` |
  | Record | "Eleven done — your best Monday this month." | `pi-sparkles` |
  | Needs a decision | "Two items are waiting on your judgement." | `pi-question-circle` |
  | Offered help | "Want me to take the routine approvals so you can focus on the escalations today?" | `pi-comments` |
  | End of day | "That's the day — 11 done, 2 carrying into tomorrow." | `pi-moon` |
  | First run | "Nothing's assigned yet — there are 12 you could pick up." | `pi-inbox` |
  | Unresolved | (fallback copy) | `pi-info-circle` |

- **Form.** Text, with **one small family icon** — the icon carries the family and the dash of colour; the **text stays neutral**. The icon is the **system voice in form** (§38.5), so it is the one place a hue is allowed; a **neutral `pi-info-circle`** is the fallback when no family resolves. Just enough to catch the eye, not distract.
- **Accent.** Neutral text (agreed). No button ever.
- **Action.** **At most one inline action per message, neutral, and only when the copy genuinely carries one**; the destination follows the family — a **review/judgement** line opens the **structured work list**, a **help/explain/offer** line goes to the **chat** (focus the composer). The rail still owns the page action; the message never duplicates it.
- **Persona control function.** The line takes the **narrowest stance that is actionable now**: **participant** if the person has assigned work → else **manager** if they manage a team → else **process owner**. A manager who is also a participant resolves by that precedence (detail at implementation). Only the **participant** variant is in scope now; the other stances are recorded, not built.
- **Demo state.** One **positive** family is on the Home page for review; the full state set is deferred.
- **OPEN — placement (agree before build).** Either (a) the **live first line of the conversation box** (the box's header strip, not a transcript turn), or (b) a **separate strip above the box**. A true transcript turn is rejected: it would go stale, scroll away, and break the §38.19 rule that the action bar is latest-turn only.

### 38.21 "Your day" — merged as the conversation's first line · **reverted (2026-10-09)**

- **This pass was reverted at 11:00** — the merge corrupted `screens/home.html`; the screen and its side files were restored to the state before it. The direction remains as §38.20 (agreed); the merge placement returns to the OPEN question there.

### 38.22 The conversation box — the initial state and the expansion rule · **Decided (2026-10-09)**

- **The box opens small and vertically centred.** The initial, first-load view is the small box (a **280px flex basis** centred in the stage), not the full-height one. *(This supersedes the "chat fills the whole page" treatment; the box fills only once it is expanded.)* The small state is sized for the composer plus the seeded opening turn.
- **It expands on submit — and nothing else moves it.** The box goes to the full-height view **only when the prompt is submitted** (send button or Enter), i.e. when the first sent turn exists; it **stays expanded** thereafter. **Focusing the field does not expand it, and attaching a document does not expand it.** The reason is the person's eye line: they are looking at the box, and moving it while they attach or type breaks their place. *(This supersedes §38.14's focus-expand and attach-expand triggers; the small initial size itself is kept.)*
- **Mechanism.** `.content.home-col .convo { flex: 0 1 280px }` ↔ `.content.home-col .convo.is-expanded { flex: 1 1 auto }`, toggled by `syncBox()` on `chatOngoing()` (a sent turn). The change animates as a **flex-grow transition, 0.35s on the standard easing**. It is **page-local** (Home's stage/placement), as §38.14 had it — the shared box in `app.css` carries no sizing.
- **The same discipline as the immediate-action rule:** the interface should not make the person re-find where they were. A control that is being looked at does not move until the person commits.

### 38.23 The hero mood mark — and the box moves to the top · **Built (2026-10-09)**

- **The box is anchored to the top** of the content column, aligned with the breadcrumb and the rail's page action — not vertically centred. A centred box floats against a rail that starts at the top; top-aligning lets the page read top-down (where am I → Start work → ask) and turns the space *below* into the conversation's growth zone. *(This supersedes §38.22's "vertically centred"; the small→expand behaviour on submit is unchanged.)*
- **The hero mood mark.** The seeded opening turn is the **one place the conversation breaks its own rules**: it carries a square mood glyph at its top-left — a **44px tile, about two lines tall** — with the text flowing to its right. Rules:
  - **A mood, never an identity.** No "AI" chip, no label. §38.13 removed participant marks and this does not bring them back; the mark dresses the opening, it does not name the speaker.
  - **Monochrome-emoji, tinted.** The glyph is an emoji set in the **mono emoji face** (Noto Emoji in the standard stack) so its colour can be driven by CSS — tinted with the **system voice** (`--info`), the assistant's voice *in form* (§38.5) and the initial screen's one spot of colour. The mono emoji face is **self-hosted** (`screens/assets/fonts/noto-emoji/`, §38.6), so the tint is identical on every machine.
  - **The first message only.** It marks the opening turn of a Home — the **onboarding welcome** (a greeting glyph, 👋) and the **day-start morning** (the celebration, 🎉) — and every later turn stays plain; position still carries identity. One mood only, and the copy carries no inline emoji — the mood lives in the mark.
- **Why.** The initial screen is otherwise empty: a small box at the top with nothing above it. The hero mark gives the first line a focal point and a spot of colour, so the page opens with a **mood** rather than a wall of neutral text — it is the initial screen's window dressing.
- **Watch-outs for the build.** The mark must stay decorative (if it ever reads as an avatar it collides with §38.13), and it is a **transcript** element, not a control — it does not dim or stand down on focus the way the next-action bar does.

### 38.24 The prompt — immediate actions and the animated placeholder · **Built (2026-10-09)**

- **The immediate-action rule.** An action the assistant surfaces is **immediate**: it navigates, applies, or names the existing control — it is **never a question whose only outcome is a turn that redirects you to a button**. "Do you want to get started?" became "…the first is [the case]", so the copy carries the action and the detail itself. The **one exception is a genuine confirmation with consequences** (a plan awaiting Apply, a destructive change): that is still asked, because it is a real decision, not a pointer to somewhere else. The next-action bar (§38.19) carries the assistant's own forward moves; when the move belongs to the page action, the copy points at it. *(The same discipline as §38.22: never make the person re-find where they were.)*
- **The animated placeholder.** The prompt shows example asks **typed in turn** (Open Design style) via a shared helper — `hamoni.typewriter(input, examples)` in `app.js` — which writes each example into the placeholder, holds, erases and moves on. **Focusing the field clears it instantly** so a real prompt can be typed; it **resumes on blur while the field is still empty**; `prefers-reduced-motion` shows the first example statically. The **behaviour is shared; the examples are page content** — Home uses the four team-participant asks ("Assign Cindy Yin's work to Bob Tang" / "Create a new process for Claims processing" / "What is my next task?" / "Show me the SLA attainment for all Claims processes yesterday").

### 38.25 Home splits in two — onboarding and day-start · **Built (2026-10-09)**

- The prompt-first Home is now **two files**: **`home-onboarding.html`** (the first-run onboarding — the setup conversation) and **`home-daystart.html`** (the everyday participant view — a single seeded "morning" turn). Both share the shell, the conversation component, and the **hero mood mark** on the first message (§38.23): the onboarding welcome carries 👋, the day-start morning carries 🎉.
- The **day-start turn is seeded** — marked as the agent's, not generated — so the "day ahead" and onboarding open as one turn, and its copy carries **live deep-links** ("three tasks" → the tasks list; the first item → its case), per the immediate-action rule (§38.24).
- **The canonical Home is still open.** The nav, launcher and breadcrumbs currently point at `home-onboarding.html`; either may become the default when the project is restructured. Layouts differ: day-start is **top-aligned with a small→expand box** (§38.22–§38.23); onboarding keeps the full-height box and the held-state header.

### 38.26 Closing note — the hifi screen set · **(2026-10-09)**

The high-fidelity set is paused here. The shared shell (`screens/assets/`) carries the conversation component, the hero mood mark, the animated placeholder, the self-hosted mono emoji face and the surface model; both Home variants are built. Everything in §38.1–§38.25 is the current contract; `screens/controls.html` is the design-language artefact it must stay in step with.

### 38.27 The first journey, by persona — and the day summary · **Recorded (2026-10-15)**

`journeys/overview.html` opens each persona with its first journey. All three start on the same hero mood message (§38.23) and diverge after it:

- **Team participant** — sign in → read the hero mood message → open the Inbox → open a task. The message is the day-start line (e.g. *"Morning. Three of yours are due before noon — one is already waiting on you."*).
- **Team manager** — sign in → read the **day summary** → manage processes.
- **Process owner** — sign in → read the hero summary → **analytics (Activity) as the destination, or a query on Home**.

**The day summary is a design we have not done.** It is the same hero mood message, tuned to teams rather than one person's queue — the working example: *"Yesterday was great — your teams overachieved by 5 cases. Today might be tough: 2 more cases than usual, and 1 of your team are off."* It is **not built**.

**Open** — the process owner's first journey: whether analytics is the primary destination, or the owner stays on Home and asks for performance there.

---

## 39 · Voice and tone — the four principles · **Decided (2026-10-16)**

The product's tone is settled in four principles. They govern every user-facing string — screen copy, labels, empty states, notifications and the assistant's replies (the hero mood message, §38.23, included). They are carried into the front-end standards (`CODING-STANDARDS.md` §18.1) and belong, back-ported, in the design system's Voice & Tone, alongside the *people-powered, people-led* vocabulary.

- **Guiding.** Copy informs the person of **the next action** — what to do to resolve something or return to normal business. It names the step, not just the state.
- **Forward-looking.** Lead with **what is coming up or what is next**, not with what has already happened.
- **Supportive and encouraging.** Most people using Hamoni do **repetitive work**, so earned appreciation — *well done*, *congratulations* — helps them feel appreciated. **Never scold.** Where a negative must be conveyed, do not state it plainly ("you did not do well yesterday", "you have 30% more tasks today"); fall back to the guiding and forward-looking approaches — the working example: *"Yesterday was tough, but the good news is that you're already ahead for today."* The encouragement must be **sincere and specific**, never condescending.
- **Precise** — in **reporting and analytics only**. Be specific about what is being displayed or shown, without wording so literal that it obscures the intent.

**Back-port:** the same four principles, with the vocabulary and imagery, belong in the design system's Voice & Tone.

**Noted for a later pass:** the §38.27 day-summary example ("Today might be tough: 2 more cases than usual…") states a negative plainly; it is a candidate for the guiding / forward-looking rewrite above.

---

## 40 · The component board — kept in the app · **Recorded (2026-10-16)**

The PrimeNG component board (`control-room-ui/src/app/pages/home/components.html`) is **kept in the Angular application** as a living test surface — a page that renders every agreed control so the preset, the Tailwind theme and the shared styles can be verified against the design language. It is not a product screen.

**Parked, to pick up later** (deliberately not chased now):

- the `p-inputnumber` spinner still grows the field taller than its neighbours (the `button.verticalPadding: 0` lever was not enough);
- the app preset's **`p-tag` severities** still carry the old map (`info` = teal); the board now uses the `.badge` component, so nothing depends on it, but any `p-tag` screen should be realigned (`info` → neutral *normal · alternative*, `success` / `warn` / `danger` → RAG);
- the four **status pill levels** and the **RAG alert pills** are on the board and `controls.html` but not yet rolled onto the real screens;
- the **RAG palette, the four tone principles and the four pill levels** are recorded in the project docs and still need their **back-sync into the design system** (`DESIGN.md`);
- the §38.27 day-summary example awaits the tone rewrite (§39).

---

## 41 · Localisation — the profile drives language, numbers and dates · **Decided (2026-10-16)**

The interface is **localisable and internationalisable**, and the **user profile is the one source of the locale**. The organisation sets a default; the person may override it; that choice is stored on the profile as locale information.

- **Static copy and messages live in the front end** — labels, messages, states, notifications and the assistant's replies. No string is written inline in a component; all copy is held in the front-end resource bundle and looked up by a key (§18.3).
- **Problem messages from the interface carry human, displayable text, not codes.** The **backend does its best to localise them** into the profile's locale before they are returned, so the client is not left translating server prose.
- **Numbers and dates come from the profile's locale** — the organisation default, overridden by the user — never from the browser's or the development machine's default.

Recorded as **ADR-0062** (`ARCHITECTURE-DECISIONS.md`) and in **`CODING-STANDARDS.md` §18.3**.

---

## 42 · The error surface — one template for every problem · **Built (2026-10-27)**

`screens/error.html` is the single error page for every problem response. It renders an **RFC 9457 problem object** and assumes nothing about the status, so 403, 404 and 5xx all use it unchanged.

**The mapping.** `type` is not displayed. `title` is the page's one `h1`. `status` is the corner numeral, set in mono because it is a code rather than a data numeral (§38.6). `detail` is the body paragraph — the only prose the template does not author; the backend localises it (§41). `instance` identifies the occurrence, and its **trailing five alphanumerics are the request ID**, shown in mono with a copy control because it is the one thing a person may need to quote.

**No ID, no rule.** When the instance carries no valid five-character ID, the reference row is dropped *and so is the hairline above it* — the rule belongs to the row, not to the page. A divider with nothing beneath it would imply a reference that failed to load, and it would draw the eye to the absence on the one surface whose job is to explain a failure. The card then runs title → detail → action on the standard 28px beat; because the card is top-anchored, only its bottom edge moves — never the title.

**No shell.** No rail, no topbar, no page head: the person's state is unknown, so nothing may imply they are already inside a safe context. The brand mark stays, inside the panel, as identity only.

**One way out, and it is a door we know.** A single solid **Back to home** → `/`. Deliberately no other destinations: a 401 or 403 has no "nearest authorised page" we can trust, and a rare failure does not earn a menu. The page is never a dead end, and it never guesses a route back.

**Colour.** Purple is the only hue, and it is the one page action (§38.5). The status code is **muted, never RAG** — it is information, and the page must not shout before the title is read. No teal. There is **no `--dim` text on this surface**: at 12px it measures 3.2:1 on the panel, under the 4.5:1 floor, so captions use `--muted` (6.4:1).

**Shared asset.** `pi-copy` was added to the inlined PrimeIcons set in `app.css`; the template loads no external icon stylesheet.

**Focus moves to the heading, without a ring.** On load the title is focused so a screen reader announces the problem immediately. It is a `tabindex="-1"` heading, not a control, and it is not in the tab order — so it must not draw the keyboard focus ring. The accent stays on the page action (§38.5); `:focus` and `:focus-visible` are both suppressed on the card title deliberately, and that is not a WCAG exception because the element is not keyboard-reachable. In the Angular build the same rule is global rather than component-local — `[tabindex='-1']:focus, [tabindex='-1']:focus-visible { outline: none }` in `styles.css` — so it also covers skip-link destinations and every other programmatic focus target.

---

## 43 · The standalone card — one standard outside the shell · **Built (2026-10-27)**

Every surface with no rail and no topbar uses one card: **sign-in, the invitation landing, the organisation chooser and every error**. The component is `.sc-*` in `app.css`; `error.html` is its **wide** variant and the three sign-in surfaces are the **narrow** one. `auth-callback.html` is the one exception — it is a transition, not a decision, and is parked for its own pass.

**Identity sits inside the card.** A logo above a card is two objects doing one job, consumes vertical space above the only action on small screens, and — once the card is left-aligned — starts to look accidental. The card's **header band** carries identity on the left — the 22px mark with **`HAMONI`** set as text beside it (13px, uppercase, +0.06em) — and one piece of context on the right; only the error page fills that slot, with the status code. The rail keeps the mark alone (§38.3): the lockup belongs to arrival surfaces, the mark to chrome.

**Two widths, never a variable one.** Narrow **440** for the sign-in experience; wide **560** for the error surface. An implementer picks the variant and never reconciles a fluid width. Card padding 32 (24 below 560px), radius 10 (8 below 560px).

**The card top is pinned — on all four.** Only **one card** is rendered and `.sc-main` anchors it to a fixed top offset rather than centring it. On the sign-in surfaces the body swaps in place (public → organisation, invite → expired); on the error surface the title and `detail` change length with the problem. In both cases the identity band and the title stay exactly where they are, so the thing a person is there to read never drifts. **No surface centres its card**, and the offset is one shared number — if it ever needs tuning, it is tuned once for the family, never per surface.

**One rhythm inside:** header band → title → one body line → actions. Title Inter 700 at `--fs-h1` (24 on small screens), body Inter 400 14px muted, both **left-aligned**, matching every other surface in the product.

**Mono is only a code.** On this card, mono is restricted to system strings: the **error status code** and the **request ID**. Everything else — titles, body, labels, the provider buttons — is Inter. The old auth `.auth-group-label` in mono is retired.

**The footer stays outside the card — on every surface, error included.** It reads `© 2026 Hamoni Limited · Privacy policy · Terms of service`, and belongs to the page, not to the object (`.sc-foot`). It is present on all four so the frame is identical; on the error it also anchors the bottom of the page, which the high card would otherwise leave open.

**Footer links are underlined at rest.** They share the muted tone with the surrounding text, so there is no colour difference to lean on and hover is not an affordance (WCAG 1.4.1, and it does not exist on touch). The **copyright item is one link** — the whole `© 2026 Hamoni Limited` is the target for the corporate site — so its underline covers the item rather than a fragment of it, and the row reads as three equal affordances. Never link only part of a footer item: a half-line underline beside two full ones looks like a rendering fault.

**Implementation shape — `CardPageLayout` + `CardPageHeaderComponent`.** Two pieces, split by who owns what:

- **`CardPageLayout`** (`card-page-layout`) is the routed frame: canvas, the pinned column, the **card shell**, and the legal footer. It takes `@Input() wide: boolean` and nothing else, so it never needs to know which page is inside it.
- **`CardPageHeaderComponent`** (`ham-card-page-header`) is the identity band — mark, wordmark, and an optional `@Input() context` in the right slot (the error's status code). The **page places it** inside the card, so the header's construction is standardised while its context stays under the page's control. Without that input the slot is empty, which is the sign-in and invitation case.
- The page supplies the title, body line and actions as content inside the card shell.

**Width comes from the route.** `data: { wide: true }` is bound to the layout's input (`withComponentInputBinding()`; otherwise read `route.snapshot.data`). Route data belongs to the routed component of *that* route, so each width needs its own layout entry — the error route carries `wide: true`, the sign-in group carries `wide: false` or omits it. A parent layout cannot read a child route's data; do not try.

Names rejected: `CentredLayout` (the card is pinned, not centred), `DialogLayout` (a routed page, not a modal — no scrim, focus trap or `role="dialog"`), and `StandaloneLayout` ("standalone" already means NgModule-free in Angular 17+). The single-component `CardPageComponent` was the earlier idea and is superseded: it forced each page to own the canvas and footer.

**Body copy is not a restatement.** The line under the title answers the question the layout cannot: **"Use the account you use for work."** on the public page, **"Use your organisation's provider, or a public account."** on an organisation's page. "Choose a login method from the selection below" was rejected — it narrates the page instead of guiding (§39).
